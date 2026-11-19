package com.omniroute.app.core.network.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/** /planning/context 的 data：回显上下文并标记就绪。 */
public final class PlanningContextData {

    @SerializedName("context")
    private PlanningContextDto context;

    @SerializedName("ready")
    private boolean ready;

    public PlanningContextDto getContext() {
        return context;
    }

    public boolean isReady() {
        return ready;
    }
}
