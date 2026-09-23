package com.kasiefm.api;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface KasieApiService {
    @GET("/api/schedule")
    Call<List<Show>> getSchedule();

    @POST("/api/messages")
    Call<MessageResponse> createMessage(@Body MessageRequest request);

    @GET("/api/stream-url")
    Call<StreamUrls> getStreamUrl();

    @GET("/api/podcasts")
    Call<List<PodcastEpisode>> getPodcasts();

    @GET("/api/shows")
    Call<List<RadioShow>> getShows();

    @GET("/api/featured")
    Call<List<FeaturedContent>> getFeatured();
}
