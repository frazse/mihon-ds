package eu.kanade.tachiyomi.ui.reader.panel

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PanelBitmapDecodeConfigTest {

    @Test
    fun `keeps normal manga pages at source size`() {
        val sampleSize = PanelBitmapDecodeConfig.calculateInSampleSize(
            sourceWidth = 1200,
            sourceHeight = 1800,
            targetMaxSide = 1280,
            minimumShortSide = 320,
        )

        assertEquals(1, sampleSize)
    }

    @Test
    fun `downsamples oversized scans near the detector target`() {
        val sampleSize = PanelBitmapDecodeConfig.calculateInSampleSize(
            sourceWidth = 4000,
            sourceHeight = 6000,
            targetMaxSide = 1280,
            minimumShortSide = 320,
        )

        assertEquals(4, sampleSize)
    }

    @Test
    fun `keeps enough short side detail for very tall pages`() {
        val sampleSize = PanelBitmapDecodeConfig.calculateInSampleSize(
            sourceWidth = 1600,
            sourceHeight = 12000,
            targetMaxSide = 1280,
            minimumShortSide = 320,
        )

        assertEquals(4, sampleSize)
    }

    @Test
    fun `ignores invalid source dimensions`() {
        val sampleSize = PanelBitmapDecodeConfig.calculateInSampleSize(
            sourceWidth = 0,
            sourceHeight = 6000,
            targetMaxSide = 1280,
            minimumShortSide = 320,
        )

        assertEquals(1, sampleSize)
    }
}
