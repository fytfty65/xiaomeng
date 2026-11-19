package com.omniroute.app.core.network.dto;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/** GET /api/v1/planning/execution 的 data。 */
public final class ExecutionData {

    @SerializedName("trip_id")
    private String tripId;

    @SerializedName("states")
    private List<ExecutionStateDto> states;

    @SerializedName("summary")
    private ExecutionSummaryDto summary;

    public String getTripId() {
        return tripId;
    }

    public List<ExecutionStateDto> getStates() {
        return states == null ? new ArrayList<>() : states;
    }

    public ExecutionSummaryDto getSummary() {
        return summary;
    }
}
