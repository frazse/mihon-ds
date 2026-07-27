package eu.kanade.tachiyomi.ui.reader.panel

import kotlin.math.max
import kotlin.math.min
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat

object PanelOrderEstimator {

    private const val DEFAULT_THRESHOLD = 0.25f
    private const val MAX_DEPTH = 32

    fun estimateOrder(panels: List<ReaderPanel>, rtl: Boolean = true, sourceWidth: Int = 0, sourceHeight: Int = 0, interceptionRatioThreshold: Float = DEFAULT_THRESHOLD): List<ReaderPanel> {
        if (panels.size <= 1) return panels
        val pageArea = if (sourceWidth > 0 && sourceHeight > 0) (sourceWidth * sourceHeight).toFloat() else 0f
        return orderWithContainment(panels, rtl, interceptionRatioThreshold, pageArea)
    }

    private fun orderWithContainment(panels: List<ReaderPanel>, rtl: Boolean, threshold: Float, pageArea: Float): List<ReaderPanel> {
        if (panels.size <= 1) return panels

        // 1. Identify Nested Panels (Insets)
        val containment = detectContainment(panels)
        val nestedChildren = containment.values.flatten().toSet()
        val topLevel = panels.filter { it !in nestedChildren }

        // 2. Sort the primary layer (Backgrounds/Standard panels)
        val fallbackPanels = mutableSetOf<ReaderPanel>()
        val orderedTopLevel = mutableListOf<ReaderPanel>()
        divide(topLevel, rtl, threshold, depth = 0, orderedTopLevel, fallbackPanels)

        val finalTopLevel = if (pageArea > 0f && fallbackPanels.isNotEmpty()) {
            demoteBackgroundPanels(orderedTopLevel, pageArea, fallbackPanels)
        } else {
            orderedTopLevel
        }

        // 3. Structural Splicing: Insert kids immediately AFTER their container
        val result = mutableListOf<ReaderPanel>()
        for (panel in finalTopLevel) {
            result.add(panel)
            val kids = containment[panel]
            if (!kids.isNullOrEmpty()) {
                result.addAll(orderWithContainment(kids, rtl, threshold, 0f))
            }
        }

        if (result.size < panels.size) result.addAll(panels.filter { it !in result.toSet() })
        return result
    }

    private fun divide(panels: List<ReaderPanel>, rtl: Boolean, threshold: Float, depth: Int, out: MutableList<ReaderPanel>, fallbackPanels: MutableSet<ReaderPanel>) {
        if (panels.size == 1) { out.add(panels[0]); return }
        if (depth > MAX_DEPTH) { val sorted = stableFallbackSort(panels, rtl); fallbackPanels.addAll(sorted); out.addAll(sorted); return }

        val split = highestPriorityDivision(panels, threshold, rtl)
        if (split == null || split.first.size == panels.size || split.second.size == panels.size) {
            val sorted = stableFallbackSort(panels, rtl); fallbackPanels.addAll(sorted); out.addAll(sorted); return
        }

        divide(split.first, rtl, threshold, depth + 1, out, fallbackPanels)
        divide(split.second, rtl, threshold, depth + 1, out, fallbackPanels)
    }

    private fun highestPriorityDivision(panels: List<ReaderPanel>, threshold: Float, rtl: Boolean): Pair<List<ReaderPanel>, List<ReaderPanel>>? {
        val bestH = findBestSplit(panels, threshold, horizontal = true, rtl = rtl)
        val bestV = findBestSplit(panels, threshold, horizontal = false, rtl = rtl)
        return when {
            bestH != null && bestV != null -> if (bestH.interception <= bestV.interception + 0.05f) bestH.groups else bestV.groups
            bestH != null -> bestH.groups
            bestV != null -> bestV.groups
            else -> null
        }
    }

    private data class SplitCandidate(
        val groups: Pair<List<ReaderPanel>, List<ReaderPanel>>,
        val interception: Float
    )

    private fun findBestSplit(
        panels: List<ReaderPanel>,
        threshold: Float,
        horizontal: Boolean,
        rtl: Boolean
    ): SplitCandidate? {
        val candidates = if (horizontal) {
            (panels.map { it.bounds.top } + panels.map { it.bounds.bottom }).distinct().sorted()
        } else {
            (panels.map { it.bounds.left } + panels.map { it.bounds.right }).distinct()
                .let { if (rtl) it.sortedDescending() else it.sorted() }
        }

        var best: SplitCandidate? = null
        for (pivot in candidates) {
            val result = tryPivot(panels, pivot, threshold, horizontal, reversedAxis = !horizontal && rtl)
                ?: continue

            if (result.interception == 0f) return result
            if (best == null || result.interception < best.interception) {
                best = result
            }
        }
        return best
    }

    private fun tryPivot(
        panels: List<ReaderPanel>,
        pivot: Float,
        threshold: Float,
        horizontal: Boolean,
        reversedAxis: Boolean,
    ): SplitCandidate? {
        val sideA = mutableListOf<ReaderPanel>()
        val sideB = mutableListOf<ReaderPanel>()
        var maxInterception = 0f

        for (panel in panels) {
            val zMin: Float; val zMax: Float; val p: Float
            if (horizontal) {
                zMin = panel.bounds.top; zMax = panel.bounds.bottom; p = pivot
            } else if (!reversedAxis) {
                zMin = panel.bounds.left; zMax = panel.bounds.right; p = pivot
            } else {
                zMin = -panel.bounds.right; zMax = -panel.bounds.left; p = -pivot
            }

            val side = pivotSideInternal(zMin, zMax, p, threshold)
            if (side.side == -1) return null

            maxInterception = max(maxInterception, side.interception)
            if (side.side == 0) sideA.add(panel) else sideB.add(panel)
        }

        if (sideA.isEmpty() || sideB.isEmpty()) return null
        return SplitCandidate(sideA to sideB, maxInterception)
    }

    private data class SideResult(val side: Int, val interception: Float)

    private fun pivotSideInternal(zMin: Float, zMax: Float, pivot: Float, threshold: Float): SideResult {
        if (pivot <= zMin) return SideResult(1, 0f)
        if (zMax <= pivot) return SideResult(0, 0f)

        val extent = zMax - zMin
        if (extent <= 0f) return SideResult(0, 0f)

        val pivotRatio = (pivot - zMin) / extent
        val interceptionRatio = minOf(pivotRatio, 1f - pivotRatio)

        return when {
            interceptionRatio > threshold -> SideResult(-1, interceptionRatio)
            pivotRatio > 0.5f -> SideResult(0, interceptionRatio)
            else -> SideResult(1, interceptionRatio)
        }
    }

    private fun detectContainment(panels: List<ReaderPanel>, containmentThreshold: Float = 0.85f, minParentAreaRatio: Float = 1.2f): Map<ReaderPanel, List<ReaderPanel>> {
        val parentOf = mutableMapOf<ReaderPanel, ReaderPanel>()
        for (child in panels) {
            val candidates = panels.filter { p -> p !== child && p.area > child.area * minParentAreaRatio && (intersectionArea(child.bounds, p.bounds) / child.area) >= containmentThreshold }
            if (candidates.isNotEmpty()) parentOf[child] = candidates.minByOrNull { it.area }!!
        }
        val result = mutableMapOf<ReaderPanel, MutableList<ReaderPanel>>()
        for (child in panels) {
            var p = parentOf[child]; while (p != null) { result.getOrPut(p) { mutableListOf() }.add(child); p = parentOf[p] }
        }
        return result
    }

    private fun demoteBackgroundPanels(ordered: List<ReaderPanel>, pageArea: Float, eligiblePanels: Set<ReaderPanel>): List<ReaderPanel> {
        val result = ordered.toMutableList(); var changed = true
        while (changed) {
            changed = false
            for (candidate in result.toList()) {
                if (candidate !in eligiblePanels || candidate.area < pageArea * 0.38f) continue
                val overlapping = result.filter { it !== candidate && it.bounds.overlaps(candidate.bounds) }
                if (overlapping.size < 2) continue
                val candidateIndex = result.indexOf(candidate); val lastOverlapIndex = overlapping.maxOf { result.indexOf(it) }
                if (candidateIndex < lastOverlapIndex) { result.removeAt(candidateIndex); result.add(overlapping.maxOf { result.indexOf(it) } + 1, candidate); changed = true; break }
            }
        }
        return result
    }

    private fun stableFallbackSort(panels: List<ReaderPanel>, rtl: Boolean) = panels.sortedWith(compareBy({ it.centerY }, { if (rtl) -it.centerX else it.centerX }))
    private fun intersectionArea(a: android.graphics.RectF, b: android.graphics.RectF): Float {
        val l = max(a.left, b.left); val t = max(a.top, b.top); val r = min(a.right, b.right); val bo = min(a.bottom, b.bottom)
        return if (r > l && bo > t) (r - l) * (bo - t) else 0f
    }
    private val ReaderPanel.area: Float get() = width * height
    private fun android.graphics.RectF.overlaps(o: android.graphics.RectF) = left < o.right && right > o.left && top < o.bottom && bottom > o.top
}
