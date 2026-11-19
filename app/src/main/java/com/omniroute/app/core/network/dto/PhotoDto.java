package com.omniroute.app.core.network.dto;

import com.google.gson.annotations.SerializedName;

/** 与网关 internal/photos.Photo 的 JSON 字段一一对应。 */
public final class PhotoDto {

    @SerializedName("id")
    private String id;

    @SerializedName("node_name")
    private String nodeName;

    @SerializedName("trip_id")
    private String tripId;

    @SerializedName("mime")
    private String mime;

    @SerializedName("bytes")
    private int bytes;

    @SerializedName("width")
    private int width;

    @SerializedName("height")
    private int height;

    /** pending / approved / rejected。 */
    @SerializedName("status")
    private String status;

    /** 审核拒绝时会回传理由，前端必须如实展示。 */
    @SerializedName("reject_reason")
    private String rejectReason;

    @SerializedName("created_at")
    private String createdAt;

    public String getId() {
        return id;
    }

    public String getNodeName() {
        return nodeName;
    }

    public String getTripId() {
        return tripId;
    }

    public String getMime() {
        return mime;
    }

    public int getBytes() {
        return bytes;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public String getStatus() {
        return status;
    }

    public String getRejectReason() {
        return rejectReason;
    }

    public String getCreatedAt() {
        return createdAt;
    }
}
