package com.kasiefm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.kasiefm.api.Show;
import com.google.gson.Gson;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Calendar;
import java.util.TimeZone;

public class ScheduleSelectorTest {

    @Test
    public void findCurrentShow_returnsFirstItemWhenMarkedCurrent() {
        Show current = show("Breakfast", "06:00", "09:00", true);
        Show later = show("Mid-morning", "09:00", "12:00", false);

        Show result = ScheduleSelector.findCurrentShow(Arrays.asList(current, later));

        assertNotNull(result);
        assertEquals("Breakfast", result.getName());
    }

    @Test
    public void findCurrentShow_returnsMarkedCurrentInMiddleOfUnorderedList() {
        Show early = show("Late Show", "22:00", "00:00", false);
        Show current = show("Drive", "15:00", "18:00", true);
        Show morning = show("Breakfast", "06:00", "09:00", false);

        Show result = ScheduleSelector.findCurrentShow(Arrays.asList(early, current, morning));

        assertNotNull(result);
        assertEquals("Drive", result.getName());
    }

    @Test
    public void findCurrentShow_returnsMarkedCurrentWhenLast() {
        Show first = show("Breakfast", "06:00", "09:00", false);
        Show second = show("Mid-morning", "09:00", "12:00", false);
        Show current = show("Lunch", "12:00", "15:00", true);

        Show result = ScheduleSelector.findCurrentShow(Arrays.asList(first, second, current));

        assertNotNull(result);
        assertEquals("Lunch", result.getName());
    }

    @Test
    public void findCurrentShow_returnsNullForEmptySchedule() {
        assertNull(ScheduleSelector.findCurrentShow(Collections.emptyList()));
    }

    @Test
    public void findCurrentShow_returnsNullForNullSchedule() {
        assertNull(ScheduleSelector.findCurrentShow(null));
    }

    @Test
    public void findCurrentShow_returnsNullWhenNoShowIsMarkedCurrent() {
        Show first = show("Breakfast", "06:00", "09:00", false);
        Show second = show("Mid-morning", "09:00", "12:00", false);

        assertNull(ScheduleSelector.findCurrentShow(Arrays.asList(first, second)));
    }

    @Test
    public void findCurrentShow_ignoresNullItemsAndNullCurrentFlags() {
        Show notCurrent = show("Breakfast", "06:00", "09:00", null);
        Show current = show("Drive", "15:00", "18:00", true);

        Show result = ScheduleSelector.findCurrentShow(Arrays.asList(null, notCurrent, current));

        assertNotNull(result);
        assertEquals("Drive", result.getName());
    }

    @Test
    public void showJson_mapsDayOfWeekAndCurrentMarker() {
        Show show = new Gson().fromJson(
                "{\"id\":10,\"name\":\"Asiye 6-9 Breakfast Show\",\"presenter\":null,"
                        + "\"startTime\":\"06:00\",\"endTime\":\"09:00\",\"description\":null,"
                        + "\"dayOfWeek\":\"MONDAY\",\"isCurrent\":true}",
                Show.class);

        assertEquals("MONDAY", show.getDayOfWeek());
        assertEquals(true, show.isCurrent());
    }

    @Test
    public void rowsInResponseOrder_preservesTheBackendOrder() {
        Show first = show("Overnight", "00:00", "03:00", false);
        Show second = show("Breakfast", "06:00", "09:00", true);
        Show third = show("Drive", "15:00", "18:00", false);

        List<Show> rows = ScheduleSelector.rowsInResponseOrder(Arrays.asList(first, second, third));

        assertEquals(first, rows.get(0));
        assertEquals(second, rows.get(1));
        assertEquals(third, rows.get(2));
    }

    @Test
    public void nextRefresh_usesCurrentShowsEndTimeOnlyToScheduleTheRefetch() {
        Show current = show("Breakfast", "06:00", "09:00", true);
        long now = atJohannesburg(2026, Calendar.SEPTEMBER, 14, 8, 59);

        assertEquals(68_000L, ScheduleSelector.millisUntilNextRefresh(
                current.getEndTime(), now, 8_000L));
    }

    @Test
    public void nextRefresh_handlesMidnightEndTimeAsTheFollowingDay() {
        long now = atJohannesburg(2026, Calendar.SEPTEMBER, 14, 21, 0);

        assertEquals(10_808_000L, ScheduleSelector.millisUntilNextRefresh("00:00", now, 8_000L));
    }

    @Test
    public void nextRefresh_usesJohannesburgTimeRegardlessOfDefaultTimezone() {
        TimeZone original = TimeZone.getDefault();
        long now = atJohannesburg(2026, Calendar.SEPTEMBER, 14, 8, 59);
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"));
            assertEquals(68_000L, ScheduleSelector.millisUntilNextRefresh("09:00", now, 8_000L));
        } finally {
            TimeZone.setDefault(original);
        }
    }

    @Test
    public void nextRefresh_rejectsInvalidEndTime() {
        assertEquals(-1L, ScheduleSelector.millisUntilNextRefresh("25:00", 0L, 8_000L));
        assertEquals(-1L, ScheduleSelector.millisUntilNextRefresh(null, 0L, 8_000L));
    }

    private static long atJohannesburg(int year, int month, int day, int hour, int minute) {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Africa/Johannesburg"));
        calendar.set(year, month, day, hour, minute, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    private static Show show(String name, String startTime, String endTime, Boolean isCurrent) {
        return new Show(1L, name, "Presenter", startTime, endTime, "Description", isCurrent);
    }
}
