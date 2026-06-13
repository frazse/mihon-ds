package tachiyomi.data.manga

import kotlinx.coroutines.flow.Flow
import tachiyomi.data.DatabaseHandler
import tachiyomi.domain.manga.model.MergedManga
import tachiyomi.domain.manga.repository.MergedMangaRepository

class MergedMangaRepositoryImpl(
    private val handler: DatabaseHandler,
) : MergedMangaRepository {

    override suspend fun getMergedMangaForManga(mangaId: Long): List<MergedManga> {
        return handler.awaitList {
            manga_mergerQueries.getMergedMangaForManga(mangaId, MergedMangaMapper::mapMergedManga)
        }
    }

    override fun getMergedMangaForMangaAsFlow(mangaId: Long): Flow<List<MergedManga>> {
        return handler.subscribeToList {
            manga_mergerQueries.getMergedMangaForManga(mangaId, MergedMangaMapper::mapMergedManga)
        }
    }

    override suspend fun insert(mergedManga: MergedManga) {
        handler.await {
            manga_mergerQueries.insert(
                mangaId = mergedManga.mangaId,
                mergeMangaId = mergedManga.mergeMangaId,
            )
        }
    }

    override suspend fun deleteByMangaId(mangaId: Long) {
        handler.await {
            manga_mergerQueries.deleteByMangaId(mangaId)
        }
    }
}
