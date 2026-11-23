package com.omniroute.app.feature.home

import com.omniroute.app.core.planning.PlanRequest

internal data class HomeQuickPlan(val city: String, val days: Int)

private val DestinationAndDays = Regex("""^(.*?)(?:\s*([0-9０-９]+)\s*[日天]游?|\s+([0-9０-９]+))$""")

internal fun parseQuickPlan(query: String): HomeQuickPlan? {
    val input = query.trim()
    val match = DestinationAndDays.matchEntire(input)
    val city = (match?.groupValues?.get(1) ?: input).trim().trimEnd(',', '，').trimEnd()
    if (!PlanRequest.isSupportedDestination(city)) return null
    if (match == null) return HomeQuickPlan(city, 3)
    if (city.last() in ".．+-－＋" || city.last().isDigit()) return null

    val digits = match.groupValues[2].ifEmpty { match.groupValues[3] }
    val days = digits.map { it.digitToInt() }.joinToString("").toIntOrNull()
        ?.takeIf { it in 1..30 }
        ?: return null
    return HomeQuickPlan(city, days)
}
