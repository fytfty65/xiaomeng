package com.omniroute.app.core.planning;

import com.omniroute.app.R;
import com.omniroute.app.core.AppStrings;
import com.omniroute.app.core.auth.AuthRepository;
import com.omniroute.app.core.network.ApiClient;
import com.omniroute.app.core.network.AppJson;
import com.omniroute.app.core.network.PlanningApi;
import com.omniroute.app.core.network.dto.ApiEnvelope;
import com.omniroute.app.core.network.dto.ApiErrors;
import com.omniroute.app.core.network.dto.ApiException;
import com.omniroute.app.core.network.dto.CreateRoomRequest;
import com.omniroute.app.core.network.dto.CreateRoomResponse;
import com.omniroute.app.core.network.dto.DestinationRecommendData;
import com.omniroute.app.core.network.dto.ExecutionData;
import com.omniroute.app.core.network.dto.ExecutionUpdateData;
import com.omniroute.app.core.network.dto.IntentParseData;
import com.omniroute.app.core.network.dto.IntentParseRequest;
import com.omniroute.app.core.network.dto.JourneyStateData;
import com.omniroute.app.core.network.dto.OptionsData;
import com.omniroute.app.core.network.dto.PackingChecklistData;
import com.omniroute.app.core.network.dto.PlanningContextData;
import com.omniroute.app.core.network.dto.PlanningContextDto;
import com.omniroute.app.core.network.dto.RenameTripRequest;
import com.omniroute.app.core.network.dto.SaveTripRequest;
import com.omniroute.app.core.network.dto.SaveTripResponse;
import com.omniroute.app.core.network.dto.TransportOptionsData;
import com.omniroute.app.core.network.dto.TripPlanDto;
import com.omniroute.app.core.network.dto.TripsResponse;
import com.omniroute.app.core.network.dto.UpdateTripContentRequest;
import com.omniroute.app.core.network.dto.UserDto;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Response;

/**
 * 规划主链路仓库：目的地 → 推荐/交通/住宿/餐饮 → 保存行程 → 行程列表。
 *
 * <p>首次规划没有真实 trip_id，按后端契约用自己创建的 room_id 作为资源键；
 * 该键只在登录用户不变时复用，避免切换账号后越权。
 *
 * <p>方法均为阻塞调用，调用方必须放在后台线程。
 */
public final class PlanningRepository {

    private static final String SESSION_ROOM_NAME = "OmniRoute 移动端规划";
    private static final String DEFAULT_CURRENCY = "CNY";
    private static final int MAX_DAYS = 30;

    private final PlanningApi api;
    private final AuthRepository authRepository;

    private volatile String cachedResourceId;
    private volatile String cachedOwnerUsername;

    public PlanningRepository(ApiClient apiClient, AuthRepository authRepository) {
        this.api = apiClient.getPlanningApi();
        this.authRepository = authRepository;
    }

    /** 串行执行上下文校验与四类方案生成，任一环节失败即整体失败并给出后端文案。 */
    public TripDraft generatePlan(String destination, String origin, int days, int travelers) throws ApiException {
        String city = destination == null ? "" : destination.trim();
        if (city.isEmpty()) {
            throw new ApiException(AppStrings.get(R.string.plan_error_destination_missing));
        }
        int plannedDays = days < 1 ? 1 : Math.min(days, MAX_DAYS);
        int party = travelers < 1 ? 1 : travelers;
        String from = origin == null ? "" : origin.trim();
        String resourceId = ensureResourceId();

        PlanningContextDto context = new PlanningContextDto();
        context.setTripId(resourceId);
        context.setDestination(city);
        context.setTravelers(party);
        context.setCurrency(DEFAULT_CURRENCY);
        if (!from.isEmpty()) {
            context.setOrigin(from);
        }

        data(api.planningContext(context), AppStrings.get(R.string.label_planning_context));
        DestinationRecommendData destinationData =
                data(api.destinationRecommend(context), AppStrings.get(R.string.label_destination_recommend));
        TransportOptionsData transportData =
                data(api.transportOptions(context), AppStrings.get(R.string.label_transport_options));
        OptionsData lodgingData =
                data(api.lodgingOptions(context), AppStrings.get(R.string.label_lodging_options));
        OptionsData diningData =
                data(api.diningOptions(context), AppStrings.get(R.string.label_dining_options));

        return new TripDraft(
                resourceId,
                city,
                from,
                plannedDays,
                party,
                destinationData.getCandidates(),
                transportData.getOptions(),
                lodgingData.getOptions(),
                diningData.getOptions(),
                destinationData.getRanking(),
                destinationData.isFallback());
    }

    /** 一句话描述交给服务端做结构化解析；调用方负责 400/网络错误提示。 */
    public IntentParseData parseIntent(String text) throws ApiException {
        String raw = text == null ? "" : text.trim();
        if (raw.isEmpty()) {
            throw new ApiException(AppStrings.get(R.string.plan_error_chat_empty));
        }
        return data(api.parseIntent(new IntentParseRequest(raw)),
                AppStrings.get(R.string.label_planning_intent));
    }

    public List<TripPlanDto> listTrips() throws ApiException {
        Response<TripsResponse> response = execute(api.trips());
        TripsResponse body = response.body();
        return body == null ? new ArrayList<>() : body.getTrips();
    }

    /** 后端没有单条行程查询接口，按 ID 在列表里定位。 */
    public TripPlanDto getTrip(String tripId) throws ApiException {
        String id = tripId == null ? "" : tripId.trim();
        if (id.isEmpty()) {
            throw new ApiException(AppStrings.get(R.string.plan_error_trip_missing));
        }
        for (TripPlanDto trip : listTrips()) {
            if (trip != null && id.equals(trip.getId())) {
                return trip;
            }
        }
        throw new ApiException(AppStrings.get(R.string.plan_error_trip_missing));
    }

    /** 重命名行程标题；只改动标题，不触碰已归档的路线内容。 */
    public void renameTrip(String tripId, String title) throws ApiException {
        String id = tripId == null ? "" : tripId.trim();
        String nextTitle = title == null ? "" : title.trim();
        if (id.isEmpty()) {
            throw new ApiException(AppStrings.get(R.string.plan_error_trip_missing));
        }
        if (nextTitle.isEmpty()) {
            throw new ApiException(AppStrings.get(R.string.plan_error_title_empty));
        }
        if (nextTitle.length() > 200) {
            throw new ApiException(AppStrings.get(R.string.plan_error_title_too_long));
        }
        execute(api.renameTrip(id, new RenameTripRequest(nextTitle)));
    }

    /** 更新已保存行程的路线内容；顺序 / 所属天 / 备注都以 content 为准。 */
    public void updateTripContent(String tripId, String content) throws ApiException {
        String id = tripId == null ? "" : tripId.trim();
        if (id.isEmpty()) {
            throw new ApiException(AppStrings.get(R.string.plan_error_trip_missing));
        }
        String next = content == null ? "" : content.trim();
        if (next.isEmpty()) {
            throw new ApiException(AppStrings.get(R.string.plan_error_no_stops));
        }
        execute(api.updateTripContent(id, new UpdateTripContentRequest(next)));
    }

    /** 删除行程；仅本人可删，归属由服务端会话校验。 */
    public void deleteTrip(String tripId) throws ApiException {
        String id = tripId == null ? "" : tripId.trim();
        if (id.isEmpty()) {
            throw new ApiException(AppStrings.get(R.string.plan_error_trip_missing));
        }
        execute(api.deleteTrip(id));
    }

    /** 保存当前方案到「我的行程」，返回新的 trip_id。 */
    public String saveTrip(TripDraft draft) throws ApiException {
        UserDto user = authRepository.getUser();
        String userId = user == null || user.getId() == null ? "" : user.getId().trim();
        if (userId.isEmpty()) {
            throw new ApiException(AppStrings.get(R.string.error_session_expired));
        }
        SaveTripRequest request = new SaveTripRequest(
                userId,
                buildTitle(draft),
                draft.getDestination(),
                buildContent(draft),
                draft.getDays(),
                draft.getTravelers());
        Response<SaveTripResponse> response = execute(api.saveTrip(request));
        SaveTripResponse body = response.body();
        String tripId = body == null || body.getTripId() == null ? "" : body.getTripId().trim();
        if (tripId.isEmpty()) {
            throw new ApiException(AppStrings.get(R.string.plan_error_missing_trip_id));
        }
        return tripId;
    }

    /**
     * 保存 WebSocket 推演产出的正式路线。
     * content 结构与 Web 端 / 行程详情页一致（summary/city/days/routes）。
     *
     * <p>2026-11-03 修复：用户可能在结果页编辑过节点与备注，此前仍按原始路书保存，
     * 表现成「页面改了、云端没改」。这里以编辑后的节点为准（null = 未编辑，用原始路线），
     * 备注按 {@code 名称|时间} 归档后写入 desc 与 note，保存后回读校验节点顺序与数量。
     */
    public String savePlan(PlanRequest request, PlanResult result, List<RouteNode> editedNodes,
                           Map<String, String> notes) throws ApiException {
        UserDto user = authRepository.getUser();
        String userId = user == null || user.getId() == null ? "" : user.getId().trim();
        if (userId.isEmpty()) {
            throw new ApiException(AppStrings.get(R.string.error_session_expired));
        }
        List<RouteNode> nodes = new ArrayList<>();
        for (RouteNode node : (editedNodes != null ? editedNodes : result.getRoute())) {
            // day < 1 表示编辑页里的「待排池」：还没排进任何一天，不写入行程
            if (node.getDay() >= 1) {
                nodes.add(node);
            }
        }
        if (nodes.isEmpty()) {
            throw new ApiException(AppStrings.get(R.string.plan_error_no_stops));
        }
        int days = Math.max(result.getDays(), request.getDays());
        String title = request.getDestination() + " " + days + " 日行程";
        SaveTripRequest save = new SaveTripRequest(
                userId,
                title,
                request.getDestination(),
                buildPlanContent(request, result, nodes, notes),
                days,
                request.getTravelers());
        Response<SaveTripResponse> response = execute(api.saveTrip(save));
        SaveTripResponse body = response.body();
        String tripId = body == null || body.getTripId() == null ? "" : body.getTripId().trim();
        if (tripId.isEmpty()) {
            throw new ApiException(AppStrings.get(R.string.plan_error_missing_trip_id));
        }
        verifySavedRoutes(tripId, nodes);
        return tripId;
    }

    private String buildPlanContent(PlanRequest request, PlanResult result, List<RouteNode> nodes,
                                    Map<String, String> notes) {
        List<Map<String, Object>> days = new ArrayList<>();
        for (int index = 1; index <= Math.max(result.getDays(), request.getDays()); index++) {
            Map<String, Object> day = new LinkedHashMap<>();
            day.put("day", index);
            days.add(day);
        }
        List<Map<String, Object>> routes = new ArrayList<>();
        for (RouteNode node : nodes) {
            Map<String, Object> route = new LinkedHashMap<>();
            route.put("name", node.getName());
            route.put("type", node.getCategory());
            route.put("day", node.getDay());
            if (!node.getTime().isEmpty()) {
                route.put("time", node.getTime());
            }
            if (!node.getCostEstimate().isEmpty()) {
                route.put("price_label", node.getCostEstimate());
            }
            String desc = node.getDescription();
            String note = notes == null ? null : notes.get(node.getName() + "|" + node.getTime());
            if (note != null && !note.trim().isEmpty()) {
                String cleanNote = note.trim();
                // 结构化字段给后续编辑用；同时并入 desc，保证行程详情页用户真能看到
                route.put("note", cleanNote);
                desc = desc.isEmpty() ? "备注：" + cleanNote : desc + "\n备注：" + cleanNote;
            }
            if (!desc.isEmpty()) {
                route.put("desc", desc);
            }
            if (node.hasCoordinate()) {
                route.put("lng", node.getLng());
                route.put("lat", node.getLat());
            }
            if (!node.getAddress().isEmpty()) {
                route.put("address", node.getAddress());
            }
            if (!node.getOpenTime().isEmpty()) {
                route.put("open_time", node.getOpenTime());
            }
            if (!node.getOfficialUrl().isEmpty()) {
                // 行程详情页据此给出「打开官方预订页」入口，不能只留在结果页内存里
                route.put("official_url", node.getOfficialUrl());
                route.put("booking_url", node.getOfficialUrl());
            }
            if (node.isEstimated() || "estimated".equals(node.getTrust())) {
                route.put("estimated", true);
            }
            if (!node.getSourceLabel().isEmpty()) {
                route.put("source_label", node.getSourceLabel());
            }
            if (!node.getObservedAt().isEmpty()) {
                route.put("observed_at", node.getObservedAt());
            }
            if (!node.getTrust().isEmpty()) {
                route.put("trust", node.getTrust());
                route.put("trust_label", node.freshnessLabel());
                route.put("trust_message", node.getTrustMessage());
            }
            routes.add(route);
        }

        Map<String, Object> content = new LinkedHashMap<>();
        content.put("summary", result.getSummary().isEmpty()
                ? "OmniRoute 生成的" + request.getDestination() + "方案"
                : result.getSummary());
        content.put("city", request.getDestination());
        content.put("days", days);
        content.put("routes", routes);
        content.put("generated_by", "omniroute-android-ws");
        return AppJson.gson().toJson(content);
    }

    /**
     * 保存后回读校验（2026-11-03）：网关会清洗占位字段，但不该改动节点数量与顺序。
     * 回读请求失败不否定已成功的保存（只说明校验没做成）；回读到内容不一致才报错，
     * 否则用户会继续看到「保存成功但云端还是旧路线」。
     */
    private void verifySavedRoutes(String tripId, List<RouteNode> expected) throws ApiException {
        TripPlanDto saved = null;
        try {
            for (TripPlanDto trip : listTrips()) {
                if (trip.getId() != null && trip.getId().equals(tripId)) {
                    saved = trip;
                    break;
                }
            }
        } catch (ApiException readBackFailure) {
            return;
        }
        if (saved == null) {
            throw new ApiException(AppStrings.get(R.string.plan_error_verify_missing));
        }
        List<String> savedNames = routeNames(saved.getContent());
        List<String> expectedNames = new ArrayList<>();
        for (RouteNode node : expected) {
            expectedNames.add(node.getName());
        }
        if (!savedNames.equals(expectedNames)) {
            throw new ApiException(AppStrings.get(
                    R.string.plan_error_verify_mismatch, savedNames.size(), expectedNames.size()));
        }
    }

    private List<String> routeNames(String content) {
        List<String> names = new ArrayList<>();
        if (content == null || content.isEmpty()) {
            return names;
        }
        try {
            JsonObject root = JsonParser.parseString(content).getAsJsonObject();
            JsonArray routes = root.has("routes") ? root.getAsJsonArray("routes") : null;
            if (routes != null) {
                for (JsonElement element : routes) {
                    if (!element.isJsonObject()) {
                        continue;
                    }
                    JsonObject route = element.getAsJsonObject();
                    names.add(route.has("name") && !route.get("name").isJsonNull()
                            ? route.get("name").getAsString() : "");
                }
            }
        } catch (RuntimeException ignored) {
            return new ArrayList<>();
        }
        return names;
    }

    /**
     * 行前清单：按行程上下文生成物品建议。接口不落库，重复进入会得到同一套规则结果。
     */
    public PackingChecklistData packingChecklist(TripPlanDto trip) throws ApiException {
        return data(api.packingChecklist(tripContext(trip)),
                AppStrings.get(R.string.label_packing_checklist));
    }

    /** 读取节点执行状态；没有记录时返回空列表而不是报错。 */
    public ExecutionData executionStates(String tripId) throws ApiException {
        return data(api.executionStates(requireTripId(tripId)),
                AppStrings.get(R.string.label_execution_state));
    }

    /**
     * 记录一个节点动作。status 仅支持 planned/in_progress/visited/skipped/delayed；
     * 延迟 ≥30 分钟时响应会附带待用户确认的重规划提案。
     */
    public ExecutionUpdateData updateExecution(String tripId, String nodeKey, String status,
                                               int delayMinutes, String note, String planId)
            throws ApiException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("trip_id", requireTripId(tripId));
        body.put("node_key", nodeKey == null ? "" : nodeKey.trim());
        body.put("status", status == null ? "" : status.trim().toLowerCase());
        body.put("delay_minutes", Math.max(0, Math.min(delayMinutes, 1440)));
        String safeNote = note == null ? "" : note.trim();
        if (!safeNote.isEmpty()) {
            body.put("note", safeNote);
        }
        String safePlan = planId == null ? "" : planId.trim();
        if (!safePlan.isEmpty()) {
            body.put("plan_id", safePlan);
        }
        return data(api.updateExecution(body), AppStrings.get(R.string.label_execution_update));
    }

    /** 执行进度 + 风险提案 + 预算的只读快照。 */
    public JourneyStateData journeyState(String tripId) throws ApiException {
        return data(api.journeyState(requireTripId(tripId)),
                AppStrings.get(R.string.label_journey_state));
    }

    /**
     * 请求最小扰动重规划候选。只传受影响节点，服务端保证其它节点不被替换；
     * 候选必须再经过 {@link #validateReplan} 与 {@link #confirmReplan} 才能落地。
     */
    public JsonObject requestReplan(TripPlanDto trip, String affectedNode, int delayMinutes)
            throws ApiException {
        if (trip == null) {
            throw new ApiException(AppStrings.get(R.string.plan_error_trip_missing));
        }
        String node = affectedNode == null ? "" : affectedNode.trim();
        if (node.isEmpty()) {
            throw new ApiException(AppStrings.get(R.string.journey_error_node_missing));
        }
        Map<String, Object> signals = new LinkedHashMap<>();
        signals.put("delay_minutes", Math.max(delayMinutes, 0));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("trip_id", requireTripId(trip.getId()));
        body.put("destination", destinationOf(trip));
        body.put("travelers", Math.max(trip.getTravelers(), 1));
        body.put("currency", DEFAULT_CURRENCY);
        body.put("existing_route", readRoute(trip.getContent()));
        List<String> affected = new ArrayList<>();
        affected.add(node);
        body.put("affected_nodes", affected);
        body.put("changed_signals", signals);
        body.put("replacement_kind", "DESTINATION");
        return data(api.replan(body), AppStrings.get(R.string.label_replan));
    }

    /** 应用候选前的硬约束校验；hard_satisfied=false 时不得继续确认。 */
    public JsonObject validateReplan(TripPlanDto trip, String affectedNode, JsonObject candidate)
            throws ApiException {
        Map<String, Object> body = replanBody(trip, affectedNode);
        body.put("candidate", candidate);
        return data(api.validateReplan(body), AppStrings.get(R.string.label_replan_validate));
    }

    /** 用户明确确认后替换单个节点；成功后应重新拉取行程。 */
    public JsonObject confirmReplan(String tripId, String proposalId, String affectedNode,
                                    JsonObject candidate) throws ApiException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("trip_id", requireTripId(tripId));
        body.put("proposal_id", proposalId == null ? "" : proposalId.trim());
        body.put("candidate", candidate);
        List<String> affected = new ArrayList<>();
        affected.add(affectedNode == null ? "" : affectedNode.trim());
        body.put("affected_nodes", affected);
        body.put("confirmation", "user_confirmed");
        return data(api.confirmReplan(body), AppStrings.get(R.string.label_replan_confirm));
    }

    /** 拒绝提案只记录用户决定，不修改行程。 */
    public void rejectReplan(String tripId, String proposalId, String reason) throws ApiException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("trip_id", requireTripId(tripId));
        body.put("proposal_id", proposalId == null ? "" : proposalId.trim());
        body.put("reason", reason == null || reason.trim().isEmpty() ? "keep_original" : reason.trim());
        data(api.rejectReplan(body), AppStrings.get(R.string.label_replan_reject));
    }

    /** content.routes 的轻量视图，供重规划请求与节点键构造复用。 */
    public List<Map<String, Object>> readRoute(String content) {
        List<Map<String, Object>> routes = new ArrayList<>();
        String raw = content == null ? "" : content.trim();
        if (raw.isEmpty()) {
            return routes;
        }
        try {
            JsonObject root = JsonParser.parseString(raw).getAsJsonObject();
            JsonArray array = root.has("routes") ? root.getAsJsonArray("routes") : null;
            if (array == null) {
                return routes;
            }
            for (JsonElement element : array) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject route = element.getAsJsonObject();
                Map<String, Object> node = new LinkedHashMap<>();
                copyIfPresent(route, node, "name");
                copyIfPresent(route, node, "location");
                copyIfPresent(route, node, "day");
                copyIfPresent(route, node, "type");
                if (!node.containsKey("name") && !node.containsKey("location")) {
                    continue;
                }
                routes.add(node);
            }
        } catch (RuntimeException ignored) {
            return new ArrayList<>();
        }
        return routes;
    }

    private Map<String, Object> replanBody(TripPlanDto trip, String affectedNode) throws ApiException {
        if (trip == null) {
            throw new ApiException(AppStrings.get(R.string.plan_error_trip_missing));
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("trip_id", requireTripId(trip.getId()));
        body.put("destination", destinationOf(trip));
        body.put("travelers", Math.max(trip.getTravelers(), 1));
        body.put("currency", DEFAULT_CURRENCY);
        body.put("existing_route", readRoute(trip.getContent()));
        List<String> affected = new ArrayList<>();
        affected.add(affectedNode == null ? "" : affectedNode.trim());
        body.put("affected_nodes", affected);
        return body;
    }

    private PlanningContextDto tripContext(TripPlanDto trip) throws ApiException {
        if (trip == null) {
            throw new ApiException(AppStrings.get(R.string.plan_error_trip_missing));
        }
        PlanningContextDto context = new PlanningContextDto();
        context.setTripId(requireTripId(trip.getId()));
        context.setDestination(destinationOf(trip));
        context.setTravelers(Math.max(trip.getTravelers(), 1));
        context.setCurrency(DEFAULT_CURRENCY);
        return context;
    }

    /** 老行程可能没有 destCity，用标题兜底，避免上下文校验直接失败。 */
    private String destinationOf(TripPlanDto trip) {
        String city = trip.getDestCity() == null ? "" : trip.getDestCity().trim();
        if (!city.isEmpty()) {
            return city;
        }
        String title = trip.getTitle() == null ? "" : trip.getTitle().trim();
        return title.isEmpty() ? "旅行" : title;
    }

    private String requireTripId(String tripId) throws ApiException {
        String id = tripId == null ? "" : tripId.trim();
        if (id.isEmpty()) {
            throw new ApiException(AppStrings.get(R.string.plan_error_trip_missing));
        }
        return id;
    }

    private void copyIfPresent(JsonObject source, Map<String, Object> target, String key) {
        JsonElement value = source.get(key);
        if (value == null || value.isJsonNull() || !value.isJsonPrimitive()) {
            return;
        }
        target.put(key, value.getAsString());
    }

    private String ensureResourceId() throws ApiException {
        UserDto user = authRepository.getUser();
        String username = user == null || user.getUsername() == null ? "" : user.getUsername().trim();
        if (username.isEmpty()) {
            throw new ApiException(AppStrings.get(R.string.error_session_expired));
        }
        String cached = cachedResourceId;
        if (cached != null && !cached.isEmpty() && username.equals(cachedOwnerUsername)) {
            return cached;
        }
        String roomId = createRoom(username);
        cachedResourceId = roomId;
        cachedOwnerUsername = username;
        return roomId;
    }

    private String createRoom(String username) throws ApiException {
        Response<CreateRoomResponse> response =
                execute(api.createRoom(new CreateRoomRequest(username, SESSION_ROOM_NAME)));
        CreateRoomResponse body = response.body();
        String roomId = body == null || body.getRoomId() == null ? "" : body.getRoomId().trim();
        if (roomId.isEmpty()) {
            throw new ApiException(AppStrings.get(R.string.plan_error_session_create));
        }
        return roomId;
    }

    private String buildTitle(TripDraft draft) {
        return draft.getDestination() + " " + draft.getDays() + " 日行程";
    }

    /**
     * content 采用 Web 端可解析的结构（summary/city/days/routes），
     * 这样保存后的行程能直接在网页「个人主页」渲染，无需二次解析。
     */
    private String buildContent(TripDraft draft) {
        TripItinerary preview = TripPreview.fromDraft(draft);
        List<Map<String, Object>> days = new ArrayList<>();
        for (int index = 1; index <= draft.getDays(); index++) {
            Map<String, Object> day = new LinkedHashMap<>();
            day.put("day", index);
            days.add(day);
        }

        // 直接复用预览的排程结果，保证「结果页日视图预览」与保存内容一致。
        List<Map<String, Object>> routes = new ArrayList<>();
        for (TripItinerary.Day day : preview.getDays()) {
            for (TripItinerary.Item item : day.getItems()) {
                Map<String, Object> route = new LinkedHashMap<>();
                route.put("name", item.getName());
                route.put("type", item.getType());
                route.put("day", day.getDayNumber());
                if (item.getDurationMinutes() > 0) {
                    route.put("duration_minutes", item.getDurationMinutes());
                }
                route.put("estimated", item.isEstimated());
                if (item.getPriceLabel() != null) {
                    route.put("price_label", item.getPriceLabel());
                }
                if (item.getBookingUrl() != null) {
                    route.put("booking_url", item.getBookingUrl());
                }
                routes.add(route);
            }
        }

        Map<String, Object> content = new LinkedHashMap<>();
        content.put("summary", "OmniRoute 生成的" + draft.getDestination() + draft.getDays()
                + "日方案（" + draft.getTravelers() + "人）");
        content.put("city", draft.getDestination());
        content.put("days", days);
        content.put("routes", routes);
        content.put("generated_by", "omniroute-android");
        return AppJson.gson().toJson(content);
    }

    private <T> Response<T> execute(Call<T> call) throws ApiException {
        try {
            Response<T> response = call.execute();
            if (!response.isSuccessful()) {
                throw new ApiException(ApiErrors.message(response));
            }
            return response;
        } catch (IOException e) {
            throw new ApiException(AppStrings.get(R.string.error_network));
        }
    }

    private <T> T data(Call<ApiEnvelope<T>> call, String label) throws ApiException {
        Response<ApiEnvelope<T>> response = execute(call);
        ApiEnvelope<T> envelope = response.body();
        if (envelope == null) {
            throw new ApiException(AppStrings.get(R.string.error_empty_response, label));
        }
        if (envelope.getData() == null) {
            String message = envelope.getError() == null ? null : envelope.getError().getMessage();
            String code = envelope.getError() == null ? null : envelope.getError().getCode();
            if (message != null && code != null) {
                throw new ApiException(AppStrings.get(R.string.error_with_code, message, code));
            }
            throw new ApiException(message != null
                    ? message
                    : AppStrings.get(R.string.error_empty_data, label));
        }
        return envelope.getData();
    }
}
