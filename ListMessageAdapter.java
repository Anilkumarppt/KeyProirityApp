package com.example.admin.keyproirityapp.adapter;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.admin.keyproirityapp.LastSeenStatus;
import com.example.admin.keyproirityapp.R;
import com.example.admin.keyproirityapp.database.StaticConfig;
import com.example.admin.keyproirityapp.model.Conversation;
import com.example.admin.keyproirityapp.model.Message;
import com.example.admin.keyproirityapp.ui.ChatActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.List;

import static android.view.View.VISIBLE;

public class ListMessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    public List<Conversation> messageList;
    private Context context;
    private Conversation conversation;
    private HashMap<String, String> bitmapAvata;
    private HashMap<String, DatabaseReference> bitmapAvataDB;
    private String bitmapAvataUser;

    public ListMessageAdapter(Context context, Conversation conversation, HashMap<String, String> bitmapAvata, String bitmapAvataUser) {
        this.context = context;
        this.conversation = conversation;
        this.bitmapAvata = bitmapAvata;
        this.bitmapAvataUser = bitmapAvataUser;
        bitmapAvataDB = new HashMap<>();
    }

    public static void loadImage(Context context, String imageUrl, ImageView imageView) {
        Glide.with(context)
                .load(imageUrl)
//                .thumbnail(thumbnailRequest)
                //.dontAnimate()
//                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(imageView);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == ChatActivity.VIEW_TYPE_FRIEND_MESSAGE) {
            View view = LayoutInflater.from(context).inflate(R.layout.rc_item_message_friend, parent, false);
            return new ItemMessageFriendHolder(view);
        } else if (viewType == ChatActivity.VIEW_TYPE_USER_MESSAGE) {
            View view = LayoutInflater.from(context).inflate(R.layout.rc_item_message_user, parent, false);
            return new ItemMessageUserHolder(view);
        }
        return null;
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {
        String contentType = conversation.getListMessageData().get(position).getContentType();
        final long time = conversation.getListMessageData().get(position).getTimestamp();
        LastSeenStatus getTime = new LastSeenStatus();
        long last_seen = time;
        //String lastSeendisplay = getTime.getTimeAgo(last_seen, context).toString();
        if (contentType.equals("text")) {
            if (holder instanceof ItemMessageFriendHolder) {
                String msg = conversation.getListMessageData().get(position).text;
                //  ((ItemMessageFriendHolder) holder).txtTimeFriend.setText(lastSeendisplay);
                ((ItemMessageFriendHolder) holder).txtContent.setText(msg + "," + position);
                String currentAvata = bitmapAvata.get(conversation.getListMessageData().get(position).idSender);
                if (currentAvata != null) {
                    loadImage(context, currentAvata, ((ItemMessageFriendHolder) holder).avata);
                } else {
                    loadImage(context, "", ((ItemMessageFriendHolder) holder).avata);
                    final String id = conversation.getListMessageData().get(position).idSender;
                    if (bitmapAvataDB.get(id) == null) {
                        bitmapAvataDB.put(id, FirebaseDatabase.getInstance().getReference().child("user/" + id + "/avata"));
                        bitmapAvataDB.get(id).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                if (dataSnapshot.getValue() != null) {
                                    String avataStr = (String) dataSnapshot.getValue();
                                    if (!avataStr.equals(StaticConfig.STR_DEFAULT_BASE64)) {
                                        ChatActivity.bitmapAvataFriend.put(id, avataStr);
                                    } else {
                                        ChatActivity.bitmapAvataFriend.put(id, "");
                                    }
                                    notifyDataSetChanged();
                                }
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        });
                    }
                }
            } else if (holder instanceof ItemMessageUserHolder) {
                Log.v("msg", conversation.getListMessageData().get(position).text);
                ((ItemMessageUserHolder) holder).txtContent.setText(conversation.getListMessageData().get(position).text + "," + position);
                //((ItemMessageUserHolder) holder).txtTimeuser.setText(lastSeendisplay);

                loadImage(context, bitmapAvataUser, ((ItemMessageUserHolder) holder).avata);
            }

        } else {
            /*image message*/
            final String imageUrl = conversation.getListMessageData().get(position).text;
            final long timeImage = conversation.getListMessageData().get(position).getTimestamp();
            if (holder instanceof ItemMessageFriendHolder) {
                ((ItemMessageFriendHolder) holder).txtContent.setVisibility(View.INVISIBLE);
                ((ItemMessageFriendHolder) holder).txtContent.setPadding(0, 0, 0, 0);
                /*    ((ItemMessageFriendHolder) holder).txtTimeFriend.setGravity(Gravity.CENTER);
                 ((ItemMessageFriendHolder) holder).txtTimeFriend.setText(lastSeendisplay);
*/
                ((ItemMessageFriendHolder) holder).imageMessageFrnd.setVisibility(VISIBLE);
                ((ItemMessageFriendHolder) holder).txtContent.setVisibility(View.INVISIBLE);
                ((ItemMessageFriendHolder) holder).txtContent.setPadding(0, 0, 0, 0);
                ((ItemMessageFriendHolder) holder).imageMessageFrnd.setVisibility(View.VISIBLE);
                StorageReference reference = FirebaseStorage.getInstance()
                        .getReference().child(imageUrl);
                reference.getDownloadUrl().addOnCompleteListener(new OnCompleteListener<Uri>() {
                    @Override
                    public void onComplete(@NonNull Task<Uri> task) {
                        if (task.isSuccessful()) {
                            loadImage(context, imageUrl, ((ItemMessageFriendHolder) holder).imageMessageFrnd);
                        }
                    }
                });

                String currentAvata = bitmapAvata.get(conversation.getListMessageData().get(position).idSender);
                if (currentAvata != null) {
                    loadImage(context, currentAvata, ((ItemMessageFriendHolder) holder).avata);
                } else {
                    loadImage(context, "", ((ItemMessageFriendHolder) holder).avata);
                    final String id = conversation.getListMessageData().get(position).idSender;
                    if (bitmapAvataDB.get(id) == null) {
                        bitmapAvataDB.put(id, FirebaseDatabase.getInstance().getReference().child("user/" + id + "/avata"));
                        bitmapAvataDB.get(id).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                if (dataSnapshot.getValue() != null) {
                                    String avataStr = (String) dataSnapshot.getValue();
                                    ChatActivity.bitmapAvataFriend.put(id, avataStr);
                                    notifyDataSetChanged();
                                }
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        });
                    }
                }
            } else if (holder instanceof ItemMessageUserHolder) {
                ((ItemMessageUserHolder) holder).txtContent.setVisibility(View.INVISIBLE);
                ((ItemMessageUserHolder) holder).txtContent.setPadding(0, 0, 0, 0);
                ((ItemMessageUserHolder) holder).imageMessage.setVisibility(VISIBLE);
                StorageReference reference = FirebaseStorage.getInstance()
                        .getReference().child(imageUrl);
                loadImage(context, imageUrl, ((ItemMessageUserHolder) holder).imageMessage);
            }

        }

    }

    @Override
    public int getItemViewType(int position) {
        return conversation.getListMessageData().get(position).idSender.equals(StaticConfig.UID) ? ChatActivity.VIEW_TYPE_USER_MESSAGE : ChatActivity.VIEW_TYPE_FRIEND_MESSAGE;
    }

    @Override
    public int getItemCount() {
        return conversation.getListMessageData().size();
    }

    public void addAll(List<Message> newUsers) {
        int initialSize = conversation.getListMessageData().size();
        for (Message message : newUsers) {
            conversation.getListMessageData().add(message);
        }

        notifyItemRangeInserted(initialSize, newUsers.size());
    }

    public String getLastItemId() {
        String idRoom, senderId, reciverId;
        // senderId=conversation.getListMessageData().get()
        int size = conversation.getListMessageData().size();
        return String.valueOf(size - 1);
        //idRoom = id.compareTo(StaticConfig.UID) > 0 ? (StaticConfig.UID + id).hashCode() + "" : "" + (id + StaticConfig.UID).hashCode();

    }

}

class ItemMessageUserHolder extends RecyclerView.ViewHolder {
    public TextView txtContent, txtTimeuser;
    public ImageView avata;
    public ImageView imageMessage;


    public ItemMessageUserHolder(View itemView) {
        super(itemView);
        txtContent = itemView.findViewById(R.id.textContentUser);
        avata = itemView.findViewById(R.id.imageView2);
        txtTimeuser = itemView.findViewById(R.id.timeuser);
        imageMessage = itemView.findViewById(R.id.imageMessageUser);
    }
}

class ItemMessageFriendHolder extends RecyclerView.ViewHolder {
    public TextView txtContent, txtTimeFriend;
    public ImageView avata;
    public ImageView imageMessageFrnd;

    //
    public ItemMessageFriendHolder(View itemView) {
        super(itemView);
        txtContent = (TextView) itemView.findViewById(R.id.textContentFriend);
        txtTimeFriend = itemView.findViewById(R.id.timefriend);
        avata = itemView.findViewById(R.id.imageView3);
        imageMessageFrnd = itemView.findViewById(R.id.imageMessage);
    }
}
