package com.kasiefm.api;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;

public interface KasieApiService {
    @GET("/api/schedule")
    Call<List<Show>> getSchedule();

    @GET("/api/stream-url")
    Call<StreamUrls> getStreamUrl();
}