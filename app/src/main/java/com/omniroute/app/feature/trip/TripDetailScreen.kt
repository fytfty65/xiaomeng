package com.omniroute.app.feature.trip

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.omniroute.app.AppContainer
import com.omniroute.app.R
import com.omniroute.app.core.planning.TripItinerary
import com.omniroute.app.core.network.dto.TripPlanDto
import com.omniroute.app.core.ui.ContentMaxWidth
import com.omniroute.app.core.ui.components.ItineraryDayCard
import com.omniroute.app.core.ui.wheelScroll
import com.omniroute.app.feature.auth.ErrorBg
import com.omniroute.app.feature.auth.ErrorFg
import com.omniroute.app.feature.auth.InlineBanner
import com.omniroute.app.feature.plan.RouteEditorScreen

/**
 * 行程详情页：多端同步数据展示与按天动线预览
 */
@Composable
fun TripDetailScreen(
    container: AppContainer,
    tripId: String,
    onReplanNode: (String, String) -> Unit,
    onOpenJourney: () -> Unit,
    onOpenBudget: () -> Unit,
    onDeleted: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    // 页面状态统一由 ViewModel 持有：旋转、进程重建都不会丢弹窗与编辑草稿。
    val viewModel: TripDetailViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                TripDetailViewModel(
                    container,
                    createSavedStateHandle(),
                    context.applicationContext,
                    tripId,
                )
            }
        },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                TripDetailEvent.Deleted -> onDeleted()
                is TripDetailEvent.Message ->
                    Toast.makeText(context, event.text, Toast.LENGTH_LONG).show()
            }
        }
    }

    val trip = state.trip

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PageBg),
        contentAlignment = Alignment.TopCenter,
    ) {
        // 平板/扩展屏上限制正文宽度，行程卡片不再被拉成长条。
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = ContentMaxWidth)
                .windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            // 顶部全局导航栏（内置统一星轨矢量 Logo）
            TripTopBar(
                cityName = trip?.destCity,
                actionsEnabled = trip != null && !state.actionBusy,
                onBack = onBack,
                onShare = {
                    val shareText = trip?.let { buildTripShareText(context, it) }.orEmpty()
                    if (shareText.isBlank()) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.trip_share_empty),
                            Toast.LENGTH_SHORT,
                        ).show()
                    } else {
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, trip?.title.orEmpty())
                            putExtra(Intent.EXTRA_TEXT, shareText)
                        }
                        context.startActivity(
                            Intent.createChooser(
                                send,
                                context.getString(R.string.trip_share_chooser),
                            ),
                        )
                    }
                },
                onEdit = { viewModel.openRename() },
                onDelete = { viewModel.openDelete() },
            )

            val itinerary = remember(trip?.content) {
                trip?.let { TripItinerary.parse(it.content) }
            }
            val listState = rememberLazyListState()
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .wheelScroll(listState),
                contentPadding = PaddingValues(horizontal = 20.dp),
            ) {
                if (trip == null) {
                    item(key = "detail_state") {
                        Column {
                            Spacer(modifier = Modifier.height(10.dp))
                            state.actionError?.let {
                                InlineBanner(text = it, bg = ErrorBg, fg = ErrorFg, icon = Icons.Filled.Warning)
                                Spacer(modifier = Modifier.height(10.dp))
                            }
                            when {
                                state.loading -> ShimmerLoadingView()
                                state.errorText != null -> RefinedErrorBlock(
                                    message = state.errorText.orEmpty(),
                                    onRetry = { viewModel.load() },
                                )
                            }
                        }
                    }
                } else {
                    item(key = "detail_header") {
                        Column {
                            Spacer(modifier = Modifier.height(10.dp))
                            state.actionError?.let {
                                InlineBanner(text = it, bg = ErrorBg, fg = ErrorFg, icon = Icons.Filled.Warning)
                                Spacer(modifier = Modifier.height(10.dp))
                            }
                            TripDetailHeader(
                                trip = trip,
                                summary = itinerary?.summary,
                                hasContent = itinerary?.hasContent() == true,
                                daysCount = itinerary?.days?.size ?: 0,
                                onEditRoute = { viewModel.openEditor() },
                                onOpenJourney = onOpenJourney,
                                onOpenBudget = onOpenBudget,
                            )
                        }
                    }
                    // 最多 30 天也只在可视范围内组合卡片；dayNumber 作为稳定 key 避免滚动时重建。
                    if (itinerary != null && itinerary.hasContent()) {
                        items(
                            items = itinerary.days,
                            key = { day -> "detail_day_${day.dayNumber}" },
                        ) { day ->
                            Column {
                                ItineraryDayCard(
                                    day = day,
                                    isBooked = { item ->
                                        state.bookingMarks.isBooked(day.dayNumber, item.name.orEmpty())
                                    },
                                    onItemClick = { item -> viewModel.openItem(day.dayNumber, item) },
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }
                    }
                }
                item(key = "detail_bottom") {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    // 已保存行程的路线编辑：复用规划结果页的编辑页，返回/完成都会把改动落库。
    if (state.editorOpen && trip != null) {
        val editorDayCount = maxOf(
            trip.days,
            state.editorNodes.maxOfOrNull { it.day } ?: 1,
            1,
        )
        RouteEditorScreen(
            nodes = state.editorNodes,
            dayCount = editorDayCount,
            notes = state.editorNotes,
            onNoteChange = { key, value -> viewModel.onNoteChange(key, value) },
            loadBaseMap = { center, zoom, width, height ->
                viewModel.loadBaseMap(center, zoom, width, height)
            },
            onClose = { viewModel.closeEditor() },
            onApply = { updated -> viewModel.applyEdits(updated) },
        )
        return
    }

    if (state.renameOpen && trip != null) {
        RenameTripDialog(
            draft = state.renameDraft,
            busy = state.actionBusy,
            errorText = state.actionError,
            onDraftChange = { viewModel.onRenameDraftChange(it) },
            onConfirm = { viewModel.rename() },
            onDismiss = { viewModel.dismissRename() },
        )
    }

    if (state.deleteOpen && trip != null) {
        DeleteTripDialog(
            title = tripTitle(context, trip),
            busy = state.actionBusy,
            errorText = state.actionError,
            onConfirm = { viewModel.delete() },
            onDismiss = { viewModel.dismissDelete() },
        )
    }

    // 行程节点操作闭环：官方预订、导航、地址复制、日历、已预订、重新规划
    val item = state.actionItem
    if (trip != null && item != null) {
        TripItemActionSheet(
            item = item,
            isBooked = state.bookingMarks.isBooked(state.actionDay, item.name.orEmpty()),
            onOpenBooking = {
                val url = item.bookingUrl.orEmpty()
                val ok = openExternal(context, url)
                if (!ok) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.trip_open_booking_error),
                        Toast.LENGTH_LONG,
                    ).show()
                }
            },
            onNavigate = {
                val ok = navigateToItem(context, item)
                if (!ok) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.trip_navigate_error),
                        Toast.LENGTH_LONG,
                    ).show()
                }
            },
            onCopyAddress = {
                val address = item.address.orEmpty()
                if (address.isBlank()) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.trip_no_address),
                        Toast.LENGTH_SHORT,
                    ).show()
                } else {
                    copyText(context, address, context.getString(R.string.trip_label_address))
                }
            },
            onAddCalendar = {
                val ok = addToCalendar(context, item)
                if (!ok) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.trip_calendar_error),
                        Toast.LENGTH_LONG,
                    ).show()
                }
            },
            onToggleBooked = {
                val nowBooked = viewModel.toggleBooked()
                Toast.makeText(
                    context,
                    context.getString(
                        if (nowBooked) {
                            R.string.trip_booked_marked
                        } else {
                            R.string.trip_booked_unmarked
                        },
                    ),
                    Toast.LENGTH_SHORT,
                ).show()
            },
            onReplan = {
                onReplanNode(trip.destCity.orEmpty(), item.name)
            },
            onDismiss = { viewModel.dismissItem() },
        )
    }
}

/** 生成纯文本快照用于系统分享，不依赖后端新增分享接口。 */
private fun buildTripShareText(context: Context, trip: TripPlanDto): String {
    val itinerary = TripItinerary.parse(trip.content)
    if (!itinerary.hasContent()) return ""
    val builder = StringBuilder()
    val title = trip.title?.takeIf { it.isNotBlank() }
        ?: itinerary.city?.takeIf { it.isNotBlank() }
        ?: trip.destCity.orEmpty()
    if (title.isNotBlank()) {
        builder.append(title).append('\n')
    }
    itinerary.days.forEach { day ->
        builder.append('\n')
            .append(context.getString(R.string.trip_share_day, day.dayNumber))
            .append('\n')
        day.items.forEach { item ->
            builder.append("· ")
            val time = item.time
            if (!time.isNullOrBlank()) {
                builder.append(time).append(' ')
            }
            builder.append(item.name.orEmpty())
            if (item.durationMinutes > 0) {
                builder.append(context.getString(R.string.trip_share_duration, item.durationMinutes))
            }
            builder.append('\n')
        }
    }
    builder.append('\n').append(context.getString(R.string.trip_share_footer))
    return builder.toString()
}
