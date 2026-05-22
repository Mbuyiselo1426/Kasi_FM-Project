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

//Adding animation imports

import android.os.Handler;
import android.view.View;
import java.util.Random;

public class MainActivity extends AppCompatActivity {


    private ExoPlayer player;
    private boolean isPlaying = false;
    private Button playPauseButton; //play button which triggers play
    private TextView statusText;

    private static final String RADIO_STREAM_URL = "https://ice1.somafm.com/groovesalad-128-mp3";
    //Attributes for animation
    private Handler animHandler = new Handler();
    private View bar1, bar2, bar3, bar4, bar5;
    private Random random = new Random();
    //closing attributes

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //addding animation constructors

        bar1 = findViewById(R.id.bar1);
        bar2 = findViewById(R.id.bar2);
        bar3 = findViewById(R.id.bar3);
        bar4 = findViewById(R.id.bar4);
        bar5 = findViewById(R.id.bar5);

        playPauseButton = findViewById(R.id.play_pause_button);
        statusText = findViewById(R.id.status_text);

        initializePlayer();

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
                runOnUiThread(() -> {
                    if (playbackState == Player.STATE_BUFFERING) {
                        statusText.setText("Buffering...");
                    } else if (playbackState == Player.STATE_READY) {
                        statusText.setText("Ready to play");
                    }
                });
            }

            @Override
            public void onPlayerError(PlaybackException error) {
                runOnUiThread(() -> {
                    statusText.setText("Error");
                    isPlaying = false;
                    playPauseButton.setText("Play");
                });
            }
        });

        player.prepare();
    }


    private void playRadio() {
        if (player != null) {
            player.setPlayWhenReady(true);
            isPlaying = true;
            playPauseButton.setText("Pause");
            statusText.setText("Playing...");
        }
    }


    private void pauseRadio() {
        if (player != null) {
            player.setPlayWhenReady(false);
            isPlaying = false;
            playPauseButton.setText("Play");
            statusText.setText("Paused");
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
    // Added a start-wave-animation
    private void startWaveAnimation() {
        animHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isPlaying) {
                    bar1.getLayoutParams().height = 10 + random.nextInt(40);
                    bar2.getLayoutParams().height = 10 + random.nextInt(50);
                    bar3.getLayoutParams().height = 10 + random.nextInt(60);
                    bar4.getLayoutParams().height = 10 + random.nextInt(50);
                    bar5.getLayoutParams().height = 10 + random.nextInt(40);
                    bar1.requestLayout();
                    bar2.requestLayout();
                    bar3.requestLayout();
                    bar4.requestLayout();
                    bar5.requestLayout();
                    startWaveAnimation();
                }
            }
        }, 200);

    }

}
















