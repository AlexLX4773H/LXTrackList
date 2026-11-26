package com.lx.tracklist.ui.theme

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Test

class TypeTokensTest {

    @Test
    fun `typography defines headline, labelSmall, and extension tokens`() {
        assertEquals(32.sp, Typography.headlineLarge.fontSize)
        assertEquals(38.sp, Typography.headlineLarge.lineHeight)
        assertEquals(26.sp, Typography.headlineMedium.fontSize)
        assertEquals(32.sp, Typography.headlineMedium.lineHeight)
        assertEquals(22.sp, Typography.headlineSmall.fontSize)
        assertEquals(28.sp, Typography.headlineSmall.lineHeight)
        assertEquals(10.sp, Typography.labelSmall.fontSize)
        assertEquals(14.sp, Typography.labelSmall.lineHeight)

        assertEquals(10.sp, Typography.badge.fontSize)
        assertEquals(FontWeight.Bold, Typography.badge.fontWeight)
        assertEquals(12.sp, Typography.badge.lineHeight)

        assertEquals(11.sp, Typography.caption.fontSize)
        assertEquals(14.sp, Typography.caption.lineHeight)

        assertEquals(12.sp, Typography.chip.fontSize)
        assertEquals(FontWeight.Medium, Typography.chip.fontWeight)
        assertEquals(16.sp, Typography.chip.lineHeight)
    }
}
