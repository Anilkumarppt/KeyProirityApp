package com.example.admin.keyproirityapp.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.admin.keyproirityapp.R;
import com.example.admin.keyproirityapp.database.StaticConfig;
import com.example.admin.keyproirityapp.model.Conversation;
import com.example.admin.keyproirityapp.model.GroupMessage;
import com.google.firebase.database.DatabaseReference;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Dell on 8/30/2018.
 */

public class GroupMessagesAdapter extends RecyclerView.Adapter<GroupMessagesAdapter.MessagesHolder> {
    public static final int VIEW_TYPE_USER_MESSAGE = 0;
    public static final int VIEW_TYPE_FRIEND_MESSAGE = 1;
    private static String TAG = "GroupMessageAdapter";
    public List<GroupMessage> conversationList;
    private Context context;
    private Conversation conversation;
    private HashMap<String, Bitmap> bitmapAvata;
    private HashMap<String, DatabaseReference> bitmapAvataDB;
    private Bitmap bitmapAvataUser;

    public GroupMessagesAdapter(Context context, List<GroupMessage> conversationList, HashMap<String, Bitmap> bitmapAvata, Bitmap bitmapAvataUser) {
        this.conversationList = conversationList;
        this.context = context;
        this.bitmapAvata = bitmapAvata;
        this.bitmapAvataDB = bitmapAvataDB;
        this.bitmapAvataUser = bitmapAvataUser;
    }

    public static String getTime(String milliseconds) {
        String dateFormat = "hh:mm:a";
        SimpleDateFormat formatter = new SimpleDateFormat(dateFormat);
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(Long.parseLong(milliseconds));
        return formatter.format(calendar.getTime());
    }

    public static void loadImage(Context context, String imageUrl, ImageView imageView, TextView textView, TextView txtSender, String sender) {
        if (TextUtils.isEmpty(imageUrl)) {
            imageView.setVisibility(View.GONE);
        } else {
            Log.d(TAG, "loadImage: " + imageUrl);
            textView.setPadding(0, 0, 0, 0);
            imageView.setVisibility(View.VISIBLE);
            textView.setVisibility(View.GONE);
            txtSender.setText(sender);
            Glide.with(context)
                    .load(imageUrl)
                    // .placeholder(R.mipmap.ic_launcher)
                    //.thumbnail(thumbnailRequest)
                    // .dontAnimate()
//                .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(imageView);
        }
    }

    @NonNull
    @Override
    public MessagesHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == VIEW_TYPE_USER_MESSAGE) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_chat_sender, parent, false);
            return new MessagesHolder(view);
        } else if (viewType == VIEW_TYPE_FRIEND_MESSAGE) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_chat_receiver, parent, false);
            return new MessagesHolder(view);
        }
        return null;
    }

    @Override
    public void onBindViewHolder(@NonNull MessagesHolder holder, int position) {
        String time = String.valueOf((conversationList.get(position).timestamp));
        String msg = conversationList.get(position).text;
        String senderName = conversationList.get(position).getSenderName();
        Log.d("messageBody", String.valueOf(time));
        holder.setMessage(msg, String.valueOf(time));
        holder.senderName.setText(conversationList.get(position).getSenderName());
        loadImage(context, conversationList.get(position).getContentLocation(), holder.imageMessage, holder.messageBody, holder.senderName, senderName);
    }

    @Override
    public int getItemCount() {
        return conversationList.size();
    }

    @Override
    public int getItemViewType(int position) {
        return conversationList.get(position).idSender.equals(StaticConfig.UID) ? VIEW_TYPE_USER_MESSAGE : VIEW_TYPE_FRIEND_MESSAGE;
    }

    public class MessagesHolder extends RecyclerView.ViewHolder {

        TextView messageBody, messageTime, senderName;
        ImageView imageMessage;

        public MessagesHolder(View itemView) {
            super(itemView);
            messageBody = itemView.findViewById(R.id.tv_message);
            messageTime = itemView.findViewById(R.id.tv_time);
            senderName = itemView.findViewById(R.id.tv_name);
            imageMessage = itemView.findViewById(R.id.iv_image);
        }

        public void setMessage(String msg, String time) {
            int viewType = GroupMessagesAdapter.this.getItemViewType(getLayoutPosition());
            messageBody.setText(msg);
            messageTime.setText(getTime(time));

        }
    }
}
