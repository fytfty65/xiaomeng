package com.omniroute.app.feature.room

import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription

import com.omniroute.app.core.ui.components.FloatingTitle

import com.omniroute.app.core.ui.theme.spatialIsland
import com.omniroute.app.core.ui.theme.world

import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import com.omniroute.app.core.ui.theme.FolioSerif
import com.omniroute.app.core.ui.theme.FolioDigits
import androidx.compose.material3.AlertDialog
import androidx.compose.ui.geometry.Size
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.BarcodeFormat
import kotlin.math.roundToInt
import androidx.compose.ui.graphics.lerp
import com.omniroute.app.core.ui.theme.editionRim
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.TextButton
import androidx.compose.runtime.saveable.rememberSaveable
import com.omniroute.app.core.ui.theme.EditionSurface
import com.omniroute.app.core.ui.theme.LocalJourneySeason
import com.omniroute.app.core.ui.theme.edition
import com.omniroute.app.core.ui.components.SeasonArtwork
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.omniroute.app.AppContainer
import com.omniroute.app.R
import com.omniroute.app.core.room.RoomMember
import com.omniroute.app.core.room.RoomSession
import com.omniroute.app.feature.auth.BrandOrange
import com.omniroute.app.feature.auth.CardBorder
import com.omniroute.app.feature.auth.ErrorBg
import com.omniroute.app.feature.auth.ErrorFg
import com.omniroute.app.feature.auth.GradientButton
import com.omniroute.app.feature.auth.InfoBg
import com.omniroute.app.feature.auth.InfoFg
import com.omniroute.app.feature.auth.Ink
import com.omniroute.app.feature.auth.InlineBanner
import com.omniroute.app.feature.auth.Muted
import com.omniroute.app.feature.auth.SoftField
import com.omniroute.app.core.ui.components.CircleIconButton
import com.omniroute.app.core.ui.theme.LocalAppPalette
import com.omniroute.app.core.ui.wheelScroll

private val CardBg: Color
    @Composable get() = LocalAppPalette.current.surface
private val TagBg: Color
    @Composable get() = LocalAppPalette.current.tagBg
private val OrangeLightBg: Color
    @Composable get() = LocalAppPalette.current.brandTint
private val OnlineGreen: Color
    @Composable get() = LocalAppPalette.current.online

/**
 * 协作房间页：创建房间 / 用 6 位邀请码加入 / 成员实时同步。
 *
 * 房间是规划的资源键：进入规划页后所有推演与成员广播都挂在同一个 room_id 上。
 */
@Composable
fun RoomScreen(
    container: AppContainer,
    onBack: () -> Unit,
    onStartPlanning: () -> Unit,
) {
    val context = LocalContext.current
    // 房间、成员与连接状态由 ViewModel 持有；成员推送会话挂在 ViewModel 上，
    // 旋转屏幕或短暂切页不会断线重连。
    val viewModel: RoomViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                RoomViewModel(container, context.applicationContext)
            }
        },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val roomScrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            RoomTopBar(
                connected = state.connected,
                hasRoom = state.room != null,
                onLeave = viewModel::leaveRoom,
                onBack = onBack,
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(roomScrollState)
                    .wheelScroll(roomScrollState)
                    .padding(horizontal = 20.dp),
            ) {
                Spacer(modifier = Modifier.height(52.dp))

                val current = state.room
                if (current == null) {
                    IntroBlock()
                    Spacer(modifier = Modifier.height(18.dp))
                    CreateRoomCard(
                        name = state.createdName,
                        onNameChange = { viewModel.onNameChange(it) },
                        working = state.working,
                        onCreate = { viewModel.createRoom() },
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    JoinRoomCard(
                        inviteCode = state.inviteInput,
                        onInviteChange = { viewModel.onInviteChange(it) },
                        working = state.working,
                        onJoin = { viewModel.joinRoom() },
                    )
                } else {
                    RoomCodeCard(
                        room = current,
                        members = state.members, connected = state.connected,
                        onCopy = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(
                                ClipData.newPlainText(
                                    context.getString(R.string.room_invite_clip_label),
                                    current.inviteCode,
                                ),
                            )
                        },
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    VariantsSection(
                        variants = state.variants,
                        loading = state.loadingVariants,
                        votingId = state.votingVariantId,
                        adoptingId = state.adoptingVariantId,
                        notice = state.variantNotice,
                        errorText = state.variantError,
                        onVote = { viewModel.voteVariant(it) },
                        onAdopt = { viewModel.adoptVariant(it) },
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                }

                state.errorText?.let { message ->
                    Spacer(modifier = Modifier.height(12.dp))
                    InlineBanner(text = message, bg = ErrorBg, fg = ErrorFg, icon = Icons.Filled.Warning)
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            if (state.room != null) {
                Surface(
                    color = EditionSurface,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                        GradientButton(
                            text = stringResource(R.string.room_enter_plan),
                            loading = false,
                            onClick = onStartPlanning,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RoomTopBar(connected: Boolean, hasRoom: Boolean, onLeave: () -> Unit, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircleIconButton(
                icon = Icons.Filled.ArrowBack,
                contentDescription = stringResource(R.string.common_back),
                onClick = onBack,
                background = CardBg,
                borderColor = CardBorder,
                tint = Ink,
            )
            Spacer(modifier = Modifier.width(12.dp))
            FloatingTitle(stringResource(R.string.room_title))
        }

        if (hasRoom) {
            Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(50), color = if (connected) InfoBg else TagBg) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (connected) OnlineGreen else Muted.copy(alpha = 0.5f)),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(
                            if (connected) R.string.room_members_synced else R.string.room_connecting,
                        ),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (connected) InfoFg else Muted,
                    )
                }
            }
            var menu by remember { mutableStateOf(false) }
            Box {
                androidx.compose.material3.IconButton(onClick = { menu = true }) {
                    Text("•••", color = Ink, modifier = Modifier.semantics { contentDescription = "房间选项" })
                }
                androidx.compose.material3.DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    androidx.compose.material3.DropdownMenuItem(text = { Text(stringResource(R.string.room_leave)) },
                        onClick = { menu = false; onLeave() })
                }
            }
            }

        }
    }
}

@Composable
private fun IntroBlock() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(OrangeLightBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = null,
                tint = BrandOrange,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = stringResource(R.string.room_hero_title),
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Ink,
            )
            Text(
                text = stringResource(R.string.room_hero_sub),
                fontSize = 12.sp,
                color = Muted,
                lineHeight = 17.sp,
            )
        }
    }
}

@Composable
private fun CreateRoomCard(
    name: String,
    onNameChange: (String) -> Unit,
    working: Boolean,
    onCreate: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = EditionSurface,
        modifier = Modifier.editionRim(RoundedCornerShape(16.dp)).fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.room_create_title),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Ink,
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = stringResource(R.string.room_create_sub),
                fontSize = 12.sp,
                color = Muted,
            )
            Spacer(modifier = Modifier.height(12.dp))
            SoftField(
                value = name,
                onValueChange = onNameChange,
                placeholder = stringResource(R.string.room_create_hint),
                icon = Icons.Filled.Edit,
                enabled = !working,
            )
            Spacer(modifier = Modifier.height(12.dp))
            GradientButton(
                text = stringResource(R.string.room_create_action),
                loading = working,
                loadingText = stringResource(R.string.room_creating),
                onClick = onCreate,
            )
        }
    }
}

@Composable
private fun JoinRoomCard(
    inviteCode: String,
    onInviteChange: (String) -> Unit,
    working: Boolean,
    onJoin: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = EditionSurface,
        modifier = Modifier.editionRim(RoundedCornerShape(16.dp)).fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.room_join_title),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Ink,
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = stringResource(R.string.room_join_sub),
                fontSize = 12.sp,
                color = Muted,
            )
            Spacer(modifier = Modifier.height(12.dp))
            SoftField(
                value = inviteCode,
                onValueChange = onInviteChange,
                placeholder = stringResource(R.string.room_join_hint),
                icon = Icons.Filled.Lock,
                enabled = !working,
            )
            Spacer(modifier = Modifier.height(12.dp))
            GradientButton(
                text = stringResource(R.string.room_join_action),
                loading = working,
                loadingText = stringResource(R.string.room_joining),
                onClick = onJoin,
            )
        }
    }
}

@Composable
private fun RoomCodeCard(
    room: RoomSession,
    members: List<RoomMember>,
    connected: Boolean,
    onCopy: () -> Unit,
) {
    var copied by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val notch = with(density) { 10.dp.toPx() }
    var headerHeight by remember { mutableIntStateOf(0) }
    val perforation = headerHeight + with(density) { 33.dp.toPx() }
    val shape = remember(notch, perforation) {
        GenericShape { size, _ ->
            val cut = perforation.coerceIn(notch * 2, size.height - notch * 2)
            val w = size.width
            val h = size.height
            val corner = notch * 2f
            moveTo(corner, 0f)
            lineTo(w - corner, 0f)
            quadraticTo(w, 0f, w, corner)
            lineTo(w, cut - notch)
            arcTo(Rect(w - notch, cut - notch, w + notch, cut + notch), -90f, -180f, false)
            lineTo(w, h - corner)
            quadraticTo(w, h, w - corner, h)
            lineTo(corner, h)
            quadraticTo(0f, h, 0f, h - corner)
            lineTo(0f, cut + notch)
            arcTo(Rect(-notch, cut - notch, notch, cut + notch), 90f, -180f, false)
            lineTo(0f, corner)
            quadraticTo(0f, 0f, corner, 0f)
            close()
        }
    }
    val accent = BrandOrange
    val style = LocalJourneySeason.current.edition
    Surface(
        shape = shape,
        color = EditionSurface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(Modifier) {
            Canvas(Modifier.matchParentSize()) {
                val y = perforation
                drawLine(
                    accent.copy(alpha = 0.40f), Offset(notch * 2, y), Offset(size.width - notch * 2, y),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 4.dp.toPx())),
                )
            }
            Column(modifier = Modifier.padding(18.dp)) {
                Column(Modifier.onSizeChanged { headerHeight = it.height }) {
                    Text("OMNIROUTE   /   BOARDING PASS", fontFamily = FolioDigits, fontSize = 13.sp, letterSpacing = 1.7.sp, color = BrandOrange)
                    Row(Modifier.fillMaxWidth().padding(top = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(room.roomName.ifBlank { stringResource(R.string.room_fallback_name) }, fontFamily = FolioSerif,
                                fontSize = 22.sp, color = Ink)
                            Text(stringResource(if (room.isHost) R.string.room_host else R.string.room_member), color = Muted, fontSize = 12.sp)
                        }
                        RoomManifest(members, connected)
                    }
                }
                Spacer(Modifier.height(30.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.room_invite_code), fontSize = 12.sp, color = Muted)
                        Text(room.inviteCode, fontFamily = FolioDigits, fontSize = if (room.inviteCode.length > 8) 28.sp else 40.sp,
                            letterSpacing = 3.sp, color = Ink)
                    }
                    val code = remember(room.inviteCode) { runCatching {
                        QRCodeWriter().encode(room.inviteCode, BarcodeFormat.QR_CODE, 1, 1)
                    }.getOrNull() }
                    if (code != null) Canvas(Modifier.size(56.dp).background(Color.White)) {
                        val unit = kotlin.math.floor(size.minDimension / code.width)
                        val offset = (size.minDimension - unit * code.width) / 2f
                        for (y in 0 until code.height) for (x in 0 until code.width) if (code[x, y])
                            drawRect(Color.Black, Offset(offset + x * unit, offset + y * unit), Size(unit, unit))
                    }
                    TextButton(onClick = { onCopy(); copied = true }) {
                        Text(stringResource(if (copied) R.string.room_copied else R.string.room_copy), color = BrandOrange)
                    }
                }
                Text(
                    text = stringResource(R.string.room_invite_hint),
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    color = Muted,
                )

            }
        }
    }
}

/** 房间方案池：成员加入候选方案后在这里投票，胜出方案可一键采纳建立预算基线。 */

@Composable
private fun VariantsSection(
    variants: List<PlanVariantUi>,
    loading: Boolean,
    votingId: String?,
    adoptingId: String?,
    notice: String?,
    errorText: String?,
    onVote: (String) -> Unit,
    onAdopt: (String) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = EditionSurface,
        modifier = Modifier.editionRim(RoundedCornerShape(16.dp)).fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    tint = BrandOrange,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.room_variants_title),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Ink,
                    )
                    Text(
                        text = stringResource(R.string.room_variants_sub),
                        fontSize = 11.5.sp,
                        color = Muted,
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            when {
                variants.isEmpty() && loading -> Text(
                    text = stringResource(R.string.room_variants_loading),
                    fontSize = 12.sp,
                    color = Muted,
                )

                variants.isEmpty() -> Text(
                    text = stringResource(R.string.room_variants_empty),
                    fontSize = 12.sp,
                    color = Muted,
                    lineHeight = 18.sp,
                )

                else -> variants.forEachIndexed { index, variant ->
                    if (index > 0) {
                        HorizontalDivider(
                            color = CardBorder,
                            modifier = Modifier.padding(vertical = 10.dp),
                        )
                    }
                    VariantRow(
                        variant = variant,
                        voteShare = variant.votes.toFloat() / variants.sumOf { it.votes }.coerceAtLeast(1),
                        voting = votingId == variant.id,
                        voteEnabled = votingId == null,
                        adopting = adoptingId == variant.id,
                        adoptEnabled = adoptingId == null,
                        onVote = { onVote(variant.id) },
                        onAdopt = { onAdopt(variant.id) },
                    )
                }
            }
            notice?.let { message ->
                Spacer(modifier = Modifier.height(10.dp))
                Text(text = message, fontSize = 11.5.sp, color = BrandOrange)
            }
            errorText?.let { message ->
                Spacer(modifier = Modifier.height(10.dp))
                InlineBanner(text = message, bg = ErrorBg, fg = ErrorFg, icon = Icons.Filled.Warning)
            }
        }
    }
}

@Composable
private fun VariantRow(
    variant: PlanVariantUi,
    voteShare: Float,
    voting: Boolean,
    voteEnabled: Boolean,
    adopting: Boolean,
    adoptEnabled: Boolean,
    onVote: () -> Unit,
    onAdopt: () -> Unit,
) {
    val meta = listOfNotNull(
        variant.style.takeIf { it.isNotBlank() },
        stringResource(R.string.room_variants_votes, variant.votes),
    ).joinToString(" · ")
    Column {
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
            val picture = when {
                "天山" in variant.name -> R.drawable.destination_tianchi
                "乌鲁木齐" in variant.name -> R.drawable.destination_urumqi
                "青岛" in variant.name -> R.drawable.destination_qingdao
                else -> null
            }
            val thumb = Modifier.size(56.dp).clip(RoundedCornerShape(4.dp))
            if (picture == null) SeasonArtwork(thumb, cover = true)
            else Image(painterResource(picture), null, thumb, contentScale = ContentScale.Crop)
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = variant.name.ifBlank { stringResource(R.string.room_variants_title) },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (variant.winner) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = OrangeLightBg,
                            shape = RoundedCornerShape(6.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.room_variants_leading),
                                fontSize = 10.sp,
                                color = BrandOrange,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(text = "${(voteShare * 100).roundToInt()}%   ·   $meta", fontFamily = FolioDigits, fontSize = 14.sp, color = Muted)
            }
            Spacer(modifier = Modifier.width(10.dp))
            Surface(onClick = onVote, enabled = voteEnabled, shape = CircleShape, color = OrangeLightBg) {
                Text(stringResource(if (voting) R.string.room_variants_voting else R.string.room_variants_vote),
                    color = BrandOrange, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
            }
        }
        Spacer(Modifier.height(9.dp))
        Box(Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(Ink.copy(alpha = 0.08f))) {
            Box(Modifier.fillMaxWidth(voteShare.coerceIn(0f, 1f)).height(8.dp).clip(CircleShape)
                .background(LocalJourneySeason.current.edition.actionGradient))
        }
        if (variant.winner) {
            Spacer(modifier = Modifier.height(8.dp))
            Surface(onClick = onAdopt, enabled = adoptEnabled, modifier = Modifier.align(Alignment.End),
                shape = RoundedCornerShape(5.dp), color = LocalJourneySeason.current.edition.anchor) {
                Text(if (adopting) stringResource(R.string.room_variants_adopting) else "采纳此方案  ↗",
                    color = Color(0xFFF6E5BC), fontSize = 12.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
            }
        }
    }
}

@Composable
private fun RoomManifest(members: List<RoomMember>, connected: Boolean) {
    var open by rememberSaveable { mutableStateOf(false) }
    val palette = LocalAppPalette.current
    Column(Modifier.clickable(role = Role.Button) { open = true }.padding(5.dp), horizontalAlignment = Alignment.End) {
        Row(horizontalArrangement = Arrangement.spacedBy((-6).dp)) {
            members.take(3).forEachIndexed { index, member ->
                Box(Modifier.size(28.dp).background(lerp(palette.surface, LocalJourneySeason.current.edition.secondary,
                    0.18f + index * 0.12f), CircleShape).border(1.dp, palette.surface, CircleShape), contentAlignment = Alignment.Center) {
                    Text(member.initial, color = Ink, fontSize = 12.sp)
                }
            }
        }
        Text(if (connected) "${members.size} 位同行者" else "连接中", color = Muted, fontSize = 11.sp)
    }
    if (open) AlertDialog(onDismissRequest = { open = false }, title = { Text(stringResource(R.string.room_online_members)) },
        text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (members.isEmpty()) Text(stringResource(R.string.room_waiting_sync))
            members.forEach { MemberRow(it) }
        } }, confirmButton = { TextButton(onClick = { open = false }) { Text(stringResource(R.string.common_confirm)) } })
}

@Composable
private fun MemberRow(member: RoomMember) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(OrangeLightBg),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = member.initial,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = BrandOrange,
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = member.name ?: stringResource(R.string.room_traveler),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val intent = member.intent?.trim().orEmpty()
            if (intent.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = intent,
                    fontSize = 12.sp,
                    color = Muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Surface(shape = RoundedCornerShape(6.dp), color = TagBg) {
            Text(
                text = member.role?.takeIf { it.isNotBlank() }
                    ?: stringResource(R.string.room_member),
                fontSize = 12.sp,
                color = Muted,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
    }
}
