package eu.kanade.tachiyomi.ui.reader

import io.kotest.matchers.floats.shouldBeExactly
import org.junit.jupiter.api.Test

class ReaderExternalScrollSensitivityTest {

    @Test
    fun `100 percent keeps external scroll one to one`() {
        ReaderExternalScrollSensitivity.scaleDistance(
            distance = 42.5f,
            sensitivityPercent = 100,
        ) shouldBeExactly 42.5f
    }

    @Test
    fun `lower and higher percentages slow down and speed up external scroll`() {
        ReaderExternalScrollSensitivity.scaleDistance(
            distance = 80f,
            sensitivityPercent = 50,
        ) shouldBeExactly 40f
        ReaderExternalScrollSensitivity.scaleDistance(
            distance = 80f,
            sensitivityPercent = 500,
        ) shouldBeExactly 400f
    }

    @Test
    fun `sensitivity is clamped to supported slider range`() {
        ReaderExternalScrollSensitivity.scaleDistance(
            distance = 100f,
            sensitivityPercent = 25,
        ) shouldBeExactly 50f
        ReaderExternalScrollSensitivity.scaleDistance(
            distance = 100f,
            sensitivityPercent = 600,
        ) shouldBeExactly 500f
    }
}
