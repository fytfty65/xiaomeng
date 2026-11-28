package com.omniroute.app.core.ui.theme

import com.omniroute.app.R

internal data class PortalArt(val landscape: Int, val window: Int, val frost: Int)
private val art = mapOf(
    JourneySeason.Spring to PortalArt(R.drawable.portal_spring, R.drawable.window_spring, R.drawable.portal_frost_spring),
    JourneySeason.Summer to PortalArt(R.drawable.portal_summer, R.drawable.window_summer, R.drawable.portal_frost_summer),
    JourneySeason.Autumn to PortalArt(R.drawable.portal_autumn, R.drawable.window_autumn, R.drawable.portal_frost_autumn),
    JourneySeason.Winter to PortalArt(R.drawable.portal_winter, R.drawable.window_winter, R.drawable.portal_frost_winter),
)
internal val JourneySeason.portalArt get() = art.getValue(this)
