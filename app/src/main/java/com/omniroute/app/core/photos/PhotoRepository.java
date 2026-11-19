package com.omniroute.app.core.photos;

import android.util.LruCache;

import com.omniroute.app.R;
import com.omniroute.app.core.AppStrings;
import com.omniroute.app.core.network.ApiClient;
import com.omniroute.app.core.network.PhotoApi;
import com.omniroute.app.core.network.dto.ApiErrors;
import com.omniroute.app.core.network.dto.ApiException;
import com.omniroute.app.core.network.dto.MyPhotosResponse;
import com.omniroute.app.core.network.dto.PhotoDto;
import com.omniroute.app.core.network.dto.PhotoUploadResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;

/**
 * 相册唯一入口。取图接口需要 Bearer，走 Retrofit 的 authorizedClient，
 * 因此这里能直接把二进制缓存成 byte[]（展示层再解码为 Bitmap）。
 */
public final class PhotoRepository {

    /** 网关单张上限 5MB，这里提前拦一次，避免白传。 */
    private static final int MAX_UPLOAD_BYTES = 5 * 1024 * 1024;

    private final PhotoApi api;
    private final LruCache<String, byte[]> cache = new LruCache<String, byte[]>(16 * 1024 * 1024) {
        @Override
        protected int sizeOf(String key, byte[] value) {
            return value == null ? 0 : value.length;
        }
    };

    public PhotoRepository(ApiClient apiClient) {
        this.api = apiClient.getPhotoApi();
    }

    /** 返回我的照片列表；enabled=false 表示服务端关掉了上传能力。 */
    public MyPhotos photos() throws ApiException {
        Response<MyPhotosResponse> response = execute(api.myPhotos(), R.string.photos_error_load);
        MyPhotosResponse body = response.body();
        if (body == null) {
            return new MyPhotos(true, new ArrayList<>());
        }
        return new MyPhotos(body.isEnabled(), body.getPhotos());
    }

    public PhotoDto upload(byte[] data, String fileName, String mimeType,
                           String nodeName, String tripId) throws ApiException {
        if (data == null || data.length == 0) {
            throw new ApiException(AppStrings.get(R.string.photos_error_empty));
        }
        if (data.length > MAX_UPLOAD_BYTES) {
            throw new ApiException(AppStrings.get(R.string.photos_error_too_large));
        }
        String safeType = mimeType == null || mimeType.trim().isEmpty() ? "image/jpeg" : mimeType;
        String safeName = fileName == null || fileName.trim().isEmpty() ? "photo.jpg" : fileName;
        RequestBody fileBody = RequestBody.create(MediaType.parse(safeType), data);
        MultipartBody.Part part = MultipartBody.Part.createFormData("file", safeName, fileBody);
        RequestBody node = textPart(nodeName);
        RequestBody trip = textPart(tripId);

        Response<PhotoUploadResponse> response = execute(api.upload(part, node, trip),
                R.string.photos_error_upload);
        PhotoUploadResponse body = response.body();
        if (body == null || body.getPhoto() == null) {
            throw new ApiException(AppStrings.get(R.string.photos_error_upload));
        }
        cache.remove(body.getPhoto().getId());
        return body.getPhoto();
    }

    /** 取图（带登录态）。命中缓存时不再发请求，列表滚动不会反复下载。 */
    public byte[] load(String id) throws ApiException {
        if (id == null || id.trim().isEmpty()) {
            throw new ApiException(AppStrings.get(R.string.photos_error_load));
        }
        byte[] cached = cache.get(id);
        if (cached != null) {
            return cached;
        }
        Response<ResponseBody> response;
        try {
            response = api.photo(id).execute();
        } catch (IOException e) {
            throw new ApiException(AppStrings.get(R.string.error_network));
        }
        if (!response.isSuccessful() || response.body() == null) {
            throw new ApiException(ApiErrors.localized(response));
        }
        try (ResponseBody body = response.body()) {
            byte[] bytes = body.bytes();
            cache.put(id, bytes);
            return bytes;
        } catch (IOException e) {
            throw new ApiException(AppStrings.get(R.string.error_network));
        }
    }

    /** 删除自己的照片；失败时缓存保持原样，下次仍能看到。 */
    public void delete(String id) throws ApiException {
        execute(api.delete(id), R.string.photos_error_delete);
        cache.remove(id);
    }

    private static RequestBody textPart(String value) {
        String safe = value == null ? "" : value.trim();
        return RequestBody.create(MediaType.parse("text/plain"), safe);
    }

    private <T> Response<T> execute(Call<T> call, int fallbackRes) throws ApiException {
        Response<T> response;
        try {
            response = call.execute();
        } catch (IOException e) {
            throw new ApiException(AppStrings.get(R.string.error_network));
        }
        if (!response.isSuccessful()) {
            String message = ApiErrors.localized(response);
            throw new ApiException(message == null || message.trim().isEmpty()
                    ? AppStrings.get(fallbackRes)
                    : message);
        }
        return response;
    }

    /** 列表结果：enabled 为服务端功能开关。 */
    public static final class MyPhotos {
        public final boolean enabled;
        public final List<PhotoDto> photos;

        public MyPhotos(boolean enabled, List<PhotoDto> photos) {
            this.enabled = enabled;
            this.photos = photos == null ? new ArrayList<>() : photos;
        }
    }
}
