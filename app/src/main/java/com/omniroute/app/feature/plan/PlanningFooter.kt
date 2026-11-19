package com.omniroute.app.feature.plan

import androidx.compose.material3.minimumInteractiveComponentSize

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omniroute.app.R
import com.omniroute.app.core.ui.theme.LocalAppPalette
import com.omniroute.app.feature.auth.ErrorBg
import com.omniroute.app.feature.auth.ErrorFg
import com.omniroute.app.feature.auth.Ink
import com.omniroute.app.feature.auth.InlineBanner
import com.omniroute.app.feature.auth.Muted

@Composable
internal fun PlanningFooter(
    phase: PlanPhase,
    preparing: Boolean,
    saving: Boolean,
    saveError: String?,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onRetry: () -> Unit,
    onEdit: () -> Unit,
    onSave: () -> Unit,
) {
    Surface(
        color = PlanCardBg,
        border = BorderStroke(1.dp, PlanSubtleBorder.copy(alpha = 0.7f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            when (phase) {
                PlanPhase.FORM -> ModernFullButton(
                    text = stringResource(R.string.plan_start),
                    loading = preparing,
                    loadingText = stringResource(R.string.plan_creating_room),
                    onClick = onStart,
                )

                PlanPhase.RUNNING -> SecondaryButton(
                    text = stringResource(R.string.plan_stop),
                    onClick = onStop,
                    modifier = Modifier.fillMaxWidth(),
                )

                PlanPhase.FAILED -> Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SecondaryButton(text = stringResource(R.string.plan_back_to_edit), onClick = onEdit)
                    Spacer(modifier = Modifier.width(10.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        ModernFullButton(
                            text = stringResource(R.string.plan_retry),
                            loading = false,
                            loadingText = stringResource(R.string.plan_retrying),
                            onClick = onRetry,
                        )
                    }
                }

                PlanPhase.DONE -> {
                    saveError?.let {
                        InlineBanner(
                            text = stringResource(R.string.plan_save_error_suffix, it),
                            bg = ErrorBg,
                            fg = ErrorFg,
                            icon = Icons.Filled.Warning,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SecondaryButton(
                            text = stringResource(R.string.plan_replan),
                            onClick = onEdit,
                            enabled = !saving,
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Box(modifier = Modifier.weight(1f)) {
                            ModernFullButton(
                                text = stringResource(
                                    if (saveError == null) R.string.plan_save_trip else R.string.plan_retry_save,
                                ),
                                loading = saving,
                                loadingText = stringResource(R.string.plan_saving),
                                onClick = onSave,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(PlanTagBg)
            .minimumInteractiveComponentSize().clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (enabled) Ink else Muted,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }
}

/** 现代满宽行动按钮 */
@Composable
internal fun ModernFullButton(
    text: String,
    loading: Boolean,
    loadingText: String,
    onClick: () -> Unit,
) {
    val palette = LocalAppPalette.current
    Button(
        onClick = onClick,
        enabled = !loading,
        colors = ButtonDefaults.buttonColors(
            containerColor = palette.brandDark,
            contentColor = palette.onBrand,
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = LocalContentColor.current,
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = if (loading) loadingText else text,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
