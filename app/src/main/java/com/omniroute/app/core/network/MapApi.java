package com.omniroute.app.core.network;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * 真实底图接口。
 *
 * <p>网关只返回底图 PNG；折线、按天配色与停靠点编号由客户端绘制，
 * 因此这里不传 paths/markers，避免把样式绑死在高德参数格式上。
 */
public interface MapApi {

    @GET("api/v1/map/static")
    Call<ResponseBody> staticMap(
            @Query("location") String location,
            @Query("zoom") int zoom,
            @Query("size") String size);
}
