package com.kasiefm;
import android.widget.SeekBar;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.PlaybackException;
import com.google.android.exoplayer2.Player;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private ExoPlayer player;
    private boolean isPlaying = false;
    private Button playPauseButton;
    private TextView statusText;
    private View bar1, bar2, bar3, bar4, bar5;
    private Handler animHandler = new Handler();
    private Random random = new Random();

    private static final String RADIO_STREAM_URL = "https://ice1.somafm.com/groovesalad-128-mp3";
    private TextView nowPlayingTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //added Actionbar
        // Hide the ActionBar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        playPauseButton = findViewById(R.id.play_pause_button);
        nowPlayingTitle = findViewById(R.id.now_playing_title); //added nowPlayingTitle
        statusText = findViewById(R.id.status_text);

        // Initialize animated bars
        bar1 = findViewById(R.id.bar1);
        bar2 = findViewById(R.id.bar2);
        bar3 = findViewById(R.id.bar3);
        bar4 = findViewById(R.id.bar4);
        bar5 = findViewById(R.id.bar5);

        initializePlayer();

        // Volume Slider
        SeekBar volumeSlider = findViewById(R.id.volume_slider);
        volumeSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && player != null) {
                    float volume = progress / 100f;
                    player.setVolume(volume);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });


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
                        nowPlayingTitle.setText("Loading stream...");

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
            nowPlayingTitle.setText("Kasie FM Live");// added nowPlayingTitle.setText
            startWaveAnimation();  // Start animation when playing



        }
    }

    private void pauseRadio() {
        if (player != null) {
            player.setPlayWhenReady(false);
            isPlaying = false;
            playPauseButton.setText("Play");
            statusText.setText("Paused");
            nowPlayingTitle.setText("Paused"); // added nowPlayingTitle.setText
            stopWaveAnimation();  // Stop animation when paused

        }
    }

    private void startWaveAnimation() {
        animHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isPlaying) {
                    // Random heights for animation
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

                    startWaveAnimation(); // Continue animation
                }
            }
        }, 200);
    }

    private void stopWaveAnimation() {
        animHandler.removeCallbacksAndMessages(null);
        // Reset to default heights
        bar1.getLayoutParams().height = 10;
        bar2.getLayoutParams().height = 20;
        bar3.getLayoutParams().height = 30;
        bar4.getLayoutParams().height = 20;
        bar5.getLayoutParams().height = 10;
        bar1.requestLayout();
        bar2.requestLayout();
        bar3.requestLayout();
        bar4.requestLayout();
        bar5.requestLayout();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (player != null) {
            player.release();
            player = null;
        }
        stopWaveAnimation();
    }
}