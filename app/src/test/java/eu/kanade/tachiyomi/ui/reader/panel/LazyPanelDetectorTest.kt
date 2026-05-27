package eu.kanade.tachiyomi.ui.reader.panel

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.Closeable

class LazyPanelDetectorTest {

    @Test
    fun `detect after close returns empty result without creating delegate`() = runTest {
        var factoryCalls = 0
        val detector = LazyPanelDetector {
            factoryCalls += 1
            StaticPanelDetector(listOf(panel("created")))
        }

        detector.close()

        val result = detector.detect(input())

        assertEquals(emptyList<ReaderPanel>(), result.panels)
        assertEquals(0, factoryCalls)
    }

    @Test
    fun `close after initialization prevents later delegate recreation`() = runTest {
        var factoryCalls = 0
        var closeCalls = 0
        val detector = LazyPanelDetector {
            factoryCalls += 1
            CloseablePanelDetector(listOf(panel("created"))) {
                closeCalls += 1
            }
        }

        assertEquals(listOf("created"), detector.detect(input()).panels.map { it.id })

        detector.close()
        val result = detector.detect(input())

        assertEquals(emptyList<ReaderPanel>(), result.panels)
        assertEquals(1, factoryCalls)
        assertEquals(1, closeCalls)

        detector.close()
        detector.detect(input())

        assertEquals(1, factoryCalls)
        assertEquals(1, closeCalls)
    }

    private fun input(): PanelDetectionInput {
        return PanelDetectionInput(
            key = PanelPageKey(
                chapterId = 1L,
                chapterUrl = "chapter://1",
                chapterName = "Chapter 1",
                pageIndex = 0,
                imageUrl = "image://0",
                pageUrl = "page://0",
                isInsertPage = false,
            ),
            imageWidth = 100,
            imageHeight = 100,
            image = PanelDetectionImage.fromBytes(byteArrayOf(1, 2, 3)),
        )
    }

    private fun panel(id: String): ReaderPanel {
        return ReaderPanel(
            id = id,
            bounds = android.graphics.RectF(0f, 0f, 1f, 1f),
            confidence = 1f,
        )
    }

    private class StaticPanelDetector(
        private val panels: List<ReaderPanel>,
    ) : PanelDetector {
        override suspend fun detect(input: PanelDetectionInput): PanelDetectionResult {
            return PanelDetectionResult(panels)
        }
    }

    private class CloseablePanelDetector(
        private val panels: List<ReaderPanel>,
        private val onClose: () -> Unit,
    ) : PanelDetector,
        Closeable {

        override suspend fun detect(input: PanelDetectionInput): PanelDetectionResult {
            return PanelDetectionResult(panels)
        }

        override fun close() {
            onClose()
        }
    }
}
