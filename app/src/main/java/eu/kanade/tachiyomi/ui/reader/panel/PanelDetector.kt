package eu.kanade.tachiyomi.ui.reader.panel

interface PanelDetector {
    suspend fun detect(input: PanelDetectionInput): PanelDetectionResult
}
