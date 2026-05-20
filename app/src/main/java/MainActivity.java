package com.kasiefm;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.PlaybackException;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;

//above is packages i should ask AI how should learn about these packages
//what are these packages?
public class MainActivity extends AppCompatActivity {

    private SimpleExoPlayer player; //the person who is playing the app
    private boolean isPlaying = false; //
    private Button playPauseButton;
    private TextView statusText;

    // Replace with your actual radio stream URL
    private static final String RADIO_STREAM_URL = "https://ice1.somafm.com/groovesalad-128-mp3";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        initializePlayer();
    }

    private void initializeViews() {
        playPauseButton = findViewById(R.id.play_pause_button);
        statusText = findViewById(R.id.status_text);

        playPauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isPlaying) {
                    pauseRadio();
                } else {
                    playRadio();
                }
            }
        });
    }

    private void initializePlayer() {
        player = new SimpleExoPlayer.Builder(this).build();

        MediaItem mediaItem = MediaItem.fromUri(RADIO_STREAM_URL);
        player.setMediaItem(mediaItem);

        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                switch (playbackState) {
                    case Player.STATE_BUFFERING:
                        statusText.setText("Buffering...");
                        break;
                    case Player.STATE_READY:
                        statusText.setText("Ready to play");
                        break;
                    case Player.STATE_ENDED:
                        statusText.setText("Stream ended");
                        isPlaying = false;
                        updatePlayPauseButton();
                        break;
                    case Player.STATE_IDLE:
                        statusText.setText("Idle");
                        break;
                }
            }

            @Override
            public void onPlayerError(PlaybackException error) {
                statusText.setText("Error: " + error.getMessage());
                isPlaying = false;
                updatePlayPauseButton();
            }
        });

        player.prepare();
    }

    private void playRadio() {
        if (player != null) {
            player.setPlayWhenReady(true);
            isPlaying = true;
            updatePlayPauseButton();
            statusText.setText("Playing Kasie FM");
        }
    }

    private void pauseRadio() {
        if (player != null) {
            player.setPlayWhenReady(false);
            isPlaying = false;
            updatePlayPauseButton();
            statusText.setText("Paused");
        }
    }

    private void updatePlayPauseButton() {
        if (isPlaying) {
            playPauseButton.setText("Pause");
        } else {
            playPauseButton.setText("Play");
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (player != null) {
            player.setPlayWhenReady(isPlaying);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (player != null) {
            player.setPlayWhenReady(false);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (player != null) {
            player.release();
            player = null;
        }
    }
}
