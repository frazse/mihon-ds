package eu.kanade.tachiyomi.ui.reader.panel

import okio.Buffer
import okio.BufferedSource

class PanelDetectionImage private constructor(
    private val imageBytes: ByteArray,
) {
    val byteCount: Int
        get() = imageBytes.size

    fun newSource(): BufferedSource {
        return Buffer().write(imageBytes)
    }

    fun copyBytes(): ByteArray {
        return imageBytes.copyOf()
    }

    companion object {
        const val MAX_SNAPSHOT_BYTE_COUNT = 8L * 1024L * 1024L
        private const val READ_SEGMENT_BYTE_COUNT = 8L * 1024L

        fun fromBytes(imageBytes: ByteArray): PanelDetectionImage {
            return PanelDetectionImage(imageBytes.copyOf())
        }

        fun fromSource(source: BufferedSource): PanelDetectionImage {
            return fromSourceOrNull(source, Long.MAX_VALUE)
                ?: error("Unable to snapshot panel detection image")
        }

        fun fromSourceOrNull(
            source: BufferedSource,
            maxByteCount: Long = MAX_SNAPSHOT_BYTE_COUNT,
        ): PanelDetectionImage? {
            if (maxByteCount < 0L) return null

            val readLimit = if (maxByteCount == Long.MAX_VALUE) {
                Long.MAX_VALUE
            } else {
                maxByteCount + 1L
            }
            val peek = source.peek()
            val snapshot = Buffer()
            var totalBytes = 0L

            while (true) {
                val remainingLimit = readLimit - totalBytes
                if (remainingLimit <= 0L) return null

                val read = peek.read(snapshot, minOf(READ_SEGMENT_BYTE_COUNT, remainingLimit))
                if (read == -1L) break

                totalBytes += read
                if (totalBytes > maxByteCount) return null
            }

            return fromOwnedBytes(snapshot.readByteArray())
        }

        internal fun fromOwnedBytes(imageBytes: ByteArray): PanelDetectionImage {
            return PanelDetectionImage(imageBytes)
        }
    }
}
