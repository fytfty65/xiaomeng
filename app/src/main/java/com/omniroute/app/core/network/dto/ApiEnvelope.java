package com.omniroute.app.core.network.dto;

import com.google.gson.annotations.SerializedName;

/**
 * /api/v1/planning 系列的统一响应封套：{data, meta}，失败时另有 error。
 * 客户端只声明需要的段，未知字段由 Gson 忽略。
 */
public final class ApiEnvelope<T> {

    @SerializedName("data")
    private T data;

    @SerializedName("meta")
    private ApiMetaDto meta;

    @SerializedName("error")
    private ApiErrorBodyDto error;

    public T getData() {
        return data;
    }

    public ApiMetaDto getMeta() {
        return meta;
    }

    public ApiErrorBodyDto getError() {
        return error;
    }
}
