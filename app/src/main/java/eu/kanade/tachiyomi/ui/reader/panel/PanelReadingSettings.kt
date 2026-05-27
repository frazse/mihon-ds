package eu.kanade.tachiyomi.ui.reader.panel

import kotlin.math.roundToInt

object PanelReadingSettings {
    const val PANEL_TRANSITION_MIN_MILLIS = 0
    const val PANEL_TRANSITION_MAX_MILLIS = 1000
    const val PANEL_TRANSITION_STEP_MILLIS = 50
    const val PANEL_TRANSITION_DEFAULT_MILLIS = 250

    const val PANEL_FOCUS_STRENGTH_MIN = 0
    const val PANEL_FOCUS_STRENGTH_MAX = 100
    const val PANEL_FOCUS_STRENGTH_DEFAULT = 80

    private const val MAX_DIM_ALPHA = 255

    fun normalizeTransitionMillis(value: Int): Int {
        val clamped = value.coerceIn(PANEL_TRANSITION_MIN_MILLIS, PANEL_TRANSITION_MAX_MILLIS)
        return ((clamped + PANEL_TRANSITION_STEP_MILLIS / 2) / PANEL_TRANSITION_STEP_MILLIS) *
            PANEL_TRANSITION_STEP_MILLIS
    }

    fun normalizeFocusStrength(value: Int): Int {
        return value.coerceIn(PANEL_FOCUS_STRENGTH_MIN, PANEL_FOCUS_STRENGTH_MAX)
    }

    fun dimAlphaForStrength(strength: Int): Int {
        return (MAX_DIM_ALPHA * (normalizeFocusStrength(strength) / 100f)).roundToInt()
    }
}

enum class PanelFocusEffect {
    OFF,
    DARKEN,
}
