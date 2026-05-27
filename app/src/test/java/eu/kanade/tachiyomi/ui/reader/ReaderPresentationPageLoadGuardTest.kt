package eu.kanade.tachiyomi.ui.reader

import eu.kanade.tachiyomi.ui.reader.panel.PanelPageKey
import eu.kanade.tachiyomi.ui.reader.panel.PanelPageRenderVariant
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ReaderPresentationPageLoadGuardTest {

    @Test
    fun `allows image apply only when view is still attached to expected page`() {
        val expected = pageKey(pageIndex = 2)

        assertTrue(
            ReaderPresentationPageLoadGuard.canApplyPageLoad(
                expectedKey = expected,
                requestedTag = expected,
                isAttached = true,
            ),
        )
    }

    @Test
    fun `blocks image apply after view is reused for another page`() {
        val expected = pageKey(pageIndex = 2)
        val actual = pageKey(pageIndex = 3)

        assertFalse(
            ReaderPresentationPageLoadGuard.canApplyPageLoad(
                expectedKey = expected,
                requestedTag = actual,
                isAttached = true,
            ),
        )
    }

    @Test
    fun `blocks image apply when view is detached`() {
        val expected = pageKey(pageIndex = 2)

        assertFalse(
            ReaderPresentationPageLoadGuard.canApplyPageLoad(
                expectedKey = expected,
                requestedTag = expected,
                isAttached = false,
            ),
        )
    }

    @Test
    fun `starts load when requested tag is for a different page`() {
        val expected = pageKey(pageIndex = 2)
        val actual = pageKey(pageIndex = 3)

        assertTrue(
            ReaderPresentationPageLoadGuard.shouldStartPageLoad(
                expectedKey = expected,
                requestedTag = actual,
            ),
        )
    }

    @Test
    fun `does not restart load while same page request is already in flight`() {
        val expected = pageKey(pageIndex = 2)

        assertFalse(
            ReaderPresentationPageLoadGuard.shouldStartPageLoad(
                expectedKey = expected,
                requestedTag = expected,
            ),
        )
    }

    private fun pageKey(pageIndex: Int): PanelPageKey {
        return PanelPageKey(
            chapterId = 1L,
            chapterUrl = "chapter://1",
            chapterName = "Chapter 1",
            pageIndex = pageIndex,
            imageUrl = "image://page-$pageIndex",
            pageUrl = "page://$pageIndex",
            isInsertPage = false,
            renderVariant = PanelPageRenderVariant.FULL,
        )
    }
}
