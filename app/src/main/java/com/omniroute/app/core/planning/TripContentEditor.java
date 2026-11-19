package com.omniroute.app.core.planning;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.omniroute.app.core.network.AppJson;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 已保存行程 content 与路线编辑页之间的桥。
 *
 * <p>content 是 Web 端行程详情页直接解析的同一份 JSON。这里不回读整份内容重建，
 * 而是复用原始 route 对象，只把用户改动的 day / 顺序 / 备注写回去，避免丢掉
 * 客户端并不认识的字段（duration_minutes、source_label 等）。
 */
public final class TripContentEditor {

    private static final String NOTE_PREFIX = "备注：";
    private static final String NOTE_PREFIX_ALT = "备注:";

    private TripContentEditor() {
    }

    /** 把 content 里的 routes 转成编辑页使用的节点列表，保持原始顺序。 */
    public static List<RouteNode> readNodes(String content) {
        JsonObject root = object(content);
        List<RouteNode> nodes = new ArrayList<>();
        if (root == null) {
            return nodes;
        }
        for (Stored stored : collectStored(root)) {
            RouteNode node = RouteNode.parse(stored.json, stored.fallbackDay);
            if (node != null) {
                nodes.add(node);
            }
        }
        return nodes;
    }

    /** 读回已存在的备注，key 与编辑页一致（名称|时间）。 */
    public static Map<String, String> readNotes(String content) {
        JsonObject root = object(content);
        Map<String, String> notes = new LinkedHashMap<>();
        if (root == null) {
            return notes;
        }
        for (Stored stored : collectStored(root)) {
            String name = nameOf(stored.json);
            if (name.isEmpty()) {
                continue;
            }
            String note = text(stored.json, "note");
            if (note == null) {
                note = noteFromDesc(text(stored.json, "desc"));
            }
            if (note != null) {
                notes.put(name + "|" + textOrEmpty(stored.json, "time"), note);
            }
        }
        return notes;
    }

    /**
     * 把编辑结果写回 content。
     *
     * <p>day &lt; 1 的节点属于「待排池」，与保存链路一致：不写入行程。
     * 原本的 summary / 城市 / 天数 / 未知字段全部保留，只替换 routes 的顺序与归属天。
     */
    public static String apply(
            String content,
            String city,
            List<RouteNode> nodes,
            Map<String, String> notes,
            int dayCount) {
        JsonObject root = object(content);
        if (root == null) {
            root = new JsonObject();
        }

        // 原始 route 对象按「名称|时间」排队，编辑后的节点依次认领；
        // 认领不到的（新增节点）才新建对象。
        Map<String, Deque<JsonObject>> originals = new LinkedHashMap<>();
        for (Stored stored : collectStored(root)) {
            String key = nameOf(stored.json) + "|" + textOrEmpty(stored.json, "time");
            originals.computeIfAbsent(key, ignored -> new ArrayDeque<>()).add(stored.json);
        }

        JsonArray routes = new JsonArray();
        int maxDay = Math.max(1, dayCount);
        for (RouteNode node : nodes) {
            if (node.getDay() < 1) {
                continue;
            }
            maxDay = Math.max(maxDay, node.getDay());
            String key = node.getName() + "|" + node.getTime();
            Deque<JsonObject> pool = originals.get(key);
            JsonObject route = pool == null ? null : pool.pollFirst();
            if (route == null) {
                route = new JsonObject();
                route.addProperty("name", node.getName());
                String category = node.getCategory();
                if (!category.isEmpty()) {
                    route.addProperty("type", category);
                }
                if (!node.getTime().isEmpty()) {
                    route.addProperty("time", node.getTime());
                }
                if (node.hasCoordinate()) {
                    route.addProperty("lng", node.getLng());
                    route.addProperty("lat", node.getLat());
                }
                if (!node.getAddress().isEmpty()) {
                    route.addProperty("address", node.getAddress());
                }
            }
            route.addProperty("day", node.getDay());
            applyNote(route, notes == null ? null : notes.get(key));
            routes.add(route);
        }

        root.add("routes", routes);
        root.add("days", buildDays(root, maxDay));
        String safeCity = city == null ? "" : city.trim();
        if (!safeCity.isEmpty()) {
            root.addProperty("city", safeCity);
        }
        if (!root.has("summary")) {
            root.addProperty("summary", "");
        }
        return AppJson.gson().toJson(root);
    }

    private static JsonArray buildDays(JsonObject root, int maxDay) {
        JsonArray source = array(root, "days");
        int sourceCount = source == null ? 0 : source.size();
        int total = Math.max(maxDay, sourceCount);
        JsonArray days = new JsonArray();
        for (int index = 0; index < total; index++) {
            JsonObject day;
            if (index < sourceCount && source.get(index).isJsonObject()) {
                day = source.get(index).getAsJsonObject();
                // routes 统一提到顶层，避免同一节点被解析两次。
                day.remove("routes");
            } else {
                day = new JsonObject();
            }
            if (!day.has("day") || day.get("day").isJsonNull()) {
                day.addProperty("day", index + 1);
            }
            days.add(day);
        }
        return days;
    }

    private static void applyNote(JsonObject route, String note) {
        String clean = note == null ? "" : note.trim();
        String base = stripNoteLines(text(route, "desc"));
        if (clean.isEmpty()) {
            route.remove("note");
        } else {
            route.addProperty("note", clean);
        }
        String next = clean.isEmpty()
                ? base
                : (base.isEmpty() ? NOTE_PREFIX + clean : base + "\n" + NOTE_PREFIX + clean);
        if (next.isEmpty()) {
            route.remove("desc");
        } else {
            route.addProperty("desc", next);
        }
    }

    private static List<Stored> collectStored(JsonObject root) {
        List<Stored> list = new ArrayList<>();
        JsonArray rootRoutes = array(root, "routes");
        if (rootRoutes != null) {
            for (JsonElement element : rootRoutes) {
                if (element != null && element.isJsonObject()) {
                    list.add(new Stored(element.getAsJsonObject(), 1));
                }
            }
            return list;
        }
        JsonArray days = array(root, "days");
        if (days == null) {
            return list;
        }
        for (int index = 0; index < days.size(); index++) {
            JsonElement element = days.get(index);
            if (element == null || !element.isJsonObject()) {
                continue;
            }
            JsonObject day = element.getAsJsonObject();
            int number = integer(day, "day", index + 1);
            JsonArray routes = array(day, "routes");
            if (routes == null) {
                continue;
            }
            for (JsonElement route : routes) {
                if (route != null && route.isJsonObject()) {
                    list.add(new Stored(route.getAsJsonObject(), number));
                }
            }
        }
        return list;
    }

    private static JsonObject object(String content) {
        String raw = content == null ? "" : content.trim();
        if (raw.isEmpty() || !raw.startsWith("{")) {
            return null;
        }
        try {
            JsonElement element = JsonParser.parseString(raw);
            return element.isJsonObject() ? element.getAsJsonObject() : null;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static String stripNoteLines(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (String line : value.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith(NOTE_PREFIX) || trimmed.startsWith(NOTE_PREFIX_ALT)) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append(line);
        }
        return builder.toString().trim();
    }

    private static String noteFromDesc(String desc) {
        if (desc == null || desc.isEmpty()) {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        for (String line : desc.split("\n")) {
            String trimmed = line.trim();
            String value = null;
            if (trimmed.startsWith(NOTE_PREFIX)) {
                value = trimmed.substring(NOTE_PREFIX.length()).trim();
            } else if (trimmed.startsWith(NOTE_PREFIX_ALT)) {
                value = trimmed.substring(NOTE_PREFIX_ALT.length()).trim();
            }
            if (value == null || value.isEmpty()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append(value);
        }
        return builder.length() == 0 ? null : builder.toString();
    }

    private static String nameOf(JsonObject route) {
        String name = text(route, "location");
        if (name == null) {
            name = text(route, "name");
        }
        return name == null ? "" : name;
    }

    private static String text(JsonObject source, String key) {
        if (source == null) {
            return null;
        }
        JsonElement element = source.get(key);
        if (element == null || element.isJsonNull() || !element.isJsonPrimitive()) {
            return null;
        }
        String value = element.getAsString();
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private static String textOrEmpty(JsonObject source, String key) {
        String value = text(source, key);
        return value == null ? "" : value;
    }

    private static int integer(JsonObject source, String key, int fallback) {
        JsonElement element = source.get(key);
        if (element == null || !element.isJsonPrimitive()) {
            return fallback;
        }
        try {
            return element.getAsInt();
        } catch (NumberFormatException | UnsupportedOperationException ignored) {
            return fallback;
        }
    }

    private static JsonArray array(JsonObject source, String key) {
        if (source == null) {
            return null;
        }
        JsonElement element = source.get(key);
        return element != null && element.isJsonArray() ? element.getAsJsonArray() : null;
    }

    private static final class Stored {
        final JsonObject json;
        final int fallbackDay;

        Stored(JsonObject json, int fallbackDay) {
            this.json = json;
            this.fallbackDay = fallbackDay;
        }
    }
}
