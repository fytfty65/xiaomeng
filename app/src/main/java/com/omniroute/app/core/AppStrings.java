package com.omniroute.app.core;

import android.content.Context;

/**
 * Java 业务层访问字符串资源的统一入口。
 * 业务仓库没有 Context，统一在 AppContainer 创建时注入 applicationContext。
 */
public final class AppStrings {

    private static Context appContext;

    private AppStrings() {
    }

    public static void init(Context context) {
        appContext = context.getApplicationContext();
    }

    public static String get(int resId, Object... args) {
        if (appContext == null) {
            throw new IllegalStateException("AppStrings 尚未初始化");
        }
        return args.length == 0 ? appContext.getString(resId) : appContext.getString(resId, args);
    }
}
