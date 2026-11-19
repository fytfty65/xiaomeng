package com.omniroute.app.feature.plan

import com.omniroute.app.core.ui.theme.LocalAppPalette

import androidx.compose.material3.minimumInteractiveComponentSize

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omniroute.app.R
import com.omniroute.app.feature.auth.BrandOrange
import com.omniroute.app.feature.auth.ErrorFg
import com.omniroute.app.feature.auth.InfoFg
import com.omniroute.app.feature.auth.Ink
import com.omniroute.app.feature.auth.Muted

@Composable
internal fun FeedbackCard(
    done: Int?,
    reason: String,
    onReasonChange: (String) -> Unit,
    busy: Boolean,
    errorText: String?,
    onFeedback: (Int) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = PlanCardBg,
        border = BorderStroke(1.dp, PlanSubtleBorder),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.plan_feedback_title),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Ink,
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = stringResource(R.string.plan_feedback_subtitle),
                fontSize = 12.sp,
                color = Muted,
            )

            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = LocalAppPalette.current.surface,
                modifier = Modifier.fillMaxWidth(),
            ) {
                BasicTextField(
                    value = reason,
                    onValueChange = onReasonChange,
                    textStyle = TextStyle(fontSize = 12.5.sp, color = Ink),
                    cursorBrush = SolidColor(BrandOrange),
                    decorationBox = { inner ->
                        Box(modifier = Modifier.padding(10.dp)) {
                            if (reason.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.plan_feedback_reason_hint),
                                    fontSize = 12.5.sp,
                                    color = Muted.copy(alpha = 0.6f),
                                )
                            }
                            inner()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FeedbackButton(
                    text = stringResource(R.string.plan_feedback_positive),
                    selected = done == 1,
                    enabled = !busy && done == null,
                    modifier = Modifier.weight(1f),
                    onClick = { onFeedback(1) },
                )
                FeedbackButton(
                    text = stringResource(R.string.plan_feedback_negative),
                    selected = done == -1,
                    enabled = !busy && done == null,
                    modifier = Modifier.weight(1f),
                    onClick = { onFeedback(-1) },
                )
            }

            errorText?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = it, fontSize = 12.sp, color = ErrorFg)
            }

            if (done != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(
                        if (done > 0) {
                            R.string.plan_feedback_positive_done
                        } else {
                            R.string.plan_feedback_negative_done
                        },
                    ),
                    fontSize = 12.sp,
                    color = InfoFg,
                )
            }
        }
    }
}

@Composable
internal fun FeedbackButton(
    text: String,
    selected: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = when {
            selected -> PlanOrangeLightBg
            enabled -> PlanTagBg
            else -> PlanTagBg.copy(alpha = 0.5f)
        },
        border = BorderStroke(1.dp, if (selected) BrandOrange.copy(alpha = 0.4f) else PlanSubtleBorder),
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .minimumInteractiveComponentSize().clickable(enabled = enabled, role = Role.Button, onClick = onClick),
    ) {
        Text(
            text = text,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (enabled || selected) Ink else Muted.copy(alpha = 0.5f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 11.dp),
        )
    }
}

