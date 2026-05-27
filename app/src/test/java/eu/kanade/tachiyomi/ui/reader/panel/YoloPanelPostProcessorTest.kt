package eu.kanade.tachiyomi.ui.reader.panel

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class YoloPanelPostProcessorTest {

    @Test
    fun `maps letterboxed panel detections back to source coordinates`() {
        val transform = PanelModelTransform(
            sourceWidth = 800,
            sourceHeight = 1200,
            inputSize = 640,
            scale = 640f / 1200f,
            padX = (640f - 800f * (640f / 1200f)) / 2f,
            padY = 0f,
        )

        val panels = YoloPanelPostProcessor.process(
            detections = listOf(
                floatArrayOf(
                    transform.padX + 100f * transform.scale,
                    200f * transform.scale,
                    transform.padX + 300f * transform.scale,
                    500f * transform.scale,
                    0.91f,
                    0f,
                ),
            ),
            transform = transform,
        )

        assertEquals(1, panels.size)
        assertEquals(100f, panels[0].bounds.left, 0.5f)
        assertEquals(200f, panels[0].bounds.top, 0.5f)
        assertEquals(300f, panels[0].bounds.right, 0.5f)
        assertEquals(500f, panels[0].bounds.bottom, 0.5f)
    }

    @Test
    fun `maps normalized model detections back to source coordinates`() {
        val transform = PanelModelTransform(
            sourceWidth = 800,
            sourceHeight = 1200,
            inputSize = 640,
            scale = 640f / 1200f,
            padX = (640f - 800f * (640f / 1200f)) / 2f,
            padY = 0f,
        )

        val panels = YoloPanelPostProcessor.process(
            detections = listOf(
                floatArrayOf(
                    (transform.padX + 100f * transform.scale) / transform.inputSize,
                    (200f * transform.scale) / transform.inputSize,
                    (transform.padX + 300f * transform.scale) / transform.inputSize,
                    (500f * transform.scale) / transform.inputSize,
                    0.91f,
                    0f,
                ),
            ),
            transform = transform,
        )

        assertEquals(1, panels.size)
        assertEquals(100f, panels[0].bounds.left, 0.5f)
        assertEquals(200f, panels[0].bounds.top, 0.5f)
        assertEquals(300f, panels[0].bounds.right, 0.5f)
        assertEquals(500f, panels[0].bounds.bottom, 0.5f)
    }

    @Test
    fun `ignores text class and low confidence detections`() {
        val transform = squareTransform()

        val panels = YoloPanelPostProcessor.process(
            detections = listOf(
                floatArrayOf(10f, 10f, 100f, 100f, 0.9f, 1f),
                floatArrayOf(110f, 110f, 200f, 200f, 0.1f, 0f),
                floatArrayOf(210f, 210f, 300f, 300f, 0.8f, 0f),
            ),
            transform = transform,
        )

        assertEquals(1, panels.size)
        assertEquals(210f, panels[0].bounds.left, 0.5f)
    }

    @Test
    fun `suppresses lower confidence duplicate panels`() {
        val transform = squareTransform()

        val panels = YoloPanelPostProcessor.process(
            detections = listOf(
                floatArrayOf(10f, 10f, 210f, 210f, 0.95f, 0f),
                floatArrayOf(20f, 20f, 220f, 220f, 0.7f, 0f),
                floatArrayOf(300f, 300f, 500f, 500f, 0.8f, 0f),
            ),
            transform = transform,
        )

        assertEquals(2, panels.size)
        assertTrue(panels.any { it.bounds.left == 10f })
        assertTrue(panels.any { it.bounds.left == 300f })
    }

    private fun squareTransform(): PanelModelTransform {
        return PanelModelTransform(
            sourceWidth = 640,
            sourceHeight = 640,
            inputSize = 640,
            scale = 1f,
            padX = 0f,
            padY = 0f,
        )
    }
}
