package eu.kanade.tachiyomi.ui.reader.panel

import android.graphics.RectF
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PanelSorterTest {

    @Test
    fun `sorts left to right rows for LTR`() {
        val panels = listOf(
            panel(id = "bottom-right", left = 200f, top = 220f),
            panel(id = "top-right", left = 200f, top = 20f),
            panel(id = "bottom-left", left = 20f, top = 220f),
            panel(id = "top-left", left = 20f, top = 20f),
        )

        val result = PanelSorter.sort(panels, PanelReadingDirection.LEFT_TO_RIGHT)

        assertEquals(
            listOf("top-left", "top-right", "bottom-left", "bottom-right"),
            result.map { it.id },
        )
    }

    @Test
    fun `sorts right to left rows for RTL`() {
        val panels = listOf(
            panel(id = "bottom-right", left = 200f, top = 220f),
            panel(id = "top-right", left = 200f, top = 20f),
            panel(id = "bottom-left", left = 20f, top = 220f),
            panel(id = "top-left", left = 20f, top = 20f),
        )

        val result = PanelSorter.sort(panels, PanelReadingDirection.RIGHT_TO_LEFT)

        assertEquals(
            listOf("top-right", "top-left", "bottom-right", "bottom-left"),
            result.map { it.id },
        )
    }

    @Test
    fun `filters invalid tiny panels`() {
        val panels = listOf(
            panel(id = "valid", left = 20f, top = 20f, width = 120f, height = 120f),
            panel(id = "tiny", left = 40f, top = 40f, width = 2f, height = 2f),
        )

        val result = PanelSorter.sort(panels, PanelReadingDirection.RIGHT_TO_LEFT)

        assertEquals(listOf("valid"), result.map { it.id })
    }

    @Test
    fun `removes near duplicate panels before sorting`() {
        val panels = listOf(
            panel(id = "left", left = 20f, top = 20f, width = 120f, height = 120f),
            panel(id = "right", left = 220f, top = 20f, width = 120f, height = 120f),
            panel(id = "right-duplicate", left = 224f, top = 24f, width = 118f, height = 118f, confidence = 0.91f),
        )

        val result = PanelSorter.sort(panels, PanelReadingDirection.LEFT_TO_RIGHT)

        assertEquals(listOf("left", "right"), result.map { it.id })
    }

    @Test
    fun `removes mostly contained panel duplicates`() {
        val panels = listOf(
            panel(id = "outer", left = 20f, top = 20f, width = 220f, height = 160f, confidence = 0.96f),
            panel(id = "inner-duplicate", left = 28f, top = 28f, width = 204f, height = 144f, confidence = 0.92f),
            panel(id = "next", left = 260f, top = 20f, width = 140f, height = 160f),
        )

        val result = PanelSorter.sort(panels, PanelReadingDirection.LEFT_TO_RIGHT)

        assertEquals(listOf("outer", "next"), result.map { it.id })
    }

    @Test
    fun `keeps smaller inset panels when they are not duplicate sized`() {
        val panels = listOf(
            panel(id = "outer", left = 20f, top = 20f, width = 300f, height = 220f, confidence = 0.96f),
            panel(id = "inset", left = 200f, top = 120f, width = 90f, height = 80f, confidence = 0.94f),
        )

        val result = PanelSorter.sort(panels, PanelReadingDirection.LEFT_TO_RIGHT)

        assertEquals(listOf("outer", "inset"), result.map { it.id })
    }

    private fun panel(
        id: String,
        left: Float,
        top: Float,
        width: Float = 120f,
        height: Float = 120f,
        confidence: Float = 0.99f,
    ): ReaderPanel {
        return ReaderPanel(
            id = id,
            bounds = RectF().apply {
                this.left = left
                this.top = top
                right = left + width
                bottom = top + height
            },
            confidence = confidence,
        )
    }
}
