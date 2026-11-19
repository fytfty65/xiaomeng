package com.omniroute.app.core.auth;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * 令牌存储：
 * - access token 只保留在内存，进程退出即消失；
 * - refresh token 经 Keystore AES/GCM 加密后写入 SharedPreferences。
 */
public final class TokenStore {

    private static final String PREFS_NAME = "omniroute_tokens";
    private static final String KEY_REFRESH_TOKEN = "refresh_token_cipher";

    private final SharedPreferences prefs;
    private final TokenCipher cipher = new TokenCipher();

    private volatile String accessToken;

    public TokenStore(Context context) {
        this.prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public String currentAccessToken() {
        return accessToken;
    }

    public void save(String accessToken, String refreshToken) {
        this.accessToken = accessToken;

        String encrypted = null;
        try {
            encrypted = cipher.encrypt(refreshToken);
        } catch (Exception ignored) {
            // 加密失败时不落盘，避免把明文 refresh token 写进磁盘。
        }

        SharedPreferences.Editor editor = prefs.edit();
        if (encrypted != null) {
            editor.putString(KEY_REFRESH_TOKEN, encrypted);
        } else {
            editor.remove(KEY_REFRESH_TOKEN);
        }
        editor.apply();
    }

    public String currentRefreshToken() {
        String payload = prefs.getString(KEY_REFRESH_TOKEN, null);
        if (payload == null) {
            return null;
        }
        String plain = cipher.decrypt(payload);
        if (plain == null) {
            prefs.edit().remove(KEY_REFRESH_TOKEN).apply();
        }
        return plain;
    }

    public void clear() {
        accessToken = null;
        prefs.edit().remove(KEY_REFRESH_TOKEN).apply();
    }
}
