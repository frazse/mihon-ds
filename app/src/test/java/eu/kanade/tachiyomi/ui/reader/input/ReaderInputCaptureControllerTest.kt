package eu.kanade.tachiyomi.ui.reader.input

import android.view.KeyEvent
import android.view.MotionEvent
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test

class ReaderInputCaptureControllerTest {

    @Test
    fun `capture stores key binding and clears active request`() {
        val controller = ReaderInputCaptureController()
        controller.startCapture(
            layer = null,
            action = ReaderAction.NEXT,
        )

        controller.capture(InputBinding.key(KeyEvent.KEYCODE_BUTTON_R1)) shouldBe true
        controller.state.value.capturedBinding shouldBe InputBinding.key(KeyEvent.KEYCODE_BUTTON_R1)
        controller.state.value.request shouldBe null
    }

    @Test
    fun `capture key event captures action up and consumes active capture events`() {
        val controller = ReaderInputCaptureController()
        controller.startCapture(
            layer = null,
            action = ReaderAction.NEXT,
        )

        controller.captureKeyEvent(
            keyEvent(
                action = KeyEvent.ACTION_DOWN,
                keyCode = KeyEvent.KEYCODE_BUTTON_R1,
            ),
        ) shouldBe true
        controller.captureKeyEvent(
            keyEvent(
                action = KeyEvent.ACTION_UP,
                keyCode = KeyEvent.KEYCODE_BUTTON_R1,
            ),
        ) shouldBe true

        controller.state.value.capturedBinding shouldBe InputBinding.key(KeyEvent.KEYCODE_BUTTON_R1)
        controller.captureKeyEvent(
            keyEvent(
                action = KeyEvent.ACTION_UP,
                keyCode = KeyEvent.KEYCODE_BUTTON_L1,
            ),
        ) shouldBe true
        controller.state.value.capturedBinding shouldBe InputBinding.key(KeyEvent.KEYCODE_BUTTON_R1)
    }

    @Test
    fun `capture motion event uses latch and consumes held repeats`() {
        val controller = ReaderInputCaptureController()
        controller.startCapture(
            layer = ReaderInputLayer.WEBTOON,
            action = ReaderAction.FAST_SCROLL_DOWN,
        )

        controller.captureMotionEvent(
            motionEvent(
                action = MotionEvent.ACTION_MOVE,
                axisValues = mapOf(MotionEvent.AXIS_RTRIGGER to 0.7f),
            ),
        ) shouldBe true
        controller.state.value.capturedBinding shouldBe InputBinding.axis(
            axis = MotionEvent.AXIS_RTRIGGER,
            direction = AxisDirection.POSITIVE,
            threshold = InputBinding.DEFAULT_AXIS_THRESHOLD,
        )

        controller.captureMotionEvent(
            motionEvent(
                action = MotionEvent.ACTION_MOVE,
                axisValues = mapOf(MotionEvent.AXIS_RTRIGGER to 0.8f),
            ),
        ) shouldBe true
        controller.state.value.capturedBinding shouldBe InputBinding.axis(
            axis = MotionEvent.AXIS_RTRIGGER,
            direction = AxisDirection.POSITIVE,
            threshold = InputBinding.DEFAULT_AXIS_THRESHOLD,
        )
    }

    @Test
    fun `capture motion event ignores axis input for hold actions`() {
        val controller = ReaderInputCaptureController()
        controller.startCapture(
            layer = ReaderInputLayer.WEBTOON,
            action = ReaderAction.HOLD_SCROLL_DOWN,
        )

        controller.captureMotionEvent(
            motionEvent(
                action = MotionEvent.ACTION_MOVE,
                axisValues = mapOf(MotionEvent.AXIS_RTRIGGER to 0.7f),
            ),
        ) shouldBe true

        controller.state.value.request?.action shouldBe ReaderAction.HOLD_SCROLL_DOWN
        controller.state.value.capturedBinding shouldBe null
        controller.capture(
            InputBinding.axis(
                axis = MotionEvent.AXIS_RTRIGGER,
                direction = AxisDirection.POSITIVE,
            ),
        ) shouldBe false
    }

    @Test
    fun `capture is one shot after request is consumed by first commit`() {
        val controller = ReaderInputCaptureController()
        controller.startCapture(
            layer = null,
            action = ReaderAction.NEXT,
        )

        controller.capture(InputBinding.key(KeyEvent.KEYCODE_BUTTON_R1)) shouldBe true
        controller.capture(InputBinding.key(KeyEvent.KEYCODE_BUTTON_L1)) shouldBe false
        controller.state.value.capturedBinding shouldBe InputBinding.key(KeyEvent.KEYCODE_BUTTON_R1)
    }

    @Test
    fun `capture returns false when no request is active`() {
        val controller = ReaderInputCaptureController()

        controller.capture(InputBinding.key(KeyEvent.KEYCODE_BUTTON_R1)) shouldBe false
    }

    @Test
    fun `cancel clears active request without captured binding`() {
        val controller = ReaderInputCaptureController()
        controller.startCapture(layer = ReaderInputLayer.WEBTOON, action = ReaderAction.SCROLL_DOWN)

        controller.cancel()

        controller.state.value.request shouldBe null
        controller.state.value.capturedBinding shouldBe null
    }

    @Test
    fun `cancel discards captured result`() {
        val controller = ReaderInputCaptureController()
        controller.startCapture(
            layer = ReaderInputLayer.WEBTOON,
            action = ReaderAction.SCROLL_DOWN,
        )
        controller.capture(InputBinding.key(KeyEvent.KEYCODE_BUTTON_R1)) shouldBe true

        controller.cancel()

        controller.state.value.capturedBinding shouldBe null
        controller.consumeCaptured() shouldBe null
    }

    @Test
    fun `consume captured returns captured input and resets state`() {
        val controller = ReaderInputCaptureController()
        val binding = InputBinding.key(KeyEvent.KEYCODE_BUTTON_R1)
        controller.startCapture(
            layer = ReaderInputLayer.WEBTOON,
            action = ReaderAction.SCROLL_DOWN,
        )

        controller.capture(binding) shouldBe true

        controller.consumeCaptured() shouldBe CapturedReaderInput(
            layer = ReaderInputLayer.WEBTOON,
            action = ReaderAction.SCROLL_DOWN,
            trigger = ReaderInputTrigger.PRESS,
            binding = binding,
        )
        controller.state.value shouldBe ReaderInputCaptureState()
        controller.consumeCaptured() shouldBe null
    }

    @Test
    fun `consume captured is one shot after first reset commits`() {
        val controller = ReaderInputCaptureController()
        val binding = InputBinding.key(KeyEvent.KEYCODE_BUTTON_R1)
        controller.startCapture(
            layer = ReaderInputLayer.WEBTOON,
            action = ReaderAction.SCROLL_DOWN,
        )
        controller.capture(binding) shouldBe true

        controller.consumeCaptured() shouldBe CapturedReaderInput(
            layer = ReaderInputLayer.WEBTOON,
            action = ReaderAction.SCROLL_DOWN,
            trigger = ReaderInputTrigger.PRESS,
            binding = binding,
        )
        controller.consumeCaptured() shouldBe null
    }

    private fun keyEvent(
        action: Int,
        keyCode: Int,
        metaState: Int = 0,
    ): KeyEvent {
        return mockk {
            every { this@mockk.action } returns action
            every { this@mockk.keyCode } returns keyCode
            every { this@mockk.metaState } returns metaState
        }
    }

    private fun motionEvent(
        action: Int,
        axisValues: Map<Int, Float>,
    ): MotionEvent {
        return mockk {
            every { this@mockk.action } returns action
            every { getAxisValue(any()) } answers { axisValues[arg<Int>(0)] ?: 0f }
        }
    }
}
