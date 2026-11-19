package com.omniroute.app.core.data;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * 最近一次规划结果快照。
 *
 * <p>候选详情的导航只携带稳定 ID；进程重建后内存注册表失效，
 * 这里持久化的原始 payload 用于重建同一份详情数据。
 */
public final class PlanningSnapshotStore {

    private static final String PREFS_NAME = "omniroute_planning_snapshot";
    private static final String KEY_PAYLOAD = "payload";

    private final SharedPreferences prefs;

    public PlanningSnapshotStore(Context context) {
        this.prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /** 最近一次完成的规划结果原始 JSON；没有则为空串。 */
    public String getPayload() {
        return prefs.getString(KEY_PAYLOAD, "");
    }

    public void savePayload(String payload) {
        if (payload == null || payload.isEmpty()) {
            return;
        }
        prefs.edit().putString(KEY_PAYLOAD, payload).apply();
    }

    public void clear() {
        prefs.edit().remove(KEY_PAYLOAD).apply();
    }
}
