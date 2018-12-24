package com.example.admin.keyproirityapp.conferenceCall;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import com.example.admin.keyproirityapp.database.StaticConfig;
import com.example.admin.keyproirityapp.videocall.CallNotification;
import com.example.admin.keyproirityapp.videocall.SessionStatus;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.opentok.android.Session;

/**
 * Created by DELL on 11/20/2018.
 */

/**
 * Created by Anil on 12/3/2018.
 */

public class UpadateCallNotification {

    private static final String tokenStatus = "TokenStatus";
    Context context;
    CallNotification notification;
    DatabaseReference notificationRef;
    DatabaseReference updateTokenStatus;
    private Session mSession;

    public UpadateCallNotification(Context context, CallNotification notification, Session session) {
        this.context = context;
        this.notification = notification;
        this.mSession = session;
    }

    public void sessionStatusToFB(final CallNotification notification) {
        SessionStatus status = new SessionStatus();
        status.setNotificationId(notification.receiverId);
        status.setSessionActive(true);
        status.setSessionId(notification.sessionId);
        status.setSessionData(mSession.getConnection().getData());
        status.setTokenId(notification.token);
        status.setConnectionTime(String.valueOf(mSession.getConnection().getCreationTime()));
        FirebaseDatabase.getInstance().getReference().child("TokenStatus/" + notification.receiverId).setValue(status).
                addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            sendNotification(notification);
                        }
                    }
                });
    }

    public void sendNotification(final CallNotification notification) {
        DatabaseReference callNotification = FirebaseDatabase.getInstance().getReference(StaticConfig.CALL_NOTIFICATION).child(notification.receiverId).push();
        callNotification.setValue(notification).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Log.i("UpdateCall", "Notification sent");
            }
        });

    }

    private void changeSessionStatus(String notificationId) {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child(tokenStatus + "/" + notificationId);

        String pushId = reference.getKey();
        FirebaseDatabase.getInstance().getReference().child(tokenStatus + "/" + notificationId + "/" + "sessionActive").setValue(false).
                addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.i("UP date", "Sucessfully change the status");
                        }
                    }
                });
    }


}
