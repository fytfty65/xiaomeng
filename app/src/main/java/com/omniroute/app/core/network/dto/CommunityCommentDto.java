package com.omniroute.app.core.network.dto;

import com.google.gson.annotations.SerializedName;

/** 帖子评论；parent_id 非空表示是对某条评论的回复。 */
public final class CommunityCommentDto {

    @SerializedName("id")
    private String id;

    @SerializedName("post_id")
    private String postId;

    @SerializedName("user_id")
    private String userId;

    @SerializedName("author")
    private String author;

    @SerializedName("author_avatar_url")
    private String authorAvatarUrl;

    @SerializedName("content")
    private String content;

    @SerializedName("parent_id")
    private String parentId;

    @SerializedName("created_at")
    private String createdAt;

    public String getId() {
        return id;
    }

    public String getPostId() {
        return postId;
    }

    public String getUserId() {
        return userId;
    }

    public String getAuthor() {
        return author;
    }

    public String getAuthorAvatarUrl() {
        return authorAvatarUrl;
    }

    public String getContent() {
        return content;
    }

    public String getParentId() {
        return parentId;
    }

    public String getCreatedAt() {
        return createdAt;
    }
}
