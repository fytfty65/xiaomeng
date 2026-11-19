package com.omniroute.app.core.network.dto;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/** POST /api/community/post 的请求体。 */
public final class PublishPostRequest {

    @SerializedName("title")
    private final String title;

    @SerializedName("content")
    private final String content;

    @SerializedName("dest_city")
    private final String destCity;

    @SerializedName("tags")
    private final List<String> tags;

    public PublishPostRequest(String title, String content, String destCity, List<String> tags) {
        this.title = title;
        this.content = content;
        this.destCity = destCity;
        this.tags = tags == null ? new ArrayList<>() : tags;
    }
}
