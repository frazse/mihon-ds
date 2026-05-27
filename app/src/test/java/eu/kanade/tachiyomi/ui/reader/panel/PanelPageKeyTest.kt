package eu.kanade.tachiyomi.ui.reader.panel

import eu.kanade.tachiyomi.data.database.models.ChapterImpl
import eu.kanade.tachiyomi.ui.reader.model.InsertPage
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PanelPageKeyTest {

    @Test
    fun `creates key when chapter id is null`() {
        val page = ReaderPage(
            index = 4,
            url = "page://4",
            imageUrl = "image://4",
        ).apply {
            chapter = readerChapter(id = null, url = "chapter://1", name = "Chapter 1")
        }

        val key = page.panelPageKey()

        assertNull(key.chapterId)
        assertEquals("chapter://1", key.chapterUrl)
        assertEquals("Chapter 1", key.chapterName)
        assertEquals(4, key.pageIndex)
        assertEquals("image://4", key.imageUrl)
        assertEquals("page://4", key.pageUrl)
        assertEquals(PanelPageRenderVariant.FULL, key.renderVariant)
    }

    @Test
    fun `null id chapters with default page urls keep distinct keys`() {
        val firstPage = ReaderPage(index = 0).apply {
            chapter = readerChapter(id = null, url = "chapter://first", name = "First Chapter")
        }
        val secondPage = ReaderPage(index = 0).apply {
            chapter = readerChapter(id = null, url = "chapter://second", name = "Second Chapter")
        }

        assertNotEquals(firstPage.panelPageKey(), secondPage.panelPageKey())
    }

    @Test
    fun `insert page uses distinct key from parent split page`() {
        val parentPage = ReaderPage(index = 2, url = "page://2", imageUrl = "image://2").apply {
            chapter = readerChapter(id = null, url = "chapter://split", name = "Split Chapter")
        }
        val insertPage = InsertPage(parentPage)

        assertNotEquals(parentPage.panelPageKey(), insertPage.panelPageKey())
        assertEquals(insertPage.panelPageKey(), InsertPage(parentPage).panelPageKey())
    }

    @Test
    fun `render variants keep distinct keys for same logical page`() {
        val page = ReaderPage(index = 1, url = "page://1", imageUrl = "image://1").apply {
            chapter = readerChapter(id = 5L, url = "chapter://5", name = "Chapter 5")
        }

        val splitLeft = page.panelPageKey(PanelPageRenderVariant.SPLIT_LEFT)
        val splitRight = page.panelPageKey(PanelPageRenderVariant.SPLIT_RIGHT)

        assertNotEquals(splitLeft, splitRight)
        assertTrue(splitLeft.hasSameLogicalPage(splitRight))
    }

    @Test
    fun `logical page identity does not collapse distinct chapters`() {
        val firstPage = ReaderPage(index = 0).apply {
            chapter = readerChapter(id = null, url = "chapter://first", name = "First Chapter")
        }
        val secondPage = ReaderPage(index = 0).apply {
            chapter = readerChapter(id = null, url = "chapter://second", name = "Second Chapter")
        }

        assertEquals(false, firstPage.panelPageKey().hasSameLogicalPage(secondPage.panelPageKey()))
    }

    @Test
    fun `creates fallback key when reader page chapter is uninitialized`() {
        val key = ReaderPage(index = 0).panelPageKey()

        assertNull(key.chapterId)
        assertEquals("", key.chapterUrl)
        assertEquals("", key.chapterName)
    }

    @Test
    fun `creates fallback key when chapter url and name are uninitialized`() {
        val page = ReaderPage(index = 0).apply {
            chapter = ReaderChapter(ChapterImpl().apply { id = null })
        }

        val key = page.panelPageKey()

        assertNull(key.chapterId)
        assertEquals("", key.chapterUrl)
        assertEquals("", key.chapterName)
    }

    private fun readerChapter(
        id: Long?,
        url: String,
        name: String,
    ): ReaderChapter {
        return ReaderChapter(
            ChapterImpl().apply {
                this.id = id
                this.url = url
                this.name = name
            },
        )
    }
}
