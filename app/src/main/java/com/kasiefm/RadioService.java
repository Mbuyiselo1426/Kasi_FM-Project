package com.kasiefm;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

// ExoPlayer 2.x imports (replaces androidx.media3)
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.PlaybackException;
import com.google.android.exoplayer2.Player;

public class RadioService extends Service {

    private ExoPlayer player;
    private static final String CHANNEL_ID = "RadioServiceChannel";
    private static final int NOTIFICATION_ID = 1;
    private static final String RADIO_STREAM_URL = "https://ice1.somafm.com/groovesalad-128-mp3";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        initializePlayer();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();

        if (action != null) {
            switch (action) {
                case "PLAY":
                    startRadio();
                    break;
                case "PAUSE":
                    pauseRadio();
                    break;
                case "STOP":
                    stopRadio();
                    break;
            }
        }

        return START_STICKY;
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
                if (playbackState == Player.STATE_READY) {
                    startForeground(NOTIFICATION_ID, createNotification("Kasie FM - Playing"));
                }
            }

            @Override
            public void onPlayerError(@NonNull PlaybackException error) {
                stopForeground(true);
                stopSelf();
            }
        });
    }

    @SuppressLint("ForegroundServiceType")
    private void startRadio() {
        if (player != null) {
            player.setPlayWhenReady(true);
            startForeground(NOTIFICATION_ID, createNotification("Kasie FM - Playing"));
        }
    }

    private void pauseRadio() {
        if (player != null) {
            player.setPlayWhenReady(false);
            stopForeground(false);
        }
    }

    private void stopRadio() {
        if (player != null) {
            player.release();
            player = null;
        }
        stopForeground(true);
        stopSelf();
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

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}