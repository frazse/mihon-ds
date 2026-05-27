package eu.kanade.tachiyomi.ui.reader.panel

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PanelModelTransformTest {

    @Test
    fun `content rect uses original source dimensions`() {
        val transform = PanelModelTransform.letterbox(
            sourceWidth = 4000,
            sourceHeight = 6000,
            inputSize = 640,
        )

        val rect = transform.contentRect()

        assertEquals(transform.padX, rect.left, 0.01f)
        assertEquals(transform.padY, rect.top, 0.01f)
        assertEquals(transform.padX + 4000f * transform.scale, rect.right, 0.01f)
        assertEquals(transform.padY + 6000f * transform.scale, rect.bottom, 0.01f)
    }
}
