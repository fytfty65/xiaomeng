package com.omniroute.app.feature.profile

import androidx.compose.ui.draw.clipToBounds
import com.omniroute.app.core.ui.theme.FolioSerif
import com.omniroute.app.core.ui.theme.FolioDigits
import androidx.compose.foundation.layout.*
import com.omniroute.app.core.ui.components.CoverBack
import com.omniroute.app.core.ui.components.CoverStatusShade
import com.omniroute.app.core.ui.components.DestinationCover
import com.omniroute.app.core.ui.SystemBarIcons
import com.omniroute.app.core.ui.theme.inkFoil
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.rotate
import com.omniroute.app.core.ui.theme.editionRim
import androidx.compose.material.icons.filled.Info
import com.omniroute.app.core.ui.components.SeasonGallery
import com.omniroute.app.core.ui.theme.EditionSurface
import com.omniroute.app.core.ui.theme.OmniSeasonLogo
import com.omniroute.app.core.ui.theme.LocalJourneySeason
import com.omniroute.app.core.ui.theme.edition
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.heightIn
import com.omniroute.app.core.ui.theme.seasonBackdrop
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import com.omniroute.app.feature.auth.ErrorBg
import com.omniroute.app.feature.auth.ErrorFg
import com.omniroute.app.feature.auth.Ink
import com.omniroute.app.feature.auth.InlineBanner
import com.omniroute.app.feature.auth.Muted
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

    SystemBarIcons(darkIcons = false, darkNavigationIcons = !androidx.compose.foundation.isSystemInDarkTheme())
    Box(
        modifier = Modifier.fillMaxSize().seasonBackdrop(),
    ) {
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
                .verticalScroll(scrollState)
                .wheelScroll(scrollState),
        ) {
            ProfileHero(
                onBack = onBack,
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

            Column(Modifier.padding(horizontal = 20.dp)) {
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

                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    val latest = state.latestTrip
                    DestinationCover(latest?.destCity, latest?.content.orEmpty().contains("天池"),
                        Modifier.weight(1.05f).fillMaxHeight().clip(RoundedCornerShape(7.dp))
                            .clickable(role = Role.Button, onClick = onOpenTrips)) {
                        Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xCE15201C)))))
                        Column(Modifier.align(Alignment.BottomStart).padding(13.dp)) {
                            Text(stringResource(R.string.profile_my_trips), fontFamily = FolioSerif, fontSize = 22.sp, color = Color.White)
                            latest?.let { Text(listOfNotNull(it.destCity, it.days.takeIf { d -> d > 0 }?.let { d -> "$d 天" })
                                .joinToString("  ·  "), color = Color.White, fontSize = 12.sp) }
                        }
                    }
                    Column(Modifier.weight(1f)) {
                        listOf(Triple(R.string.profile_room_title, Icons.Filled.Person, onOpenRoom),
                            Triple(R.string.profile_gallery, Icons.Filled.Add, onOpenGallery),
                            Triple(R.string.profile_risk_title, Icons.Filled.Info, onOpenNotifications)).forEach { (label, icon, action) ->
                            HorizontalDivider(color = SubtleLine)
                            Row(Modifier.fillMaxWidth().heightIn(min = 53.dp).clickable(role = Role.Button, onClick = action),
                                verticalAlignment = Alignment.CenterVertically) {
                                Icon(icon, null, tint = Muted, modifier = Modifier.size(16.dp))
                                Text(stringResource(label), Modifier.weight(1f).padding(start = 8.dp), color = Ink, fontSize = 14.sp)
                                Text("›", color = Muted)
                            }
                        }
                        HorizontalDivider(color = SubtleLine)
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
        CoverStatusShade()
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
private fun ProfileHero(
    onBack: () -> Unit,
    user: UserDto?,
    tripCount: Int?,
    cityCount: Int?,
    loading: Boolean,
    uploading: Boolean,
    onEdit: () -> Unit,
    onPickAvatar: () -> Unit,
) {
    val style = LocalJourneySeason.current.edition
    val foil = Color(0xFFE5DCBF)
    Box(Modifier.fillMaxWidth().inkFoil().clipToBounds()) {
        OmniSeasonLogo(Modifier.align(Alignment.CenterEnd).offset(x = 24.dp).rotate(-14f), diameter = 154.dp,
            tint = foil.copy(alpha = 0.075f))
        Column(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 20.dp, end = 20.dp, bottom = 22.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                CoverBack(onBack)
                Text("OMNIROUTE   /   PASSPORT", fontSize = 12.sp, fontFamily = FolioDigits, color = foil, letterSpacing = 2.sp)
            }
            Text("旅行护照", fontFamily = FolioSerif, fontSize = 35.sp, lineHeight = 46.sp, color = Color.White)
            Spacer(Modifier.height(9.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                val initial = user?.displayName?.trim()?.firstOrNull()?.uppercase() ?: "U"
                Box {
                    RemoteAvatar(
                        path = user?.avatarUrl,
                        fallback = initial,
                        size = 44.dp,
                        background = AccentOrangeBg,
                        foreground = BrandOrange,
                        fontSize = 20.sp,
                        contentDescription = stringResource(R.string.profile_avatar_change),
                        modifier = Modifier.border(2.dp, Color.White, CircleShape).clickable(
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
                            color = Color.White,
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
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        fontSize = 12.5.sp,
                        color = foil,
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
            tripCount?.let {
                HorizontalDivider(color = foil.copy(alpha = 0.28f))
                Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("%02d".format(it), color = foil, fontFamily = FolioDigits, fontSize = 32.sp)
                    Text("份路书", color = foil, fontSize = 12.sp, modifier = Modifier.padding(bottom = 4.dp, end = 16.dp))
                    cityCount?.let { cities ->
                        Text("%02d".format(cities), color = foil, fontFamily = FolioDigits, fontSize = 32.sp)
                        Text("座城", color = foil, fontSize = 12.sp, modifier = Modifier.padding(bottom = 4.dp))
                    }
                }
            }
            user?.signature?.trim()?.takeIf { it.isNotEmpty() }?.let {
                Text(it, fontSize = 12.sp, color = foil, modifier = Modifier.padding(top = 6.dp))
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Ink)
}

@Composable
private fun EntryGroup(content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = EditionSurface,
        modifier = Modifier.editionRim(RoundedCornerShape(18.dp)).fillMaxWidth(),
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
        color = EditionSurface,
        modifier = Modifier.editionRim(RoundedCornerShape(18.dp)).fillMaxWidth(),
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
