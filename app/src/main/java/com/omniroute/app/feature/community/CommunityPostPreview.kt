package com.omniroute.app.feature.community

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.omniroute.app.core.planning.PlanResult

internal data class CommunityPostPreview(
    val summary: String,
    val days: Int? = null,
    val stops: List<String> = emptyList(),
)

internal fun readPostPreview(content: String): CommunityPostPreview {
    val source = content.trim()
    val unwrapped = if (source.startsWith("```") && source.endsWith("```")) {
        source.substringAfter('\n').removeSuffix("```").trim()
    } else source
    if (!unwrapped.startsWith("{")) return CommunityPostPreview(source)
    val payload = runCatching { JsonParser.parseString(unwrapped) }.getOrNull() as? JsonObject
        ?: return CommunityPostPreview(source)
    val plan = PlanResult.parse(payload)
    val summary = sequenceOf("summary", "negotiation_summary", "description")
        .mapNotNull { key ->
            val value = payload.get(key)
            value?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }
                ?.asString?.trim()?.takeIf { it.isNotEmpty() }
        }
        .firstOrNull() ?: plan?.summary.orEmpty()
    val days = runCatching { payload.get("days")?.asInt }.getOrNull()?.takeIf { it in 1..30 }
        ?: plan?.days?.takeIf { it > 0 }
    val stops = plan?.route.orEmpty().mapNotNull { node ->
        node.name?.trim()?.takeIf { it.isNotEmpty() }
    }.distinct().take(3)
    return CommunityPostPreview(summary, days, stops)
}
