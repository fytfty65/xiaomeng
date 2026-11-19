package com.omniroute.app.feature.community

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.omniroute.app.AppContainer
import com.omniroute.app.R
import com.omniroute.app.core.AppStrings
import com.omniroute.app.core.network.dto.CommunityCommentDto
import com.omniroute.app.core.network.dto.CommunityPostDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** 帖子在界面上的可变视图；DTO 字段不可写，互动结果在这里合并。 */
data class CommunityPostUi(
    val id: String,
    val author: String,
    val avatarUrl: String?,
    val title: String,
    val content: String,
    val city: String,
    val tags: List<String>,
    val likes: Int,
    val commentCount: Int,
    val favorites: Int,
    val favorited: Boolean,
    val createdAt: String,
)

data class CommunityUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val posts: List<CommunityPostUi> = emptyList(),
    val tags: List<String> = emptyList(),
    val selectedTag: String = "",
    val likedIds: Set<String> = emptySet(),
    val busyIds: Set<String> = emptySet(),
    val selectedPost: CommunityPostUi? = null,
    val comments: List<CommunityCommentDto> = emptyList(),
    val commentsLoading: Boolean = false,
    val detailError: String? = null,
    val commentSending: Boolean = false,
    val publishing: Boolean = false,
    val publishOpen: Boolean = false,
    val publishError: String? = null,
    val notice: String? = null,
)

class CommunityViewModel(
    private val container: AppContainer,
    private val context: Context,
) : ViewModel() {

    private val _state = MutableStateFlow(CommunityUiState())
    val state: StateFlow<CommunityUiState> = _state.asStateFlow()

    private var tagsLoaded = false

    init {
        loadFeed()
    }

    fun loadFeed() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val tag = _state.value.selectedTag
            val outcome = withContext(Dispatchers.IO) {
                runCatching {
                    val posts = container.communityRepository.feed(tag).map { it.toUi() }
                    val tags = if (tagsLoaded) null else container.communityRepository.tags()
                    posts to tags
                }
            }
            outcome.onSuccess { (posts, tags) ->
                if (tags != null) tagsLoaded = true
                _state.update { current ->
                    current.copy(
                        loading = false,
                        error = null,
                        posts = posts,
                        tags = tags ?: current.tags,
                    )
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        loading = false,
                        error = error.message ?: context.getString(R.string.community_error_load),
                    )
                }
            }
        }
    }

    fun selectTag(tag: String) {
        if (_state.value.selectedTag == tag) return
        _state.update { it.copy(selectedTag = tag) }
        loadFeed()
    }

    fun openPost(post: CommunityPostUi) {
        _state.update {
            it.copy(
                selectedPost = post,
                comments = emptyList(),
                detailError = null,
                commentsLoading = true,
            )
        }
        loadComments(post.id)
    }

    fun closePost() {
        _state.update {
            it.copy(
                selectedPost = null,
                comments = emptyList(),
                detailError = null,
                commentsLoading = false,
            )
        }
    }

    private fun loadComments(postId: String) {
        viewModelScope.launch {
            val outcome = withContext(Dispatchers.IO) {
                runCatching { container.communityRepository.comments(postId) }
            }
            outcome.onSuccess { comments ->
                _state.update { it.copy(commentsLoading = false, comments = comments) }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        commentsLoading = false,
                        detailError = error.message ?: context.getString(R.string.community_error_load),
                    )
                }
            }
        }
    }

    fun sendComment(text: String) {
        val post = _state.value.selectedPost ?: return
        if (_state.value.commentSending || text.trim().isEmpty()) return
        _state.update { it.copy(commentSending = true, detailError = null) }
        viewModelScope.launch {
            val outcome = withContext(Dispatchers.IO) {
                runCatching { container.communityRepository.comment(post.id, text, "") }
            }
            outcome.onSuccess {
                _state.update { current ->
                    current.copy(
                        commentSending = false,
                        selectedPost = current.selectedPost?.copy(
                            commentCount = current.selectedPost.commentCount + 1,
                        ),
                        posts = current.posts.map {
                            if (it.id == post.id) it.copy(commentCount = it.commentCount + 1) else it
                        },
                    )
                }
                loadComments(post.id)
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        commentSending = false,
                        detailError = error.message ?: context.getString(R.string.community_error_comment),
                    )
                }
            }
        }
    }

    fun toggleLike(post: CommunityPostUi) {
        if (post.id in _state.value.likedIds || post.id in _state.value.busyIds) return
        _state.update { it.copy(busyIds = it.busyIds + post.id) }
        viewModelScope.launch {
            val outcome = withContext(Dispatchers.IO) {
                runCatching { container.communityRepository.like(post.id) }
            }
            outcome.onSuccess { likes ->
                _state.update { current ->
                    current.copy(
                        busyIds = current.busyIds - post.id,
                        likedIds = current.likedIds + post.id,
                        posts = current.posts.map {
                            if (it.id == post.id) it.copy(likes = likes.coerceAtLeast(it.likes)) else it
                        },
                        selectedPost = current.selectedPost?.let {
                            if (it.id == post.id) it.copy(likes = likes.coerceAtLeast(it.likes)) else it
                        },
                    )
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        busyIds = it.busyIds - post.id,
                        notice = error.message ?: context.getString(R.string.community_error_action),
                    )
                }
            }
        }
    }

    fun toggleFavorite(post: CommunityPostUi) {
        if (post.id in _state.value.busyIds) return
        _state.update { it.copy(busyIds = it.busyIds + post.id) }
        viewModelScope.launch {
            val outcome = withContext(Dispatchers.IO) {
                runCatching { container.communityRepository.favorite(post.id) }
            }
            outcome.onSuccess { result ->
                _state.update { current ->
                    current.copy(
                        busyIds = current.busyIds - post.id,
                        posts = current.posts.map {
                            if (it.id == post.id) {
                                it.copy(favorited = result.favorited, favorites = result.favorites)
                            } else {
                                it
                            }
                        },
                        selectedPost = current.selectedPost?.let {
                            if (it.id == post.id) {
                                it.copy(favorited = result.favorited, favorites = result.favorites)
                            } else {
                                it
                            }
                        },
                    )
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        busyIds = it.busyIds - post.id,
                        notice = error.message ?: context.getString(R.string.community_error_action),
                    )
                }
            }
        }
    }

    fun openPublish() = _state.update { it.copy(publishOpen = true, publishError = null) }

    fun dismissPublish() {
        if (_state.value.publishing) return
        _state.update { it.copy(publishOpen = false, publishError = null) }
    }

    fun publish(title: String, content: String, city: String, tagsRaw: String) {
        if (_state.value.publishing) return
        _state.update { it.copy(publishing = true, publishError = null) }
        val tags = tagsRaw
            .split(',', '，', ' ', '#')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .take(8)
        viewModelScope.launch {
            val outcome = withContext(Dispatchers.IO) {
                runCatching { container.communityRepository.publish(title, content, city, tags) }
            }
            outcome.onSuccess {
                _state.update {
                    it.copy(
                        publishing = false,
                        publishOpen = false,
                        publishError = null,
                        notice = context.getString(R.string.community_publish_success),
                    )
                }
                tagsLoaded = false
                loadFeed()
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        publishing = false,
                        publishError = error.message ?: context.getString(R.string.community_error_publish),
                    )
                }
            }
        }
    }

    fun clearNotice() = _state.update { it.copy(notice = null) }

    fun clearDetailError() = _state.update { it.copy(detailError = null) }

    companion object {
        fun factory(container: AppContainer, context: Context): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    CommunityViewModel(container, context.applicationContext)
                }
            }
    }
}

private fun CommunityPostDto.toUi(): CommunityPostUi {
    return CommunityPostUi(
        id = id.orEmpty(),
        author = author?.takeIf { it.isNotBlank() }
            ?: AppStrings.get(R.string.community_anonymous),
        avatarUrl = authorAvatarUrl,
        title = title.orEmpty(),
        content = content.orEmpty(),
        city = destCity.orEmpty(),
        tags = tags,
        likes = likes,
        commentCount = comments,
        favorites = favorites,
        favorited = isFavorited,
        createdAt = createdAt.orEmpty(),
    )
}
