package com.omniroute.app.core.network.dto;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** GET /api/v1/plan/variants：房间方案池（按得票降序）。 */
public final class PlanVariantsResponse {

    @SerializedName("variants")
    public List<PlanVariantDto> variants;

    @SerializedName("winner_id")
    public String winnerId;

    @SerializedName("has_winner")
    public boolean hasWinner;

    public List<PlanVariantDto> variantsOrEmpty() {
        if (variants == null) {
            return Collections.emptyList();
        }
        List<PlanVariantDto> result = new ArrayList<>();
        for (PlanVariantDto variant : variants) {
            if (variant != null) {
                result.add(variant);
            }
        }
        return result;
    }
}
