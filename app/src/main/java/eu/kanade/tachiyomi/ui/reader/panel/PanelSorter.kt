package eu.kanade.tachiyomi.ui.reader.panel

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

object PanelSorter {

    fun sort(
        panels: List<ReaderPanel>,
        direction: PanelReadingDirection,
        algorithm: PanelSortingAlgorithm = PanelSortingAlgorithm.ROW_BASED,
    ): List<ReaderPanel> {
        val valid = panels
            .filter { it.width >= MIN_PANEL_SIZE && it.height >= MIN_PANEL_SIZE }
            .removeDuplicatePanels()

        if (valid.isEmpty()) return emptyList()

        return when (algorithm) {
            PanelSortingAlgorithm.ROW_BASED -> sortRowBased(valid, direction)
            PanelSortingAlgorithm.XY_CUT -> sortXyCut(valid, direction)
        }
    }

    // ── Row-based sorting (original algorithm) ────────────────────────────────

    private fun sortRowBased(
        valid: List<ReaderPanel>,
        direction: PanelReadingDirection,
    ): List<ReaderPanel> {
        val sorted = valid.sortedBy { it.bounds.top }

        val rowTolerance = sorted
            .map { it.height }
            .sorted()
            .let { heights -> heights[heights.lastIndex / 2] * 0.75f }
            .coerceAtLeast(MIN_ROW_TOLERANCE)

        val rows = mutableListOf<MutableList<ReaderPanel>>()
        sorted.forEach { panel ->
            val row = rows.firstOrNull { existing ->
                val rowTop = existing.minOf { it.bounds.top }
                val rowBottom = existing.maxOf { it.bounds.bottom }
                val verticalOverlap = panel.bounds.top < rowBottom && panel.bounds.bottom > rowTop
                val centerClose = abs(existing.averageCenterY() - panel.centerY) <= rowTolerance
                verticalOverlap || centerClose
            }
            if (row != null) {
                row += panel
            } else {
                rows += mutableListOf(panel)
            }
        }

        return rows
            .sortedBy { row -> row.minOf { it.bounds.top } }
            .flatMap { row ->
                when (direction) {
                    PanelReadingDirection.LEFT_TO_RIGHT -> row.sortedBy { it.bounds.left }
                    PanelReadingDirection.RIGHT_TO_LEFT -> row.sortedByDescending { it.bounds.left }
                }
            }
    }

    // ── XY-cut sorting ────────────────────────────────────────────────────────

    private fun sortXyCut(
        valid: List<ReaderPanel>,
        direction: PanelReadingDirection,
    ): List<ReaderPanel> {
        if (valid.size <= 1) return valid
        return xyCut(valid, direction)
    }

    private fun xyCut(
        panels: List<ReaderPanel>,
        direction: PanelReadingDirection,
    ): List<ReaderPanel> {
        if (panels.size <= 1) return panels

        val hCut = findHorizontalCut(panels)
        if (hCut != null) {
            val top = panels.filter { it.bounds.bottom <= hCut }
            val bottom = panels.filter { it.bounds.top >= hCut }
            val overlap = panels.filter { it.bounds.top < hCut && it.bounds.bottom > hCut }
            val topFinal = top + overlap.filter { it.centerY < hCut }
            val bottomFinal = bottom + overlap.filter { it.centerY >= hCut }
            return xyCut(topFinal, direction) + xyCut(bottomFinal, direction)
        }

        val vCut = findVerticalCut(panels)
        if (vCut != null) {
            val left = panels.filter { it.bounds.right <= vCut }
            val right = panels.filter { it.bounds.left >= vCut }
            val overlap = panels.filter { it.bounds.left < vCut && it.bounds.right > vCut }
            val leftFinal = left + overlap.filter { it.centerX < vCut }
            val rightFinal = right + overlap.filter { it.centerX >= vCut }
            return when (direction) {
                PanelReadingDirection.RIGHT_TO_LEFT -> xyCut(rightFinal, direction) + xyCut(leftFinal, direction)
                PanelReadingDirection.LEFT_TO_RIGHT -> xyCut(leftFinal, direction) + xyCut(rightFinal, direction)
            }
        }

        // No clean cut found — fall back to row-based for this sub-region
        return sortRowBased(panels, direction)
    }

    private fun findHorizontalCut(panels: List<ReaderPanel>): Float? {
        val bottoms = panels.map { it.bounds.bottom }.sorted()
        val minTop = panels.minOf { it.bounds.top }
        val maxBottom = bottoms.last()
        return bottoms.dropLast(1).firstOrNull { cutY ->
            cutY > minTop &&
                cutY < maxBottom &&
                panels.none { it.bounds.top < cutY && it.bounds.bottom > cutY }
        }
    }

    private fun findVerticalCut(panels: List<ReaderPanel>): Float? {
        val rights = panels.map { it.bounds.right }.sorted()
        val minLeft = panels.minOf { it.bounds.left }
        val maxRight = rights.last()
        return rights.dropLast(1).firstOrNull { cutX ->
            cutX > minLeft &&
                cutX < maxRight &&
                panels.none { it.bounds.left < cutX && it.bounds.right > cutX }
        }
    }

    // ── Shared helpers ────────────────────────────────────────────────────────

    private val ReaderPanel.centerX: Float
        get() = bounds.left + width / 2f

    private fun List<ReaderPanel>.averageCenterY(): Float {
        return sumOf { it.centerY.toDouble() }.toFloat() / size
    }

    private fun List<ReaderPanel>.removeDuplicatePanels(): List<ReaderPanel> {
        val kept = mutableListOf<ReaderPanel>()
        sortedWith(
            compareByDescending<ReaderPanel> { it.confidence }
                .thenByDescending { it.area },
        ).forEach { candidate ->
            if (kept.none { candidate.isDuplicateOf(it) }) {
                kept += candidate
            }
        }
        return kept
    }

    private fun ReaderPanel.isDuplicateOf(other: ReaderPanel): Boolean {
        val intersection = bounds.intersectionArea(other.bounds)
        if (intersection <= 0f) return false

        val union = area + other.area - intersection
        val iou = if (union > 0f) intersection / union else 0f
        if (iou >= DUPLICATE_IOU_THRESHOLD) return true

        val smallerArea = min(area, other.area)
        val largerArea = max(area, other.area)
        if (smallerArea <= 0f || largerArea <= 0f) return false

        val smallerCoverage = intersection / smallerArea
        val sizeSimilarity = smallerArea / largerArea
        return smallerCoverage >= CONTAINED_DUPLICATE_COVERAGE_THRESHOLD &&
            sizeSimilarity >= CONTAINED_DUPLICATE_SIZE_RATIO_THRESHOLD
    }

    private val ReaderPanel.area: Float
        get() = width * height

    private fun android.graphics.RectF.intersectionArea(other: android.graphics.RectF): Float {
        val left = max(left, other.left)
        val top = max(top, other.top)
        val right = min(right, other.right)
        val bottom = min(bottom, other.bottom)
        return max(0f, right - left) * max(0f, bottom - top)
    }

    private const val MIN_PANEL_SIZE = 8f
    private const val MIN_ROW_TOLERANCE = 24f
    private const val DUPLICATE_IOU_THRESHOLD = 0.55f
    private const val CONTAINED_DUPLICATE_COVERAGE_THRESHOLD = 0.9f
    private const val CONTAINED_DUPLICATE_SIZE_RATIO_THRESHOLD = 0.65f
}
