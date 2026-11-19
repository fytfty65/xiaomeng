package com.omniroute.app.core.network.dto;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/** /api/v1/my/photos 的响应：enabled 为 false 时表示服务端关掉了照片上传。 */
public final class MyPhotosResponse {

    @SerializedName("photos")
    private List<PhotoDto> photos;

    @SerializedName("enabled")
    private boolean enabled = true;

    public List<PhotoDto> getPhotos() {
        return photos == null ? new ArrayList<>() : photos;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
