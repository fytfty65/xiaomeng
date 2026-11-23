package com.omniroute.app.core.notify;

import com.omniroute.app.R;
import com.omniroute.app.core.AppStrings;
import com.omniroute.app.core.network.ApiClient;
import com.omniroute.app.core.network.PlanningApi;
import com.omniroute.app.core.network.dto.ApiEnvelope;
import com.omniroute.app.core.network.dto.ApiErrors;
import com.omniroute.app.core.network.dto.ApiException;
import com.omniroute.app.core.network.dto.PushTokenRequest;
import com.omniroute.app.core.network.dto.RiskNotificationDto;
import com.omniroute.app.core.network.dto.RiskNotificationsData;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Response;

/**
 * 风险通知与推送设备登记。阻塞调用，需放在后台线程。
 *
 * <p>FCM 设备 token 由客户端推送 SDK 提供；当前工程未附 Firebase 配置时
 * 不会产生真实 token，登记入口仍保留，配置后可原样启用。
 */
public final class NotificationRepository {

    private final PlanningApi api;

    public NotificationRepository(ApiClient apiClient) {
        this.api = apiClient.getPlanningApi();
    }

    /** @param status unread / read / acknowledged，空串表示不过滤 */
    public List<RiskNotificationDto> listRiskNotifications(String status) throws ApiException {
        RiskNotificationsData data = data(
                api.riskNotifications(status), AppStrings.get(R.string.label_risk_notifications));
        return data.getNotifications();
    }

    public int unreadCount() throws ApiException {
        return unreadRiskNotifications().getUnreadCount();
    }

    public RiskNotificationsData unreadRiskNotifications() throws ApiException {
        return data(
                api.riskNotifications("unread"), AppStrings.get(R.string.label_risk_notifications));
    }

    public void markRead(String notificationId, String status) throws ApiException {
        String target = status == null || status.trim().isEmpty() ? "read" : status.trim();
        data(
                api.acknowledgeRiskNotification(notificationId, target),
                AppStrings.get(R.string.label_notification_state));
    }

    /** 一次请求把所有未读通知标记为已读。 */
    public void markAllRead(String status) throws ApiException {
        String target = status == null || status.trim().isEmpty() ? "read" : status.trim();
        data(
                api.markAllRiskNotificationsRead(target),
                AppStrings.get(R.string.label_notification_state));
    }

    /** 订阅一个目的地的风险监控；重复订阅由网关做幂等处理。 */
    public void subscribeCity(String city) throws ApiException {
        String trimmed = city == null ? "" : city.trim();
        if (trimmed.isEmpty()) {
            return;
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("city", trimmed);
        raw(api.subscribeRisk(body));
    }

    /** 已订阅的风险城市（用于界面回显）。 */
    public List<String> subscribedCities() throws ApiException {
        JsonObject data = raw(api.riskSubscriptions());
        List<String> cities = new ArrayList<>();
        JsonArray list = data.has("subscriptions") && data.get("subscriptions").isJsonArray()
                ? data.getAsJsonArray("subscriptions")
                : new JsonArray();
        for (JsonElement element : list) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonElement city = element.getAsJsonObject().get("city");
            if (city != null && !city.isJsonNull()) {
                String name = city.getAsString();
                if (!name.trim().isEmpty()) {
                    cities.add(name.trim());
                }
            }
        }
        return cities;
    }

    /** 触发一次风险巡检：变更会写成站内通知，前台可据此弹系统通知。 */
    public void checkRiskSubscriptions() throws ApiException {
        raw(api.checkRiskSubscriptions(Collections.emptyMap()));
    }

    public void registerPushToken(String token) throws ApiException {
        String trimmed = token == null ? "" : token.trim();
        if (trimmed.isEmpty()) {
            throw new ApiException(AppStrings.get(R.string.error_missing_push_credential));
        }
        data(
                api.registerPushToken(new PushTokenRequest(trimmed, "android")),
                AppStrings.get(R.string.label_push_register));
    }

    public void unregisterPushToken(String token) throws ApiException {
        String trimmed = token == null ? "" : token.trim();
        if (trimmed.isEmpty()) {
            return;
        }
        data(api.unregisterPushToken(trimmed), AppStrings.get(R.string.label_push_unregister));
    }

    private <T> T data(Call<ApiEnvelope<T>> call, String label) throws ApiException {
        Response<ApiEnvelope<T>> response;
        try {
            response = call.execute();
        } catch (IOException e) {
            throw new ApiException(AppStrings.get(R.string.error_network));
        }
        if (!response.isSuccessful()) {
            throw new ApiException(ApiErrors.message(response));
        }
        ApiEnvelope<T> envelope = response.body();
        if (envelope == null || envelope.getData() == null) {
            String message = envelope == null || envelope.getError() == null
                    ? null
                    : envelope.getError().getMessage();
            throw new ApiException(
                    message != null ? message : AppStrings.get(R.string.error_empty_data, label));
        }
        return envelope.getData();
    }

    private JsonObject raw(Call<JsonObject> call) throws ApiException {
        Response<JsonObject> response;
        try {
            response = call.execute();
        } catch (IOException e) {
            throw new ApiException(AppStrings.get(R.string.error_network));
        }
        if (!response.isSuccessful()) {
            throw new ApiException(ApiErrors.message(response));
        }
        JsonObject body = response.body();
        if (body == null) {
            throw new ApiException(
                    AppStrings.get(R.string.error_empty_data, AppStrings.get(R.string.label_risk_notifications)));
        }
        return body;
    }

    /** 便于界面层做本地过滤兜底。 */
    public static List<RiskNotificationDto> unreadOnly(List<RiskNotificationDto> source) {
        List<RiskNotificationDto> result = new ArrayList<>();
        if (source == null) {
            return result;
        }
        for (RiskNotificationDto item : source) {
            if (item != null && item.isUnread()) {
                result.add(item);
            }
        }
        return result;
    }
}
