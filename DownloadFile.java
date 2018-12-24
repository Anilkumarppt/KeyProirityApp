package com.example.admin.keyproirityapp;

import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;

import com.example.admin.keyproirityapp.listners.DownloadFileListener;
import com.example.admin.keyproirityapp.ui.BaseActivity;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;

/**
 * Created by DELL on 12/5/2018.
 */

public class DownloadFile extends BaseActivity implements DownloadFileListener {

    StorageReference storageReference;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public void downloadFile(Context context, String file_url) {
        storageReference = FirebaseStorage.getInstance().getReference();
        File rootPath = new File(getExternalFilesDir(
                Environment.DIRECTORY_PICTURES), "ChatPdfFiles");
        if (!rootPath.exists()) {
            rootPath.mkdir();
        }


    }

    @Override
    public void downloadAudiofile(String file) {

    }
}
