package eu.kanade.tachiyomi.ui.reader.panel

import okio.Buffer
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Test

class PanelDetectionImageTest {

    @Test
    fun `newSource returns independent readable sources`() {
        val bytes = byteArrayOf(0, 1, 2, 3, 4)
        val image = PanelDetectionImage.fromBytes(bytes)

        bytes[0] = 9

        image.newSource().use { source ->
            assertArrayEquals(byteArrayOf(0, 1), source.readByteArray(2L))
        }
        image.newSource().use { source ->
            assertArrayEquals(byteArrayOf(0, 1, 2, 3, 4), source.readByteArray())
        }
    }

    @Test
    fun `copyBytes returns a defensive copy`() {
        val image = PanelDetectionImage.fromBytes(byteArrayOf(5, 6, 7))

        val copy = image.copyBytes()
        copy[0] = 0

        image.newSource().use { source ->
            assertArrayEquals(byteArrayOf(5, 6, 7), source.readByteArray())
        }
    }

    @Test
    fun `fromSource snapshots without consuming source`() {
        val bytes = byteArrayOf(8, 9, 10, 11)
        val source = Buffer().write(bytes)

        val image = PanelDetectionImage.fromSource(source)

        assertArrayEquals(bytes, image.copyBytes())
        assertArrayEquals(bytes, source.readByteArray())
    }

    @Test
    fun `fromSourceOrNull returns null for sources over byte limit without consuming source`() {
        val bytes = byteArrayOf(8, 9, 10, 11)
        val source = Buffer().write(bytes)

        val image = PanelDetectionImage.fromSourceOrNull(source, maxByteCount = 3L)

        assertArrayEquals(bytes, source.readByteArray())
        org.junit.jupiter.api.Assertions.assertNull(image)
    }
}
