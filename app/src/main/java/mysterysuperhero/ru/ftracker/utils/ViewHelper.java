package mysterysuperhero.ru.ftracker.utils;

import android.bluetooth.BluetoothDevice;
import android.support.design.widget.Snackbar;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by dmitri on 21.11.16.
 */

public class ViewHelper {

    public static void showSnackBar(View view, String text) {
        Snackbar snackbar = Snackbar
                .make(view, text, Snackbar.LENGTH_LONG);
        snackbar.show();
    }

    public static ArrayList<String> bluetoothDevicesNames(List<BluetoothDevice> devices) {
        ArrayList<String> result = new ArrayList<>();
        for (BluetoothDevice device : devices) {
            result.add(device.getName());
        }
        return result;
    }

}
