package com.kasiefm;

/** Small view-state holder; it has no network or schedule-selection responsibilities. */
final class ScheduleExpansionState {
    private boolean expanded;

    boolean isExpanded() {
        return expanded;
    }

    void toggle() {
        expanded = !expanded;
    }

    String getActionText() {
        return expanded ? "Close schedule ↑" : "Full schedule ›";
    }
}
