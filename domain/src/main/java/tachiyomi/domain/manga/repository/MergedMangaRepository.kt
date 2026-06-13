package tachiyomi.domain.manga.repository

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.manga.model.MergedManga

interface MergedMangaRepository {

    suspend fun getMergedMangaForManga(mangaId: Long): List<MergedManga>

    fun getMergedMangaForMangaAsFlow(mangaId: Long): Flow<List<MergedManga>>

    suspend fun insert(mergedManga: MergedManga)

    suspend fun deleteByMangaId(mangaId: Long)
}
