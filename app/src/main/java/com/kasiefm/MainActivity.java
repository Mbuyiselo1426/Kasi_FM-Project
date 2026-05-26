package com.kasiefm;

import android.os.Bundle;
import android.os.Handler;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.PlaybackException;
import com.google.android.exoplayer2.Player;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private ExoPlayer player;
    private boolean isPlaying = false;
    private Button listenNowButton;
    private TextView titleText;
    private TextView artistText;
    private TextView showTitle;
    private TextView showTime;
    private Handler animHandler = new Handler();
    private Random random = new Random();

    private static final String RADIO_STREAM_URL = "https://ice1.somafm.com/groovesalad-128-mp3";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Setup Toolbar (Top Navbar)
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Hide default ActionBar title, we set our own in toolbar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        // Initialize views
        listenNowButton = findViewById(R.id.play_pause_button);
        titleText = findViewById(R.id.title_text);
        artistText = findViewById(R.id.artist_text);
        showTitle = findViewById(R.id.show_title);
        showTime = findViewById(R.id.show_time);

        // Set initial schedule info
        showTitle.setText("#HomeDrive");
        showTime.setText("15:00 - 18:00");

        initializePlayer();

        // Listen Now button
        listenNowButton.setOnClickListener(v -> {
            if (isPlaying) {
                pauseRadio();
            } else {
                playRadio();
            }
        });

        // Bottom Navigation
        BottomNavigationView bottomNavigation = findViewById(R.id.bottom_navigation);
        bottomNavigation.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();

                if (id == R.id.nav_home) {
                    titleText.setText("Kasie FM Live");
                    artistText.setText("Kasie FM");
                    showTitle.setText("#HomeDrive");
                    showTime.setText("15:00 - 18:00");
                    return true;
                } else if (id == R.id.nav_featured) {
                    titleText.setText("Featured Segment");
                    artistText.setText("Best of Kasie FM");
                    showTitle.setText("#Throwback");
                    showTime.setText("18:00 - 20:00");
                    return true;
                } else if (id == R.id.nav_highlights) {
                    titleText.setText("Today's Highlights");
                    artistText.setText("Top Moments");
                    showTitle.setText("#Highlights");
                    showTime.setText("20:00 - 22:00");
                    return true;
                } else if (id == R.id.nav_news) {
                    titleText.setText("Kasie FM News");
                    artistText.setText("Community Updates");
                    showTitle.setText("#NewsHour");
                    showTime.setText("22:00 - 00:00");
                    return true;
                }
                return false;
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
                        listenNowButton.setText("BUFFERING...");
                    } else if (playbackState == Player.STATE_READY) {
                        listenNowButton.setText("LISTEN NOW");
                    }
                });


            @Override
            public void onPlayerError(PlaybackException error) {
                runOnUiThread(() -> {
                    listenNowButton.setText("ERROR - TAP TO RETRY");
                    isPlaying = false;
                });
            }
        });

        player.prepare();
    }

    private void playRadio() {
        if (player != null) {
            player.setPlayWhenReady(true);
            isPlaying = true;
            listenNowButton.setText("PAUSE");
            titleText.setText("Kasie FM Live");
            artistText.setText("Kasie FM");
        }
    }

    private void pauseRadio() {
        if (player != null) {
            player.setPlayWhenReady(false);
            isPlaying = false;
            listenNowButton.setText("LISTEN NOW");
            titleText.setText("Paused");
            artistText.setText("Tap play to resume");
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