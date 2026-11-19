package com.omniroute.app.core.network;

import com.omniroute.app.core.network.dto.MessageResponse;
import com.omniroute.app.core.network.dto.PlanVariantsResponse;
import com.omniroute.app.core.network.dto.SaveVariantResponse;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

/** 房间多方案：保存候选、拉取方案池、投票与采纳。 */
public interface VariantApi {

    @GET("api/v1/plan/variants")
    Call<PlanVariantsResponse> list(@Query("room_id") String roomId);

    @POST("api/v1/plan/variant")
    Call<SaveVariantResponse> save(@Body Map<String, Object> body);

    @POST("api/v1/plan/variant/vote")
    Call<MessageResponse> vote(@Body Map<String, Object> body);

    @POST("api/v1/plan/variant/adopt")
    Call<MessageResponse> adopt(@Body Map<String, Object> body);
}
