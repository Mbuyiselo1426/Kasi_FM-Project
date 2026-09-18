package com.kasiefm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.google.gson.Gson;
import com.kasiefm.api.RadioShow;
import org.junit.Test;

public class RadioShowJsonTest {
    @Test
    public void parsesProductionShowPayloadIncludingNullableFields() {
        String json = "{\"id\":16,\"name\":\"Vuka Kasie\","
                + "\"presenterName\":\"Sipho Nkosi\","
                + "\"presenterNickname\":\"JB Nkosi\","
                + "\"imageUrl\":\"https://example.com/vuka.jpg\","
                + "\"description\":null}";

        RadioShow show = new Gson().fromJson(json, RadioShow.class);

        assertEquals(16L, show.getId());
        assertEquals("Vuka Kasie", show.getName());
        assertEquals("Sipho Nkosi", show.getPresenterName());
        assertEquals("JB Nkosi", show.getPresenterNickname());
        assertEquals("https://example.com/vuka.jpg", show.getImageUrl());
        assertNull(show.getDescription());
    }
}
