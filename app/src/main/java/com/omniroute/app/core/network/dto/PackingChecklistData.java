package com.omniroute.app.core.network.dto;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/** POST /api/v1/planning/packing/checklist 的 data。 */
public final class PackingChecklistData {

    @SerializedName("items")
    private List<PackingItemDto> items;

    @SerializedName("generated_at")
    private String generatedAt;

    public List<PackingItemDto> getItems() {
        return items == null ? new ArrayList<>() : items;
    }

    public String getGeneratedAt() {
        return generatedAt;
    }
}
