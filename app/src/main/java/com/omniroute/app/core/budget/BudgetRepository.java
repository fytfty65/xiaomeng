package com.omniroute.app.core.budget;

import com.omniroute.app.R;
import com.omniroute.app.core.AppStrings;
import com.omniroute.app.core.network.ApiClient;
import com.omniroute.app.core.network.BudgetApi;
import com.omniroute.app.core.network.dto.ApiErrors;
import com.omniroute.app.core.network.dto.ApiException;
import com.omniroute.app.core.network.dto.BudgetSummaryDto;
import com.omniroute.app.core.network.dto.BudgetPredictResponse;
import com.omniroute.app.core.network.dto.BudgetSummaryResponse;
import com.omniroute.app.core.network.dto.ExpenseDto;
import com.omniroute.app.core.network.dto.OcrExpenseResponse;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Response;

/** 预算管家唯一入口：预算、记账、汇总。金额全部是整数分。 */
public final class BudgetRepository {

    private final BudgetApi api;

    public BudgetRepository(ApiClient apiClient) {
        this.api = apiClient.getBudgetApi();
    }

    public BudgetSummaryDto summary(String tripId) throws ApiException {
        Response<BudgetSummaryResponse> response = execute(
                api.summary(tripId), R.string.budget_error_load);
        return response.body() == null
                ? new BudgetSummaryDto()
                : response.body().summaryOrEmpty();
    }

    public List<ExpenseDto> expenses(String tripId) throws ApiException {
        Response<BudgetSummaryResponse.ExpensesResponse> response = execute(
                api.expenses(tripId), R.string.budget_error_load);
        return response.body() == null
                ? new java.util.ArrayList<>()
                : response.body().expensesOrEmpty();
    }

    public void setBudget(String tripId, long totalCents, String currency) throws ApiException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("trip_id", tripId);
        body.put("total_budget_cents", totalCents);
        body.put("currency", currency);
        execute(api.setBudget(body), R.string.budget_error_save);
    }

    /** 记录后服务端立即重算汇总，返回值可直接刷新预警，不必再拉一次。 */
    public BudgetSummaryDto addExpense(String tripId, String category, long amountCents,
                                       String currency, String note) throws ApiException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("trip_id", tripId);
        body.put("category", category);
        body.put("amount_cents", amountCents);
        body.put("currency", currency);
        body.put("note", note == null ? "" : note);
        Response<BudgetSummaryResponse.AddExpenseResponse> response = execute(
                api.addExpense(body), R.string.budget_error_add);
        if (response.body() == null || response.body().summary == null) {
            throw new ApiException(AppStrings.get(R.string.budget_error_add));
        }
        return response.body().summary;
    }

    /** 小票记账：把 OCR 文本交给网关解析金额与分类，成功后返回整条响应（含回显信息）。 */
    public OcrExpenseResponse ocrExpense(String tripId, String ocrText, String currency)
            throws ApiException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("trip_id", tripId);
        body.put("ocr_text", ocrText);
        body.put("currency", currency);
        Response<OcrExpenseResponse> response = execute(
                api.ocrExpense(body), R.string.budget_ocr_fail);
        if (response.body() == null || response.body().summary == null) {
            throw new ApiException(AppStrings.get(R.string.budget_ocr_fail));
        }
        return response.body();
    }

    /** 规划阶段的参考预算（元）：按目的地城市档位 × 天数估算，不落库。 */
    public double predict(String city, int days) throws ApiException {
        Response<BudgetPredictResponse> response = execute(
                api.predict(city, days), R.string.budget_predict_failed);
        if (response.body() == null) {
            throw new ApiException(AppStrings.get(R.string.budget_predict_failed));
        }
        return response.body().estimatedCost;
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
}
