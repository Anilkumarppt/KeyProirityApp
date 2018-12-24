package com.example.admin.keyproirityapp.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

import com.example.admin.keyproirityapp.model.Friend;
import com.example.admin.keyproirityapp.model.ListFriend;

/**
 * Created by Anil on 12/3/2018.
 */

public final class FriendDB {
    private static final String TEXT_TYPE = " TEXT";
    private static final String COMMA_SEP = ",";
    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + FeedEntry.TABLE_NAME + " (" +
                    FeedEntry.COLUMN_NAME_ID + " TEXT PRIMARY KEY," +
                    FeedEntry.COLUMN_NAME_NAME + TEXT_TYPE + COMMA_SEP +
                    FeedEntry.COLUMN_NAME_EMAIL + TEXT_TYPE + COMMA_SEP +
                    FeedEntry.COLUMN_NAME_ID_ROOM + TEXT_TYPE + COMMA_SEP +
                    FeedEntry.COLUMN_NAME_AVATA + TEXT_TYPE + COMMA_SEP +
                    FeedEntry.COLUMN_NAME_DEVICE_TOKEN + TEXT_TYPE + " )";
    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + FeedEntry.TABLE_NAME;
    private static FriendDBHelper mDbHelper = null;
    private static FriendDB instance = null;

    // To prevent someone from accidentally instantiating the contract class,
    // make the constructor private.
    private FriendDB() {
    }

    public static FriendDB getInstance(Context context) {
        if (instance == null) {
            instance = new FriendDB();
            mDbHelper = new FriendDBHelper(context);
        }
        return instance;
    }

    public long addFriend(Friend friend) {
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        // Create a new map of values, where column names are the keys
        ContentValues values = new ContentValues();
        values.put(FeedEntry.COLUMN_NAME_ID, friend.id);
        values.put(FeedEntry.COLUMN_NAME_NAME, friend.name);
        values.put(FeedEntry.COLUMN_NAME_EMAIL, friend.email);
        values.put(FeedEntry.COLUMN_NAME_ID_ROOM, friend.idRoom);
        values.put(FeedEntry.COLUMN_NAME_AVATA, friend.avata);
        values.put(FeedEntry.COLUMN_NAME_DEVICE_TOKEN, friend.deviceToken);
        // Insert the new row, returning the primary key value of the new row
        return db.insert(FeedEntry.TABLE_NAME, null, values);
    }

    public void addListFriend(ListFriend listFriend) {
        for (Friend friend : listFriend.getListFriend()) {
            addFriend(friend);
        }
    }

    public boolean isMasterEmpty() {

        boolean flag;
        String quString = "SELECT * FROM " + FeedEntry.TABLE_NAME;
        SQLiteDatabase db = mDbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(quString, null);
        cursor.moveToFirst();
        int count = cursor.getInt(0);
        if (count == 1) {
            flag = false;
        } else {
            flag = true;
        }
        cursor.close();
        db.close();

        return flag;
    }

    public Friend getFriend(String uid) {
        SQLiteDatabase db = mDbHelper.getReadableDatabase();
        Cursor cursor = null;
        Friend friend = new Friend();
        cursor = db.query(FeedEntry.TABLE_NAME, new String[]{FeedEntry.COLUMN_NAME_ID,
                        FeedEntry.COLUMN_NAME_AVATA, FeedEntry.COLUMN_NAME_NAME, FeedEntry.COLUMN_NAME_EMAIL, FeedEntry.COLUMN_NAME_DEVICE_TOKEN}, FeedEntry.COLUMN_NAME_ID + "=?",
                new String[]{String.valueOf(uid)}, null, null, null, null);
        // cursor = db.rawQuery("SELECT * FROM"+ FeedEntry.TABLE_NAME +"WHERE"+FeedEntry.COLUMN_NAME_ID=?", new String[] {empNo + ""});
        if (cursor != null && cursor.moveToFirst()) {
            cursor.moveToFirst();
            String name = cursor.getString(2);
            String email = cursor.getString(3);
            String avata = cursor.getString(1);
            String deviceToken = cursor.getString(4);
            friend.avata = avata;
            friend.deviceToken = deviceToken;
            friend.name = name;
            friend.id = uid;
            friend.email = email;
            cursor.close();
        }
        return friend;
    }

    public ListFriend getListFriend() {
        ListFriend listFriend = new ListFriend();
        SQLiteDatabase db = mDbHelper.getReadableDatabase();
        try {
            Cursor cursor = db.rawQuery("select * from " + FeedEntry.TABLE_NAME, null);
            while (cursor.moveToNext()) {
                Friend friend = new Friend();
                friend.id = cursor.getString(0);
                friend.name = cursor.getString(1);
                friend.email = cursor.getString(2);
                friend.idRoom = cursor.getString(3);
                friend.avata = cursor.getString(4);
                friend.deviceToken = cursor.getString(5);
                listFriend.getListFriend().add(friend);
            }
            cursor.close();
        } catch (Exception e) {
            return new ListFriend();
        }
        return listFriend;
    }

    public void dropDB() {
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        db.execSQL(SQL_DELETE_ENTRIES);
        db.execSQL(SQL_CREATE_ENTRIES);
    }

    /* Inner class that defines the table contents */
    public static class FeedEntry implements BaseColumns {
        static final String TABLE_NAME = "friend";
        static final String COLUMN_NAME_ID = "friendID";
        static final String COLUMN_NAME_NAME = "name";
        static final String COLUMN_NAME_EMAIL = "email";
        static final String COLUMN_NAME_ID_ROOM = "idRoom";
        static final String COLUMN_NAME_AVATA = "avata";
        public static String COLUMN_NAME_DEVICE_TOKEN = "deviceToken";
    }

    private static class FriendDBHelper extends SQLiteOpenHelper {
        // If you change the database schema, you must increment the database version.
        static final int DATABASE_VERSION = 1;
        static final String DATABASE_NAME = "FriendChat.db";

        FriendDBHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        public void onCreate(SQLiteDatabase db) {

            db.execSQL(SQL_CREATE_ENTRIES);
        }

        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            // This database is only a cache for online data, so its upgrade policy is
            // to simply to discard the data and start over
            db.execSQL(SQL_DELETE_ENTRIES);
            onCreate(db);
        }

        public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            onUpgrade(db, oldVersion, newVersion);
        }
    }
}
