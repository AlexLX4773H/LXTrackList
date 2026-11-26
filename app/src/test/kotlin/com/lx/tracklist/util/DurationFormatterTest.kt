package com.lx.tracklist.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.TimeZone

class DurationFormatterTest {

    @Test
    fun `formatRuntime handles boundary minute values`() {
        assertEquals("0m", DurationFormatter.formatRuntime(0))
        assertEquals("59m", DurationFormatter.formatRuntime(59))
        assertEquals("1h", DurationFormatter.formatRuntime(60))
        assertEquals("1h 1m", DurationFormatter.formatRuntime(61))
        assertEquals("1h 30m", DurationFormatter.formatRuntime(90))
        assertEquals("2h", DurationFormatter.formatRuntime(120))
        assertEquals("0m", DurationFormatter.formatRuntime(null))
    }

    @Test
    fun `formatPerEpisode and formatTrack match expected style`() {
        assertEquals("24 min/ep", DurationFormatter.formatPerEpisode(24))
        assertEquals("3:05", DurationFormatter.formatTrack(185))
        assertEquals("0:00", DurationFormatter.formatTrack(0))
    }

    @Test
    fun `formatHours handles sub-hour, boundaries, trailing zeros, and compact rounding`() {
        assertEquals("0 min", DurationFormatter.formatHours(0.0))
        assertEquals("30 min", DurationFormatter.formatHours(0.5))
        assertEquals("45 min", DurationFormatter.formatHours(0.75))
        assertEquals("1 hr", DurationFormatter.formatHours(1.0))
        assertEquals("1 day", DurationFormatter.formatHours(24.0))
        assertEquals("7 days", DurationFormatter.formatHours(6.96 * 24.0))
        assertEquals("1 wk", DurationFormatter.formatHours(7.0 * 24.0))
        assertEquals("1 mo", DurationFormatter.formatHours(30.44 * 24.0))
        assertEquals("1 yr", DurationFormatter.formatHours(365.25 * 24.0))

        // Compact rounding must round total minutes first so 1.999h is "2h", never "1h 60m"
        assertEquals("2h", DurationFormatter.formatHours(1.999, compact = true))
        assertEquals("30m", DurationFormatter.formatHours(0.5, compact = true))
    }

    @Test
    fun `DateFormats parses and formats calendar dates in UTC regardless of device timezone`() {
        val originalTz = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"))
            val parsedIst = DateFormats.parseDateOnly("2026-06-01")
            assertNotNull(parsedIst)
            assertEquals("Jun 1, 2026", DateFormats.formatDateOnly(parsedIst!!))

            TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"))
            val parsedPst = DateFormats.parseDateOnly("2026-06-01")
            assertEquals(parsedIst, parsedPst)
            assertEquals("Jun 1, 2026", DateFormats.formatDateOnly(parsedPst!!))

            assertNull(DateFormats.parseDateOnly(null))
            assertNull(DateFormats.parseDateOnly(""))
            assertNull(DateFormats.parseDateOnly("2026-02-30"))
        } finally {
            TimeZone.setDefault(originalTz)
        }
    }
}
