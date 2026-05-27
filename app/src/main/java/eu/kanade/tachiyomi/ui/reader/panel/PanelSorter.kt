package eu.kanade.tachiyomi.ui.reader.panel

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

object PanelSorter {

    fun sort(
        panels: List<ReaderPanel>,
        direction: PanelReadingDirection,
    ): List<ReaderPanel> {
        val valid = panels
            .filter { it.width >= MIN_PANEL_SIZE && it.height >= MIN_PANEL_SIZE }
            .removeDuplicatePanels()
            .sortedBy { it.bounds.top }

        if (valid.isEmpty()) return emptyList()

        val rowTolerance = valid
            .map { it.height }
            .sorted()
            .let { heights -> heights[heights.lastIndex / 2] * 0.5f }
            .coerceAtLeast(MIN_ROW_TOLERANCE)

        val rows = mutableListOf<MutableList<ReaderPanel>>()
        valid.forEach { panel ->
            val row = rows.firstOrNull { existing ->
                abs(existing.averageCenterY() - panel.centerY) <= rowTolerance
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
    private const val DUPLICATE_IOU_THRESHOLD = 0.72f
    private const val CONTAINED_DUPLICATE_COVERAGE_THRESHOLD = 0.9f
    private const val CONTAINED_DUPLICATE_SIZE_RATIO_THRESHOLD = 0.65f
}
