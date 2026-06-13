package tachiyomi.data.manga

import tachiyomi.domain.manga.model.MergedManga

object MergedMangaMapper {
    fun mapMergedManga(
        id: Long,
        mangaId: Long,
        mergeMangaId: Long,
    ): MergedManga = MergedManga(
        id = id,
        mangaId = mangaId,
        mergeMangaId = mergeMangaId,
    )
}
