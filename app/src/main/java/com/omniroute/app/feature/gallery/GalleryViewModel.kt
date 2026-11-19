package com.omniroute.app.feature.gallery

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.omniroute.app.AppContainer
import com.omniroute.app.R
import com.omniroute.app.core.network.dto.PhotoDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/** 相册列表项；DTO 只读，界面需要的派生字段在这里拼好。 */
data class GalleryPhotoUi(
    val id: String,
    val nodeName: String,
    val status: String,
    val rejectReason: String?,
    val createdAt: String,
)

data class GalleryUiState(
    val loading: Boolean = true,
    val enabled: Boolean = true,
    val photos: List<GalleryPhotoUi> = emptyList(),
    val error: String? = null,
    val uploading: Boolean = false,
    val uploadError: String? = null,
    val notice: String? = null,
    val pendingUri: Uri? = null,
    val viewer: GalleryPhotoUi? = null,
    val confirmDelete: GalleryPhotoUi? = null,
    val deleting: Boolean = false,
)

class GalleryViewModel(
    private val container: AppContainer,
    private val context: Context,
) : ViewModel() {

    private val _state = MutableStateFlow(GalleryUiState())
    val state: StateFlow<GalleryUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val outcome = withContext(Dispatchers.IO) {
                runCatching { container.photoRepository.photos() }
            }
            outcome.onSuccess { result ->
                _state.update {
                    it.copy(
                        loading = false,
                        enabled = result.enabled,
                        photos = result.photos.map { photo -> photo.toUi() },
                        error = null,
                    )
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        loading = false,
                        error = error.message ?: context.getString(R.string.photos_error_load),
                    )
                }
            }
        }
    }

    fun onPicked(uri: Uri?) {
        if (uri == null) return
        _state.update { it.copy(pendingUri = uri, uploadError = null) }
    }

    fun dismissUpload() {
        if (_state.value.uploading) return
        _state.update { it.copy(pendingUri = null, uploadError = null) }
    }

    /** nodeName 是后端用来分组的拍摄地点，允许留空。 */
    fun confirmUpload(nodeName: String) {
        val current = _state.value
        val uri = current.pendingUri ?: return
        if (current.uploading) return
        _state.update { it.copy(uploading = true, uploadError = null) }
        viewModelScope.launch {
            val outcome = withContext(Dispatchers.IO) {
                runCatching {
                    val bytes = readCompressedPhoto(uri)
                        ?: throw IllegalStateException(context.getString(R.string.photos_error_read))
                    container.photoRepository.upload(bytes, "photo.jpg", "image/jpeg", nodeName, "")
                }
            }
            outcome.onSuccess {
                _state.update {
                    it.copy(
                        uploading = false,
                        pendingUri = null,
                        uploadError = null,
                        notice = context.getString(R.string.photos_upload_pending),
                    )
                }
                refresh()
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        uploading = false,
                        uploadError = error.message ?: context.getString(R.string.photos_error_upload),
                    )
                }
            }
        }
    }

    fun openViewer(photo: GalleryPhotoUi) = _state.update { it.copy(viewer = photo) }

    fun closeViewer() = _state.update { it.copy(viewer = null) }

    fun requestDelete(photo: GalleryPhotoUi) = _state.update { it.copy(confirmDelete = photo) }

    fun cancelDelete() {
        if (_state.value.deleting) return
        _state.update { it.copy(confirmDelete = null) }
    }

    fun confirmDelete() {
        val photo = _state.value.confirmDelete ?: return
        if (_state.value.deleting) return
        _state.update { it.copy(deleting = true) }
        viewModelScope.launch {
            val outcome = withContext(Dispatchers.IO) {
                runCatching { container.photoRepository.delete(photo.id) }
            }
            outcome.onSuccess {
                _state.update { current ->
                    current.copy(
                        deleting = false,
                        confirmDelete = null,
                        viewer = current.viewer?.takeIf { it.id != photo.id },
                        notice = context.getString(R.string.photos_delete_success),
                    )
                }
                refresh()
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        deleting = false,
                        confirmDelete = null,
                        notice = error.message ?: context.getString(R.string.photos_error_delete),
                    )
                }
            }
        }
    }

    fun clearNotice() = _state.update { it.copy(notice = null) }

    fun clearUploadError() = _state.update { it.copy(uploadError = null) }

    /** 供缩略图懒加载；失败返回 null，界面显示占位而不是崩溃。 */
    suspend fun loadImage(id: String): ByteArray? = withContext(Dispatchers.IO) {
        runCatching { container.photoRepository.load(id) }.getOrNull()
    }

    /** 采样到最长边 1600（与网关规范化口径一致），JPEG 85。 */
    private fun readCompressedPhoto(uri: Uri): ByteArray? {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sample = 1
        while (bounds.outWidth / sample > MAX_EDGE || bounds.outHeight / sample > MAX_EDGE) {
            sample *= 2
        }
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        val bitmap: Bitmap = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        } ?: return null

        return ByteArrayOutputStream().use { output ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, output)
            bitmap.recycle()
            output.toByteArray()
        }
    }

    companion object {
        private const val MAX_EDGE = 1600

        fun factory(container: AppContainer, context: Context): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    GalleryViewModel(container, context.applicationContext)
                }
            }
    }
}

private fun PhotoDto.toUi(): GalleryPhotoUi {
    return GalleryPhotoUi(
        id = id.orEmpty(),
        nodeName = nodeName.orEmpty(),
        status = status.orEmpty(),
        rejectReason = rejectReason?.takeIf { it.isNotBlank() },
        createdAt = createdAt.orEmpty(),
    )
}
