package mysterysuperhero.ru.ftracker.ui;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import mysterysuperhero.ru.ftracker.R;
import mysterysuperhero.ru.ftracker.data.BPMItem;
import mysterysuperhero.ru.ftracker.data.StepsItem;

/**
 * Created by dmitri on 22.11.16.
 */

public class TrackerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private StepsItem stepsItem;
    private ArrayList<BPMItem> bpmList = new ArrayList<>();

    private final static int EMPTY_VIEWHOLDER = 0;
    private final static int STEPS_VIEWHOLDER = 1;
    private final static int BPM_VIEWHOLDER = 2;

    private class StepsViewHolder extends RecyclerView.ViewHolder {

        TextView stepsCountTextView;
        TextView dateTextView;

        StepsViewHolder(View itemView) {
            super(itemView);
            stepsCountTextView = (TextView) itemView.findViewById(R.id.stepsCountTextView);
            dateTextView = (TextView) itemView.findViewById(R.id.dateTextView);
        }
    }

    private class BPMViewHolder extends RecyclerView.ViewHolder {

        TextView bpmTextView;
        TextView dateTextView;

        BPMViewHolder(View itemView) {
            super(itemView);
            bpmTextView = (TextView) itemView.findViewById(R.id.bpmTextView);
            dateTextView = (TextView) itemView.findViewById(R.id.dateTextView);
        }
    }

    public TrackerAdapter(StepsItem stepsItem, ArrayList<BPMItem> bpmItems) {
        this.stepsItem = stepsItem;
        this.bpmList.addAll(bpmItems);
    }

    private class EmptyViewHolder extends RecyclerView.ViewHolder {


        EmptyViewHolder(View itemView) {
            super(itemView);
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        switch (viewType) {
            case EMPTY_VIEWHOLDER:
                final View v = inflater.inflate(R.layout.item_empty_layout, parent, false);
                return new EmptyViewHolder(v);
            case STEPS_VIEWHOLDER:
                final View vv = inflater.inflate(R.layout.item_steps, parent, false);
                return new StepsViewHolder(vv);
            case BPM_VIEWHOLDER:
                final View vvv = inflater.inflate(R.layout.item_heart_rate, parent, false);
                return new BPMViewHolder(vvv);
        }
        return null;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (bpmList.isEmpty() && stepsItem == null) {
            return;
        }

        if (position == 0) {
            StepsViewHolder stepsViewHolder = (StepsViewHolder) holder;
            if (stepsItem != null) {
                stepsViewHolder.stepsCountTextView.setText(String.valueOf(stepsItem.getSteps()));
                Date date = new Date(stepsItem.getTime());
                SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yy HH:mm:ss");
                stepsViewHolder.dateTextView.setText(formatter.format(date));
            } else {
                stepsViewHolder.stepsCountTextView.setText("0");
                stepsViewHolder.dateTextView.setText("");
            }
        } else {
            BPMViewHolder bpmViewHolder = (BPMViewHolder) holder;
            BPMItem bpmItem = bpmList.get(position - 1);
            bpmViewHolder.bpmTextView.setText(String.valueOf(bpmItem.getBpm()));
            Date date = new Date(bpmItem.getTime());
            SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yy HH:mm:ss");
            bpmViewHolder.dateTextView.setText(formatter.format(date));
        }
    }

    @Override
    public int getItemCount() {
        return bpmList.size() + 1;
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) {
//            if (stepsItem == null) {
//                if (bpmList.isEmpty()) {
//                    return EMPTY_VIEWHOLDER;
//                }
//                return STEPS_VIEWHOLDER;
//            } else {
                return STEPS_VIEWHOLDER;
//            }
        } else {
            return BPM_VIEWHOLDER;
        }
    }

    public Long getStepsCount() {
        return stepsItem.getSteps();
    }

    public void setSteps(StepsItem stepsItem) {
        this.stepsItem = stepsItem;
    }

    public void addBPMItem(BPMItem bpmItem) {
        bpmList.add(bpmItem);
    }

}

