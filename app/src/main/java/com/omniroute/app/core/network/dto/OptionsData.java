package com.omniroute.app.core.network.dto;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/** /planning/lodging/options 与 /planning/dining/options 共用同一结构。 */
public final class OptionsData {

    @SerializedName("context")
    private PlanningContextDto context;

    @SerializedName("options")
    private List<CandidateDto> options;

    @SerializedName("ranking")
    private List<String> ranking;

    @SerializedName("source")
    private CandidateSourceDto source;

    public PlanningContextDto getContext() {
        return context;
    }

    public List<CandidateDto> getOptions() {
        return options == null ? new ArrayList<>() : options;
    }

    public List<String> getRanking() {
        return ranking == null ? new ArrayList<>() : ranking;
    }

    public CandidateSourceDto getSource() {
        return source;
    }
}
