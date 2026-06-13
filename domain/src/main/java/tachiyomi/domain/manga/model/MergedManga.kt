package tachiyomi.domain.manga.model

import java.io.Serializable

data class MergedManga(
    val id: Long,
    val mangaId: Long,
    val mergeMangaId: Long,
) : Serializable
