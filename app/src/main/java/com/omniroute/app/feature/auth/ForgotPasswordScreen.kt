package com.omniroute.app.feature.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omniroute.app.R
import com.omniroute.app.core.network.dto.ForgotPasswordResponse
import com.omniroute.app.core.ui.components.CircleIconButton
import com.omniroute.app.core.ui.theme.LocalAppPalette
import kotlinx.coroutines.launch

private val CardBg: Color
    @Composable get() = LocalAppPalette.current.surface

/**
 * 找回密码：先申请一次性令牌，再在同一页完成重置。
 * 开发环境服务端会直返令牌，生产环境只提示「如账号存在已发送说明」。
 */
@Composable
fun ForgotPasswordScreen(
    onRequestReset: suspend (username: String) -> Result<ForgotPasswordResponse>,
    onResetPassword: suspend (token: String, newPassword: String) -> Result<String>,
    onBackToLogin: () -> Unit,
) {
    var username by rememberSaveable { mutableStateOf("") }
    // 1 申请令牌 / 2 设置新密码 / 3 完成
    var phase by rememberSaveable { mutableIntStateOf(1) }
    var token by rememberSaveable { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var hintText by remember { mutableStateOf<String?>(null) }
    var doneMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val requestToken: () -> Unit = {
        val name = username.trim()
        when {
            loading -> Unit
            name.isEmpty() -> errorText = context.getString(R.string.auth_error_username_required)
            else -> {
                errorText = null
                loading = true
                scope.launch {
                    val result = onRequestReset(name)
                    loading = false
                    result.onSuccess { response ->
                        if (response.hasResetToken()) {
                            token = response.resetToken.orEmpty()
                            hintText = context.getString(R.string.auth_forgot_dev_hint)
                            phase = 2
                        } else {
                            hintText = context.getString(R.string.auth_forgot_prod_hint)
                            phase = 3
                            doneMessage = response.message?.takeIf { it.isNotBlank() }
                        }
                    }.onFailure {
                        errorText = it.message ?: context.getString(R.string.auth_forgot_failed)
                    }
                }
            }
        }
    }

    val submitReset: () -> Unit = {
        when {
            loading -> Unit
            token.isBlank() -> errorText = context.getString(R.string.auth_error_token_required)
            newPassword.isBlank() -> errorText = context.getString(R.string.auth_error_password_length)
            newPassword != confirmPassword ->
                errorText = context.getString(R.string.auth_error_password_confirm_mismatch)
            else -> {
                errorText = null
                loading = true
                scope.launch {
                    val result = onResetPassword(token.trim(), newPassword)
                    loading = false
                    result.onSuccess { message ->
                        doneMessage = message
                        phase = 3
                    }.onFailure {
                        errorText = it.message ?: context.getString(R.string.auth_reset_failed)
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CreamBg),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 18.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircleIconButton(
                    icon = Icons.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.common_back),
                    onClick = onBackToLogin,
                    background = CardBg,
                    borderColor = CardBorder,
                    tint = Ink,
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = stringResource(R.string.auth_forgot_title),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Ink,
                    )
                    Text(
                        text = stringResource(R.string.auth_forgot_sub),
                        fontSize = 12.sp,
                        color = Muted,
                    )
                }
            }

            Spacer(modifier = Modifier.height(26.dp))

            errorText?.let {
                InlineBanner(text = it, bg = ErrorBg, fg = ErrorFg, icon = Icons.Filled.Warning)
                Spacer(modifier = Modifier.height(14.dp))
            }
            hintText?.let {
                InlineBanner(text = it, bg = InfoBg, fg = InfoFg, icon = Icons.Filled.Check)
                Spacer(modifier = Modifier.height(14.dp))
            }

            Surface(
                shape = RoundedCornerShape(18.dp),
                color = CardBg,
                border = BorderStroke(1.dp, CardBorder),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    when (phase) {
                        1 -> {
                            OutlinedTextField(
                                value = username,
                                onValueChange = {
                                    username = it
                                    if (errorText != null) errorText = null
                                },
                                label = {
                                    Text(
                                        text = stringResource(R.string.auth_forgot_username),
                                        fontSize = 12.sp,
                                    )
                                },
                                singleLine = true,
                                enabled = !loading,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }

                        2 -> {
                            OutlinedTextField(
                                value = token,
                                onValueChange = { token = it },
                                label = {
                                    Text(
                                        text = stringResource(R.string.auth_reset_token),
                                        fontSize = 12.sp,
                                    )
                                },
                                singleLine = true,
                                enabled = !loading,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = newPassword,
                                onValueChange = { newPassword = it },
                                label = {
                                    Text(
                                        text = stringResource(R.string.auth_reset_new_password),
                                        fontSize = 12.sp,
                                    )
                                },
                                singleLine = true,
                                enabled = !loading,
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = confirmPassword,
                                onValueChange = { confirmPassword = it },
                                label = {
                                    Text(
                                        text = stringResource(R.string.auth_reset_confirm_password),
                                        fontSize = 12.sp,
                                    )
                                },
                                singleLine = true,
                                enabled = !loading,
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }

                        else -> {
                            doneMessage?.let {
                                Text(
                                    text = it,
                                    fontSize = 13.5.sp,
                                    color = Ink,
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            val action: () -> Unit = if (phase == 2) submitReset else requestToken
            if (phase != 3) {
                Button(
                    onClick = action,
                    enabled = !loading,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandOrange),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                ) {
                    if (loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(17.dp),
                            strokeWidth = 2.dp,
                            color = Color.White,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = stringResource(
                            when {
                                loading && phase == 2 -> R.string.auth_resetting
                                loading -> R.string.auth_forgot_sending
                                phase == 2 -> R.string.auth_reset_action
                                else -> R.string.auth_forgot_send
                            },
                        ),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                }
            } else {
                Button(
                    onClick = onBackToLogin,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandOrange),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                ) {
                    Text(
                        text = stringResource(R.string.auth_reset_back_login),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                }
            }

            if (phase == 2) {
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = stringResource(R.string.auth_reset_back_login),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = BrandOrange,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable(role = Role.Button, onClick = onBackToLogin)
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                    )
                }
            }
        }
    }
}
