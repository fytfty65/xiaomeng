package com.omniroute.app.core.network.dto;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/**
 * 规划候选（目的地/交通/住宿/餐饮同构）。
 * extra 是后端开放的扩展字段袋，这里只读取界面需要的部分。
 */
public final class CandidateDto {

    @SerializedName("id")
    private String id;

    @SerializedName("name")
    private String name;

    @SerializedName("score")
    private double score;

    @SerializedName("price")
    private double price;

    @SerializedName("duration_minutes")
    private int durationMinutes;

    @SerializedName("tags")
    private List<String> tags;

    @SerializedName("reasons")
    private List<String> reasons;

    @SerializedName("constraints")
    private List<String> constraints;

    @SerializedName("source")
    private CandidateSourceDto source;

    @SerializedName("confidence")
    private double confidence;

    @SerializedName("freshness_seconds")
    private int freshnessSeconds;

    @SerializedName("extra")
    private JsonObject extra;

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public double getScore() {
        return score;
    }

    public double getPrice() {
        return price;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public List<String> getTags() {
        return tags == null ? new ArrayList<>() : tags;
    }

    public List<String> getReasons() {
        return reasons == null ? new ArrayList<>() : reasons;
    }

    public List<String> getConstraints() {
        return constraints == null ? new ArrayList<>() : constraints;
    }

    public CandidateSourceDto getSource() {
        return source;
    }

    public double getConfidence() {
        return confidence;
    }

    public int getFreshnessSeconds() {
        return freshnessSeconds;
    }

    /** 是否由本地估算生成：是则价格一律不可当作真实报价。 */
    public boolean isEstimated() {
        return source != null && source.isEstimated();
    }

    public String getExtraString(String key) {
        if (extra == null) {
            return null;
        }
        JsonElement element = extra.get(key);
        if (element == null || element.isJsonNull() || !element.isJsonPrimitive()) {
            return null;
        }
        String value = element.getAsString();
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    public int getExtraInt(String key) {
        if (extra == null) {
            return 0;
        }
        JsonElement element = extra.get(key);
        if (element == null || !element.isJsonPrimitive()) {
            return 0;
        }
        try {
            return element.getAsInt();
        } catch (NumberFormatException | UnsupportedOperationException ignored) {
            return 0;
        }
    }

    public List<String> getExtraStringList(String key) {
        List<String> values = new ArrayList<>();
        if (extra == null) {
            return values;
        }
        JsonElement element = extra.get(key);
        if (element == null || !element.isJsonArray()) {
            return values;
        }
        JsonArray array = element.getAsJsonArray();
        for (JsonElement item : array) {
            if (item != null && item.isJsonPrimitive()) {
                values.add(item.getAsString());
            }
        }
        return values;
    }

    public String getPriceLabel() {
        return getExtraString("price_label");
    }

    public String getPriceRange() {
        return getExtraString("estimated_price_range");
    }

    public String getBookingLabel() {
        return getExtraString("booking_label");
    }

    public String getBookingUrl() {
        return getExtraString("booking_url");
    }

    public String getDisclaimer() {
        return getExtraString("disclaimer");
    }

    public String getPriceStatus() {
        return getExtraString("price_status");
    }

    public String getVerificationUrl() {
        return getExtraString("verification_url");
    }

    /** 交通候选的分段构成，例如「铁路」「地铁/公交」。 */
    public List<String> getSegments() {
        return getExtraStringList("segments");
    }

    public int getTransferBufferMinutes() {
        return getExtraInt("transfer_buffer_minutes");
    }

    public int getCarbonKgEstimate() {
        return getExtraInt("carbon_kg_estimate");
    }

    /** 住宿候选的所在区域。 */
    public String getArea() {
        return getExtraString("area");
    }
}
