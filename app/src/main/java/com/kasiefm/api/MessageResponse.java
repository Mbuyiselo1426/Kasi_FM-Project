package com.kasiefm.api;

public class MessageResponse {
    private Long id;
    private String senderName;
    private String category;
    private String message;
    private String songTitle;
    private String artist;
    private String status;
    private String createdAt;

    public Long getId() { return id; }
    public String getSenderName() { return senderName; }
    public String getCategory() { return category; }
    public String getMessage() { return message; }
    public String getSongTitle() { return songTitle; }
    public String getArtist() { return artist; }
    public String getStatus() { return status; }
    public String getCreatedAt() { return createdAt; }
}
