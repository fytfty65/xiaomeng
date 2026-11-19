package com.omniroute.app.core.feedback;

import com.omniroute.app.R;
import com.omniroute.app.core.AppStrings;
import com.omniroute.app.core.auth.AuthRepository;
import com.omniroute.app.core.network.ApiClient;
import com.omniroute.app.core.network.PlanningApi;
import com.omniroute.app.core.network.dto.ApiErrors;
import com.omniroute.app.core.network.dto.ApiException;
import com.omniroute.app.core.network.dto.FeedbackRequest;
import com.omniroute.app.core.network.dto.UserDto;
import com.omniroute.app.core.room.RoomSession;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Response;

/**
 * 路线反馈：写完会触发后端刷新用户画像，属于持续学习闭环的一部分。
 * 阻塞调用，需放在后台线程。
 */
public final class FeedbackRepository {

    private final PlanningApi api;
    private final AuthRepository authRepository;

    public FeedbackRepository(ApiClient apiClient, AuthRepository authRepository) {
        this.api = apiClient.getPlanningApi();
        this.authRepository = authRepository;
    }

    /**
     * @param target 反馈对象描述（例如「成都 3 日行程 · 全程方案」）
     * @param score  1 认可 / -1 需要调整
     */
    public void submit(RoomSession room, String target, int score, String reason) throws ApiException {
        UserDto user = authRepository.getUser();
        String userId = user == null || user.getId() == null ? "" : user.getId().trim();
        if (userId.isEmpty()) {
            throw new ApiException(AppStrings.get(R.string.error_session_expired));
        }
        if (score != 1 && score != -1) {
            throw new ApiException(AppStrings.get(R.string.feedback_invalid_score));
        }
        FeedbackRequest request = new FeedbackRequest(
                room.getRoomId(),
                userId,
                target,
                score,
                reason == null ? "" : reason.trim());
        execute(api.submitFeedback(request));
    }

    private <T> void execute(Call<T> call) throws ApiException {
        try {
            Response<T> response = call.execute();
            if (!response.isSuccessful()) {
                throw new ApiException(ApiErrors.message(response));
            }
        } catch (IOException e) {
            throw new ApiException(AppStrings.get(R.string.feedback_submit_failed));
        }
    }
}
