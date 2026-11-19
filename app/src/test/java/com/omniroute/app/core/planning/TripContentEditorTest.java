package com.omniroute.app.core.planning;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** content 与编辑页节点/备注之间的读写契约。 */
public class TripContentEditorTest {

    private static final String CONTENT = "{"
            + "\"city\":\"上海\","
            + "\"summary\":\"城市漫步\","
            + "\"routes\":["
            + "{\"name\":\"外滩\",\"day\":1,\"time\":\"09:00\","
            + "\"duration_minutes\":90,\"source_label\":\"官方\",\"lng\":121.49,\"lat\":31.24},"
            + "{\"name\":\"豫园\",\"day\":1,\"time\":\"13:00\",\"desc\":\"备注：人很多\"}"
            + "]}";

    @Test
    public void readNodesKeepsTopLevelOrder() {
        List<RouteNode> nodes = TripContentEditor.readNodes(CONTENT);

        assertEquals(2, nodes.size());
        assertEquals("外滩", nodes.get(0).getName());
        assertEquals(1, nodes.get(0).getDay());
        assertTrue(nodes.get(0).hasCoordinate());
        assertEquals(121.49, nodes.get(0).getLng(), 0.0001);
        assertEquals("豫园", nodes.get(1).getName());
    }

    @Test
    public void readNodesFallsBackToDayNumber() {
        String content = "{\"days\":["
                + "{\"day\":1,\"routes\":[{\"name\":\"甲\"}]},"
                + "{\"day\":2,\"routes\":[{\"name\":\"乙\"}]}"
                + "]}";

        List<RouteNode> nodes = TripContentEditor.readNodes(content);

        assertEquals(2, nodes.size());
        assertEquals(1, nodes.get(0).getDay());
        assertEquals(2, nodes.get(1).getDay());
    }

    @Test
    public void readNotesUnderstandsNoteFieldAndDescPrefix() {
        String content = "{\"routes\":["
                + "{\"name\":\"甲\",\"time\":\"09:00\",\"note\":\"需预约\"},"
                + "{\"name\":\"乙\",\"time\":\"11:00\",\"desc\":\"看展\\n备注：排队久\"}"
                + "]}";

        Map<String, String> notes = TripContentEditor.readNotes(content);

        assertEquals("需预约", notes.get("甲|09:00"));
        assertEquals("排队久", notes.get("乙|11:00"));
    }

    @Test
    public void applyWritesDayAndNoteWithoutDroppingUnknownFields() {
        List<RouteNode> nodes = new ArrayList<>(TripContentEditor.readNodes(CONTENT));
        nodes.set(0, nodes.get(0).withDay(2));

        Map<String, String> notes = new HashMap<>(TripContentEditor.readNotes(CONTENT));
        notes.put("外滩|09:00", "提前预约");

        String updated = TripContentEditor.apply(CONTENT, "杭州", nodes, notes, 2);
        JsonObject root = JsonParser.parseString(updated).getAsJsonObject();
        JsonObject bund = findRoute(root, "外滩");

        assertNotNull(bund);
        assertEquals(2, bund.get("day").getAsInt());
        assertEquals(90, bund.get("duration_minutes").getAsInt());
        assertEquals("官方", bund.get("source_label").getAsString());
        assertEquals("提前预约", bund.get("note").getAsString());
        assertTrue(bund.get("desc").getAsString().contains("备注：提前预约"));

        JsonObject yu = findRoute(root, "豫园");
        assertNotNull(yu);
        assertEquals("人很多", yu.get("note").getAsString());

        assertEquals("杭州", root.get("city").getAsString());
        assertEquals("城市漫步", root.get("summary").getAsString());
        assertEquals(2, root.getAsJsonArray("days").size());
    }

    @Test
    public void applyDropsNodesInPendingPool() {
        List<RouteNode> nodes = new ArrayList<>(TripContentEditor.readNodes(CONTENT));
        nodes.set(0, nodes.get(0).withDay(0));

        String updated = TripContentEditor.apply(CONTENT, "上海", nodes, null, 1);
        JsonArray routes = JsonParser.parseString(updated)
                .getAsJsonObject()
                .getAsJsonArray("routes");

        assertEquals(1, routes.size());
        assertEquals("豫园", routes.get(0).getAsJsonObject().get("name").getAsString());
    }

    private static JsonObject findRoute(JsonObject root, String name) {
        for (JsonElement element : root.getAsJsonArray("routes")) {
            JsonObject route = element.getAsJsonObject();
            if (name.equals(route.get("name").getAsString())) {
                return route;
            }
        }
        return null;
    }
}
