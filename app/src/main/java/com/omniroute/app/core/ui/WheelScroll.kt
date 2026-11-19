package com.omniroute.app.core.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/**
 * 让鼠标滚轮（模拟器滚轮、外接鼠标/触控板）在页面滚动容器上稳定生效。
 *
 * Compose 自带的滚轮处理在部分页面（输入框聚焦、下拉刷新、点击区密集的表单）会被跳过，
 * 表现是「手指能滑，滚轮不动」。这里在事件分发的最后阶段兜底：
 * 子节点没有消费纵向滚动时，按滚轮增量滚动页面；原生已处理的页面不会重复滚动。
 */
fun Modifier.wheelScroll(state: ScrollState): Modifier =
    wheelScroll(state) { state.dispatchRawDelta(it) }

fun Modifier.wheelScroll(state: LazyListState): Modifier =
    wheelScroll(state) { state.dispatchRawDelta(it) }

private fun Modifier.wheelScroll(state: Any, dispatch: (Float) -> Float): Modifier =
    pointerInput(state) {
        // 与系统 ViewConfiguration 的滚轮行距保持一致：一格滚轮约 64dp。
        val stepPx = 64.dp.toPx()
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Final)
                if (event.type != PointerEventType.Scroll) continue
                // 原生滚动或子节点已经处理过，避免滚动两次。
                if (event.changes.any { it.isConsumed }) continue
                var dy = 0f
                var dx = 0f
                event.changes.forEach {
                    dy += it.scrollDelta.y
                    dx += it.scrollDelta.x
                }
                // 横向为主时交给原生（例如横向卡片列表），这里只兜底纵向。
                if (dy == 0f || abs(dy) <= abs(dx)) continue
                dispatch(-dy * stepPx)
            }
        }
    }
