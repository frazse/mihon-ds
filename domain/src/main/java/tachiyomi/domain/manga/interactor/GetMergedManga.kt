package tachiyomi.domain.manga.interactor

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.manga.model.MergedManga
import tachiyomi.domain.manga.repository.MergedMangaRepository

class GetMergedManga(
    private val repository: MergedMangaRepository,
) {

    suspend fun await(mangaId: Long): List<MergedManga> {
        return repository.getMergedMangaForManga(mangaId)
    }

    fun subscribe(mangaId: Long): Flow<List<MergedManga>> {
        return repository.getMergedMangaForMangaAsFlow(mangaId)
    }
}
