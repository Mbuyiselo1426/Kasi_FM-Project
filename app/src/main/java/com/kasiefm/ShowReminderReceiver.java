package com.kasiefm;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

/** Posts the notification for a previously scheduled one-airing reminder. */
public class ShowReminderReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "show_reminders";
    private static final int NOTIFICATION_ID_OFFSET = 20_000;

    @Override public void onReceive(Context context, Intent intent) {
        long showId = intent.getLongExtra(ShowReminderScheduler.EXTRA_SHOW_ID, -1L);
        String showName = intent.getStringExtra(ShowReminderScheduler.EXTRA_SHOW_NAME);
        if (showId < 0L || showName == null || showName.trim().isEmpty()) return;
        createChannel(context);
        Intent openApp = new Intent(context, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) flags |= PendingIntent.FLAG_IMMUTABLE;
        PendingIntent contentIntent = PendingIntent.getActivity(context, showId.hashCode(), openApp, flags);
        NotificationCompat.Builder notification = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_reminder_bell)
                .setContentTitle("Kasie FM")
                .setContentText(showName.trim() + " starts in 5 minutes.")
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_OFFSET + showId.hashCode(),
                    notification.build());
        } catch (SecurityException ignored) {
            // The listener may have revoked Android 13+ notification permission after scheduling.
        }
    }

    private void createChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                "Kasie FM Show Reminders", NotificationManager.IMPORTANCE_DEFAULT);
        context.getSystemService(NotificationManager.class).createNotificationChannel(channel);
    }
}
