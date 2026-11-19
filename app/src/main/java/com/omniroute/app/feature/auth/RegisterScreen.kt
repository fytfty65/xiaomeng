package com.omniroute.app.feature.auth

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
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

    AuthEntryLayout(
        season = season,
        onSeasonChange = onSeasonChange,
        alternateAction = stringResource(R.string.auth_tab_login_short),
        onAlternateAction = onBackToLogin,
    ) {
        Text(
            text = stringResource(R.string.auth_register_submit),
            style = MaterialTheme.typography.headlineSmall,
            color = colors.heading,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(modifier = Modifier.height(16.dp))

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
    }
}
