package com.omniroute.app.core.network.dto;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/** /planning/destination/recommend 的 data。 */
public final class DestinationRecommendData {

    @SerializedName("context")
    private PlanningContextDto context;

    @SerializedName("candidates")
    private List<CandidateDto> candidates;

    @SerializedName("ranking")
    private List<String> ranking;

    @SerializedName("source")
    private CandidateSourceDto source;

    @SerializedName("fallback")
    private boolean fallback;

    public PlanningContextDto getContext() {
        return context;
    }

    public List<CandidateDto> getCandidates() {
        return candidates == null ? new ArrayList<>() : candidates;
    }

    public List<String> getRanking() {
        return ranking == null ? new ArrayList<>() : ranking;
    }

    public CandidateSourceDto getSource() {
        return source;
    }

    /** true 表示后端没有可用供应商，返回的是本地兜底候选。 */
    public boolean isFallback() {
        return fallback;
    }
}
