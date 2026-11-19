package com.omniroute.app.core.network.dto;

import com.google.gson.annotations.SerializedName;

/** POST /api/community/post 的响应。 */
public final class PublishPostResponse {

    @SerializedName("message")
    private String message;

    @SerializedName("post_id")
    private String postId;

    public String getMessage() {
        return message;
    }

    public String getPostId() {
        return postId;
    }
}
