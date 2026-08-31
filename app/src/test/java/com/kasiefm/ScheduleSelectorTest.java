package com.kasiefm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.kasiefm.api.Show;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

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

    private static Show show(String name, String startTime, String endTime, Boolean isCurrent) {
        return new Show(1L, name, "Presenter", startTime, endTime, "Description", isCurrent);
    }
}
