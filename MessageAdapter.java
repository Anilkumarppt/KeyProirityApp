package com.example.admin.keyproirityapp.adapter;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.example.admin.keyproirityapp.GlideApp;
import com.example.admin.keyproirityapp.R;
import com.example.admin.keyproirityapp.database.MessageDB;
import com.example.admin.keyproirityapp.listners.ClickListenerChatFirebase;
import com.example.admin.keyproirityapp.listners.DownloadFileListener;
import com.example.admin.keyproirityapp.model.ChatModel;
import com.example.admin.keyproirityapp.model.LocalFile;
import com.example.admin.keyproirityapp.service.DownloadFileFromURL;
import com.example.admin.keyproirityapp.util.AppUtils;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Created by Anil on 12/3/2018.
 */

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static final String TAG = MessageAdapter.class.getSimpleName();
    public static final int TEXT_TYPE = 0;
    public static final int IMAGE_TYPE = 1;
    public static final int AUDIO_TYPE = 2;
    public static final int DOCUMENT_TYPE = 3;
    private static final int MSG_UPDATE_SEEK_BAR = 1845;
    public MediaPlayer mediaPlayer;
    public MediaTypeViewHolder playingHolder;
    List<ChatModel> messages = new ArrayList<>();
    Context mContext;
    DownloadFileListener downloadFileListener;
    private ClickListenerChatFirebase mClickListenerChatFirebase;
    private Handler uiUpdateHandler;
    private int playingPosition;
    private MediaTypeViewHolder mediaTypeViewHolder;
    private int currentPlayingPosition;
    private SeekBarUpdater seekBarUpdater;

    public MessageAdapter(Context mContext, List<ChatModel> messages, ClickListenerChatFirebase mClickListenerChatFirebase) {
        this.messages = messages;
        this.mContext = mContext;
        this.mClickListenerChatFirebase = mClickListenerChatFirebase;
        this.currentPlayingPosition = -1;
        seekBarUpdater = new SeekBarUpdater();

    }

    public static String getTime(String milliseconds) {
        String dateFormat = "hh:mm:a";
        SimpleDateFormat formatter = new SimpleDateFormat(dateFormat);
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(Long.parseLong(milliseconds));
        return formatter.format(calendar.getTime());
    }

    public static void loadImage(final Context context, String imageUrl, final ImageView imageView, String imageThumnailUrl) {
        if (TextUtils.isEmpty(imageUrl)) {
            imageView.setImageDrawable(null);
            imageView.setPadding(0, 0, 0, 0);
            imageView.setVisibility(View.GONE);
        } else {
            imageView.setVisibility(View.VISIBLE);
            if (imageThumnailUrl.equals("defalut")) {
                GlideApp.with(context)
                        .load(imageThumnailUrl)
                        .apply(new RequestOptions().override(250, 250).placeholder(R.drawable.nophoto).error(R.drawable.androidphoto))
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .fitCenter()
                        .into(imageView);

            } else {
                GlideApp.with(context)
                        .load(imageUrl)
                        .apply(new RequestOptions().override(250, 250).placeholder(R.drawable.nophoto).error(R.drawable.androidphoto))
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .fitCenter()
                        .into(imageView);
            }
        }
    }

    public boolean isValidFileToView(LocalFile localFile) {
        if (localFile != null && localFile.getLocalPath() != null) {
            try {
                File file = new File(localFile.getLocalPath());
                if (file.exists()) {
                    return true;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;

        switch (viewType) {

            case TEXT_TYPE:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_text_message, parent, false);
                return new TextViewHolder(view);
            case IMAGE_TYPE:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_image_message, parent, false);
                return new ImageTypeViewHolder(view);
            case AUDIO_TYPE:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_audio_message, parent, false);
                return new MediaTypeViewHolder(view);
            case DOCUMENT_TYPE:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_pdf_file, parent, false);
                return new DocumentTypeViewHolder(view);
        }
        return null;
    }

    @Override
    public int getItemViewType(int position) {

        switch (messages.get(position).getContentType()) {
            case "text":
                return TEXT_TYPE;
            case "Image":
                return IMAGE_TYPE;
            case "audio":
                return AUDIO_TYPE;
            case "Pdf":
                return DOCUMENT_TYPE;
            default:
                return -1;
        }
    }

    public ChatModel getItemPosition(int position) {
        return messages.get(position);
    }

    @Override
    public void onBindViewHolder(@NonNull final RecyclerView.ViewHolder holder, final int position) {
        int viewType = getItemViewType(position);
        final ChatModel model = getItemPosition(position);
        holder.setIsRecyclable(false);
        String thumNailImage;
        if (model.getImageThumnailUrl() != null) {
            thumNailImage = model.getImageThumnailUrl();
            Log.d(TAG, "onBindViewHolder: ThumNail" + thumNailImage);
        }

        switch (viewType) {

            case TEXT_TYPE:
                TextViewHolder textViewHolder = (TextViewHolder) holder;
                if (model.isSender()) {
                    textViewHolder.relativeLayout.requestLayout();
                    RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                            RelativeLayout.LayoutParams.WRAP_CONTENT,
                            RelativeLayout.LayoutParams.WRAP_CONTENT);
                    params.addRule(RelativeLayout.ALIGN_PARENT_END);
                    textViewHolder.relativeLayout.setLayoutParams(params);
                    textViewHolder.textMsg.setBackgroundResource(R.drawable.message_sent_chip);
                    textViewHolder.textMsg.setText(model.getMessage());
                    textViewHolder.textTime.setText(getTime(model.getTime()));
                    textViewHolder.textName.setVisibility(View.GONE);
                } else {
                    textViewHolder.textMsg.setText(model.getMessage());
                    textViewHolder.textName.setText(model.getUserName());
                    textViewHolder.textTime.setText(getTime(model.getTime()));
                }
                break;
            case IMAGE_TYPE:
                final ImageTypeViewHolder imageTypeViewHolder = (ImageTypeViewHolder) holder;
                String imageLocalPath = null;
                if (model.isSender()) {
                    RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,
                            RelativeLayout.LayoutParams.WRAP_CONTENT);
                    params.addRule(RelativeLayout.ALIGN_PARENT_END);
                    imageTypeViewHolder.relativeLayout.setLayoutParams(params);
                    imageTypeViewHolder.imageSender.setText(model.getUserName());
                    imageTypeViewHolder.msg_time.setText(getTime(model.getTime()));
                    LocalFile imageLocalFile = MessageDB.getInstance(mContext).getLocalFile(model.getMessageId());
                    if (isValidFileToView(imageLocalFile)) {
                        imageLocalPath = imageLocalFile.getLocalPath();
                        File file = new File(imageLocalPath);
                        if (file.exists()) {
                            Log.d(TAG, "File existed");
                            Log.d(TAG, "Image Local File MessageId" + imageLocalFile.getMessageId() + "  " + "Image Uri Path" + Uri.fromFile(new File(imageLocalPath)));
                            Log.d(TAG, "Image Local File" + imageLocalPath);
                            GlideApp.with(mContext)
                                    .load(file)
                                    .into(imageTypeViewHolder.imageMsg);
                        } else {
                            Log.d(TAG, "File Not existed");
                        }
                    }
                    loadImage(mContext, model.getImageUrl(), imageTypeViewHolder.imageMsg, model.getImageThumnailUrl());
                    imageTypeViewHolder.imageMsg.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            String userName = model.getUserName();
                            String userPhoto = model.getAvatarUrl();
                            if (userName != null) {
                                mClickListenerChatFirebase.clickImageChat(view, position, userName, "default", model.getImageUrl());
                            }
                            mClickListenerChatFirebase.clickImageChat(view, position, "default", "default", model.getImageUrl());
                        }
                    });
                } else if (!model.isSender()) {
                    imageTypeViewHolder.imageSender.setText(model.getUserName());
                    imageTypeViewHolder.msg_time.setText(getTime(model.getTime()));
                    loadImage(mContext, model.getImageUrl(), imageTypeViewHolder.imageMsg, model.getImageThumnailUrl());
                    imageTypeViewHolder.imageMsg.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            String userName = model.getUserName();
                            String userPhoto = model.getAvatarUrl();
                            if (userName != null) {
                                mClickListenerChatFirebase.clickImageChat(view, position, userName, "default", model.getImageUrl());
                            }
                            mClickListenerChatFirebase.clickImageChat(view, position, "default", "default", model.getImageUrl());
                        }
                    });
                }

                break;
            case AUDIO_TYPE:
                LocalFile audiolocalFile = MessageDB.getInstance(mContext).getLocalFile(model.getMessageId());
                final MediaTypeViewHolder mediaTypeViewHolder = (MediaTypeViewHolder) holder;
                boolean isValidFile = isValidFileToView(audiolocalFile);
                if (isValidFile) {
                    mediaTypeViewHolder.downloadAudio.setVisibility(View.GONE);
                    mediaTypeViewHolder.audioButton.setVisibility(View.VISIBLE);
                } else {
                    if (model.mAduioFile.getFileName() == null) {
                        mediaTypeViewHolder.downloadAudio.setVisibility(View.GONE);
                    } else {
                        mediaTypeViewHolder.downloadAudio.setVisibility(View.VISIBLE);
                    }
                    mediaTypeViewHolder.audioButton.setVisibility(View.GONE);
                }
                if (position == currentPlayingPosition) {
                    playingHolder = (MediaTypeViewHolder) holder;
                    updatePlayingView();
                } else {
                    updateNonPlayingView(holder);
                }

                mediaTypeViewHolder.mProgressbar.setVisibility(View.GONE);
                String audioSize = AppUtils.convertToStringRepresentation(audiolocalFile.getFileName());

                mediaTypeViewHolder.downloadAudio.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mediaTypeViewHolder.mProgressbar.setVisibility(View.VISIBLE);
                        LocalFile localFile = MessageDB.getInstance(mContext).getLocalFile(model.getMessageId());
                        if (isValidFileToView(localFile)) {
                            Log.d(TAG, "processFinish: " + localFile.toString());
                            Toast.makeText(mContext, "File already Downloaded", Toast.LENGTH_SHORT).show();
                            mediaTypeViewHolder.mProgressbar.setVisibility(View.GONE);
                        } else {
                            new DownloadFileFromURL(mContext, model.getContentLocation(), model.mAduioFile.getFileName(), "mp3", mediaTypeViewHolder.mProgressbar,
                                    new DownloadFileFromURL.AsyncResponse() {
                                        @Override
                                        public void processFinish(String result) {
                                            mediaTypeViewHolder.mProgressbar.setVisibility(View.GONE);
                                            Toast.makeText(mContext, result + "AudioFile Downloaded", Toast.LENGTH_SHORT).show();
                                            LocalFile file = new LocalFile();
                                            file.setFileUrl(model.getContentLocation());
                                            file.setFileName(model.mAduioFile.getFileName());
                                            file.setLocalPath(result);
                                            file.setMessageId(model.getMessageId());
                                            long rowInserted = MessageDB.getInstance(mContext).addMessageDB(file);
                                            if (rowInserted != -1) {
                                                Toast.makeText(mContext, "New Row  Inserted", Toast.LENGTH_SHORT).show();
                                                Log.d(TAG, "processFinish: Message Id" + model.messageId);
                                                //LocalFile localFile=MessageDB.getInstance(mContext).getLocalFile(model.getMessageId());
                                           /* if(localFile!=null){
                                                Log.d(TAG, "processFinish: "+localFile.toString());
                                            }
*/
                                                notifyDataSetChanged();
                                            } else {
                                                Toast.makeText(mContext, "Something wrong", Toast.LENGTH_SHORT).show();
                                            }

                                        }
                                    }).execute();

                        }
                    }
                });
                if (audioSize.equals("0")) {
                    audioSize = "Undefined size";
                }

                if (model.isSender()) {
                    Log.d(TAG, "onBindViewHolder: sender Audio");
                    mediaTypeViewHolder.relativeLayout.requestLayout();
                    RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,
                            RelativeLayout.LayoutParams.WRAP_CONTENT);
                    params.addRule(RelativeLayout.ALIGN_PARENT_END);
                    mediaTypeViewHolder.relativeLayout.setLayoutParams(params);
                    mediaTypeViewHolder.audioTime.setText(getTime(model.getTime()));
                    mediaTypeViewHolder.auioTitle.setText(model.mAduioFile.getFileName());
                    mediaTypeViewHolder.audioFileSize.setText(audioSize + "MB");
                } else {
                    mediaTypeViewHolder.relativeLayout.requestLayout();
                    RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,
                            RelativeLayout.LayoutParams.WRAP_CONTENT);
                    params.addRule(RelativeLayout.ALIGN_PARENT_START);
                    mediaTypeViewHolder.relativeLayout.setLayoutParams(params);

                    mediaTypeViewHolder.audioSender.setText(model.getUserName());
                    mediaTypeViewHolder.audioTime.setText(getTime(model.getTime()));
                    mediaTypeViewHolder.auioTitle.setText(model.mAduioFile.getFileName());
                    mediaTypeViewHolder.audioFileSize.setText(audioSize + "MB");
                }
                break;
            case DOCUMENT_TYPE:
                final DocumentTypeViewHolder viewHolder = (DocumentTypeViewHolder) holder;
                LocalFile localFile = MessageDB.getInstance(mContext).getLocalFile(model.getMessageId());
                String localPath;
                viewHolder.download_pdf_file.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Toast.makeText(mContext, "Document", Toast.LENGTH_SHORT).show();
                        LocalFile localFile = MessageDB.getInstance(mContext).getLocalFile(model.getMessageId());
                        if (localFile.getLocalPath() != null) {
                            Log.d(TAG, "processFinish: " + localFile.toString());
                            LocalFile file = getStorageMetadata(model.getContentLocation());
                            Log.d(TAG, "onClick: fileMetadata" + file.toString());
                        } else {
                            new DownloadFileFromURL(mContext, model.getContentLocation(), model.mPdfFile.getPdfFileName(), "pdf",
                                    new DownloadFileFromURL.AsyncResponse() {
                                        @Override
                                        public void processFinish(String result) {
                                            Toast.makeText(mContext, result + "Pdf File Downloaded", Toast.LENGTH_SHORT).show();
                                            LocalFile file = new LocalFile();
                                            file.setFileUrl(model.getContentLocation());
                                            file.setFileName(model.mAduioFile.getFileName());
                                            file.setLocalPath(result);
                                            file.setMessageId(model.getMessageId());
                                            long rowInserted = MessageDB.getInstance(mContext).addMessageDB(file);
                                            if (rowInserted != -1) {

                                            } else {
                                                Toast.makeText(mContext, "Something wrong", Toast.LENGTH_SHORT).show();
                                            }

                                        }
                                    }).execute();
                            viewHolder.download_pdf_file.setVisibility(View.INVISIBLE);
                        }
                    }
                });
                if (model.isSender()) {
                    if (isValidFileToView(localFile)) {
                        viewHolder.download_pdf_file.setVisibility(View.GONE);
                        viewHolder.msg_time.setText(getTime(model.getTime()));
                        viewHolder.msg_text.setText(model.mPdfFile.getPdfFileName() + ".pdf");
                    } else {

                    }
                } else {
                    RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                    viewHolder.pdfFileLayout.requestLayout();
                    params.addRule(RelativeLayout.ALIGN_PARENT_START);
                    viewHolder.pdfFileLayout.setLayoutParams(params);
                    viewHolder.msg_time.setText(getTime(model.getTime()));
                    viewHolder.msg_text.setText(model.mPdfFile.getPdfFileName() + ".pdf");
                    viewHolder.senderName.setText(model.getUserName());
                }
                viewHolder.pdfImage.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setDataAndType(Uri.parse(model.getContentLocation()), "application/pdf");
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        Intent newIntent = Intent.createChooser(intent, "Open File");
                        try {
                            mContext.startActivity(newIntent);
                        } catch (ActivityNotFoundException e) {
                            Toast.makeText(mContext, "Please install any PDF Reader Application", Toast.LENGTH_SHORT).show();
                            // Instruct the user to install a PDF reader here, or something
                        } //downloadFileListener.downloadFile(mContext,model.mPdfFile.getPdfFileLocation());
                    }
                });
                break;
            default:
        }

    }

    private void updateNonPlayingView(RecyclerView.ViewHolder holder) {
        MediaTypeViewHolder holder1 = (MediaTypeViewHolder) holder;
        holder1.auioTitle.setVisibility(View.VISIBLE);
        holder1.mProgressbar.setVisibility(View.GONE);
        holder1.sbProgress.removeCallbacks(seekBarUpdater);
        holder1.sbProgress.setEnabled(false);
        holder1.sbProgress.setProgress(0);
        holder1.audioButton.setImageResource(R.drawable.ic_play_arrow);
        holder1.sbProgress.setVisibility(View.GONE);
    }

    private void updatePlayingView() {
        if (mediaPlayer != null) {
            playingHolder.auioTitle.setVisibility(View.INVISIBLE);
            playingHolder.sbProgress.setVisibility(View.VISIBLE);
            playingHolder.sbProgress.setMax(mediaPlayer.getDuration());
            playingHolder.sbProgress.setProgress(mediaPlayer.getCurrentPosition());
            playingHolder.sbProgress.setEnabled(true);
            playingHolder.mProgressbar.setVisibility(View.GONE);
            if (mediaPlayer.isPlaying()) {
                playingHolder.sbProgress.postDelayed(seekBarUpdater, 100);
                playingHolder.audioButton.setImageResource(R.drawable.ic_pause);
            } else {
                playingHolder.sbProgress.removeCallbacks(seekBarUpdater);
                playingHolder.audioButton.setImageResource(R.drawable.ic_play_arrow);
            }
        }

    }

    public void stopPlayer() {
        if (null != mediaPlayer) {
            releaseMediaPlayer();
        }
    }

    private void startMediaPlayer(String audioURL) throws IOException {
        Log.d(TAG, "startMediaPlayer: ");
        mediaPlayer = new MediaPlayer();

        mediaPlayer.setDataSource(audioURL);
        try {
            mediaPlayer.prepare();
            mediaPlayer.start();
//            mediaPlayer.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
        }
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                releaseMediaPlayer();
            }
        });
    }

    private void releaseMediaPlayer() {
        if (null != playingHolder) {
            updateNonPlayingView(playingHolder);
        }
        mediaPlayer.release();
        mediaPlayer = null;
        currentPlayingPosition = -1;
    }

    @Override
    public int getItemCount() {
        return messages.size();

    }

    public LocalFile getStorageMetadata(String fileUrl) {

        final LocalFile localFile = new LocalFile();
        StorageReference storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(fileUrl);
        storageRef.getMetadata().addOnSuccessListener(new OnSuccessListener<StorageMetadata>() {
            @Override
            public void onSuccess(StorageMetadata storageMetadata) {
                long bytes = storageMetadata.getSizeBytes();
                Log.d(TAG, "onSuccess: Metadata" + storageMetadata.toString());
                long kb = bytes / 1024;
                Log.d(TAG, "onSuccess: size in kb" + kb);
                long mb = kb / 1024;
                Log.d(TAG, "onSuccess: size in mb" + mb);
                localFile.setFileSize(String.valueOf(mb));
                Log.d(TAG, "onSuccess: storage file size" + storageMetadata.getSizeBytes() + "Bytes");
            }
        });
        return localFile;
    }

    public static class TextViewHolder extends RecyclerView.ViewHolder {
        TextView textMsg, textTime, textName;
        RelativeLayout relativeLayout;
        CardView cardView;

        public TextViewHolder(View itemView) {
            super(itemView);
            relativeLayout = itemView.findViewById(R.id.layout_text_message);
            textMsg = itemView.findViewById(R.id.item_message);
            textTime = itemView.findViewById(R.id.item_time);
            textName = itemView.findViewById(R.id.item_sender_name);
        }

    }

    public static class ImageTypeViewHolder extends RecyclerView.ViewHolder {

        TextView imageSender, msg_time;

        ImageView imageMsg;

        RelativeLayout relativeLayout;
        CardView cardView;

        public ImageTypeViewHolder(View itemView) {
            super(itemView);
            relativeLayout = itemView.findViewById(R.id.layout_image_message);
            imageSender = itemView.findViewById(R.id.item_image_sender_name);
            msg_time = itemView.findViewById(R.id.item_image_time);
            imageMsg = itemView.findViewById(R.id.item_image_message);

        }
    }

    public static class DocumentTypeViewHolder extends RecyclerView.ViewHolder {

        CardView cardView;
        TextView senderName, msg_time, msg_text;
        ImageView pdfImage;
        RelativeLayout pdfFileLayout;
        ImageButton download_pdf_file;

        public DocumentTypeViewHolder(View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.pdf_card_view);
            senderName = itemView.findViewById(R.id.pdf_msg_sender);
            msg_time = itemView.findViewById(R.id.pdf_msg_time);
            pdfImage = itemView.findViewById(R.id.pdf_msg_image);
            msg_text = itemView.findViewById(R.id.pdf_file_name);
            pdfFileLayout = itemView.findViewById(R.id.pdf_file_layout);
            download_pdf_file = itemView.findViewById(R.id.download_pdf_file);
        }

    }

    class MediaTypeViewHolder extends RecyclerView.ViewHolder implements SeekBar.OnSeekBarChangeListener, View.OnClickListener, DownloadFileListener {
        CardView cardView;
        TextView audioSender, audioTime, auioTitle, audioFileSize;
        ImageButton audioButton, downloadAudio;
        SeekBar sbProgress;
        RelativeLayout relativeLayout;
        ProgressBar mProgressbar;
        boolean finish = false;
        private Handler uiUpdateHandler;
        private int resumePosition;


        public MediaTypeViewHolder(View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardview);
            audioSender = itemView.findViewById(R.id.audio_msg_sender);
            audioTime = itemView.findViewById(R.id.audio_msg_time);
            audioButton = itemView.findViewById(R.id.audio_play_pause);
            auioTitle = itemView.findViewById(R.id.audio_title);
            sbProgress = itemView.findViewById(R.id.sbProgress);
            audioButton.setOnClickListener(this);
            audioFileSize = itemView.findViewById(R.id.audio_msg_size);
            relativeLayout = itemView.findViewById(R.id.layout_audio_message);
            mProgressbar = itemView.findViewById(R.id.download_progress);
            downloadAudio = itemView.findViewById(R.id.download_file);
        }


        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromuser) {
            if (fromuser) {
                mediaPlayer.seekTo(progress);

            }

        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onClick(View view) {

            Log.d(TAG, "onClick: CurrentPlaying position" + currentPlayingPosition);
            if (getAdapterPosition() == currentPlayingPosition && mediaPlayer != null) {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();
                } else {
                    mediaPlayer.start();
                }
                // updatePlayingView();
            } else {
                currentPlayingPosition = getAdapterPosition();
                if (mediaPlayer != null) {
                    if (null != playingHolder) {
                        updateNonPlayingView(playingHolder);
                    }
                    mediaPlayer.release();
                }
                playingHolder = this;
                ChatModel po = getItemPosition(currentPlayingPosition);
                LocalFile localFile = MessageDB.getInstance(mContext).getLocalFile(po.getMessageId());
                if (localFile != null && localFile.getLocalPath() != null) {
                    try {
                        File file = new File(localFile.getLocalPath());
                        if (file.exists()) {
                            startMediaPlayer(localFile.getLocalPath());
                        } else {
                            Toast.makeText(mContext, "Downloaded file not existed in storage", Toast.LENGTH_SHORT).show();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                } else {
                    Toast.makeText(mContext, "Please download the File before play", Toast.LENGTH_SHORT).show();
                }
            }
            updatePlayingView();

        }

        @Override
        public void downloadFile(Context context, String file_url) {

        }


        @Override
        public void downloadAudiofile(String file) {
            Log.d(TAG, "downloadAudiofile: File link" + file);

        }
    }

    private class SeekBarUpdater implements Runnable {
        @Override
        public void run() {
            if (null != playingHolder) {
                playingHolder.sbProgress.setProgress(mediaPlayer.getCurrentPosition());
                playingHolder.sbProgress.postDelayed(this, 100);
            }
        }
    }
}
