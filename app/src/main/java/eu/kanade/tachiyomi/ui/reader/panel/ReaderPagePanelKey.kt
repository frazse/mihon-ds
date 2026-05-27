package eu.kanade.tachiyomi.ui.reader.panel

import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.ui.reader.model.InsertPage
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage

fun ReaderPage.panelPageKey(
    renderVariant: PanelPageRenderVariant = PanelPageRenderVariant.FULL,
): PanelPageKey {
    val chapter = runCatching { chapter.chapter }.getOrNull()
    return PanelPageKey(
        chapterId = chapter?.id,
        chapterUrl = chapter.safeUrl(),
        chapterName = chapter.safeName(),
        pageIndex = index,
        imageUrl = imageUrl,
        pageUrl = url,
        isInsertPage = this is InsertPage,
        renderVariant = renderVariant,
    )
}

private fun Chapter?.safeUrl(): String {
    return this?.let { runCatching { it.url }.getOrNull() }.orEmpty()
}

private fun Chapter?.safeName(): String {
    return this?.let { runCatching { it.name }.getOrNull() }.orEmpty()
}
