package com.omniroute.app.core.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.omniroute.app.MainActivity
import com.omniroute.app.R
import com.omniroute.app.core.network.dto.RiskNotificationDto

/**
 * 风险提醒的系统通知出口。
 *
 * <p>不依赖 Firebase：后台巡检拉到未读风险后直接发本地通知。
 * 去重以通知 id 为键，避免 15 分钟一轮巡检把同一条提醒重复弹出。
 */
internal object RiskNotifier {

    private const val CHANNEL_ID = "risk_alerts"
    private const val PREFS = "risk_alerts"
    private const val KEY_DELIVERED = "delivered_ids"
    private const val MAX_DELIVERED = 200

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notify_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.notify_channel_desc)
        }
        manager.createNotificationChannel(channel)
    }

    fun canPost(context: Context): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled()

    /** @return 是否真正发出（已发过或没有权限时返回 false）。 */
    fun notify(context: Context, item: RiskNotificationDto): Boolean {
        val id = item.id?.takeIf { it.isNotBlank() } ?: return false
        if (alreadyDelivered(context, id)) return false
        if (!canPost(context)) return false
        ensureChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val high = item.severity?.uppercase() in setOf("HIGH", "SEVERE", "CRITICAL")
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(context.applicationInfo.icon)
            .setContentTitle(item.title ?: context.getString(R.string.notify_channel_name))
            .setContentText(item.detail.orEmpty())
            .setStyle(NotificationCompat.BigTextStyle().bigText(item.detail.orEmpty()))
            .setPriority(if (high) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        NotificationManagerCompat.from(context).notify(id.hashCode(), builder.build())
        markDelivered(context, id)
        return true
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun alreadyDelivered(context: Context, id: String): Boolean =
        prefs(context).getStringSet(KEY_DELIVERED, emptySet())?.contains(id) == true

    private fun markDelivered(context: Context, id: String) {
        val current = prefs(context).getStringSet(KEY_DELIVERED, emptySet())
            ?.toMutableSet() ?: mutableSetOf()
        current.add(id)
        while (current.size > MAX_DELIVERED) {
            current.remove(current.first())
        }
        prefs(context).edit().putStringSet(KEY_DELIVERED, current).apply()
    }
}
