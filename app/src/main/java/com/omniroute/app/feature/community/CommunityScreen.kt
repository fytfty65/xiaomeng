package com.omniroute.app.feature.community

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
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
import androidx.compose.runtime.remember
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.omniroute.app.AppContainer
import com.omniroute.app.R
import com.omniroute.app.core.network.dto.CommunityCommentDto
import com.omniroute.app.core.ui.components.CircleIconButton
import com.omniroute.app.core.ui.components.RemoteAvatar
import com.omniroute.app.core.ui.theme.LocalAppPalette
import com.omniroute.app.core.ui.wheelScroll
import kotlinx.coroutines.delay

@Composable
fun CommunityScreen(
    container: AppContainer,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val viewModel: CommunityViewModel = viewModel(
        factory = CommunityViewModel.factory(container, context),
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.notice) {
        if (state.notice != null) {
            delay(3_500)
            viewModel.clearNotice()
        }
    }

    if (state.selectedPost != null) {
        BackHandler { viewModel.closePost() }
        PostDetail(
            state = state,
            onBack = viewModel::closePost,
            onLike = viewModel::toggleLike,
            onFavorite = viewModel::toggleFavorite,
            onSendComment = viewModel::sendComment,
        )
    } else {
        Feed(
            state = state,
            onBack = onBack,
            onRefresh = viewModel::loadFeed,
            onSelectTag = viewModel::selectTag,
            onOpenPost = viewModel::openPost,
            onLike = viewModel::toggleLike,
            onFavorite = viewModel::toggleFavorite,
            onPublish = viewModel::openPublish,
        )
    }

    if (state.publishOpen) {
        PublishDialog(
            publishing = state.publishing,
            error = state.publishError,
            onDismiss = viewModel::dismissPublish,
            onPublish = viewModel::publish,
        )
    }
}

@Composable
private fun Feed(
    state: CommunityUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onSelectTag: (String) -> Unit,
    onOpenPost: (CommunityPostUi) -> Unit,
    onLike: (CommunityPostUi) -> Unit,
    onFavorite: (CommunityPostUi) -> Unit,
    onPublish: () -> Unit,
) {
    val palette = LocalAppPalette.current
    val listState = rememberLazyListState()
    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .wheelScroll(listState),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 14.dp,
            bottom = 30.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "header") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircleIconButton(
                    icon = Icons.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.common_back),
                    onClick = onBack,
                    background = palette.surface,
                    borderColor = palette.border,
                    tint = palette.ink,
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.community_title),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.ink,
                    )
                    Text(
                        text = stringResource(R.string.community_sub),
                        fontSize = 12.sp,
                        color = palette.muted,
                    )
                }
                PublishButton(onClick = onPublish)
            }
        }

        state.notice?.let { message ->
            item(key = "notice") {
                SimpleBanner(
                    text = message,
                    background = palette.brandTint,
                    foreground = palette.brand,
                )
            }
        }

        state.error?.let { message ->
            item(key = "error") {
                SimpleBanner(
                    text = message,
                    background = palette.errorBg,
                    foreground = palette.errorFg,
                )
            }
            item(key = "retry") {
                TextButton(onClick = onRefresh, modifier = Modifier.fillMaxWidth()) {
                    Text(text = stringResource(R.string.journey_retry), color = palette.brand)
                }
            }
        }

        if (state.tags.isNotEmpty()) {
            item(key = "tags") {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item(key = "tag_all") {
                        TagChip(
                            text = stringResource(R.string.community_all_tags),
                            active = state.selectedTag.isBlank(),
                            onClick = { onSelectTag("") },
                        )
                    }
                    items(state.tags, key = { "tag_$it" }) { tag ->
                        TagChip(
                            text = tag,
                            active = state.selectedTag == tag,
                            onClick = { onSelectTag(tag) },
                        )
                    }
                }
            }
        }

        if (state.loading) {
            item(key = "loading") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 60.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = palette.brand)
                }
            }
        } else if (state.posts.isEmpty() && state.error == null) {
            item(key = "empty") {
                Text(
                    text = stringResource(R.string.community_empty),
                    fontSize = 13.sp,
                    color = palette.muted,
                    modifier = Modifier.padding(vertical = 40.dp),
                )
            }
        }

        items(state.posts, key = { "post_${it.id}" }) { post ->
            PostCard(
                post = post,
                liked = post.id in state.likedIds,
                busy = post.id in state.busyIds,
                onClick = { onOpenPost(post) },
                onLike = { onLike(post) },
                onFavorite = { onFavorite(post) },
            )
        }
    }
}

@Composable
private fun PostCard(
    post: CommunityPostUi,
    liked: Boolean,
    busy: Boolean,
    onClick: () -> Unit,
    onLike: () -> Unit,
    onFavorite: () -> Unit,
) {
    val palette = LocalAppPalette.current
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = palette.surface,
        border = BorderStroke(1.dp, palette.border),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick),
    ) {
        Column(modifier = Modifier.padding(15.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RemoteAvatar(
                    path = post.avatarUrl,
                    fallback = post.author.take(1).uppercase(),
                    size = 34.dp,
                    background = palette.brandTint,
                    foreground = palette.brand,
                    fontSize = 14.sp,
                    contentDescription = null,
                )
                Spacer(modifier = Modifier.width(9.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = post.author,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.ink,
                    )
                    Text(
                        text = listOf(post.city, post.createdAt.take(10))
                            .filter { it.isNotBlank() }
                            .joinToString(" · "),
                        fontSize = 10.5.sp,
                        color = palette.muted,
                    )
                }
            }

            if (post.title.isNotBlank()) {
                Spacer(modifier = Modifier.height(9.dp))
                Text(
                    text = post.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.ink,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (post.content.isNotBlank()) {
                Spacer(modifier = Modifier.height(5.dp))
                Text(
                    text = post.content,
                    fontSize = 12.5.sp,
                    color = palette.ink.copy(alpha = 0.82f),
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (post.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(9.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    post.tags.take(4).forEach { tag ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(palette.tagBg)
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                        ) {
                            Text(text = "#$tag", fontSize = 10.sp, color = palette.muted)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                CountAction(
                    text = stringResource(R.string.community_like),
                    count = post.likes,
                    active = liked,
                    enabled = !busy,
                    onClick = onLike,
                )
                CountAction(
                    text = stringResource(
                        if (post.favorited) R.string.community_favorited
                        else R.string.community_favorite,
                    ),
                    count = post.favorites,
                    active = post.favorited,
                    enabled = !busy,
                    onClick = onFavorite,
                )
                CountAction(
                    text = stringResource(R.string.community_comments),
                    count = post.commentCount,
                    active = false,
                    enabled = true,
                    onClick = onClick,
                )
            }
        }
    }
}

@Composable
private fun PostDetail(
    state: CommunityUiState,
    onBack: () -> Unit,
    onLike: (CommunityPostUi) -> Unit,
    onFavorite: (CommunityPostUi) -> Unit,
    onSendComment: (String) -> Unit,
) {
    val palette = LocalAppPalette.current
    val post = state.selectedPost ?: return
    var draft by rememberSaveable(post.id) { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircleIconButton(
                icon = Icons.Filled.ArrowBack,
                contentDescription = stringResource(R.string.common_back),
                onClick = onBack,
                background = palette.surface,
                borderColor = palette.border,
                tint = palette.ink,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.community_detail),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = palette.ink,
            )
        }

        val detailListState = rememberLazyListState()
        LazyColumn(
            state = detailListState,
            modifier = Modifier
                .weight(1f)
                .wheelScroll(detailListState),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 20.dp,
                end = 20.dp,
                bottom = 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(key = "post") {
                PostCard(
                    post = post,
                    liked = post.id in state.likedIds,
                    busy = post.id in state.busyIds,
                    onClick = {},
                    onLike = { onLike(post) },
                    onFavorite = { onFavorite(post) },
                )
            }
            item(key = "comments_title") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.community_comments),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.ink,
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = stringResource(R.string.community_comment_count, post.commentCount),
                        fontSize = 11.sp,
                        color = palette.muted,
                    )
                }
            }
            state.detailError?.let { message ->
                item(key = "detail_error") {
                    SimpleBanner(
                        text = message,
                        background = palette.errorBg,
                        foreground = palette.errorFg,
                    )
                }
            }
            if (state.commentsLoading) {
                item(key = "comments_loading") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = palette.brand,
                        )
                    }
                }
            } else if (state.comments.isEmpty()) {
                item(key = "comments_empty") {
                    Text(
                        text = stringResource(R.string.community_comments_empty),
                        fontSize = 12.5.sp,
                        color = palette.muted,
                        modifier = Modifier.padding(vertical = 18.dp),
                    )
                }
            } else {
                items(state.comments, key = { "comment_${it.id}" }) { comment ->
                    CommentRow(comment)
                }
            }
        }

        HorizontalDivider(color = palette.border)
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                placeholder = {
                    Text(
                        text = stringResource(R.string.community_comment_hint),
                        fontSize = 12.5.sp,
                        color = palette.muted,
                    )
                },
                enabled = !state.commentSending,
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (draft.isBlank()) palette.brandTint else palette.brand)
                    .clickable(
                        enabled = draft.isNotBlank() && !state.commentSending,
                        role = Role.Button,
                    ) {
                        val text = draft
                        draft = ""
                        onSendComment(text)
                    }
                    .padding(11.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Send,
                    contentDescription = stringResource(R.string.community_comment_send),
                    tint = if (draft.isBlank()) palette.brand else palette.onBrand,
                    modifier = Modifier.size(17.dp),
                )
            }
        }
    }
}

@Composable
private fun CommentRow(comment: CommunityCommentDto) {
    val palette = LocalAppPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.Top,
    ) {
        RemoteAvatar(
            path = comment.authorAvatarUrl,
            fallback = (comment.author ?: "U").take(1).uppercase(),
            size = 28.dp,
            background = palette.fieldBg,
            foreground = palette.muted,
            fontSize = 11.sp,
            contentDescription = null,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = comment.author ?: "",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = palette.ink,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = comment.createdAt.orEmpty().take(10),
                    fontSize = 10.sp,
                    color = palette.muted,
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = comment.content.orEmpty(),
                fontSize = 12.5.sp,
                color = palette.ink.copy(alpha = 0.85f),
            )
        }
    }
}

@Composable
private fun PublishDialog(
    publishing: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onPublish: (title: String, content: String, city: String, tags: String) -> Unit,
) {
    val palette = LocalAppPalette.current
    var title by rememberSaveable { mutableStateOf("") }
    var content by rememberSaveable { mutableStateOf("") }
    var city by rememberSaveable { mutableStateOf("") }
    var tags by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.community_publish),
                fontSize = 16.sp,
                color = palette.ink,
            )
        },
        text = {
            val publishScrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .height(320.dp)
                    .verticalScroll(publishScrollState)
                    .wheelScroll(publishScrollState),
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(text = stringResource(R.string.community_publish_title), fontSize = 12.sp) },
                    singleLine = true,
                    enabled = !publishing,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text(text = stringResource(R.string.community_publish_content), fontSize = 12.sp) },
                    minLines = 4,
                    maxLines = 7,
                    enabled = !publishing,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = city,
                    onValueChange = { city = it },
                    label = { Text(text = stringResource(R.string.community_publish_city), fontSize = 12.sp) },
                    singleLine = true,
                    enabled = !publishing,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = tags,
                    onValueChange = { tags = it },
                    label = { Text(text = stringResource(R.string.community_publish_tags), fontSize = 12.sp) },
                    singleLine = true,
                    enabled = !publishing,
                    modifier = Modifier.fillMaxWidth(),
                )
                error?.let {
                    Spacer(modifier = Modifier.height(10.dp))
                    SimpleBanner(
                        text = it,
                        background = palette.errorBg,
                        foreground = palette.errorFg,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !publishing && (title.isNotBlank() || content.isNotBlank()),
                onClick = { onPublish(title, content, city, tags) },
            ) {
                Text(
                    text = stringResource(
                        if (publishing) R.string.community_publishing
                        else R.string.community_publish_action,
                    ),
                    color = palette.brand,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !publishing) {
                Text(text = stringResource(R.string.common_cancel), color = palette.muted)
            }
        },
    )
}

@Composable
private fun PublishButton(onClick: () -> Unit) {
    val palette = LocalAppPalette.current
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(palette.brand)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(
            text = stringResource(R.string.community_publish),
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Bold,
            color = palette.onBrand,
        )
    }
}

@Composable
private fun TagChip(text: String, active: Boolean, onClick: () -> Unit) {
    val palette = LocalAppPalette.current
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (active) palette.brand else palette.surface)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
    ) {
        Text(
            text = if (text.startsWith("#")) text else "#$text",
            fontSize = 11.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (active) palette.onBrand else palette.muted,
        )
    }
}

@Composable
private fun CountAction(
    text: String,
    count: Int,
    active: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val palette = LocalAppPalette.current
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "$text $count",
            fontSize = 11.5.sp,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
            color = if (active) palette.brand else palette.muted,
        )
    }
}

@Composable
private fun SimpleBanner(text: String, background: Color, foreground: Color) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = background,
        modifier = Modifier.fillMaxWidth(),
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
