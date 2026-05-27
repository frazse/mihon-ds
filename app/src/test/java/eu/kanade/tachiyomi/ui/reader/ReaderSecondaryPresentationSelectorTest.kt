package eu.kanade.tachiyomi.ui.reader

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ReaderSecondaryPresentationSelectorTest {

    @Test
    fun `uses controls presentation when companion and panel reading are disabled`() {
        assertEquals(
            SecondaryPresentationMode.CONTROLS,
            ReaderSecondaryPresentationSelector.mode(
                companionPageEnabled = false,
                panelReadingEnabled = false,
            ),
        )
    }

    @Test
    fun `uses reader presentation when companion page is enabled`() {
        assertEquals(
            SecondaryPresentationMode.PAGE,
            ReaderSecondaryPresentationSelector.mode(
                companionPageEnabled = true,
                panelReadingEnabled = false,
            ),
        )
    }

    @Test
    fun `uses reader presentation when panel reading is enabled`() {
        assertEquals(
            SecondaryPresentationMode.PAGE,
            ReaderSecondaryPresentationSelector.mode(
                companionPageEnabled = false,
                panelReadingEnabled = true,
            ),
        )
    }
}
