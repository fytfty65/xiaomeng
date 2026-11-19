package com.omniroute.app.core.network.dto;

import com.google.gson.annotations.SerializedName;

/** PUT /api/user/trip/{id}/content 请求体：只传路线内容，归属由服务端会话校验。 */
public final class UpdateTripContentRequest {

    @SerializedName("content")
    private final String content;

    public UpdateTripContentRequest(String content) {
        this.content = content;
    }

    public String getContent() {
        return content;
    }
}
