package com.omniroute.app.core.network.dto;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** GET /api/v1/planning/journey/state 的 data：执行、风险、预算的只读汇总。 */
public final class JourneyStateData {

    @SerializedName("trip_id")
    private String tripId;

    @SerializedName("phase")
    private String phase;

    @SerializedName("progress")
    private double progress;

    @SerializedName("execution")
    private ExecutionSummaryDto execution;

    @SerializedName("risk")
    private RiskStateDto risk;

    @SerializedName("budget")
    private Map<String, Object> budget;

    @SerializedName("next_actions")
    private List<String> nextActions;

    @SerializedName("updated_at")
    private String updatedAt;

    public String getTripId() {
        return tripId;
    }

    /** planned / in_progress / completed */
    public String getPhase() {
        return phase;
    }

    public double getProgress() {
        return progress;
    }

    public ExecutionSummaryDto getExecution() {
        return execution;
    }

    public RiskStateDto getRisk() {
        return risk;
    }

    public Map<String, Object> getBudget() {
        return budget;
    }

    public List<String> getNextActions() {
        return nextActions == null ? new ArrayList<>() : nextActions;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }
}
