package com.lx.tracklist.ui.theme

import com.lx.tracklist.data.model.MediaStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ThemeColorsTest {

    @Test
    fun `red-primary themes do not use red for success`() {
        val crimsonSuccess = CrimsonDarkColors.success
        assertNotEquals(CrimsonDarkColors.primary, crimsonSuccess)
        assertNotEquals(CrimsonDarkColors.error, crimsonSuccess)

        val beyondSuccess = BeyondThemeColors.success
        assertNotEquals(BeyondThemeColors.primary, beyondSuccess)
        assertNotEquals(BeyondThemeColors.error, beyondSuccess)
    }

    @Test
    fun `statusColor maps all 5 statuses to theme semantic tokens`() {
        val presets = listOf(
            DefaultDarkColors,
            CleanLightColors,
            CrimsonDarkColors,
            SlateBlueGreyColors,
            BeyondThemeColors,
            SlimeThemeColors,
            GateThemeColors,
            SkyThemeColors
        )

        for (preset in presets) {
            assertEquals(preset.primary, preset.statusColor(MediaStatus.WATCHING))
            assertEquals(preset.success, preset.statusColor(MediaStatus.COMPLETED))
            assertEquals(preset.warning, preset.statusColor(MediaStatus.ON_HOLD))
            assertEquals(preset.error, preset.statusColor(MediaStatus.DROPPED))
            assertEquals(preset.textSecondary, preset.statusColor(MediaStatus.PLAN_TO_WATCH))
        }
    }
}
