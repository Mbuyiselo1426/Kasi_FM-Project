package com.kasiefm;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.PlaybackException;
import com.google.android.exoplayer2.Player;

/**
 *
 *
 * Owns the ExoPlayer instance and is the single source of truth for playback
 * state. MainActivity binds to this Service and asks it to play/pause instead
 * of holding its own player. Because it's also started as a foreground
 * service (see MainActivity#onStart), it keeps running - and keeps the audio
 * playing - even if the Activity is destroyed or the app is backgrounded.
 *
 * TODO(station): RADIO_STREAM_URL is still the SomaFM placeholder. Once the
 * backend's /api/stream-url is wired in here (Retrofit call before
 * player.prepare()), this constant goes away entirely. That's a separate,
 * later change - it doesn't block this fix.
 */
public class RadioService extends Service {

    /** Callback so the UI can reflect playback state without touching the player. */
    public interface PlaybackStateListener {
        void onStateChanged(String state); // "LIVE" | "BUFFERING" | "PAUSED"
        void onError();
    }

    private final IBinder binder = new LocalBinder();
    private PlaybackStateListener listener;

    private ExoPlayer player;
    private static final String CHANNEL_ID = "RadioServiceChannel";
    private static final int NOTIFICATION_ID = 1;
    private static final String RADIO_STREAM_URL = "https://s44.myradiostream.com/9128/listen.mp3";

    public class LocalBinder extends Binder {
        RadioService getService() {
            return RadioService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        initializePlayer();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Started (not just bound) so the service survives after the Activity unbinds.
        return START_STICKY;
    }

    public void setPlaybackStateListener(PlaybackStateListener listener) {
        this.listener = listener;
    }

    public boolean isPlaying() {
        return player != null && player.getPlayWhenReady();
    }

    /**
     * Called by MainActivity right after binding, so the UI can sync itself to
     * whatever the player is already doing instead of waiting for the next
     * onPlaybackStateChanged - which may never come if nothing changes between
     * you leaving the app and coming back to it.
     */
    public String getCurrentState() {
        if (player == null) {
            return "PAUSED";
        }
        if (!player.getPlayWhenReady()) {
            return "PAUSED";
        }
        if (player.getPlaybackState() == Player.STATE_BUFFERING) {
            return "BUFFERING";
        }
        return "LIVE";
    }

    private void initializePlayer() {
        player = new ExoPlayer.Builder(this).build();

        MediaItem mediaItem = MediaItem.fromUri(RADIO_STREAM_URL);
        player.setMediaItem(mediaItem);
        player.prepare();

        player.addListener(new Player.Listener() {
            @SuppressLint("ForegroundServiceType")
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (playbackState == Player.STATE_BUFFERING) {
                    notifyState("BUFFERING");
                } else if (playbackState == Player.STATE_READY && player.getPlayWhenReady()) {
                    startForeground(NOTIFICATION_ID, createNotification("Kasie FM - Playing"));
                    notifyState("LIVE");
                }
            }

            @Override
            public void onPlayerError(@NonNull PlaybackException error) {
                stopForeground(true);
                if (listener != null) {
                    listener.onError();
                }
            }
        });
    }

    @SuppressLint("ForegroundServiceType")
    public void play() {
        if (player != null) {
            player.setPlayWhenReady(true);
            startForeground(NOTIFICATION_ID, createNotification("Kasie FM - Playing"));
        }
    }

    public void pause() {
        if (player != null) {
            player.setPlayWhenReady(false);
            stopForeground(false);
            notifyState("PAUSED");
        }
    }

    private void notifyState(String state) {
        if (listener != null) {
            listener.onStateChanged(state);
        }
    }

    private Notification createNotification(String content) {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Kasie FM")
                .setContentText(content)
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setOngoing(true)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Radio Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (player != null) {
            player.release();
            player = null;
        }
    }
}
