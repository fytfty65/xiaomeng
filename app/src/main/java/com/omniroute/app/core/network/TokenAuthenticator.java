package com.omniroute.app.core.network;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import okhttp3.Authenticator;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;

/**
 * 处理 401：请求失败的 token 交给 AuthRepository 单飞刷新。
 * 重试请求打上标记，避免刷新后再次 401 时无限循环。
 */
public final class TokenAuthenticator implements Authenticator {

    private static final String RETRY_HEADER = "X-OmniRoute-Auth-Retry";

    private final ApiClient.TokenRefresher refresher;

    public TokenAuthenticator(ApiClient.TokenRefresher refresher) {
        this.refresher = refresher;
    }

    @Nullable
    @Override
    public Request authenticate(@Nullable Route route, @NonNull Response response) {
        if (response.request().header(RETRY_HEADER) != null) {
            return null;
        }

        String staleToken = null;
        String header = response.request().header("Authorization");
        if (header != null) {
            String candidate = header.startsWith("Bearer ") ? header.substring("Bearer ".length()) : header;
            candidate = candidate.trim();
            if (!candidate.isEmpty()) {
                staleToken = candidate;
            }
        }

        String freshToken = refresher.refresh(staleToken);
        if (freshToken == null) {
            return null;
        }

        return response.request().newBuilder()
                .header("Authorization", "Bearer " + freshToken)
                .header(RETRY_HEADER, "1")
                .build();
    }
}
