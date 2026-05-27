package eu.kanade.tachiyomi.ui.reader.viewer

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import eu.kanade.tachiyomi.ui.reader.panel.PanelFocusEffect
import eu.kanade.tachiyomi.ui.reader.panel.PanelReadingSettings

class PanelHighlightOverlay @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    data class PanelRegion(
        val panelIndex: Int,
        val number: Int?,
        val bounds: RectF,
        val active: Boolean,
    )

    var panelRegions: List<PanelRegion> = emptyList()
        set(value) {
            field = value.map { region ->
                region.copy(bounds = RectF(region.bounds))
            }
            invalidate()
        }

    var focusEffect: PanelFocusEffect = PanelFocusEffect.DARKEN
        set(value) {
            if (field == value) return
            field = value
            invalidate()
        }

    var focusStrength: Int = PanelReadingSettings.PANEL_FOCUS_STRENGTH_DEFAULT
        set(value) {
            val normalized = PanelReadingSettings.normalizeFocusStrength(value)
            if (field == normalized) return
            field = normalized
            invalidate()
        }

    var onPanelClick: ((Int) -> Unit)? = null

    private val dimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val activeAccentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(115, 58, 137, 255)
        style = Paint.Style.STROKE
        strokeWidth = 8f * resources.displayMetrics.density
    }

    private val activeOutlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(235, 90, 156, 255)
        style = Paint.Style.STROKE
        strokeWidth = 5f
    }

    private val inactiveOutlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(185, 245, 247, 250)
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    private val numberBackgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(220, 28, 32, 38)
        style = Paint.Style.FILL
    }

    private val activeNumberBackgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(235, 58, 137, 255)
        style = Paint.Style.FILL
    }

    private val numberTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        textSize = 12f * resources.displayMetrics.scaledDensity
        typeface = Typeface.DEFAULT_BOLD
    }

    private val dimPath = Path()
    private val cornerRadius = 6f
    private val numberBounds = RectF()
    private var pressedPanelIndex: Int? = null

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val activeRegion = panelRegions.firstOrNull { it.active }
        activeRegion?.let { drawFocusEffect(canvas, it) }

        panelRegions
            .filterNot { it.active }
            .forEach { region ->
                canvas.drawRoundRect(region.bounds, cornerRadius, cornerRadius, inactiveOutlinePaint)
                drawNumber(canvas, region)
            }

        activeRegion?.let { region ->
            canvas.drawRoundRect(region.bounds, cornerRadius, cornerRadius, activeAccentPaint)
            canvas.drawRoundRect(region.bounds, cornerRadius, cornerRadius, activeOutlinePaint)
            drawNumber(canvas, region)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val callback = onPanelClick ?: return false
        val hitRegion = panelRegions
            .asReversed()
            .firstOrNull { it.bounds.contains(event.x, event.y) }

        return when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                pressedPanelIndex = hitRegion?.panelIndex
                hitRegion != null
            }
            MotionEvent.ACTION_UP -> {
                val pressedIndex = pressedPanelIndex
                pressedPanelIndex = null
                if (pressedIndex != null && hitRegion?.panelIndex == pressedIndex) {
                    callback(pressedIndex)
                    performClick()
                    true
                } else {
                    false
                }
            }
            MotionEvent.ACTION_CANCEL -> {
                pressedPanelIndex = null
                false
            }
            else -> pressedPanelIndex != null
        }
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    private fun drawNumber(canvas: Canvas, region: PanelRegion) {
        val number = region.number ?: return
        val text = number.toString()
        val horizontalPadding = 7f * resources.displayMetrics.density
        val badgeHeight = 20f * resources.displayMetrics.density
        val badgeWidth = maxOf(
            badgeHeight,
            numberTextPaint.measureText(text) + horizontalPadding * 2f,
        )
        val left = region.bounds.left + 6f * resources.displayMetrics.density
        val top = region.bounds.top + 6f * resources.displayMetrics.density
        numberBounds.set(left, top, left + badgeWidth, top + badgeHeight)

        canvas.drawRoundRect(
            numberBounds,
            badgeHeight / 2f,
            badgeHeight / 2f,
            if (region.active) activeNumberBackgroundPaint else numberBackgroundPaint,
        )

        val baseline = numberBounds.centerY() - (numberTextPaint.descent() + numberTextPaint.ascent()) / 2f
        canvas.drawText(text, numberBounds.centerX(), baseline, numberTextPaint)
    }

    private fun drawFocusEffect(canvas: Canvas, activeRegion: PanelRegion) {
        if (focusStrength <= 0 || focusEffect == PanelFocusEffect.OFF) return

        buildOutsidePath(activeRegion.bounds)
        when (focusEffect) {
            PanelFocusEffect.OFF -> Unit
            PanelFocusEffect.DARKEN -> drawDim(canvas, alpha = PanelReadingSettings.dimAlphaForStrength(focusStrength))
        }
    }

    private fun buildOutsidePath(activeBounds: RectF) {
        dimPath.reset()
        dimPath.fillType = Path.FillType.EVEN_ODD
        dimPath.addRect(0f, 0f, width.toFloat(), height.toFloat(), Path.Direction.CW)
        dimPath.addRoundRect(activeBounds, cornerRadius, cornerRadius, Path.Direction.CW)
    }

    private fun drawDim(canvas: Canvas, alpha: Int) {
        if (alpha <= 0) return

        dimPaint.color = Color.argb(alpha, 0, 0, 0)
        canvas.drawPath(dimPath, dimPaint)
    }
}
