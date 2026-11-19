package com.omniroute.app.core.planning;

import com.omniroute.app.core.network.dto.CandidateDto;

import java.util.ArrayList;
import java.util.List;

/**
 * 一次规划生成结果（只读快照）。界面直接展示各分组候选，
 * 保存行程时由仓库层取每组得分最高的候选写入 content。
 */
public final class TripDraft {

    private final String resourceId;
    private final String destination;
    private final String origin;
    private final int days;
    private final int travelers;
    private final List<CandidateDto> destinationCandidates;
    private final List<CandidateDto> transportOptions;
    private final List<CandidateDto> lodgingOptions;
    private final List<CandidateDto> diningOptions;
    private final List<String> destinationRanking;
    private final boolean destinationFallback;

    TripDraft(
            String resourceId,
            String destination,
            String origin,
            int days,
            int travelers,
            List<CandidateDto> destinationCandidates,
            List<CandidateDto> transportOptions,
            List<CandidateDto> lodgingOptions,
            List<CandidateDto> diningOptions,
            List<String> destinationRanking,
            boolean destinationFallback) {
        this.resourceId = resourceId;
        this.destination = destination;
        this.origin = origin;
        this.days = days;
        this.travelers = travelers;
        this.destinationCandidates = copy(destinationCandidates);
        this.transportOptions = copy(transportOptions);
        this.lodgingOptions = copy(lodgingOptions);
        this.diningOptions = copy(diningOptions);
        this.destinationRanking = copy(destinationRanking);
        this.destinationFallback = destinationFallback;
    }

    private static <T> List<T> copy(List<T> source) {
        return source == null ? new ArrayList<>() : new ArrayList<>(source);
    }

    public String getResourceId() {
        return resourceId;
    }

    public String getDestination() {
        return destination;
    }

    public String getOrigin() {
        return origin;
    }

    public int getDays() {
        return days;
    }

    public int getTravelers() {
        return travelers;
    }

    public List<CandidateDto> getDestinationCandidates() {
        return destinationCandidates;
    }

    public List<CandidateDto> getTransportOptions() {
        return transportOptions;
    }

    public List<CandidateDto> getLodgingOptions() {
        return lodgingOptions;
    }

    public List<CandidateDto> getDiningOptions() {
        return diningOptions;
    }

    public List<String> getDestinationRanking() {
        return destinationRanking;
    }

    /** 目的地推荐是否来自后端本地兜底（无实时供应商）。 */
    public boolean isDestinationFallback() {
        return destinationFallback;
    }
}
