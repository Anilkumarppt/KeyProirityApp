package com.example.admin.keyproirityapp;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.admin.keyproirityapp.adapter.GroupMembersAdapter;
import com.example.admin.keyproirityapp.model.ListFriend;
import com.example.admin.keyproirityapp.model.RoomModel;
import com.example.admin.keyproirityapp.model.User;
import com.example.admin.keyproirityapp.util.ImageUtils;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.StorageReference;
import com.yarolegovich.lovelydialog.LovelyInfoDialog;
import com.yarolegovich.lovelydialog.LovelyProgressDialog;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class GroupInfo extends AppCompatActivity {
    private static final int PICK_IMAGE = 1994;
    CircleImageView groupIcon;
    TextView txtGroupname;
    RecyclerView membersList;
    FirebaseDatabase firebaseDatabase;
    DatabaseReference groupReference;
    String mGroupId, mGroupName;
    List<User> list = new ArrayList<>();
    TextView groupName;
    Toolbar toolbar;
    Context context = this;
    DatabaseReference groupDB;
    HashMap<String, String> tokenHashmap;
    private ListFriend dataListFriend = null;
    private ListFriend listFriend;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private GroupMembersAdapter membersAdapter;
    private ArrayList<String> listFriendID = null;
    private LovelyProgressDialog waitingDialog;
    private StorageReference mStorage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_info);
        Intent intent = getIntent();
        toolbar = findViewById(R.id.groupinfo_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("GroupInformation");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        membersAdapter = new GroupMembersAdapter(list, GroupInfo.this);
        groupName = findViewById(R.id.txt_groupname);
        mGroupId = intent.getStringExtra("GroupId");
        mGroupName = intent.getStringExtra("GroupName");
        groupName.setText(mGroupName);
        groupDB = FirebaseDatabase.getInstance().getReference().child("userGroup" + "/" + mGroupId);
        groupIcon = findViewById(R.id.groupicon);
        setgorupImage();
        tokenHashmap = new HashMap<>();
        txtGroupname = findViewById(R.id.txt_groupname);
        membersList = findViewById(R.id.members_recycler_view);
        membersList.setHasFixedSize(true);
        membersList.setLayoutManager(new LinearLayoutManager(this));
        membersList.setAdapter(membersAdapter);
        membersAdapter.notifyDataSetChanged();
        if (listFriendID == null) {
            listFriendID = new ArrayList<>();
            fetchmembersList();
        }
        waitingDialog = new LovelyProgressDialog(this);

    }

    private void setgorupImage() {
        groupDB.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                if (dataSnapshot != null) {

                    HashMap mapGroup = (HashMap) dataSnapshot.getValue();
                    RoomModel model = dataSnapshot.getValue(RoomModel.class);

                    //  ArrayList<String> member = (ArrayList<String>) mapGroup.get("member");

                    HashMap mapGroupInfo = (HashMap) mapGroup.get("groupInfo");
                    String name = (String) mapGroupInfo.get("name");
                    String avatar = model.groupInfo.avtar;
                    //String avatar = (String) mapGroupInfo.get("groupIcon");
                    setImageAvatar(getApplicationContext(), avatar);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }

    private void setImageAvatar(Context context, String imgBase64) {
        try {
            Resources res = getResources();

            Bitmap src;
            if (imgBase64.equals("default")) {
                src = BitmapFactory.decodeResource(res, R.drawable.default_avata);
            } else {
                byte[] decodedString = Base64.decode(imgBase64, Base64.DEFAULT);
                src = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
            }

            groupIcon.setImageDrawable(ImageUtils.roundedImage(context, src));
        } catch (Exception e) {
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        //setgorupImage();

    }

    private void fetchmembersList() {
        firebaseDatabase = FirebaseDatabase.getInstance();
        groupReference = firebaseDatabase.getReference().child("group");
        groupDB.keepSynced(true);
        final ArrayList<String> members = new ArrayList<>();

        groupDB.child("groupMembers").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot != null) {
                    for (DataSnapshot ds : dataSnapshot.getChildren()) {
                        RoomModel.GroupMember groupMember = ds.getValue(RoomModel.GroupMember.class);
                        Log.d("Member", groupMember.id);
                        members.add(groupMember.id);
                    }
                    Iterator listKey = members.iterator();
                    while (listKey.hasNext()) {
                        listFriendID.add(listKey.next().toString());
                    }
                    getAllFriendInfo(0);
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void getAllFriendInfo(final int i) {
        if (i == listFriendID.size()) {
            membersAdapter.notifyDataSetChanged();

        } else {
            final String id = listFriendID.get(i);
            FirebaseDatabase.getInstance().getReference().child("user" + "/" + id).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.getValue() != null) {
                        User user = new User();
                        HashMap mapUserInfo = (HashMap) dataSnapshot.getValue();
                        user.name = (String) mapUserInfo.get("name");
                        user.avata = (String) mapUserInfo.get("avata");
                        user.email = (String) mapUserInfo.get("email");
                        list.add(user);
                    }
                    getAllFriendInfo(i + 1);
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });


        }


    }


    public void setGroupIcon(View view) {
        new AlertDialog.Builder(this)
                .setTitle("Avatar")
                .setMessage("Are you sure want to change avatar profile?")
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Intent intent = new Intent();
                        intent.setType("image/*");
                        intent.setAction(Intent.ACTION_PICK);
                        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE);
                        dialogInterface.dismiss();
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                }).show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE && resultCode == Activity.RESULT_OK) {
            if (data == null) {
                Toast.makeText(context, "", Toast.LENGTH_LONG).show();
                return;
            }
            try {
                InputStream inputStream = this.getContentResolver().openInputStream(data.getData());

                Bitmap imgBitmap = BitmapFactory.decodeStream(inputStream);
                imgBitmap = ImageUtils.cropToSquare(imgBitmap);
                InputStream is = ImageUtils.convertBitmapToInputStream(imgBitmap);
                final Bitmap liteImage = ImageUtils.makeImageLite(is,
                        imgBitmap.getWidth(), imgBitmap.getHeight(),
                        ImageUtils.AVATAR_WIDTH, ImageUtils.AVATAR_HEIGHT);

                String imageBase64 = ImageUtils.encodeBase64(liteImage);

                waitingDialog.setCancelable(false)
                        .setTitle("ProfilePhoto updating....")
                        .setTopColorRes(R.color.colorPrimary)
                        .show();

                groupDB.child("groupInfo" + "/" + "groupIcon").setValue(imageBase64)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {

                                    waitingDialog.dismiss();
                                    groupIcon.setImageDrawable(ImageUtils.roundedImage(context, liteImage));
                                    new LovelyInfoDialog(context)
                                            .setTopColorRes(R.color.colorPrimary)
                                            .setTitle("Success")
                                            .setMessage("Update Profile Photo successfully!")
                                            .show();
                                }
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                waitingDialog.dismiss();
                                Log.d("Update Avatar", "failed");
                                new LovelyInfoDialog(context)
                                        .setTopColorRes(R.color.colorAccent)
                                        .setTitle("False")
                                        .setMessage("False to update avatar")
                                        .show();
                            }
                        });
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }


}
