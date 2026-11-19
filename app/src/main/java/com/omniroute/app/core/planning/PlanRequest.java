package com.omniroute.app.core.planning;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 一次 AI 规划的输入参数。
 * 后端的 agent_negotiate 只开放 destinations / mode / role / intent 等字段，
 * 因此日期、预算与偏好会被编排进 intent 文本里下发给推理引擎。
 */
public final class PlanRequest {

    private final String destination;
    private final String origin;
    private final String startDate;
    private final int days;
    private final int travelers;
    private final int budget;
    private final List<String> preferences;

    public PlanRequest(
            String destination,
            String origin,
            String startDate,
            int days,
            int travelers,
            int budget,
            List<String> preferences) {
        this.destination = destination == null ? "" : destination.trim();
        this.origin = origin == null ? "" : origin.trim();
        this.startDate = startDate == null ? "" : startDate.trim();
        this.days = days;
        this.travelers = travelers;
        this.budget = budget;
        this.preferences = preferences == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(preferences));
    }

    public String getDestination() {
        return destination;
    }

    public String getOrigin() {
        return origin;
    }

    public String getStartDate() {
        return startDate;
    }

    public int getDays() {
        return days;
    }

    public int getTravelers() {
        return travelers;
    }

    public int getBudget() {
        return budget;
    }

    public List<String> getPreferences() {
        return preferences;
    }

    /**
     * 路书数据来自高德 POI，只覆盖中国大陆城市。
     *
     * <p>引擎侧的城市识别只认中文地名：非中文目的地（如 Osaka）匹配不到任何中文规则，
     * 会静默兜底成默认城市，用户拿到一份完全不相干的方案。与其给错误结果，不如提交前就拦下。
     */
    public static boolean isSupportedDestination(String destination) {
        String text = destination == null ? "" : destination.trim();
        if (text.isEmpty()) {
            return false;
        }
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch >= '\u4e00' && ch <= '\u9fff') {
                return true;
            }
        }
        return false;
    }

    /** 把结构化参数拼成一段可读诉求，供多智能体推演使用。 */
    public String buildIntent() {
        StringBuilder builder = new StringBuilder();
        builder.append("目的地：").append(destination);
        if (!origin.isEmpty()) {
            builder.append("；出发地：").append(origin);
        }
        if (!startDate.isEmpty()) {
            builder.append("；出发日期：").append(startDate);
        }
        builder.append("；行程天数：").append(days).append(" 天");
        builder.append("；出行人数：").append(travelers).append(" 人");
        if (budget > 0) {
            builder.append("；总预算：").append(budget).append(" 元");
        }
        if (!preferences.isEmpty()) {
            builder.append("；偏好：").append(String.join("、", preferences));
        }
        return builder.toString();
    }
}
