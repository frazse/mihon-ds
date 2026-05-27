package eu.kanade.tachiyomi.ui.reader.panel

import android.graphics.RectF

data class PanelModelTransform(
    val sourceWidth: Int,
    val sourceHeight: Int,
    val inputSize: Int,
    val scale: Float,
    val padX: Float,
    val padY: Float,
) {
    fun contentRect(): RectF {
        return RectF().apply {
            left = padX
            top = padY
            right = padX + sourceWidth * scale
            bottom = padY + sourceHeight * scale
        }
    }

    companion object {
        fun letterbox(
            sourceWidth: Int,
            sourceHeight: Int,
            inputSize: Int,
        ): PanelModelTransform {
            val scale = minOf(
                inputSize.toFloat() / sourceWidth.toFloat(),
                inputSize.toFloat() / sourceHeight.toFloat(),
            )
            return PanelModelTransform(
                sourceWidth = sourceWidth,
                sourceHeight = sourceHeight,
                inputSize = inputSize,
                scale = scale,
                padX = (inputSize - sourceWidth * scale) / 2f,
                padY = (inputSize - sourceHeight * scale) / 2f,
            )
        }
    }
}
