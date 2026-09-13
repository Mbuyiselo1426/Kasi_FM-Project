package com.kasiefm;

import com.google.gson.Gson;
import com.kasiefm.api.Show;
import org.junit.Test;
import java.util.Arrays;
import java.util.List;
import static org.junit.Assert.*;

public class ScheduleJsonTest {
    private List<Show> parse(String json) {
        return Arrays.asList(new Gson().fromJson(json, Show[].class));
    }

    @Test
    public void exactWireMarkerSelectsSameShowInEveryPermutation() {
        String a = "{\"id\":1,\"name\":\"Breakfast\",\"isCurrent\":false}";
        String b = "{\"id\":2,\"name\":\"Drive\",\"startTime\":\"15:00\",\"endTime\":\"18:00\",\"isCurrent\":true}";
        String c = "{\"id\":3,\"name\":\"Evening\",\"isCurrent\":false}";
        for (String[] order : new String[][]{{a,b,c},{a,c,b},{b,a,c},{b,c,a},{c,a,b},{c,b,a}}) {
            Show current = ScheduleSelector.findCurrentShow(parse("[" + String.join(",", order) + "]"));
            assertNotNull(current);
            assertEquals(Long.valueOf(2), current.getId());
            assertEquals("Drive", current.getName());
            assertEquals("15:00", current.getStartTime());
            assertEquals("18:00", current.getEndTime());
        }
    }

    @Test
    public void missingNullFalseAndWronglyNamedMarkersDoNotSelect() {
        assertNull(ScheduleSelector.findCurrentShow(parse(
                "[null,{\"id\":1},{\"isCurrent\":null},{\"isCurrent\":false},{\"current\":true}]")));
        assertNull(ScheduleSelector.findCurrentShow(parse("[]")));
    }
}
