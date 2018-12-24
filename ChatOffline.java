package com.example.admin.keyproirityapp;

import android.app.Application;
import android.content.Context;
import android.support.multidex.MultiDex;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.squareup.picasso.OkHttpDownloader;
import com.squareup.picasso.Picasso;

public class ChatOffline extends Application {
    private DatabaseReference UserDatabaseReference;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        //Load the all the Users details
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);
        //load picture Picaso
        Picasso.Builder builder = new Picasso.Builder(this);
        builder.downloader(new OkHttpDownloader(this, Integer.MAX_VALUE));
        Picasso build = builder.build();
        build.setIndicatorsEnabled(true);
        build.setLoggingEnabled(true);
        Picasso.setSingletonInstance(build);
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        /*if(currentUser!=null){
            String  OnlineUserid=mAuth.getCurrentUser().getUid();
            UserDatabaseReference=FirebaseDatabase.getInstance().getReference().child("user")
                    .child(OnlineUserid);
            UserDatabaseReference.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    UserDatabaseReference.child("status").child("isOnline").onDisconnect().setValue(ServerValue.TIMESTAMP);
                    UserDatabaseReference.child("status").child("isOnline").setValue(true);
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }*/
    }

}
