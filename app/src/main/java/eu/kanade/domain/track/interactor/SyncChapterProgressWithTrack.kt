package eu.kanade.domain.track.interactor

import eu.kanade.domain.track.model.toDbTrack
import eu.kanade.tachiyomi.data.track.EnhancedTracker
import eu.kanade.tachiyomi.data.track.Tracker
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.chapter.interactor.GetChaptersByMangaId
import tachiyomi.domain.chapter.interactor.UpdateChapter
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.chapter.model.ChapterUpdate
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
        if (tracker !is EnhancedTracker) {
            return
        }

        try {
            val dbChapters = getRecognizedChapters(mangaId)
            val localLastRead = getContinuousLocalLastRead(dbChapters)
            val lastRead = max(remoteTrack.lastChapterRead, localLastRead)
            val updatedTrack = remoteTrack.copy(lastChapterRead = lastRead)

            if (lastRead > remoteTrack.lastChapterRead) {
                tracker.update(updatedTrack.toDbTrack())
                insertTrack.await(updatedTrack)
            }

            updateChaptersReadFromTrack(dbChapters, remoteTrack, tracker)
        } catch (e: Throwable) {
            logcat(LogPriority.WARN, e)
        }
    }

    suspend fun syncFromTrack(
        mangaId: Long,
        remoteTrack: Track,
        tracker: Tracker,
    ) {
        try {
            updateChaptersReadFromTrack(getRecognizedChapters(mangaId), remoteTrack, tracker)
        } catch (e: Throwable) {
            logcat(LogPriority.WARN, e)
        }
    }

    private suspend fun getRecognizedChapters(mangaId: Long): List<Chapter> {
        return getChaptersByMangaId.await(mangaId)
            .sortedByDescending { it.sourceOrder }
            .filter { it.isRecognizedNumber }
    }

    private fun getContinuousLocalLastRead(dbChapters: List<Chapter>): Double {
        return dbChapters
            .sortedBy { it.chapterNumber }
            .takeWhile { it.read }
            .lastOrNull()
            ?.chapterNumber ?: 0.0
    }

    private suspend fun updateChaptersReadFromTrack(
        dbChapters: List<Chapter>,
        remoteTrack: Track,
        tracker: Tracker,
    ) {
        if (tracker.hasNotStartedReading(remoteTrack.status)) {
            return
        }

        var lastCheckChapter: Double
        var checkingChapter = 0.0
        // Use source order to stop at volume resets or other non-continuous chapter numbering.
        val chapterUpdates = dbChapters
            .takeWhile { chapter ->
                lastCheckChapter = checkingChapter
                checkingChapter = chapter.chapterNumber
                chapter.chapterNumber >= lastCheckChapter && chapter.chapterNumber <= remoteTrack.lastChapterRead
            }
            .filter { chapter -> !chapter.read }
            .map { ChapterUpdate(id = it.id, read = true) }

        if (chapterUpdates.isNotEmpty()) {
            updateChapter.awaitAll(chapterUpdates)
        }
    }
}
