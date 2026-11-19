package com.omniroute.app.core.network.dto;

import com.google.gson.annotations.SerializedName;

/** 风险通知条目（后端 risk_notifications 表的一行）。 */
public final class RiskNotificationDto {

    @SerializedName("id")
    private String id;

    @SerializedName("trip_id")
    private String tripId;

    @SerializedName("city")
    private String city;

    @SerializedName("type")
    private String type;

    @SerializedName("severity")
    private String severity;

    @SerializedName("title")
    private String title;

    @SerializedName("detail")
    private String detail;

    @SerializedName("source")
    private String source;

    @SerializedName("estimated")
    private boolean estimated;

    @SerializedName("observed_at")
    private String observedAt;

    @SerializedName("replan_proposal")
    private String replanProposal;

    @SerializedName("status")
    private String status;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("read_at")
    private String readAt;

    public String getId() {
        return id;
    }

    public String getTripId() {
        return tripId;
    }

    public String getCity() {
        return city;
    }

    public String getType() {
        return type;
    }

    public String getSeverity() {
        return severity;
    }

    public String getTitle() {
        return title;
    }

    public String getDetail() {
        return detail;
    }

    public String getSource() {
        return source;
    }

    public boolean isEstimated() {
        return estimated;
    }

    public String getObservedAt() {
        return observedAt;
    }

    public String getReplanProposal() {
        return replanProposal;
    }

    public String getStatus() {
        return status;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public String getReadAt() {
        return readAt;
    }

    /** 未读是否成立：后端只保证明确写 unread。 */
    public boolean isUnread() {
        return status != null && status.trim().equalsIgnoreCase("unread");
    }
}
