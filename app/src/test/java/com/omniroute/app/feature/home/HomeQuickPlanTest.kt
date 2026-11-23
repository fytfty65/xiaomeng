package com.omniroute.app.feature.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HomeQuickPlanTest {
    @Test
    fun separatesDestinationFromTypedDuration() {
        assertEquals(HomeQuickPlan("乌鲁木齐", 5), parseQuickPlan(" 乌鲁木齐 5日 "))
        assertEquals(HomeQuickPlan("阿勒泰", 2), parseQuickPlan("阿勒泰２天游"))
        assertEquals(HomeQuickPlan("杭州", 7), parseQuickPlan("杭州 7"))
        assertEquals(HomeQuickPlan("禾木", 3), parseQuickPlan("禾木"))
    }

    @Test
    fun rejectsMissingDestinationAndInvalidDuration() {
        listOf("", "3日", "Osaka", "禾木 0天", "禾木 31天", "禾木 -3天", "禾木 1.5日")
            .forEach { assertNull(it, parseQuickPlan(it)) }
    }
}
