package com.kasiefm;

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

public class MainActivity extends AppCompatActivity {

    private SimpleExoPlayer player;
    private boolean isPlaying = false;
    private Button playPauseButton;
    private TextView statusText;

    // Test stream - replace with Kasie FM URL later
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
                if (playbackState == Player.STATE_BUFFERING) {
                    statusText.setText("Buffering...");
                } else if (playbackState == Player.STATE_READY) {
                    statusText.setText("Ready to play");
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
            statusText.setText("Playing...");
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
        if (playPauseButton != null) {
            playPauseButton.setText(isPlaying ? "Pause" : "Play");
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