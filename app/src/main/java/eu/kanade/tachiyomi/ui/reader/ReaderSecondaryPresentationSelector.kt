package eu.kanade.tachiyomi.ui.reader

enum class SecondaryPresentationMode {
    CONTROLS,
    PAGE,
}

object ReaderSecondaryPresentationSelector {
    fun mode(
        companionPageEnabled: Boolean,
        panelReadingEnabled: Boolean,
    ): SecondaryPresentationMode {
        return if (companionPageEnabled || panelReadingEnabled) {
            SecondaryPresentationMode.PAGE
        } else {
            SecondaryPresentationMode.CONTROLS
        }
    }
}
