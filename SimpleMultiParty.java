package com.example.admin.keyproirityapp.conferenceCall;

import android.Manifest;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.example.admin.keyproirityapp.R;
import com.example.admin.keyproirityapp.database.StaticConfig;
import com.example.admin.keyproirityapp.network.APIClient;
import com.example.admin.keyproirityapp.network.ApiInterface;
import com.example.admin.keyproirityapp.videocall.CallNotification;
import com.example.admin.keyproirityapp.videocall.MyResponse;
import com.example.admin.keyproirityapp.videocall.SessionStatus;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by Anil on 12/3/2018.
 */

public class SimpleMultiParty extends AppCompatActivity implements Session.SessionListener, PublisherKit.PublisherListener,
        EasyPermissions.PermissionCallbacks,
        Session.SignalListener,
        PublisherKit.VideoStatsListener {
    private static final int RC_SETTINGS_SCREEN_PERM = 123;
    private static final int RC_VIDEO_APP_PERM = 124;
    private final int MAX_NUM_SUBSCRIBERS = 4;
    private RelativeLayout mPublisherViewContainer;
    private Session mSession;
    private Publisher mPublisher;

    private String TAG = SimpleMultiParty.class.getSimpleName();
    private ArrayList<Subscriber> mSubscribers = new ArrayList<Subscriber>();
    private HashMap<Stream, Subscriber> mSubscriberStreams = new HashMap<Stream, Subscriber>();

    private String receiverId;
    private String senderId;
    private String senderName;
    private String receivername;
    private String senderAvata = "default";
    private String tokenStatus = "TokenStatus";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple_multi_party);
        mPublisherViewContainer = (RelativeLayout) findViewById(R.id.publisherview);

        final Button swapCamera = (Button) findViewById(R.id.swapCamera);
        swapCamera.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (mPublisher == null) {
                    return;
                }
                mPublisher.cycleCamera();
            }
        });

        final ToggleButton toggleAudio = (ToggleButton) findViewById(R.id.toggleAudio);
        toggleAudio.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (mPublisher == null) {
                    return;
                }
                if (isChecked) {
                    mPublisher.setPublishAudio(true);
                } else {
                    mPublisher.setPublishAudio(false);
                }
            }
        });

        final ToggleButton toggleVideo = (ToggleButton) findViewById(R.id.toggleVideo);
        toggleVideo.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (mPublisher == null) {
                    return;
                }
                if (isChecked) {
                    mPublisher.setPublishVideo(true);
                } else {
                    mPublisher.setPublishVideo(false);
                }
            }
        });
        if (getIntent().getExtras() != null) {
            receiverId = getIntent().getExtras().getString("friendId");
            senderId = getIntent().getExtras().getString(StaticConfig.INTENT_KEY_CHAT_SENDER_ID);
            receivername = getIntent().getExtras().getString("friendName");
            requestPermissions();
        }

    }

    @Override
    protected void onStart() {
        Log.d(TAG, "onStart");

        super.onStart();
    }

    @Override
    protected void onRestart() {
        Log.d(TAG, "onRestart");

        super.onRestart();
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
    protected void onStop() {
        Log.d(TAG, "onPause");

        super.onStop();
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


    private void initOpentok(final String receiverId, final String receivername, final String senderId) {
        ApiInterface apiInterface = APIClient.getClient().create(ApiInterface.class);
        Call<MyResponse> call = apiInterface.getSession();
        call.enqueue(new Callback<MyResponse>() {
            @Override
            public void onResponse(Call<MyResponse> call, Response<MyResponse> response) {
                if (response.code() == 200) {
                    Log.i(TAG, response.body().toString());

                    sessionDetails(response.body(), receiverId, receivername, senderId);

                    //initSession(response.body());
                }
                Log.i(TAG, "ResponseCode " + response.code());
            }

            @Override
            public void onFailure(Call<MyResponse> call, Throwable t) {
                Log.i(TAG, "Error message" + t.getMessage());
                Toast.makeText(getApplicationContext(), "Session Creation Error", Toast.LENGTH_SHORT).show();
                finish();
            }
        });

    }

    private void sessionDetails(MyResponse body, String receiverId, String receivername, String senderId) {
        CallNotification notification = new CallNotification();
        notification.senderId = StaticConfig.UID;
        notification.receiverId = receiverId;
        notification.receiverName = receivername;
        notification.api = body.apiKey;
        notification.sessionId = body.sessionId;
        notification.token = body.token;
        notification.senderAvata = senderAvata;
        notification.callieName = senderName;
        //initialize session and connect to session
        //initSession(notification.api, notification.sessionId, notification.token);
        mSession = new Session.Builder(SimpleMultiParty.this, notification.api, notification.sessionId).build();
        mSession.setSessionListener(this);
        mSession.setSignalListener(this);
        mSession.connect(notification.token);
        sessionStatusToFB(notification);
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

    @AfterPermissionGranted(RC_VIDEO_APP_PERM)
    private void requestPermissions() {
        String[] perms = {
                Manifest.permission.INTERNET,
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
        };
        if (EasyPermissions.hasPermissions(this, perms)) {
            initOpentok(receiverId, receivername, senderId);
        } else {
            EasyPermissions.requestPermissions(this, getString(R.string.rationale_video_app), RC_VIDEO_APP_PERM, perms);
        }
    }

    @Override
    public void onConnected(Session session) {
        Log.d(TAG, "onConnected: Connected to session " + session.getSessionId());

        mPublisher = new Publisher.Builder(SimpleMultiParty.this).name("publisher").build();

        mPublisher.setPublisherListener(this);
        mPublisher.setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE, BaseVideoRenderer.STYLE_VIDEO_FILL);
        mPublisherViewContainer.addView(mPublisher.getView());
        mSession.publish(mPublisher);

    }

    @Override
    public void onDisconnected(Session session) {
        Log.d(TAG, "onDisconnected: disconnected from session " + session.getSessionId());

        mSession = null;

    }

    @Override
    public void onStreamReceived(Session session, Stream stream) {

        Log.d(TAG, "onStreamReceived: New stream " + stream.getStreamId() + " in session " + session.getSessionId());
        if (mSubscribers.size() + 1 > MAX_NUM_SUBSCRIBERS) {
            Toast.makeText(this, "New subscriber ignored. MAX_NUM_SUBSCRIBERS limit reached.", Toast.LENGTH_LONG).show();
            return;
        }
        final Subscriber subscriber = new Subscriber.Builder(SimpleMultiParty.this, stream).build();
        mSession.subscribe(subscriber);
        mSubscribers.add(subscriber);
        mSubscriberStreams.put(stream, subscriber);
        int position = mSubscribers.size() - 1;
        int id = getResources().getIdentifier("subscriberview" + (new Integer(position)).toString(), "id", SimpleMultiParty.this.getPackageName());
        RelativeLayout subscriberViewContainer = (RelativeLayout) findViewById(id);
        subscriber.setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE, BaseVideoRenderer.STYLE_VIDEO_FILL);
        subscriberViewContainer.addView(subscriber.getView());
        id = getResources().getIdentifier("toggleAudioSubscriber" + (new Integer(position)).toString(), "id", SimpleMultiParty.this.getPackageName());
        final ToggleButton toggleAudio = (ToggleButton) findViewById(id);
        toggleAudio.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    subscriber.setSubscribeToAudio(true);
                } else {
                    subscriber.setSubscribeToAudio(false);
                }
            }
        });
        toggleAudio.setVisibility(View.VISIBLE);

    }

    @Override
    public void onStreamDropped(Session session, Stream stream) {
        Log.d(TAG, "onStreamDropped: Stream " + stream.getStreamId() + " dropped from session " + session.getSessionId());
        Subscriber subscriber = mSubscriberStreams.get(stream);
        int position = mSubscribers.indexOf(subscriber);
        int id = getResources().getIdentifier("subscriberview" + (new Integer(position)).toString(), "id", SimpleMultiParty.this.getPackageName());

        mSubscribers.remove(subscriber);
        mSubscriberStreams.remove(stream);

        RelativeLayout subscriberViewContainer = (RelativeLayout) findViewById(id);
        subscriberViewContainer.removeView(subscriber.getView());

        id = getResources().getIdentifier("toggleAudioSubscriber" + (new Integer(position)).toString(), "id", SimpleMultiParty.this.getPackageName());
        final ToggleButton toggleAudio = (ToggleButton) findViewById(id);
        toggleAudio.setOnCheckedChangeListener(null);
        toggleAudio.setVisibility(View.INVISIBLE);

    }

    @Override
    public void onError(Session session, OpentokError opentokError) {

        Toast.makeText(this, "Session error. See the logcat please.", Toast.LENGTH_LONG).show();
        finish();

    }

    @Override
    public void onStreamCreated(PublisherKit publisherKit, Stream stream) {
        Log.d(TAG, "onStreamCreated: Own stream " + stream.getStreamId() + " created");

    }

    @Override
    public void onStreamDestroyed(PublisherKit publisherKit, Stream stream) {
        Log.d(TAG, "onStreamDestroyed: Own stream " + stream.getStreamId() + " destroyed");

    }

    @Override
    public void onError(PublisherKit publisherKit, OpentokError opentokError) {
        Log.d(TAG, "onError: Error (" + opentokError.getMessage() + ") in publisher");

        Toast.makeText(this, "Session error. See the logcat please.", Toast.LENGTH_LONG).show();
        finish();

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
            mPublisherViewContainer.removeView(mPublisher.getView());
            mSession.unpublish(mPublisher);
            mPublisher.destroy();
            mPublisher = null;
        }
        mSession.disconnect();
    }

    @Override
    public void onVideoStats(PublisherKit publisherKit, PublisherKit.PublisherVideoStats[] publisherVideoStats) {

    }

    @Override
    public void onSignalReceived(Session session, String s, String s1, Connection connection) {

    }

    public void endSession(View view) {
        changeSessionStatus(receiverId);
        //  mSession = null;
        disconnectSession();
        this.finish();

    }

    private void changeSessionStatus(String receiverId) {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child(tokenStatus + "/" + receiverId);

        String pushId = reference.getKey();
        Log.i(TAG, "Push Id" + pushId);
        FirebaseDatabase.getInstance().getReference().child(tokenStatus + "/" + receiverId + "/" + "sessionActive").setValue(false).
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
