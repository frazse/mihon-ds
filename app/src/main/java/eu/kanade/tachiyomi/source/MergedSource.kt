package eu.kanade.tachiyomi.source

import tachiyomi.domain.source.model.Source as DomainSource

class MergedSource : Source {
    override val id: Long = ID
    override val name: String = "Merged"
    override val lang: String = ""

    companion object {
        const val ID = -6L

        fun isMerged(source: DomainSource): Boolean = source.id == ID
        fun isMerged(sourceId: Long): Boolean = sourceId == ID
    }
}
