package com.kasiefm;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;

import com.kasifm.R;

public class MainActivity extends AppCompatActivity {

    private ExoPlayer player;
    private boolean isPlaying = false;
    private Button playPauseButton;
    private TextView statusText;

    private static final String RADIO_STREAM_URL = "https://example.com/radio-stream.mp3"; // Replace later

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
        player = new ExoPlayer.Builder(this).build();
        MediaItem mediaItem = MediaItem.fromUri(RADIO_STREAM_URL);
        player.setMediaItem(mediaItem);

        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (playbackState == Player.STATE_READY) {
                    statusText.setText("Ready to play");
                } else if (playbackState == Player.STATE_BUFFERING) {
                    statusText.setText("Buffering...");
                }
            }

            @Override
            public void onPlayerError(@NonNull PlaybackException error) {
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
        if (playPauseButton != null) {
            if (isPlaying) {
                playPauseButton.setText("Pause");
            } else {
                playPauseButton.setText("Play");
            }
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