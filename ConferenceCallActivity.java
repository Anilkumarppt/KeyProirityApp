package com.example.admin.keyproirityapp.conferenceCall;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.admin.keyproirityapp.R;
import com.example.admin.keyproirityapp.database.SharedPreferenceHelper;
import com.example.admin.keyproirityapp.database.StaticConfig;
import com.example.admin.keyproirityapp.model.User;
import com.example.admin.keyproirityapp.network.APIClient;
import com.example.admin.keyproirityapp.network.ApiInterface;
import com.example.admin.keyproirityapp.ui.FriendListView;
import com.example.admin.keyproirityapp.videocall.CallNotification;
import com.example.admin.keyproirityapp.videocall.MyResponse;
import com.google.android.gms.tasks.OnCompleteListener;
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
import com.opentok.android.SubscriberKit;

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

public class ConferenceCallActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks,
        Session.SessionListener,
        Session.SignalListener,
        Publisher.PublisherListener, SubscriberKit.VideoListener, Session.ConnectionListener, SubscriberKit.SubscriberListener {

    private static final String tokenStatus = "TokenStatus";
    private static final int RC_VIDEO_APP_PERM = 124;
    private static final int RC_SETTINGS_SCREEN_PERM = 123;
    private static String API_KEY;
    private static String SESSION_ID;
    private static String TOKEN;
    DatabaseReference callNotification;
    String receiverId;
    int widthView;
    RelativeLayout.LayoutParams view0Params, view1Params, view2Params, viewParams;
    LinearLayout callView;
    boolean isPick = false;
    private String TAG = ConferenceCallActivity.class.getSimpleName();
    private Session mSession;
    private Publisher mPublisher;
    private Subscriber mSubscriber;
    private String friendAvatar;
    private String senderAvata, senderName, senderId;
    private ArrayList<Subscriber> mSubscribers = new ArrayList<Subscriber>();
    private HashMap<Stream, Subscriber> mSubscriberStreams = new HashMap<Stream, Subscriber>();
    private RelativeLayout mPublisherViewContainer;
    private RelativeLayout subscriberview1, subscriberview2, subscriberview3;
    private int MAX_NUM_SUBSCRIBERS = 4;
    private String receivername;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conference_call);

        //callView=findViewById(R.id.callview);
        subscriberview1 = findViewById(R.id.subscriber_container1);
        subscriberview2 = findViewById(R.id.subscriber_container2);
        subscriberview2.setVisibility(View.VISIBLE);
        subscriberview1.setVisibility(View.VISIBLE);
        // subscriberview3=findViewById(R.id.sub3);
        mPublisherViewContainer = findViewById(R.id.publisher_container);

        if (getIntent().getExtras() != null) {
            String bundleType = getIntent().getExtras().getString("callType");
            if (bundleType.equals("OutGoing")) {
                receiverId = getIntent().getExtras().getString("friendId");
                senderId = getIntent().getExtras().getString(StaticConfig.INTENT_KEY_CHAT_SENDER_ID);
                receivername = getIntent().getExtras().getString("friendName");
                friendAvatar = getIntent().getExtras().getString("friendAvatar");
                requestPermissions();
                getUserDetails();
            } else {
                API_KEY = getIntent().getExtras().getString("api");
                SESSION_ID = getIntent().getExtras().getString("sessionId");
                TOKEN = getIntent().getExtras().getString("sessionToken");
                getUserDetails();
                mSession = new Session.Builder(ConferenceCallActivity.this, API_KEY, SESSION_ID).
                        sessionOptions(new Session.SessionOptions() {
                            @Override
                            public boolean useTextureViews() {
                                return true;
                            }
                        }).build();
                mSession.setSessionListener(ConferenceCallActivity.this);
                mSession.setSignalListener(ConferenceCallActivity.this);
                mSession.setConnectionListener(this);
                mSession.connect(TOKEN);
            }
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            friendAvatar = data.getExtras().getString("friendAvata");
            receiverId = data.getExtras().getString("friendId");
            receivername = data.getExtras().getString("friendName");
            Log.i(TAG, "FriendAvatar " + friendAvatar + "FriendName " + receivername);
            initNotificationObject();
        }
    }

    private void getUserDetails() {
        SharedPreferenceHelper preferenceHelper;
        preferenceHelper = SharedPreferenceHelper.getInstance(this);
        User userInfo = preferenceHelper.getUserInfo();
        senderAvata = userInfo.avata;
        senderName = userInfo.name;
        senderId = preferenceHelper.getUID();
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

    @AfterPermissionGranted(RC_VIDEO_APP_PERM)
    private void requestPermissions() {
        String[] perms = {
                Manifest.permission.INTERNET,
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
        };
        if (EasyPermissions.hasPermissions(this, perms)) {
            createSession();
            startPublisherPreview();
            mPublisherViewContainer.addView(mPublisher.getView());
            //mPublisher.getView().setId(R.id.publisher_view);
        } else {
            EasyPermissions.requestPermissions(this, getString(R.string.rationale_video_app), RC_VIDEO_APP_PERM, perms);
        }
    }

    private void startPublisherPreview() {
        mPublisher = new Publisher.Builder(ConferenceCallActivity.this).name("Anil").build();
        mPublisher.setPublisherListener(this);
        mPublisher.setPublishAudio(false);
        mPublisher.setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE, BaseVideoRenderer.STYLE_VIDEO_FILL);
        mPublisher.startPreview();
    }

    private void createSession() {
        ApiInterface apiInterface = APIClient.getClient().create(ApiInterface.class);
        Call<MyResponse> call = apiInterface.getSession();
        call.enqueue(new Callback<MyResponse>() {
            @Override
            public void onResponse(Call<MyResponse> call, Response<MyResponse> response) {
                if (response.code() == 200) {
                    Log.i(TAG, response.body().toString());
                    API_KEY = response.body().getApiKey();
                    SESSION_ID = response.body().getSessionId();
                    TOKEN = response.body().getToken();

                    initSession();
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

    private void initSession() {
        mSession = new Session.Builder(ConferenceCallActivity.this, API_KEY, SESSION_ID).
                sessionOptions(new Session.SessionOptions() {
                    @Override
                    public boolean useTextureViews() {
                        return true;
                    }
                }).build();
        mSession.setSessionListener(ConferenceCallActivity.this);
        mSession.setSignalListener(ConferenceCallActivity.this);
        mSession.setConnectionListener(this);
        mSession.connect(TOKEN);
        initNotificationObject();
        //initSession(response.body());
        //
    }

    private void initNotificationObject() {
        CallNotification notification = new CallNotification();
        notification.api = API_KEY;
        notification.sessionId = SESSION_ID;
        notification.token = TOKEN;
        notification.senderId = StaticConfig.UID;
        notification.receiverId = receiverId;
        notification.receiverName = receivername;
        notification.senderAvata = senderAvata;
        notification.callieName = senderName;
        UpadateCallNotification obj = new UpadateCallNotification(ConferenceCallActivity.this, notification, mSession);
        obj.sessionStatusToFB(notification);
    }

    private void startTimer() {

        final boolean stop = isPick;
        final Handler handler = new Handler();
        Thread task = new Thread();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {

            }

        };

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
        changeSessionStatus(receiverId);
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

    @Override
    public void onConnected(Session session) {
        Log.d(TAG, "onConnected: Connected to session " + session.getSessionId());
        mSession.publish(mPublisher);
        String sessionTime = String.valueOf(session.getConnection().getCreationTime());
        Log.d(TAG, "onConnected time " + sessionTime);
        Log.d(TAG, "onConnected: session Data" + session.getConnection().getData());
//        startTimer();
    }

    @Override
    public void onDisconnected(Session session) {
        Log.d(TAG, "onDisconnected: disconnected from session " + session.getSessionId());
        mSession = null;

    }

    @Override
    public void onStreamReceived(Session session, Stream stream) {
        isPick = true;
        Log.d(TAG, "onStreamReceived: New stream " + stream.getStreamId() + " in session " + session.getSessionId());
        Toast.makeText(this, "New Stream Received from " + stream.getName(), Toast.LENGTH_SHORT).show();
        Log.d(TAG, "Stream name " + stream.getName());
        if (mSubscribers.size() + 1 > MAX_NUM_SUBSCRIBERS) {
            Toast.makeText(this, "New subscriber ignored. MAX_NUM_SUBSCRIBERS limit reached.", Toast.LENGTH_LONG).show();
            return;
        }
        final Subscriber subscriber = new Subscriber.Builder(ConferenceCallActivity.this, stream).build();
        mSession.subscribe(subscriber);
        mSubscribers.add(subscriber);
        subscriber.setVideoListener(this);
        subscriber.setSubscriberListener(this);

        if (subscriber == null) {
            return;
        }

        mSubscriberStreams.put(stream, subscriber);
        subscriber.setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE, BaseVideoRenderer.STYLE_VIDEO_FILL);
        //callView.setVisibility(View.VISIBLE);
        int size = mSubscribers.size();
        switch (size) {
            case 1:
                subscriberview1.setVisibility(View.VISIBLE);
                subscriberview2.setVisibility(View.GONE);
                TextView status1 = findViewById(R.id.text_sub1);
                status1.setText("connecting to " + receivername);
                status1.setTextColor(getResources().getColor(R.color.blue));
                //subscriberview3.setVisibility(View.GONE);
                subscriberview1.addView(subscriber.getView());
                //mPublisherViewContainer.addView(mPublisher.getView());
                break;
            case 2:
                subscriberview1.setVisibility(View.VISIBLE);
                subscriberview2.setVisibility(View.VISIBLE);
                subscriberview2.addView(subscriber.getView());
                TextView status = findViewById(R.id.tv_sub2);
                status.setText("Connecting to " + receivername);
                //subscriberview3.setVisibility(View.GONE);
                break;
           /* case 3:
                subscriberview1.setVisibility(View.VISIBLE);
                subscriberview2.setVisibility(View.VISIBLE);
                subscriberview3.setVisibility(View.VISIBLE);
                break;*/
            default:
        }
    }

    @Override
    public void onStreamDropped(Session session, Stream stream) {
        Log.d(TAG, "onStreamDropped: Stream " + stream.getStreamId() + " dropped from session " + session.getSessionId());
        Toast.makeText(this, "Stream Dropped from " + stream.getName(), Toast.LENGTH_SHORT).show();
        Subscriber subscriber = mSubscriberStreams.get(stream);
        if (subscriber == null) {
            return;
        }

        int position = mSubscribers.indexOf(subscriber);
        int id = getResources().getIdentifier("sub" + (new Integer(position)).toString(), "id", ConferenceCallActivity.this.getPackageName());
        mSubscribers.remove(subscriber);
        mSubscriberStreams.remove(stream);
        RelativeLayout subscriberViewContainer = (RelativeLayout) findViewById(id);
        subscriberViewContainer.removeView(subscriber.getView());
    }

    @Override
    public void onError(Session session, OpentokError opentokError) {
        Log.d(TAG, "onError: Error (" + opentokError.getMessage() + ") in session " + session.getSessionId());
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

    @Override
    public void onSignalReceived(Session session, String s, String s1, Connection connection) {

    }

    public void hangup(View view) {
        disconnectSession();
        this.finish();
    }

    @Override
    public void onVideoDataReceived(SubscriberKit subscriberKit) {
        Toast.makeText(this, "Video data Received", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onVideoDisabled(SubscriberKit subscriberKit, String s) {
        Toast.makeText(this, "Video disabled", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onVideoEnabled(SubscriberKit subscriberKit, String s) {
        Toast.makeText(this, "Video enabled", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onVideoDisableWarning(SubscriberKit subscriberKit) {

    }

    @Override
    public void onVideoDisableWarningLifted(SubscriberKit subscriberKit) {

    }

    public void endCall(View view) {
//        changeSessionStatus(receiverId);
        //  mSession = null;
        disconnectSession();
        this.finish();
    }


    public void addFriendCall(View view) {

        Intent addFriend = new Intent(this, FriendListView.class);
        startActivityForResult(addFriend, 1);
        //startActivity(new Intent(this, FriendListView.class));
        int subcribers = mSubscriberStreams.size();
        if (subcribers == 1) {
            subscriberview2.setVisibility(View.VISIBLE);
            subscriberview1.requestLayout();
            RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, 400);
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
            subscriberview1.setLayoutParams(layoutParams);
            subscriberview2.requestLayout();
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, 400);
            params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            subscriberview2.setLayoutParams(params);
        }
    }

    public void switchCamera(View view) {
        if (mPublisher == null) {
            return;
        }
        mPublisher.cycleCamera();

    }


    @Override
    public void onConnectionCreated(Session session, Connection connection) {
        Log.d(TAG, "onConnectionCreated: " + session.getConnection().getData());
        Log.d(TAG, "onConnectionCreated: " + session.getConnection().getConnectionId());
    }

    @Override
    public void onConnectionDestroyed(Session session, Connection connection) {
        Log.d(TAG, "onConnectionDestroyed: connection id" + connection.getConnectionId());
        Log.d(TAG, "onConnectionDestroyed: connection time");


    }

    @Override
    public void onConnected(SubscriberKit subscriberKit) {
        Log.d(TAG, "Subcriber Kit onConnected: " + subscriberKit.getSession().getConnection().getConnectionId());
    }

    @Override
    public void onDisconnected(SubscriberKit subscriberKit) {
        Log.d(TAG, "onDisconnected: " + subscriberKit.getSession().getConnection().getConnectionId());
    }

    @Override
    public void onError(SubscriberKit subscriberKit, OpentokError opentokError) {

    }
}
