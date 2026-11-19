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
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.omniroute.app.core.ui.theme.JourneySeason
import kotlinx.coroutines.launch

/**
 * 注册页：与登录页共用 Web 端版式（秋景大图 + 纸面卡片），
 * 只是标题、字段与主按钮换成注册语义。
 */
@Composable
fun RegisterScreen(
    season: JourneySeason,
    onSeasonChange: (JourneySeason) -> Unit,
    onRegister: suspend (username: String, password: String, nickname: String?) -> Result<String>,
    onBackToLogin: () -> Unit,
    onRegistered: (message: String) -> Unit,
) {
    var username by rememberSaveable { mutableStateOf("") }
    var nickname by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmVisible by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val colors = authWebColors()

    val submit: () -> Unit = {
        val name = username.trim()
        val nick = nickname.trim().takeIf { it.isNotEmpty() }
        when {
            loading -> Unit
            name.isEmpty() -> errorText = context.getString(R.string.auth_register_empty_name)
            password.length < 6 -> errorText = context.getString(R.string.auth_password_too_short)
            password != confirmPassword ->
                errorText = context.getString(R.string.auth_password_mismatch)
            else -> {
                errorText = null
                loading = true
                scope.launch {
                    val result = onRegister(name, password, nick)
                    loading = false
                    result
                        .onSuccess { onRegistered(it) }
                        .onFailure {
                            errorText = it.message
                                ?: context.getString(R.string.auth_register_failed)
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
            eyebrow = stringResource(R.string.auth_register_hero_eyebrow),
            title = stringResource(R.string.auth_register_hero_title),
            height = 260.dp,
            initialSeason = HeroSeason.fromKey(season.key),
            onSeasonChanged = { onSeasonChange(JourneySeason.fromKey(it.key)) },
            topBar = {
                AuthHeroBrand(
                    trailingText = stringResource(R.string.auth_tab_login_short),
                    onTrailingClick = onBackToLogin,
                )
            },
        )

        AuthPaperCard(modifier = Modifier.weight(1f)) {
            AuthCardBrandLine()

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.auth_register_greeting),
                fontFamily = FontFamily.Serif,
                fontSize = 30.sp,
                fontWeight = FontWeight.Medium,
                color = colors.heading,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.auth_register_sub_web),
                fontSize = 13.5.sp,
                color = colors.muted,
            )

            Spacer(modifier = Modifier.height(18.dp))

            AuthWebTabs(
                registerSelected = true,
                onLogin = onBackToLogin,
                onRegister = {},
            )

            Spacer(modifier = Modifier.height(18.dp))

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

            Spacer(modifier = Modifier.height(12.dp))

            AuthLabeledField(
                label = stringResource(R.string.auth_label_nickname),
                value = nickname,
                onValueChange = {
                    nickname = it
                    if (errorText != null) errorText = null
                },
                placeholder = stringResource(R.string.auth_nickname_hint),
                icon = Icons.Filled.Face,
                enabled = !loading,
                imeAction = ImeAction.Next,
            )

            Spacer(modifier = Modifier.height(12.dp))

            AuthLabeledField(
                label = stringResource(R.string.auth_label_password),
                value = password,
                onValueChange = {
                    password = it
                    if (errorText != null) errorText = null
                },
                placeholder = stringResource(R.string.auth_set_password_hint),
                icon = Icons.Filled.Lock,
                enabled = !loading,
                isPassword = true,
                passwordVisible = passwordVisible,
                onTogglePassword = { passwordVisible = !passwordVisible },
                imeAction = ImeAction.Next,
            )

            Spacer(modifier = Modifier.height(12.dp))

            AuthLabeledField(
                label = stringResource(R.string.auth_label_confirm_password),
                value = confirmPassword,
                onValueChange = {
                    confirmPassword = it
                    if (errorText != null) errorText = null
                },
                placeholder = stringResource(R.string.auth_confirm_password_hint),
                icon = Icons.Filled.Lock,
                enabled = !loading,
                isPassword = true,
                passwordVisible = confirmVisible,
                onTogglePassword = { confirmVisible = !confirmVisible },
                imeAction = ImeAction.Done,
                onImeAction = submit,
            )

            Spacer(modifier = Modifier.height(20.dp))

            AuthPrimaryButton(
                text = stringResource(R.string.auth_register_submit),
                loading = loading,
                loadingText = stringResource(R.string.auth_creating_account),
                onClick = submit,
            )

            Spacer(modifier = Modifier.height(20.dp))

            HorizontalDivider(thickness = 1.dp, color = colors.hairline)

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.auth_have_account),
                    fontSize = 13.sp,
                    color = colors.muted,
                )
                Spacer(modifier = Modifier.width(6.dp))
                AuthInlineLink(
                    text = stringResource(R.string.auth_login_action),
                    enabled = !loading,
                    onClick = onBackToLogin,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
