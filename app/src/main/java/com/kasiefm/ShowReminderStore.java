package com.kasiefm;

import android.content.Context;
import android.content.SharedPreferences;

import com.kasiefm.api.Show;

/** Small persistent record for a one-airing reminder. */
final class ShowReminderStore {
    private static final String PREFS = "show_reminders";

    private final SharedPreferences preferences;

    ShowReminderStore(Context context) {
        preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    void save(Show show, long triggerMillis) {
        String key = ShowReminderTime.reminderKey(show);
        if (key == null) return;
        preferences.edit()
                .putBoolean(key + "_enabled", true)
                .putLong(key + "_trigger", triggerMillis)
                .putString(key + "_name", show.getName())
                .putString(key + "_day", show.getDayOfWeek())
                .putString(key + "_start", show.getStartTime())
                .apply();
    }

    boolean isSet(Show show, long nowMillis) {
        String key = ShowReminderTime.reminderKey(show);
        if (key == null || !preferences.getBoolean(key + "_enabled", false)) return false;
        if (preferences.getLong(key + "_trigger", -1L) > nowMillis) return true;
        clear(show);
        return false;
    }

    void clear(Show show) {
        String key = ShowReminderTime.reminderKey(show);
        if (key == null) return;
        preferences.edit()
                .remove(key + "_enabled")
                .remove(key + "_trigger")
                .remove(key + "_name")
                .remove(key + "_day")
                .remove(key + "_start")
                .apply();
    }
}
