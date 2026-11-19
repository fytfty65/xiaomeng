package com.omniroute.app.core.notify

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.omniroute.app.AppContainer

/**
 * 后台巡检：刷新订阅城市风险 → 拉未读 → 补发系统通知。
 *
 * <p>失败一律按成功结束，交给下一轮周期任务重试，避免失败退避把电耗光。
 * 未登录或开关关闭时直接跳过，不发请求。
 */
internal class RiskAlertWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        if (!RiskAlertScheduler.isEnabled(context)) return Result.success()
        if (!RiskNotifier.canPost(context)) {
            // 用户关掉了系统通知权限：停掉周期任务，等下次进页面重新开启
            RiskAlertScheduler.sync(context)
            return Result.success()
        }

        val container = AppContainer(context)
        runCatching {
            var user = container.authRepository.user
            if (user == null) {
                container.authRepository.refreshSilent()
                user = container.authRepository.user
            }
            if (user == null) return Result.success()

            container.notificationRepository.checkRiskSubscriptions()
            container.notificationRepository.listRiskNotifications("unread")
                .forEach { RiskNotifier.notify(context, it) }
        }
        return Result.success()
    }
}
