package com.omniroute.app.core.network.dto;

import com.google.gson.annotations.SerializedName;

import java.util.Map;

/**
 * 规划上下文：既是所有 /api/v1/planning/* 请求体，也是响应里回显的 context。
 * 授权使用 trip_id 字段传资源键（真实行程 ID 或已加入的 room_id）。
 */
public final class PlanningContextDto {

    @SerializedName("user_id")
    private String userId;

    @SerializedName("trip_id")
    private String tripId;

    @SerializedName("room_id")
    private String roomId;

    @SerializedName("destination")
    private String destination;

    @SerializedName("origin")
    private String origin;

    @SerializedName("start_date")
    private String startDate;

    @SerializedName("end_date")
    private String endDate;

    @SerializedName("travelers")
    private int travelers;

    @SerializedName("budget")
    private double budget;

    @SerializedName("currency")
    private String currency;

    @SerializedName("preferences")
    private Map<String, Object> preferences;

    public String getUserId() {
        return userId;
    }

    public String getTripId() {
        return tripId;
    }

    public void setTripId(String tripId) {
        this.tripId = tripId;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public int getTravelers() {
        return travelers;
    }

    public void setTravelers(int travelers) {
        this.travelers = travelers;
    }

    public double getBudget() {
        return budget;
    }

    public void setBudget(double budget) {
        this.budget = budget;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Map<String, Object> getPreferences() {
        return preferences;
    }

    public void setPreferences(Map<String, Object> preferences) {
        this.preferences = preferences;
    }
}
