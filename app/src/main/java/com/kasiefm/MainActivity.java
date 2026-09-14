package com.kasiefm;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.test.espresso.idling.CountingIdlingResource;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.kasiefm.api.Show;

public class MainActivity extends AppCompatActivity {

    private Button playPauseButton;
    private ImageView playPauseIcon;
    private TextView titleText;
    private TextView artistText;
    private BottomNavigationView bottomNavigation;
    private TextView liveBadge;

    /** Called by Home after the API has selected its isCurrent show. */
    public void renderScheduleHero(@Nullable Show currentShow, @NonNull String fallbackTitle,
                                   @NonNull String fallbackTime) {
        TextView heroTitle = findViewById(R.id.hero_show_title);
        TextView heroTime = findViewById(R.id.hero_show_time);
        if (currentShow == null) {
            heroTitle.setText(fallbackTitle);
            heroTime.setText(fallbackTime);
            return;
        }
        heroTitle.setText(currentShow.getName() == null ? "Current show" : currentShow.getName());
        String start = currentShow.getStartTime() == null ? "" : currentShow.getStartTime();
        String end = currentShow.getEndTime() == null ? "" : currentShow.getEndTime();
        heroTime.setText(start + " - " + end);
    }

    // Cached so switching tabs doesn't re-fetch the schedule or rebuild the
    // CONNECT form every time - each tab's Fragment instance is created once
    // and just shown/hidden.
    private Fragment homeFragment;
    private Fragment featuredFragment;
    private Fragment highlightsFragment;
    private Fragment connectFragment;
    private Fragment activeFragment;

    // MainActivity no longer owns an ExoPlayer or a stream URL constant - RadioService
    // owns both. This class only asks the Service to play/pause and reflects whatever
    // state the Service reports back through the PlaybackStateListener callback.
    private RadioService radioService;
    private boolean serviceBound = false;

    // Lets Espresso tests wait for RadioService to actually bind instead of
    // racing the async bindService() callback with a sleep.
    private static final CountingIdlingResource BINDING_IDLING_RESOURCE =
            new CountingIdlingResource("radioServiceBinding");

    @VisibleForTesting
    public static CountingIdlingResource getIdlingResource() {
        return BINDING_IDLING_RESOURCE;
    }

    // Reflects RadioService's actual playWhenReady state rather than the UI
    // labels, which only update synchronously on the pause() path - checking
    // this directly is what proves a tap was actually routed to the service.
    @VisibleForTesting
    public boolean isPlayingForTesting() {
        return serviceBound && radioService != null && radioService.isPlaying();
    }

    @VisibleForTesting
    @Nullable
    public String getPlaybackStateForTesting() {
        return serviceBound && radioService != null ? radioService.getCurrentState() : null;
    }

    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            RadioService.LocalBinder binder = (RadioService.LocalBinder) service;
            radioService = binder.getService();
            serviceBound = true;

            // Sync immediately: if we're rebinding to a service that's already
            // LIVE (or already PAUSED), there's no new state transition coming to
            // trigger the listener below, so we'd otherwise be stuck showing
            // whatever the UI happened to say before this Activity was recreated.
            updateUiForState(radioService.getCurrentState());
            if (!BINDING_IDLING_RESOURCE.isIdleNow()) {
                BINDING_IDLING_RESOURCE.decrement();
            }

            radioService.setPlaybackStateListener(new RadioService.PlaybackStateListener() {
                @Override
                public void onStateChanged(String state) {
                    runOnUiThread(() -> updateUiForState(state));
                }

                @Override
                public void onError() {
                    runOnUiThread(MainActivity.this::showPlaybackError);
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
        playPauseIcon = findViewById(R.id.playPauseIcon);
        titleText = findViewById(R.id.title_text);
        artistText = findViewById(R.id.artist_text);
        liveBadge = findViewById(R.id.live_badge);

        bottomNavigation = findViewById(R.id.bottom_navigation);

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

        if (savedInstanceState == null) {
            // Fresh start: fragment_container in activity_main.xml already declares
            // HomeFragment via android:name, so the FragmentManager has already
            // created and shown it - just record it as the active one instead of
            // adding a second copy.
            homeFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
            activeFragment = homeFragment;
        } else {
            // Fragment restored by the system after a config change - reattach by tag.
            homeFragment = getSupportFragmentManager().findFragmentByTag("home");
            featuredFragment = getSupportFragmentManager().findFragmentByTag("featured");
            highlightsFragment = getSupportFragmentManager().findFragmentByTag("highlights");
            connectFragment = getSupportFragmentManager().findFragmentByTag("connect");
        }

        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                if (homeFragment == null) {
                    homeFragment = new HomeFragment();
                }
                showFragment(homeFragment, "home");
                return true;
            } else if (itemId == R.id.nav_featured) {
                if (featuredFragment == null) {
                    featuredFragment = SimpleContentFragment.newInstance(
                            "FEATURED SEGMENT", "Best of Kasie FM", "Highlights from past shows");
                }
                showFragment(featuredFragment, "featured");
                return true;
            } else if (itemId == R.id.nav_highlights) {
                if (highlightsFragment == null) {
                    highlightsFragment = SimpleContentFragment.newInstance(
                            "TODAY'S HIGHLIGHTS", "Top Moments", "The best bits from today");
                }
                showFragment(highlightsFragment, "highlights");
                return true;
            } else if (itemId == R.id.nav_connect) {
                if (connectFragment == null) {
                    connectFragment = new ConnectFragment();
                }
                showFragment(connectFragment, "connect");
                return true;
            }

            return false;
        });
    }

    /**
     * Swaps the visible tab content Fragment without touching titleText/
     * artistText/liveBadge - those stay Activity-level and are driven only
     * by RadioService's playback state, never by which tab is selected.
     * This is the fix for the old bug where tab-switching and playback
     * state used to fight over the same TextViews.
     */
    private void showFragment(Fragment fragment, String tag) {
        if (activeFragment == fragment) {
            return;
        }
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction transaction = fm.beginTransaction();
        if (!fragment.isAdded()) {
            transaction.add(R.id.fragment_container, fragment, tag);
        }
        for (Fragment existing : new Fragment[]{homeFragment, featuredFragment, highlightsFragment, connectFragment}) {
            if (existing != null && existing != fragment && existing.isAdded()) {
                transaction.hide(existing);
            }
        }
        transaction.show(fragment);
        transaction.commit();
        activeFragment = fragment;
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Started so the Service (and its player) survives beyond this Activity's
        // lifecycle, and bound so this Activity can call play()/pause() on it and
        // receive state callbacks while it's in the foreground.
        Intent intent = new Intent(this, RadioService.class);
        ContextCompat.startForegroundService(this, intent);
        BINDING_IDLING_RESOURCE.increment();
        bindService(intent, connection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (serviceBound) {
            radioService.setPlaybackStateListener(null);
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
                playPauseIcon.setImageResource(R.drawable.ic_pause);
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
                playPauseIcon.setImageResource(R.drawable.ic_play);
                titleText.setText("Paused");
                artistText.setText("Tap play to resume");
                break;
            case "ERROR":
                showPlaybackError();
                break;
        }
    }

    private void showPlaybackError() {
        playPauseButton.setEnabled(true);
        playPauseButton.setText(" ");
        playPauseButton.setContentDescription(getString(R.string.play_action));
        playPauseIcon.setImageResource(R.drawable.ic_play);
        if (liveBadge != null) {
            liveBadge.setText("OFFLINE");
        }
        titleText.setText(getString(R.string.network_error_message));
        artistText.setText("Tap to retry");
    }
}
