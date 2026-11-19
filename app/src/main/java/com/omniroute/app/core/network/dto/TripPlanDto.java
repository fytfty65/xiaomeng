package com.omniroute.app.core.network.dto;

import com.google.gson.annotations.SerializedName;

/**
 * /api/user/trips 返回的行程。TripPlan 在网关侧没有 json tag，
 * 因此字段名就是 Go 结构体名（大写）。
 */
public final class TripPlanDto {

    @SerializedName("ID")
    private String id;

    @SerializedName("UserID")
    private String userId;

    @SerializedName("Title")
    private String title;

    @SerializedName("DestCity")
    private String destCity;

    @SerializedName("Content")
    private String content;

    @SerializedName("Days")
    private int days;

    @SerializedName("Travelers")
    private int travelers;

    @SerializedName("CreatedAt")
    private String createdAt;

    public String getId() {
        return id;
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

    public String getCreatedAt() {
        return createdAt;
    }
}
