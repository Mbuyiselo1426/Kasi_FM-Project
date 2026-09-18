package com.kasiefm;

import static org.junit.Assert.assertEquals;

import com.google.gson.Gson;
import com.kasiefm.api.RadioShow;
import org.junit.Test;

public class ShowPreviewFormatterTest {
    @Test
    public void formatsAvailablePresenterMetadataWithoutNullLabels() {
        RadioShow both = show("Sipho Nkosi", "JB Nkosi");
        RadioShow nameOnly = show("Sipho Nkosi", null);
        RadioShow neither = show(null, null);

        assertEquals("Sipho Nkosi\n\"JB Nkosi\"", ShowPreviewFormatter.presenterText(both));
        assertEquals("Sipho Nkosi", ShowPreviewFormatter.presenterText(nameOnly));
        assertEquals("", ShowPreviewFormatter.presenterText(neither));
    }

    private static RadioShow show(String presenterName, String presenterNickname) {
        return new Gson().fromJson("{\"presenterName\":" + jsonValue(presenterName)
                + ",\"presenterNickname\":" + jsonValue(presenterNickname) + "}", RadioShow.class);
    }

    private static String jsonValue(String value) {
        return value == null ? "null" : "\"" + value + "\"";
    }
}
