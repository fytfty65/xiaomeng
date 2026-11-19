package com.omniroute.app.core.network.dto;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/** /planning/transport/options 的 data。 */
public final class TransportOptionsData {

    @SerializedName("context")
    private PlanningContextDto context;

    @SerializedName("options")
    private List<CandidateDto> options;

    @SerializedName("optimization")
    private JsonObject optimization;

    @SerializedName("source")
    private CandidateSourceDto source;

    public PlanningContextDto getContext() {
        return context;
    }

    public List<CandidateDto> getOptions() {
        return options == null ? new ArrayList<>() : options;
    }

    public JsonObject getOptimization() {
        return optimization;
    }

    public CandidateSourceDto getSource() {
        return source;
    }
}
