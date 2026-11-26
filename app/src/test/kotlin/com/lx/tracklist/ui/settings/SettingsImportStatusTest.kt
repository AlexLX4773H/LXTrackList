package com.lx.tracklist.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsImportStatusTest {

    @Test
    fun `BUG-20 formatImportStatusMessage formats inserted updated and skipped counts clearly`() {
        assertEquals(
            "Imported 5 new • updated 2 • skipped 1",
            formatImportStatusMessage(inserted = 5, updated = 2, skipped = 1)
        )
        assertEquals(
            "Imported 3 new • updated 0 • skipped 2 (Row 4: blank title)",
            formatImportStatusMessage(inserted = 3, updated = 0, skipped = 2, parseErrorNote = "Row 4: blank title")
        )
    }
}
