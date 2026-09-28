package com.kasiefm;

import com.kasiefm.api.Show;

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

/** Calculates one concrete reminder trigger; it never rolls an airing into a future week. */
public final class ShowReminderTime {
    private static final TimeZone STATION_TIME_ZONE = TimeZone.getTimeZone("Africa/Johannesburg");
    private static final long LEAD_MILLIS = 5L * 60L * 1000L;

    private ShowReminderTime() { }

    /** Returns -1 when this response's airing cannot still produce a valid one-off reminder. */
    public static long calculateTriggerMillis(Show show, long nowMillis) {
        if (show == null || show.getId() == null) return -1L;
        int[] time = parseTime(show.getStartTime());
        if (time == null) return -1L;

        Calendar airing = Calendar.getInstance(STATION_TIME_ZONE);
        airing.setTimeInMillis(nowMillis);
        if (!dayMatchesToday(show.getDayOfWeek(), airing.get(Calendar.DAY_OF_WEEK))) return -1L;
        airing.set(Calendar.HOUR_OF_DAY, time[0]);
        airing.set(Calendar.MINUTE, time[1]);
        airing.set(Calendar.SECOND, 0);
        airing.set(Calendar.MILLISECOND, 0);
        long trigger = airing.getTimeInMillis() - LEAD_MILLIS;
        return trigger > nowMillis ? trigger : -1L;
    }

    /** Stable identity is based only on the schedule-entry ID, not the RadioShow catalogue. */
    public static String reminderKey(Show show) {
        return show != null && show.getId() != null ? "schedule_reminder_" + show.getId() : null;
    }

    private static int[] parseTime(String value) {
        if (value == null || !value.matches("[0-2]\\d:[0-5]\\d")) return null;
        int hour = Integer.parseInt(value.substring(0, 2));
        if (hour > 23) return null;
        return new int[]{hour, Integer.parseInt(value.substring(3, 5))};
    }

    private static boolean dayMatchesToday(String dayOfWeek, int today) {
        if (dayOfWeek == null || dayOfWeek.trim().isEmpty()) return true;
        String value = dayOfWeek.trim().toUpperCase(Locale.US);
        String[] names = {"", "SUNDAY", "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY"};
        return today >= Calendar.SUNDAY && today <= Calendar.SATURDAY && names[today].equals(value);
    }
}
