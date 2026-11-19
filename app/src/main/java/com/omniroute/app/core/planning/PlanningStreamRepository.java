package com.omniroute.app.core.planning;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.omniroute.app.R;
import com.omniroute.app.core.AppStrings;
import com.omniroute.app.core.auth.AuthRepository;
import com.omniroute.app.core.network.ApiClient;
import com.omniroute.app.core.network.AppJson;
import com.omniroute.app.core.network.dto.UserDto;
import com.omniroute.app.core.room.RoomMember;
import com.omniroute.app.core.room.RoomSession;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

/**
 * WebSocket 实时通道：房间成员同步 + AI 规划推演。
 *
 * <p>后端 /ws 只会向房间内成员广播，鉴权走 Bearer。客户端上行
 * agent_negotiate 后，服务端会持续下发 stream_token、阶段事件与 final_route。
 *
 * <p>回调全部发生在 OkHttp 的线程上；界面层必须把事件切回主线程。
 */
public final class PlanningStreamRepository {

    /** 规划阶段的中文文案。未知类型不下发，避免界面出现英文枚举。 */
    private static String stageLabel(String type) {
        switch (type) {
            case "negotiation_started":
                return AppStrings.get(R.string.plan_stage_negotiation_started);
            case "reasoning_step":
                return AppStrings.get(R.string.plan_stage_reasoning);
            case "target_city":
                return AppStrings.get(R.string.plan_stage_target_city);
            case "weather_info":
                return AppStrings.get(R.string.plan_stage_weather);
            case "traffic_info":
                return AppStrings.get(R.string.plan_stage_traffic);
            case "travel_details":
                return AppStrings.get(R.string.plan_stage_travel_details);
            case "budget_breakdown":
                return AppStrings.get(R.string.plan_stage_budget);
            case "safety_info":
                return AppStrings.get(R.string.plan_stage_safety);
            case "specialist_proposals_ready":
                return AppStrings.get(R.string.plan_stage_proposals_ready);
            default:
                return null;
        }
    }

    public interface Listener {
        void onOpen();

        void onMembers(List<RoomMember> members);

        /** 房间方案池发生变化（新增 / 投票 / 采纳），收到后重新拉取即可。 */
        void onVariantSync();

        /** 规划阶段推进；detail 可能为空。 */
        void onStage(String label, String detail);

        /** 逐字推理片段。界面可累积展示，也可只做进度提示。 */
        void onToken(String token);

        void onError(String message);

        void onFinished(PlanResult result);

        /** 连接关闭（含用户主动取消），reason 只用于诊断展示。 */
        void onClosed(String reason);
    }

    /**
     * 取消标记：Session 与 Bridge 共享。
     *
     * <p>OkHttp 在 socket 被 cancel 之后仍可能回调 onFailure/onClosed；若不做这层判断，
     * 用户已经退出房间了，界面还会被旧连接写回「实时通道中断」。
     */
    static final class CancelFlag {
        private volatile boolean cancelled;
    }

    /** 一次实时会话句柄；取消后不再回调。 */
    public static final class Session {
        @Nullable
        private final WebSocket webSocket;
        private final CancelFlag flag;

        Session(@Nullable WebSocket webSocket, CancelFlag flag) {
            this.webSocket = webSocket;
            this.flag = flag;
        }

        public void cancel() {
            flag.cancelled = true;
            if (webSocket != null) {
                webSocket.cancel();
            }
        }

        boolean isCancelled() {
            return flag.cancelled;
        }
    }

    private final ApiClient apiClient;
    private final AuthRepository authRepository;

    public PlanningStreamRepository(ApiClient apiClient, AuthRepository authRepository) {
        this.apiClient = apiClient;
        this.authRepository = authRepository;
    }

    /** 只观察房间成员变化，不触发推演（房间页使用）。 */
    public Session watchRoom(RoomSession room, Listener listener) {
        return connect(room, "", false, listener);
    }

    /** 发起一次 AI 规划推演。 */
    public Session startPlan(RoomSession room, PlanRequest request, Listener listener) {
        return connect(room, request.buildIntent(), true, listener);
    }

    private Session connect(RoomSession room, String intent, boolean negotiate, Listener listener) {
        HttpUrl base = HttpUrl.parse(apiClient.getBaseUrl());
        if (base == null) {
            listener.onError(AppStrings.get(R.string.plan_error_gateway_invalid));
            return new Session(null, new CancelFlag());
        }
        // OkHttp 的 Request 只接受 http/https，newWebSocket 会自行升级成 ws/wss；
        // 这里若直接写 ws:// 会抛出 IllegalArgumentException: unexpected scheme: ws。
        HttpUrl url = base.newBuilder()
                .encodedPath("/ws")
                .addQueryParameter("room_id", room.getRoomId())
                .addQueryParameter("nickname", nickname())
                .addQueryParameter("role", room.getRole() == null ? "成员" : room.getRole())
                .addQueryParameter("intent", intent)
                .build();

        Request request = new Request.Builder().url(url).build();
        CancelFlag flag = new CancelFlag();
        WebSocket webSocket = apiClient.openWebSocket(
                request, new Bridge(listener, negotiate, room, intent, flag));
        return new Session(webSocket, flag);
    }

    private String nickname() {
        UserDto user = authRepository.getUser();
        if (user == null) {
            return "旅行者";
        }
        String display = user.getDisplayName();
        if (display != null && !display.trim().isEmpty()) {
            return display.trim();
        }
        String username = user.getUsername();
        return username == null ? "旅行者" : username.trim();
    }

    private final class Bridge extends WebSocketListener {
        private final Listener listener;
        private final boolean negotiate;
        private final RoomSession room;
        private final String intent;
        private final CancelFlag flag;
        private boolean finished;

        Bridge(Listener listener, boolean negotiate, RoomSession room, String intent, CancelFlag flag) {
            this.listener = listener;
            this.negotiate = negotiate;
            this.room = room;
            this.intent = intent;
            this.flag = flag;
        }

        @Override
        public void onOpen(@NonNull WebSocket webSocket, @NonNull okhttp3.Response response) {
            if (flag.cancelled) {
                return;
            }
            listener.onOpen();
            if (negotiate) {
                // 房间成员会在注册连接时由网关主动推送，这里只负责发起推演。
                webSocket.send(negotiateJson(room, intent));
            }
        }

        @Override
        public void onMessage(@NonNull WebSocket webSocket, @NonNull String text) {
            if (flag.cancelled) {
                return;
            }
            JsonObject root;
            try {
                JsonElement parsed = JsonParser.parseString(text);
                if (!parsed.isJsonObject()) {
                    return;
                }
                root = parsed.getAsJsonObject();
            } catch (RuntimeException ignored) {
                return;
            }
            String type = string(root, "type");
            JsonElement payload = root.get("payload");
            if (type.isEmpty() || payload == null || payload.isJsonNull()) {
                return;
            }

            switch (type) {
                case "room_members_update":
                    listener.onMembers(parseMembers(payload));
                    return;
                case "plan_variant_sync":
                    listener.onVariantSync();
                    return;
                case "stream_token":
                    String token = payload.isJsonPrimitive() ? payload.getAsString() : "";
                    // final_route 会单独下发结构化结果；流里还会混入原始 JSON 片段，一并丢弃。
                    if (isReadableToken(token)) {
                        listener.onToken(token);
                    }
                    return;
                case "error":
                    listener.onError(errorMessage(payload));
                    return;
                case "final_route":
                case "final_route_ready":
                case "ai_negotiation_result":
                    PlanResult result = PlanResult.parse(payload);
                    if (result != null && !finished) {
                        finished = true;
                        listener.onFinished(result);
                    }
                    return;
                default:
                    String label = stageLabel(type);
                    if (label != null) {
                        listener.onStage(label, stageDetail(payload));
                    }
            }
        }

        @Override
        public void onFailure(@NonNull WebSocket webSocket, @NonNull Throwable t, @Nullable okhttp3.Response response) {
            if (flag.cancelled) {
                return;
            }
            // 推演中断时界面只会说「通道中断」，真实原因（读超时/协议错误/EOF）必须留在日志里，
            // 否则只能靠猜。这里只打异常类型与消息，不含 token 或行程内容。
            android.util.Log.w("PlanningStream", "WebSocket 失败: " + t.getClass().getSimpleName() + " / " + t.getMessage(), t);
            String raw = t.getMessage() == null ? "" : t.getMessage();
            if (raw.contains("401")) {
                listener.onError(AppStrings.get(R.string.error_session_expired_retry));
            } else if (!finished) {
                listener.onError(AppStrings.get(R.string.plan_error_stream_interrupted));
            }
            listener.onClosed(AppStrings.get(R.string.plan_error_connect_failed));
        }

        @Override
        public void onClosed(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
            if (flag.cancelled) {
                return;
            }
            android.util.Log.w("PlanningStream", "WebSocket 关闭: code=" + code + " reason=" + reason);
            listener.onClosed(reason == null || reason.trim().isEmpty()
                    ? AppStrings.get(R.string.plan_error_connect_closed)
                    : reason);
        }
    }

    /**
     * 推理流里除了 [FINAL_JSON] 还会直接吐原始 JSON 片段（如 {@code "lnglat": [...]}）
     * 与 markdown 代码块围栏（如 {@code ```negotiation_summary}）。
     * 这些属于结构化结果，最终由 final_route 单独下发，不应混进正文。
     */
    private static boolean isReadableToken(String token) {
        if (token == null || token.isEmpty() || token.contains("[FINAL_JSON]")) {
            return false;
        }
        for (char ch : new char[]{'"', '{', '}', '[', ']', '`', '\\'}) {
            if (token.indexOf(ch) >= 0) {
                return false;
            }
        }
        return true;
    }

    private static String negotiateJson(RoomSession room, String intent) {
        JsonObject root = new JsonObject();
        root.addProperty("type", "agent_negotiate");

        JsonObject payload = new JsonObject();
        JsonArray destinations = new JsonArray();
        // intent 里已包含目的地文本；这里同时补一个结构化目的地给引擎提取城市。
        String destination = extractDestination(intent);
        if (!destination.isEmpty()) {
            destinations.add(destination);
        }
        payload.add("destinations", destinations);

        JsonObject preferences = new JsonObject();
        // 引擎对 mode/role 做严格枚举校验（非法值直接 400），必须发英文模式名与偏好定位。
        preferences.addProperty("mode", "coop");
        preferences.addProperty("role", roleFor(intent));
        preferences.addProperty("intent", intent);
        preferences.add("history_sequence", new JsonArray());
        preferences.add("current_existing_route", new JsonArray());
        payload.add("user_preferences", preferences);

        root.add("payload", payload);
        return root.toString();
    }

    /**
     * 把用户的旅行偏好映射成引擎允许的偏好定位枚举。
     * 引擎只接受 寻味探索 / 视觉体验 / 休闲漫步 / 深度探索，房间角色（主理人/成员）不能直接下发。
     */
    private static String roleFor(String intent) {
        String text = intent == null ? "" : intent;
        if (text.contains("美食")) {
            return "寻味探索";
        }
        if (text.contains("摄影") || text.contains("自然风光") || text.contains("夜生活")) {
            return "视觉体验";
        }
        if (text.contains("慢节奏") || text.contains("亲子") || text.contains("购物")) {
            return "休闲漫步";
        }
        return "深度探索";
    }

    /** 从「目的地：成都；…」里取出城市名，避免让引擎从整段诉求里猜。 */
    private static String extractDestination(String intent) {
        if (intent == null) {
            return "";
        }
        for (String part : intent.split("；")) {
            String trimmed = part.trim();
            if (trimmed.startsWith("目的地：")) {
                return trimmed.substring("目的地：".length()).trim();
            }
        }
        return "";
    }

    private static List<RoomMember> parseMembers(JsonElement payload) {
        List<RoomMember> members = new ArrayList<>();
        if (!payload.isJsonArray()) {
            return members;
        }
        RoomMember[] parsed = AppJson.gson().fromJson(payload, RoomMember[].class);
        if (parsed == null) {
            return members;
        }
        for (RoomMember member : parsed) {
            if (member != null) {
                members.add(member);
            }
        }
        return members;
    }

    private static String stageDetail(JsonElement payload) {
        if (payload.isJsonPrimitive()) {
            return payload.getAsString();
        }
        if (!payload.isJsonObject()) {
            return "";
        }
        JsonObject obj = payload.getAsJsonObject();
        for (String key : new String[]{"summary", "message", "detail", "city", "label"}) {
            JsonElement value = obj.get(key);
            if (value != null && value.isJsonPrimitive()) {
                String text = value.getAsString().trim();
                if (!text.isEmpty()) {
                    return text.length() > 120 ? text.substring(0, 120) + "…" : text;
                }
            }
        }
        return "";
    }

    private static String errorMessage(JsonElement payload) {
        if (payload.isJsonPrimitive()) {
            return payload.getAsString();
        }
        if (payload.isJsonObject()) {
            JsonElement message = payload.getAsJsonObject().get("message");
            if (message != null && message.isJsonPrimitive()) {
                return message.getAsString();
            }
        }
        return AppStrings.get(R.string.plan_error_failed_retry);
    }

    private static String string(JsonObject obj, String key) {
        JsonElement element = obj.get(key);
        if (element == null || element.isJsonNull() || !element.isJsonPrimitive()) {
            return "";
        }
        return element.getAsString().trim().toLowerCase(Locale.US);
    }
}
