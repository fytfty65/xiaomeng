package com.omniroute.app.core.data;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * 行程节点的「已预订」本地标记。
 *
 * <p>标记只存在设备上，不上传后端：用户在详情页点「标记已预订」后，
 * 即使退出应用或重新登录，同一行程仍能看到标记。
 *
 * <p>键按「天 + 名称」组合，跨天同名节点不会被一起标记；旧版本只按名称存
 * 的标记仍可读取，切换时会顺手清理。
 */
public final class TripBookingStore {

    private static final String PREFS_NAME = "omniroute_trip_actions";
    private static final String KEY_PREFIX = "booked_v2_";
    private static final String LEGACY_PREFIX = "booked_";

    private final SharedPreferences prefs;

    public TripBookingStore(Context context) {
        this.prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /** 同一天的同名节点视为同一条；跨天同名分别记录。 */
    public static String nodeKey(int day, String name) {
        return day + "|" + (name == null ? "" : name.trim());
    }

    /** 读取一次行程标记的只读快照，供界面渲染使用。 */
    public Marks marks(String tripId) {
        Set<String> keys = new HashSet<>(
                prefs.getStringSet(KEY_PREFIX + tripId, Collections.emptySet()));
        Set<String> legacy = new HashSet<>(
                prefs.getStringSet(LEGACY_PREFIX + tripId, Collections.emptySet()));
        return new Marks(keys, legacy);
    }

    /**
     * 切换某个节点的预订标记并写盘。
     *
     * @return 切换后的状态，true 表示已预订
     */
    public boolean toggle(String tripId, int day, String name) {
        String key = nodeKey(day, name);
        String plainName = name == null ? "" : name.trim();
        Set<String> keys = new HashSet<>(
                prefs.getStringSet(KEY_PREFIX + tripId, Collections.emptySet()));
        Set<String> legacy = new HashSet<>(
                prefs.getStringSet(LEGACY_PREFIX + tripId, Collections.emptySet()));

        boolean booked = keys.contains(key) || legacy.contains(plainName);
        keys.remove(key);
        legacy.remove(plainName);
        if (!booked) {
            keys.add(key);
        }

        prefs.edit()
                .putStringSet(KEY_PREFIX + tripId, keys)
                .putStringSet(LEGACY_PREFIX + tripId, legacy)
                .apply();
        return !booked;
    }

    /** 删除行程后清理该行程的本地标记，避免残留键。 */
    public void clear(String tripId) {
        prefs.edit()
                .remove(KEY_PREFIX + tripId)
                .remove(LEGACY_PREFIX + tripId)
                .apply();
    }

    /** 某次行程的只读标记快照。 */
    public static final class Marks {

        private final Set<String> keys;
        private final Set<String> legacyNames;

        Marks(Set<String> keys, Set<String> legacyNames) {
            this.keys = keys;
            this.legacyNames = legacyNames;
        }

        public boolean isBooked(int day, String name) {
            String plainName = name == null ? "" : name.trim();
            return keys.contains(nodeKey(day, plainName)) || legacyNames.contains(plainName);
        }
    }
}
