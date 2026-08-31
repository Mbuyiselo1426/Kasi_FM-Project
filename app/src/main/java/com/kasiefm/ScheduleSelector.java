package com.kasiefm;

import com.kasiefm.api.Show;

import java.util.List;

/** Selects the explicitly marked current show without relying on API list order. */
public final class ScheduleSelector {

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
}
