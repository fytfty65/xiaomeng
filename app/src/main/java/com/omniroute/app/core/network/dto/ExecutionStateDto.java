package com.omniroute.app.core.network.dto;

import com.google.gson.annotations.SerializedName;

/** 单个节点的执行状态；状态取值 planned/in_progress/visited/skipped/delayed。 */
public final class ExecutionStateDto {

    @SerializedName("id")
    private String id;

    @SerializedName("trip_id")
    private String tripId;

    @SerializedName("plan_id")
    private String planId;

    @SerializedName("node_key")
    private String nodeKey;

    @SerializedName("status")
    private String status;

    @SerializedName("delay_minutes")
    private int delayMinutes;

    @SerializedName("note")
    private String note;

    @SerializedName("updated_at")
    private String updatedAt;

    @SerializedName("created_at")
    private String createdAt;

    public String getId() {
        return id;
    }

    public String getTripId() {
        return tripId;
    }

    public String getPlanId() {
        return planId;
    }

    public String getNodeKey() {
        return nodeKey;
    }

    public String getStatus() {
        return status;
    }

    public int getDelayMinutes() {
        return delayMinutes;
    }

    public String getNote() {
        return note;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public String getCreatedAt() {
        return createdAt;
    }
}
