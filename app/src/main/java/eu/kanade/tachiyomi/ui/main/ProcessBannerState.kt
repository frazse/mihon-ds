package eu.kanade.tachiyomi.ui.main

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object ProcessBannerState {

    private val _state = MutableStateFlow<State?>(null)
    val state = _state.asStateFlow()

    fun show(
        id: Long,
        title: String,
        subtitle: String? = null,
        progress: Float? = null,
    ) {
        _state.update {
            State(
                id = id,
                title = title,
                subtitle = subtitle,
                progress = progress,
            )
        }
    }

    fun hide(id: Long) {
        _state.update {
            if (it?.id == id) null else it
        }
    }

    data class State(
        val id: Long,
        val title: String,
        val subtitle: String? = null,
        val progress: Float? = null,
    )

    const val LIBRARY_UPDATE_ID = 1L
    const val SYNC_ID = 2L
    const val BACKUP_RESTORE_ID = 3L
}
