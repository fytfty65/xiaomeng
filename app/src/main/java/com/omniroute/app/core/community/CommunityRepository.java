package com.omniroute.app.core.community;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.omniroute.app.R;
import com.omniroute.app.core.AppStrings;
import com.omniroute.app.core.network.ApiClient;
import com.omniroute.app.core.network.CommunityApi;
import com.omniroute.app.core.network.dto.ApiErrors;
import com.omniroute.app.core.network.dto.ApiException;
import com.omniroute.app.core.network.dto.CommunityCommentDto;
import com.omniroute.app.core.network.dto.CommunityPostDto;
import com.omniroute.app.core.network.dto.MessageResponse;
import com.omniroute.app.core.network.dto.PublishPostRequest;
import com.omniroute.app.core.network.dto.PublishPostResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Response;

/** 社区读写的唯一入口，错误统一转成可展示文案。 */
public final class CommunityRepository {

    private final CommunityApi api;

    public CommunityRepository(ApiClient apiClient) {
        this.api = apiClient.getCommunityApi();
    }

    public List<CommunityPostDto> feed(String tag) throws ApiException {
        Response<com.omniroute.app.core.network.dto.CommunityFeedResponse> response =
                execute(api.listPosts(tag == null ? "" : tag.trim()),
                        R.string.community_error_load);
        return response.body() == null ? new ArrayList<>() : response.body().getPosts();
    }

    public List<CommunityCommentDto> comments(String postId) throws ApiException {
        Response<com.omniroute.app.core.network.dto.CommunityCommentsResponse> response =
                execute(api.comments(postId), R.string.community_error_load);
        return response.body() == null ? new ArrayList<>() : response.body().getComments();
    }

    /** 热门话题按出现次数降序，最多取 12 个用于筛选。 */
    public List<String> tags() throws ApiException {
        Response<JsonObject> response = execute(api.tags(), R.string.community_error_load);
        JsonArray array = response.body() == null ? null : response.body().getAsJsonArray("tags");
        if (array == null) {
            return new ArrayList<>();
        }
        List<String> tags = new ArrayList<>();
        for (JsonElement element : array) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonElement tag = element.getAsJsonObject().get("tag");
            if (tag != null && tag.isJsonPrimitive()) {
                String value = tag.getAsString().trim();
                if (!value.isEmpty()) {
                    tags.add(value);
                }
                if (tags.size() >= 12) {
                    break;
                }
            }
        }
        return tags;
    }

    public void publish(String title, String content, String destCity, List<String> tags)
            throws ApiException {
        if ((title == null || title.trim().isEmpty())
                && (content == null || content.trim().isEmpty())) {
            throw new ApiException(AppStrings.get(R.string.community_error_empty));
        }
        Response<PublishPostResponse> response = execute(
                api.publishPost(new PublishPostRequest(
                        title == null ? "" : title.trim(),
                        content == null ? "" : content.trim(),
                        destCity == null ? "" : destCity.trim(),
                        tags == null ? new ArrayList<>() : tags)),
                R.string.community_error_publish);
        if (response.body() == null || response.body().getPostId() == null) {
            throw new ApiException(AppStrings.get(R.string.community_error_publish));
        }
    }

    public void comment(String postId, String content, String parentId) throws ApiException {
        String text = content == null ? "" : content.trim();
        if (text.isEmpty()) {
            throw new ApiException(AppStrings.get(R.string.community_error_comment_empty));
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("post_id", postId);
        body.put("content", text);
        if (parentId != null && !parentId.trim().isEmpty()) {
            body.put("parent_id", parentId.trim());
        }
        execute(api.comment(body), R.string.community_error_comment);
    }

    public int like(String postId) throws ApiException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("post_id", postId);
        Response<Map<String, Object>> response = execute(api.like(body), R.string.community_error_action);
        Object likes = response.body() == null ? null : response.body().get("likes");
        return likes instanceof Number ? ((Number) likes).intValue() : 0;
    }

    public FavoriteResult favorite(String postId) throws ApiException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("post_id", postId);
        Response<Map<String, Object>> response = execute(
                api.favorite(body), R.string.community_error_action);
        Map<String, Object> data = response.body();
        boolean favorited = data != null && Boolean.TRUE.equals(data.get("favorited"));
        int favorites = 0;
        Object count = data == null ? null : data.get("favorites");
        if (count instanceof Number) {
            favorites = ((Number) count).intValue();
        }
        return new FavoriteResult(favorited, favorites);
    }

    private <T> Response<T> execute(Call<T> call, int fallbackRes) throws ApiException {
        Response<T> response;
        try {
            response = call.execute();
        } catch (IOException e) {
            throw new ApiException(AppStrings.get(R.string.error_network));
        }
        if (!response.isSuccessful() || response.body() == null) {
            String message = ApiErrors.localized(response);
            throw new ApiException(
                    message == null || message.trim().isEmpty()
                            ? AppStrings.get(fallbackRes)
                            : message);
        }
        return response;
    }

    /** 收藏切换结果。 */
    public static final class FavoriteResult {
        public final boolean favorited;
        public final int favorites;

        public FavoriteResult(boolean favorited, int favorites) {
            this.favorited = favorited;
            this.favorites = favorites;
        }
    }
}
