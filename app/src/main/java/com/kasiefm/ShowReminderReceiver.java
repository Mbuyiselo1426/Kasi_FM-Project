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

/**
 * Posts the notification for a previously scheduled one-airing reminder.
 */
public class ShowReminderReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "show_reminders";
    private static final int NOTIFICATION_ID_OFFSET = 20_000;

    @Override
    public void onReceive(Context context, Intent intent) {

        // Get the show information that was attached when the alarm was scheduled.
        long showId = intent.getLongExtra(
                ShowReminderScheduler.EXTRA_SHOW_ID,
                -1L
        );

        String showName = intent.getStringExtra(
                ShowReminderScheduler.EXTRA_SHOW_NAME
        );

        // Do nothing if the reminder does not contain valid show information.
        if (showId < 0L || showName == null || showName.trim().isEmpty()) {
            return;
        }

        // Android 8+ requires a notification channel.
        createChannel(context);

        // Open Kasie FM when the listener taps the notification.
        Intent openApp = new Intent(context, MainActivity.class)
                .addFlags(
                        Intent.FLAG_ACTIVITY_CLEAR_TOP
                                | Intent.FLAG_ACTIVITY_SINGLE_TOP
                );

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        /*
         * showId is a primitive long.
         *
         * We cannot do:
         *
         *     showId.hashCode()
         *
         * because primitives do not have methods.
         *
         * Long.hashCode(showId) converts the long into the int request code
         * required by PendingIntent.
         */
        int reminderId = Long.hashCode(showId);

        PendingIntent contentIntent = PendingIntent.getActivity(
                context,
                reminderId,
                openApp,
                flags
        );

        NotificationCompat.Builder notification =
                new NotificationCompat.Builder(context, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_reminder_bell)
                        .setContentTitle("Kasie FM")
                        .setContentText(
                                showName.trim() + " starts in 5 minutes."
                        )
                        .setContentIntent(contentIntent)
                        .setAutoCancel(true)
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        try {
            /*
             * The notification ID must also be an int.
             *
             * We reuse reminderId so the same schedule entry has a stable
             * notification identifier.
             */
            NotificationManagerCompat
                    .from(context)
                    .notify(
                            NOTIFICATION_ID_OFFSET + reminderId,
                            notification.build()
                    );

        } catch (SecurityException ignored) {
            /*
             * Android 13+ users can revoke notification permission after
             * scheduling the reminder.
             *
             * In that situation we simply avoid crashing the app.
             */
        }
    }

    /**
     * Creates a notification channel specifically for show reminders.
     *
     * Notification channels are required on Android 8.0+.
     */
    private void createChannel(Context context) {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Kasie FM Show Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
        );

        NotificationManager notificationManager =
                context.getSystemService(NotificationManager.class);

        if (notificationManager != null) {
            notificationManager.createNotificationChannel(channel);
        }
    }
}