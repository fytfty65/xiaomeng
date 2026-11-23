package com.omniroute.app.feature.profile

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omniroute.app.AppContainer
import com.omniroute.app.R
import com.omniroute.app.core.network.dto.UserDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/** 我的页面完整可观察状态。 */
internal data class ProfileUiState(
    val user: UserDto? = null,
    val loadingProfile: Boolean = true,
    val profileError: String? = null,
    val tripCount: Int = 0,
    val cityCount: Int? = null,
    val tripCountError: Boolean = false,
    val editing: Boolean = false,
    val nicknameDraft: String = "",
    val signatureDraft: String = "",
    val saving: Boolean = false,
    val saveError: String? = null,
    val loggingOut: Boolean = false,
    val confirmLogout: Boolean = false,
    val showChangePassword: Boolean = false,
    val changingPassword: Boolean = false,
    val changePasswordError: String? = null,
    val avatarUploading: Boolean = false,
    val avatarError: String? = null,
    /** 一次性操作提示（改密成功、头像更新），页面展示后自动清除。 */
    val notice: String? = null,
)

internal sealed interface ProfileEvent {
    /** 退出登录完成：页面据此回到登录页。 */
    data object LoggedOut : ProfileEvent
}

/**
 * 我的页面状态持有者。
 *
 * <p>弹窗开关与资料编辑草稿都放在这里，旋转屏幕不会丢；进程重建时草稿通过
 * [SavedStateHandle] 恢复，头像资料重新从仓库拉取。
 */
internal class ProfileViewModel(
    private val container: AppContainer,
    private val handle: SavedStateHandle,
    private val context: Context,
    initialUser: UserDto?,
) : ViewModel() {

    private val _state = MutableStateFlow(restoreState(initialUser))
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<ProfileEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<ProfileEvent> = _events.asSharedFlow()

    init {
        loadProfile()
        loadTripCount()
        viewModelScope.launch {
            _state.collect { persist(it) }
        }
    }

    fun loadProfile() {
        _state.update { it.copy(loadingProfile = true) }
        viewModelScope.launch {
            val outcome = withContext(Dispatchers.IO) {
                runCatching { container.authRepository.loadProfile() }
            }
            outcome.onSuccess { user ->
                _state.update { it.copy(user = user, profileError = null, loadingProfile = false) }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        profileError = error.message
                            ?: context.getString(R.string.profile_load_failed),
                        loadingProfile = false,
                    )
                }
            }
        }
    }

    fun loadTripCount() {
        viewModelScope.launch {
            val outcome = withContext(Dispatchers.IO) {
                runCatching { container.planningRepository.listTrips() }
            }
            outcome.onSuccess { trips ->
                val cityCount = trips.mapNotNull { trip -> trip.destCity?.trim()?.takeIf(String::isNotEmpty) }.distinct().size
                _state.update { it.copy(tripCount = trips.size, cityCount = cityCount, tripCountError = false) }
            }.onFailure {
                _state.update { it.copy(tripCountError = true) }
            }
        }
    }

    fun startEdit() {
        val user = _state.value.user ?: return
        _state.update {
            it.copy(
                editing = true,
                nicknameDraft = user.nickname.orEmpty(),
                signatureDraft = user.signature.orEmpty(),
                saveError = null,
            )
        }
    }

    fun onNicknameChange(value: String) = _state.update { it.copy(nicknameDraft = value.take(80)) }

    fun onSignatureChange(value: String) = _state.update { it.copy(signatureDraft = value.take(300)) }

    fun dismissEdit() {
        if (_state.value.saving) return
        _state.update { it.copy(editing = false, saveError = null) }
    }

    fun saveProfile() {
        val current = _state.value
        if (current.saving) return
        if (current.nicknameDraft.isBlank() && current.signatureDraft.isBlank()) return
        _state.update { it.copy(saving = true, saveError = null) }
        val nickname = current.nicknameDraft
        val signature = current.signatureDraft
        viewModelScope.launch {
            val outcome = withContext(Dispatchers.IO) {
                runCatching { container.authRepository.updateProfile(nickname, signature) }
            }
            outcome.onSuccess { user ->
                _state.update {
                    it.copy(user = user, editing = false, saving = false, saveError = null)
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        saving = false,
                        saveError = error.message ?: context.getString(R.string.profile_save_failed),
                    )
                }
            }
        }
    }

    fun requestLogout() = _state.update { it.copy(confirmLogout = true) }

    fun dismissLogout() {
        if (_state.value.loggingOut) return
        _state.update { it.copy(confirmLogout = false) }
    }

    fun openChangePassword() {
        _state.update { it.copy(showChangePassword = true, changePasswordError = null) }
    }

    fun dismissChangePassword() {
        if (_state.value.changingPassword) return
        _state.update { it.copy(showChangePassword = false, changePasswordError = null) }
    }

    /** 前端先做一致性校验，避免无意义请求；服务端仍是最终判定方。 */
    fun changePassword(current: String, newPassword: String, confirmPassword: String) {
        val snapshot = _state.value
        if (snapshot.changingPassword) return
        val validation = when {
            current.isBlank() || newPassword.isBlank() ->
                context.getString(R.string.auth_error_password_change_incomplete)
            newPassword != confirmPassword ->
                context.getString(R.string.auth_error_password_confirm_mismatch)
            newPassword.length < 6 || newPassword.length > 72 ->
                context.getString(R.string.auth_error_password_length)
            else -> null
        }
        if (validation != null) {
            _state.update { it.copy(changePasswordError = validation) }
            return
        }
        _state.update { it.copy(changingPassword = true, changePasswordError = null) }
        viewModelScope.launch {
            val outcome = withContext(Dispatchers.IO) {
                runCatching { container.authRepository.changePassword(current, newPassword) }
            }
            outcome.onSuccess { message ->
                _state.update {
                    it.copy(
                        changingPassword = false,
                        showChangePassword = false,
                        changePasswordError = null,
                        notice = message,
                    )
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        changingPassword = false,
                        changePasswordError = error.message
                            ?: context.getString(R.string.auth_error_password_change_incomplete),
                    )
                }
            }
        }
    }

    /** 头像先压到 1024px 内再上传，既控制流量也避开服务端 5MB 限制。 */
    fun uploadAvatar(uri: Uri) {
        if (_state.value.avatarUploading) return
        _state.update { it.copy(avatarUploading = true, avatarError = null) }
        viewModelScope.launch {
            val outcome = withContext(Dispatchers.IO) {
                runCatching {
                    val bytes = readCompressedAvatar(uri)
                        ?: throw IllegalStateException(context.getString(R.string.profile_avatar_pick_failed))
                    if (bytes.size > MAX_AVATAR_BYTES) {
                        throw IllegalStateException(context.getString(R.string.profile_avatar_too_large))
                    }
                    container.authRepository.uploadAvatar(bytes, "avatar.jpg", "image/jpeg")
                }
            }
            outcome.onSuccess { user ->
                _state.update {
                    it.copy(
                        user = user,
                        avatarUploading = false,
                        avatarError = null,
                        notice = context.getString(R.string.profile_avatar_uploaded),
                    )
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        avatarUploading = false,
                        avatarError = error.message
                            ?: context.getString(R.string.profile_avatar_failed),
                    )
                }
            }
        }
    }

    fun clearNotice() = _state.update { it.copy(notice = null) }

    fun clearAvatarError() = _state.update { it.copy(avatarError = null) }

    private fun readCompressedAvatar(uri: Uri): ByteArray? {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sample = 1
        while (bounds.outWidth / sample > MAX_AVATAR_EDGE || bounds.outHeight / sample > MAX_AVATAR_EDGE) {
            sample *= 2
        }
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        val bitmap = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        } ?: return null

        return ByteArrayOutputStream().use { output ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 88, output)
            bitmap.recycle()
            output.toByteArray()
        }
    }

    fun logout() {
        val current = _state.value
        if (current.loggingOut) return
        _state.update { it.copy(loggingOut = true) }
        viewModelScope.launch {
            withContext(Dispatchers.IO) { runCatching { container.authRepository.logout() } }
            _state.update { it.copy(loggingOut = false, confirmLogout = false) }
            _events.emit(ProfileEvent.LoggedOut)
        }
    }

    private fun restoreState(initialUser: UserDto?): ProfileUiState {
        return ProfileUiState(
            user = initialUser,
            editing = handle[KEY_EDITING] ?: false,
            nicknameDraft = handle[KEY_NICKNAME] ?: "",
            signatureDraft = handle[KEY_SIGNATURE] ?: "",
            confirmLogout = handle[KEY_CONFIRM_LOGOUT] ?: false,
            showChangePassword = handle[KEY_SHOW_CHANGE_PASSWORD] ?: false,
        )
    }

    private fun persist(state: ProfileUiState) {
        handle[KEY_EDITING] = state.editing
        handle[KEY_NICKNAME] = state.nicknameDraft
        handle[KEY_SIGNATURE] = state.signatureDraft
        handle[KEY_CONFIRM_LOGOUT] = state.confirmLogout
        handle[KEY_SHOW_CHANGE_PASSWORD] = state.showChangePassword
    }

    companion object {
        private const val KEY_EDITING = "profile_editing"
        private const val KEY_NICKNAME = "profile_nickname_draft"
        private const val KEY_SIGNATURE = "profile_signature_draft"
        private const val KEY_CONFIRM_LOGOUT = "profile_confirm_logout"
        private const val KEY_SHOW_CHANGE_PASSWORD = "profile_show_change_password"
        private const val MAX_AVATAR_EDGE = 1024
        private const val MAX_AVATAR_BYTES = 5 * 1024 * 1024
    }
}
