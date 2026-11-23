package com.omniroute.app.core.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.omniroute.app.R
import java.util.Calendar

/**
 * 四季主题：与 Web 端 journey-seasons.css 的四组 token 一一对应。
 *
 * <p>只替换品牌/装饰色（主色、纸面、描边、软底），成功、警告、错误等语义色保持全季一致。
 * 登录页选中的季节会持久化，进入应用后所有页面沿用同一套季节配色。
 */
enum class JourneySeason(val key: String, val labelRes: Int) {
    Spring("spring", R.string.auth_season_spring),
    Summer("summer", R.string.auth_season_summer),
    Autumn("autumn", R.string.auth_season_autumn),
    Winter("winter", R.string.auth_season_winter);

    /** 英文期号，如 SPRING · 01 */
    val tagRes: Int
        get() = when (this) {
            Spring -> R.string.home_tag_spring
            Summer -> R.string.home_tag_summer
            Autumn -> R.string.home_tag_autumn
            Winter -> R.string.home_tag_winter
        }

    /** 季节标题，如 花间漫游 */
    val titleRes: Int
        get() = when (this) {
            Spring -> R.string.home_season_title_spring
            Summer -> R.string.home_season_title_summer
            Autumn -> R.string.home_season_title_autumn
            Winter -> R.string.home_season_title_winter
        }

    /** 一句话短句 */
    val phraseRes: Int
        get() = when (this) {
            Spring -> R.string.home_season_phrase_spring
            Summer -> R.string.home_season_phrase_summer
            Autumn -> R.string.home_season_phrase_autumn
            Winter -> R.string.home_season_phrase_winter
        }

    /** 季节手记标题 */
    val journalRes: Int
        get() = when (this) {
            Spring -> R.string.season_journal_spring
            Summer -> R.string.season_journal_summer
            Autumn -> R.string.season_journal_autumn
            Winter -> R.string.season_journal_winter
        }

    /** 季节注脚 */
    val noteRes: Int
        get() = when (this) {
            Spring -> R.string.season_note_spring
            Summer -> R.string.season_note_summer
            Autumn -> R.string.season_note_autumn
            Winter -> R.string.season_note_winter
        }

    /** 同路人寄语 */
    val communityRes: Int
        get() = when (this) {
            Spring -> R.string.season_community_spring
            Summer -> R.string.season_community_summer
            Autumn -> R.string.season_community_autumn
            Winter -> R.string.season_community_winter
        }

    /** 色彩灵感 */
    val paletteRes: Int
        get() = when (this) {
            Spring -> R.string.season_palette_spring
            Summer -> R.string.season_palette_summer
            Autumn -> R.string.season_palette_autumn
            Winter -> R.string.season_palette_winter
        }

    fun palette(darkTheme: Boolean): AppPalette = tokens().toPalette(darkTheme)

    private fun tokens(): SeasonTokens = when (this) {
        Spring -> SeasonTokens(
            accent = Color(0xFFAD405E),
            accentInk = Color(0xFF97324E),
            soft = Color(0xFFFCE8ED),
            paper = Color(0xFFFFFAF8),
            line = Color(0xFFECDCE0),
            night = Color(0xFF241E23),
            nightCard = Color(0xFF30272E),
            nightRaised = Color(0xFF3B3038),
            nightSoft = Color(0xFF492E3B),
            nightInk = Color(0xFFF3AEC4),
            nightLine = Color(0xFF57424E),
        )

        Summer -> SeasonTokens(
            accent = Color(0xFF197569),
            accentInk = Color(0xFF146257),
            soft = Color(0xFFE1F3EB),
            paper = Color(0xFFFAFCF6),
            line = Color(0xFFD7E6DB),
            night = Color(0xFF1B2523),
            nightCard = Color(0xFF26322F),
            nightRaised = Color(0xFF303E38),
            nightSoft = Color(0xFF24483E),
            nightInk = Color(0xFF96DDC6),
            nightLine = Color(0xFF3C5A4D),
        )

        Autumn -> SeasonTokens(
            accent = Color(0xFFB74B2C),
            accentInk = Color(0xFFA13C20),
            soft = Color(0xFFFFEAD8),
            paper = Color(0xFFFFFAF3),
            line = Color(0xFFEFDDCB),
            night = Color(0xFF251E1B),
            nightCard = Color(0xFF322823),
            nightRaised = Color(0xFF3E312A),
            nightSoft = Color(0xFF533528),
            nightInk = Color(0xFFFFBD91),
            nightLine = Color(0xFF604535),
        )

        Winter -> SeasonTokens(
            accent = Color(0xFF2C4458),
            accentInk = Color(0xFF2C4458),
            soft = Color(0xFFEBF1F6),
            paper = Color(0xFFF6F8FA),
            line = Color(0xFFD9E2EA),
            night = Color(0xFF151A21),
            nightCard = Color(0xFF202A34),
            nightRaised = Color(0xFF293745),
            nightSoft = Color(0xFF2C4458),
            nightInk = Color(0xFFBCDAED),
            nightLine = Color(0xFF46586A),
        )
    }

    companion object {
        /** 按月份落到当季，与登录页封面的规则保持一致。 */
        fun ofCurrentMonth(): JourneySeason =
            when (Calendar.getInstance().get(Calendar.MONTH) + 1) {
                3, 4, 5 -> Spring
                6, 7, 8 -> Summer
                12, 1, 2 -> Winter
                else -> Autumn
            }

        fun fromKey(key: String?): JourneySeason =
            values().firstOrNull { it.key == key } ?: ofCurrentMonth()
    }
}

private data class SeasonTokens(
    val accent: Color,
    val accentInk: Color,
    val soft: Color,
    val paper: Color,
    val line: Color,
    val night: Color,
    val nightCard: Color,
    val nightRaised: Color,
    val nightSoft: Color,
    val nightInk: Color,
    val nightLine: Color,
)

private fun SeasonTokens.toPalette(darkTheme: Boolean): AppPalette {
    if (darkTheme) {
        return AppPalette(
            brand = nightInk,
            brandDark = nightInk,
            brandLight = lerp(nightInk, Color.White, 0.16f),
            onBrand = night,
            ink = Color(0xFFF8F0EB),
            muted = Color(0xFFBDB0B3),
            background = night,
            surface = nightCard,
            fieldBg = nightRaised,
            tabTrack = nightRaised,
            border = nightLine,
            divider = lerp(nightLine, night, 0.45f),
            tagBg = nightRaised,
            brandTint = nightSoft,
            errorBg = Color(0xFF3A1E19),
            errorFg = Color(0xFFFFB4AB),
            infoBg = Color(0xFF17281F),
            infoFg = Color(0xFF7FD3A8),
            online = Color(0xFF4CC98A),
            verifiedBg = Color(0xFF17301F),
            verifiedFg = Color(0xFF7FD3A8),
            warningBg = Color(0xFF352A12),
            warningFg = Color(0xFFF2C46D),
        )
    }
    return AppPalette(
        brand = accent,
        brandDark = accentInk,
        brandLight = lerp(accent, Color.White, 0.28f),
        onBrand = Color(0xFFFFF8E8),
        ink = Color(0xFF352E32),
        muted = Color(0xFF786A70),
        background = paper,
        surface = Color(0xFFFFFEFD),
        fieldBg = lerp(soft, Color.White, 0.45f),
        tabTrack = lerp(line, Color.White, 0.30f),
        border = line,
        divider = lerp(line, paper, 0.45f),
        tagBg = lerp(soft, Color.White, 0.60f),
        brandTint = soft,
        errorBg = Color(0xFFFFF0ED),
        errorFg = Color(0xFFC63B2E),
        infoBg = Color(0xFFF0F7F3),
        infoFg = Color(0xFF2E7D5B),
        online = Color(0xFF2E9E6B),
        verifiedBg = Color(0xFFE8F7EE),
        verifiedFg = Color(0xFF1B8755),
        warningBg = Color(0xFFFEF3C7),
        warningFg = Color(0xFFB45309),
    )
}

/** 当前生效的季节，供登录后的页面读取（无需逐层传参）。 */
val LocalJourneySeason = staticCompositionLocalOf { JourneySeason.ofCurrentMonth() }
