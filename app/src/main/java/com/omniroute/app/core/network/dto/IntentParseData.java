package com.omniroute.app.core.network.dto;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/** /planning/intent/parse 的 data：只包含可确定的结构化字段，未识别项为空。 */
public final class IntentParseData {

    @SerializedName("city")
    private String city = "";

    @SerializedName("origin")
    private String origin = "";

    @SerializedName("days")
    private int days;

    @SerializedName("start_date")
    private String startDate = "";

    @SerializedName("preferences")
    private List<String> preferences = new ArrayList<>();

    public String getCity() {
        return city == null ? "" : city;
    }

    public String getOrigin() {
        return origin == null ? "" : origin;
    }

    public int getDays() {
        return days;
    }

    public String getStartDate() {
        return startDate == null ? "" : startDate;
    }

    public List<String> getPreferences() {
        return preferences == null ? new ArrayList<>() : preferences;
    }
}
