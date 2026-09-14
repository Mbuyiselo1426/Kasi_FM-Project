package com.kasiefm.api;

/** Payload from Community; the server assigns id, status and createdAt. */
public class MessageRequest {
    private final String senderName;
    private final String category;
    private final String message;
    private final String songTitle;
    private final String artist;

    public MessageRequest(String senderName, String category, String message,
                          String songTitle, String artist) {
        this.senderName = senderName;
        this.category = category;
        this.message = message;
        this.songTitle = songTitle;
        this.artist = artist;
    }

    public String getSenderName() { return senderName; }
    public String getCategory() { return category; }
    public String getMessage() { return message; }
    public String getSongTitle() { return songTitle; }
    public String getArtist() { return artist; }
}
