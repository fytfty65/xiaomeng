package com.omniroute.app.core.planning;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * final_route.payload.route[] 里的单个行程节点。
 *
 * <p>后端字段名在历史上出现过 name/location、lnglat 三种形态（数组/字符串/对象），
 * 这里做一次归一化，界面层只消费稳定后的值。
 */
public final class RouteNode {

    private final int day;
    private final String name;
    private final String time;
    private final String transport;
    private final String costEstimate;
    private final String description;
    private final String address;
    private final String openTime;
    private final String rating;
    private final List<String> tags;
    private final boolean hotel;
    private final double lng;
    private final double lat;
    private final boolean hasCoordinate;
    // 后端 node_contract 在最终路线上补充的可信度与时序字段（2026-10-08 起）
    private final String officialUrl;
    private final String sourceLabel;
    private final String observedAt;
    private final boolean estimated;
    private final String trust;
    private final String trustLabel;
    private final String trustAction;
    private final String trustMessage;

    private RouteNode(
            int day,
            String name,
            String time,
            String transport,
            String costEstimate,
            String description,
            String address,
            String openTime,
            String rating,
            List<String> tags,
            boolean hotel,
            double lng,
            double lat,
            boolean hasCoordinate,
            String officialUrl,
            String sourceLabel,
            String observedAt,
            boolean estimated,
            String trust,
            String trustLabel,
            String trustAction,
            String trustMessage) {
        this.day = day;
        this.name = name;
        this.time = time;
        this.transport = transport;
        this.costEstimate = costEstimate;
        this.description = description;
        this.address = address;
        this.openTime = openTime;
        this.rating = rating;
        this.tags = tags;
        this.hotel = hotel;
        this.lng = lng;
        this.lat = lat;
        this.hasCoordinate = hasCoordinate;
        this.officialUrl = officialUrl;
        this.sourceLabel = sourceLabel;
        this.observedAt = observedAt;
        this.estimated = estimated;
        this.trust = trust;
        this.trustLabel = trustLabel;
        this.trustAction = trustAction;
        this.trustMessage = trustMessage;
    }

    /** 官方核验/预订链接（节点自带 official_url/booking_url/url 时才有）。 */
    public String getOfficialUrl() {
        return officialUrl;
    }

    public String getSourceLabel() {
        return sourceLabel;
    }

    public String getObservedAt() {
        return observedAt;
    }

    public boolean isEstimated() {
        return estimated;
    }

    public String getTrust() {
        return trust;
    }

    public String getTrustLabel() {
        return trustLabel;
    }

    public String getTrustAction() {
        return trustAction;
    }

    public String getTrustMessage() {
        return trustMessage;
    }

    /** 数据时效标签：已核实/估算/信息较旧/尚未核实。 */
    public String freshnessLabel() {
        if (trust != null && !trust.isEmpty()) {
            return trustLabel != null && !trustLabel.isEmpty() ? trustLabel : trust;
        }
        return estimated ? "估算" : "尚未核实";
    }

    public int getDay() {
        return day;
    }

    public String getName() {
        return name;
    }

    public String getTime() {
        return time;
    }

    public String getTransport() {
        return transport;
    }

    public String getCostEstimate() {
        return costEstimate;
    }

    public String getDescription() {
        return description;
    }

    public String getAddress() {
        return address;
    }

    public String getOpenTime() {
        return openTime;
    }

    public String getRating() {
        return rating;
    }

    public List<String> getTags() {
        return tags;
    }

    public boolean isHotel() {
        return hotel;
    }

    public double getLng() {
        return lng;
    }

    public double getLat() {
        return lat;
    }

    public boolean hasCoordinate() {
        return hasCoordinate;
    }

    /**
     * 编辑页手动新增停靠点：只收集用户能填的字段。
     *
     * <p>经纬度选填，缺失时该点只在列表里出现、不画到地图上（需要真实坐标才能投影）。
     */
    public static RouteNode create(
            int day,
            String name,
            String time,
            String category,
            String address,
            String lnglat) {
        String safeName = trim(name);
        if (safeName.isEmpty()) {
            return null;
        }
        String safeCategory = trim(category);
        double lng = 0;
        double lat = 0;
        boolean hasCoordinate = false;
        // 仅用于让用户能补一个已知坐标的私有点位，格式不对就按缺失处理。
        String[] pair = trim(lnglat).split(",");
        if (pair.length >= 2) {
            try {
                double parsedLng = Double.parseDouble(pair[0].trim());
                double parsedLat = Double.parseDouble(pair[1].trim());
                if (parsedLng > -180 && parsedLng < 180 && parsedLat > -90 && parsedLat < 90) {
                    lng = parsedLng;
                    lat = parsedLat;
                    hasCoordinate = true;
                }
            } catch (NumberFormatException ignored) {
                // 坐标留空或格式不对都不阻断新增，只是不上地图。
            }
        }
        List<String> tags = new ArrayList<>();
        if (!safeCategory.isEmpty()) {
            tags.add(safeCategory);
        }
        return new RouteNode(
                day,
                safeName,
                trim(time),
                "",
                "",
                "",
                trim(address),
                "",
                "",
                tags,
                safeCategory.contains("住宿"),
                lng,
                lat,
                hasCoordinate,
                "",
                "",
                "",
                false,
                "",
                "",
                "",
                "");
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }

    /** 路线编辑时把停靠点调整到另一天，其余字段保持不变。 */
    public RouteNode withDay(int nextDay) {
        if (nextDay == day) {
            return this;
        }
        return new RouteNode(
                nextDay,
                name,
                time,
                transport,
                costEstimate,
                description,
                address,
                openTime,
                rating,
                tags,
                hotel,
                lng,
                lat,
                hasCoordinate,
                officialUrl,
                sourceLabel,
                observedAt,
                estimated,
                trust,
                trustLabel,
                trustAction,
                trustMessage);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof RouteNode)) {
            return false;
        }
        RouteNode that = (RouteNode) other;
        return day == that.day
                && hotel == that.hotel
                && hasCoordinate == that.hasCoordinate
                && estimated == that.estimated
                && Double.compare(lng, that.lng) == 0
                && Double.compare(lat, that.lat) == 0
                && Objects.equals(name, that.name)
                && Objects.equals(time, that.time)
                && Objects.equals(transport, that.transport)
                && Objects.equals(costEstimate, that.costEstimate)
                && Objects.equals(description, that.description)
                && Objects.equals(address, that.address)
                && Objects.equals(openTime, that.openTime)
                && Objects.equals(rating, that.rating)
                && Objects.equals(tags, that.tags)
                && Objects.equals(officialUrl, that.officialUrl)
                && Objects.equals(sourceLabel, that.sourceLabel)
                && Objects.equals(observedAt, that.observedAt)
                && Objects.equals(trust, that.trust)
                && Objects.equals(trustLabel, that.trustLabel)
                && Objects.equals(trustAction, that.trustAction)
                && Objects.equals(trustMessage, that.trustMessage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                day,
                name,
                time,
                transport,
                costEstimate,
                description,
                address,
                openTime,
                rating,
                tags,
                hotel,
                lng,
                lat,
                hasCoordinate,
                officialUrl,
                sourceLabel,
                observedAt,
                estimated,
                trust,
                trustLabel,
                trustAction,
                trustMessage);
    }

    /** 行程卡片使用的类型标签（住宿/餐饮/交通/景点）。 */
    public String getCategory() {
        if (hotel) {
            return "住宿";
        }
        for (String tag : tags) {
            if (tag.contains("食") || tag.contains("餐") || tag.contains("寻味")) {
                return "餐饮";
            }
            if (tag.contains("交通") || tag.contains("接驳")) {
                return "交通";
            }
        }
        // transport 表示「怎么去这个点」，不代表这个点本身是交通设施：
        // 按它分类会把博物馆、公园一律标成交通，因此只看名称里的枢纽特征。
        if (name.contains("机场") || name.contains("车站") || name.contains("码头")
                || name.contains("高铁") || name.contains("地铁")) {
            return "交通";
        }
        return "景点";
    }

    static RouteNode parse(JsonElement element) {
        return parse(element, 1);
    }

    /** fallbackDay 用于 content 里「路线挂在 days[i].routes 下、自身没写 day」的历史数据。 */
    static RouteNode parse(JsonElement element, int fallbackDay) {
        if (element == null || !element.isJsonObject()) {
            return null;
        }
        JsonObject obj = element.getAsJsonObject();
        String name = firstNonEmpty(asString(obj, "location"), asString(obj, "name"));
        if (name.isEmpty()) {
            return null;
        }
        int day = asInt(obj, "day", fallbackDay);
        if (day < 1) {
            day = fallbackDay;
        }
        List<String> tags = asStringList(obj.get("tags"));
        boolean hotel = asBool(obj, "is_hotel")
                || name.contains("酒店") || name.contains("民宿") || name.contains("客栈");
        for (String tag : tags) {
            if (tag.contains("住宿")) {
                hotel = true;
            }
        }

        // 推演结果用 lnglat，保存后的 content 用顶层 lng/lat；两种都要能读回来。
        double[] coordinate = parseCoordinate(obj.get("lnglat"));
        if (coordinate == null) {
            coordinate = parseLngLat(obj);
        }
        boolean hasCoordinate = coordinate != null;
        double lng = hasCoordinate ? coordinate[0] : 0;
        double lat = hasCoordinate ? coordinate[1] : 0;

        String trust = asString(obj, "trust");
        boolean estimated = asBool(obj, "estimated") || "estimated".equals(trust);

        return new RouteNode(
                day,
                name,
                asString(obj, "time"),
                asString(obj, "transport"),
                firstNonEmpty(
                        firstNonEmpty(asString(obj, "cost_estimate"), asString(obj, "cost")),
                        asString(obj, "price_label")),
                firstNonEmpty(asString(obj, "desc"), asString(obj, "action")),
                asString(obj, "address"),
                asString(obj, "open_time"),
                asString(obj, "rating"),
                tags,
                hotel,
                lng,
                lat,
                hasCoordinate,
                firstNonEmpty(
                        firstNonEmpty(asString(obj, "official_url"), asString(obj, "booking_url")),
                        firstNonEmpty(asString(obj, "url"), asString(obj, "amap_url"))),
                firstNonEmpty(asString(obj, "source_label"), asString(obj, "source")),
                firstNonEmpty(asString(obj, "observed_at"), asString(obj, "price_retrieved_at")),
                estimated,
                trust,
                asString(obj, "trust_label"),
                asString(obj, "trust_action"),
                asString(obj, "trust_message"));
    }

    /** content 里的顶层 lng / lat 数值字段。 */
    private static double[] parseLngLat(JsonObject obj) {
        double lng = asDouble(obj.get("lng"));
        double lat = asDouble(obj.get("lat"));
        if (Double.isNaN(lng) || Double.isNaN(lat)) {
            return null;
        }
        if (lng <= -180 || lng >= 180 || lat <= -90 || lat >= 90) {
            return null;
        }
        if (lng == 0 && lat == 0) {
            return null;
        }
        return new double[]{lng, lat};
    }

    /** 支持 [lng,lat] / "lng,lat" / {lng,lat}；非法坐标视为缺失而不是 0,0。 */
    private static double[] parseCoordinate(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return null;
        }
        double lng = 0;
        double lat = 0;
        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            if (array.size() < 2) {
                return null;
            }
            lng = asDouble(array.get(0));
            lat = asDouble(array.get(1));
        } else if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            lng = asDouble(obj.get("lng"));
            lat = asDouble(obj.get("lat"));
        } else if (element.isJsonPrimitive()) {
            JsonPrimitive primitive = element.getAsJsonPrimitive();
            if (!primitive.isString()) {
                return null;
            }
            String raw = primitive.getAsString().trim();
            String[] parts = raw.split(",");
            if (parts.length < 2) {
                return null;
            }
            lng = asDouble(new JsonPrimitive(parts[0].trim()));
            lat = asDouble(new JsonPrimitive(parts[1].trim()));
        }
        if (Double.isNaN(lng) || Double.isNaN(lat)) {
            return null;
        }
        if (lng <= -180 || lng >= 180 || lat <= -90 || lat >= 90) {
            return null;
        }
        return new double[]{lng, lat};
    }

    private static List<String> asStringList(JsonElement element) {
        List<String> values = new ArrayList<>();
        if (element == null || !element.isJsonArray()) {
            return values;
        }
        for (JsonElement item : element.getAsJsonArray()) {
            String value = asString(item);
            if (!value.isEmpty()) {
                values.add(value);
            }
        }
        return values;
    }

    private static String firstNonEmpty(String first, String second) {
        if (first != null && !first.trim().isEmpty()) {
            return first.trim();
        }
        return second == null ? "" : second.trim();
    }

    private static String asString(JsonObject obj, String key) {
        return asString(obj.get(key));
    }

    private static String asString(JsonElement element) {
        if (element == null || element.isJsonNull() || !element.isJsonPrimitive()) {
            return "";
        }
        return element.getAsString();
    }

    private static int asInt(JsonObject obj, String key, int fallback) {
        JsonElement element = obj.get(key);
        if (element == null || element.isJsonNull()) {
            return fallback;
        }
        try {
            if (element.isJsonPrimitive()) {
                JsonPrimitive primitive = element.getAsJsonPrimitive();
                if (primitive.isNumber()) {
                    return primitive.getAsInt();
                }
                return Integer.parseInt(primitive.getAsString().trim());
            }
        } catch (NumberFormatException ignored) {
            return fallback;
        }
        return fallback;
    }

    private static boolean asBool(JsonObject obj, String key) {
        JsonElement element = obj.get(key);
        if (element == null || element.isJsonNull() || !element.isJsonPrimitive()) {
            return false;
        }
        try {
            return element.getAsBoolean();
        } catch (UnsupportedOperationException | IllegalStateException ignored) {
            return false;
        }
    }

    private static double asDouble(JsonElement element) {
        if (element == null || element.isJsonNull() || !element.isJsonPrimitive()) {
            return Double.NaN;
        }
        try {
            return element.getAsDouble();
        } catch (NumberFormatException ignored) {
            return Double.NaN;
        }
    }
}
