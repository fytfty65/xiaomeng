package com.omniroute.app.core.auth;

import android.os.Handler;
import android.os.Looper;

import com.omniroute.app.R;
import com.omniroute.app.core.AppStrings;
import com.omniroute.app.core.network.ApiClient;
import com.omniroute.app.core.network.dto.ApiErrors;
import com.omniroute.app.core.network.dto.ApiException;
import com.omniroute.app.core.network.dto.AuthResponse;
import com.omniroute.app.core.network.dto.AvatarUploadResponse;
import com.omniroute.app.core.network.dto.ChangePasswordRequest;
import com.omniroute.app.core.network.dto.ForgotPasswordRequest;
import com.omniroute.app.core.network.dto.ForgotPasswordResponse;
import com.omniroute.app.core.network.dto.LoginRequest;
import com.omniroute.app.core.network.dto.LogoutRequest;
import com.omniroute.app.core.network.dto.MessageResponse;
import com.omniroute.app.core.network.dto.RefreshRequest;
import com.omniroute.app.core.network.dto.RegisterRequest;
import com.omniroute.app.core.network.dto.RegisterResponse;
import com.omniroute.app.core.network.dto.ResetPasswordRequest;
import com.omniroute.app.core.network.dto.UpdateProfileRequest;
import com.omniroute.app.core.network.dto.UserDto;

import java.io.IOException;
import java.util.concurrent.locks.ReentrantLock;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Response;

/**
 * 鉴权仓库：登录/注册/刷新/登出，以及 401 触发的单飞刷新。
 * 刷新失败会清空本地会话并通过 {@link SessionExpiredListener} 通知 UI 回到登录页。
 *
 * <p>方法均为阻塞调用，调用方需要放在后台线程（OkHttp 要求在非主线程执行同步请求）。
 */
public final class AuthRepository {

    public interface UserListener {
        void onUserChanged(UserDto user);
    }

    public interface SessionExpiredListener {
        void onSessionExpired();
    }

    private final ApiClient apiClient;
    private final TokenStore tokenStore;
    private final ReentrantLock refreshLock = new ReentrantLock();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private volatile UserDto user;
    private volatile UserListener userListener;
    private volatile SessionExpiredListener sessionExpiredListener;

    public AuthRepository(ApiClient apiClient, TokenStore tokenStore) {
        this.apiClient = apiClient;
        this.tokenStore = tokenStore;
        // OkHttp Authenticator 运行在后台线程，这里同步等待刷新结果。
        apiClient.setRefresher(this::refreshBlocking);
    }

    public UserDto login(String username, String password) throws ApiException {
        String trimmedUsername = username == null ? "" : username.trim();
        Response<AuthResponse> response;
        try {
            response = apiClient.getAuthApi().login(new LoginRequest(trimmedUsername, password)).execute();
        } catch (IOException e) {
            throw new ApiException(AppStrings.get(R.string.error_network));
        }
        AuthResponse body = response.body();
        if (!response.isSuccessful() || body == null) {
            throw new ApiException(ApiErrors.message(response));
        }
        UserDto loggedInUser = body.getUser();
        if (loggedInUser == null) {
            throw new ApiException(AppStrings.get(R.string.auth_response_missing_user));
        }
        tokenStore.save(body.getAccessToken(), body.getRefreshToken());
        publishUser(loggedInUser);
        return loggedInUser;
    }

    public String register(String username, String password, String nickname) throws ApiException {
        String trimmedUsername = username == null ? "" : username.trim();
        String trimmedNickname = nickname == null ? null : nickname.trim();
        if (trimmedNickname != null && trimmedNickname.isEmpty()) {
            trimmedNickname = null;
        }
        Response<RegisterResponse> response;
        try {
            response = apiClient.getAuthApi()
                    .register(new RegisterRequest(trimmedUsername, password, trimmedNickname))
                    .execute();
        } catch (IOException e) {
            throw new ApiException(AppStrings.get(R.string.error_network));
        }
        if (!response.isSuccessful()) {
            throw new ApiException(ApiErrors.message(response));
        }
        RegisterResponse body = response.body();
        String message = body == null ? null : body.getMessage();
        if (message == null || message.trim().isEmpty()) {
            return AppStrings.get(R.string.auth_register_success);
        }
        return message;
    }

    /** 启动时静默刷新：有 refresh token 才尝试，失败即回登录页。 */
    public boolean refreshSilent() {
        String refreshToken = tokenStore.currentRefreshToken();
        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            return false;
        }
        return refreshBlocking(tokenStore.currentAccessToken()) != null;
    }

    public UserDto loadProfile() throws ApiException {
        Response<com.omniroute.app.core.network.dto.ProfileResponse> response;
        try {
            response = apiClient.getApi().profile().execute();
        } catch (IOException e) {
            throw new ApiException(AppStrings.get(R.string.error_network));
        }
        com.omniroute.app.core.network.dto.ProfileResponse body = response.body();
        if (!response.isSuccessful() || body == null) {
            throw new ApiException(ApiErrors.message(response));
        }
        UserDto profile = body.getUser();
        if (profile == null) {
            throw new ApiException(AppStrings.get(R.string.auth_profile_missing_user));
        }
        publishUser(profile);
        return profile;
    }

    /** 更新昵称与签名；服务端更新接口不回传 user，这里再拉一次资料并刷新本地快照。 */
    public UserDto updateProfile(String nickname, String signature) throws ApiException {
        String trimmedNickname = nickname == null ? "" : nickname.trim();
        String trimmedSignature = signature == null ? "" : signature.trim();
        if (trimmedNickname.isEmpty() && trimmedSignature.isEmpty()) {
            throw new ApiException(AppStrings.get(R.string.auth_profile_empty));
        }
        Response<Object> response;
        try {
            response = apiClient.getAuthApi()
                    .updateProfile(new UpdateProfileRequest(trimmedNickname, trimmedSignature))
                    .execute();
        } catch (IOException e) {
            throw new ApiException(AppStrings.get(R.string.error_network));
        }
        if (!response.isSuccessful()) {
            throw new ApiException(ApiErrors.message(response));
        }
        return loadProfile();
    }

    /** 请求重置密码；开发环境会直接返回 reset_token，生产环境只返回统一提示。 */
    public ForgotPasswordResponse forgotPassword(String username) throws ApiException {
        String name = username == null ? "" : username.trim();
        if (name.isEmpty()) {
            throw new ApiException(AppStrings.get(R.string.auth_error_username_required));
        }
        Response<ForgotPasswordResponse> response;
        try {
            response = apiClient.getAuthApi()
                    .forgotPassword(new ForgotPasswordRequest(name))
                    .execute();
        } catch (IOException e) {
            throw new ApiException(AppStrings.get(R.string.error_network));
        }
        if (!response.isSuccessful() || response.body() == null) {
            throw new ApiException(ApiErrors.localized(response));
        }
        return response.body();
    }

    /** 用一次性令牌重置密码；成功后服务端撤销全部会话，调用方应回到登录页。 */
    public String resetPassword(String token, String newPassword) throws ApiException {
        String safeToken = token == null ? "" : token.trim();
        validateNewPassword(newPassword);
        Response<MessageResponse> response;
        try {
            response = apiClient.getAuthApi()
                    .resetPassword(new ResetPasswordRequest(safeToken, newPassword))
                    .execute();
        } catch (IOException e) {
            throw new ApiException(AppStrings.get(R.string.error_network));
        }
        if (!response.isSuccessful()) {
            throw new ApiException(ApiErrors.localized(response));
        }
        MessageResponse body = response.body();
        String message = body == null ? null : body.getMessage();
        return message == null || message.trim().isEmpty()
                ? AppStrings.get(R.string.auth_password_reset_done)
                : message;
    }

    /** 修改密码（需登录）；成功后其他设备会被强制退出。 */
    public String changePassword(String currentPassword, String newPassword) throws ApiException {
        String current = currentPassword == null ? "" : currentPassword;
        if (current.isEmpty()) {
            throw new ApiException(AppStrings.get(R.string.auth_error_password_change_incomplete));
        }
        validateNewPassword(newPassword);
        Response<MessageResponse> response;
        try {
            response = apiClient.getApi()
                    .changePassword(new ChangePasswordRequest(current, newPassword))
                    .execute();
        } catch (IOException e) {
            throw new ApiException(AppStrings.get(R.string.error_network));
        }
        if (!response.isSuccessful()) {
            throw new ApiException(ApiErrors.localized(response));
        }
        MessageResponse body = response.body();
        String message = body == null ? null : body.getMessage();
        return message == null || message.trim().isEmpty()
                ? AppStrings.get(R.string.auth_password_changed)
                : message;
    }

    /** 上传头像并刷新本地用户快照，返回带新 avatar_url 的用户。 */
    public UserDto uploadAvatar(byte[] data, String fileName, String mimeType) throws ApiException {
        if (data == null || data.length == 0) {
            throw new ApiException(AppStrings.get(R.string.auth_error_avatar_empty));
        }
        String safeType = mimeType == null || mimeType.trim().isEmpty() ? "image/jpeg" : mimeType;
        String safeName = fileName == null || fileName.trim().isEmpty() ? "avatar.jpg" : fileName;
        RequestBody body = RequestBody.create(MediaType.parse(safeType), data);
        MultipartBody.Part part = MultipartBody.Part.createFormData("file", safeName, body);
        Response<AvatarUploadResponse> response;
        try {
            response = apiClient.getApi().uploadAvatar(part).execute();
        } catch (IOException e) {
            throw new ApiException(AppStrings.get(R.string.error_network));
        }
        if (!response.isSuccessful()) {
            throw new ApiException(ApiErrors.localized(response));
        }
        return loadProfile();
    }

    private void validateNewPassword(String newPassword) throws ApiException {
        String value = newPassword == null ? "" : newPassword;
        if (value.length() < 6 || value.length() > 72) {
            throw new ApiException(AppStrings.get(R.string.auth_error_password_length));
        }
    }

    public void logout() {
        String refreshToken = tokenStore.currentRefreshToken();
        try {
            apiClient.getAuthApi()
                    .logout(new LogoutRequest(refreshToken == null ? "" : refreshToken))
                    .execute();
        } catch (IOException ignored) {
            // 服务端撤销失败不影响本地退出，避免用户被困在已失效的会话里。
        }
        tokenStore.clear();
        publishUser(null);
    }

    public UserDto getUser() {
        return user;
    }

    public void setUserListener(UserListener listener) {
        this.userListener = listener;
        if (listener != null) {
            listener.onUserChanged(user);
        }
    }

    public void setSessionExpiredListener(SessionExpiredListener listener) {
        this.sessionExpiredListener = listener;
    }

    /**
     * 供 OkHttp Authenticator 调用的同步刷新。
     * 用锁保证并发 401 只真正刷新一次；若其他请求已经换到新 token 则直接复用。
     */
    private String refreshBlocking(String staleAccessToken) {
        refreshLock.lock();
        try {
            String current = tokenStore.currentAccessToken();
            if (current != null && !current.equals(staleAccessToken)) {
                return current;
            }
            return performRefresh();
        } finally {
            refreshLock.unlock();
        }
    }

    private String performRefresh() {
        String refreshToken = tokenStore.currentRefreshToken();
        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            clearSessionAndNotify();
            return null;
        }

        Response<AuthResponse> response;
        try {
            response = apiClient.getAuthApi().refresh(new RefreshRequest(refreshToken)).execute();
        } catch (IOException e) {
            response = null;
        }
        return applyAuthResponse(response);
    }

    private String applyAuthResponse(Response<AuthResponse> response) {
        if (response == null || !response.isSuccessful() || response.body() == null) {
            clearSessionAndNotify();
            return null;
        }
        AuthResponse body = response.body();
        tokenStore.save(body.getAccessToken(), body.getRefreshToken());
        publishUser(body.getUser());
        return body.getAccessToken();
    }

    private void clearSessionAndNotify() {
        tokenStore.clear();
        publishUser(null);

        SessionExpiredListener listener = sessionExpiredListener;
        if (listener != null) {
            mainHandler.post(listener::onSessionExpired);
        }
    }

    private void publishUser(UserDto next) {
        this.user = next;
        UserListener listener = userListener;
        if (listener != null) {
            mainHandler.post(() -> listener.onUserChanged(next));
        }
    }
}
