package com.omniroute.app.core.network.dto;

import com.google.gson.annotations.SerializedName;

/** 房间方案池里的单个候选方案，含实时得票与胜出标记。 */
public final class PlanVariantDto {

    @SerializedName("id")
    public String id;

    @SerializedName("room_id")
    public String roomId;

    @SerializedName("name")
    public String name;

    @SerializedName("style")
    public String style;

    /** 路线 JSON 字符串，采纳时由网关解析并估算预算基线。 */
    @SerializedName("route")
    public String route;

    @SerializedName("created_by")
    public String createdBy;

    @SerializedName("votes")
    public int votes;

    @SerializedName("winner")
    public boolean winner;

    @SerializedName("created_at")
    public String createdAt;
}
