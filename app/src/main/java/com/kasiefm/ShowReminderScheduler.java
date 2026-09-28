package com.kasiefm;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.kasiefm.api.Show;

/** Schedules and cancels one local alarm for the concrete schedule row selected by a listener. */
public final class ShowReminderScheduler {
    static final String EXTRA_SHOW_ID = "show_id";
    static final String EXTRA_SHOW_NAME = "show_name";

    private final Context context;
    private final AlarmManager alarmManager;
    private final ShowReminderStore store;

    public ShowReminderScheduler(Context context) {
        this.context = context.getApplicationContext();
        alarmManager = (AlarmManager) this.context.getSystemService(Context.ALARM_SERVICE);
        store = new ShowReminderStore(this.context);
    }

    public boolean schedule(Show show, long nowMillis) {
        long triggerMillis = ShowReminderTime.calculateTriggerMillis(show, nowMillis);
        if (triggerMillis < 0L || alarmManager == null) return false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Avoids exact-alarm special access while still allowing delivery during device idle.
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent(show));
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent(show));
        }
        store.save(show, triggerMillis);
        return true;
    }

    public void cancel(Show show) {
        if (alarmManager != null) alarmManager.cancel(pendingIntent(show));
        store.clear(show);
    }

    public boolean isReminderSet(Show show) {
        return store.isSet(show, System.currentTimeMillis());
    }

    private PendingIntent pendingIntent(Show show) {
        Intent intent = new Intent(context, ShowReminderReceiver.class)
                .setAction("com.kasiefm.SHOW_REMINDER." + show.getId())
                .putExtra(EXTRA_SHOW_ID, show.getId())
                .putExtra(EXTRA_SHOW_NAME, show.getName());
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) flags |= PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getBroadcast(context, show.getId().hashCode(), intent, flags);
    }
}
