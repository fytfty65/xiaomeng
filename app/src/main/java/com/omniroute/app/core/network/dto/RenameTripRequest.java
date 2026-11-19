package com.omniroute.app.core.network.dto;

import com.google.gson.annotations.SerializedName;

/** PUT /api/user/trip/:id 的请求体：只允许改标题，行程内容不动。 */
public final class RenameTripRequest {

    @SerializedName("title")
    private final String title;

    public RenameTripRequest(String title) {
        this.title = title;
    }
}
