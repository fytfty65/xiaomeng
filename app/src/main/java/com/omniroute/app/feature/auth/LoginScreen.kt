package com.omniroute.app.feature.auth

import android.animation.ValueAnimator
import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import com.omniroute.app.R
import com.omniroute.app.core.network.dto.UserDto
import com.omniroute.app.core.ui.SystemBarIcons
import com.omniroute.app.core.ui.theme.JourneySeason
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    initialUsername: String,
    notice: String?,
    season: JourneySeason,
    onSeasonChange: (JourneySeason) -> Unit,
    onLogin: suspend (username: String, password: String) -> Result<UserDto>,
    onRegister: suspend (username: String, password: String, nickname: String?) -> Result<String>,
    onLoggedIn: (UserDto) -> Unit,
    onForgotPassword: () -> Unit,
) {
    var ticketSeason by rememberSaveable { mutableStateOf<JourneySeason?>(null) }
    var register by rememberSaveable { mutableStateOf(false) }
    var input by remember(initialUsername) { mutableStateOf(PassInput(username = initialUsername)) }
    var phase by remember { mutableStateOf(PassPhase.Editing) }
    var failedAttempt by remember { mutableIntStateOf(0) }
    var error by remember { mutableStateOf<String?>(null) }
    var localNotice by remember { mutableStateOf<String?>(null) }
    val focus = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val view = LocalView.current
    val motion = ValueAnimator.areAnimatorsEnabled()
    val selected = ticketSeason ?: season

    val close = {
        if (phase == PassPhase.Editing) {
            focus.clearFocus()
            input = input.copy(password = "", confirmation = "")
            error = null
            localNotice = null
            failedAttempt = 0
            ticketSeason = null
        }
    }

    BackHandler(enabled = ticketSeason != null) { close() }

    fun reject(message: String) {
        phase = PassPhase.Editing
        error = message
        failedAttempt++
    }

    val submit: () -> Unit = submit@{
        if (phase != PassPhase.Editing) return@submit
        val username = input.username.trim()
        val password = input.password
        val nickname = input.nickname.trim().ifEmpty { null }
        val creating = register
        if (username.isBlank() || password.isEmpty()) return@submit
        if (creating && password != input.confirmation) {
            reject(context.getString(R.string.auth_password_mismatch))
            return@submit
        }
        focus.clearFocus()
        phase = PassPhase.Checking
        error = null
        localNotice = null
        scope.launch {
            if (creating) {
                val issued = onRegister(username, password, nickname)
                if (issued.isFailure) {
                    reject(issued.exceptionOrNull()?.message ?: context.getString(R.string.auth_register_failed))
                    return@launch
                }
            }
            val result = onLogin(username, password)
            val user = result.getOrNull()
            if (user == null) {
                if (creating) {
                    phase = PassPhase.Editing
                    register = false
                    input = input.copy(password = "", confirmation = "")
                    localNotice = context.getString(R.string.pass_issued_login)
                } else {
                    val detail = result.exceptionOrNull()?.message
                    reject(
                        if (detail.isNullOrBlank() || detail == context.getString(R.string.auth_error_invalid_credentials)) {
                            context.getString(R.string.pass_invalid_key)
                        } else {
                            detail
                        },
                    )
                }
                return@launch
            }
            input = input.copy(password = "", confirmation = "")
            phase = PassPhase.Verified
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            }
            if (motion) delay(745L)
            onLoggedIn(user)
        }
    }

    SystemBarIcons(darkIcons = ticketSeason != null && selected != JourneySeason.Winter)
    LoginLanding(
        season = selected,
        ticketOpen = ticketSeason != null,
        onSeasonChange = onSeasonChange,
        onOpen = { creating ->
            ticketSeason = season
            register = creating
            failedAttempt = 0
            error = null
            localNotice = null
        },
    ) {
        TravelerPass(
            season = selected,
            register = register,
            input = input,
            onInput = {
                input = it
                error = null
            },
            phase = phase,
            error = error,
            notice = localNotice ?: notice,
            failedAttempt = failedAttempt,
            onMode = { creating ->
                if (register != creating) {
                    register = creating
                    error = null
                    input = input.copy(password = "", confirmation = "")
                }
            },
            onSubmit = submit,
            onClose = close,
            onRecover = {
                input = input.copy(password = "", confirmation = "")
                onForgotPassword()
            },
        )
    }
}
