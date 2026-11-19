package com.omniroute.app.feature.plan

import com.omniroute.app.core.ui.theme.LocalAppPalette

import androidx.compose.material3.minimumInteractiveComponentSize

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omniroute.app.R
import com.omniroute.app.core.planning.PlanRequest
import com.omniroute.app.core.room.RoomMember
import com.omniroute.app.feature.auth.BrandOrange
import com.omniroute.app.feature.auth.ErrorBg
import com.omniroute.app.feature.auth.ErrorFg
import com.omniroute.app.feature.auth.InfoFg
import com.omniroute.app.feature.auth.Ink
import com.omniroute.app.feature.auth.Muted

/** 推演中的进度视图：阶段列表 + 推理片段 + 实时成员。 */
@Composable
internal fun RunningContent(
    destination: String,
    request: PlanRequest?,
    retrievalHint: String?,
    stages: List<StageEntry>,
    streamText: String,
    members: List<RoomMember>,
    connected: Boolean,
) {
    var detailsExpanded by rememberSaveable { mutableStateOf(false) }
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = PlanCardBg,
        border = BorderStroke(1.dp, PlanSubtleBorder),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(
                            R.string.plan_running_header,
                            request?.destination ?: destination,
                        ),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = request?.buildIntent()
                            ?: stringResource(R.string.plan_running_intent_fallback),
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        color = Muted,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = BrandOrange,
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = PlanFormDivider, thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))

            retrievalHint?.let {
                Text(
                    text = it,
                    fontSize = 12.sp,
                    lineHeight = 15.sp,
                    color = InfoFg,
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            if (stages.isEmpty()) {
                Text(
                    text = stringResource(R.string.plan_running_waiting),
                    fontSize = 12.5.sp,
                    color = Muted,
                )
            } else {
                stages.forEachIndexed { index, entry ->
                    StageRow(
                        index = index,
                        entry = entry,
                        isCurrent = index == stages.lastIndex,
                    )
                    if (index != stages.lastIndex) {
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }

            if (streamText.isNotBlank()) {
                TextButton(onClick = { detailsExpanded = !detailsExpanded }) {
                    Text(stringResource(
                        if (detailsExpanded) R.string.plan_hide_progress_detail else R.string.plan_show_progress_detail,
                    ))
                }
                if (detailsExpanded) {
                    Text(
                        text = streamText,
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        color = Muted,
                        maxLines = 5,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(if (connected) PlanOnlineGreen else Muted.copy(alpha = 0.5f)),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(
                        if (connected) R.string.plan_ws_connected else R.string.plan_ws_connecting,
                    ),
                    fontSize = 12.sp,
                    color = if (connected) InfoFg else Muted,
                )
                Spacer(modifier = Modifier.weight(1f))
                if (members.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        members.take(5).forEach { member ->
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(PlanOrangeLightBg),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = member.initial,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandOrange,
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.plan_members_sync, members.size),
                            fontSize = 12.sp,
                            color = Muted,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StageRow(index: Int, entry: StageEntry, isCurrent: Boolean) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(if (isCurrent) PlanOrangeLightBg else Color(0xFFEAF5EF)),
            contentAlignment = Alignment.Center,
        ) {
            if (isCurrent) {
                Text(
                    text = (index + 1).toString(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandOrange,
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = InfoFg,
                    modifier = Modifier.size(12.dp),
                )
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.label,
                fontSize = 12.5.sp,
                fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Medium,
                color = Ink,
            )
            if (entry.detail.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = entry.detail,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = Muted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** 失败态：异常文案 + 重试 + 返回修改。 */
@Composable
internal fun FailureContent(message: String, onRetry: () -> Unit, onEdit: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = PlanCardBg,
        border = BorderStroke(1.dp, PlanSubtleBorder),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(ErrorBg),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = null,
                        tint = ErrorFg,
                        modifier = Modifier.size(16.dp),
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.plan_failed_title),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Ink,
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = message, fontSize = 12.5.sp, lineHeight = 18.sp, color = Muted)
            Spacer(modifier = Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SecondaryButton(text = stringResource(R.string.plan_back_to_edit), onClick = onEdit)
                Box(modifier = Modifier.weight(1f)) {
                    ModernFullButton(
                        text = stringResource(R.string.plan_retry),
                        loading = false,
                        loadingText = stringResource(R.string.plan_retrying),
                        onClick = onRetry,
                    )
                }
            }
        }
    }
}

/** 结果视图：概要 + 路线图 + 每日时间轴 + 反馈。 */
