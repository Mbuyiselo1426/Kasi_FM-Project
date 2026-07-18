package com.kasiefm.model;

/**
 * A listener-submitted message (song request, shout-out, prayer request,
 * announcement, etc). Named StudioMessage rather than SongRequest so the
 * same model covers every request type the CONNECT module supports now and
 * later, without a breaking rename down the line.
 *
 * stationId is a hardcoded constant for now ("kasie-fm-thokoza") - there is
 * no real multi-station concept anywhere else in the app yet. It's here so
 * the shape is already correct for the future
 * POST /api/stations/{stationId}/messages endpoint, which is NOT
 * implemented yet (Phase 1 only uses WhatsApp/SMS/Call intents - see
 * ConnectFragment). Don't wire this to a real backend call until that
 * endpoint actually exists.
 */
public class StudioMessage {

    public static final String DEFAULT_STATION_ID = "kasie-fm-thokoza";

    private final String id;
    private final String stationId;
    private final String listenerName;
    private final MessageType messageType;
    private final String songTitle;
    private final String artist;
    private final String message;
    private final long createdAt;

    public enum MessageType {
        SONG_REQUEST,
        BIRTHDAY_SHOUTOUT,
        PRAYER_REQUEST,
        COMMUNITY_ANNOUNCEMENT,
        OTHER
    }

    public StudioMessage(String id, String stationId, String listenerName, MessageType messageType,
                          String songTitle, String artist, String message, long createdAt) {
        this.id = id;
        this.stationId = stationId;
        this.listenerName = listenerName;
        this.messageType = messageType;
        this.songTitle = songTitle;
        this.artist = artist;
        this.message = message;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public String getStationId() { return stationId; }
    public String getListenerName() { return listenerName; }
    public MessageType getMessageType() { return messageType; }
    public String getSongTitle() { return songTitle; }
    public String getArtist() { return artist; }
    public String getMessage() { return message; }
    public long getCreatedAt() { return createdAt; }

    /**
     * Human-readable label for display and for composing the WhatsApp/SMS text.
     */
    public String getMessageTypeLabel() {
        switch (messageType) {
            case SONG_REQUEST: return "Song Request";
            case BIRTHDAY_SHOUTOUT: return "Birthday Shout-out";
            case PRAYER_REQUEST: return "Prayer Request";
            case COMMUNITY_ANNOUNCEMENT: return "Community Announcement";
            default: return "Message";
        }
    }
}
