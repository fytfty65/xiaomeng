package com.omniroute.app.core.network;

import android.util.Log;

import androidx.annotation.NonNull;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * 轻量日志拦截器：只记录方法、路径、状态码与耗时。
 * 刻意不输出任何请求/响应体和头部，确保 refresh token、密码不会进入 logcat。
 * 仅 debug 构建启用。
 */
public final class SafeLoggingInterceptor implements Interceptor {

    private static final String TAG = "OmniRouteHttp";

    private final boolean enabled;

    public SafeLoggingInterceptor(boolean enabled) {
        this.enabled = enabled;
    }

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request request = chain.request();
        if (!enabled) {
            return chain.proceed(request);
        }

        long startNs = System.nanoTime();
        Response response;
        try {
            response = chain.proceed(request);
        } catch (IOException e) {
            Log.w(TAG, request.method() + " " + request.url().encodedPath()
                    + " 网络异常: " + e.getClass().getSimpleName());
            throw e;
        }
        long costMs = (System.nanoTime() - startNs) / 1_000_000L;
        Log.d(TAG, request.method() + " " + request.url().encodedPath()
                + " -> " + response.code() + " (" + costMs + "ms)");
        return response;
    }
}
