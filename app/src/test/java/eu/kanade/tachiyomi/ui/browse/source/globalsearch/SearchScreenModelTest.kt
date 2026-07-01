package eu.kanade.tachiyomi.ui.browse.source.globalsearch

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class SearchScreenModelTest {

    @Test
    fun `default search filter is all when no sources are pinned`() {
        defaultSearchSourceFilter(emptySet()) shouldBe SourceFilter.All
    }

    @Test
    fun `default search filter is pinned only when sources are pinned`() {
        defaultSearchSourceFilter(setOf("1")) shouldBe SourceFilter.PinnedOnly
    }
}
