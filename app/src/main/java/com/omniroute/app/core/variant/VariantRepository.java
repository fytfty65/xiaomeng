package com.omniroute.app.core.variant;

import com.omniroute.app.R;
import com.omniroute.app.core.AppStrings;
import com.omniroute.app.core.auth.AuthRepository;
import com.omniroute.app.core.network.ApiClient;
import com.omniroute.app.core.network.VariantApi;
import com.omniroute.app.core.network.dto.ApiErrors;
import com.omniroute.app.core.network.dto.ApiException;
import com.omniroute.app.core.network.dto.PlanVariantDto;
import com.omniroute.app.core.network.dto.PlanVariantsResponse;
import com.omniroute.app.core.network.dto.UserDto;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Response;

/**
 * 房间多方案投票。
 *
 * <p>方案池以 room_id 为键，网关负责一人一票与实时广播；
 * 这里只做阻塞调用，线程切换由 ViewModel 负责。
 */
public final class VariantRepository {

    private final VariantApi api;
    private final AuthRepository authRepository;

    public VariantRepository(ApiClient apiClient, AuthRepository authRepository) {
        this.api = apiClient.getVariantApi();
        this.authRepository = authRepository;
    }

    public List<PlanVariantDto> list(String roomId) throws ApiException {
        Response<PlanVariantsResponse> response = execute(
                api.list(roomId), R.string.variant_error_load);
        if (response.body() == null) {
            return Collections.emptyList();
        }
        return response.body().variantsOrEmpty();
    }

    public void save(String roomId, String name, String style, String route) throws ApiException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("room_id", roomId);
        body.put("name", name);
        body.put("style", style == null ? "" : style);
        body.put("route", route);
        body.put("created_by", userId());
        execute(api.save(body), R.string.variant_error_save);
    }

    public void vote(String variantId) throws ApiException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("variant_id", variantId);
        body.put("user_id", userId());
        execute(api.vote(body), R.string.variant_error_vote);
    }

    /** 采纳后网关会用路线节点费用建立预算基线，此处只关心是否成功。 */
    public void adopt(String variantId) throws ApiException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("variant_id", variantId);
        body.put("user_id", userId());
        execute(api.adopt(body), R.string.variant_error_adopt);
    }

    private String userId() throws ApiException {
        UserDto user = authRepository.getUser();
        String id = user == null || user.getId() == null ? "" : user.getId().trim();
        if (id.isEmpty()) {
            throw new ApiException(AppStrings.get(R.string.error_session_expired));
        }
        return id;
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
