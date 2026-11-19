package com.omniroute.app.core.room;

import com.omniroute.app.R;
import com.omniroute.app.core.AppStrings;
import com.omniroute.app.core.auth.AuthRepository;
import com.omniroute.app.core.data.SettingsStore;
import com.omniroute.app.core.network.ApiClient;
import com.omniroute.app.core.network.PlanningApi;
import com.omniroute.app.core.network.dto.ApiErrors;
import com.omniroute.app.core.network.dto.ApiException;
import com.omniroute.app.core.network.dto.CreateRoomRequest;
import com.omniroute.app.core.network.dto.CreateRoomResponse;
import com.omniroute.app.core.network.dto.JoinRoomRequest;
import com.omniroute.app.core.network.dto.JoinRoomResponse;
import com.omniroute.app.core.network.dto.UserDto;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Response;

/**
 * 协作房间：创建 / 用邀请码加入 / 记住当前房间。
 *
 * <p>房间是规划的资源键，也是 WebSocket 的会话键，因此登录用户切换后
 * 必须丢弃上一个账号留下的房间，避免越权或串房。
 *
 * <p>所有方法均为阻塞调用。
 */
public final class RoomRepository {

    private static final String DEFAULT_ROOM_NAME = "OmniRoute 协作规划";
    private static final String ROLE_HOST = "主理人";
    private static final String ROLE_MEMBER = "成员";

    private final PlanningApi api;
    private final SettingsStore settingsStore;
    private final AuthRepository authRepository;

    private volatile RoomSession cached;

    public RoomRepository(ApiClient apiClient, SettingsStore settingsStore, AuthRepository authRepository) {
        this.api = apiClient.getPlanningApi();
        this.settingsStore = settingsStore;
        this.authRepository = authRepository;
    }

    public RoomSession currentRoom() {
        UserDto user = authRepository.getUser();
        String username = usernameOf(user);
        if (username.isEmpty()) {
            return null;
        }
        RoomSession cachedSession = cached;
        if (cachedSession != null) {
            return cachedSession;
        }
        String roomId = settingsStore.getRoomId();
        String owner = settingsStore.getRoomOwner();
        if (roomId.isEmpty() || !username.equals(owner)) {
            return null;
        }
        cached = new RoomSession(
                roomId,
                settingsStore.getRoomName(),
                settingsStore.getRoomInviteCode(),
                settingsStore.getRoomRole(),
                ROLE_HOST.equals(settingsStore.getRoomRole()));
        return cached;
    }

    /** 没有可用房间时创建一个，供「直接规划」路径使用。 */
    public RoomSession ensureRoom() throws ApiException {
        RoomSession existing = currentRoom();
        if (existing != null) {
            return existing;
        }
        return createRoom(DEFAULT_ROOM_NAME);
    }

    public RoomSession createRoom(String roomName) throws ApiException {
        String username = requireUsername();
        String trimmedName = roomName == null ? "" : roomName.trim();
        if (trimmedName.isEmpty()) {
            throw new ApiException(AppStrings.get(R.string.room_error_name_required));
        }
        Response<CreateRoomResponse> response =
                execute(api.createRoom(new CreateRoomRequest(username, trimmedName)));
        CreateRoomResponse body = response.body();
        String roomId = body == null || body.getRoomId() == null ? "" : body.getRoomId().trim();
        if (roomId.isEmpty()) {
            throw new ApiException(AppStrings.get(R.string.room_create_failed));
        }
        String inviteCode = body.getInviteCode() == null ? roomId : body.getInviteCode().trim();
        RoomSession session = new RoomSession(roomId, trimmedName, inviteCode, ROLE_HOST, true);
        persist(session, username);
        return session;
    }

    public RoomSession joinRoom(String inviteCode) throws ApiException {
        String username = requireUsername();
        String code = inviteCode == null ? "" : inviteCode.trim().toUpperCase();
        if (code.length() != 6) {
            throw new ApiException(AppStrings.get(R.string.room_error_invite_invalid));
        }
        Response<JoinRoomResponse> response =
                execute(api.joinRoom(new JoinRoomRequest(username, code, ROLE_MEMBER)));
        JoinRoomResponse body = response.body();
        String roomId = body == null || body.getRoomId() == null ? "" : body.getRoomId().trim();
        if (roomId.isEmpty()) {
            throw new ApiException(AppStrings.get(R.string.room_join_failed));
        }
        String roomName = body.getRoomName() == null ? "协作房间" : body.getRoomName().trim();
        RoomSession session = new RoomSession(roomId, roomName, code, ROLE_MEMBER, false);
        persist(session, username);
        return session;
    }

    /** 退出当前房间（仅清本地引用，后端没有解散房间接口）。 */
    public void clear() {
        cached = null;
        settingsStore.clearRoom();
    }

    private void persist(RoomSession session, String username) {
        cached = session;
        settingsStore.saveRoom(
                session.getRoomId(),
                session.getRoomName(),
                session.getInviteCode(),
                username,
                session.getRole());
    }

    private String requireUsername() throws ApiException {
        String username = usernameOf(authRepository.getUser());
        if (username.isEmpty()) {
            throw new ApiException(AppStrings.get(R.string.error_session_expired));
        }
        return username;
    }

    private static String usernameOf(UserDto user) {
        if (user == null || user.getUsername() == null) {
            return "";
        }
        return user.getUsername().trim();
    }

    private <T> Response<T> execute(Call<T> call) throws ApiException {
        try {
            Response<T> response = call.execute();
            if (!response.isSuccessful()) {
                throw new ApiException(ApiErrors.message(response));
            }
            return response;
        } catch (IOException e) {
            throw new ApiException(AppStrings.get(R.string.error_network));
        }
    }
}
