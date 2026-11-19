package com.omniroute.app.core.network.dto;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 执行状态汇总；既用于 GET /planning/execution，也用于 journey/state.execution。 */
public final class ExecutionSummaryDto {

    @SerializedName("total")
    private int total;

    @SerializedName("completed")
    private int completed;

    @SerializedName("by_status")
    private Map<String, Integer> byStatus;

    @SerializedName("states")
    private List<ExecutionStateDto> states;

    public int getTotal() {
        return total;
    }

    public int getCompleted() {
        return completed;
    }

    public Map<String, Integer> getByStatus() {
        return byStatus == null ? new HashMap<>() : byStatus;
    }

    public List<ExecutionStateDto> getStates() {
        return states == null ? new ArrayList<>() : states;
    }
}
