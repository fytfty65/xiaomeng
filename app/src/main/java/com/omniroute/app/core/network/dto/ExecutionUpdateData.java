package com.omniroute.app.core.network.dto;

import com.google.gson.annotations.SerializedName;

/** POST /api/v1/planning/execution 的 data。 */
public final class ExecutionUpdateData {

    @SerializedName("state")
    private ExecutionStateDto state;

    @SerializedName("event_type")
    private String eventType;

    /** 延迟 ≥30 分钟时才会返回，需要用户确认后才可应用。 */
    @SerializedName("replan_proposal")
    private ReplanProposalDto replanProposal;

    public ExecutionStateDto getState() {
        return state;
    }

    public String getEventType() {
        return eventType;
    }

    public ReplanProposalDto getReplanProposal() {
        return replanProposal;
    }
}
