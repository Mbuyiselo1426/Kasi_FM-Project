package com.kasiefm;

import com.kasiefm.api.Show;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Calendar;
import java.util.TimeZone;

/** Selects the explicitly marked current show without relying on API list order. */
public final class ScheduleSelector {
    private static final TimeZone STATION_TIME_ZONE = TimeZone.getTimeZone("Africa/Johannesburg");

    private ScheduleSelector() {
    }

    public static Show findCurrentShow(List<Show> schedule) {
        if (schedule == null || schedule.isEmpty()) {
            return null;
        }

        for (Show show : schedule) {
            if (show != null && show.isCurrent()) {
                return show;
            }
        }

        return null;
    }

    /** Returns the API response order unchanged for rendering today's schedule. */
    public static List<Show> rowsInResponseOrder(List<Show> schedule) {
        if (schedule == null || schedule.isEmpty()) {
            return Collections.emptyList();
        }
        return new ArrayList<>(schedule);
    }

    /**
     * Calculates only when to ask the server again. It never infers which show
     * should be current; the next response remains the authority for that.
     */
    public static long millisUntilNextRefresh(String endTime, long nowMillis, long delayMillis) {
        if (endTime == null || !endTime.matches("[0-2]\\d:[0-5]\\d")) {
            return -1L;
        }
        String[] parts = endTime.split(":");
        int hour = Integer.parseInt(parts[0]);
        int minute = Integer.parseInt(parts[1]);
        if (hour > 23) {
            return -1L;
        }

        // Schedule times belong to the station, not to the listener's device timezone.
        Calendar boundary = Calendar.getInstance(STATION_TIME_ZONE);
        boundary.setTimeInMillis(nowMillis);
        boundary.set(Calendar.HOUR_OF_DAY, hour);
        boundary.set(Calendar.MINUTE, minute);
        boundary.set(Calendar.SECOND, 0);
        boundary.set(Calendar.MILLISECOND, 0);
        if (boundary.getTimeInMillis() <= nowMillis) {
            boundary.add(Calendar.DAY_OF_YEAR, 1);
        }
        return Math.max(0L, boundary.getTimeInMillis() - nowMillis + Math.max(0L, delayMillis));
    }
}
