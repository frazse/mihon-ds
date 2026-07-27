package eu.kanade.tachiyomi.ui.reader.panel

import android.content.Context
import android.content.SharedPreferences
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import java.util.Locale

/**
 * Manages manual panel order corrections provided by the user.
 *
 * Uses a "Fuzzy Layout DNA" to identify pages across different manga
 * that share similar panel structures.
 */
class PanelCorrectionStore(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("panel_order_corrections", Context.MODE_PRIVATE)

    fun saveCorrection(panels: List<ReaderPanel>, correctedIndices: List<Int>) {
        val hash = generateLayoutHash(panels)
        val value = correctedIndices.joinToString(",")
        prefs.edit().putString(hash, value).apply()
        logcat(LogPriority.INFO) { "AI TRAINING: Pattern Saved! DNA=$hash. PERSISTENCE CHECK: Key exists in Prefs: ${prefs.contains(hash)}" }
    }

    fun getCorrection(panels: List<ReaderPanel>): List<Int>? {
        val hash = generateLayoutHash(panels)
        val value = prefs.getString(hash, null)

        if (value != null) {
            logcat(LogPriority.INFO) { "AI TRAINING: Pattern MATCHED! DNA=$hash" }
            return value.split(",").mapNotNull { it.toIntOrNull() }.takeIf { it.size == panels.size }
        } else {
            logcat(LogPriority.INFO) { "AI TRAINING: Pattern MISSED. DNA=$hash" }
            return null
        }
    }

    /**
     * Generates a stable Fuzzy Geometric Signature for a page.
     *
     * IMPROVED: Now uses 10% buckets to reduce jitter sensitivity even further.
     */
    private fun generateLayoutHash(panels: List<ReaderPanel>): String {
        if (panels.isEmpty()) return "empty"

        // 1. Find bounding box of all panels to normalize
        var minL = Float.MAX_VALUE; var minT = Float.MAX_VALUE
        var maxR = Float.MIN_VALUE; var maxB = Float.MIN_VALUE
        for (p in panels) {
            minL = minOf(minL, p.bounds.left); minT = minOf(minT, p.bounds.top)
            maxR = maxOf(maxR, p.bounds.right); maxB = maxOf(maxB, p.bounds.bottom)
        }
        val w = maxOf(1f, maxR - minL); var h = maxOf(1f, maxB - minT)

        // 2. Sort by geometric position (Top then Left) to ensure signature is stable regardless of input list order
        val sortedPanels = panels.sortedWith(compareBy({ it.bounds.top }, { it.bounds.left }))

        // 3. Round to nearest 10% (High fuzzy tolerance)
        val signature = sortedPanels.asSequence()
            .map { p ->
                val nl = (((p.bounds.left - minL) / w * 10).toInt() * 10)
                val nt = (((p.bounds.top - minT) / h * 10).toInt() * 10)
                val nr = (((p.bounds.right - minL) / w * 10).toInt() * 10)
                val nb = (((p.bounds.bottom - minT) / h * 10).toInt() * 10)
                "($nl,$nt,$nr,$nb)"
            }
            .joinToString("|")

        return String.format(Locale.ROOT, "FUZZY_V2:%d:%s", panels.size, signature)
    }
}
