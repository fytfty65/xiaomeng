package com.omniroute.app.core.data;

import android.content.Context;
import android.content.SharedPreferences;

/** 非敏感偏好设置。令牌不放在这里（见 TokenStore）。 */
public final class SettingsStore {

    private static final String PREFS_NAME = "omniroute_settings";
    private static final String KEY_LAST_USERNAME = "last_username";
    private static final String KEY_DYNAMIC_COLOR = "dynamic_color";
    private static final String KEY_ROOM_ID = "room_id";
    private static final String KEY_ROOM_NAME = "room_name";
    private static final String KEY_ROOM_INVITE = "room_invite";
    private static final String KEY_ROOM_OWNER = "room_owner";
    private static final String KEY_ROOM_ROLE = "room_role";
    private static final String KEY_JOURNEY_SEASON = "journey_season";

    private final SharedPreferences prefs;

    public SettingsStore(Context context) {
        this.prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /** 上次登录成功的用户名，用于登录页预填。 */
    public String getLastUsername() {
        return prefs.getString(KEY_LAST_USERNAME, "");
    }

    public void setLastUsername(String username) {
        prefs.edit().putString(KEY_LAST_USERNAME, username).apply();
    }

    /** 是否跟随系统动态取色（Android 12+ 生效）。默认关闭：品牌界面按固定暖橙配色。 */
    public boolean isDynamicColorEnabled() {
        return prefs.getBoolean(KEY_DYNAMIC_COLOR, false);
    }

    public void setDynamicColorEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_DYNAMIC_COLOR, enabled).apply();
    }

    /** 用户在登录页选中的季节 key（spring/summer/autumn/winter），空串表示按当前月份。 */
    public String getJourneySeason() {
        return prefs.getString(KEY_JOURNEY_SEASON, "");
    }

    public void setJourneySeason(String seasonKey) {
        prefs.edit().putString(KEY_JOURNEY_SEASON, seasonKey).apply();
    }

    /** 当前协作房间的持久化快照（跨进程留存，便于重进应用后继续规划）。 */
    public String getRoomId() {
        return prefs.getString(KEY_ROOM_ID, "");
    }

    public String getRoomName() {
        return prefs.getString(KEY_ROOM_NAME, "");
    }

    public String getRoomInviteCode() {
        return prefs.getString(KEY_ROOM_INVITE, "");
    }

    public String getRoomOwner() {
        return prefs.getString(KEY_ROOM_OWNER, "");
    }

    public String getRoomRole() {
        return prefs.getString(KEY_ROOM_ROLE, "");
    }

    public void saveRoom(String roomId, String roomName, String inviteCode, String owner, String role) {
        prefs.edit()
                .putString(KEY_ROOM_ID, roomId)
                .putString(KEY_ROOM_NAME, roomName)
                .putString(KEY_ROOM_INVITE, inviteCode)
                .putString(KEY_ROOM_OWNER, owner)
                .putString(KEY_ROOM_ROLE, role)
                .apply();
    }

    public void clearRoom() {
        prefs.edit()
                .remove(KEY_ROOM_ID)
                .remove(KEY_ROOM_NAME)
                .remove(KEY_ROOM_INVITE)
                .remove(KEY_ROOM_OWNER)
                .remove(KEY_ROOM_ROLE)
                .apply();
    }
}
