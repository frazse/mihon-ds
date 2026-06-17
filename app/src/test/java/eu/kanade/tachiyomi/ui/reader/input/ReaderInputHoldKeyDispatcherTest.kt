package eu.kanade.tachiyomi.ui.reader.input

import android.view.KeyEvent
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class ReaderInputHoldKeyDispatcherTest {

    @Test
    fun `short release dispatches press action when hold mapping exists`() = runTest {
        val dispatched = mutableListOf<ReaderAction>()
        val stopped = mutableListOf<ReaderAction>()
        val dispatcher = dispatcher(
            resolve = { _, trigger ->
                when (trigger) {
                    ReaderInputTrigger.PRESS -> ReaderAction.SCROLL_DOWN
                    ReaderInputTrigger.HOLD -> ReaderAction.HOLD_SCROLL_DOWN
                }
            },
            dispatch = { action ->
                dispatched += action
                true
            },
            stop = { stopped += it },
        )

        dispatcher.handleKeyEvent(keyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BUTTON_R1)) shouldBe true
        advanceTimeBy(ReaderInputHoldKeyDispatcher.DEFAULT_HOLD_DELAY_MILLIS - 1)
        runCurrent()

        dispatched.shouldBeEmpty()
        dispatcher.handleKeyEvent(keyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BUTTON_R1)) shouldBe true

        dispatched shouldBe listOf(ReaderAction.SCROLL_DOWN)
        stopped.shouldBeEmpty()
    }

    @Test
    fun `holding past threshold dispatches hold action and suppresses press action`() = runTest {
        val dispatched = mutableListOf<ReaderAction>()
        val stopped = mutableListOf<ReaderAction>()
        val dispatcher = dispatcher(
            resolve = { _, trigger ->
                when (trigger) {
                    ReaderInputTrigger.PRESS -> ReaderAction.SCROLL_DOWN
                    ReaderInputTrigger.HOLD -> ReaderAction.HOLD_SCROLL_DOWN
                }
            },
            dispatch = { action ->
                dispatched += action
                true
            },
            stop = { stopped += it },
        )

        dispatcher.handleKeyEvent(keyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BUTTON_R1)) shouldBe true
        advanceTimeBy(ReaderInputHoldKeyDispatcher.DEFAULT_HOLD_DELAY_MILLIS)
        runCurrent()
        dispatcher.handleKeyEvent(keyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BUTTON_R1)) shouldBe true

        dispatched shouldBe listOf(ReaderAction.HOLD_SCROLL_DOWN)
        stopped shouldBe listOf(ReaderAction.HOLD_SCROLL_DOWN)
    }

    @Test
    fun `keys without hold mapping are ignored by hold dispatcher`() = runTest {
        val dispatched = mutableListOf<ReaderAction>()
        val dispatcher = dispatcher(
            resolve = { _, trigger ->
                when (trigger) {
                    ReaderInputTrigger.PRESS -> ReaderAction.SCROLL_DOWN
                    ReaderInputTrigger.HOLD -> null
                }
            },
            dispatch = { action ->
                dispatched += action
                true
            },
            stop = {},
        )

        dispatcher.handleKeyEvent(keyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BUTTON_R1)) shouldBe false
        dispatcher.handleKeyEvent(keyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BUTTON_R1)) shouldBe false

        dispatched.shouldBeEmpty()
    }

    @Test
    fun `hold threshold consumes key even when hold action is unsupported by current viewer`() = runTest {
        val dispatched = mutableListOf<ReaderAction>()
        val dispatcher = dispatcher(
            resolve = { _, trigger ->
                when (trigger) {
                    ReaderInputTrigger.PRESS -> ReaderAction.SCROLL_DOWN
                    ReaderInputTrigger.HOLD -> ReaderAction.HOLD_SCROLL_DOWN
                }
            },
            dispatch = { action ->
                dispatched += action
                false
            },
            stop = {},
        )

        dispatcher.handleKeyEvent(keyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BUTTON_R1)) shouldBe true
        advanceTimeBy(ReaderInputHoldKeyDispatcher.DEFAULT_HOLD_DELAY_MILLIS)
        runCurrent()

        dispatcher.handleKeyEvent(keyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BUTTON_R1)) shouldBe true
        dispatched shouldBe listOf(ReaderAction.HOLD_SCROLL_DOWN)
    }

    @Test
    fun `cancel stops active hold action`() = runTest {
        val stopped = mutableListOf<ReaderAction>()
        val dispatcher = dispatcher(
            resolve = { _, trigger ->
                when (trigger) {
                    ReaderInputTrigger.PRESS -> ReaderAction.SCROLL_DOWN
                    ReaderInputTrigger.HOLD -> ReaderAction.HOLD_SCROLL_DOWN
                }
            },
            dispatch = { true },
            stop = { stopped += it },
        )

        dispatcher.handleKeyEvent(keyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BUTTON_R1)) shouldBe true
        advanceTimeBy(ReaderInputHoldKeyDispatcher.DEFAULT_HOLD_DELAY_MILLIS)
        runCurrent()

        dispatcher.cancel()

        stopped shouldBe listOf(ReaderAction.HOLD_SCROLL_DOWN)
    }

    private fun CoroutineScope.dispatcher(
        resolve: (InputBinding, ReaderInputTrigger) -> ReaderAction?,
        dispatch: (ReaderAction) -> Boolean,
        stop: (ReaderAction) -> Unit,
    ): ReaderInputHoldKeyDispatcher {
        return ReaderInputHoldKeyDispatcher(
            scope = this,
            resolve = resolve,
            dispatch = dispatch,
            stop = stop,
        )
    }

    private fun keyEvent(
        action: Int,
        keyCode: Int,
        metaState: Int = 0,
        repeatCount: Int = 0,
    ): KeyEvent {
        return mockk {
            every { this@mockk.action } returns action
            every { this@mockk.keyCode } returns keyCode
            every { this@mockk.metaState } returns metaState
            every { this@mockk.repeatCount } returns repeatCount
        }
    }
}
