package eu.kanade.tachiyomi.ui.reader.panel

import java.io.Closeable

class LazyPanelDetector(
    private val factory: () -> PanelDetector,
) : PanelDetector,
    Closeable {

    private val lock = Any()
    private var delegate: PanelDetector? = null
    private var closed = false

    override suspend fun detect(input: PanelDetectionInput): PanelDetectionResult {
        val detector = detector() ?: return PanelDetectionResult(emptyList())
        return detector.detect(input)
    }

    override fun close() {
        val initialized = synchronized(lock) {
            if (closed) {
                null
            } else {
                closed = true
                delegate.also { delegate = null }
            }
        }
        (initialized as? Closeable)?.close()
    }

    private fun detector(): PanelDetector? {
        return synchronized(lock) {
            if (closed) {
                null
            } else {
                delegate ?: factory().also { delegate = it }
            }
        }
    }
}
