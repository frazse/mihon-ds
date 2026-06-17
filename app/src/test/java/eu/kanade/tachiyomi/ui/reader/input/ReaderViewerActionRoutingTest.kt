package eu.kanade.tachiyomi.ui.reader.input

import eu.kanade.tachiyomi.ui.reader.viewer.pager.PagerReaderActionRoute
import eu.kanade.tachiyomi.ui.reader.viewer.pager.pagerReaderActionRoute
import eu.kanade.tachiyomi.ui.reader.viewer.webtoon.WebtoonReaderActionRoute
import eu.kanade.tachiyomi.ui.reader.viewer.webtoon.webtoonReaderActionRoute
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class ReaderViewerActionRoutingTest {

    @Test
    fun `pager next and previous actions stay on physical routing`() {
        pagerReaderActionRoute(ReaderAction.NEXT) shouldBe PagerReaderActionRoute.MOVE_RIGHT
        pagerReaderActionRoute(ReaderAction.PREVIOUS) shouldBe PagerReaderActionRoute.MOVE_LEFT
    }

    @Test
    fun `pager panel actions use semantic next and previous routing`() {
        pagerReaderActionRoute(ReaderAction.NEXT_PANEL) shouldBe PagerReaderActionRoute.MOVE_TO_NEXT
        pagerReaderActionRoute(ReaderAction.PREVIOUS_PANEL) shouldBe PagerReaderActionRoute.MOVE_TO_PREVIOUS
    }

    @Test
    fun `webtoon maps paging actions to scroll actions and ignores panel actions`() {
        webtoonReaderActionRoute(ReaderAction.NEXT) shouldBe WebtoonReaderActionRoute.SCROLL_DOWN
        webtoonReaderActionRoute(ReaderAction.PREVIOUS) shouldBe WebtoonReaderActionRoute.SCROLL_UP
        webtoonReaderActionRoute(ReaderAction.HOLD_SCROLL_DOWN) shouldBe WebtoonReaderActionRoute.HOLD_SCROLL_DOWN
        webtoonReaderActionRoute(ReaderAction.HOLD_SCROLL_UP) shouldBe WebtoonReaderActionRoute.HOLD_SCROLL_UP
        webtoonReaderActionRoute(ReaderAction.NEXT_PANEL) shouldBe null
        pagerReaderActionRoute(ReaderAction.HOLD_SCROLL_DOWN) shouldBe null
    }
}
