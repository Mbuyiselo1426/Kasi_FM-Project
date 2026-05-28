package com.kasiefm;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.PlaybackException;
import com.google.android.exoplayer2.Player;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class MainActivity extends AppCompatActivity {

    private ExoPlayer player;
    private boolean isPlaying = false;
    private FloatingActionButton playPauseButton;  // Changed from Button to FAB
    private TextView titleText;
    private TextView artistText;
    private TextView showTitle;
    private TextView showTime;
    private TextView navHome, navFeatured, navHighlights, navNews;
    private TextView liveBadge;  // Optional: to change LIVE badge text/color

    private static final String RADIO_STREAM_URL = "https://ice1.somafm.com/groovesalad-128-mp3";

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Setup Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        // Initialize views - UPDATED for modern design
        playPauseButton = findViewById(R.id.playPauseButton);  // Changed ID to match XML
        titleText = findViewById(R.id.title_text);
        artistText = findViewById(R.id.artist_text);
        showTitle = findViewById(R.id.show_title);
        showTime = findViewById(R.id.show_time);
        liveBadge = findViewById(R.id.live_badge);  // Optional: add this to your XML

        // Navigation TextViews
        navHome = findViewById(R.id.nav_home);
        navFeatured = findViewById(R.id.nav_featured);
        navHighlights = findViewById(R.id.nav_highlights);
        navNews = findViewById(R.id.nav_news);

        // Set initial schedule
        showTitle.setText("#HomeDrive");
        showTime.setText("15:00 - 18:00");

        initializePlayer();

        // Modern play button click listener
        playPauseButton.setOnClickListener(v -> {
            if (isPlaying) {
                pauseRadio();
            } else {
                playRadio();
            }
        });

        // Navigation clicks (keeping your existing logic)
        navHome.setOnClickListener(v -> {
            titleText.setText("Kasie FM Live");
            artistText.setText("Kasie FM");
            showTitle.setText("#HomeDrive");
            showTime.setText("15:00 - 18:00");
            updateNavigationColors(R.id.nav_home);
        });

        navFeatured.setOnClickListener(v -> {
            titleText.setText("Featured Segment");
            artistText.setText("Best of Kasie FM");
            showTitle.setText("#Throwback");
            showTime.setText("18:00 - 20:00");
            updateNavigationColors(R.id.nav_featured);
        });

        navHighlights.setOnClickListener(v -> {
            titleText.setText("Today's Highlights");
            artistText.setText("Top Moments");
            showTitle.setText("#Highlights");
            showTime.setText("20:00 - 22:00");
            updateNavigationColors(R.id.nav_highlights);
        });

        navNews.setOnClickListener(v -> {
            titleText.setText("Kasie FM News");
            artistText.setText("Community Updates");
            showTitle.setText("#NewsHour");
            showTime.setText("22:00 - 00:00");
            updateNavigationColors(R.id.nav_news);
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
                        // Change FAB icon to buffering state (optional)
                        playPauseButton.setEnabled(false);
                        if (liveBadge != null) {
                            liveBadge.setText("BUFFERING");
                            liveBadge.setBackgroundColor(getColor(android.R.color.holo_orange_dark));
                        }
                    } else if (playbackState == Player.STATE_READY) {
                        playPauseButton.setEnabled(true);
                        if (liveBadge != null) {
                            liveBadge.setText("LIVE");
                            liveBadge.setBackgroundColor(getColor(android.R.color.holo_orange_dark));
                        }
                    }
                });
            }

            @Override
            public void onPlayerError(PlaybackException error) {
                runOnUiThread(() -> {
                    playPauseButton.setEnabled(true);
                    isPlaying = false;
                    playPauseButton.setImageResource(R.drawable.ic_play_arrow_24dp);
                    if (liveBadge != null) {
                        liveBadge.setText("OFFLINE");
                        liveBadge.setBackgroundColor(getColor(android.R.color.holo_red_dark));
                    }
                    titleText.setText("Connection Error");
                    artistText.setText("Tap to retry");
                });
            }
        });

        player.prepare();
    }

    private void playRadio() {
        if (player != null) {
            player.setPlayWhenReady(true);
            isPlaying = true;
            // Change FAB icon to pause
            playPauseButton.setImageResource(R.drawable.ic_pause_24dp);
            // Add scale animation
            playPauseButton.animate()
                    .scaleX(1.1f)
                    .scaleY(1.1f)
                    .setDuration(200)
                    .withEndAction(() -> playPauseButton.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(200)
                            .start());

            titleText.setText("Kasie FM Live");
            artistText.setText("Kasie FM");

            // Optional: Change LIVE badge to animating state
            if (liveBadge != null) {
                liveBadge.setText("LIVE");
            }
        }
    }

    private void pauseRadio() {
        if (player != null) {
            player.setPlayWhenReady(false);
            isPlaying = false;
            // Change FAB icon to play
            playPauseButton.setImageResource(R.drawable.ic_play_arrow_24dp);

            titleText.setText("Paused");
            artistText.setText("Tap play to resume");
        }
    }

    private void updateNavigationColors(int activeNavId) {
        // Reset all navigation colors to gray
        navHome.setTextColor(getColor(android.R.color.darker_gray));
        navFeatured.setTextColor(getColor(android.R.color.darker_gray));
        navHighlights.setTextColor(getColor(android.R.color.darker_gray));
        navNews.setTextColor(getColor(android.R.color.darker_gray));

        // Set active navigation color to pink (E91E63) or sky blue (87CEEB)
        int activeColor = getColor(android.R.color.white); // or use a custom color

        if (activeNavId == R.id.nav_home) {
            navHome.setTextColor(activeColor);
        } else if (activeNavId == R.id.nav_featured) {
            navFeatured.setTextColor(activeColor);
        } else if (activeNavId == R.id.nav_highlights) {
            navHighlights.setTextColor(activeColor);
        } else if (activeNavId == R.id.nav_news) {
            navNews.setTextColor(activeColor);
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