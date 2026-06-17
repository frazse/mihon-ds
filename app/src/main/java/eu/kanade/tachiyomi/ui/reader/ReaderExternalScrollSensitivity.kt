package eu.kanade.tachiyomi.ui.reader

import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences

object ReaderExternalScrollSensitivity {

    fun scaleDistance(distance: Float, sensitivityPercent: Int): Float {
        val clampedSensitivity = sensitivityPercent.coerceIn(
            ReaderPreferences.SECONDARY_DISPLAY_SCROLL_SENSITIVITY_MIN,
            ReaderPreferences.SECONDARY_DISPLAY_SCROLL_SENSITIVITY_MAX,
        )
        return distance * (clampedSensitivity / 100f)
    }
}
