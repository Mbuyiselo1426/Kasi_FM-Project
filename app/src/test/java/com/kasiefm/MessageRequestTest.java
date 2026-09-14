package com.kasiefm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.kasiefm.api.MessageRequest;
import com.kasiefm.model.StudioMessage;
import org.junit.Test;

public class MessageRequestTest {
    @Test
    public void songRequestPreservesBackendCategoryAndSongFields() {
        StudioMessage message = new StudioMessage("client-id", "station", "Mbuyiselo",
                StudioMessage.MessageType.SONG_REQUEST, "Kasi Anthem", "Napo", "For family", 1L);
        MessageRequest request = requestFor(message);

        assertEquals("SONG_REQUEST", request.getCategory());
        assertEquals("Kasi Anthem", request.getSongTitle());
        assertEquals("Napo", request.getArtist());
    }

    @Test
    public void nonSongRequestKeepsCategoryAndHasNoSongFields() {
        StudioMessage message = new StudioMessage("client-id", "station", "Lindiwe",
                StudioMessage.MessageType.PRAYER_REQUEST, null, null, "Please pray", 1L);
        MessageRequest request = requestFor(message);

        assertEquals("PRAYER_REQUEST", request.getCategory());
        assertNull(request.getSongTitle());
        assertNull(request.getArtist());
    }

    private static MessageRequest requestFor(StudioMessage message) {
        return new MessageRequest(message.getListenerName(), message.getMessageType().name(),
                message.getMessage(), message.getSongTitle(), message.getArtist());
    }
}
