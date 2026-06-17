package eu.kanade.tachiyomi.ui.reader.input

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class ReaderActionDispatcherTest {

    @Test
    fun `activity action is handled by activity target`() {
        val target = FakeTarget()

        ReaderActionDispatcher.dispatch(ReaderAction.NEXT_CHAPTER, target) shouldBe true

        target.actions shouldBe listOf(ReaderAction.NEXT_CHAPTER)
    }

    @Test
    fun `viewer action is handled by viewer target`() {
        val target = FakeTarget(viewerHandledActions = setOf(ReaderAction.NEXT))

        ReaderActionDispatcher.dispatch(ReaderAction.NEXT, target) shouldBe true

        target.viewerActions shouldBe listOf(ReaderAction.NEXT)
    }

    @Test
    fun `unhandled viewer action returns false`() {
        val target = FakeTarget()

        ReaderActionDispatcher.dispatch(ReaderAction.SCROLL_DOWN, target) shouldBe false
    }

    private class FakeTarget(
        private val viewerHandledActions: Set<ReaderAction> = emptySet(),
    ) : ReaderActionTarget {
        val actions = mutableListOf<ReaderAction>()
        val viewerActions = mutableListOf<ReaderAction>()

        override fun handleActivityAction(action: ReaderAction): Boolean {
            actions += action
            return true
        }

        override fun handleViewerAction(action: ReaderAction): Boolean {
            viewerActions += action
            return action in viewerHandledActions
        }
    }
}
