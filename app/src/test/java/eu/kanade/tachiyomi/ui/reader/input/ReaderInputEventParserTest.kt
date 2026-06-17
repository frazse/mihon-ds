package eu.kanade.tachiyomi.ui.reader.input

import android.view.KeyEvent
import android.view.MotionEvent
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test

class ReaderInputEventParserTest {

    @Test
    fun `key binding normalizes to key code and meta state`() {
        ReaderInputEventParser.keyBinding(
            keyCode = KeyEvent.KEYCODE_DPAD_RIGHT,
            metaState = KeyEvent.META_CTRL_ON or KeyEvent.META_CTRL_LEFT_ON,
        ) shouldBe InputBinding.key(
            keyCode = KeyEvent.KEYCODE_DPAD_RIGHT,
            metaState = KeyEvent.META_CTRL_ON,
        )
    }

    @Test
    fun `key binding normalizes shift meta state`() {
        ReaderInputEventParser.keyBinding(
            keyCode = KeyEvent.KEYCODE_DPAD_RIGHT,
            metaState = KeyEvent.META_SHIFT_ON or KeyEvent.META_SHIFT_RIGHT_ON,
        ) shouldBe InputBinding.key(
            keyCode = KeyEvent.KEYCODE_DPAD_RIGHT,
            metaState = KeyEvent.META_SHIFT_ON,
        )
    }

    @Test
    fun `key binding normalizes alt meta state`() {
        ReaderInputEventParser.keyBinding(
            keyCode = KeyEvent.KEYCODE_DPAD_RIGHT,
            metaState = KeyEvent.META_ALT_ON or KeyEvent.META_ALT_LEFT_ON,
        ) shouldBe InputBinding.key(
            keyCode = KeyEvent.KEYCODE_DPAD_RIGHT,
            metaState = KeyEvent.META_ALT_ON,
        )
    }

    @Test
    fun `binding from key event returns null for action down`() {
        ReaderInputEventParser.bindingFromKeyEvent(
            keyEvent(
                action = KeyEvent.ACTION_DOWN,
                keyCode = KeyEvent.KEYCODE_DPAD_RIGHT,
            ),
        ) shouldBe null
    }

    @Test
    fun `binding from key event returns normalized binding for action up`() {
        ReaderInputEventParser.bindingFromKeyEvent(
            keyEvent(
                action = KeyEvent.ACTION_UP,
                keyCode = KeyEvent.KEYCODE_DPAD_RIGHT,
                metaState = KeyEvent.META_CTRL_LEFT_ON,
            ),
        ) shouldBe InputBinding.key(
            keyCode = KeyEvent.KEYCODE_DPAD_RIGHT,
            metaState = KeyEvent.META_CTRL_ON,
        )
    }

    @Test
    fun `axis binding captures positive trigger above threshold`() {
        ReaderInputEventParser.axisBinding(
            axis = MotionEvent.AXIS_RTRIGGER,
            value = 0.7f,
            threshold = 0.5f,
        ) shouldBe InputBinding.axis(
            axis = MotionEvent.AXIS_RTRIGGER,
            direction = AxisDirection.POSITIVE,
            threshold = 0.5f,
        )
    }

    @Test
    fun `axis binding ignores values inside threshold`() {
        ReaderInputEventParser.axisBinding(
            axis = MotionEvent.AXIS_RTRIGGER,
            value = 0.2f,
            threshold = 0.5f,
        ) shouldBe null
    }

    @Test
    fun `axis binding ignores values equal to threshold`() {
        ReaderInputEventParser.axisBinding(
            axis = MotionEvent.AXIS_RTRIGGER,
            value = 0.5f,
            threshold = 0.5f,
        ) shouldBe null

        ReaderInputEventParser.axisBinding(
            axis = MotionEvent.AXIS_HAT_X,
            value = -0.5f,
            threshold = 0.5f,
        ) shouldBe null
    }

    @Test
    fun `axis binding captures negative hat movement`() {
        ReaderInputEventParser.axisBinding(
            axis = MotionEvent.AXIS_HAT_X,
            value = -1f,
            threshold = 0.5f,
        ) shouldBe InputBinding.axis(
            axis = MotionEvent.AXIS_HAT_X,
            direction = AxisDirection.NEGATIVE,
            threshold = 0.5f,
        )
    }

    @Test
    fun `binding from motion event scans supported axes using default threshold`() {
        ReaderInputEventParser.bindingFromMotionEvent(
            motionEvent(
                action = MotionEvent.ACTION_MOVE,
                axisValues = mapOf(
                    MotionEvent.AXIS_HAT_X to 0.2f,
                    MotionEvent.AXIS_RTRIGGER to 0.7f,
                ),
            ),
        ) shouldBe InputBinding.axis(
            axis = MotionEvent.AXIS_RTRIGGER,
            direction = AxisDirection.POSITIVE,
            threshold = InputBinding.DEFAULT_AXIS_THRESHOLD,
        )
    }

    @Test
    fun `binding from motion event scans each supported axis`() {
        listOf(
            MotionEvent.AXIS_HAT_X,
            MotionEvent.AXIS_HAT_Y,
            MotionEvent.AXIS_LTRIGGER,
            MotionEvent.AXIS_RTRIGGER,
            MotionEvent.AXIS_BRAKE,
            MotionEvent.AXIS_GAS,
        ).forEach { axis ->
            ReaderInputEventParser.bindingFromMotionEvent(
                motionEvent(
                    action = MotionEvent.ACTION_MOVE,
                    axisValues = mapOf(axis to 0.7f),
                ),
            ) shouldBe InputBinding.axis(
                axis = axis,
                direction = AxisDirection.POSITIVE,
                threshold = InputBinding.DEFAULT_AXIS_THRESHOLD,
            )
        }
    }

    @Test
    fun `binding from motion event returns null for non move action`() {
        ReaderInputEventParser.bindingFromMotionEvent(
            motionEvent(
                action = MotionEvent.ACTION_DOWN,
                axisValues = mapOf(MotionEvent.AXIS_RTRIGGER to 0.7f),
            ),
        ) shouldBe null
    }

    @Test
    fun `binding from motion event returns null when no supported axis exceeds threshold`() {
        ReaderInputEventParser.bindingFromMotionEvent(
            motionEvent(
                action = MotionEvent.ACTION_MOVE,
                axisValues = mapOf(MotionEvent.AXIS_X to 1f),
            ),
        ) shouldBe null
    }

    @Test
    fun `motion event latch emits once until axis returns to neutral`() {
        val latch = ReaderInputMotionEventLatch()

        latch.bindingFromMotionEvent(
            motionEvent(
                action = MotionEvent.ACTION_MOVE,
                axisValues = mapOf(MotionEvent.AXIS_RTRIGGER to 0.7f),
            ),
        ) shouldBe InputBinding.axis(
            axis = MotionEvent.AXIS_RTRIGGER,
            direction = AxisDirection.POSITIVE,
            threshold = InputBinding.DEFAULT_AXIS_THRESHOLD,
        )
        latch.bindingFromMotionEvent(
            motionEvent(
                action = MotionEvent.ACTION_MOVE,
                axisValues = mapOf(MotionEvent.AXIS_RTRIGGER to 0.8f),
            ),
        ) shouldBe null

        latch.bindingFromMotionEvent(
            motionEvent(
                action = MotionEvent.ACTION_MOVE,
                axisValues = mapOf(MotionEvent.AXIS_RTRIGGER to 0.1f),
            ),
        ) shouldBe null
        latch.bindingFromMotionEvent(
            motionEvent(
                action = MotionEvent.ACTION_MOVE,
                axisValues = mapOf(MotionEvent.AXIS_RTRIGGER to 0.7f),
            ),
        ) shouldBe InputBinding.axis(
            axis = MotionEvent.AXIS_RTRIGGER,
            direction = AxisDirection.POSITIVE,
            threshold = InputBinding.DEFAULT_AXIS_THRESHOLD,
        )
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
}
