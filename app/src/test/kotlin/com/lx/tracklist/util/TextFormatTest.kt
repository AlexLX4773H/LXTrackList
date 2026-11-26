package com.lx.tracklist.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class TextFormatTest {

    @Test
    fun `plural handles 0, 1, 2 and irregular plurals`() {
        assertEquals("0 entries", plural(0, "entry", "entries"))
        assertEquals("1 entry", plural(1, "entry", "entries"))
        assertEquals("2 entries", plural(2, "entry", "entries"))

        assertEquals("0 episodes", plural(0, "episode"))
        assertEquals("1 episode", plural(1, "episode"))
        assertEquals("2 episodes", plural(2, "episode"))
    }

    @Test
    fun `formatIncludeExclude covers all four states`() {
        assertEquals("+2 / \u22121", formatIncludeExclude(2, 1))
        assertEquals("+2", formatIncludeExclude(2, 0))
        assertEquals("\u22121", formatIncludeExclude(0, 1))
        assertEquals("", formatIncludeExclude(0, 0))
    }

    @Test
    fun `formatCount formats integers cleanly`() {
        assertEquals("0", formatCount(0, Locale.US))
        assertEquals("42", formatCount(42, Locale.US))
        assertEquals("1,250", formatCount(1250, Locale.US))
    }
}
