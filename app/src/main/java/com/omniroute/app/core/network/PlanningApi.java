package com.omniroute.app.core.network;

import com.omniroute.app.core.network.dto.ApiEnvelope;
import com.omniroute.app.core.network.dto.CreateRoomRequest;
import com.omniroute.app.core.network.dto.CreateRoomResponse;
import com.omniroute.app.core.network.dto.DestinationRecommendData;
import com.omniroute.app.core.network.dto.ExecutionData;
import com.omniroute.app.core.network.dto.ExecutionUpdateData;
import com.omniroute.app.core.network.dto.FeedbackRequest;
import com.omniroute.app.core.network.dto.IntentParseData;
import com.omniroute.app.core.network.dto.IntentParseRequest;
import com.omniroute.app.core.network.dto.JoinRoomRequest;
import com.omniroute.app.core.network.dto.JoinRoomResponse;
import com.omniroute.app.core.network.dto.JourneyStateData;
import com.omniroute.app.core.network.dto.OptionsData;
import com.omniroute.app.core.network.dto.PackingChecklistData;
import com.omniroute.app.core.network.dto.PlanningContextData;
import com.omniroute.app.core.network.dto.PlanningContextDto;
import com.omniroute.app.core.network.dto.PushTokenRequest;
import com.omniroute.app.core.network.dto.RenameTripRequest;
import com.omniroute.app.core.network.dto.RiskNotificationsData;
import com.omniroute.app.core.network.dto.SaveTripRequest;
import com.omniroute.app.core.network.dto.SaveTripResponse;
import com.omniroute.app.core.network.dto.TransportOptionsData;
import com.omniroute.app.core.network.dto.TripsResponse;
import com.omniroute.app.core.network.dto.UpdateTripContentRequest;

import com.google.gson.JsonObject;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * 规划主链路、房间、反馈与通知接口。
 * /api/v1/planning/* 与 /api/v1/risk|feedback|notifications 返回 {data, meta} 信封；
 * 房间创建/加入返回扁平 JSON。
 */
public interface PlanningApi {

    /** 用 room 作为首次规划的资源键；房间同时是后续联合规划的协作空间。 */
    @POST("api/v1/room/create")
    Call<CreateRoomResponse> createRoom(@Body CreateRoomRequest body);

    @POST("api/v1/room/join")
    Call<JoinRoomResponse> joinRoom(@Body JoinRoomRequest body);

    /** 一句话描述 → 结构化表单字段（服务端解析，客户端展示后由用户确认）。 */
    @POST("api/v1/planning/intent/parse")
    Call<ApiEnvelope<IntentParseData>> parseIntent(@Body IntentParseRequest body);

    @POST("api/v1/planning/context")
    Call<ApiEnvelope<PlanningContextData>> planningContext(@Body PlanningContextDto body);

    @POST("api/v1/planning/destination/recommend")
    Call<ApiEnvelope<DestinationRecommendData>> destinationRecommend(@Body PlanningContextDto body);

    @POST("api/v1/planning/transport/options")
    Call<ApiEnvelope<TransportOptionsData>> transportOptions(@Body PlanningContextDto body);

    @POST("api/v1/planning/lodging/options")
    Call<ApiEnvelope<OptionsData>> lodgingOptions(@Body PlanningContextDto body);

    @POST("api/v1/planning/dining/options")
    Call<ApiEnvelope<OptionsData>> diningOptions(@Body PlanningContextDto body);

    /** 行前清单：按上下文（天气/活动/健康/行李额度）生成，不落库。 */
    @POST("api/v1/planning/packing/checklist")
    Call<ApiEnvelope<PackingChecklistData>> packingChecklist(@Body PlanningContextDto body);

    /** 读取当前行程的节点执行状态（按 node_key 幂等）。 */
    @GET("api/v1/planning/execution")
    Call<ApiEnvelope<ExecutionData>> executionStates(@Query("trip_id") String tripId);

    /** 记录一个节点动作；延迟 ≥30 分钟会返回待确认的重规划提案。 */
    @POST("api/v1/planning/execution")
    Call<ApiEnvelope<ExecutionUpdateData>> updateExecution(@Body Map<String, Object> body);

    /** 执行 + 风险 + 预算的只读快照，用于旅途页状态机展示。 */
    @GET("api/v1/planning/journey/state")
    Call<ApiEnvelope<JourneyStateData>> journeyState(@Query("trip_id") String tripId);

    /** 最小扰动重规划：只返回受影响节点的替代候选，不直接改行程。 */
    @POST("api/v1/planning/replan")
    Call<ApiEnvelope<JsonObject>> replan(@Body Map<String, Object> body);

    /** 应用前的硬约束校验；服务端不落库。 */
    @POST("api/v1/planning/replan/validate")
    Call<ApiEnvelope<JsonObject>> validateReplan(@Body Map<String, Object> body);

    /** 用户明确确认后才真正替换节点。 */
    @POST("api/v1/planning/replan/confirm")
    Call<ApiEnvelope<JsonObject>> confirmReplan(@Body Map<String, Object> body);

    /** 拒绝提案只记录决定，不改行程。 */
    @POST("api/v1/planning/replan/reject")
    Call<ApiEnvelope<JsonObject>> rejectReplan(@Body Map<String, Object> body);

    @GET("api/user/trips")
    Call<TripsResponse> trips();

    @POST("api/user/trip")
    Call<SaveTripResponse> saveTrip(@Body SaveTripRequest body);

    /** 行程详情编辑：重命名标题（归属由服务端会话校验）。 */
    @PUT("api/user/trip/{id}")
    Call<Object> renameTrip(@Path("id") String tripId, @Body RenameTripRequest body);

    /** 行程详情编辑：更新路线内容（顺序 / 所属天 / 备注都写在 content 里）。 */
    @PUT("api/user/trip/{id}/content")
    Call<Object> updateTripContent(@Path("id") String tripId, @Body UpdateTripContentRequest body);

    @DELETE("api/user/trip/{id}")
    Call<Object> deleteTrip(@Path("id") String tripId);

    @POST("api/v1/feedback")
    Call<ApiEnvelope<Object>> submitFeedback(@Body FeedbackRequest body);

    @GET("api/v1/risk/notifications")
    Call<ApiEnvelope<RiskNotificationsData>> riskNotifications(@Query("status") String status);

    @POST("api/v1/risk/notifications/{id}/read")
    Call<ApiEnvelope<Object>> acknowledgeRiskNotification(@Path("id") String id, @Query("status") String status);

    @POST("api/v1/risk/notifications/read-all")
    Call<ApiEnvelope<Object>> markAllRiskNotificationsRead(@Query("status") String status);

    /** 订阅目的地风险监控（幂等，按城市）。 */
    @POST("api/v1/risk/subscribe")
    Call<JsonObject> subscribeRisk(@Body Map<String, Object> body);

    /** 已订阅的风险城市列表。 */
    @GET("api/v1/risk/subscriptions")
    Call<JsonObject> riskSubscriptions();

    /** 触发一次风险巡检：网关刷新快照、生成变更通知并落库。 */
    @POST("api/v1/risk/subscriptions/check")
    Call<JsonObject> checkRiskSubscriptions(@Body Map<String, Object> body);

    @POST("api/v1/notifications/push/fcm-token")
    Call<ApiEnvelope<Object>> registerPushToken(@Body PushTokenRequest body);

    @DELETE("api/v1/notifications/push/fcm-token")
    Call<ApiEnvelope<Object>> unregisterPushToken(@Query("token") String token);
}
