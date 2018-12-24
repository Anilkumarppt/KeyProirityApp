package com.example.admin.keyproirityapp.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.admin.keyproirityapp.R;
import com.example.admin.keyproirityapp.videocall.CallMessage;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Created by DELL on 11/27/2018.
 */

public class CallHistoryAdapter extends RecyclerView.Adapter<CallHistoryAdapter.HistoryHolder> {
    public View view;
    public Context context;
    public String TAG = CallHistoryAdapter.class.getSimpleName();
    List<CallMessage> callHistory = new ArrayList<>();

    //
    public CallHistoryAdapter(List<CallMessage> callHistory, Context context) {
        this.callHistory = callHistory;
        this.context = context;
    }

    public static String getTime(String milliseconds) {
        String dateFormat = "dd/MM/yy hh:mm:a";
        SimpleDateFormat formatter = new SimpleDateFormat(dateFormat);

        // Create a calendar object that will convert the date and time value in milliseconds to date.
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(Long.parseLong(milliseconds));
        Log.d("Get TIme", "getTime: " + calendar.getTime());
        return formatter.format(calendar.getTime());
    }

    @NonNull
    @Override
    public HistoryHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_call_history, parent, false);

        return new HistoryHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryHolder holder, int position) {
        boolean isIncoming = callHistory.get(position).isIncoming;
        if (callHistory.get(position).getDuration() != null) {
            Log.d(TAG, "onBindViewHolder: call duration " + callHistory.get(position).getDuration());
        }
        String time = String.valueOf(callHistory.get(position).startTime);
        if (time != null) {
            if (isIncoming) {
                time = getTime(String.valueOf(callHistory.get(position).startTime));
                holder.callDuration.setText("InComing call " + time);
                holder.callerName.setText(callHistory.get(position).getSenderName());

            } else {
                time = getTime(String.valueOf(callHistory.get(position).startTime));
                holder.callerName.setText(callHistory.get(position).getReceiverName());
                holder.callDuration.setText("OutGoing call " + time);
            }
        }

    }

    @Override
    public int getItemCount() {
        return callHistory.size();
    }

    public class HistoryHolder extends RecyclerView.ViewHolder {
        public TextView callerName, callDuration;

        public HistoryHolder(View itemView) {
            super(itemView);
            callerName = itemView.findViewById(R.id.tv_contact_name);
            callDuration = itemView.findViewById(R.id.tv_call_duration);
        }
    }
}
