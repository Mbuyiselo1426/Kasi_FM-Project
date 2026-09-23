package com.kasiefm.api;

/** Featured payload returned by GET /api/featured. */
public class FeaturedContent {
    private long id;
    private String type;
    private String title;
    private String summary;
    private String imageUrl;
    private String externalUrl;
    private String publishedAt;

    public long getId() { return id; }
    public String getType() { return type; }
    public String getTitle() { return title; }
    public String getSummary() { return summary; }
    public String getImageUrl() { return imageUrl; }
    public String getExternalUrl() { return externalUrl; }
    public String getPublishedAt() { return publishedAt; }
}
