package com.omniroute.app.core.network.dto;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/** 社区帖子（列表与详情共用同一形状）。 */
public final class CommunityPostDto {

    @SerializedName("id")
    private String id;

    @SerializedName("user_id")
    private String userId;

    @SerializedName("author")
    private String author;

    @SerializedName("author_avatar_url")
    private String authorAvatarUrl;

    @SerializedName("title")
    private String title;

    @SerializedName("content")
    private String content;

    @SerializedName("dest_city")
    private String destCity;

    @SerializedName("tags")
    private List<String> tags;

    @SerializedName("likes")
    private int likes;

    @SerializedName("comments")
    private int comments;

    @SerializedName("favorites")
    private int favorites;

    @SerializedName("favorited")
    private boolean favorited;

    @SerializedName("created_at")
    private String createdAt;

    public String getId() {
        return id;
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

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public String getDestCity() {
        return destCity;
    }

    public List<String> getTags() {
        return tags == null ? new ArrayList<>() : tags;
    }

    public int getLikes() {
        return likes;
    }

    public int getComments() {
        return comments;
    }

    public int getFavorites() {
        return favorites;
    }

    public boolean isFavorited() {
        return favorited;
    }

    public String getCreatedAt() {
        return createdAt;
    }
}
