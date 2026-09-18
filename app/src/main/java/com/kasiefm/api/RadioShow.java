package com.kasiefm.api;

/** Radio show payload returned by GET /api/shows. */
public class RadioShow {
    private long id;
    private String name;
    private String presenterName;
    private String presenterNickname;
    private String imageUrl;
    private String description;

    public long getId() { return id; }
    public String getName() { return name; }
    public String getPresenterName() { return presenterName; }
    public String getPresenterNickname() { return presenterNickname; }
    public String getImageUrl() { return imageUrl; }
    public String getDescription() { return description; }
}
