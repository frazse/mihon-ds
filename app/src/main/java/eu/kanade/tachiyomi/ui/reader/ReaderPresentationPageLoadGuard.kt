package eu.kanade.tachiyomi.ui.reader

import eu.kanade.tachiyomi.ui.reader.panel.PanelPageKey

internal object ReaderPresentationPageLoadGuard {

    fun shouldStartPageLoad(
        expectedKey: PanelPageKey,
        requestedTag: Any?,
    ): Boolean {
        return requestedTag != expectedKey
    }

    fun canApplyPageLoad(
        expectedKey: PanelPageKey,
        requestedTag: Any?,
        isAttached: Boolean,
    ): Boolean {
        return isAttached && requestedTag == expectedKey
    }
}
