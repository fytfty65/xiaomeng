package com.omniroute.app.feature.plan

import android.content.Context
import com.omniroute.app.R
import com.omniroute.app.core.network.dto.CandidateDto
import com.omniroute.app.core.planning.PlanResult
import com.omniroute.app.core.planning.RouteNode
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * 方案详情页的统一展示模型。
 *
 * <p>候选详情页原本只接受 [CandidateDto]；但当前规划主链路走 WebSocket，
 * 结果页拿到的是 [RouteNode]，候选对象并不存在。这里把两者归一成同一份只读快照，
 * 让「景点/交通/酒店/餐饮」节点与真实候选共用同一套详情渲染。
 */
data class CandidateDetailData(
    val id: String,
    val name: String,
    val groupTitle: String,
    val rank: Int,
    val score: Double,
    val tags: List<String>,
    val reasons: List<String>,
    val constraints: List<String>,
    val priceLabel: String?,
    val priceRange: String?,
    val priceStatus: String?,
    val estimated: Boolean,
    val trustLabel: String?,
    val segments: List<String>,
    val durationMinutes: Int,
    val transferBufferMinutes: Int,
    val carbonKgEstimate: Int,
    val area: String?,
    val confidence: Double,
    val freshnessSeconds: Int,
    val sourceProvider: String?,
    val sourceRetrievedAt: String?,
    val bookingLabel: String?,
    val bookingUrl: String?,
    val disclaimer: String?,
)

/** 真实候选（目的地/交通/住宿/餐饮接口返回）→ 详情模型。 */
fun CandidateDto.toDetailData(
    context: Context,
    groupTitle: String,
    rank: Int,
): CandidateDetailData {
    val source = this.source
    return CandidateDetailData(
        id = id?.takeIf { it.isNotBlank() } ?: ("$groupTitle|$name|$rank"),
        name = name.orEmpty(),
        groupTitle = groupTitle,
        rank = rank,
        score = score,
        tags = tags,
        reasons = reasons,
        constraints = constraints,
        priceLabel = priceLabel,
        priceRange = priceRange,
        priceStatus = priceStatus,
        estimated = isEstimated,
        trustLabel = context.getString(
            if (isEstimated) R.string.plan_detail_estimated else R.string.plan_detail_realtime,
        ),
        segments = segments,
        durationMinutes = durationMinutes,
        transferBufferMinutes = transferBufferMinutes,
        carbonKgEstimate = carbonKgEstimate,
        area = area,
        confidence = confidence,
        freshnessSeconds = freshnessSeconds,
        sourceProvider = source?.provider,
        sourceRetrievedAt = source?.retrievedAt,
        bookingLabel = bookingLabel,
        bookingUrl = bookingUrl ?: verificationUrl,
        disclaimer = disclaimer,
    )
}

/** 路书节点（WebSocket final_route）→ 详情模型。 */
fun RouteNode.toDetailData(
    context: Context,
    groupTitle: String,
    rank: Int,
): CandidateDetailData {
    val categoryTags = tags.toMutableList()
    if (categoryTags.none { it == category }) {
        categoryTags.add(0, category)
    }
    val observed = observedAt.takeIf { it.isNotBlank() }
    return CandidateDetailData(
        id = routeNodeDetailId(this),
        name = name,
        groupTitle = groupTitle,
        rank = rank,
        score = 0.0,
        tags = categoryTags.distinct(),
        reasons = buildList {
            if (description.isNotBlank()) add(description)
            if (trustMessage.isNotBlank() && !description.contains(trustMessage)) add(trustMessage)
        },
        constraints = buildList {
            if (trust.isNotBlank() && trust != "verified" && trustAction.isNotBlank()) {
                add(trustAction)
            }
            if (openTime.isNotBlank()) {
                add(context.getString(R.string.plan_detail_open_time, openTime))
            }
        },
        priceLabel = costEstimate.takeIf { it.isNotBlank() },
        priceRange = null,
        priceStatus = when (trust) {
            "estimated" -> "estimated"
            "unknown", "stale" -> "unknown"
            else -> if (isEstimated) "estimated" else null
        },
        estimated = isEstimated || trust == "estimated" || trust == "unknown",
        trustLabel = freshnessLabel(),
        segments = emptyList(),
        durationMinutes = 0,
        transferBufferMinutes = 0,
        carbonKgEstimate = 0,
        area = address.takeIf { it.isNotBlank() },
        confidence = 0.0,
        freshnessSeconds = freshnessSecondsOf(observed),
        sourceProvider = sourceLabel.takeIf { it.isNotBlank() },
        sourceRetrievedAt = observed,
        bookingLabel = if (officialUrl.isNotBlank()) {
            context.getString(R.string.plan_detail_booking_fallback)
        } else {
            null
        },
        bookingUrl = officialUrl.takeIf { it.isNotBlank() },
        disclaimer = context.getString(R.string.plan_detail_node_disclaimer),
    )
}

internal fun routeNodeDetailId(node: RouteNode): String {
    return node.name + "|" + node.time + "|" + node.day
}

/**
 * 规划结果 → 详情模型列表：按分类分组、组内按出现顺序编号。
 * 规划页与候选详情页共用同一套编号规则，保证导航里的稳定 ID 一致。
 */
internal fun PlanResult.toDetailDataList(context: Context): List<CandidateDetailData> {
    val ranks = mutableMapOf<String, Int>()
    return route.map { node ->
        val group = node.category
        val rank = (ranks[group] ?: 0) + 1
        ranks[group] = rank
        node.toDetailData(context, group, rank)
    }
}

private fun freshnessSecondsOf(raw: String?): Int {
    if (raw.isNullOrBlank()) return 0
    val millis = runCatching { Instant.parse(raw).toEpochMilli() }
        .recoverCatching {
            LocalDateTime.parse(raw.removeSuffix("Z"))
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        }
        .getOrNull() ?: return 0
    return ((System.currentTimeMillis() - millis) / 1000).coerceAtLeast(0).toInt()
}

/**
 * 详情页数据只在同一次会话内有效：路线是内存态，进程重建后拿不到。
 * 用进程内注册表按 (分组, 候选 ID) 索引，避免把整份路书塞进导航参数。
 */
object CandidateDetailStore {

    private val items = LinkedHashMap<String, CandidateDetailData>()

    private fun key(group: String, id: String): String = group + "::" + id

    fun put(detail: CandidateDetailData) {
        items[key(detail.groupTitle, detail.id)] = detail
    }

    fun putAll(details: Collection<CandidateDetailData>) {
        details.forEach { put(it) }
    }

    fun get(group: String, id: String): CandidateDetailData? {
        return items[key(group, id)]
    }

    fun clear() {
        items.clear()
    }
}

/** 行程详情「重新规划该节点」跳到规划页时的一次性预填参数。 */
object PlanningPrefill {

    private var city: String = ""
    private var nodeName: String = ""

    fun set(city: String, nodeName: String) {
        this.city = city
        this.nodeName = nodeName
    }

    /** 读取并清空，避免下次进入规划页重复预填。 */
    fun consume(): Pair<String, String>? {
        if (city.isBlank() && nodeName.isBlank()) return null
        val value = city to nodeName
        city = ""
        nodeName = ""
        return value
    }
}
