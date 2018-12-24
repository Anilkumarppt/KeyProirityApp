package com.example.admin.keyproirityapp.adapter;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;

import com.example.admin.keyproirityapp.R;
import com.example.admin.keyproirityapp.model.AudioFile;

import java.io.IOException;
import java.util.List;

/**
 * Created by DELL on 12/4/2018.
 */

public class AudioItemAdapter extends RecyclerView.Adapter<AudioItemAdapter.AudioItemsViewHolder> {

    private MediaPlayer mediaPlayer;
    //
    private List<AudioFile> audioItems;
    private int currentPlayingPosition;
    private SeekBarUpdater seekBarUpdater;
    private AudioItemsViewHolder playingHolder;

    AudioItemAdapter(List<AudioFile> audioItems) {
        this.audioItems = audioItems;
        this.currentPlayingPosition = -1;
        seekBarUpdater = new SeekBarUpdater();
    }

    @Override
    public AudioItemsViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new AudioItemsViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_audio_message, parent, false));
    }

    @Override
    public void onBindViewHolder(AudioItemsViewHolder holder, int position) {
        if (position == currentPlayingPosition) {
            playingHolder = holder;
            updatePlayingView();
        } else {
            updateNonPlayingView(holder);
        }
    }

    @Override
    public void onViewRecycled(AudioItemsViewHolder holder) {
        super.onViewRecycled(holder);
        if (currentPlayingPosition == holder.getAdapterPosition()) {
            updateNonPlayingView(playingHolder);
            playingHolder = null;
        }
    }

    private void updateNonPlayingView(AudioItemsViewHolder holder) {
        holder.sbProgress.removeCallbacks(seekBarUpdater);
        holder.sbProgress.setEnabled(false);
        holder.sbProgress.setProgress(0);
        holder.ivPlayPause.setImageResource(R.drawable.ic_play_arrow);
    }

    private void updatePlayingView() {
        playingHolder.sbProgress.setMax(mediaPlayer.getDuration());
        playingHolder.sbProgress.setProgress(mediaPlayer.getCurrentPosition());
        playingHolder.sbProgress.setEnabled(true);
        if (mediaPlayer.isPlaying()) {
            playingHolder.sbProgress.postDelayed(seekBarUpdater, 100);
            playingHolder.ivPlayPause.setImageResource(R.drawable.ic_pause);
        } else {
            playingHolder.sbProgress.removeCallbacks(seekBarUpdater);
            playingHolder.ivPlayPause.setImageResource(R.drawable.ic_play_arrow);
        }
    }

    void stopPlayer() {
        if (null != mediaPlayer) {
            releaseMediaPlayer();
        }
    }

    @Override
    public int getItemCount() {
        return audioItems.size();
    }

    private void startMediaPlayer(String audioResId) throws IOException {
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPlayer.setDataSource(audioResId);
        mediaPlayer.prepare();
        mediaPlayer.start();
        //mediaPlayer = MediaPlayer.(getApplicationContext(), audioResId);
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

    private class SeekBarUpdater implements Runnable {
        @Override
        public void run() {
            if (null != playingHolder) {
                playingHolder.sbProgress.setProgress(mediaPlayer.getCurrentPosition());
                playingHolder.sbProgress.postDelayed(this, 100);
            }
        }
    }

    class AudioItemsViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, SeekBar.OnSeekBarChangeListener {
        SeekBar sbProgress;
        ImageView ivPlayPause;

        AudioItemsViewHolder(View itemView) {
            super(itemView);
            ivPlayPause = (ImageView) itemView.findViewById(R.id.audio_play_pause);
            ivPlayPause.setOnClickListener(this);
            sbProgress = (SeekBar) itemView.findViewById(R.id.sbProgress);
            sbProgress.setOnSeekBarChangeListener(this);
        }

        @Override
        public void onClick(View v) {
            if (getAdapterPosition() == currentPlayingPosition) {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();
                } else {
                    mediaPlayer.start();
                }
            } else {
                currentPlayingPosition = getAdapterPosition();
                if (mediaPlayer != null) {
                    if (null != playingHolder) {
                        updateNonPlayingView(playingHolder);
                    }
                    mediaPlayer.release();
                }
                playingHolder = this;
                try {
                    startMediaPlayer(audioItems.get(currentPlayingPosition).getFileLocation());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            updatePlayingView();
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (fromUser) {
                mediaPlayer.seekTo(progress);
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
        }
    }
}
