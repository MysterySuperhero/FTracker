package mysterysuperhero.ru.ftracker.ui;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import mysterysuperhero.ru.ftracker.R;
import mysterysuperhero.ru.ftracker.data.BPMItem;
import mysterysuperhero.ru.ftracker.data.DataBaseHelper;
import mysterysuperhero.ru.ftracker.data.StepsItem;
import mysterysuperhero.ru.ftracker.events.UpdatingStepsFailedEvent;
import mysterysuperhero.ru.ftracker.utils.ViewHelper;

import static mysterysuperhero.ru.ftracker.utils.ViewHelper.showSnackBar;

public class MainActivity extends AppCompatActivity {

    private FloatingActionButton floatingActionButton;
    private RecyclerView recyclerView;
    private TrackerAdapter adapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private FrameLayout dialogFrameLayout;
    private AlertDialog alertDialog;

    private boolean updating = false;

    private DataBaseHelper dataBaseHelper;

    private BluetoothAdapter mBTAdapter;
    private List<BluetoothDevice> mPairedDevices = new ArrayList<>();
    private List<BluetoothDevice> mFoundedDevices = new ArrayList<>();
    private Handler mHandler; // Our main handler that will receive callback notifications
    private ConnectedThread mConnectedThread; // bluetooth background worker thread to send and receive data
    private BluetoothSocket mBTSocket = null; // bi-directional client-to-client data path

    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // "random" unique identifier

    // #defines for identifying shared types between calling functions
    private final static int REQUEST_ENABLE_BT = 4; // used to identify adding bluetooth names
    private final static int MESSAGE_READ = 2; // used in bluetooth handler to identify message update
    private final static int CONNECTING_STATUS = 3; // used in bluetooth handler to identify message status

    private final static String MEASURE_BPM_CODE = "9";
    private final static String UPDATE_STEPS_CODE = "8";

    final BroadcastReceiver blReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                mFoundedDevices.add(device);
            } else if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        break;
                    case BluetoothAdapter.STATE_ON:
                        showSnackBar(findViewById(R.id.scrollView),
                                getResources().getString(R.string.message_bluetooth_on));
                        discover();
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        break;
                }
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EventBus.getDefault().register(this);

        dataBaseHelper = new DataBaseHelper();

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        floatingActionButton = (FloatingActionButton) findViewById(R.id.fab);
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mConnectedThread != null) {
                    mConnectedThread.write(MEASURE_BPM_CODE);
                } else {
                    ViewHelper.showSnackBar(findViewById(R.id.scrollView),
                            getResources().getString(R.string.message_connect_to_device));
                }
            }
        });

        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(linearLayoutManager);
        List<BPMItem> bpmItems = dataBaseHelper.getBPMList();
        StepsItem stepsItem = dataBaseHelper.getSteps();
        adapter = new TrackerAdapter(stepsItem, (ArrayList<BPMItem>) bpmItems);
        recyclerView.setAdapter(adapter);

        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if (mConnectedThread != null) {
                    mConnectedThread.write(UPDATE_STEPS_CODE);
                    updating = true;
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Thread.sleep(10000L);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            if (updating) {
                                EventBus.getDefault().post(new UpdatingStepsFailedEvent());
                            }
                        }
                    }).start();

                } else {
                    ViewHelper.showSnackBar(findViewById(R.id.scrollView),
                            getResources().getString(R.string.message_connect_to_device));
                    swipeRefreshLayout.setRefreshing(false);
                }
            }
        });

        dialogFrameLayout = (FrameLayout) findViewById(R.id.dialogLayout);

        mBTAdapter = BluetoothAdapter.getDefaultAdapter(); // get a handle on the bluetooth radio

        if (mBTAdapter.isEnabled())
            discover();

        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(blReceiver, filter);

        mHandler = new Handler() {
            public void handleMessage(android.os.Message msg) {
                if (msg.what == MESSAGE_READ) {
                    String readMessage;
                    try {
                        readMessage = new String((byte[]) msg.obj, "UTF-8");
                        swipeRefreshLayout.setRefreshing(false);
                        long response = getResponse(readMessage);
                        if (updating) {
                            updating = false;
                            dataBaseHelper.saveStepsCount(response);
                            adapter.setSteps(dataBaseHelper.getSteps());
                        } else {
                            dataBaseHelper.saveBPM(response);
                            adapter.addBPMItem(dataBaseHelper.getLastBPM());
                        }
                        adapter.notifyDataSetChanged();
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                }

                if (msg.what == CONNECTING_STATUS) {
                    if (msg.arg1 == 1) {
                        mConnectedThread.write("0");
                        ViewHelper.showSnackBar(findViewById(R.id.scrollView),
                                getResources().getString(R.string.message_connected));
                    } else {
                        ViewHelper.showSnackBar(findViewById(R.id.scrollView),
                                getResources().getString(R.string.message_connection_failed));
                    }

                    if (alertDialog != null) alertDialog.cancel();
                }
            }
        };
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(blReceiver);
        EventBus.getDefault().unregister(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id) {
            case R.id.action_connect:
                showPairedDevices();
                break;
            case R.id.action_show_devices:
                showFoundedDevices();
                break;
            case R.id.action_bluetooth_on:
                bluetoothOn(findViewById(R.id.scrollView));
                break;
            case R.id.action_bluetooth_off:
                bluetoothOff(findViewById(R.id.scrollView));
                break;
        }

        return super.onOptionsItemSelected(item);
    }


    private void searchDevices() {
    }


    private void bluetoothOn(View view) {
        if (!mBTAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            showSnackBar(view, getResources().getString(R.string.message_bluettoth_already_on));
        }
    }

    private void bluetoothOff(View view) {
        mBTAdapter.disable(); // turn off
        mFoundedDevices.clear();
        mPairedDevices.clear();
        showSnackBar(view, getResources().getString(R.string.message_bluetooth_off));
    }

    private void showPairedDevices() {
        Set<BluetoothDevice> pairedDevices = mBTAdapter.getBondedDevices();
        mPairedDevices.clear();
        for (BluetoothDevice device : pairedDevices) {
            mPairedDevices.add(device);
        }

        if (mBTAdapter.isEnabled()) {
            if (!mPairedDevices.isEmpty()) {
                ArrayList<String> list = ViewHelper.bluetoothDevicesNames(mPairedDevices);
                new MaterialDialog.Builder(this)
                        .title(R.string.title_paired_devices)
                        .items(list)
                        .itemsCallbackSingleChoice(0, new MaterialDialog.ListCallbackSingleChoice() {
                            @Override
                            public boolean onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
                                connect(false, which);
                                return true; // allow selection
                            }
                        })
                        .positiveText(R.string.md_choose_label)
                        .negativeText(R.string.md_cancel_label)
                        .show();
            } else {
                ViewHelper.showSnackBar(findViewById(R.id.scrollView),
                        getResources().getString(R.string.message_paired_devices_list_is_empty));
            }
        } else {
            ViewHelper.showSnackBar(findViewById(R.id.scrollView), getResources().getString(R.string.message_bluetooth_off));
        }
    }

    private void showFoundedDevices() {
        if (mBTAdapter.isEnabled()) {
            if (!mFoundedDevices.isEmpty()) {
                ArrayList<String> list = ViewHelper.bluetoothDevicesNames(mFoundedDevices);
                new MaterialDialog.Builder(this)
                        .title(R.string.title_founded_devices)
                        .items(list)
                        .itemsCallbackSingleChoice(0, new MaterialDialog.ListCallbackSingleChoice() {
                            @Override
                            public boolean onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
                                connect(true, which);
                                return true; // allow selection
                            }
                        })
                        .positiveText(R.string.md_choose_label)
                        .negativeText(R.string.md_cancel_label)
                        .show();
            } else {
                ViewHelper.showSnackBar(findViewById(R.id.scrollView),
                        getResources().getString(R.string.message_searching));
            }
        } else {
            ViewHelper.showSnackBar(findViewById(R.id.scrollView), getResources().getString(R.string.message_bluetooth_off));
        }
    }

    private void discover() {
        // Check if the device is already discovering
        if (mBTAdapter.isDiscovering()) {
            mBTAdapter.cancelDiscovery();
        } else {
            if (mBTAdapter.isEnabled()) {
                mBTAdapter.startDiscovery();
                registerReceiver(blReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
            } else {
                ViewHelper.showSnackBar(findViewById(R.id.scrollView), getResources().getString(R.string.message_bluetooth_off));
            }
        }
    }

    private void connect(boolean founded, int index) {

        final String address = founded ? mFoundedDevices.get(index).getAddress() : mPairedDevices.get(index).getAddress();
        final String name = founded ? mFoundedDevices.get(index).getName() : mPairedDevices.get(index).getName();

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_connecting, null);
        dialogBuilder.setView(dialogView);
        dialogBuilder.setCancelable(false);

        alertDialog = dialogBuilder.create();
        alertDialog.show();

        // Spawn a new thread to avoid blocking the GUI one
        new Thread() {
            public void run() {
                boolean fail = false;

                BluetoothDevice device = mBTAdapter.getRemoteDevice(address);

                try {
                    mBTSocket = createBluetoothSocket(device);
                } catch (IOException e) {
                    fail = true;
                    Toast.makeText(getBaseContext(), "Socket creation failed", Toast.LENGTH_SHORT).show();
                }
                // Establish the Bluetooth socket connection.
                try {
                    mBTSocket.connect();
                } catch (IOException e) {
                    try {
                        fail = true;
                        mBTSocket.close();
                        mHandler.obtainMessage(CONNECTING_STATUS, -1, -1)
                                .sendToTarget();
                    } catch (IOException e2) {
                        //insert code to deal with this
                        Toast.makeText(getBaseContext(), "Socket creation failed", Toast.LENGTH_SHORT).show();
                    }
                }
                if (fail == false) {
                    mConnectedThread = new ConnectedThread(mBTSocket);
                    mConnectedThread.start();

                    mHandler.obtainMessage(CONNECTING_STATUS, 1, -1, name)
                            .sendToTarget();
                }
            }
        }.start();
    }

    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        return device.createRfcommSocketToServiceRecord(BTMODULEUUID);
        //creates secure outgoing connection with BT device using UUID
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];  // buffer store for the stream
            int bytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream

                    if (mmInStream.available() > 0) {
                        bytes = mmInStream.read(buffer);
                        mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer)
                                .sendToTarget();
                    } else {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                } catch (IOException e) {
                    break;
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(String input) {
            byte[] bytes = input.getBytes();           //converts entered String into bytes
            try {
                 mmOutStream.write(bytes);
            } catch (IOException e) {
            }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onUpdatingStepsFailed(UpdatingStepsFailedEvent event) {
        updating = false;
        swipeRefreshLayout.setRefreshing(false);
        ViewHelper.showSnackBar(findViewById(R.id.scrollView),
                getResources().getString(R.string.message_connection_failed));
    }


    private Long getResponse(String reply) {
        String result = "";
        for (int i = 0; i < reply.length(); ++i) {
            if (reply.charAt(i) >= 48 && reply.charAt(i) <= 57) {
                result = result.concat(String.valueOf(reply.charAt(i)));
            } else {
                return !result.isEmpty() ? Long.valueOf(result) : 0L;
            }
        }
        return 0L;
    }

}
