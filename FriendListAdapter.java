package com.example.admin.keyproirityapp.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.example.admin.keyproirityapp.R;
import com.example.admin.keyproirityapp.model.ListFriend;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Created by DELL on 11/22/2018.
 */

public class FriendListAdapter extends RecyclerView.Adapter<FriendListAdapter.FriendHolder> {

    public ListFriend listFriend = new ListFriend();
    public Context context;
    public CallbackInterface mCallBack;
    View view;
    private String TAG = FriendListAdapter.class.getSimpleName();
//

    public FriendListAdapter(Context context, ListFriend listFriend) {
        this.context = context;
        this.listFriend = listFriend;
        try {
            mCallBack = (CallbackInterface) context;
        } catch (ClassCastException e) {
            Log.e(TAG, "Must Implement the call back interface  " + e.getMessage());
        }
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @NonNull
    @Override
    public FriendHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        view = LayoutInflater.from(parent.getContext()).inflate(R.layout.rc_item_add_friend_call, parent, false);
        Log.i(TAG, "oncreateView Holder");
        return new FriendHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final FriendListAdapter.FriendHolder holder, final int position) {
        final String friendId = listFriend.getListFriend().get(position).id;
        final String friendName = listFriend.getListFriend().get(position).name;
        final String friendAvata = listFriend.getListFriend().get(position).avata;

        holder.txtName.setText(listFriend.getListFriend().get(position).name);
        holder.txtEmail.setText(listFriend.getListFriend().get(position).email);

        holder.addTocall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mCallBack != null) {
                    mCallBack.onHandleSelection(position, friendId, friendName, friendAvata);

                }
                Log.i(TAG, listFriend.getListFriend().get(position).id + " " + listFriend.getListFriend().get(position).name);
            }
        });
    }

    @Override
    public int getItemCount() {
        return listFriend.getListFriend().size();
    }

    public interface CallbackInterface {

        /**
         * Callback invoked when clicked
         *
         * @param position - the position
         * @param id       - the text to pass back
         */
        void onHandleSelection(int position, String id, String friendName, String friendAvata);
    }

    public class FriendHolder extends RecyclerView.ViewHolder {
        public TextView txtName, txtEmail;
        public CircleImageView avata;
        public ImageButton addTocall;


        public FriendHolder(View itemView) {
            super(itemView);
            txtName = (TextView) itemView.findViewById(R.id.txtName);
            txtEmail = (TextView) itemView.findViewById(R.id.txtEmail);
            avata = (CircleImageView) itemView.findViewById(R.id.icon_avata);
            addTocall = (ImageButton) itemView.findViewById(R.id.addto_call);
        }

    }

}
