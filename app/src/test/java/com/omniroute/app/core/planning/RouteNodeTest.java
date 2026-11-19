package com.omniroute.app.core.planning;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import com.google.gson.JsonParser;

import org.junit.Test;

/** RouteNode 的值语义：编辑前后比较、列表去重都依赖它。 */
public class RouteNodeTest {

    private static RouteNode parse(String json) {
        return RouteNode.parse(JsonParser.parseString(json));
    }

    @Test
    public void identicalFieldsAreEqual() {
        RouteNode first = parse(
                "{\"name\":\"外滩\",\"day\":1,\"time\":\"09:00\","
                        + "\"lnglat\":[121.49,31.24],\"tags\":[\"景点\"]}");
        RouteNode second = parse(
                "{\"name\":\"外滩\",\"day\":1,\"time\":\"09:00\","
                        + "\"lnglat\":[121.49,31.24],\"tags\":[\"景点\"]}");

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
    }

    @Test
    public void changedTimeIsNotEqual() {
        RouteNode first = parse("{\"name\":\"外滩\",\"day\":1,\"time\":\"09:00\"}");
        RouteNode second = parse("{\"name\":\"外滩\",\"day\":1,\"time\":\"10:00\"}");

        assertNotEquals(first, second);
    }

    @Test
    public void changedDayIsNotEqual() {
        RouteNode first = parse("{\"name\":\"外滩\",\"day\":1}");
        RouteNode second = parse("{\"name\":\"外滩\",\"day\":2}");

        assertNotEquals(first, second);
    }

    @Test
    public void differentTagsAreNotEqual() {
        RouteNode first = parse("{\"name\":\"外滩\",\"tags\":[\"景点\"]}");
        RouteNode second = parse("{\"name\":\"外滩\",\"tags\":[\"购物\"]}");

        assertNotEquals(first, second);
    }

    @Test
    public void coordinateChangeIsTracked() {
        RouteNode first = parse("{\"name\":\"外滩\",\"lnglat\":[121.49,31.24]}");
        RouteNode second = parse("{\"name\":\"外滩\",\"lnglat\":[121.50,31.24]}");

        assertFalse(first.equals(second));
    }

    @Test
    public void withDayKeepsOtherFields() {
        RouteNode original = parse(
                "{\"name\":\"外滩\",\"day\":1,\"time\":\"09:00\",\"desc\":\"看夜景\"}");
        RouteNode moved = original.withDay(3);

        assertEquals(3, moved.getDay());
        assertEquals(original.getName(), moved.getName());
        assertEquals(original.getTime(), moved.getTime());
        assertEquals(original.getDescription(), moved.getDescription());
        assertTrue(original.equals(original.withDay(1)));
    }
}
