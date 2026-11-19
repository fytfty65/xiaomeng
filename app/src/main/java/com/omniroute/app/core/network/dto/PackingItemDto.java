package com.omniroute.app.core.network.dto;

import com.google.gson.annotations.SerializedName;

/** 行前清单中的一条物品/准备项。 */
public final class PackingItemDto {

    @SerializedName("id")
    private String id;

    @SerializedName("name")
    private String name;

    @SerializedName("priority")
    private String priority;

    @SerializedName("category")
    private String category;

    @SerializedName("reason")
    private String reason;

    @SerializedName("estimated")
    private boolean estimated;

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getPriority() {
        return priority;
    }

    public String getCategory() {
        return category;
    }

    public String getReason() {
        return reason;
    }

    public boolean isEstimated() {
        return estimated;
    }
}
