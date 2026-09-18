package com.kasiefm;

import com.kasiefm.api.RadioShow;

/** Formats API presenter metadata for the compact Home show cards. */
public final class ShowPreviewFormatter {
    private ShowPreviewFormatter() {
    }

    public static String presenterText(RadioShow show) {
        if (show == null) return "";
        String name = nonBlank(show.getPresenterName());
        String nickname = nonBlank(show.getPresenterNickname());
        if (name == null) return nickname == null ? "" : "\"" + nickname + "\"";
        return nickname == null ? name : name + "\n\"" + nickname + "\"";
    }

    public static String showName(RadioShow show) {
        String name = show == null ? null : nonBlank(show.getName());
        return name == null ? "Show" : name;
    }

    private static String nonBlank(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
