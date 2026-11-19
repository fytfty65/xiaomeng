package com.omniroute.app.core.network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * 全局 JSON 配置。后端会持续演进字段，客户端只声明自己需要的部分，
 * Gson 默认忽略未知字段，避免多一个字段就解析失败。
 */
public final class AppJson {

    private static final Gson GSON = new GsonBuilder().create();

    private AppJson() {
    }

    public static Gson gson() {
        return GSON;
    }
}
