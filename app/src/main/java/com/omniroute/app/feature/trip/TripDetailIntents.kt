package com.omniroute.app.feature.trip

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import android.widget.Toast
import com.omniroute.app.R
import com.omniroute.app.core.planning.TripItinerary
import java.util.Calendar

/** 打开官方链接；返回是否成功调起外部应用。 */
internal fun openExternal(context: Context, url: String): Boolean {
    if (url.isBlank() || (!url.startsWith("http://") && !url.startsWith("https://"))) {
        return false
    }
    return runCatching {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
        )
    }.isSuccess
}

/** 优先调起本机地图导航，失败时回退到高德网页版。 */
internal fun navigateToItem(context: Context, item: TripItinerary.Item): Boolean {
    if (!item.hasCoordinate()) return false
    val name = Uri.encode(item.name)
    val geo = "geo:${item.lat},${item.lng}?q=${item.lat},${item.lng}($name)"
    val launched = runCatching {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(geo)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
        )
    }.isSuccess
    if (launched) return true
    return openExternal(
        context,
        "https://uri.amap.com/marker?position=${item.lng},${item.lat}&name=$name",
    )
}

internal fun copyText(context: Context, text: String, label: String) {
    val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    if (manager == null) {
        Toast.makeText(
            context,
            context.getString(R.string.trip_clipboard_unsupported),
            Toast.LENGTH_SHORT,
        ).show()
        return
    }
    manager.setPrimaryClip(ClipData.newPlainText(label, text))
    Toast.makeText(context, context.getString(R.string.trip_copied, label), Toast.LENGTH_SHORT).show()
}

/** 用节点时间在「今天」创建一条日历事件；失败时返回 false 交给界面提示。 */
internal fun addToCalendar(context: Context, item: TripItinerary.Item): Boolean {
    val hour = item.time?.substringBefore(":")?.toIntOrNull() ?: 9
    val minute = item.time?.substringAfter(":", "")?.take(2)?.toIntOrNull() ?: 0
    val begin = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
        set(Calendar.MINUTE, minute.coerceIn(0, 59))
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    val end = begin + (item.durationMinutes.takeIf { it > 0 } ?: 90) * 60_000L
    val description = buildList {
        if (!item.address.isNullOrBlank()) {
            add(context.getString(R.string.trip_fact_address, item.address))
        }
        if (!item.description.isNullOrBlank()) add(item.description!!)
        if (!item.bookingUrl.isNullOrBlank()) {
            add(context.getString(R.string.trip_fact_booking, item.bookingUrl))
        }
    }.joinToString("\n")
    return runCatching {
        context.startActivity(
            Intent(Intent.ACTION_INSERT)
                .setData(CalendarContract.Events.CONTENT_URI)
                .putExtra(CalendarContract.Events.TITLE, item.name)
                .putExtra(CalendarContract.Events.DESCRIPTION, description)
                .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, begin)
                .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, end)
                .putExtra(CalendarContract.Events.EVENT_LOCATION, item.address)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }.isSuccess
}
