package com.kasiefm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ScheduleExpansionStateTest {
    @Test
    public void scheduleIsHiddenByDefaultAndUsesExpandAction() {
        ScheduleExpansionState state = new ScheduleExpansionState();

        assertFalse(state.isExpanded());
        assertEquals("Full schedule ›", state.getActionText());
    }

    @Test
    public void toggleRevealsThenHidesScheduleWithoutAnyNetworkBehavior() {
        ScheduleExpansionState state = new ScheduleExpansionState();

        state.toggle();
        assertTrue(state.isExpanded());
        assertEquals("Close schedule ↑", state.getActionText());

        state.toggle();
        assertFalse(state.isExpanded());
        assertEquals("Full schedule ›", state.getActionText());
    }
}
