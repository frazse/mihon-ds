package eu.kanade.tachiyomi.ui.reader.input

import android.view.KeyEvent
import android.view.MotionEvent

object ReaderInputCaptureRegistry {

    private val lock = Any()
    private var activeRegistration: Registration? = null

    fun register(controller: ReaderInputCaptureController): Registration {
        return Registration(controller).also { registration ->
            synchronized(lock) {
                activeRegistration = registration
            }
        }
    }

    fun captureKeyEvent(event: KeyEvent): Boolean {
        val controller = synchronized(lock) { activeRegistration?.controller }
        return controller?.captureKeyEvent(event) == true
    }

    fun captureMotionEvent(event: MotionEvent): Boolean {
        val controller = synchronized(lock) { activeRegistration?.controller }
        return controller?.captureMotionEvent(event) == true
    }

    class Registration internal constructor(
        internal val controller: ReaderInputCaptureController,
    ) : AutoCloseable {

        override fun close() {
            synchronized(lock) {
                if (activeRegistration === this) {
                    activeRegistration = null
                }
            }
        }
    }
}
