package com.omniroute.app.core.network.dto;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/** GET /api/community/comments 的响应。 */
public final class CommunityCommentsResponse {

    @SerializedName("comments")
    private List<CommunityCommentDto> comments;

    public List<CommunityCommentDto> getComments() {
        return comments == null ? new ArrayList<>() : comments;
    }
}
