package eu.kanade.tachiyomi.ui.reader.input

import android.view.KeyEvent
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test

class ReaderInputCaptureRegistryTest {

    @Test
    fun `active controller captures key events before normal navigation`() {
        val controller = ReaderInputCaptureController()
        controller.startCapture(layer = null, action = ReaderAction.NEXT)

        val registration = ReaderInputCaptureRegistry.register(controller)

        ReaderInputCaptureRegistry.captureKeyEvent(
            keyEvent(
                action = KeyEvent.ACTION_DOWN,
                keyCode = KeyEvent.KEYCODE_DPAD_RIGHT,
            ),
        ) shouldBe true
        ReaderInputCaptureRegistry.captureKeyEvent(
            keyEvent(
                action = KeyEvent.ACTION_UP,
                keyCode = KeyEvent.KEYCODE_DPAD_RIGHT,
            ),
        ) shouldBe true
        controller.state.value.capturedBinding shouldBe InputBinding.key(KeyEvent.KEYCODE_DPAD_RIGHT)

        registration.close()

        ReaderInputCaptureRegistry.captureKeyEvent(
            keyEvent(
                action = KeyEvent.ACTION_UP,
                keyCode = KeyEvent.KEYCODE_DPAD_LEFT,
            ),
        ) shouldBe false
    }

    @Test
    fun `closing older registration does not unregister newer active controller`() {
        val oldController = ReaderInputCaptureController()
        val newController = ReaderInputCaptureController()
        oldController.startCapture(layer = null, action = ReaderAction.NEXT)
        newController.startCapture(layer = null, action = ReaderAction.PREVIOUS)

        val oldRegistration = ReaderInputCaptureRegistry.register(oldController)
        ReaderInputCaptureRegistry.register(newController)

        oldRegistration.close()

        ReaderInputCaptureRegistry.captureKeyEvent(
            keyEvent(
                action = KeyEvent.ACTION_UP,
                keyCode = KeyEvent.KEYCODE_DPAD_LEFT,
            ),
        ) shouldBe true
        oldController.state.value.capturedBinding shouldBe null
        newController.state.value.capturedBinding shouldBe InputBinding.key(KeyEvent.KEYCODE_DPAD_LEFT)
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
