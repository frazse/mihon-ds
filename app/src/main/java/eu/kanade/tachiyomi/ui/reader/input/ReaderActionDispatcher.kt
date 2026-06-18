package eu.kanade.tachiyomi.ui.reader.input

interface ReaderActionTarget {
    fun handleActivityAction(action: ReaderAction): Boolean
    fun handleViewerAction(action: ReaderAction): Boolean
}

object ReaderActionDispatcher {

    private val activityActions = setOf(
        ReaderAction.NEXT_CHAPTER,
        ReaderAction.PREVIOUS_CHAPTER,
        ReaderAction.TOGGLE_MENU,
        ReaderAction.TOGGLE_COMPANION_PAGE,
        ReaderAction.TOGGLE_GUIDED_READING,
        ReaderAction.OPEN_READER_SETTINGS,
    )

    fun dispatch(action: ReaderAction, target: ReaderActionTarget): Boolean {
        return if (action in activityActions) {
            target.handleActivityAction(action)
        } else {
            target.handleViewerAction(action)
        }
    }
}
