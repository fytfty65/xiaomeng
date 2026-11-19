package com.omniroute.app.core.notify

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * 风险巡检的调度入口。
 *
 * <p>用 WorkManager 做周期任务（系统下限 15 分钟）：用户开启系统通知后，
 * 即使应用被杀掉，只要进程被调度起来就会巡检一次并补发通知。
 */
internal object RiskAlertScheduler {

    private const val PERIODIC_WORK = "risk_alert_periodic"
    private const val ONCE_WORK = "risk_alert_once"
    private const val PREFS = "risk_alerts"
    private const val KEY_ENABLED = "enabled"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun isEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_ENABLED, false)

    fun setEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    /** 状态变化后调用：开关打开且仍有通知权限时保活周期任务，否则取消。 */
    fun sync(context: Context) {
        val manager = WorkManager.getInstance(context.applicationContext)
        if (isEnabled(context) && RiskNotifier.canPost(context)) {
            manager.enqueueUniquePeriodicWork(
                PERIODIC_WORK,
                ExistingPeriodicWorkPolicy.UPDATE,
                PeriodicWorkRequestBuilder<RiskAlertWorker>(15, TimeUnit.MINUTES)
                    .setConstraints(networkConstraints())
                    .build(),
            )
        } else {
            manager.cancelUniqueWork(PERIODIC_WORK)
        }
    }

    /** 立即巡检一次（开启开关、点「立即检查」时用）。 */
    fun runOnce(context: Context) {
        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            ONCE_WORK,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<RiskAlertWorker>()
                .setConstraints(networkConstraints())
                .build(),
        )
    }

    private fun networkConstraints(): Constraints =
        Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
}
