package com.omniroute.app.core.network.dto;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/**
 * 执行延迟达到阈值时后端生成的待确认重规划提案。
 * 客户端只能展示与确认/拒绝，不能自行修改提案内容。
 */
public final class ReplanProposalDto {

    @SerializedName("proposal_id")
    private String proposalId;

    @SerializedName("status")
    private String status;

    @SerializedName("trip_id")
    private String tripId;

    @SerializedName("node_key")
    private String nodeKey;

    @SerializedName("reason")
    private String reason;

    @SerializedName("delay_minutes")
    private int delayMinutes;

    @SerializedName("changed_signals")
    private List<String> changedSignals;

    @SerializedName("confirmation_required")
    private boolean confirmationRequired;

    @SerializedName("generated_at")
    private String generatedAt;

    @SerializedName("expires_at")
    private String expiresAt;

    @SerializedName("deduplicated")
    private boolean deduplicated;

    public String getProposalId() {
        return proposalId;
    }

    public String getStatus() {
        return status;
    }

    public String getTripId() {
        return tripId;
    }

    public String getNodeKey() {
        return nodeKey;
    }

    public String getReason() {
        return reason;
    }

    public int getDelayMinutes() {
        return delayMinutes;
    }

    public List<String> getChangedSignals() {
        return changedSignals == null ? new ArrayList<>() : changedSignals;
    }

    public boolean isConfirmationRequired() {
        return confirmationRequired;
    }

    public String getGeneratedAt() {
        return generatedAt;
    }

    public String getExpiresAt() {
        return expiresAt;
    }

    public boolean isDeduplicated() {
        return deduplicated;
    }

    /** proposal 的 node_key 形如 "2:西湖"，展示时取冒号后的节点名。 */
    public String getAffectedNodeName() {
        String key = nodeKey == null ? "" : nodeKey.trim();
        int index = key.indexOf(':');
        return index >= 0 ? key.substring(index + 1) : key;
    }
}
