package com.omniroute.app.core.planning;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 行程 content 的只读视图。
 *
 * <p>content 是 Web 端「个人主页」直接解析的同一份 JSON，约定结构为
 * {summary, city, days:[{day}], routes:[{name, type, day, duration_minutes,
 * estimated, price_label, booking_url}]}。这里只做容错解析，不修改原始数据。
 */
public final class TripItinerary {

    private final String summary;
    private final String city;
    private final List<Day> days;

    private TripItinerary(String summary, String city, List<Day> days) {
        this.summary = summary;
        this.city = city;
        this.days = days;
    }

    /** content 为空或不是合法 JSON 时返回空行程，界面据此展示兜底文案。 */
    public static TripItinerary parse(String content) {
        String raw = content == null ? "" : content.trim();
        if (raw.isEmpty()) {
            return empty();
        }
        JsonObject root;
        try {
            JsonElement element = JsonParser.parseString(raw);
            root = element.isJsonObject() ? element.getAsJsonObject() : null;
        } catch (RuntimeException ignored) {
            root = null;
        }
        if (root == null) {
            return empty();
        }

        String summary = text(root, "summary");
        String city = text(root, "city");

        Map<Integer, List<Item>> byDay = new LinkedHashMap<>();
        int declaredDays = 0;
        JsonArray dayArray = array(root, "days");
        if (dayArray != null) {
            for (JsonElement element : dayArray) {
                declaredDays++;
                int number = declaredDays;
                if (element != null && element.isJsonObject()) {
                    JsonObject dayObject = element.getAsJsonObject();
                    int own = integer(dayObject, "day");
                    if (own > 0) {
                        number = own;
                    }
                    collectRoutes(dayObject, number, byDay);
                }
            }
        }

        collectRoutes(root, 1, byDay);

        int totalDays = declaredDays;
        for (Integer number : byDay.keySet()) {
            if (number > totalDays) {
                totalDays = number;
            }
        }
        if (totalDays < 1) {
            totalDays = 1;
        }

        List<Day> days = new ArrayList<>();
        for (int index = 1; index <= totalDays; index++) {
            List<Item> items = byDay.get(index);
            days.add(new Day(index, items == null ? new ArrayList<>() : items));
        }
        return new TripItinerary(summary, city, days);
    }

    private static TripItinerary empty() {
        return new TripItinerary(null, null, new ArrayList<>());
    }

    /** 供排程规则直接构造按天视图，避免走 JSON 序列化再解析。 */
    public static TripItinerary fromDays(List<Day> days) {
        return new TripItinerary(null, null, new ArrayList<>(days));
    }

    /** routes 可能是数组，也可能是单个对象；统一按天归档，保持原始顺序。 */
    private static void collectRoutes(JsonObject source, int fallbackDay, Map<Integer, List<Item>> byDay) {
        JsonElement routes = source == null ? null : source.get("routes");
        if (routes == null) {
            return;
        }
        if (routes.isJsonArray()) {
            for (JsonElement element : routes.getAsJsonArray()) {
                addRoute(element, fallbackDay, byDay);
            }
        } else if (routes.isJsonObject()) {
            addRoute(routes, fallbackDay, byDay);
        }
    }

    private static void addRoute(JsonElement element, int fallbackDay, Map<Integer, List<Item>> byDay) {
        if (element == null || !element.isJsonObject()) {
            return;
        }
        JsonObject route = element.getAsJsonObject();
        String name = firstText(route, "name", "title", "location");
        if (name == null) {
            return;
        }
        int day = integer(route, "day");
        if (day < 1) {
            day = fallbackDay;
        }
        Item item = new Item(
                name,
                text(route, "type"),
                integer(route, "duration_minutes"),
                bool(route, "estimated"),
                text(route, "price_label"),
                firstText(route, "official_url", "booking_url", "url"),
                text(route, "time"),
                firstText(route, "desc", "note"),
                text(route, "address"),
                decimal(route, "lng"),
                decimal(route, "lat"),
                text(route, "trust_label"),
                text(route, "observed_at"));
        List<Item> items = byDay.get(day);
        if (items == null) {
            items = new ArrayList<>();
            byDay.put(day, items);
        }
        items.add(item);
    }

    public String getSummary() {
        return summary;
    }

    public String getCity() {
        return city;
    }

    public List<Day> getDays() {
        return days;
    }

    /** 是否有可展示的日程内容。 */
    public boolean hasContent() {
        for (Day day : days) {
            if (!day.getItems().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static JsonArray array(JsonObject source, String key) {
        JsonElement element = source.get(key);
        return element != null && element.isJsonArray() ? element.getAsJsonArray() : null;
    }

    private static String firstText(JsonObject source, String... keys) {
        for (String key : keys) {
            String value = text(source, key);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private static String text(JsonObject source, String key) {
        JsonElement element = source.get(key);
        if (element == null || element.isJsonNull() || !element.isJsonPrimitive()) {
            return null;
        }
        String value = element.getAsString();
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private static int integer(JsonObject source, String key) {
        JsonElement element = source.get(key);
        if (element == null || !element.isJsonPrimitive()) {
            return 0;
        }
        try {
            return element.getAsInt();
        } catch (NumberFormatException | UnsupportedOperationException ignored) {
            return 0;
        }
    }

    private static boolean bool(JsonObject source, String key) {
        JsonElement element = source.get(key);
        if (element == null || !element.isJsonPrimitive()) {
            return false;
        }
        try {
            return element.getAsBoolean();
        } catch (UnsupportedOperationException ignored) {
            return false;
        }
    }

    private static double decimal(JsonObject source, String key) {
        JsonElement element = source.get(key);
        if (element == null || !element.isJsonPrimitive()) {
            return 0;
        }
        try {
            return element.getAsDouble();
        } catch (NumberFormatException | UnsupportedOperationException ignored) {
            return 0;
        }
    }

    /** 一天的日程。 */
    public static final class Day {

        private final int dayNumber;
        private final List<Item> items;

        Day(int dayNumber, List<Item> items) {
            this.dayNumber = dayNumber;
            this.items = new ArrayList<>(items);
        }

        public int getDayNumber() {
            return dayNumber;
        }

        public List<Item> getItems() {
            return items;
        }
    }

    /** 一天中的一个安排节点（主题/交通/住宿/餐饮等）。 */
    public static final class Item {

        private final String name;
        private final String type;
        private final int durationMinutes;
        private final boolean estimated;
        private final String priceLabel;
        private final String bookingUrl;
        private final String time;
        private final String description;
        private final String address;
        private final double lng;
        private final double lat;
        private final String trustLabel;
        private final String observedAt;

        Item(
                String name,
                String type,
                int durationMinutes,
                boolean estimated,
                String priceLabel,
                String bookingUrl,
                String time,
                String description,
                String address,
                double lng,
                double lat,
                String trustLabel,
                String observedAt) {
            this.name = name;
            this.type = type;
            this.durationMinutes = durationMinutes;
            this.estimated = estimated;
            this.priceLabel = priceLabel;
            this.bookingUrl = bookingUrl;
            this.time = time;
            this.description = description;
            this.address = address;
            this.lng = lng;
            this.lat = lat;
            this.trustLabel = trustLabel;
            this.observedAt = observedAt;
        }

        public String getName() {
            return name;
        }

        public String getType() {
            return type;
        }

        public int getDurationMinutes() {
            return durationMinutes;
        }

        public boolean isEstimated() {
            return estimated;
        }

        public String getPriceLabel() {
            return priceLabel;
        }

        public String getBookingUrl() {
            return bookingUrl;
        }

        public String getTime() {
            return time;
        }

        public String getDescription() {
            return description;
        }

        public String getAddress() {
            return address;
        }

        public double getLng() {
            return lng;
        }

        public double getLat() {
            return lat;
        }

        public boolean hasCoordinate() {
            return lng > -180 && lng < 180 && lat > -90 && lat < 90
                    && !(lng == 0 && lat == 0);
        }

        public String getTrustLabel() {
            return trustLabel;
        }

        public String getObservedAt() {
            return observedAt;
        }
    }
}
