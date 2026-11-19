package com.omniroute.app.core.network;

import com.omniroute.app.core.network.dto.AuthResponse;
import com.omniroute.app.core.network.dto.AvatarUploadResponse;
import com.omniroute.app.core.network.dto.ChangePasswordRequest;
import com.omniroute.app.core.network.dto.ForgotPasswordRequest;
import com.omniroute.app.core.network.dto.ForgotPasswordResponse;
import com.omniroute.app.core.network.dto.LoginRequest;
import com.omniroute.app.core.network.dto.LogoutRequest;
import com.omniroute.app.core.network.dto.LogoutResponse;
import com.omniroute.app.core.network.dto.MessageResponse;
import com.omniroute.app.core.network.dto.ProfileResponse;
import com.omniroute.app.core.network.dto.RefreshRequest;
import com.omniroute.app.core.network.dto.RegisterRequest;
import com.omniroute.app.core.network.dto.RegisterResponse;
import com.omniroute.app.core.network.dto.ResetPasswordRequest;
import com.omniroute.app.core.network.dto.UpdateProfileRequest;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;

/** Gateway 的移动端鉴权与个人资料接口，路径见 gateway/internal/api/route.go。 */
public interface AuthApi {

    @POST("api/auth/mobile/login")
    Call<AuthResponse> login(@Body LoginRequest body);

    @POST("api/auth/mobile/refresh")
    Call<AuthResponse> refresh(@Body RefreshRequest body);

    @POST("api/auth/mobile/logout")
    Call<LogoutResponse> logout(@Body LogoutRequest body);

    @POST("api/auth/register")
    Call<RegisterResponse> register(@Body RegisterRequest body);

    @GET("api/user/profile")
    Call<ProfileResponse> profile();

    /** 更新昵称与签名；返回体不含 user，调用方需再拉一次 profile 同步本地快照。 */
    @PUT("api/user/profile")
    Call<Object> updateProfile(@Body UpdateProfileRequest body);

    /** 忘记密码：生产环境只返回统一提示，开发环境直接返回 reset_token。 */
    @POST("api/auth/password/forgot")
    Call<ForgotPasswordResponse> forgotPassword(@Body ForgotPasswordRequest body);

    /** 用一次性令牌重置密码；成功后服务端会撤销该账号的全部会话。 */
    @POST("api/auth/password/reset")
    Call<MessageResponse> resetPassword(@Body ResetPasswordRequest body);

    /** 修改密码（需登录）；成功后除当前设备外的会话都会被撤销。 */
    @POST("api/auth/password/change")
    Call<MessageResponse> changePassword(@Body ChangePasswordRequest body);

    /** 上传头像（multipart 字段名必须是 file，JPG/PNG ≤5MB）。 */
    @Multipart
    @POST("api/user/avatar/upload")
    Call<AvatarUploadResponse> uploadAvatar(@Part MultipartBody.Part file);
}
