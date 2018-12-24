package com.example.admin.keyproirityapp.adapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.admin.keyproirityapp.R;
import com.example.admin.keyproirityapp.database.StaticConfig;
import com.example.admin.keyproirityapp.service.MessagingService;
import com.example.admin.keyproirityapp.ui.ChatActivity;
import com.example.admin.keyproirityapp.ui.ChatListItemModel;
import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Created by DELL on 11/3/2018.
 */

public class ChatListAdapter extends RecyclerView.Adapter<ChatListAdapter.MyChatListHolder> {
    public static final String TAG = ChatListAdapter.class.getSimpleName();
    private List<ChatListItemModel> listFriend;
    private Context context;

    //
    public ChatListAdapter(Context context, List<ChatListItemModel> listFriend) {
        this.listFriend = listFriend;
        this.context = context;
    }

    public static String getTime(String milliseconds) {
        String dateFormat = "hh:mm:a";
        SimpleDateFormat formatter = new SimpleDateFormat(dateFormat);
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(Long.parseLong(milliseconds));
        return formatter.format(calendar.getTime());
    }

    @NonNull
    @Override
    public MyChatListHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.rc_item_friend, parent, false);
        return new MyChatListHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final MyChatListHolder holder, int position) {
        final ChatListItemModel model = listFriend.get(holder.getAdapterPosition());
        if (model.getFriendId() == FirebaseAuth.getInstance().getCurrentUser().getUid()) {
            Log.i(TAG, "Same Uid" + FirebaseAuth.getInstance().getCurrentUser().getUid());
        }
        Log.i("ChatListData", model.toString());
        holder.txtName.setText(model.getFriendName());
        holder.txtMessage.setText(model.getRecentMessage());
        String friendAvatar = model.getAvtar();
        if (friendAvatar != null) {
            if (friendAvatar.equals("default")) {
                holder.avata.setImageResource(R.drawable.profile_icon);
            } else {
                byte[] decodedString = Base64.decode(model.getAvtar(), Base64.DEFAULT);
                Bitmap src = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                (holder).avata.setImageBitmap(src);
            }
        }
        String time = new SimpleDateFormat("EEE, d MMM yyyy").format(new Date(model.getRecentTimeStamp()));
        String today = new SimpleDateFormat("EEE, d MMM yyyy").format(new Date(System.currentTimeMillis()));
        if (today.equals(time)) {
            holder.txtTime.setText(getTime(String.valueOf(model.getRecentTimeStamp())));
        } else {
            holder.txtTime.setText(new SimpleDateFormat("MMM d").format(new Date(model.getRecentTimeStamp())));
        }

      /*  holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Collections.sort(listFriend, new ChatListItemModel.TimeStampCamparator());
                notifyDataSetChanged();
            }
        });
      */
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(context, ChatActivity.class);
                intent.putExtra(StaticConfig.INTENT_KEY_CHAT_FRIEND, model.getFriendName());
                ArrayList<CharSequence> idFriend = new ArrayList<CharSequence>();
                idFriend.add(model.getFriendId());
                intent.putExtra(StaticConfig.INTENT_KEY_CHAT_ID, model.getRoomId());
                intent.putExtra(StaticConfig.PERSONAL_CHAT, "onetoone");
                intent.putCharSequenceArrayListExtra(StaticConfig.INTENT_KEY_CHAT_ID, idFriend);
                intent.putExtra(StaticConfig.INTENT_KEY_CHAT_ROOM_ID, model.getRoomId());
                intent.putExtra(MessagingService.FCM_SENDER_ID, model.getFriendId());
                context.startActivity(intent);
            }
        });

    }

    @Override
    public int getItemCount() {
        return listFriend.size();
    }

    public class MyChatListHolder extends RecyclerView.ViewHolder {
        public CircleImageView avata;
        public TextView txtName, txtTime, txtMessage;

        public MyChatListHolder(View itemView) {
            super(itemView);
            avata = (CircleImageView) itemView.findViewById(R.id.icon_avata);
            txtName = (TextView) itemView.findViewById(R.id.txtName);
            txtTime = (TextView) itemView.findViewById(R.id.txtTime);
            txtMessage = (TextView) itemView.findViewById(R.id.txtMessage);
        }
    }
}
