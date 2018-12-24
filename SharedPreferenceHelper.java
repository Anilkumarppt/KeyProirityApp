package com.example.admin.keyproirityapp.database;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.admin.keyproirityapp.model.NotificationResponse;
import com.example.admin.keyproirityapp.model.User;

/**
 * Created by Anil on 12/3/2018.
 */


public class SharedPreferenceHelper {
    private static final String SHARE_KEY_ROOMID = "roomId";
    private static SharedPreferenceHelper instance = null;
    private static SharedPreferences preferences;
    private static SharedPreferences.Editor editor;
    private static String SHARE_USER_INFO = "userinfo";
    private static String SHARE_KEY_NAME = "name";
    private static String SHARE_KEY_EMAIL = "email";
    private static String SHARE_KEY_AVATA = "avata";
    private static String SHARE_KEY_UID = "uid";
    private static String GROUPICON = "groupIcon";
    private static String GROUPNAME = "name";
    private static String SHARE_KEY_DEVICE_TOKEN = "deviceToken";
    private static String SHARE_KEY_MOBILE = "mobile";

    private static String SHARE_NOTIFICATION_KEY = "notification_key";
    private static String SHARE_NOTIFICATION_KEY_NAME = "notification_key_name";

    private SharedPreferenceHelper() {
    }

    public static SharedPreferenceHelper getInstance(Context context) {
        if (instance == null) {
            instance = new SharedPreferenceHelper();
            preferences = context.getSharedPreferences(SHARE_USER_INFO, Context.MODE_PRIVATE);
            editor = preferences.edit();
        }
        return instance;
    }


    public void saveUserInfo(User user) {
        editor.putString(SHARE_KEY_NAME, user.name);
        editor.putString(SHARE_KEY_EMAIL, user.email);
        editor.putString(SHARE_KEY_AVATA, user.avata);
        editor.putString(SHARE_KEY_UID, StaticConfig.UID);
        editor.putString(SHARE_KEY_MOBILE, user.mobile);
        editor.putString(SHARE_KEY_DEVICE_TOKEN, user.deviceToken);
        editor.apply();
    }

    public String getGroupId() {
        String roomId = preferences.getString(SHARE_KEY_ROOMID, "");
        return roomId;
    }

    public void setGroupId(String groupId) {
        editor.putString(SHARE_KEY_ROOMID, groupId);
    }

    public NotificationResponse getGroupNotificationKey() {
        String notificationKey = preferences.getString(SHARE_NOTIFICATION_KEY, "");
        String notificationGroupId = preferences.getString(SHARE_KEY_ROOMID, "");
        String notificationKeyname = preferences.getString(SHARE_NOTIFICATION_KEY_NAME, "");
        NotificationResponse key = new NotificationResponse(notificationKey, notificationKeyname, notificationGroupId);
        return key;
    }

    public void setGroupNotificationKey(NotificationResponse notificationResponse) {
        editor.putString(SHARE_NOTIFICATION_KEY, notificationResponse.getNotification_key());
        editor.putString(SHARE_KEY_ROOMID, notificationResponse.getGroupId());
        editor.putString(SHARE_NOTIFICATION_KEY_NAME, notificationResponse.getNotification_key_name());
    }

    public User getUserInfo() {
        String userName = preferences.getString(SHARE_KEY_NAME, "");
        String email = preferences.getString(SHARE_KEY_EMAIL, "");
        String avatar = preferences.getString(SHARE_KEY_AVATA, "default");
        String mobile = preferences.getString(SHARE_KEY_MOBILE, "0");
        String deviceToken = preferences.getString(SHARE_KEY_DEVICE_TOKEN, "default");
        User user = new User(userName, email, avatar, mobile, deviceToken);
        return user;
    }

    public String getUID() {
        return preferences.getString(SHARE_KEY_UID, "");
    }

}

