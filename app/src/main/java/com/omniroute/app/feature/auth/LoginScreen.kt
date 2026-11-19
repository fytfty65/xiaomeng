package com.omniroute.app.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omniroute.app.R
import com.omniroute.app.core.network.dto.UserDto
import com.omniroute.app.core.ui.theme.JourneySeason
import kotlinx.coroutines.launch

/**
 * 登录页：版式对齐 Web 端（118.178.237.166）的浮动登录卡片——
 * 上方秋景大图 + 左下宋体主标题，下方纸面卡片承载「好久不见。」与表单。
 */
@Composable
fun LoginScreen(
    initialUsername: String,
    notice: String?,
    season: JourneySeason,
    onSeasonChange: (JourneySeason) -> Unit,
    onLogin: suspend (username: String, password: String) -> Result<UserDto>,
    onLoggedIn: (UserDto) -> Unit,
    onGoRegister: () -> Unit,
    onForgotPassword: () -> Unit,
) {
    var username by rememberSaveable { mutableStateOf(initialUsername) }
    var password by rememberSaveable { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var passwordVisible by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val colors = authWebColors()

    LaunchedEffect(initialUsername) {
        if (username.isBlank()) username = initialUsername
    }

    val submit: () -> Unit = {
        val name = username.trim()
        when {
            loading -> Unit
            name.isEmpty() || password.isEmpty() ->
                errorText = context.getString(R.string.auth_login_empty)
            else -> {
                errorText = null
                loading = true
                scope.launch {
                    val result = onLogin(name, password)
                    loading = false
                    result
                        .onSuccess { onLoggedIn(it) }
                        .onFailure {
                            errorText = it.message ?: context.getString(R.string.auth_login_failed)
                        }
                }
            }
        }
    }

    AuthLightStatusBarIcons()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.page),
    ) {
        AuthHero(
            eyebrow = stringResource(R.string.auth_hero_eyebrow),
            title = stringResource(R.string.auth_hero_title),
            initialSeason = HeroSeason.fromKey(season.key),
            onSeasonChanged = { onSeasonChange(JourneySeason.fromKey(it.key)) },
            topBar = {
                AuthHeroBrand(
                    trailingText = stringResource(R.string.auth_tab_register_short),
                    onTrailingClick = onGoRegister,
                )
            },
        )

        AuthPaperCard(modifier = Modifier.weight(1f)) {
            AuthCardBrandLine()

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = stringResource(R.string.auth_login_greeting),
                fontFamily = FontFamily.Serif,
                fontSize = 30.sp,
                fontWeight = FontWeight.Medium,
                color = colors.heading,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.auth_login_sub_web),
                fontSize = 13.5.sp,
                color = colors.muted,
            )

            Spacer(modifier = Modifier.height(20.dp))

            AuthWebTabs(
                registerSelected = false,
                onLogin = {},
                onRegister = onGoRegister,
            )

            Spacer(modifier = Modifier.height(20.dp))

            notice?.let {
                InlineBanner(text = it, bg = InfoBg, fg = InfoFg, icon = Icons.Filled.Check)
                Spacer(modifier = Modifier.height(12.dp))
            }
            errorText?.let {
                InlineBanner(text = it, bg = ErrorBg, fg = ErrorFg, icon = Icons.Filled.Warning)
                Spacer(modifier = Modifier.height(12.dp))
            }

            AuthLabeledField(
                label = stringResource(R.string.auth_username_hint),
                value = username,
                onValueChange = {
                    username = it
                    if (errorText != null) errorText = null
                },
                placeholder = stringResource(R.string.auth_username_placeholder),
                icon = Icons.Filled.Person,
                enabled = !loading,
                imeAction = ImeAction.Next,
            )

            Spacer(modifier = Modifier.height(16.dp))

            AuthLabeledField(
                label = stringResource(R.string.auth_label_password),
                value = password,
                onValueChange = {
                    password = it
                    if (errorText != null) errorText = null
                },
                placeholder = stringResource(R.string.auth_password_placeholder),
                icon = Icons.Filled.Lock,
                enabled = !loading,
                isPassword = true,
                passwordVisible = passwordVisible,
                onTogglePassword = { passwordVisible = !passwordVisible },
                imeAction = ImeAction.Done,
                onImeAction = submit,
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
            ) {
                AuthInlineLink(
                    text = stringResource(R.string.auth_forgot_link),
                    enabled = !loading,
                    onClick = onForgotPassword,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            AuthPrimaryButton(
                text = stringResource(R.string.auth_login_action),
                loading = loading,
                loadingText = stringResource(R.string.auth_entering),
                onClick = submit,
            )

            Spacer(modifier = Modifier.height(22.dp))

            HorizontalDivider(thickness = 1.dp, color = colors.hairline)

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.auth_no_account),
                    fontSize = 13.sp,
                    color = colors.muted,
                )
                Spacer(modifier = Modifier.width(6.dp))
                AuthInlineLink(
                    text = stringResource(R.string.auth_free_register),
                    enabled = !loading,
                    onClick = onGoRegister,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
