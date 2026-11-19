package com.omniroute.app.core.network.dto;

import com.google.gson.annotations.SerializedName;

/** journey/state 中的风险快照；只读，不直接修改行程。 */
public final class RiskStateDto {

    @SerializedName("status")
    private String status;

    @SerializedName("triggered_at")
    private String triggeredAt;

    @SerializedName("proposal")
    private ReplanProposalDto proposal;

    public String getStatus() {
        return status;
    }

    public String getTriggeredAt() {
        return triggeredAt;
    }

    public ReplanProposalDto getProposal() {
        return proposal;
    }
}
