package com.omniroute.app.feature.gallery

import com.omniroute.app.core.ui.theme.FolioSerif
import com.omniroute.app.core.ui.theme.FolioDigits
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import com.omniroute.app.core.ui.SystemBarIcons
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontFamily
import com.omniroute.app.core.ui.components.SeasonArtwork
import com.omniroute.app.core.ui.theme.LocalJourneySeason
import com.omniroute.app.core.ui.theme.edition
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import com.omniroute.app.core.ui.theme.seasonBackdrop
import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.omniroute.app.AppContainer
import com.omniroute.app.R
import com.omniroute.app.core.ui.components.CircleIconButton
import com.omniroute.app.core.ui.theme.LocalAppPalette
import kotlinx.coroutines.delay

/** 我的相册：上传实拍、查看审核状态、删除自己的照片。 */
@Composable
fun GalleryScreen(
    container: AppContainer,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val viewModel: GalleryViewModel = viewModel(
        factory = GalleryViewModel.factory(container, context),
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri -> viewModel.onPicked(uri) }

    LaunchedEffect(state.notice) {
        if (state.notice != null) {
            delay(3_500)
            viewModel.clearNotice()
        }
    }

    val viewer = state.viewer
    if (viewer != null) {
        BackHandler { viewModel.closeViewer() }
        PhotoViewer(
            photo = viewer,
            viewModel = viewModel,
            onClose = viewModel::closeViewer,
            onRequestDelete = viewModel::requestDelete,
        )
    } else {
        GalleryContent(
            state = state,
            viewModel = viewModel,
            onBack = onBack,
            onPick = {
                picker.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                )
            },
        )
    }

    state.pendingUri?.let { _ ->
        UploadDialog(
            uploading = state.uploading,
            error = state.uploadError,
            onDismiss = viewModel::dismissUpload,
            onConfirm = viewModel::confirmUpload,
        )
    }

    state.confirmDelete?.let { photo ->
        AlertDialog(
            onDismissRequest = viewModel::cancelDelete,
            title = { Text(text = stringResource(R.string.gallery_delete_title), fontSize = 16.sp) },
            text = {
                Text(
                    text = stringResource(R.string.gallery_delete_message),
                    fontSize = 13.sp,
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !state.deleting,
                    onClick = { viewModel.confirmDelete() },
                ) {
                    Text(
                        text = stringResource(
                            if (state.deleting) R.string.gallery_deleting else R.string.gallery_delete,
                        ),
                        color = LocalAppPalette.current.errorFg,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelDelete) {
                    Text(text = stringResource(R.string.common_cancel))
                }
            },
        )
    }
}

@Composable
private fun GalleryContent(
    state: GalleryUiState,
    viewModel: GalleryViewModel,
    onBack: () -> Unit,
    onPick: () -> Unit,
) {
    val palette = LocalAppPalette.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .seasonBackdrop()
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        Row(
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircleIconButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.common_back),
                onClick = onBack,
                background = palette.surface,
                borderColor = palette.border,
                tint = palette.ink,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.gallery_title),
                    fontFamily = FolioSerif, fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.ink,
                )
                Text(
                    text = "${LocalJourneySeason.current.name.uppercase()}   ROLL   /   ${"%02d".format(state.photos.size)} FRAMES",
                    fontFamily = FolioDigits, letterSpacing = 1.3.sp,
                    fontSize = 12.sp,
                    color = palette.muted,
                )
            }
            if (state.enabled && state.photos.isNotEmpty()) {
                AddButton(onClick = onPick)
            }
        }

        state.notice?.let { message ->
            Banner(
                text = message,
                background = palette.brandTint,
                foreground = palette.brand,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            )
        }

        if (!state.enabled) {
            Banner(
                text = stringResource(R.string.gallery_disabled),
                background = palette.warningBg,
                foreground = palette.warningFg,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            )
        }

        state.error?.let { message ->
            Banner(
                text = message,
                background = palette.errorBg,
                foreground = palette.errorFg,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            )
            TextButton(onClick = viewModel::refresh, modifier = Modifier.fillMaxWidth()) {
                Text(text = stringResource(R.string.journey_retry), color = palette.brand)
            }
        }

        if (state.loading && state.photos.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = palette.brand)
            }
        } else if (state.photos.isEmpty()) {
            GalleryEmpty(enabled = state.enabled, onPick = onPick)
        } else {
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Adaptive(140.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 30.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalItemSpacing = 14.dp,
            ) {
                items(state.photos, key = { it.id }) { photo ->
                    PhotoCell(
                        photo = photo,
                        viewModel = viewModel,
                        onClick = { viewModel.openViewer(photo) },
                    )
                }
            }
        }
    }
}

@Composable
private fun GalleryEmpty(enabled: Boolean, onPick: () -> Unit) {
    val palette = LocalAppPalette.current
    val style = LocalJourneySeason.current.edition
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        repeat(3) { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                repeat(2) { col ->
                    val index = row * 2 + col
                    val border = palette.ink.copy(alpha = 0.24f)
                    Box(Modifier.weight(1f).aspectRatio(0.98f).drawBehind {
                        drawRect(border, style = Stroke(1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx()))))
                    }.then(if (index == 0 && enabled) Modifier.clickable(role = Role.Button, onClick = onPick) else Modifier)) {
                        if (index == 0) {
                            SeasonArtwork(Modifier.matchParentSize(), cover = true)
                            Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xB8151A1D)))))
                            Text(if (enabled) "＋ 上传第一张实拍" else "第一帧旅途光影", color = Color.White, fontFamily = FolioSerif,
                                fontSize = 17.sp, modifier = Modifier.align(Alignment.BottomStart).padding(12.dp))
                        } else if (index == 1) {
                            Text("把一路见到的风景，\n留在这里。", color = palette.muted, fontFamily = FolioSerif,
                                fontSize = 16.sp, lineHeight = 26.sp, modifier = Modifier.align(Alignment.Center).padding(12.dp))
                        }
                        Text("%02d".format(index + 1), fontFamily = FolioDigits, fontSize = 15.sp,
                            color = if (index == 0) Color.White else palette.muted,
                            modifier = Modifier.align(Alignment.TopStart).padding(10.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun PhotoCell(
    photo: GalleryPhotoUi,
    viewModel: GalleryViewModel,
    onClick: () -> Unit,
) {
    val palette = LocalAppPalette.current
    Column(Modifier.clip(RoundedCornerShape(7.dp)).background(palette.surface).padding(5.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(if (photo.id.hashCode() and 1 == 0) 0.82f else 1.13f)
                .clip(RoundedCornerShape(12.dp))
                .background(palette.fieldBg)
                .clickable(role = Role.Button, onClick = onClick),
        ) {
            val image by produceState<androidx.compose.ui.graphics.ImageBitmap?>(
                initialValue = null,
                photo.id,
            ) {
                value = viewModel.loadImage(photo.id)?.let { bytes ->
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                }
            }
            val bitmap = image
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = photo.nodeName.ifBlank {
                        stringResource(R.string.gallery_untitled)
                    },
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        color = palette.muted,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            StatusBadge(
                status = photo.status,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp),
            )
        }
        Spacer(modifier = Modifier.height(5.dp))
        Text(
            text = photo.nodeName.ifBlank { stringResource(R.string.gallery_untitled) },
            fontSize = 11.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = palette.ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        val date = photo.createdAt.take(10)
        if (date.isNotBlank()) {
            Text(date, fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = palette.muted,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 3.dp))
        }
        if (photo.status == STATUS_REJECTED && photo.rejectReason != null) {
            Text(
                text = stringResource(R.string.gallery_reject_reason, photo.rejectReason),
                fontSize = 10.sp,
                color = palette.errorFg,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun StatusBadge(status: String, modifier: Modifier = Modifier) {
    val palette = LocalAppPalette.current
    val (background, foreground) = when (status) {
        STATUS_APPROVED -> palette.verifiedBg to palette.verifiedFg
        STATUS_REJECTED -> palette.errorBg to palette.errorFg
        else -> palette.warningBg to palette.warningFg
    }
    Surface(
        shape = RoundedCornerShape(50),
        color = background,
        modifier = modifier,
    ) {
        Text(
            text = stringResource(
                when (status) {
                    STATUS_APPROVED -> R.string.gallery_status_approved
                    STATUS_REJECTED -> R.string.gallery_status_rejected
                    else -> R.string.gallery_status_pending
                },
            ),
            fontSize = 9.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = foreground,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
        )
    }
}

@Composable
private fun PhotoViewer(
    photo: GalleryPhotoUi,
    viewModel: GalleryViewModel,
    onClose: () -> Unit,
    onRequestDelete: (GalleryPhotoUi) -> Unit,
) {
    SystemBarIcons(darkIcons = false)
    val palette = LocalAppPalette.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.radialGradient(listOf(LocalJourneySeason.current.edition.deep, Color(0xFF111218)))),
    ) {
        val image by produceState<androidx.compose.ui.graphics.ImageBitmap?>(
            initialValue = null,
            photo.id,
        ) {
            value = viewModel.loadImage(photo.id)?.let { bytes ->
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            }
        }
        val bitmap = image
        if (bitmap != null) {
            if (android.os.Build.VERSION.SDK_INT >= 31) {
                Image(bitmap, null, modifier = Modifier.fillMaxSize().blur(28.dp).alpha(0.22f), contentScale = ContentScale.Crop)
            }
            Image(
                bitmap = bitmap,
                contentDescription = photo.nodeName.ifBlank {
                    stringResource(R.string.gallery_untitled)
                },
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color.White)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircleIconButton(
                icon = Icons.Filled.Close,
                contentDescription = stringResource(R.string.gallery_close),
                onClick = onClose,
                background = Color(0x66000000),
                borderColor = Color(0x33FFFFFF),
                tint = Color.White,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = photo.nodeName.ifBlank { stringResource(R.string.gallery_untitled) },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = photo.createdAt.take(10),
                    fontSize = 11.sp,
                    color = Color(0xB3FFFFFF),
                )
            }
            Text(
                text = stringResource(R.string.gallery_delete),
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFFF9E80),
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable(role = Role.Button) { onRequestDelete(photo) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }

        if (photo.status == STATUS_REJECTED && photo.rejectReason != null) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = palette.errorBg,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(20.dp),
            ) {
                Text(
                    text = stringResource(R.string.gallery_reject_reason, photo.rejectReason),
                    fontSize = 12.sp,
                    color = palette.errorFg,
                    modifier = Modifier.padding(12.dp),
                )
            }
        }
    }
}

@Composable
private fun UploadDialog(
    uploading: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var nodeName by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = { if (!uploading) onDismiss() },
        title = { Text(text = stringResource(R.string.gallery_upload_title), fontSize = 16.sp) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.gallery_upload_hint),
                    fontSize = 12.sp,
                    color = LocalAppPalette.current.muted,
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = nodeName,
                    onValueChange = { nodeName = it.take(40) },
                    singleLine = true,
                    label = { Text(text = stringResource(R.string.gallery_upload_node)) },
                    enabled = !uploading,
                )
                if (error != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = error, fontSize = 12.sp, color = LocalAppPalette.current.errorFg)
                }
            }
        },
        confirmButton = {
            TextButton(enabled = !uploading, onClick = { onConfirm(nodeName) }) {
                Text(
                    text = stringResource(
                        if (uploading) R.string.gallery_uploading else R.string.gallery_upload_confirm,
                    ),
                    color = LocalAppPalette.current.brand,
                )
            }
        },
        dismissButton = {
            TextButton(enabled = !uploading, onClick = onDismiss) {
                Text(text = stringResource(R.string.common_cancel))
            }
        },
    )
}

@Composable
private fun AddButton(onClick: () -> Unit) {
    val palette = LocalAppPalette.current
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(palette.brand)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = null,
            tint = palette.onBrand,
            modifier = Modifier.size(14.dp),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = stringResource(R.string.gallery_add),
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Bold,
            color = palette.onBrand,
        )
    }
}

@Composable
private fun Banner(
    text: String,
    background: Color,
    foreground: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = background,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = null,
                tint = foreground,
                modifier = Modifier.size(15.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = text, fontSize = 12.sp, color = foreground)
        }
    }
}

private const val STATUS_APPROVED = "approved"
private const val STATUS_REJECTED = "rejected"
