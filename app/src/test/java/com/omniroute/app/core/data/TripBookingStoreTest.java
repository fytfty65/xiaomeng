package com.omniroute.app.core.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Collections;

/** 预订标记的键格式与旧数据兼容读取。 */
public class TripBookingStoreTest {

    @Test
    public void nodeKeyCombinesDayAndTrimmedName() {
        assertEquals("1|外滩", TripBookingStore.nodeKey(1, " 外滩 "));
    }

    @Test
    public void sameNameOnDifferentDaysIsNotShared() {
        assertNotEquals(
                TripBookingStore.nodeKey(1, "酒店"),
                TripBookingStore.nodeKey(2, "酒店"));
    }

    @Test
    public void marksReadsDayAwareKeys() {
        TripBookingStore.Marks marks = new TripBookingStore.Marks(
                Collections.singleton(TripBookingStore.nodeKey(1, "外滩")),
                Collections.<String>emptySet());

        assertTrue(marks.isBooked(1, "外滩"));
        assertFalse(marks.isBooked(2, "外滩"));
    }

    @Test
    public void marksStillReadsLegacyNameOnlyEntries() {
        TripBookingStore.Marks marks = new TripBookingStore.Marks(
                Collections.<String>emptySet(),
                Collections.singleton("豫园"));

        assertTrue(marks.isBooked(1, " 豫园 "));
    }
}
