package com.omniroute.app.core.network.dto;

import com.google.gson.annotations.SerializedName;

/** 上传结果：新照片一律 pending，服务端也会再给一句说明。 */
public final class PhotoUploadResponse {

    @SerializedName("photo")
    private PhotoDto photo;

    @SerializedName("status")
    private String status;

    @SerializedName("message")
    private String message;

    public PhotoDto getPhoto() {
        return photo;
    }

    public String getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}
