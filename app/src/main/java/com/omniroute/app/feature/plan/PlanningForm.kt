package com.omniroute.app.feature.plan

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.List
import com.omniroute.app.core.ui.theme.JourneySeason
import androidx.compose.ui.text.font.FontFamily
import com.omniroute.app.core.ui.theme.OmniSeasonLogo
import com.omniroute.app.core.ui.theme.LocalJourneySeason
import com.omniroute.app.core.ui.theme.EditionSurface
import com.omniroute.app.core.ui.theme.edition
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.ui.graphics.Brush
import android.content.Context
import androidx.compose.material3.minimumInteractiveComponentSize

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omniroute.app.R
import com.omniroute.app.core.room.RoomSession
import com.omniroute.app.core.ui.components.OmniLogoMark
import com.omniroute.app.feature.auth.BrandOrange
import com.omniroute.app.feature.auth.InfoBg
import com.omniroute.app.feature.auth.InfoFg
import com.omniroute.app.feature.auth.Ink
import com.omniroute.app.feature.auth.InlineBanner
import com.omniroute.app.feature.auth.Muted
import java.time.LocalDate

internal val PreferenceOptions = listOf(
    "美食", "自然风光", "人文历史", "亲子", "摄影", "慢节奏", "购物", "夜生活",
)

/** 把结构化参数翻译成「会按哪些方向检索」的提示，避免承诺具体数据源结果。 */
internal fun retrievalHint(
    context: Context,
    city: String,
    days: Int,
    preferences: Collection<String>,
): String {
    val parts = mutableListOf(
        context.getString(R.string.plan_retrieval_highlights, city),
        context.getString(R.string.plan_retrieval_pace, city, days),
    )
    if (preferences.contains("美食")) {
        parts += context.getString(R.string.plan_retrieval_food, city)
    }
    if (preferences.contains("自然风光")) {
        parts += context.getString(R.string.plan_retrieval_nature, city)
    }
    if (preferences.contains("人文历史")) {
        parts += context.getString(R.string.plan_retrieval_culture, city)
    }
    if (preferences.contains("亲子")) {
        parts += context.getString(R.string.plan_retrieval_family, city)
    }
    parts += context.getString(R.string.plan_retrieval_weather, city)
    return context.getString(
        R.string.plan_retrieval_summary,
        parts.joinToString(context.getString(R.string.plan_retrieval_join)),
    )
}

@Composable
private fun ChatIdeaCard(
    text: String,
    onTextChange: (String) -> Unit,
    hint: String?,
    parsing: Boolean,
    parsed: ParsedIntent?,
    onApply: () -> Unit,
    onConfirm: () -> Unit,
) {
    val season = LocalJourneySeason.current
    val style = season.edition
    val seasonExamples = when (season) {
        JourneySeason.Spring -> listOf("婺源看油菜花，3天", "杭州沿西湖散步，2天", "林芝赏桃花，4天")
        JourneySeason.Summer -> listOf("青岛看海，3天", "威海沿海骑行，2天", "舟山海鲜之旅，3天")
        JourneySeason.Autumn -> listOf("阿尔山看秋林，3天", "喀纳斯徒步，4天", "腾冲看银杏，3天")
        JourneySeason.Winter -> listOf("阿勒泰滑雪，4天慢节奏", "禾木雪景摄影，3天", "长白山看雾凇，3天")
    }
    val shape = RoundedCornerShape(18.dp)
    Surface(
        shape = shape,
        color = EditionSurface,
        modifier = Modifier.fillMaxWidth().border(1.2.dp, Brush.horizontalGradient(listOf(style.accent, style.secondary, style.light)), shape),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OmniSeasonLogo(diameter = 28.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.plan_form_chat_title),
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Ink,
                )
            }

            Spacer(modifier = Modifier.height(9.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(PlanTagBg)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                if (text.isEmpty()) {
                    Text(
                        text = stringResource(R.string.plan_form_chat_placeholder),
                        fontSize = 12.sp,
                        color = Muted,
                    )
                }
                BasicTextField(
                    value = text,
                    onValueChange = { if (it.length <= 200) onTextChange(it) },
                    textStyle = TextStyle(fontSize = 13.sp, color = Ink),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 42.dp),
                )
            }

            Spacer(Modifier.height(5.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                seasonExamples.forEach { example ->
                    androidx.compose.material3.SuggestionChip(
                        onClick = { onTextChange(example) },
                        label = { Text(example, fontSize = 10.5.sp) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(9.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 只有拿到结构化结果后才显示「确认填入」，避免误识别直接覆盖表单。
                if (parsed != null) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = PlanOrangeLightBg,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .minimumInteractiveComponentSize()
                            .clickable(role = Role.Button, onClick = onConfirm),
                    ) {
                        Text(
                            text = stringResource(R.string.plan_form_chat_confirm),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = BrandOrange,
                            modifier = Modifier.padding(horizontal = 13.dp, vertical = 7.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = BrandOrange,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .minimumInteractiveComponentSize()
                        .clickable(enabled = !parsing, role = Role.Button, onClick = onApply),
                ) {
                    Text(
                        text = stringResource(
                            if (parsing) {
                                R.string.plan_form_chat_parsing
                            } else {
                                R.string.plan_form_chat_apply
                            },
                        ),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 13.dp, vertical = 7.dp),
                    )
                }
            }

            hint?.let {
                Spacer(modifier = Modifier.height(9.dp))
                InlineBanner(text = it, bg = InfoBg, fg = InfoFg, icon = Icons.Filled.Info)
            }
        }
    }
}

@Composable
internal fun FormContent(
    chatText: String,
    onChatTextChange: (String) -> Unit,
    chatHint: String?,
    chatParsing: Boolean,
    chatParsed: ParsedIntent?,
    onApplyChat: () -> Unit,
    onConfirmChat: () -> Unit,
    destination: String,
    onDestinationChange: (String) -> Unit,
    origin: String,
    onOriginChange: (String) -> Unit,
    startDate: String,
    onStartDateSelected: (Long) -> Unit,
    onStartDateClear: () -> Unit,
    days: Int,
    onDaysChange: (Int) -> Unit,
    travelers: Int,
    onTravelersChange: (Int) -> Unit,
    budget: String,
    onBudgetChange: (String) -> Unit,
    preferences: Set<String>,
    onTogglePreference: (String) -> Unit,
    room: RoomSession?,
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        OmniSeasonLogo(diameter = 42.dp)
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = stringResource(R.string.plan_form_engine_title),
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Ink,
            )
            Text(
                text = stringResource(R.string.plan_form_engine_subtitle),
                fontSize = 12.sp,
                color = Muted,
            )
        }
    }

    Spacer(modifier = Modifier.height(18.dp))

    ChatIdeaCard(
        text = chatText,
        onTextChange = onChatTextChange,
        hint = chatHint,
        parsing = chatParsing,
        parsed = chatParsed,
        onApply = onApplyChat,
        onConfirm = onConfirmChat,
    )

    Spacer(modifier = Modifier.height(22.dp))

    FormSectionTitle(text = stringResource(R.string.plan_form_section_route))
    Spacer(modifier = Modifier.height(8.dp))

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = EditionSurface,
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 5.dp,
                shape = RoundedCornerShape(18.dp),
                ambientColor = Color.Black.copy(alpha = 0.02f),
                spotColor = BrandOrange.copy(alpha = 0.05f),
            ),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            IntegratedCityInputRow(
                value = destination,
                onValueChange = onDestinationChange,
                label = stringResource(R.string.plan_form_destination_label),
                placeholder = stringResource(R.string.plan_form_destination_hint),
                isEssential = true,
                imeAction = ImeAction.Next,
            )
            HorizontalDivider(color = PlanFormDivider, thickness = 1.dp)
            IntegratedCityInputRow(
                value = origin,
                onValueChange = onOriginChange,
                label = stringResource(R.string.plan_form_origin_label),
                placeholder = stringResource(R.string.plan_form_origin_hint),
                isEssential = false,
                imeAction = ImeAction.Done,
            )
            if (destination.isNotBlank() || origin.isNotBlank()) {
                TextButton(
                    onClick = {
                        onDestinationChange(origin)
                        onOriginChange(destination)
                    },
                    modifier = Modifier.align(Alignment.End).padding(end = 8.dp),
                ) { Text("互换出发地和目的地", fontSize = 12.sp) }
            }
        }
    }

    Spacer(modifier = Modifier.height(20.dp))

    FormSectionTitle(text = stringResource(R.string.plan_form_section_time))
    Spacer(modifier = Modifier.height(8.dp))

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = EditionSurface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
            DateInputRow(
                value = startDate,
                onDateSelected = onStartDateSelected,
                onClear = onStartDateClear,
            )
            HorizontalDivider(color = PlanFormDivider, thickness = 1.dp)
            val dayTitle = stringResource(R.string.plan_form_days_title)
            val dayUnit = stringResource(R.string.plan_form_days_unit)
            val travelerTitle = stringResource(R.string.plan_form_travelers_title)
            val travelerUnit = stringResource(R.string.plan_form_travelers_unit)
            BoxWithConstraints(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
                val compact = maxWidth < 294.dp || LocalDensity.current.fontScale > 1.2f
                if (compact) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        TripStepper(dayTitle, days, dayUnit, 30, onDaysChange, Modifier.fillMaxWidth())
                        TripStepper(travelerTitle, travelers, travelerUnit, 20, onTravelersChange, Modifier.fillMaxWidth())
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TripStepper(dayTitle, days, dayUnit, 30, onDaysChange, Modifier.weight(1f))
                        TripStepper(travelerTitle, travelers, travelerUnit, 20, onTravelersChange, Modifier.weight(1f))
                    }
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(20.dp))

    FormSectionTitle(text = stringResource(R.string.plan_form_section_budget))
    Spacer(modifier = Modifier.height(8.dp))

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = EditionSurface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
            BudgetInputRow(value = budget, onValueChange = onBudgetChange)
            HorizontalDivider(color = PlanFormDivider, thickness = 1.dp)
            Column(modifier = Modifier.padding(vertical = 12.dp)) {
                Text(
                    text = stringResource(R.string.plan_form_preference_title),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Ink,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.plan_form_preference_hint),
                    fontSize = 12.sp,
                    color = Muted,
                )
                Spacer(modifier = Modifier.height(10.dp))
                val rows = PreferenceOptions.chunked(4)
                rows.forEachIndexed { rowIndex, row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        row.forEach { label ->
                            PreferenceChip(
                                label = label,
                                selected = preferences.contains(label),
                                onClick = { onTogglePreference(label) },
                            )
                        }
                    }
                    if (rowIndex != rows.lastIndex) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }

    room?.let {
        Spacer(modifier = Modifier.height(14.dp))
        InlineBanner(
            text = stringResource(R.string.plan_form_room_hint, it.roomName, it.inviteCode),
            bg = InfoBg,
            fg = InfoFg,
            icon = Icons.Filled.Info,
        )
    }
}

@Composable
private fun FormSectionTitle(text: String) {
    Text(text = text, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Ink)
}

/** 现代一体式输入行组件 */
@Composable
private fun IntegratedCityInputRow(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    isEssential: Boolean,
    imeAction: ImeAction,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (isEssential) PlanOrangeLightBg else PlanTagBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.LocationOn,
                contentDescription = null,
                tint = if (isEssential) BrandOrange else Muted,
                modifier = Modifier.size(17.dp),
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = label,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isFocused) BrandOrange else Muted,
                )
                if (isEssential) {
                    Text(text = " *", fontSize = 12.sp, color = BrandOrange)
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Box(modifier = Modifier.fillMaxWidth()) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        fontSize = 13.5.sp,
                        color = Muted.copy(alpha = 0.55f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = TextStyle(
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Ink,
                    ),
                    cursorBrush = SolidColor(BrandOrange),
                    interactionSource = interactionSource,
                    keyboardOptions = KeyboardOptions(imeAction = imeAction),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        if (value.isNotEmpty()) {
            Icon(
                imageVector = Icons.Filled.Clear,
                contentDescription = stringResource(R.string.plan_form_clear),
                tint = Muted.copy(alpha = 0.5f),
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .clickable { onValueChange("") },
            )
        }
    }
}

/** 出发日期：使用 Material 3 DatePicker，禁止选择过去日期。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateInputRow(
    value: String,
    onDateSelected: (Long) -> Unit,
    onClear: () -> Unit,
) {
    var showPicker by remember { mutableStateOf(false) }
    val todayMillis = remember { LocalDate.now().toEpochDay() * MILLIS_PER_DAY }
    val selectedMillis = remember(value) {
        runCatching { LocalDate.parse(value).toEpochDay() * MILLIS_PER_DAY }.getOrNull()
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .clickable(role = Role.Button) { showPicker = true },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(PlanTagBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.DateRange,
                contentDescription = null,
                tint = Muted,
                modifier = Modifier.size(17.dp),
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.plan_form_date_label),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Muted,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value.ifEmpty { stringResource(R.string.plan_form_date_hint) },
                fontSize = 13.5.sp,
                fontWeight = if (value.isEmpty()) FontWeight.Normal else FontWeight.SemiBold,
                color = if (value.isEmpty()) Muted.copy(alpha = 0.55f) else Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (value.isNotEmpty()) {
            Text(
                text = stringResource(R.string.plan_form_clear),
                fontSize = 12.sp,
                color = Muted,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .clickable(role = Role.Button, onClick = onClear)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }

    if (showPicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedMillis ?: todayMillis,
            selectableDates = remember {
                // 过去日期直接禁选；ViewModel 仍会再做一次解析校验，防止恢复的脏数据。
                object : SelectableDates {
                    override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                        return utcTimeMillis >= todayMillis
                    }
                }
            },
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(
                    enabled = pickerState.selectedDateMillis != null,
                    onClick = {
                        pickerState.selectedDateMillis?.let(onDateSelected)
                        showPicker = false
                    },
                ) {
                    Text(text = stringResource(R.string.common_confirm), color = BrandOrange)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text(text = stringResource(R.string.common_cancel), color = Muted)
                }
            },
        ) {
            DatePicker(state = pickerState, showModeToggle = false)
        }
    }
}

private const val MILLIS_PER_DAY = 86_400_000L

/** 预算：数字键盘，空值表示不设上限。 */
@Composable
private fun BudgetInputRow(value: String, onValueChange: (String) -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(PlanTagBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                tint = Muted,
                modifier = Modifier.size(17.dp),
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.plan_form_budget_label),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isFocused) BrandOrange else Muted,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Box(modifier = Modifier.fillMaxWidth()) {
                if (value.isEmpty()) {
                    Text(
                        text = stringResource(R.string.plan_form_budget_hint),
                        fontSize = 13.5.sp,
                        color = Muted.copy(alpha = 0.55f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        singleLine = true,
                        textStyle = TextStyle(
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Ink,
                        ),
                        cursorBrush = SolidColor(BrandOrange),
                        interactionSource = interactionSource,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done,
                        ),
                        modifier = Modifier.weight(1f),
                    )
                    if (value.isNotEmpty()) {
                        Text(text = stringResource(R.string.plan_form_budget_unit), fontSize = 12.sp, color = Muted)
                    }
                }
            }
        }
    }
}

@Composable
private fun PreferenceChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val style = LocalJourneySeason.current.edition
    val icon = when (label) {
        "美食" -> Icons.Filled.Favorite
        "自然风光" -> Icons.Filled.Place
        "人文历史" -> Icons.Filled.List
        "亲子" -> Icons.Filled.Face
        "摄影" -> Icons.Filled.Search
        "购物" -> Icons.Filled.ShoppingCart
        "夜生活" -> Icons.Filled.Star
        else -> Icons.Filled.Info
    }
    val backdrop = if (selected) style.actionGradient else Brush.linearGradient(listOf(PlanTagBg, PlanTagBg))
    val ink = if (selected) Color.White else Ink
    Row(
        modifier = Modifier.clip(CircleShape).background(backdrop)
            .minimumInteractiveComponentSize().toggleable(selected, role = Role.Checkbox) { onClick() }
            .padding(horizontal = 9.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(icon, null, tint = if (selected) Color.White else BrandOrange, modifier = Modifier.size(13.dp))
        Text(label, fontSize = 12.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium, color = ink)
    }
}

@Composable
private fun TripStepper(
    title: String,
    value: Int,
    unit: String,
    max: Int,
    onChange: (Int) -> Unit,
    modifier: Modifier,
) {
    Surface(modifier = modifier, shape = RoundedCornerShape(12.dp), color = PlanTagBg) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(title, fontSize = 12.sp, color = Ink, fontWeight = FontWeight.SemiBold)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterHorizontally),
            ) {
                TactileStepperButton("−", value > 1) { onChange(value - 1) }
                Text("$value $unit", fontSize = 14.sp, color = Ink, fontFamily = FontFamily.Monospace)
                TactileStepperButton("+", value < max) { onChange(value + 1) }
            }
        }
    }
}

@Composable
private fun TactileStepperButton(symbol: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(if (enabled) PlanOrangeLightBg else PlanTagBg)
            .minimumInteractiveComponentSize().clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = symbol,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = if (enabled) BrandOrange else Muted.copy(alpha = 0.4f),
        )
    }
}
