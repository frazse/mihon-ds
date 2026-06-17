package eu.kanade.tachiyomi.ui.reader.input

import android.view.KeyEvent
import android.view.MotionEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ReaderInputCaptureController {

    private val mutableState = MutableStateFlow(ReaderInputCaptureState())
    private val motionEventLatch = ReaderInputMotionEventLatch()
    val state: StateFlow<ReaderInputCaptureState> = mutableState.asStateFlow()

    fun startCapture(layer: ReaderInputLayer?, action: ReaderAction) {
        motionEventLatch.reset()
        mutableState.update {
            ReaderInputCaptureState(
                request = ReaderInputCaptureRequest(
                    layer = layer,
                    action = action,
                    trigger = action.defaultInputTrigger(),
                ),
            )
        }
    }

    fun captureKeyEvent(event: KeyEvent): Boolean {
        if (!isCaptureActive() || event.keyCode == KeyEvent.KEYCODE_BACK) return false
        ReaderInputEventParser.bindingFromKeyEvent(event)?.let(::capture)
        return true
    }

    fun captureMotionEvent(event: MotionEvent): Boolean {
        if (!isCaptureActive()) return false
        motionEventLatch.bindingFromMotionEvent(event)?.let(::capture)
        return true
    }

    fun capture(binding: InputBinding): Boolean {
        while (true) {
            val current = mutableState.value
            val request = current.request ?: return false
            if (!request.supportsBinding(binding)) return false
            val updated = ReaderInputCaptureState(
                request = null,
                capturedBinding = binding,
                capturedLayer = request.layer,
                capturedAction = request.action,
                capturedTrigger = request.trigger,
            )
            if (mutableState.compareAndSet(current, updated)) {
                return true
            }
        }
    }

    fun consumeCaptured(): CapturedReaderInput? {
        while (true) {
            val current = mutableState.value
            val binding = current.capturedBinding ?: return null
            val action = current.capturedAction ?: return null
            val captured = CapturedReaderInput(
                layer = current.capturedLayer,
                action = action,
                trigger = current.capturedTrigger,
                binding = binding,
            )
            if (mutableState.compareAndSet(current, ReaderInputCaptureState())) {
                motionEventLatch.reset()
                return captured
            }
        }
    }

    fun cancel() {
        motionEventLatch.reset()
        mutableState.update { ReaderInputCaptureState() }
    }

    private fun isCaptureActive(): Boolean {
        val current = mutableState.value
        return current.request != null || current.capturedBinding != null
    }

    private fun ReaderInputCaptureRequest.supportsBinding(binding: InputBinding): Boolean {
        return trigger != ReaderInputTrigger.HOLD || binding.type == InputBindingType.KEY
    }
}

data class ReaderInputCaptureState(
    val request: ReaderInputCaptureRequest? = null,
    val capturedBinding: InputBinding? = null,
    val capturedLayer: ReaderInputLayer? = null,
    val capturedAction: ReaderAction? = null,
    val capturedTrigger: ReaderInputTrigger = ReaderInputTrigger.PRESS,
)

data class ReaderInputCaptureRequest(
    val layer: ReaderInputLayer?,
    val action: ReaderAction,
    val trigger: ReaderInputTrigger,
)

data class CapturedReaderInput(
    val layer: ReaderInputLayer?,
    val action: ReaderAction,
    val trigger: ReaderInputTrigger,
    val binding: InputBinding,
)

fun ReaderAction.defaultInputTrigger(): ReaderInputTrigger {
    return when (this) {
        ReaderAction.HOLD_SCROLL_DOWN,
        ReaderAction.HOLD_SCROLL_UP,
        -> ReaderInputTrigger.HOLD
        else -> ReaderInputTrigger.PRESS
    }
}
