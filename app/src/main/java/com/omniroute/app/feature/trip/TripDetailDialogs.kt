package com.omniroute.app.feature.trip

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omniroute.app.R
import com.omniroute.app.core.planning.TripItinerary
import com.omniroute.app.feature.auth.BrandOrange
import com.omniroute.app.feature.auth.ErrorBg
import com.omniroute.app.feature.auth.ErrorFg
import com.omniroute.app.feature.auth.Ink
import com.omniroute.app.feature.auth.InlineBanner
import com.omniroute.app.feature.auth.Muted

/** 重命名行程弹窗。 */
@Composable
internal fun RenameTripDialog(
    draft: String,
    busy: Boolean,
    errorText: String?,
    onDraftChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.trip_rename_title), fontSize = 16.sp, color = Ink) },
        text = {
            Column {
                OutlinedTextField(
                    value = draft,
                    onValueChange = onDraftChange,
                    label = { Text(text = stringResource(R.string.trip_rename_label), fontSize = 12.sp) },
                    singleLine = true,
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (errorText != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    InlineBanner(
                        text = errorText,
                        bg = ErrorBg,
                        fg = ErrorFg,
                        icon = Icons.Filled.Warning,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !busy && draft.trim().isNotEmpty(),
                onClick = onConfirm,
            ) {
                Text(
                    text = if (busy) {
                        stringResource(R.string.trip_saving)
                    } else {
                        stringResource(R.string.common_save)
                    },
                    color = BrandOrange,
                )
            }
        },
        dismissButton = {
            TextButton(enabled = !busy, onClick = onDismiss) {
                Text(text = stringResource(R.string.common_cancel), color = Muted)
            }
        },
    )
}

/** 删除行程确认弹窗。 */
@Composable
internal fun DeleteTripDialog(
    title: String,
    busy: Boolean,
    errorText: String?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.trip_delete_title), fontSize = 16.sp, color = Ink) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.trip_delete_message, title),
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    color = Muted,
                )
                if (errorText != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    InlineBanner(
                        text = errorText,
                        bg = ErrorBg,
                        fg = ErrorFg,
                        icon = Icons.Filled.Warning,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !busy,
                onClick = onConfirm,
            ) {
                Text(
                    text = if (busy) {
                        stringResource(R.string.trip_deleting)
                    } else {
                        stringResource(R.string.common_delete)
                    },
                    color = ErrorFg,
                )
            }
        },
        dismissButton = {
            TextButton(enabled = !busy, onClick = onDismiss) {
                Text(text = stringResource(R.string.common_cancel), color = Muted)
            }
        },
    )
}

/** 单个行程节点的可执行操作面板。 */
@Composable
internal fun TripItemActionSheet(
    item: TripItinerary.Item,
    isBooked: Boolean,
    onOpenBooking: () -> Unit,
    onNavigate: () -> Unit,
    onCopyAddress: () -> Unit,
    onAddCalendar: () -> Unit,
    onToggleBooked: () -> Unit,
    onReplan: () -> Unit,
    onDismiss: () -> Unit,
) {
    val hasBooking = !item.bookingUrl.isNullOrBlank()
    val hasAddress = !item.address.isNullOrBlank()
    val hasCoordinate = item.hasCoordinate()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(text = item.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Ink)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = buildList {
                        if (item.type.isNotBlank()) add(item.type)
                        if (!item.time.isNullOrBlank()) add(item.time!!)
                        if (item.isEstimated) add(stringResource(R.string.trip_tag_estimated))
                        if (!item.observedAt.isNullOrBlank()) {
                            add(stringResource(R.string.trip_tag_observed, item.observedAt!!))
                        }
                    }.joinToString(" · "),
                    fontSize = 12.sp,
                    color = Muted,
                )
            }
        },
        text = {
            Column {
                ActionSheetRow(
                    text = stringResource(
                        if (hasBooking) {
                            R.string.trip_action_booking
                        } else {
                            R.string.trip_action_booking_empty
                        },
                    ),
                    enabled = hasBooking,
                    onClick = onOpenBooking,
                )
                ActionSheetRow(
                    text = stringResource(
                        if (hasCoordinate) {
                            R.string.trip_action_navigate
                        } else {
                            R.string.trip_action_navigate_empty
                        },
                    ),
                    enabled = hasCoordinate,
                    onClick = onNavigate,
                )
                ActionSheetRow(
                    text = if (hasAddress) {
                        stringResource(R.string.trip_action_copy, item.address.orEmpty())
                    } else {
                        stringResource(R.string.trip_action_copy_empty)
                    },
                    enabled = hasAddress,
                    onClick = onCopyAddress,
                )
                ActionSheetRow(
                    text = stringResource(R.string.trip_action_calendar),
                    enabled = true,
                    onClick = onAddCalendar,
                )
                ActionSheetRow(
                    text = stringResource(
                        if (isBooked) {
                            R.string.trip_action_unbook
                        } else {
                            R.string.trip_action_book
                        },
                    ),
                    enabled = true,
                    onClick = onToggleBooked,
                )
                ActionSheetRow(
                    text = stringResource(R.string.trip_action_replan),
                    enabled = true,
                    onClick = onReplan,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.trip_action_close), color = Muted)
            }
        },
    )
}

@Composable
private fun ActionSheetRow(text: String, enabled: Boolean, onClick: () -> Unit) {
    TextButton(
        enabled = enabled,
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            color = if (enabled) Ink else Muted.copy(alpha = 0.55f),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
