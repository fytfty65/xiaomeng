package com.omniroute.app.core.network.dto;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/** GET /api/community/list 的响应。 */
public final class CommunityFeedResponse {

    @SerializedName("posts")
    private List<CommunityPostDto> posts;

    public List<CommunityPostDto> getPosts() {
        return posts == null ? new ArrayList<>() : posts;
    }
}
