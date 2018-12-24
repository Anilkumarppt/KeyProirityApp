package com.example.admin.keyproirityapp.conferenceCall;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.admin.keyproirityapp.R;
import com.example.admin.keyproirityapp.database.StaticConfig;
import com.example.admin.keyproirityapp.videocall.CallNotification;
import com.example.admin.keyproirityapp.videocall.SessionStatus;
import com.example.admin.keyproirityapp.widgets.ConstraintSetHelper;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.opentok.android.BaseVideoRenderer;
import com.opentok.android.Connection;
import com.opentok.android.OpentokError;
import com.opentok.android.Publisher;
import com.opentok.android.PublisherKit;
import com.opentok.android.Session;
import com.opentok.android.Stream;
import com.opentok.android.Subscriber;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;

/**
 * Created by Anil on 12/3/2018.
 */

public class Multiconstraint extends AppCompatActivity implements EasyPermissions.PermissionCallbacks,
        Publisher.PublisherListener,
        Session.SessionListener, Session.SignalListener {
    private static final String TAG = Multiconstraint.class.getSimpleName();
    private static final int RC_SETTINGS_SCREEN_PERM = 123;
    private static final int RC_VIDEO_APP_PERM = 124;
    private static final String tokenStatus = "TokenStatus";
    String receiverId, receivername;
    String senderId;
    private Session mSession;
    private Publisher mPublisher;
    private String API_KEY, SESSION_ID, TOKEN;
    private ArrayList<Subscriber> mSubscribers = new ArrayList<Subscriber>();
    private HashMap<Stream, Subscriber> mSubscriberStreams = new HashMap<Stream, Subscriber>();
    private String friendAvatar;
    private String senderAvata, senderName;
    private ConstraintLayout mContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multiconstraint);
        mContainer = (ConstraintLayout) findViewById(R.id.main_container);
        if (getIntent().getExtras() != null) {
            receiverId = getIntent().getExtras().getString("friendId");
            senderId = getIntent().getExtras().getString(StaticConfig.INTENT_KEY_CHAT_SENDER_ID);
            receivername = getIntent().getExtras().getString("friendName");
            friendAvatar = getIntent().getExtras().getString("friendAvatar");
        }

        requestPermissions();
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume");

        super.onResume();

        if (mSession == null) {
            return;
        }
        mSession.onResume();
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause");

        super.onPause();

        if (mSession == null) {
            return;
        }
        mSession.onPause();

        if (isFinishing()) {
            disconnectSession();
        }
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy");
        disconnectSession();
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {
        Log.d(TAG, "onPermissionsGranted:" + requestCode + ":" + perms.size());
    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {
        Log.d(TAG, "onPermissionsDenied:" + requestCode + ":" + perms.size());

        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            new AppSettingsDialog.Builder(this)
                    .setTitle(getString(R.string.title_settings_dialog))
                    .setRationale(getString(R.string.rationale_ask_again))
                    .setPositiveButton(getString(R.string.setting))
                    .setNegativeButton(getString(R.string.cancel))
                    .setRequestCode(RC_SETTINGS_SCREEN_PERM)
                    .build()
                    .show();
        }
    }

    private void startPublisherPreview() {
        mPublisher = new Publisher.Builder(Multiconstraint.this).name("Publisher").build();
        mPublisher.setPublisherListener(this);
        mPublisher.setPublishAudio(false);
        mPublisher.setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE, BaseVideoRenderer.STYLE_VIDEO_FILL);
        mPublisher.startPreview();
    }

    @AfterPermissionGranted(RC_VIDEO_APP_PERM)
    private void requestPermissions() {
        String[] perms = {
                Manifest.permission.INTERNET,
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
        };
        if (EasyPermissions.hasPermissions(this, perms)) {
            connectToSession();
            startPublisherPreview();
            mPublisher.getView().setId(R.id.publisher_view_id);
            mContainer.addView(mPublisher.getView());
            calculateLayout();
        } else {
            EasyPermissions.requestPermissions(this, getString(R.string.rationale_video_app), RC_VIDEO_APP_PERM, perms);
        }
    }

    private void connectToSession() {
        RequestQueue reqQueue = Volley.newRequestQueue(this);
        reqQueue.add(new JsonObjectRequest(Request.Method.GET,
                "https://demovideocall.herokuapp.com" + "/session",
                null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    API_KEY = response.getString("apiKey");
                    SESSION_ID = response.getString("sessionId");
                    TOKEN = response.getString("token");
                    mSession = new Session.Builder(getApplicationContext(), API_KEY, SESSION_ID).sessionOptions(new Session.SessionOptions() {
                        @Override
                        public boolean useTextureViews() {
                            return true;
                        }
                    }).build();
                    mSession.setSessionListener(Multiconstraint.this);
                    mSession.setSignalListener(Multiconstraint.this);
                    mSession.connect(TOKEN);
                    Log.i(TAG, "API_KEY: " + API_KEY);
                    Log.i(TAG, "SESSION_ID: " + SESSION_ID);
                    Log.i(TAG, "TOKEN: " + TOKEN);
                    CallNotification notification = new CallNotification();
                    notification.api = API_KEY;
                    notification.sessionId = SESSION_ID;
                    notification.token = TOKEN;
                    notification.senderId = StaticConfig.UID;
                    notification.receiverId = receiverId;
                    notification.receiverName = receivername;
                    notification.senderAvata = senderAvata;
                    notification.callieName = senderName;
                    sessionStatusToFB(notification);

                } catch (JSONException error) {
                    Log.e(TAG, "Web Service error: " + error.getMessage());
                    finish();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, "Web Service error: " + error.getMessage());
            }
        }));

    }

    private void sessionStatusToFB(final CallNotification notification) {
        SessionStatus status = new SessionStatus();
        status.setNotificationId(notification.receiverId);
        status.setSessionActive(true);
        status.setSessionId(notification.sessionId);
        status.setTokenId(notification.token);
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

    private void sendNotification(final CallNotification notification) {
        DatabaseReference callNotification = FirebaseDatabase.getInstance().getReference(StaticConfig.CALL_NOTIFICATION).child(notification.receiverId).push();
        callNotification.setValue(notification).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Log.i(TAG, "Notification sent");
            }
        });

    }

    @Override
    public void onConnected(Session session) {
        Log.d(TAG, "onConnected: Connected to session " + session.getSessionId());
        session.sendSignal("Mutlticall", "hai hero");
        mSession.publish(mPublisher);
    }

    @Override
    public void onDisconnected(Session session) {
        Log.d(TAG, "onDisconnected: disconnected from session " + session.getSessionId());
        mSession = null;
    }

    @Override
    public void onError(Session session, OpentokError opentokError) {
        Log.d(TAG, "onError: Error (" + opentokError.getMessage() + ") in session " + session.getSessionId());

        Toast.makeText(this, "Session error. See the logcat please.", Toast.LENGTH_LONG).show();
        finish();
    }

    private int getResIdForSubscriberIndex(int index) {
        TypedArray arr = getResources().obtainTypedArray(R.array.subscriber_view_ids);
        int subId = arr.getResourceId(index, 0);
        arr.recycle();
        return subId;
    }

    @Override
    public void onStreamReceived(Session session, Stream stream) {
        Log.d(TAG, "onStreamReceived: New stream " + stream.getStreamId() + " in session " + session.getSessionId());
        final Subscriber subscriber = new Subscriber.Builder(Multiconstraint.this, stream).build();
        mSession.subscribe(subscriber);
        mSubscribers.add(subscriber);
        mSubscriberStreams.put(stream, subscriber);
        int subId = getResIdForSubscriberIndex(mSubscribers.size() - 1);
        subscriber.getView().setId(subId);
        mContainer.addView(subscriber.getView());
        Toast.makeText(this, "Subscribers size" + mSubscribers.size(), Toast.LENGTH_SHORT).show();
        calculateLayout();
    }

    @Override
    public void onStreamDropped(Session session, Stream stream) {
        Log.d(TAG, "onStreamDropped: Stream " + stream.getStreamId() + " dropped from session " + session.getSessionId());

        Subscriber subscriber = mSubscriberStreams.get(stream);
        if (subscriber == null) {
            return;
        }

        mSubscribers.remove(subscriber);
        mSubscriberStreams.remove(stream);
        mContainer.removeView(subscriber.getView());
        Toast.makeText(this, "Subscribers size" + mSubscribers.size(), Toast.LENGTH_SHORT).show();
        // Recalculate view Ids
        for (int i = 0; i < mSubscribers.size(); i++) {
            mSubscribers.get(i).getView().setId(getResIdForSubscriberIndex(i));
        }
        calculateLayout();
    }

    @Override
    public void onStreamCreated(PublisherKit publisherKit, Stream stream) {
        Log.d(TAG, "onStreamCreated: Own stream " + stream.getStreamId() + " created");
    }

    @Override
    public void onStreamDestroyed(PublisherKit publisherKit, Stream stream) {
        publisherKit.destroy();
        Log.d(TAG, "onStreamDestroyed: Own stream " + stream.getStreamId() + " destroyed");
    }

    @Override
    public void onError(PublisherKit publisherKit, OpentokError opentokError) {
        Log.d(TAG, "onError: Error (" + opentokError.getMessage() + ") in publisher");

        Toast.makeText(this, "Session error. See the logcat please.", Toast.LENGTH_LONG).show();
        finish();
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private void calculateLayout() {

        ConstraintSetHelper set = new ConstraintSetHelper(R.id.main_container);

        int size = mSubscribers.size();
        if (size == 0) {
            // Publisher full screen
            set.layoutViewFullScreen(R.id.publisher_view_id);
        } else if (size == 1) {
            // Publisher
            // Subscriber
            set.layoutViewAboveView(R.id.publisher_view_id, getResIdForSubscriberIndex(0));
            set.layoutViewWithTopBound(R.id.publisher_view_id, R.id.main_container);
            set.layoutViewWithBottomBound(getResIdForSubscriberIndex(0), R.id.main_container);
            set.layoutViewAllContainerWide(R.id.publisher_view_id, R.id.main_container);
            set.layoutViewAllContainerWide(getResIdForSubscriberIndex(0), R.id.main_container);
        } else if (size > 1 && size % 2 == 0) {
            //  Publisher
            // Sub1 | Sub2
            // Sub3 | Sub4
            //    .....
            set.layoutViewWithTopBound(R.id.publisher_view_id, R.id.main_container);
            set.layoutViewAllContainerWide(R.id.publisher_view_id, R.id.main_container);

            for (int i = 0; i < size; i += 2) {
                if (i == 0) {
                    set.layoutViewAboveView(R.id.publisher_view_id, getResIdForSubscriberIndex(i));
                    set.layoutViewAboveView(R.id.publisher_view_id, getResIdForSubscriberIndex(i + 1));
                } else {
                    set.layoutViewAboveView(getResIdForSubscriberIndex(i - 2), getResIdForSubscriberIndex(i));
                    set.layoutViewAboveView(getResIdForSubscriberIndex(i - 1), getResIdForSubscriberIndex(i + 1));
                }

                set.layoutTwoViewsOccupyingAllRow(getResIdForSubscriberIndex(i), getResIdForSubscriberIndex(i + 1));
            }

            set.layoutViewWithBottomBound(getResIdForSubscriberIndex(size - 2), R.id.main_container);
            set.layoutViewWithBottomBound(getResIdForSubscriberIndex(size - 1), R.id.main_container);
        } else if (size > 1) {
            // Pub  | Sub1
            // Sub2 | Sub3
            // Sub3 | Sub4
            //    .....

            set.layoutViewWithTopBound(R.id.publisher_view_id, R.id.main_container);
            set.layoutViewWithTopBound(getResIdForSubscriberIndex(0), R.id.main_container);
            set.layoutTwoViewsOccupyingAllRow(R.id.publisher_view_id, getResIdForSubscriberIndex(0));

            for (int i = 1; i < size; i += 2) {
                if (i == 1) {
                    set.layoutViewAboveView(R.id.publisher_view_id, getResIdForSubscriberIndex(i));
                    set.layoutViewAboveView(getResIdForSubscriberIndex(0), getResIdForSubscriberIndex(i + 1));
                } else {
                    set.layoutViewAboveView(getResIdForSubscriberIndex(i - 2), getResIdForSubscriberIndex(i));
                    set.layoutViewAboveView(getResIdForSubscriberIndex(i - 1), getResIdForSubscriberIndex(i + 1));
                }
                set.layoutTwoViewsOccupyingAllRow(getResIdForSubscriberIndex(i), getResIdForSubscriberIndex(i + 1));
            }
            set.layoutViewWithBottomBound(getResIdForSubscriberIndex(size - 2), R.id.main_container);
            set.layoutViewWithBottomBound(getResIdForSubscriberIndex(size - 1), R.id.main_container);
        }
        set.applyToLayout(mContainer, true);
    }

    private void disconnectSession() {
        if (mSession == null) {
            return;
        }

        if (mSubscribers.size() > 0) {
            for (Subscriber subscriber : mSubscribers) {
                if (subscriber != null) {
                    mSession.unsubscribe(subscriber);
                    subscriber.destroy();
                }
            }
        }

        if (mPublisher != null) {
            mSession.unpublish(mPublisher);
            mContainer.removeView(mPublisher.getView());
            mPublisher.destroy();
            mPublisher = null;
        }
        mSession.disconnect();
    }

    public void endCall(View view) {
        if (mSession != null) {
            if (mPublisher != null) {
                mSession = mPublisher.getSession();
                Log.i(TAG, mPublisher.getSession().getConnection().getData());
                mSession.unpublish(mPublisher);
                mContainer.removeView(mPublisher.getView());
                mPublisher = null;
                mSession.disconnect();
                this.finish();
            }
        }
    }

    @Override
    public void onSignalReceived(Session session, String type, String data, Connection connection) {
        String myConnectionId = session.getConnection().getConnectionId();
        if (connection != null && connection.getConnectionId().equals(myConnectionId)) {
            Toast.makeText(this, "Signal Received with data " + data, Toast.LENGTH_SHORT).show();
            // Signal received from another client
        }
    }

    public void callEnd(View view) {
        changeSessionStatus(receiverId);
        //  mSession = null;
        disconnectSession();
        this.finish();

    }

    private void changeSessionStatus(String notificationId) {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child(tokenStatus + "/" + notificationId);

        String pushId = reference.getKey();
        Log.i(TAG, "Push Id" + pushId);
        FirebaseDatabase.getInstance().getReference().child(tokenStatus + "/" + notificationId + "/" + "sessionActive").setValue(false).
                addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.i(TAG, "Sucessfully change the status");
                        }
                    }
                });
    }

}


