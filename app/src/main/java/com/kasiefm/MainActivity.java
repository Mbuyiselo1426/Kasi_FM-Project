package com.kasiefm;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import com.kasiefm.api.KasieApiService;
import com.kasiefm.api.RetrofitClient;
import com.kasiefm.api.Show;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private Button playPauseButton;
    private TextView titleText;
    private TextView artistText;
    private TextView showTitle;
    private TextView showTime;
    private BottomNavigationView bottomNavigation;
    private TextView liveBadge;

    // MainActivity no longer owns an ExoPlayer or a stream URL constant - RadioService
    // owns both. This class only asks the Service to play/pause and reflects whatever
    // state the Service reports back through the PlaybackStateListener callback.
    private RadioService radioService;
    private boolean serviceBound = false;

    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            RadioService.LocalBinder binder = (RadioService.LocalBinder) service;
            radioService = binder.getService();
            serviceBound = true;

            radioService.setPlaybackStateListener(new RadioService.PlaybackStateListener() {
                @Override
                public void onStateChanged(String state) {
                    runOnUiThread(() -> updateUiForState(state));
                }

                @Override
                public void onError() {
                    runOnUiThread(() -> {
                        playPauseButton.setEnabled(true);
                        playPauseButton.setText(" ");
                        playPauseButton.setContentDescription(getString(R.string.play_action));
                        if (liveBadge != null) {
                            liveBadge.setText("OFFLINE");
                        }
                        titleText.setText("Connection Error");
                        artistText.setText("Tap to retry");
                    });
                }
            });
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            radioService = null;
            serviceBound = false;
        }
    };

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        playPauseButton = findViewById(R.id.playPauseButton);
        titleText = findViewById(R.id.title_text);
        artistText = findViewById(R.id.artist_text);
        showTitle = findViewById(R.id.show_title);
        showTime = findViewById(R.id.show_time);
        liveBadge = findViewById(R.id.live_badge);

        bottomNavigation = findViewById(R.id.bottom_navigation);

        loadSchedule();

        playPauseButton.setOnClickListener(v -> {
            if (!serviceBound || radioService == null) {
                return; // service hasn't bound yet - button briefly no-ops rather than crashing
            }
            if (radioService.isPlaying()) {
                radioService.pause();
            } else {
                radioService.play();
            }
        });

        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                titleText.setText("Kasie FM Live");
                artistText.setText("Kasie FM");
                loadSchedule();
                return true;
            } else if (itemId == R.id.nav_featured) {
                titleText.setText("Featured Segment");
                artistText.setText("Best of Kasie FM");
                showTitle.setText("#Throwback");
                showTime.setText("18:00 - 20:00");
                return true;
            } else if (itemId == R.id.nav_highlights) {
                titleText.setText("Today's Highlights");
                artistText.setText("Top Moments");
                showTitle.setText("#Highlights");
                showTime.setText("20:00 - 22:00");
                return true;
            } else if (itemId == R.id.nav_news) {
                titleText.setText("Kasie FM News");
                artistText.setText("Community Updates");
                showTitle.setText("#NewsHour");
                showTime.setText("22:00 - 00:00");
                return true;
            }

            return false;
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Started so the Service (and its player) survives beyond this Activity's
        // lifecycle, and bound so this Activity can call play()/pause() on it and
        // receive state callbacks while it's in the foreground.
        Intent intent = new Intent(this, RadioService.class);
        ContextCompat.startForegroundService(this, intent);
        bindService(intent, connection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (serviceBound) {
            unbindService(connection);
            serviceBound = false;
            // Deliberately NOT calling stopService() here - that's the whole point:
            // audio keeps playing in the foreground service after the Activity stops.
        }
    }

    private void updateUiForState(String state) {
        switch (state) {
            case "LIVE":
                playPauseButton.setEnabled(true);
                playPauseButton.setText(" ");
                playPauseButton.setContentDescription(getString(R.string.pause_action));
                titleText.setText("Kasie FM Live");
                artistText.setText("Kasie FM");
                if (liveBadge != null) liveBadge.setText("LIVE");
                break;
            case "BUFFERING":
                playPauseButton.setEnabled(false);
                if (liveBadge != null) liveBadge.setText("BUFFERING");
                break;
            case "PAUSED":
                playPauseButton.setEnabled(true);
                playPauseButton.setText(" ");
                playPauseButton.setContentDescription(getString(R.string.play_action));
                titleText.setText("Paused");
                artistText.setText("Tap play to resume");
                break;
        }
    }

    private void loadSchedule() {
        showTitle.setText("Loading schedule...");
        showTime.setText("");

        KasieApiService api = RetrofitClient.getInstance().create(KasieApiService.class);

        api.getSchedule().enqueue(new Callback<List<Show>>() {
            @Override
            public void onResponse(Call<List<Show>> call, Response<List<Show>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    Show current = response.body().get(0);
                    showTitle.setText(current.getName());
                    showTime.setText(current.getStartTime() + " - " + current.getEndTime());
                } else {
                    showTitle.setText("Schedule unavailable");
                    showTime.setText("");
                }
            }

            @Override
            public void onFailure(Call<List<Show>> call, Throwable t) {
                showTitle.setText("Unable to load schedule");
                showTime.setText("Check your connection");
            }
        });
    }
}