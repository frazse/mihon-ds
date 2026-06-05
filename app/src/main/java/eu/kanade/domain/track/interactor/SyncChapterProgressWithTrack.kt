package eu.kanade.domain.track.interactor

import eu.kanade.domain.track.model.toDbTrack
import eu.kanade.tachiyomi.data.track.EnhancedTracker
import eu.kanade.tachiyomi.data.track.Tracker
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.chapter.interactor.GetChaptersByMangaId
import tachiyomi.domain.chapter.interactor.UpdateChapter
import tachiyomi.domain.chapter.model.toChapterUpdate
import tachiyomi.domain.track.interactor.InsertTrack
import tachiyomi.domain.track.model.Track
import kotlin.math.max

class SyncChapterProgressWithTrack(
    private val updateChapter: UpdateChapter,
    private val insertTrack: InsertTrack,
    private val getChaptersByMangaId: GetChaptersByMangaId,
) {

    suspend fun await(
        mangaId: Long,
        remoteTrack: Track,
        tracker: Tracker,
    ) {
        // EnhancedTrackers like Suwayomi manage read state themselves —
        // syncing chapter progress from them is redundant and can cause all
        // chapters to be incorrectly marked as read due to numbering mismatches.
        if (tracker is EnhancedTracker) return

        // Current chapters in database
        val sortedChapters = getChaptersByMangaId.await(mangaId)
            .sortedBy { it.chapterNumber }
            .filter { it.isRecognizedNumber }

        // Chapters to update to follow tracker
        val maxLocalChapter = sortedChapters.lastOrNull()?.chapterNumber?.toDouble() ?: 0.0
        val chapterUpdates = if (remoteTrack.lastChapterRead > 0.0 && maxLocalChapter > 0.0) {
            val effectiveLastRead = minOf(remoteTrack.lastChapterRead, maxLocalChapter)
            sortedChapters
                .filter { chapter -> chapter.chapterNumber <= effectiveLastRead && !chapter.read }
                .map { it.copy(read = true).toChapterUpdate() }
        } else {
            emptyList()
        }

        // only take into account continuous reading
        val localLastRead = sortedChapters.takeWhile { it.read }.lastOrNull()?.chapterNumber ?: 0F
        val lastRead = max(remoteTrack.lastChapterRead, localLastRead.toDouble())
        val updatedTrack = remoteTrack.copy(lastChapterRead = lastRead)

        try {
            // Update Tracker to localLastRead if needed
            if (updatedTrack.lastChapterRead > remoteTrack.lastChapterRead) {
                tracker.update(updatedTrack.toDbTrack())
                // update Track in database
                insertTrack.await(updatedTrack)
            }
            // Update local chapters following Tracker
            updateChapter.awaitAll(chapterUpdates)
        } catch (e: Throwable) {
            logcat(LogPriority.WARN, e)
        }
    }
}
