package com.example.admin.keyproirityapp.adapter;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.graphics.drawable.BitmapDrawable;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.BaseTarget;
import com.bumptech.glide.request.target.SizeReadyCallback;
import com.bumptech.glide.request.transition.Transition;
import com.example.admin.keyproirityapp.GlideApp;
import com.example.admin.keyproirityapp.R;
import com.example.admin.keyproirityapp.databinding.RowChatReceiverBinding;
import com.example.admin.keyproirityapp.databinding.RowChatSenderBinding;
import com.example.admin.keyproirityapp.listners.ClickListenerChatFirebase;
import com.example.admin.keyproirityapp.model.ChatModel;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

/**
 * Created by Dell on 9/1/2018.
 */

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static final String TAG = ChatAdapter.class.getSimpleName();
    private static final int SENDER = 1;
    private static final int RECEIVER = 2;
    private final LayoutInflater inflater;
    private final List<ChatModel> mChatList;
    private ClickListenerChatFirebase mClickListenerChatFirebase;

    //
    public ChatAdapter(Context context, List<ChatModel> chatList, ClickListenerChatFirebase clickListenerChatFirebase) {
        inflater = LayoutInflater.from(context);
        this.mChatList = chatList;
        this.mClickListenerChatFirebase = clickListenerChatFirebase;

    }

    public static String getTime(String milliseconds) {
        String dateFormat = "hh:mm:a";
        SimpleDateFormat formatter = new SimpleDateFormat(dateFormat);
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(Long.parseLong(milliseconds));
        return formatter.format(calendar.getTime());
    }

    public static void loadImage(final Context context, String imageUrl, final ImageView imageView, TextView textView) {
        BaseTarget target2 = new BaseTarget<BitmapDrawable>() {
            @Override
            public void onResourceReady(BitmapDrawable bitmap, Transition<? super BitmapDrawable> transition) {
                // do something with the bitmap
                // for demonstration purposes, let's set it to an imageview
                imageView.setImageDrawable(bitmap);
            }

            @Override
            public void getSize(SizeReadyCallback cb) {
                cb.onSizeReady(250, 250);
            }

            @Override
            public void removeCallback(SizeReadyCallback cb) {
            }
        };
        if (TextUtils.isEmpty(imageUrl)) {
            imageView.setImageDrawable(null);
            imageView.setPadding(0, 0, 0, 0);
            imageView.setVisibility(View.GONE);
        } else {
            imageView.setVisibility(View.VISIBLE);
            textView.setVisibility(View.GONE);
            GlideApp.with(context)
                    .load(imageUrl)
                    .apply(new RequestOptions().override(250, 250).placeholder(R.drawable.nophoto).error(R.drawable.androidphoto))
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .fitCenter()
                    .into(imageView);
        }

    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        switch (viewType) {
            case SENDER:
                View senderView = inflater.inflate(R.layout.row_chat_sender, parent, false);
                return new SenderHolder(senderView);
            case RECEIVER:
                View receiverView = inflater.inflate(R.layout.row_chat_receiver, parent, false);
                return new ReceiverHolder(receiverView);
            default:
        }
        return null;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder baseHolder, final int position) {
        int viewType = getItemViewType(position);
        final ChatModel chatModel = getItemAtPosition(position);

        baseHolder.setIsRecyclable(false);
        switch (viewType) {
            case SENDER:
                final SenderHolder senderHolder = (SenderHolder) baseHolder;
                senderHolder.senderBinding.tvName.setText(chatModel.getUserName());
                senderHolder.senderBinding.tvTime.setText(getTime(chatModel.getTime()));
                senderHolder.senderBinding.tvMessage.setText(chatModel.getMessage());

                if (chatModel.isIsseen()) {
                }
                if (chatModel.getContentType().equals("audio")) {
                    Log.d(TAG, "onBindViewHolder: " + chatModel.getContentLocation());
                    senderHolder.senderBinding.tvName.setText(chatModel.getUserName());
                    senderHolder.senderBinding.tvTime.setText(getTime(chatModel.getTime()));
                    senderHolder.senderBinding.tvMessage.setText(chatModel.getMessage());

                }
                Log.d(TAG, "onBindViewHolder: audio file" + chatModel.getContentType().equals("audio"));
                if (chatModel.getContentType().equals("Image")) {
                    loadImage(inflater.getContext(), chatModel.getImageUrl(), senderHolder.senderBinding.ivImage, senderHolder.senderBinding.tvMessage);
                    senderHolder.senderBinding.ivImage.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            String userName = chatModel.getUserName();
                            String userPhoto = chatModel.getAvatarUrl();
                            if (userName != null) {
                                mClickListenerChatFirebase.clickImageChat(view, position, userName, "default", chatModel.getImageUrl());
                            }
                            mClickListenerChatFirebase.clickImageChat(view, position, "default", "default", chatModel.getImageUrl());
                        }
                    });

                }
                break;
            case RECEIVER:
                ReceiverHolder receiverHolder = (ReceiverHolder) baseHolder;
                receiverHolder.receiverBinding.tvName.setText(chatModel.getUserName());
                receiverHolder.receiverBinding.tvTime.setText(getTime(chatModel.getTime()));
                receiverHolder.receiverBinding.tvMessage.setText(chatModel.getMessage());
                Log.d(TAG, "onBindViewHolder: audio file" + chatModel.getContentType().equals("audio"));
                loadImage(inflater.getContext(), chatModel.getImageUrl(), receiverHolder.receiverBinding.ivImage, receiverHolder.receiverBinding.tvMessage);
                if (chatModel.getContentType().equals("audio")) {
                    receiverHolder.receiverBinding.tvName.setText(chatModel.getUserName());
                    receiverHolder.receiverBinding.tvTime.setText(getTime(chatModel.getTime()));
                    receiverHolder.receiverBinding.tvMessage.setText(chatModel.getMessage());
                    Log.d(TAG, "onBindViewHolder: " + chatModel.getContentLocation());

                }
                if (chatModel.getContentType().equals("Image")) {
                    receiverHolder.receiverBinding.ivImage.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            String userName = chatModel.getUserName();
                            String userPhoto = chatModel.getAvatarUrl();
                            if (userName != null) {
                                mClickListenerChatFirebase.clickImageChat(view, position, userName, "default", chatModel.getImageUrl());
                            }
                            mClickListenerChatFirebase.clickImageChat(view, position, "default", "default", chatModel.getImageUrl());
                        }
                    });

                }

                break;
            default:
        }
    }

    @Override
    public int getItemViewType(int position) {
        /*switch (mChatList.get(position).getContentType()){
            case "text":
                if(mChatList.get(position).isSender()){
                    return SENDER;
                }
                if(!mChatList.get(position).isSender()){
                    return RECEIVER;
                }
                break;
            case "Image":
                if(mChatList.get(position).isSender()){
                    return SENDER;
                }
                if(!mChatList.get(position).isSender()){
                    return RECEIVER;
                }
                break;
            case "audio":
                if(mChatList.get(position).isSender()){
                    return SENDER;
                }
                if(!mChatList.get(position).isSender()){
                    return RECEIVER;
                }
                break;
            default:
                return -1;
        }
        */
        Log.d(TAG, "getItemViewType: item type" + mChatList.get(position).getContentType().equals("audio"));
        return getItemAtPosition(position).isSender() ? SENDER : RECEIVER;

    }

    public ChatModel getItemAtPosition(int position) {
        return mChatList.get(position);
    }

    @Override
    public int getItemCount() {
        return mChatList.size();
    }

    public static class SenderHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        RowChatSenderBinding senderBinding;

        public SenderHolder(View itemView) {
            super(itemView);
            senderBinding = DataBindingUtil.bind(itemView);
        }

        @Override
        public void onClick(View v) {
        }
    }

    public static class ReceiverHolder extends RecyclerView.ViewHolder {
        RowChatReceiverBinding receiverBinding;

        public ReceiverHolder(View itemView) {
            super(itemView);
            receiverBinding = DataBindingUtil.bind(itemView);
        }
    }
}


