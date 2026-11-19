package com.omniroute.app.core.planning;

import com.omniroute.app.core.network.dto.CandidateDto;

import java.util.ArrayList;
import java.util.List;

/**
 * 排程预览：把一次规划得到的候选按天编排成行程日视图。
 *
 * <p>结果页的「日视图预览」与保存到「我的行程」的 content 都走这里，
 * 保证预览所见即保存所得，不会出现预览有安排、保存后空白的情况。
 */
public final class TripPreview {

    private TripPreview() {
    }

    /** 第 1 天排主题/交通/住宿，其余天数用餐饮候选轮换填充。 */
    public static TripItinerary fromDraft(TripDraft draft) {
        int totalDays = Math.max(1, draft.getDays());
        List<TripItinerary.Day> days = new ArrayList<>();
        List<CandidateDto> dining = usable(draft.getDiningOptions());

        for (int day = 1; day <= totalDays; day++) {
            List<TripItinerary.Item> items = new ArrayList<>();
            if (day == 1) {
                addItem(items, pickTop(draft.getDestinationCandidates()), "主题");
                addItem(items, pickTop(draft.getTransportOptions()), "交通");
                addItem(items, pickTop(draft.getLodgingOptions()), "住宿");
            }
            if (!dining.isEmpty()) {
                addItem(items, dining.get((day - 1) % dining.size()), "餐饮");
            }
            days.add(new TripItinerary.Day(day, items));
        }
        return TripItinerary.fromDays(days);
    }

    private static void addItem(List<TripItinerary.Item> items, CandidateDto candidate, String type) {
        if (candidate == null || candidate.getName() == null || candidate.getName().trim().isEmpty()) {
            return;
        }
        items.add(new TripItinerary.Item(
                candidate.getName().trim(),
                type,
                candidate.getDurationMinutes(),
                candidate.isEstimated(),
                candidate.getPriceLabel(),
                candidate.getBookingUrl(),
                null,
                null,
                null,
                0,
                0,
                null,
                null));
    }

    private static List<CandidateDto> usable(List<CandidateDto> source) {
        List<CandidateDto> result = new ArrayList<>();
        for (CandidateDto candidate : source) {
            if (candidate != null && candidate.getName() != null && !candidate.getName().trim().isEmpty()) {
                result.add(candidate);
            }
        }
        return result;
    }

    /** 后端已按排序算法排好序，这里仍按得分兜底取最高分。 */
    private static CandidateDto pickTop(List<CandidateDto> candidates) {
        CandidateDto best = null;
        for (CandidateDto candidate : candidates) {
            if (candidate == null || candidate.getName() == null) {
                continue;
            }
            if (best == null || candidate.getScore() > best.getScore()) {
                best = candidate;
            }
        }
        return best;
    }
}
