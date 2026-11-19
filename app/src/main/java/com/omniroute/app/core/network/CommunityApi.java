package com.omniroute.app.core.network;

import com.google.gson.JsonObject;
import com.omniroute.app.core.network.dto.CommunityCommentsResponse;
import com.omniroute.app.core.network.dto.CommunityFeedResponse;
import com.omniroute.app.core.network.dto.MessageResponse;
import com.omniroute.app.core.network.dto.PublishPostRequest;
import com.omniroute.app.core.network.dto.PublishPostResponse;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

/** 社区接口都需要登录态，统一挂 authorizedClient。 */
public interface CommunityApi {

    @GET("api/community/list")
    Call<CommunityFeedResponse> listPosts(@Query("tag") String tag);

    @POST("api/community/post")
    Call<PublishPostResponse> publishPost(@Body PublishPostRequest body);

    @GET("api/community/comments")
    Call<CommunityCommentsResponse> comments(@Query("post_id") String postId);

    @POST("api/community/comment")
    Call<MessageResponse> comment(@Body Map<String, Object> body);

    /** 点赞只增不减，服务端返回最新 likes。 */
    @POST("api/community/like")
    Call<Map<String, Object>> like(@Body Map<String, Object> body);

    /** 收藏是切换语义，返回 favorited 与 favorites。 */
    @POST("api/community/favorite")
    Call<Map<String, Object>> favorite(@Body Map<String, Object> body);

    @GET("api/community/tags")
    Call<JsonObject> tags();
}
