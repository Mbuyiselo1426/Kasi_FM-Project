package com.kasiefm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.kasiefm.api.Show;

import org.junit.Test;

import java.util.Calendar;
import java.util.TimeZone;

public class ShowReminderTimeTest {
    private static final TimeZone JOHANNESBURG = TimeZone.getTimeZone("Africa/Johannesburg");

    @Test public void nineOClockShowTriggersAtEightFiftyFive() {
        long now = atJohannesburg(2026, Calendar.SEPTEMBER, 14, 8, 0);
        Show show = show(42L, "Morning Essentials", "MONDAY", "09:00");

        assertEquals(atJohannesburg(2026, Calendar.SEPTEMBER, 14, 8, 55),
                ShowReminderTime.calculateTriggerMillis(show, now));
    }

    @Test public void passedTriggerIsRejectedInsteadOfMovingToAnotherWeek() {
        long now = atJohannesburg(2026, Calendar.SEPTEMBER, 14, 8, 56);
        Show show = show(42L, "Morning Essentials", "MONDAY", "09:00");

        assertEquals(-1L, ShowReminderTime.calculateTriggerMillis(show, now));
    }

    @Test public void validFutureAiringProducesFutureTrigger() {
        long now = atJohannesburg(2026, Calendar.SEPTEMBER, 14, 7, 30);
        Show show = show(5L, "Morning Essentials", "MONDAY", "09:00");

        assertTrue(ShowReminderTime.calculateTriggerMillis(show, now) > now);
    }

    @Test public void scheduleIdProducesStableReminderIdentity() {
        assertEquals("schedule_reminder_42", ShowReminderTime.reminderKey(
                show(42L, "Morning Essentials", "MONDAY", "09:00")));
        assertNull(ShowReminderTime.reminderKey(show(null, "Morning Essentials", "MONDAY", "09:00")));
    }

    @Test public void invalidOrMissingStartTimeFailsSafely() {
        long now = atJohannesburg(2026, Calendar.SEPTEMBER, 14, 7, 30);
        assertEquals(-1L, ShowReminderTime.calculateTriggerMillis(
                show(42L, "Morning Essentials", "MONDAY", "25:00"), now));
        assertEquals(-1L, ShowReminderTime.calculateTriggerMillis(
                show(42L, "Morning Essentials", "MONDAY", null), now));
    }

    private static Show show(Long id, String name, String day, String start) {
        return new Show(id, name, "Presenter", start, "12:00", "", day, false);
    }

    private static long atJohannesburg(int year, int month, int day, int hour, int minute) {
        Calendar calendar = Calendar.getInstance(JOHANNESBURG);
        calendar.set(year, month, day, hour, minute, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }
}
