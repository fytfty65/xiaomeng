package com.omniroute.app.core.network.dto;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.omniroute.app.R;
import com.omniroute.app.core.AppStrings;
import com.omniroute.app.core.network.AppJson;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import retrofit2.Response;

/**
 * 把后端真实的错误体转成可展示文案，保留 error 与 code。
 * 兼容两种形状：
 * - 鉴权/资源校验的扁平 {"error","code"}；
 * - /api/v1/planning/* 的 Envelope {"error":{"code","message"}}。
 * 解析失败或没有错误体时退化为 HTTP 状态码，绝不伪造成功。
 */
public final class ApiErrors {

    /** 已知错误码 → 本地化文案。未知错误码仍展示服务端 message，避免吞掉细节。 */
    private static final Map<String, Integer> LOCALIZED = new HashMap<>();

    static {
        LOCALIZED.put("PASSWORD_LENGTH_INVALID", R.string.auth_error_password_length);
        LOCALIZED.put("INVALID_CURRENT_PASSWORD", R.string.auth_error_current_password);
        LOCALIZED.put("INVALID_PASSWORD_CHANGE_REQUEST", R.string.auth_error_password_change_incomplete);
        LOCALIZED.put("INVALID_PASSWORD_RESET_REQUEST", R.string.auth_error_password_reset_incomplete);
        LOCALIZED.put("RESET_TOKEN_INVALID", R.string.auth_error_reset_token_invalid);
        LOCALIZED.put("RESET_TOKEN_EXPIRED", R.string.auth_error_reset_token_expired);
        LOCALIZED.put("RESET_DELIVERY_UNAVAILABLE", R.string.auth_error_reset_unavailable);
        LOCALIZED.put("RESET_DELIVERY_FAILED", R.string.auth_error_reset_delivery_failed);
        LOCALIZED.put("USERNAME_REQUIRED", R.string.auth_error_username_required);
        LOCALIZED.put("USERNAME_TOO_LONG", R.string.auth_error_username_too_long);
        LOCALIZED.put("USERNAME_TAKEN", R.string.auth_error_username_taken);
        LOCALIZED.put("INVALID_CREDENTIALS", R.string.auth_error_invalid_credentials);
        LOCALIZED.put("ACCOUNT_TEMPORARILY_LOCKED", R.string.auth_error_account_locked);
        LOCALIZED.put("INVALID_REGISTRATION_REQUEST", R.string.auth_error_registration_invalid);
        LOCALIZED.put("AUTH_REQUIRED", R.string.error_session_expired);
        LOCALIZED.put("USER_MISMATCH", R.string.auth_error_user_mismatch);
        LOCALIZED.put("PAYLOAD_TOO_LARGE", R.string.auth_error_payload_too_large);
        LOCALIZED.put("INVALID_BODY", R.string.auth_error_invalid_body);
        LOCALIZED.put("SESSION_REVOKE_FAILED", R.string.auth_error_session_revoke);
    }

    private ApiErrors() {
    }

    /**
     * 业务动作优先使用：已知错误码输出中文文案，未知错误码保留后端原文，
     * 这样新增能力不会出现「只有 code、没有可读原因」。
     */
    public static String localized(Response<?> response) {
        Parsed parsed = parse(response);
        if (parsed.code != null) {
            Integer resId = LOCALIZED.get(parsed.code);
            if (resId != null) {
                return AppStrings.get(resId);
            }
        }
        return message(response);
    }

    public static String message(Response<?> response) {
        Parsed parsed = parse(response);
        if (parsed.text != null && parsed.code != null) {
            return AppStrings.get(R.string.error_with_code, parsed.text, parsed.code);
        }
        if (parsed.text != null) {
            return parsed.text;
        }
        if (parsed.code != null) {
            return AppStrings.get(R.string.error_request_code, parsed.code);
        }
        return AppStrings.get(R.string.error_request_http, response.code());
    }

    private static Parsed parse(Response<?> response) {
        Parsed parsed = new Parsed();
        JsonObject root = parseErrorBody(response);
        if (root == null) {
            return parsed;
        }
        JsonElement error = root.get("error");
        if (error != null && error.isJsonObject()) {
            JsonObject detail = error.getAsJsonObject();
            parsed.text = stringValue(detail, "message");
            parsed.code = stringValue(detail, "code");
        } else if (error != null && error.isJsonPrimitive()) {
            parsed.text = stringValue(root, "error");
            parsed.code = stringValue(root, "code");
        }
        return parsed;
    }

    private static JsonObject parseErrorBody(Response<?> response) {
        try {
            String raw = response.errorBody() == null ? null : response.errorBody().string();
            if (raw == null || raw.trim().isEmpty()) {
                return null;
            }
            return AppJson.gson().fromJson(raw, JsonObject.class);
        } catch (IOException | JsonSyntaxException | IllegalStateException ignored) {
            // 错误体不可解析时退回状态码描述。
            return null;
        }
    }

    private static String stringValue(JsonObject object, String key) {
        JsonElement element = object.get(key);
        if (element == null || element.isJsonNull() || !element.isJsonPrimitive()) {
            return null;
        }
        String value = element.getAsString();
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static final class Parsed {
        String text;
        String code;
    }
}
