package com.kasiefm;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
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
    private View homeLiveHero;

    private String communityShowTitle = "Kasie FM Live";
    private String communityShowTime = "";

    /** Called by Home after the API has selected its isCurrent show. */
    public void renderScheduleHero(@Nullable Show currentShow, @NonNull String fallbackTitle,
                                   @NonNull String fallbackTime) {
        TextView heroTitle = findViewById(R.id.hero_show_title);
        TextView heroTime = findViewById(R.id.hero_show_time);
        if (currentShow == null) {
            heroTitle.setText(fallbackTitle);
            heroTime.setText(fallbackTime);
            updateCommunityShow("Kasie FM Live", "");
            return;
        }
        String showName = currentShow.getName() == null ? "Current show" : currentShow.getName();
        String start = currentShow.getStartTime() == null ? "" : currentShow.getStartTime();
        String end = currentShow.getEndTime() == null ? "" : currentShow.getEndTime();
        String showTime = start + " - " + end;
        heroTitle.setText(showName);
        heroTime.setText(showTime);
        updateCommunityShow(currentShow.getName() == null ? "Kasie FM Live" : showName, showTime);
    }

    // Cached so switching tabs doesn't re-fetch the schedule or rebuild the
    // CONNECT form every time - each tab's Fragment instance is created once
    // and just shown/hidden.
    private Fragment homeFragment;
    private Fragment featuredFragment;
    private Fragment highlightsFragment;
    private PodcastFragment podcastsFragment;
    private ConnectFragment connectFragment;
    private AllShowsFragment allShowsFragment;
    private ShowDetailsFragment showDetailsFragment;
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
        homeLiveHero = findViewById(R.id.home_live_hero);

        bottomNavigation = findViewById(R.id.bottom_navigation);

        findViewById(R.id.btn_call_studio).setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            openStudioDialer();
        });

        playPauseButton.setOnClickListener(v -> {
            if (!serviceBound || radioService == null) {
                return; // service hasn't bound yet - button briefly no-ops rather than crashing
            }
            if (radioService.isPlaying()) {
                radioService.pause();
            } else {
                pausePodcastPlayback();
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
            Fragment restoredPodcasts = getSupportFragmentManager().findFragmentByTag("podcasts");
            if (restoredPodcasts instanceof PodcastFragment) {
                podcastsFragment = (PodcastFragment) restoredPodcasts;
            }
            Fragment restoredConnect = getSupportFragmentManager().findFragmentByTag("connect");
            if (restoredConnect instanceof ConnectFragment) {
                connectFragment = (ConnectFragment) restoredConnect;
            }
            Fragment restoredAllShows = getSupportFragmentManager().findFragmentByTag("all_shows");
            if (restoredAllShows instanceof AllShowsFragment) {
                allShowsFragment = (AllShowsFragment) restoredAllShows;
                if (!allShowsFragment.isHidden()) {
                    activeFragment = allShowsFragment;
                }
            }
            Fragment restoredShowDetails = getSupportFragmentManager().findFragmentByTag("show_details");
            if (restoredShowDetails instanceof ShowDetailsFragment) {
                showDetailsFragment = (ShowDetailsFragment) restoredShowDetails;
                if (!showDetailsFragment.isHidden()) {
                    activeFragment = showDetailsFragment;
                }
            }
        }

        // A recreated Activity can restore Community as the visible fragment before
        // the user makes another navigation selection.
        if (connectFragment != null && connectFragment.isAdded() && !connectFragment.isHidden()) {
            activeFragment = connectFragment;
            updateHomeHeroVisibility(connectFragment);
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
                if (podcastsFragment == null) {
                    podcastsFragment = new PodcastFragment();
                }
                showFragment(podcastsFragment, "podcasts");
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

    private void openStudioDialer() {
        Intent dialIntent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:+27102360002"));
        if (dialIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(dialIntent);
        } else {
            Toast.makeText(this, "Unable to open phone dialer", Toast.LENGTH_SHORT).show();
        }
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
            updateHomeHeroVisibility(fragment);
            return;
        }
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction transaction = fm.beginTransaction();
        if (!fragment.isAdded()) {
            transaction.add(R.id.fragment_container, fragment, tag);
        }
        for (Fragment existing : new Fragment[]{homeFragment, featuredFragment, highlightsFragment,
                podcastsFragment, connectFragment, allShowsFragment, showDetailsFragment}) {
            if (existing != null && existing != fragment && existing.isAdded()) {
                transaction.hide(existing);
            }
        }
        transaction.show(fragment);
        transaction.commit();
        activeFragment = fragment;
        updateHomeHeroVisibility(fragment);
    }

    /** The large hero belongs to Home; Community uses its own compact service-backed player. */
    private void updateHomeHeroVisibility(Fragment destination) {
        if (homeLiveHero != null) {
            homeLiveHero.setVisibility(destination == homeFragment ? View.VISIBLE : View.GONE);
        }
    }

    /** Opens the Home-owned all-shows route without changing the selected bottom-nav tab. */
    public void openAllShows() {
        if (allShowsFragment == null) {
            allShowsFragment = new AllShowsFragment();
        }
        if (activeFragment == allShowsFragment) {
            return;
        }
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        if (activeFragment != null && activeFragment.isAdded()) {
            transaction.hide(activeFragment);
        }
        if (!allShowsFragment.isAdded()) {
            transaction.add(R.id.fragment_container, allShowsFragment, "all_shows");
        }
        transaction.show(allShowsFragment).commit();
        activeFragment = allShowsFragment;
    }

    /** Returns from the all-shows route to the already-cached Home fragment. */
    public void returnHomeFromAllShows() {
        if (homeFragment == null) {
            homeFragment = new HomeFragment();
        }
        showFragment(homeFragment, "home");
        bottomNavigation.setSelectedItemId(R.id.nav_home);
    }

    /** Opens details for one radio-show ID; the fragment fetches its own source data. */
    public void openShowDetails(long showId) {
        showDetailsFragment = ShowDetailsFragment.newInstance(showId);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        if (activeFragment != null && activeFragment.isAdded()) {
            transaction.hide(activeFragment);
        }
        transaction.add(R.id.fragment_container, showDetailsFragment, "show_details");
        transaction.show(showDetailsFragment).commit();
        activeFragment = showDetailsFragment;
    }

    /** Returns from a transient show-details route to the already-loaded all-shows list. */
    public void returnToAllShowsFromDetails() {
        if (allShowsFragment == null) {
            allShowsFragment = new AllShowsFragment();
        }
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        if (showDetailsFragment != null && showDetailsFragment.isAdded()) {
            transaction.remove(showDetailsFragment);
        }
        if (!allShowsFragment.isAdded()) {
            transaction.add(R.id.fragment_container, allShowsFragment, "all_shows");
        }
        transaction.show(allShowsFragment).commit();
        showDetailsFragment = null;
        activeFragment = allShowsFragment;
    }

    /** Called before podcast playback starts so the app never plays two streams at once. */
    public void pauseLiveRadioForPodcast() {
        if (serviceBound && radioService != null && radioService.isPlaying()) {
            radioService.pause();
        }
    }

    private void pausePodcastPlayback() {
        if (podcastsFragment != null) {
            podcastsFragment.pausePlayback();
        }
    }

    /** Uses the existing RadioService player for Community's compact control. */
    public void toggleLiveRadioFromCommunity() {
        if (!serviceBound || radioService == null) {
            return;
        }
        if (radioService.isPlaying()) {
            radioService.pause();
        } else {
            pausePodcastPlayback();
            radioService.play();
        }
    }

    public String getCommunityShowTitle() {
        return communityShowTitle;
    }

    public String getCommunityShowTime() {
        return communityShowTime;
    }

    private void updateCommunityShow(String title, String time) {
        communityShowTitle = title == null || title.trim().isEmpty() ? "Kasie FM Live" : title;
        communityShowTime = time == null ? "" : time;
        if (connectFragment != null) {
            connectFragment.renderLiveMiniPlayer(communityShowTitle, communityShowTime,
                    getPlaybackStateForTesting());
        }
    }

    private void updateCommunityPlaybackState(String state) {
        if (connectFragment != null) {
            connectFragment.renderLiveMiniPlayer(communityShowTitle, communityShowTime, state);
        }
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
                return;
        }
        updateCommunityPlaybackState(state);
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
        updateCommunityPlaybackState("ERROR");
    }
}
