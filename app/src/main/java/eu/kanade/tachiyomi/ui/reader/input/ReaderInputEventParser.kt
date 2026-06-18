package eu.kanade.tachiyomi.ui.reader.input

import android.view.KeyEvent
import android.view.MotionEvent
import kotlin.math.abs

object ReaderInputEventParser {

    internal val supportedAxes = listOf(
        MotionEvent.AXIS_HAT_X,
        MotionEvent.AXIS_HAT_Y,
        MotionEvent.AXIS_LTRIGGER,
        MotionEvent.AXIS_RTRIGGER,
        MotionEvent.AXIS_BRAKE,
        MotionEvent.AXIS_GAS,
    )

    fun bindingFromKeyEvent(event: KeyEvent): InputBinding? {
        if (event.action != KeyEvent.ACTION_UP) return null
        return keyBinding(event.keyCode, event.metaState)
    }

    fun keyBinding(keyCode: Int, metaState: Int): InputBinding {
        return InputBinding.key(
            keyCode = keyCode,
            metaState = normalizeMetaState(metaState),
        )
    }

    fun bindingFromMotionEvent(event: MotionEvent): InputBinding? {
        if (event.action != MotionEvent.ACTION_MOVE) return null
        return supportedAxes.firstNotNullOfOrNull { axis ->
            axisBinding(
                axis = axis,
                value = event.getAxisValue(axis),
                threshold = InputBinding.DEFAULT_AXIS_THRESHOLD,
            )
        }
    }

    fun axisBinding(
        axis: Int,
        value: Float,
        threshold: Float,
    ): InputBinding? {
        if (abs(value) <= threshold) return null
        val direction = if (value > 0f) AxisDirection.POSITIVE else AxisDirection.NEGATIVE
        return InputBinding.axis(axis, direction, threshold)
    }

    private fun normalizeMetaState(metaState: Int): Int {
        var normalized = 0
        if (
            metaState and KeyEvent.META_CTRL_ON != 0 ||
            metaState and KeyEvent.META_CTRL_LEFT_ON != 0 ||
            metaState and KeyEvent.META_CTRL_RIGHT_ON != 0
        ) {
            normalized = normalized or KeyEvent.META_CTRL_ON
        }
        if (
            metaState and KeyEvent.META_SHIFT_ON != 0 ||
            metaState and KeyEvent.META_SHIFT_LEFT_ON != 0 ||
            metaState and KeyEvent.META_SHIFT_RIGHT_ON != 0
        ) {
            normalized = normalized or KeyEvent.META_SHIFT_ON
        }
        if (
            metaState and KeyEvent.META_ALT_ON != 0 ||
            metaState and KeyEvent.META_ALT_LEFT_ON != 0 ||
            metaState and KeyEvent.META_ALT_RIGHT_ON != 0
        ) {
            normalized = normalized or KeyEvent.META_ALT_ON
        }
        return normalized
    }
}

class ReaderInputMotionEventLatch(
    private val threshold: Float = InputBinding.DEFAULT_AXIS_THRESHOLD,
) {
    private val activeAxes = mutableSetOf<Int>()

    fun bindingFromMotionEvent(event: MotionEvent): InputBinding? {
        if (event.action != MotionEvent.ACTION_MOVE) return null

        var firstNewBinding: InputBinding? = null
        ReaderInputEventParser.supportedAxes.forEach { axis ->
            val binding = ReaderInputEventParser.axisBinding(
                axis = axis,
                value = event.getAxisValue(axis),
                threshold = threshold,
            )
            if (binding == null) {
                activeAxes.remove(axis)
            } else if (axis !in activeAxes) {
                activeAxes += axis
                if (firstNewBinding == null) {
                    firstNewBinding = binding
                }
            }
        }
        return firstNewBinding
    }

    fun reset() {
        activeAxes.clear()
    }
}
