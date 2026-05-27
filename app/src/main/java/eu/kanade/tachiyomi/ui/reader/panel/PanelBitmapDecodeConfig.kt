package eu.kanade.tachiyomi.ui.reader.panel

internal object PanelBitmapDecodeConfig {

    fun calculateInSampleSize(
        sourceWidth: Int,
        sourceHeight: Int,
        targetMaxSide: Int,
        minimumShortSide: Int,
    ): Int {
        if (sourceWidth <= 0 || sourceHeight <= 0 || targetMaxSide <= 0 || minimumShortSide <= 0) {
            return 1
        }

        val maxSide = maxOf(sourceWidth, sourceHeight)
        val minSide = minOf(sourceWidth, sourceHeight)
        var sampleSize = 1

        while (
            maxSide / (sampleSize * 2) >= targetMaxSide &&
            minSide / (sampleSize * 2) >= minimumShortSide
        ) {
            sampleSize *= 2
        }

        return sampleSize
    }
}
