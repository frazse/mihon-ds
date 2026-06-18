package eu.kanade.domain.track.interactor

import eu.kanade.domain.track.model.toDbTrack
import eu.kanade.domain.track.model.toDomainTrack
import eu.kanade.tachiyomi.data.track.Tracker
import eu.kanade.tachiyomi.data.track.TrackerManager
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import tachiyomi.domain.track.interactor.GetTracks
import tachiyomi.domain.track.interactor.InsertTrack

enum class TrackProgressSyncMode {
    EnhancedTrackersOnly,
    AllTrackersFromRemote,
}

class RefreshTracks(
    private val getTracks: GetTracks,
    private val trackerManager: TrackerManager,
    private val insertTrack: InsertTrack,
    private val syncChapterProgressWithTrack: SyncChapterProgressWithTrack,
) {

    /**
     * Fetches updated tracking data from all logged in trackers.
     * Chapter progress is synced for enhanced trackers by default, or pulled from all trackers when requested.
     *
     * @return Failed updates.
     */
    suspend fun await(
        mangaId: Long,
        progressSyncMode: TrackProgressSyncMode = TrackProgressSyncMode.EnhancedTrackersOnly,
    ): List<Pair<Tracker?, Throwable>> {
        return supervisorScope {
            return@supervisorScope getTracks.await(mangaId)
                .map { it to trackerManager.get(it.trackerId) }
                .filter { (_, service) -> service?.isLoggedIn == true }
                .map { (track, service) ->
                    async {
                        return@async try {
                            val tracker = service!!
                            val dbTrack = track.toDbTrack()
                            val updatedTrack = when (progressSyncMode) {
                                TrackProgressSyncMode.EnhancedTrackersOnly -> {
                                    tracker.refresh(dbTrack).toDomainTrack()!!
                                }
                                TrackProgressSyncMode.AllTrackersFromRemote -> {
                                    tracker.fetchRemoteTrack(dbTrack)?.toDomainTrack() ?: return@async null
                                }
                            }
                            insertTrack.await(updatedTrack)
                            when (progressSyncMode) {
                                TrackProgressSyncMode.EnhancedTrackersOnly -> {
                                    syncChapterProgressWithTrack.await(mangaId, updatedTrack, tracker)
                                }
                                TrackProgressSyncMode.AllTrackersFromRemote -> {
                                    syncChapterProgressWithTrack.syncFromTrack(mangaId, updatedTrack, tracker)
                                }
                            }
                            null
                        } catch (e: Throwable) {
                            service to e
                        }
                    }
                }
                .awaitAll()
                .filterNotNull()
        }
    }
}
