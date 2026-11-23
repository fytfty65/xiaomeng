package com.omniroute.app.feature.profile

import androidx.compose.material.icons.filled.Info
import com.omniroute.app.core.ui.components.SeasonGallery
import com.omniroute.app.core.ui.theme.EditionSurface
import com.omniroute.app.core.ui.theme.OmniSeasonLogo
import com.omniroute.app.core.ui.theme.LocalJourneySeason
import com.omniroute.app.core.ui.theme.edition
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.text.font.FontFamily
import com.omniroute.app.core.ui.theme.seasonBackdrop
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.omniroute.app.AppContainer
import com.omniroute.app.R
import com.omniroute.app.core.network.dto.UserDto
import com.omniroute.app.feature.auth.BrandOrange
import com.omniroute.app.feature.auth.CardBorder
import com.omniroute.app.feature.auth.CreamBg
import com.omniroute.app.feature.auth.ErrorBg
import com.omniroute.app.feature.auth.ErrorFg
import com.omniroute.app.feature.auth.Ink
import com.omniroute.app.feature.auth.InlineBanner
import com.omniroute.app.feature.auth.Muted
import com.omniroute.app.core.ui.components.CircleIconButton
import com.omniroute.app.core.ui.components.RemoteAvatar
import com.omniroute.app.core.ui.theme.JourneySeason
import com.omniroute.app.core.ui.theme.LocalAppPalette
import com.omniroute.app.core.ui.wheelScroll
import kotlinx.coroutines.delay

private val CardBg: Color
    @Composable get() = LocalAppPalette.current.surface
private val SoftTagBg: Color
    @Composable get() = LocalAppPalette.current.tagBg
private val AccentOrangeBg: Color
    @Composable get() = LocalAppPalette.current.brandTint
private val SubtleLine: Color
    @Composable get() = LocalAppPalette.current.divider

/**
 * 我的 / 设置页：账号资料、行程归档与协作入口的统一入口。
 *
 * <p>资料编辑成功后由仓库侧刷新本地用户快照，页面只负责展示与收集输入。
 */
@Composable
fun ProfileScreen(
    container: AppContainer,
    initialUser: UserDto?,
    season: JourneySeason,
    onSeasonChange: (JourneySeason) -> Unit,
    onOpenTrips: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenRoom: () -> Unit,
    onOpenCommunity: () -> Unit,
    onOpenGallery: () -> Unit,
    onLoggedOut: () -> Unit,
    onBack: () -> Unit,
) {
    // 页面状态统一由 ViewModel 持有：旋转、进程重建都不会丢资料编辑草稿与弹窗。
    val context = LocalContext.current
    val viewModel: ProfileViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                ProfileViewModel(
                    container,
                    createSavedStateHandle(),
                    context.applicationContext,
                    initialUser,
                )
            }
        },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val user = state.user

    val avatarPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) viewModel.uploadAvatar(uri)
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect {
            when (it) {
                ProfileEvent.LoggedOut -> onLoggedOut()
            }
        }
    }

    // 成功提示与头像错误都是一次性反馈，展示几秒后自动收起。
    LaunchedEffect(state.notice) {
        if (state.notice != null) {
            delay(4_000)
            viewModel.clearNotice()
        }
    }
    LaunchedEffect(state.avatarError) {
        if (state.avatarError != null) {
            delay(6_000)
            viewModel.clearAvatarError()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .seasonBackdrop(),
    ) {
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(scrollState)
                .wheelScroll(scrollState)
                .padding(horizontal = 20.dp),
        ) {
            ProfileTopBar(onBack = onBack)
            Spacer(modifier = Modifier.height(6.dp))

            ProfileHero(
                user = user,
                tripCount = state.tripCount.takeIf { state.cityCount != null && !state.tripCountError },
                cityCount = state.cityCount,
                loading = state.loadingProfile,
                uploading = state.avatarUploading,
                onEdit = { viewModel.startEdit() },
                onPickAvatar = {
                    avatarPicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                },
            )

            state.profileError?.let {
                Spacer(modifier = Modifier.height(12.dp))
                InlineBanner(text = it, bg = ErrorBg, fg = ErrorFg, icon = Icons.Filled.Warning)
            }

            state.avatarError?.let {
                Spacer(modifier = Modifier.height(12.dp))
                InlineBanner(text = it, bg = ErrorBg, fg = ErrorFg, icon = Icons.Filled.Warning)
            }

            state.notice?.let {
                Spacer(modifier = Modifier.height(12.dp))
                InlineBanner(text = it, bg = AccentOrangeBg, fg = BrandOrange, icon = Icons.Filled.Check)
            }

            Spacer(modifier = Modifier.height(24.dp))
            SectionTitle(text = stringResource(R.string.profile_section_season))
            Spacer(modifier = Modifier.height(10.dp))

            SeasonGallery(season, onSeasonChange)


            Spacer(modifier = Modifier.height(24.dp))
            SectionTitle(text = stringResource(R.string.profile_section_content))
            Spacer(modifier = Modifier.height(10.dp))

            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    ProfileTile(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        icon = Icons.Filled.List,
                        title = stringResource(R.string.profile_my_trips),
                        subtitle = when {
                            state.tripCount > 0 -> stringResource(R.string.profile_trips_count, state.tripCount)
                            state.tripCountError -> stringResource(R.string.profile_trips_count_error)
                            else -> stringResource(R.string.profile_trips_sub)
                        },
                        onClick = onOpenTrips,
                    )
                    ProfileTile(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        icon = Icons.Filled.Info,
                        title = stringResource(R.string.profile_risk_title),
                        subtitle = stringResource(R.string.profile_risk_sub),
                        onClick = onOpenNotifications,
                    )
                }
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    ProfileTile(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        icon = Icons.Filled.Person,
                        title = stringResource(R.string.profile_room_title),
                        subtitle = stringResource(R.string.profile_room_sub),
                        onClick = onOpenRoom,
                    )
                    ProfileTile(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        icon = Icons.Filled.Add,
                        title = stringResource(R.string.profile_gallery),
                        subtitle = stringResource(R.string.profile_gallery_sub),
                        onClick = onOpenGallery,
                    )
                }
            }


            Spacer(modifier = Modifier.height(24.dp))
            SectionTitle(text = stringResource(R.string.profile_section_account))
            Spacer(modifier = Modifier.height(10.dp))

            AccountInfoCard(user = user)

            Spacer(modifier = Modifier.height(12.dp))

            EntryGroup {
                ProfileEntryRow(
                    icon = Icons.Filled.Lock,
                    title = stringResource(R.string.profile_change_password),
                    subtitle = stringResource(R.string.profile_change_password_sub),
                    onClick = { viewModel.openChangePassword() },
                )
                HorizontalDivider(color = CardBorder)
                ProfileEntryRow(
                    icon = Icons.Filled.List,
                    title = stringResource(R.string.profile_community),
                    subtitle = stringResource(R.string.profile_community_sub),
                    onClick = onOpenCommunity,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            LogoutButton(
                loggingOut = state.loggingOut,
                onClick = { viewModel.requestLogout() },
            )

            Spacer(modifier = Modifier.height(28.dp))
        }
    }

    if (state.editing && user != null) {
        EditProfileDialog(
            nickname = state.nicknameDraft,
            signature = state.signatureDraft,
            saving = state.saving,
            error = state.saveError,
            onNicknameChange = { viewModel.onNicknameChange(it) },
            onSignatureChange = { viewModel.onSignatureChange(it) },
            onDismiss = { viewModel.dismissEdit() },
            onConfirm = { viewModel.saveProfile() },
        )
    }

    if (state.showChangePassword) {
        ChangePasswordDialog(
            changing = state.changingPassword,
            error = state.changePasswordError,
            onDismiss = { viewModel.dismissChangePassword() },
            onConfirm = { current, next, confirm -> viewModel.changePassword(current, next, confirm) },
        )
    }

    if (state.confirmLogout) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissLogout() },
            title = { Text(text = stringResource(R.string.profile_logout_title), fontSize = 16.sp, color = Ink) },
            text = {
                Text(
                    text = stringResource(R.string.profile_logout_message),
                    fontSize = 13.sp,
                    color = Muted,
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !state.loggingOut,
                    onClick = { viewModel.logout() },
                ) {
                    Text(
                        text = stringResource(
                            if (state.loggingOut) R.string.profile_logging_out else R.string.profile_logout,
                        ),
                        color = BrandOrange,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissLogout() }, enabled = !state.loggingOut) {
                    Text(text = stringResource(R.string.common_cancel), color = Muted)
                }
            },
        )
    }
}

@Composable
private fun ProfileTopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
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
            Column {
                Text(
                    text = stringResource(R.string.profile_title),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Ink,
                )
                Text(
                    text = stringResource(R.string.profile_subtitle),
                    fontSize = 12.sp,
                    color = Muted,
                )
            }
        }
    }
}

@Composable
private fun ProfileHero(
    user: UserDto?,
    tripCount: Int?,
    cityCount: Int?,
    loading: Boolean,
    uploading: Boolean,
    onEdit: () -> Unit,
    onPickAvatar: () -> Unit,
) {
    val style = LocalJourneySeason.current.edition
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = EditionSurface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(17.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("旅行护照", fontFamily = style.titleFont, fontWeight = style.titleWeight, fontSize = 22.sp, color = Ink)
                    Text("OMNIROUTE PASSPORT", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = BrandOrange, letterSpacing = 1.sp)
                }
                OmniSeasonLogo(diameter = 38.dp)
            }
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                val initial = user?.displayName?.trim()?.firstOrNull()?.uppercase() ?: "U"
                Box {
                    RemoteAvatar(
                        path = user?.avatarUrl,
                        fallback = initial,
                        size = 60.dp,
                        background = AccentOrangeBg,
                        foreground = BrandOrange,
                        fontSize = 24.sp,
                        contentDescription = stringResource(R.string.profile_avatar_change),
                        modifier = Modifier.clickable(
                            enabled = !uploading,
                            role = Role.Button,
                            onClick = onPickAvatar,
                        ),
                    )
                    if (uploading) {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(Ink.copy(alpha = 0.45f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = Color.White,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = user?.displayName?.takeIf { it.isNotBlank() }
                                ?: stringResource(R.string.profile_not_logged_in),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Ink,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (loading) {
                            Spacer(modifier = Modifier.width(8.dp))
                            CircularProgressIndicator(
                                modifier = Modifier.size(12.dp),
                                strokeWidth = 1.6.dp,
                                color = BrandOrange,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = user?.username?.takeIf { it.isNotBlank() }?.let { "@$it" } ?: "—",
                        fontSize = 12.5.sp,
                        color = Muted,
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(AccentOrangeBg)
                        .clickable(role = Role.Button, onClick = onEdit)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = null,
                            tint = BrandOrange,
                            modifier = Modifier.size(13.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.profile_edit),
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandOrange,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = SubtleLine, thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))

            tripCount?.let {
                val trips = stringResource(R.string.profile_trips_count, it)
                val cities = cityCount?.let { count -> "探索 $count 座城市" }
                Text(listOfNotNull(trips, cities).joinToString("  ·  "), color = BrandOrange, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(5.dp))
            }
            Text(
                fontFamily = style.titleFont,
                text = user?.signature?.trim()?.takeIf { it.isNotEmpty() }
                    ?: stringResource(R.string.profile_no_signature),
                fontSize = 12.5.sp,
                lineHeight = 19.sp,
                color = if (user?.signature.isNullOrBlank()) Muted else Ink.copy(alpha = 0.85f),
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(width = 4.dp, height = 14.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(BrandOrange),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Ink,
        )
    }
}

@Composable
private fun EntryGroup(content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = CardBg,
        border = BorderStroke(1.dp, CardBorder),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            content()
        }
    }
}

@Composable
private fun EntryDivider() {
    HorizontalDivider(
        color = SubtleLine,
        thickness = 1.dp,
        modifier = Modifier.padding(start = 52.dp, end = 16.dp),
    )
}

@Composable
private fun ProfileTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalAppPalette.current
    Surface(onClick = onClick, modifier = modifier, shape = RoundedCornerShape(17.dp), color = EditionSurface) {
        Column(Modifier.padding(11.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Surface(shape = CircleShape, color = palette.brandTint) {
                Icon(icon, null, tint = palette.brand, modifier = Modifier.padding(6.dp).size(17.dp))
            }
            Text(title, color = Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = Muted, fontSize = 11.sp, lineHeight = 15.sp)
        }
    }
}

@Composable
private fun ProfileEntryRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(SoftTagBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Muted,
                modifier = Modifier.size(16.dp),
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Ink,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = Muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            imageVector = Icons.Filled.ArrowForward,
            contentDescription = null,
            tint = Muted,
            modifier = Modifier.size(15.dp),
        )
    }
}

@Composable
private fun AccountInfoCard(user: UserDto?) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = CardBg,
        border = BorderStroke(1.dp, CardBorder),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            InfoRow(
                label = stringResource(R.string.profile_login_account),
                value = user?.username?.takeIf { it.isNotBlank() } ?: "—",
            )
            Spacer(modifier = Modifier.height(10.dp))
            InfoRow(
                label = stringResource(R.string.profile_registered_at),
                value = user?.createdAt?.take(10)?.takeIf { it.isNotBlank() } ?: "—",
            )
            Spacer(modifier = Modifier.height(10.dp))
            InfoRow(
                label = stringResource(R.string.profile_account_type),
                value = stringResource(R.string.profile_account_type_value),
            )
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, fontSize = 12.5.sp, color = Muted)
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = Ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun LogoutButton(loggingOut: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(CardBg)
            .border(1.dp, SubtleLine, RoundedCornerShape(12.dp))
            .clickable(enabled = !loggingOut, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(
                if (loggingOut) R.string.profile_logging_out else R.string.profile_logout,
            ),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = ErrorFg,
        )
    }
}

@Composable
private fun EditProfileDialog(
    nickname: String,
    signature: String,
    saving: Boolean,
    error: String?,
    onNicknameChange: (String) -> Unit,
    onSignatureChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.profile_edit), fontSize = 16.sp, color = Ink) },
        text = {
            Column {
                OutlinedTextField(
                    value = nickname,
                    onValueChange = onNicknameChange,
                    label = { Text(text = stringResource(R.string.profile_nickname), fontSize = 12.sp) },
                    singleLine = true,
                    enabled = !saving,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = signature,
                    onValueChange = onSignatureChange,
                    label = { Text(text = stringResource(R.string.profile_signature), fontSize = 12.sp) },
                    enabled = !saving,
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (error != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    InlineBanner(text = error, bg = ErrorBg, fg = ErrorFg, icon = Icons.Filled.Warning)
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !saving && (nickname.isNotBlank() || signature.isNotBlank()),
                onClick = onConfirm,
            ) {
                Text(
                    text = stringResource(
                        if (saving) R.string.profile_saving else R.string.common_save,
                    ),
                    color = BrandOrange,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !saving) {
                Text(text = stringResource(R.string.common_cancel), color = Muted)
            }
        },
    )
}

/** 修改密码：输入只留在本次弹窗内，不写入 SavedState，避免明文密码落盘。 */
@Composable
private fun ChangePasswordDialog(
    changing: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onConfirm: (current: String, newPassword: String, confirmPassword: String) -> Unit,
) {
    var current by rememberSaveable { mutableStateOf("") }
    var newPassword by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.profile_change_password),
                fontSize = 16.sp,
                color = Ink,
            )
        },
        text = {
            Column {
                PasswordField(
                    value = current,
                    labelRes = R.string.profile_current_password,
                    enabled = !changing,
                    onValueChange = { current = it },
                )
                Spacer(modifier = Modifier.height(10.dp))
                PasswordField(
                    value = newPassword,
                    labelRes = R.string.profile_new_password,
                    enabled = !changing,
                    onValueChange = { newPassword = it },
                )
                Spacer(modifier = Modifier.height(10.dp))
                PasswordField(
                    value = confirmPassword,
                    labelRes = R.string.profile_confirm_new_password,
                    enabled = !changing,
                    onValueChange = { confirmPassword = it },
                )
                if (error != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    InlineBanner(
                        text = error,
                        bg = ErrorBg,
                        fg = ErrorFg,
                        icon = Icons.Filled.Warning,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !changing &&
                    current.isNotBlank() &&
                    newPassword.isNotBlank() &&
                    confirmPassword.isNotBlank(),
                onClick = { onConfirm(current, newPassword, confirmPassword) },
            ) {
                Text(
                    text = stringResource(
                        if (changing) R.string.profile_changing_password
                        else R.string.profile_change_password_action,
                    ),
                    color = BrandOrange,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !changing) {
                Text(text = stringResource(R.string.common_cancel), color = Muted)
            }
        },
    )
}

@Composable
private fun PasswordField(
    value: String,
    labelRes: Int,
    enabled: Boolean,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(text = stringResource(labelRes), fontSize = 12.sp) },
        singleLine = true,
        enabled = enabled,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        modifier = Modifier.fillMaxWidth(),
    )
}
