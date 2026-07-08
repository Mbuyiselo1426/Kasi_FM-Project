package com.kasiefm;
import android.annotation.SuppressLint;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.PlaybackException;
import com.google.android.exoplayer2.Player;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import com.kasiefm.api.KasieApiService;
import com.kasiefm.api.RetrofitClient;
import com.kasiefm.api.Show;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

private ExoPlayer player;
private boolean isPlaying = false;
private Button playPauseButton;
private TextView titleText;
 private TextView artistText;
 private TextView showTitle;
 private TextView showTime;
 private BottomNavigationView bottomNavigation;private TextView liveBadge;
 private static final String RADIO_STREAM_URL = "https://ice1.somafm.com/groovesalad-128-mp3";

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
    initializePlayer();

    playPauseButton.setOnClickListener(v -> {
if (isPlaying) {
pauseRadio();
} else {
playRadio();
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

private void initializePlayer() {
player = new ExoPlayer.Builder(this).build();
MediaItem mediaItem = MediaItem.fromUri(RADIO_STREAM_URL);
player.setMediaItem(mediaItem);

player.addListener(new Player.Listener() {
@Override
public void onPlaybackStateChanged(int playbackState) {
runOnUiThread(() -> {
if (playbackState == Player.STATE_BUFFERING) {
playPauseButton.setEnabled(false);
if (liveBadge != null) {
liveBadge.setText("BUFFERING");
}
} else if (playbackState == Player.STATE_READY) {
playPauseButton.setEnabled(true);
if (liveBadge != null) {
liveBadge.setText("LIVE");
}
}
});
}

@Override
public void onPlayerError(PlaybackException error) {
runOnUiThread(() -> {
playPauseButton.setEnabled(true);
isPlaying = false;
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

player.prepare();
}

private void playRadio() {
if (player != null) {
player.setPlayWhenReady(true);
isPlaying = true;
playPauseButton.setText(" ");
playPauseButton.setContentDescription(getString(R.string.pause_action));

titleText.setText("Kasie FM Live");
artistText.setText("Kasie FM");

if (liveBadge != null) {
liveBadge.setText("LIVE");
}
}
}

private void pauseRadio() {
if (player != null) {
player.setPlayWhenReady(false);
isPlaying = false;
playPauseButton.setText(" ");
playPauseButton.setContentDescription(getString(R.string.play_action));

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