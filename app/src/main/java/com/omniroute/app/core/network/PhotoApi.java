package com.omniroute.app.core.network;

import com.omniroute.app.core.network.dto.MessageResponse;
import com.omniroute.app.core.network.dto.MyPhotosResponse;
import com.omniroute.app.core.network.dto.PhotoUploadResponse;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;

/** 用户实拍照片：上传 / 我的列表 / 取图 / 删除，全部需要登录态。 */
public interface PhotoApi {

    @Multipart
    @POST("api/v1/photos")
    Call<PhotoUploadResponse> upload(
            @Part MultipartBody.Part file,
            @Part("node_name") RequestBody nodeName,
            @Part("trip_id") RequestBody tripId);

    /** 列表走 /my/photos，避开 /photos/:id 的同级静态路由冲突。 */
    @GET("api/v1/my/photos")
    Call<MyPhotosResponse> myPhotos();

    /** 取图返回原始二进制；待审照片只有上传者本人能取到。 */
    @GET("api/v1/photos/{id}")
    Call<ResponseBody> photo(@Path("id") String id);

    @DELETE("api/v1/photos/{id}")
    Call<MessageResponse> delete(@Path("id") String id);
}
