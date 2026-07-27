package com.kasiefm;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.PlaybackException;
import com.google.android.exoplayer2.Player;
import com.kasiefm.api.KasieApiService;
import com.kasiefm.api.RetrofitClient;
import com.kasiefm.api.StreamUrls;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 *
 *
 * Owns the ExoPlayer instance and is the single source of truth for playback
 * state. MainActivity binds to this Service and asks it to play/pause instead
 * of holding its own player. Because it's also started as a foreground
 * service (see MainActivity#onStart), it keeps running - and keeps the audio
 * playing - even if the Activity is destroyed or the app is backgrounded.
 */
public class RadioService extends Service {

    private static final String TAG = "RadioService";

    /** Callback so the UI can reflect playback state without touching the player. */
    public interface PlaybackStateListener {
        void onStateChanged(String state); // "LIVE" | "BUFFERING" | "PAUSED"
        void onError();
    }

    private final IBinder binder = new LocalBinder();
    private PlaybackStateListener listener;

    private ExoPlayer player;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private static final String CHANNEL_ID = "RadioServiceChannel";
    private static final int NOTIFICATION_ID = 1;
    private static final String RADIO_STREAM_URL = "https://s44.myradiostream.com/9128/listen.mp3";

    // Bounded auto-reconnect: a few short-backoff attempts before we give up
    // and surface onError to the UI.
    private static final int MAX_RECONNECT_ATTEMPTS = 3;
    private static final long RECONNECT_BASE_DELAY_MS = 2000L;
    private int reconnectAttempts = 0;

    // Whatever URL we last successfully resolved (API or fallback constant) -
    // reconnect attempts reuse this instead of always going back to the constant.
    private volatile String currentStreamUrl = RADIO_STREAM_URL;

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

        player.addListener(new Player.Listener() {
            @SuppressLint("ForegroundServiceType")
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (playbackState == Player.STATE_BUFFERING) {
                    notifyState("BUFFERING");
                } else if (playbackState == Player.STATE_READY && player.getPlayWhenReady()) {
                    reconnectAttempts = 0;
                    startForeground(NOTIFICATION_ID, createNotification("Kasie FM - Playing"));
                    notifyState("LIVE");
                }
            }

            @Override
            public void onPlayerError(@NonNull PlaybackException error) {
                Log.w(TAG, "Player error, will attempt reconnect: " + error.getMessage(), error);
                stopForeground(true);
                attemptReconnect();
            }
        });

        // Resolve the stream URL (API first, constant fallback) before we ever
        // touch setMediaItem/prepare - see resolveStreamUrlAndPrepare().
        resolveStreamUrlAndPrepare();
    }

    /**
     * Fetches /api/stream-url off the main thread; falls back to the hardcoded
     * constant on failure or an empty response. Either way, the actual player
     * calls are hopped back onto the main thread since ExoPlayer must be
     * driven from the thread it was created on.
     */
    private void resolveStreamUrlAndPrepare() {
        KasieApiService api = RetrofitClient.getInstance().create(KasieApiService.class);
        api.getStreamUrl().enqueue(new Callback<StreamUrls>() {
            @Override
            public void onResponse(@NonNull Call<StreamUrls> call, @NonNull Response<StreamUrls> response) {
                StreamUrls body = response.body();
                String url = (body != null && body.getStreamUrl() != null && !body.getStreamUrl().trim().isEmpty())
                        ? body.getStreamUrl()
                        : RADIO_STREAM_URL;
                Log.i(TAG, "Resolved stream URL from API: " + url);
                mainHandler.post(() -> prepareMedia(url));
            }

            @Override
            public void onFailure(@NonNull Call<StreamUrls> call, @NonNull Throwable t) {
                Log.w(TAG, "Failed to fetch stream URL from API, falling back to default", t);
                mainHandler.post(() -> prepareMedia(RADIO_STREAM_URL));
            }
        });
    }

    /** Sets up the given URL and prepares the player, but does NOT auto-play it. */
    private void prepareMedia(String url) {
        if (player == null) {
            return;
        }
        currentStreamUrl = url;
        Log.i(TAG, "Preparing stream: " + url);
        player.setMediaItem(MediaItem.fromUri(url));
        player.prepare();
        // Explicit: ExoPlayer's playWhenReady defaults to true, which would
        // otherwise auto-buffer/play before the user ever taps play - and
        // disable playPauseButton (see MainActivity#updateUiForState) while
        // it does. Stay paused until the user asks for playback.
        player.setPlayWhenReady(false);
    }

    private void attemptReconnect() {
        if (reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
            reconnectAttempts++;
            long delay = RECONNECT_BASE_DELAY_MS * (1L << (reconnectAttempts - 1));
            Log.i(TAG, "Reconnect attempt " + reconnectAttempts + "/" + MAX_RECONNECT_ATTEMPTS
                    + " in " + delay + "ms");
            notifyState("BUFFERING");
            mainHandler.postDelayed(() -> {
                if (player == null) {
                    return;
                }
                player.setMediaItem(MediaItem.fromUri(currentStreamUrl));
                player.prepare();
                player.setPlayWhenReady(true);
            }, delay);
        } else {
            Log.e(TAG, "Reconnect attempts exhausted, surfacing error to UI");
            reconnectAttempts = 0;
            if (listener != null) {
                listener.onError();
            }
        }
    }

    @SuppressLint("ForegroundServiceType")
    public void play() {
        if (player == null) {
            return;
        }
        Log.i(TAG, "play() requested");
        if (player.getPlaybackState() == Player.STATE_IDLE) {
            // Player landed in IDLE after an exhausted reconnect (or was never
            // prepared yet) - this is the tap-to-retry path, re-prepare first.
            reconnectAttempts = 0;
            player.setMediaItem(MediaItem.fromUri(currentStreamUrl));
            player.prepare();
        }
        player.setPlayWhenReady(true);
        startForeground(NOTIFICATION_ID, createNotification("Kasie FM - Playing"));
    }

    public void pause() {
        if (player != null) {
            Log.i(TAG, "pause() requested");
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

    /** Swiping the app away from recents should stop playback, not leave it running. */
    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        Log.i(TAG, "onTaskRemoved: stopping playback and foreground service");
        mainHandler.removeCallbacksAndMessages(null);
        if (player != null) {
            player.stop();
        }
        stopForeground(true);
        stopSelf();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mainHandler.removeCallbacksAndMessages(null);
        if (player != null) {
            player.release();
            player = null;
        }
    }
}
