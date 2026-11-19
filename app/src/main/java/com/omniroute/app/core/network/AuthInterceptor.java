package com.omniroute.app.core.network;

import androidx.annotation.NonNull;

import com.omniroute.app.core.auth.TokenStore;

import java.io.IOException;
import java.util.UUID;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/** 给每个请求注入 Bearer 与 X-Request-Id；没有 access token 时只带请求 ID。 */
public final class AuthInterceptor implements Interceptor {

    private final TokenStore tokenStore;

    public AuthInterceptor(TokenStore tokenStore) {
        this.tokenStore = tokenStore;
    }

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request.Builder builder = chain.request().newBuilder()
                .header("X-Request-Id", UUID.randomUUID().toString());

        String accessToken = tokenStore.currentAccessToken();
        if (accessToken != null && !accessToken.trim().isEmpty()) {
            builder.header("Authorization", "Bearer " + accessToken);
        }

        return chain.proceed(builder.build());
    }
}
