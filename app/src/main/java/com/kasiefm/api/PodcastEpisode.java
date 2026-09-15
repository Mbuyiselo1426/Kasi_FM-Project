package com.kasiefm.api;

/** Podcast payload returned by GET /api/podcasts. */
public class PodcastEpisode {
    private long id;
    private String title;
    private String showName;
    private String description;
    private String audioUrl;
    private String publishedAt;
    private String createdAt;
    private boolean published;

    public long getId() { return id; }
    public String getTitle() { return title; }
    public String getShowName() { return showName; }
    public String getDescription() { return description; }
    public String getAudioUrl() { return audioUrl; }
    public String getPublishedAt() { return publishedAt; }
    public String getCreatedAt() { return createdAt; }
    public boolean isPublished() { return published; }
}
