package com.example.admin.keyproirityapp.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.util.Log;

import com.example.admin.keyproirityapp.model.LocalFile;

/**
 * Created by Anil on 12/3/2018.
 */

public class MessageDB {

    private static final String TEXT_TYPE = " TEXT";
    private static final String COMMA_SEP = ",";
    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + MessageDB.FeedEntry.TABLE_NAME + " (" +
                    MessageDB.FeedEntry.COLUMN_MESSAGE_ID + " TEXT PRIMARY KEY," +
                    MessageDB.FeedEntry.COLUMN_MESSAGE_FILE_NAME + TEXT_TYPE + COMMA_SEP +
                    MessageDB.FeedEntry.COLUMN_MESSAGE_LOCALPATH + TEXT_TYPE + COMMA_SEP +
                    MessageDB.FeedEntry.COLUMN_MESSAGE_URL + TEXT_TYPE + COMMA_SEP +
                    MessageDB.FeedEntry.COLUMN_MESSAGE_FILE_TYPE + TEXT_TYPE + COMMA_SEP +
                    MessageDB.FeedEntry.COLUMN_MESSAGE_SIZE + TEXT_TYPE + " )";
    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + MessageDB.FeedEntry.TABLE_NAME;

    private static MessageDB.MessageDBHelper mDbHelper = null;
    private static MessageDB instance = null;

    public static MessageDB getInstance(Context context) {
        if (instance == null) {
            instance = new MessageDB();
            mDbHelper = new MessageDBHelper(context);
        }
        return instance;
    }

    public long addMessageDB(LocalFile localFile) {

        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(FeedEntry.COLUMN_MESSAGE_FILE_NAME, localFile.getFileName());
        values.put(FeedEntry.COLUMN_MESSAGE_ID, localFile.getMessageId());
        values.put(FeedEntry.COLUMN_MESSAGE_LOCALPATH, localFile.getLocalPath());
        values.put(FeedEntry.COLUMN_MESSAGE_URL, localFile.getFileUrl());
        values.put(FeedEntry.COLUMN_MESSAGE_SIZE, localFile.getFileSize());
        values.put(FeedEntry.COLUMN_MESSAGE_FILE_TYPE, localFile.getFileType());
        return db.insertWithOnConflict(MessageDB.FeedEntry.TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public LocalFile getLocalFile(String messageId) {
        SQLiteDatabase db = mDbHelper.getReadableDatabase();
        Cursor cursor = null;
        LocalFile localFile = new LocalFile();
        cursor = db.query(FeedEntry.TABLE_NAME, new String[]{FeedEntry.COLUMN_MESSAGE_ID,
                        FeedEntry.COLUMN_MESSAGE_LOCALPATH, FeedEntry.COLUMN_MESSAGE_URL, FeedEntry.COLUMN_MESSAGE_ID}, FeedEntry.COLUMN_MESSAGE_ID + "=?",
                new String[]{String.valueOf(messageId)}, null, null, null, null);

        /*String selectQuery = "SELECT  * FROM " + FeedEntry.TABLE_NAME +"WHERE"+FeedEntry.COLUMN_MESSAGE_ID+"="+messageId ;

        Log.e("MessageDB", selectQuery);

        Cursor c = db.rawQuery(selectQuery, null);
       */
        if (cursor != null && cursor.moveToFirst()) {
            cursor.moveToFirst();
            localFile.setLocalPath(cursor.getString(cursor.getColumnIndex(FeedEntry.COLUMN_MESSAGE_LOCALPATH)));
            localFile.setFileUrl(cursor.getString(cursor.getColumnIndex(FeedEntry.COLUMN_MESSAGE_URL)));
            localFile.setMessageId(cursor.getString(cursor.getColumnIndex(FeedEntry.COLUMN_MESSAGE_ID)));
            Log.d("MessageDB", "getLocalFile: " + localFile.toString());
        }
       /* cursor = db.query(MessageDB.FeedEntry.TABLE_NAME, new String[]{FeedEntry.COLUMN_MESSAGE_ID,
                        FeedEntry.COLUMN_MESSAGE_URL, FeedEntry.COLUMN_MESSAGE_LOCALPATH, FeedEntry.COLUMN_MESSAGE_FILE_NAME, FeedEntry.COLUMN_MESSAGE_FILE_NAME}, FriendDB.FeedEntry.COLUMN_NAME_ID + "=?",
                new String[]{String.valueOf(messageId)}, null, null, null, null);

        // cursor = db.rawQuery("SELECT * FROM"+ FeedEntry.TABLE_NAME +"WHERE"+FeedEntry.COLUMN_NAME_ID=?", new String[] {empNo + ""});
        if(cursor!=null && cursor.moveToFirst()){
            cursor.moveToFirst();
            String localFilePath=cursor.getString(1);

            Log.d("LocalDB", "getLocalFile: 1"+cursor.getString(1));
            Log.d("LocalDB", "getLocalFile: 0"+cursor.getString(0));
            Log.d("LocalDB", "getLocalFile: 2"+cursor.getString(2));

        }
       */
        return localFile;
    }

    public static class FeedEntry implements BaseColumns {

        static final String TABLE_NAME = "localfileDB";
        static final String COLUMN_MESSAGE_ID = "messageId";
        static final String COLUMN_MESSAGE_URL = "fileUrl";
        static final String COLUMN_MESSAGE_LOCALPATH = "localPath";
        static final String COLUMN_MESSAGE_SIZE = "fileSize";
        static final String COLUMN_MESSAGE_FILE_NAME = "fileName";
        static final String COLUMN_MESSAGE_FILE_TYPE = "fileType";
    }

    private static class MessageDBHelper extends SQLiteOpenHelper {
        static final int DATABASE_VERSION = 1;
        static final String DATABASE_NAME = "MessageDB.db";

        MessageDBHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase sqLiteDatabase) {
            sqLiteDatabase.execSQL(SQL_CREATE_ENTRIES);

        }

        @Override
        public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
            // This database is only a cache for online data, so its upgrade policy is
            // to simply to discard the data and start over
            sqLiteDatabase.execSQL(SQL_DELETE_ENTRIES);
            onCreate(sqLiteDatabase);
        }

        @Override
        public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            onUpgrade(db, oldVersion, newVersion);
        }
    }
}
