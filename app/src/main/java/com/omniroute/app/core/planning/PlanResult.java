package com.omniroute.app.core.planning;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** final_route / ai_negotiation_result 归一化后的规划结果。 */
public final class PlanResult {

    private final String status;
    private final String summary;
    private final List<RouteNode> route;
    private final JsonObject payload;

    private PlanResult(String status, String summary, List<RouteNode> route, JsonObject payload) {
        this.status = status;
        this.summary = summary;
        this.route = Collections.unmodifiableList(route);
        this.payload = payload;
    }

    public String getStatus() {
        return status;
    }

    public String getSummary() {
        return summary;
    }

    public List<RouteNode> getRoute() {
        return route;
    }

    public JsonObject getPayload() {
        return payload;
    }

    /** 路线包含的天数：以最大 day 为准，最少 1 天。 */
    public int getDays() {
        int max = 1;
        for (RouteNode node : route) {
            max = Math.max(max, node.getDay());
        }
        return max;
    }

    /** 后端降级/兜底标记（payload.fallback 存在时展示提示条）。 */
    public boolean isFallback() {
        return payload != null && payload.has("fallback") && !payload.get("fallback").isJsonNull();
    }

    public static PlanResult parse(JsonElement payloadElement) {
        if (payloadElement == null || !payloadElement.isJsonObject()) {
            return null;
        }
        JsonObject payload = payloadElement.getAsJsonObject();
        JsonArray routeArray = findRouteArray(payload);
        if (routeArray == null) {
            return null;
        }
        List<RouteNode> nodes = new ArrayList<>();
        for (JsonElement element : routeArray) {
            RouteNode node = RouteNode.parse(element);
            if (node != null) {
                nodes.add(node);
            }
        }
        if (nodes.isEmpty()) {
            return null;
        }
        String status = text(payload, "status");
        String summary = firstNonEmpty(text(payload, "negotiation_summary"), text(payload, "summary"));
        return new PlanResult(status, summary, nodes, payload);
    }

    /** 兼容 route 直接挂在 payload 下，或被包在 result/plan 子对象里的两种形态。 */
    private static JsonArray findRouteArray(JsonObject payload) {
        JsonArray direct = array(payload, "route");
        if (direct != null) {
            return direct;
        }
        for (String key : new String[]{"result", "plan", "data"}) {
            JsonElement nested = payload.get(key);
            if (nested != null && nested.isJsonObject()) {
                JsonArray nestedRoute = array(nested.getAsJsonObject(), "route");
                if (nestedRoute != null) {
                    return nestedRoute;
                }
            }
        }
        return null;
    }

    private static JsonArray array(JsonObject obj, String key) {
        JsonElement element = obj.get(key);
        if (element == null || !element.isJsonArray()) {
            return null;
        }
        return element.getAsJsonArray();
    }

    private static String text(JsonObject obj, String key) {
        JsonElement element = obj.get(key);
        if (element == null || element.isJsonNull() || !element.isJsonPrimitive()) {
            return "";
        }
        return element.getAsString().trim();
    }

    private static String firstNonEmpty(String first, String second) {
        return first == null || first.isEmpty() ? second : first;
    }
}
