package com.omniroute.app.core.network;

import com.omniroute.app.core.network.dto.BudgetSummaryResponse;
import com.omniroute.app.core.network.dto.BudgetPredictResponse;
import com.omniroute.app.core.network.dto.OcrExpenseResponse;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

/** 预算管家：预算设置、记账、汇总。金额一律以整数分传输。 */
public interface BudgetApi {

    @POST("api/v1/budget")
    Call<BudgetSummaryResponse.SetBudgetResponse> setBudget(@Body Map<String, Object> body);

    @POST("api/v1/expense")
    Call<BudgetSummaryResponse.AddExpenseResponse> addExpense(@Body Map<String, Object> body);

    /** 小票文本由客户端 OCR 产出，网关只负责金额与分类解析。 */
    @POST("api/v1/expense/ocr")
    Call<OcrExpenseResponse> ocrExpense(@Body Map<String, Object> body);

    @GET("api/v1/expenses")
    Call<BudgetSummaryResponse.ExpensesResponse> expenses(@Query("trip_id") String tripId);

    @GET("api/v1/budget/summary")
    Call<BudgetSummaryResponse> summary(@Query("trip_id") String tripId);

    /** 规划阶段参考预算：网关按城市档位 × 天数估算。 */
    @GET("api/v1/budget/predict")
    Call<BudgetPredictResponse> predict(@Query("city") String city, @Query("days") int days);
}
