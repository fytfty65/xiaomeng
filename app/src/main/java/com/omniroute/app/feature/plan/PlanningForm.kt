package com.omniroute.app.feature.plan

import com.omniroute.app.core.ui.theme.FolioSerif
import com.omniroute.app.core.ui.theme.FolioDigits
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.omniroute.app.core.ui.theme.editionRim
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.List
import com.omniroute.app.core.ui.theme.JourneySeason
import com.omniroute.app.core.ui.theme.LocalJourneySeason
import com.omniroute.app.core.ui.theme.EditionSurface
import com.omniroute.app.core.ui.theme.edition
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.ui.graphics.Brush
import android.content.Context
import androidx.compose.material3.minimumInteractiveComponentSize

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omniroute.app.R
import com.omniroute.app.core.room.RoomSession
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
    Column(Modifier.fillMaxWidth()) {
        Text("下一站，\n你想奔赴哪场风景？", color = Ink, fontSize = 30.sp, lineHeight = 41.sp, fontFamily = FolioSerif)
        Spacer(Modifier.height(12.dp))
        BasicTextField(value = text, onValueChange = { if (it.length <= 200) onTextChange(it) },
            modifier = Modifier.fillMaxWidth().heightIn(min = 62.dp),
            textStyle = TextStyle(fontSize = 16.sp, lineHeight = 25.sp, color = Ink),
            cursorBrush = SolidColor(BrandOrange),
            decorationBox = { input ->
                Box {
                    if (text.isEmpty()) Text(stringResource(R.string.plan_form_chat_placeholder),
                        color = Muted, fontSize = 14.sp, lineHeight = 23.sp)
                    input()
                }
            })
        HorizontalDivider(color = PlanFormDivider)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(13.dp)) {
            seasonExamples.forEach { example ->
                Text(example.substringBefore("，"), color = BrandOrange, fontSize = 12.sp,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier.clickable(role = Role.Button) { onTextChange(example) }.padding(vertical = 9.dp))
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            if (parsed != null) TextButton(onClick = onConfirm) { Text(stringResource(R.string.plan_form_chat_confirm)) }
            Surface(onClick = onApply, enabled = !parsing, color = style.anchor, shape = RoundedCornerShape(5.dp)) {
                Text(stringResource(if (parsing) R.string.plan_form_chat_parsing else R.string.plan_form_chat_apply) + "  ↗",
                    color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 15.dp, vertical = 8.dp))
            }
        }
        hint?.let {
            Spacer(modifier = Modifier.height(9.dp))
            InlineBanner(text = it, bg = InfoBg, fg = InfoFg, icon = Icons.Filled.Info)
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

    Column(Modifier.fillMaxWidth().background(EditionSurface).border(0.8.dp, PlanFormDivider)
        .padding(horizontal = 13.dp, vertical = 10.dp)) {
        Text("行程登机单", color = Ink, fontFamily = FolioSerif, fontSize = 18.sp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) { IntegratedCityInputRow(origin, onOriginChange, "出发地", "从哪里出发", false, ImeAction.Next) }
            Text("⇄", color = BrandOrange, fontSize = 22.sp, modifier = Modifier.semantics { contentDescription = "互换出发地和目的地" }
                .clickable(role = Role.Button) { onDestinationChange(origin); onOriginChange(destination) }.padding(7.dp))
            Box(Modifier.weight(1f)) { IntegratedCityInputRow(destination, onDestinationChange, "目的地", "想去哪里", true, ImeAction.Done) }
        }
        HorizontalDivider(color = PlanFormDivider)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1.38f)) { DateInputRow(startDate, onStartDateSelected, onStartDateClear) }
            TicketNumber("天数", days, "天", 30, onDaysChange, Modifier.weight(1f))
            TicketNumber("人数", travelers, "人", 20, onTravelersChange, Modifier.weight(1f))
        }
    }
    Spacer(Modifier.height(17.dp))
    FormSectionTitle(text = stringResource(R.string.plan_form_section_budget))
    Spacer(modifier = Modifier.height(8.dp))

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = EditionSurface,
        modifier = Modifier.editionRim(RoundedCornerShape(18.dp)).fillMaxWidth(),
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
    Text(text = text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Ink)
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
            .heightIn(min = 64.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
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
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.plan_form_date_label),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Muted,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value.ifEmpty { "选择日期" },
                fontSize = 13.5.sp,
                fontWeight = if (value.isEmpty()) FontWeight.Normal else FontWeight.SemiBold,
                color = if (value.isEmpty()) Muted.copy(alpha = 0.55f) else Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (value.isNotEmpty()) {
            Text(
                text = "×",
                fontSize = 16.sp,
                color = Muted,
                modifier = Modifier
                    .semantics { contentDescription = "清除日期" }
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
private fun TicketNumber(label: String, value: Int, unit: String, limit: Int, change: (Int) -> Unit, modifier: Modifier) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        Column(Modifier.fillMaxWidth().clickable(role = Role.Button) { open = true }.padding(horizontal = 8.dp, vertical = 6.dp)) {
            Text(label, color = Muted, fontSize = 11.sp)
            Text("$value $unit  ⌄", color = Ink, fontFamily = FolioDigits, fontSize = 18.sp)
        }
        DropdownMenu(open, { open = false }, modifier = Modifier.heightIn(max = 260.dp)) {
            (1..limit).forEach { n -> DropdownMenuItem(text = { Text("$n $unit") }, onClick = { change(n); open = false }) }
        }
    }
}
