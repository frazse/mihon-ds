package eu.kanade.tachiyomi.ui.reader.input

import android.view.KeyEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ReaderInputHoldKeyDispatcher(
    private val scope: CoroutineScope,
    private val holdDelayMillis: Long = DEFAULT_HOLD_DELAY_MILLIS,
    private val resolve: (InputBinding, ReaderInputTrigger) -> ReaderAction?,
    private val dispatch: (ReaderAction) -> Boolean,
    private val stop: (ReaderAction) -> Unit,
) {
    private var pending: PendingHold? = null
    private var active: ActiveHold? = null

    fun handleKeyEvent(event: KeyEvent): Boolean {
        val binding = ReaderInputEventParser.keyBinding(event.keyCode, event.metaState)
        val holdAction = resolve(binding, ReaderInputTrigger.HOLD) ?: return false

        return when (event.action) {
            KeyEvent.ACTION_DOWN -> handleKeyDown(event, binding, holdAction)
            KeyEvent.ACTION_UP -> handleKeyUp(binding)
            else -> false
        }
    }

    fun cancel() {
        pending?.job?.cancel()
        pending = null
        active?.let { stop(it.action) }
        active = null
    }

    private fun handleKeyDown(
        event: KeyEvent,
        binding: InputBinding,
        holdAction: ReaderAction,
    ): Boolean {
        if (event.repeatCount > 0) return true
        if (active?.binding == binding || pending?.binding == binding) return true

        pending?.job?.cancel()
        pending = PendingHold(
            binding = binding,
            holdAction = holdAction,
            job = scope.launch {
                delay(holdDelayMillis)
                val current = pending?.takeIf { it.binding == binding } ?: return@launch
                pending = null
                dispatch(current.holdAction)
                active = ActiveHold(binding = binding, action = current.holdAction)
            },
        )
        return true
    }

    private fun handleKeyUp(binding: InputBinding): Boolean {
        val currentActive = active?.takeIf { it.binding == binding }
        if (currentActive != null) {
            stop(currentActive.action)
            active = null
            return true
        }

        val currentPending = pending?.takeIf { it.binding == binding } ?: return false
        currentPending.job.cancel()
        pending = null

        val pressAction = resolve(binding, ReaderInputTrigger.PRESS) ?: return true
        dispatch(pressAction)
        return true
    }

    private data class PendingHold(
        val binding: InputBinding,
        val holdAction: ReaderAction,
        val job: Job,
    )

    private data class ActiveHold(
        val binding: InputBinding,
        val action: ReaderAction,
    )

    companion object {
        const val DEFAULT_HOLD_DELAY_MILLIS = 250L
    }
}
