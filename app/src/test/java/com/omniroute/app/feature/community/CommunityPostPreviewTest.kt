package com.omniroute.app.feature.community

import org.junit.Assert.assertEquals
import org.junit.Test

class CommunityPostPreviewTest {
    @Test
    fun rendersLegacyRouteJsonAsSummaryStopsAndDuration() {
        val post = readPostPreview(
            """{"summary":"沿湖散步","days":2,"route":[{"name":"市区","day":1},{"name":"天池","day":2}]}""",
        )
        assertEquals("沿湖散步", post.summary)
        assertEquals(2, post.days)
        assertEquals(listOf("市区", "天池"), post.stops)
    }

    @Test
    fun preservesPlainTravelWriting() {
        assertEquals("沿湖散步。", readPostPreview("沿湖散步。").summary)
    }
}
