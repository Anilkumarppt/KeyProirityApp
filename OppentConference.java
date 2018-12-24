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
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.admin.keyproirityapp.MainActivity;
import com.example.admin.keyproirityapp.R;
import com.example.admin.keyproirityapp.widgets.ConstraintSetHelper;
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

/**
 * Created by Anil on 12/3/2018.
 */

public class OppentConference extends AppCompatActivity
        implements EasyPermissions.PermissionCallbacks,
        Publisher.PublisherListener,
        Session.SessionListener, Session.SignalListener, SubscriberKit.VideoListener, SubscriberKit.SubscriberListener {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int RC_SETTINGS_SCREEN_PERM = 123;
    private static final int RC_VIDEO_APP_PERM = 124;
    private Session mSession;
    private Publisher mPublisher;
    private String API_KEY, SESSION_ID, TOKEN;
    private ArrayList<Subscriber> mSubscribers = new ArrayList<Subscriber>();
    private HashMap<Stream, Subscriber> mSubscriberStreams = new HashMap<Stream, Subscriber>();
    private RelativeLayout subscriberview1, subscriberview2, subscriberview3;

    private ConstraintLayout mContainer;
    private int MAX_NUM_SUBSCRIBERS = 4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conference_call);
        subscriberview1 = findViewById(R.id.subscriber_container1);
        subscriberview2 = findViewById(R.id.subscriber_container2);
        subscriberview2.setVisibility(View.VISIBLE);
        subscriberview1.setVisibility(View.VISIBLE);
        if (getIntent().getExtras() != null) {
            API_KEY = getIntent().getExtras().getString("api");
            SESSION_ID = getIntent().getExtras().getString("sessionId");
            TOKEN = getIntent().getExtras().getString("sessionToken");
            Log.d(TAG, "onCreate: TOken" + TOKEN);
            Log.d(TAG, "onCreate: SESSION ID" + SESSION_ID);
            mSession = new Session.Builder(OppentConference.this, API_KEY, SESSION_ID).build();
            mSession.setSessionListener(OppentConference.this);
            mSession.setSignalListener(OppentConference.this);
            mSession.connect(TOKEN);
            startPublisherPreview();
            mPublisher.getView().setId(R.id.publisher_view_id);
            mContainer.addView(mPublisher.getView());
            //mSession= (Session) getIntent().getSerializableExtra("session");
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
        mPublisher = new Publisher.Builder(OppentConference.this).name("Publisher").build();
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
            //calculateLayout();
        } else {
            EasyPermissions.requestPermissions(this, getString(R.string.rationale_video_app), RC_VIDEO_APP_PERM, perms);
        }
    }

    @Override
    public void onConnected(Session session) {
        Log.d(TAG, "onConnected: Connected to session " + session.getSessionId());
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
        Toast.makeText(this, "New Stream Received from " + stream.getName(), Toast.LENGTH_SHORT).show();
        Log.d(TAG, "Stream name " + stream.getName());
        if (mSubscribers.size() + 1 > MAX_NUM_SUBSCRIBERS) {
            Toast.makeText(this, "New subscriber ignored. MAX_NUM_SUBSCRIBERS limit reached.", Toast.LENGTH_LONG).show();
            return;
        }
        final Subscriber subscriber = new Subscriber.Builder(OppentConference.this, stream).build();
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
                status1.setText("connecting to ");
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
                status.setText("Connecting to ");
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
        Log.d(TAG, "onStreamDropped: Stream " + stream.getStreamId() + " dropped from session " + session.getSessionId());
        Toast.makeText(this, "Stream Dropped from " + stream.getName(), Toast.LENGTH_SHORT).show();
        Subscriber subscriber = mSubscriberStreams.get(stream);
        if (subscriber == null) {
            return;
        }

        int position = mSubscribers.indexOf(subscriber);
        int id = getResources().getIdentifier("sub" + (new Integer(position)).toString(), "id", OppentConference.this.getPackageName());
        mSubscribers.remove(subscriber);
        mSubscriberStreams.remove(stream);
        RelativeLayout subscriberViewContainer = (RelativeLayout) findViewById(id);
        subscriberViewContainer.removeView(subscriber.getView());
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

    @Override
    public void onVideoDataReceived(SubscriberKit subscriberKit) {

    }

    @Override
    public void onVideoDisabled(SubscriberKit subscriberKit, String s) {

    }

    @Override
    public void onVideoEnabled(SubscriberKit subscriberKit, String s) {

    }

    @Override
    public void onVideoDisableWarning(SubscriberKit subscriberKit) {

    }

    @Override
    public void onVideoDisableWarningLifted(SubscriberKit subscriberKit) {

    }

    @Override
    public void onConnected(SubscriberKit subscriberKit) {

    }

    @Override
    public void onDisconnected(SubscriberKit subscriberKit) {

    }

    @Override
    public void onError(SubscriberKit subscriberKit, OpentokError opentokError) {

    }

    public void addFriendCall(View view) {
        Toast.makeText(this, "Add friend to call", Toast.LENGTH_SHORT).show();
    }

    public void switchCamera(View view) {
        if (mPublisher != null) {
            mPublisher.cycleCamera();
        }
    }

}
