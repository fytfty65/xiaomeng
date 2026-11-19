package com.omniroute.app.core.network.dto;

import com.google.gson.annotations.SerializedName;

/** POST /api/user/trip 请求体。user_id 由网关用登录态覆盖，但 binding 要求非空。 */
public final class SaveTripRequest {

    @SerializedName("user_id")
    private final String userId;

    @SerializedName("title")
    private final String title;

    @SerializedName("dest_city")
    private final String destCity;

    @SerializedName("content")
    private final String content;

    @SerializedName("days")
    private final int days;

    @SerializedName("travelers")
    private final int travelers;

    public SaveTripRequest(String userId, String title, String destCity, String content, int days, int travelers) {
        this.userId = userId;
        this.title = title;
        this.destCity = destCity;
        this.content = content;
        this.days = days;
        this.travelers = travelers;
    }

    public String getUserId() {
        return userId;
    }

    public String getTitle() {
        return title;
    }

    public String getDestCity() {
        return destCity;
    }

    public String getContent() {
        return content;
    }

    public int getDays() {
        return days;
    }

    public int getTravelers() {
        return travelers;
    }
}
