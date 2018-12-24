package com.example.admin.keyproirityapp.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.widget.RecyclerView;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.admin.keyproirityapp.R;
import com.example.admin.keyproirityapp.database.StaticConfig;
import com.example.admin.keyproirityapp.model.User;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class GroupMembersAdapter extends RecyclerView.Adapter<GroupMembersAdapter.MemebersHolder> {
    View view;
    Context ctx;
    private List<User> listFriend;

    public GroupMembersAdapter(List<User> listFriend, Context ctx) {
        this.listFriend = listFriend;
        this.ctx = ctx;
    }

    //
    @Override
    public GroupMembersAdapter.MemebersHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        view = LayoutInflater.from(parent.getContext()).inflate(R.layout.allusersitem, parent, false);
        MemebersHolder holder = new MemebersHolder(view);

        return holder;
    }

    @Override
    public void onBindViewHolder(GroupMembersAdapter.MemebersHolder holder, int position) {

        String name = listFriend.get(position).name;
        holder.userName.setText(name);
        holder.userEmail.setText(listFriend.get(position).email);
        if (listFriend.get(position).avata.equals(StaticConfig.STR_DEFAULT_BASE64)) {
            holder.profileImage.setImageResource(R.drawable.default_avata);
        } else {
            byte[] decodedString = Base64.decode(listFriend.get(position).avata, Base64.DEFAULT);
            Bitmap src = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
            holder.profileImage.setImageBitmap(src);
        }
    }

    @Override
    public int getItemCount() {
        return listFriend.size();
    }

    public class MemebersHolder extends RecyclerView.ViewHolder {
        CircleImageView profileImage;
        TextView userName, userEmail;

        public MemebersHolder(View itemView) {
            super(itemView);
            profileImage = itemView.findViewById(R.id.profile_pic);
            userName = itemView.findViewById(R.id.txt_contactName);
            userEmail = itemView.findViewById(R.id.txt_contacStatus);
        }
    }
}
