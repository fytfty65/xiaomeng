package com.omniroute.app.feature.auth

import android.animation.ValueAnimator
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omniroute.app.R
import com.omniroute.app.core.ui.theme.JourneySeason

internal enum class PassPhase { Editing, Checking, Verified }

@Immutable
internal data class PassInput(
    val username: String = "",
    val nickname: String = "",
    val password: String = "",
    val confirmation: String = "",
)

@Composable
internal fun BoxScope.TravelerPass(
    season: JourneySeason,
    register: Boolean,
    input: PassInput,
    onInput: (PassInput) -> Unit,
    phase: PassPhase,
    error: String?,
    notice: String?,
    failedAttempt: Int,
    onMode: (Boolean) -> Unit,
    onSubmit: () -> Unit,
    onClose: () -> Unit,
    onRecover: () -> Unit,
) {
    val style = season.passStyle()
    val density = LocalDensity.current
    val compact = WindowInsets.ime.getBottom(density) > 0
    val motion = ValueAnimator.areAnimatorsEnabled()
    val shake = remember { Animatable(0f) }
    LaunchedEffect(failedAttempt) {
        if (failedAttempt > 0 && motion) {
            shake.animateTo(0f, keyframes {
                durationMillis = 270
                -6f at 48
                5f at 98
                -3f at 150
                2f at 205
            })
        }
    }
    val arrival = remember { Animatable(if (motion) 1f else 0f) }
    LaunchedEffect(Unit) { if (motion) arrival.animateTo(0f, spring(dampingRatio = .89f, stiffness = 255f)) }
    val leave by animateFloatAsState(
        if (phase == PassPhase.Verified && motion) 1f else 0f,
        tween(270, delayMillis = 405),
        label = "ticket-leave",
    )
    val stubExit by animateFloatAsState(
        if (phase == PassPhase.Verified && motion && !compact) 1f else 0f,
        tween(220, delayMillis = 190),
        label = "ticket-stub",
    )
    val scan = if (phase == PassPhase.Checking && motion) {
        val transition = rememberInfiniteTransition(label = "pass-scan")
        transition.animateFloat(
            -1f, 1.35f,
            infiniteRepeatable(tween(1050, easing = LinearEasing)),
            label = "scan-x",
        )
    } else null
    var footerPx by remember { mutableIntStateOf(0) }
    val outline = remember(footerPx, density) { density.passOutline(footerPx) }
    val enabled = phase == PassPhase.Editing
    val ready = input.username.isNotBlank() && input.password.isNotEmpty() &&
        (!register || (input.password.length >= 6 && input.confirmation.isNotEmpty()))

    Column(
        modifier = Modifier
            .align(Alignment.Center)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .imePadding()
            .widthIn(max = 476.dp)
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .graphicsLayer {
                translationX = shake.value.dp.toPx()
                translationY = arrival.value * size.height * .72f - leave * 96.dp.toPx()
                alpha = (1f - arrival.value * .32f) * (1f - leave)
            }
            .padding(horizontal = 18.dp, vertical = if (compact) 5.dp else 32.dp),
    ) {
        IconButton(
            onClick = onClose,
            enabled = enabled,
            modifier = Modifier
                .align(Alignment.End)
                .size(44.dp)
                .background(style.paper.copy(alpha = .68f), CircleShape),
        ) {
            Icon(Icons.Filled.Close, stringResource(R.string.pass_close), tint = style.ink, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.height(5.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .shadow(if (compact) 0.dp else 6.dp, outline, clip = false)
                .clip(outline),
        ) {
            Column(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.fillMaxWidth().passMaterial(season, style)
                        .padding(start = 17.dp, end = 17.dp, top = 13.dp, bottom = 11.dp),
                ) {
                    PassHeading(
                        season = season, compact = compact, register = register, passenger = input.nickname
                            .takeIf { register && it.isNotBlank() } ?: input.username,
                        error = error != null, enabled = enabled, onMode = onMode,
                    )
                    Spacer(Modifier.height(if (compact) 6.dp else 10.dp))
                    PassField(
                        label = stringResource(R.string.pass_traveler_id),
                        value = input.username, onChange = { onInput(input.copy(username = it)) },
                        style = style, enabled = enabled,
                    )
                    AnimatedVisibility(
                        visible = register,
                        enter = expandVertically(spring(dampingRatio = .85f, stiffness = 420f)) + fadeIn(tween(180)),
                        exit = shrinkVertically(spring(dampingRatio = .90f, stiffness = 440f)) + fadeOut(tween(110)),
                    ) {
                        Column {
                            Spacer(Modifier.height(5.dp))
                            PassField(
                                label = stringResource(R.string.pass_nickname),
                                value = input.nickname, onChange = { onInput(input.copy(nickname = it)) },
                                style = style, enabled = enabled,
                            )
                        }
                    }
                    Spacer(Modifier.height(5.dp))
                    var visible by remember { mutableStateOf(false) }
                    PassField(
                        label = stringResource(R.string.pass_access_key),
                        value = input.password, onChange = { onInput(input.copy(password = it)) },
                        style = style, enabled = enabled, secret = !visible, password = true,
                        submit = onSubmit.takeIf { !register && ready && enabled },
                        showPassword = { visible = !visible },
                    )
                    AnimatedVisibility(
                        visible = register,
                        enter = expandVertically(spring(dampingRatio = .85f, stiffness = 420f)) + fadeIn(tween(180)),
                        exit = shrinkVertically(spring(dampingRatio = .90f, stiffness = 440f)) + fadeOut(tween(110)),
                    ) {
                        Column {
                            Spacer(Modifier.height(5.dp))
                            PassField(
                                label = stringResource(R.string.pass_confirm_key),
                                value = input.confirmation, onChange = { onInput(input.copy(confirmation = it)) },
                                style = style, enabled = enabled, secret = true, password = true,
                                submit = onSubmit.takeIf { ready && enabled },
                            )
                        }
                    }
                    if (!register) {
                        Box(Modifier.align(Alignment.End)) {
                            PassLink(stringResource(R.string.pass_recover), style.muted, enabled, onRecover)
                        }
                    }
                    (error ?: notice)?.let {
                        Text(
                            text = it,
                            color = if (error != null) {
                                if (season == JourneySeason.Winter) Color(0xFFFFB0A9) else Color(0xFF9B3A36)
                            } else style.ink,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(top = 4.dp).semantics { liveRegion = LiveRegionMode.Polite },
                        )
                    }
                }
                Column(
                    Modifier
                        .fillMaxWidth()
                        .onSizeChanged { if (footerPx != it.height) footerPx = it.height }
                        .graphicsLayer {
                            translationY = stubExit * 12.dp.toPx()
                            alpha = 1f - stubExit
                        }
                        .passMaterial(season, style, stub = true),
                ) {
                    Canvas(Modifier.fillMaxWidth().height(1.dp).padding(horizontal = 12.dp)) {
                        val center = size.height / 2f
                        drawLine(
                            style.ink.copy(alpha = .18f),
                            Offset(0f, center), Offset(size.width, center),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx())),
                        )
                        scan?.let {
                            val middle = it.value * size.width
                            val span = size.width * .19f
                            drawLine(
                                Brush.horizontalGradient(
                                    listOf(Color.Transparent, style.stamp.copy(alpha = .78f), Color.Transparent),
                                    startX = middle - span, endX = middle + span,
                                ),
                                Offset(0f, center), Offset(size.width, center), strokeWidth = 1.dp.toPx(),
                            )
                        }
                    }
                    Column(Modifier.padding(start = 17.dp, end = 17.dp, top = 11.dp, bottom = 11.dp)) {
                        PassAction(style, register, phase, ready, scan, onSubmit)
                        AnimatedVisibility(visible = !compact, enter = fadeIn(), exit = fadeOut()) {
                            PassBarcode(season, style, Modifier.fillMaxWidth().padding(top = 7.dp))
                        }
                    }
                }
            }
            androidx.compose.animation.AnimatedVisibility(
                visible = phase == PassPhase.Verified,
                modifier = Modifier.align(Alignment.Center),
                enter = scaleIn(spring(dampingRatio = .64f, stiffness = 600f), initialScale = 1.30f) + fadeIn(tween(150)),
            ) {
                val ink = if (season == JourneySeason.Winter) Color(0xFFC9E3DC) else Color(0xFF335C49)
                Text(
                    stringResource(R.string.pass_verified),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = ink,
                    modifier = Modifier
                        .rotate(-10f)
                        .background(style.paper.copy(alpha = .97f), RoundedCornerShape(5.dp))
                        .border(1.5.dp, ink, RoundedCornerShape(5.dp))
                        .padding(horizontal = 13.dp, vertical = 7.dp)
                        .semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
        }
    }
}

@Composable
private fun PassHeading(
    season: JourneySeason,
    compact: Boolean,
    register: Boolean,
    passenger: String,
    enabled: Boolean,
    error: Boolean,
    onMode: (Boolean) -> Unit,
) {
    val style = season.passStyle()
    Text(
        stringResource(
            R.string.pass_header,
            stringResource(if (register) R.string.pass_new_issue else R.string.pass_return_pass),
        ),
        color = style.muted, fontFamily = FontFamily.Monospace, fontSize = 9.sp, letterSpacing = .22.sp,
    )
    Spacer(Modifier.height(7.dp))
    val windowHeight = if (compact) 43.dp else 96.dp
    val verticalBias = when (season) {
        JourneySeason.Spring, JourneySeason.Autumn -> -.65f
        JourneySeason.Summer -> .35f
        JourneySeason.Winter -> -.48f
    }
    Box(Modifier.fillMaxWidth().height(windowHeight + if (compact) 10.dp else 14.dp)) {
        Image(
            painterResource(style.windowArt), contentDescription = null,
            modifier = Modifier.fillMaxWidth().height(windowHeight)
                .border(.5.dp, style.windowBorder, RoundedCornerShape(3.dp))
                .clip(RoundedCornerShape(3.dp)),
            alignment = BiasAlignment(0f, verticalBias),
            contentScale = ContentScale.Crop,
        )
        SeasonSeal(
            season = season, error = error,
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 7.dp)
                .size(if (compact) 42.dp else 59.dp),
        )
    }
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            stringResource(R.string.pass_title),
            color = style.ink,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Medium,
            fontSize = 22.sp,
            lineHeight = 30.sp,
            modifier = Modifier.weight(1f).semantics { heading() },
        )
        Row(
            Modifier
                .background(style.ink.copy(alpha = .06f), CircleShape)
                .clip(CircleShape)
                .selectableGroup(),
        ) {
            listOf(false, true).forEach { choice ->
                Box(
                    Modifier
                        .heightIn(min = 40.dp)
                        .clip(CircleShape)
                        .background(if (register == choice) style.ink.copy(alpha = .13f) else Color.Transparent)
                        .selectable(register == choice, enabled = enabled, role = Role.Tab) { onMode(choice) }
                        .padding(horizontal = 7.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        stringResource(if (choice) R.string.pass_issue else R.string.pass_return),
                        color = style.ink, fontSize = 11.sp,
                        fontWeight = if (register == choice) FontWeight.SemiBold else FontWeight.Normal,
                    )
                }
            }
        }
    }
    Text(
        stringResource(R.string.pass_edition, style.english, season.ordinal + 1, stringResource(style.motto)),
        fontFamily = FontFamily.Monospace, color = style.muted, fontSize = 10.sp, letterSpacing = .28.sp,
    )
    Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            stringResource(R.string.pass_passenger),
            color = style.muted, fontSize = 10.sp, fontFamily = FontFamily.Monospace,
        )
        Spacer(Modifier.width(6.dp))
        AnimatedContent(
            targetState = passenger.trim().ifEmpty { "GUEST" }.take(36),
            transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(80)) },
            label = "ticket-passenger",
            modifier = Modifier.weight(1f),
        ) { name ->
            Text(name, color = style.ink, fontSize = 11.sp, fontFamily = FontFamily.Monospace,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun PassField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    style: SeasonPassStyle,
    enabled: Boolean,
    secret: Boolean = false,
    password: Boolean = false,
    submit: (() -> Unit)? = null,
    showPassword: (() -> Unit)? = null,
) {
    var focused by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth()) {
        Text(label, color = style.muted, fontSize = 11.sp, letterSpacing = .55.sp)
        Row(Modifier.fillMaxWidth().heightIn(min = 37.dp), verticalAlignment = Alignment.CenterVertically) {
            BasicTextField(
                value = value,
                onValueChange = onChange,
                modifier = Modifier.weight(1f)
                    .heightIn(min = 36.dp)
                    .onFocusChanged { focused = it.isFocused }
                    .semantics { contentDescription = label },
                enabled = enabled,
                singleLine = true,
                textStyle = TextStyle(color = style.ink, fontFamily = FontFamily.Monospace, fontSize = 16.sp),
                cursorBrush = SolidColor(style.stamp),
                visualTransformation = if (secret) PasswordVisualTransformation() else VisualTransformation.None,
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (password) KeyboardType.Password else KeyboardType.Text,
                    imeAction = if (submit == null) ImeAction.Next else ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { submit?.invoke() }),
                decorationBox = { content ->
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) { content() }
                },
            )
            if (showPassword != null) {
                PassLink(
                    stringResource(if (secret) R.string.auth_show else R.string.auth_hide),
                    style.muted, enabled, showPassword,
                )
            }
        }
        HorizontalDivider(color = if (focused) style.stamp else style.ink.copy(alpha = .22f), thickness = if (focused) 1.2.dp else .5.dp)
    }
}

@Composable
private fun PassLink(text: String, color: Color, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.heightIn(min = 42.dp).widthIn(min = 44.dp)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = color, fontSize = 12.sp)
    }
}

@Composable
private fun PassAction(
    style: SeasonPassStyle,
    register: Boolean,
    phase: PassPhase,
    ready: Boolean,
    scan: State<Float>?,
    onClick: () -> Unit,
) {
    val opacity by animateFloatAsState(if (ready || phase != PassPhase.Editing) 1f else .65f, tween(240), label = "ticket-ready")
    val readySweep = remember { Animatable(-1f) }
    val motion = ValueAnimator.areAnimatorsEnabled()
    LaunchedEffect(ready, phase, motion) {
        if (ready && phase == PassPhase.Editing && motion) {
            readySweep.snapTo(-.2f)
            readySweep.animateTo(1.2f, tween(490, easing = LinearEasing))
        } else {
            readySweep.snapTo(-1f)
        }
    }
    Box(
        Modifier.fillMaxWidth().clip(CircleShape),
    ) {
        Button(
            onClick, enabled = ready && phase == PassPhase.Editing,
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = style.action.copy(alpha = opacity), contentColor = style.onAction,
                disabledContainerColor = style.action.copy(alpha = opacity), disabledContentColor = style.onAction,
            ),
        ) {
            Text(
                stringResource(
                    when (phase) {
                        PassPhase.Checking -> R.string.pass_checking
                        PassPhase.Verified -> R.string.pass_depart
                        PassPhase.Editing -> if (register) R.string.pass_sign_depart else R.string.pass_depart
                    },
                ),
                fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
            )
        }
        Canvas(Modifier.matchParentSize()) {
            val position = scan?.value ?: readySweep.value
            if (position in -.2f..1.2f) {
                val x = position * size.width
                val spread = size.width * .16f
                drawRect(
                    Brush.horizontalGradient(
                        listOf(Color.Transparent, style.onAction.copy(alpha = .22f), Color.Transparent),
                        startX = x - spread, endX = x + spread,
                    ),
                )
            }
        }
    }
}

@Composable
private fun PassBarcode(season: JourneySeason, style: SeasonPassStyle, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.End) {
        Canvas(Modifier.width(96.dp).height(14.dp)) {
            val widths = listOf(1, 3, 1, 2, 1, 1, 3, 2, 1, 4, 1, 2, 1, 3, 2, 1, 1, 2, 3, 1, 2, 1, 3, 2)
            val unit = size.width / (widths.sum() + widths.size)
            var x = 0f
            widths.forEachIndexed { i, width ->
                val bar = if (i % 6 == 0) width * .68f else width.toFloat()
                drawRect(style.ink.copy(alpha = .65f), Offset(x, 0f), Size(bar * unit, size.height))
                x += (width + 1) * unit
            }
        }
        Text(
            stringResource(R.string.pass_serial, style.code, season.ordinal + 1),
            color = style.muted, fontFamily = FontFamily.Monospace, fontSize = 8.sp, letterSpacing = .32.sp,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

private fun Density.passOutline(footerPx: Int): Shape = GenericShape { size, _ ->
    val w = size.width
    val h = size.height
    val radius = 18.dp.toPx()
    val cut = 10.5.dp.toPx()
    val footer = if (footerPx > 0) footerPx.toFloat() else 104.dp.toPx()
    val seam = (h - footer).coerceIn(radius + cut, maxOf(radius + cut, h - radius - cut))
    moveTo(radius, 0f)
    lineTo(w - radius, 0f)
    quadraticTo(w, 0f, w, radius)
    lineTo(w, seam - cut)
    arcTo(Rect(w - cut, seam - cut, w + cut, seam + cut), -90f, -180f, false)
    lineTo(w, h - radius)
    quadraticTo(w, h, w - radius, h)
    lineTo(radius, h)
    quadraticTo(0f, h, 0f, h - radius)
    lineTo(0f, seam + cut)
    arcTo(Rect(-cut, seam - cut, cut, seam + cut), 90f, -180f, false)
    lineTo(0f, radius)
    quadraticTo(0f, 0f, radius, 0f)
    close()
}
