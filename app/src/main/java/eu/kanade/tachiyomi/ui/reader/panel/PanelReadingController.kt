package eu.kanade.tachiyomi.ui.reader.panel

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat

class PanelReadingController(
    private val scope: CoroutineScope,
    private val detector: PanelDetector,
    private val isEnabled: () -> Boolean,
    private val readingDirection: () -> PanelReadingDirection,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) {

    private val cachedRawPanels = LinkedHashMap<PanelPageKey, List<ReaderPanel>>()
    private val queuedInputs = LinkedHashMap<PanelPageKey, PanelDetectionInput>()
    private var runningJob: Job? = null
    private var runningKey: PanelPageKey? = null
    private var visibleKeys = emptyList<PanelPageKey>()
    private val mutableState = MutableStateFlow(PanelReadingState())

    val state: StateFlow<PanelReadingState> = mutableState.asStateFlow()

    fun setEnabledState(enabled: Boolean) {
        mutableState.value = if (enabled) {
            mutableState.value.copy(enabled = true)
        } else {
            cancel()
            PanelReadingState(enabled = false)
        }
    }

    fun onPageImageReady(input: PanelDetectionInput) {
        if (!isEnabled()) return

        if (input.image.byteCount > PanelDetectionImage.MAX_SNAPSHOT_BYTE_COUNT) {
            onPageDetectionUnavailable(input.key)
            return
        }

        cachedRawPanels[input.key]?.let { rawPanels ->
            if (isVisible(input.key)) {
                activatePage(input.key, rawPanels, preferredPanelIndex = -1)
            }
            return
        }

        if (runningKey == input.key || queuedInputs.containsKey(input.key)) return

        if (isVisible(input.key)) {
            mutableState.value = PanelReadingState(
                enabled = true,
                key = input.key,
                status = PanelDetectionStatus.Pending,
            )
        }

        queuedInputs[input.key] = input
        trimQueuedInputs()
        startNextDetection()
    }

    fun onPageDetectionUnavailable(key: PanelPageKey) {
        if (!isEnabled()) return

        queuedInputs.remove(key)
        if (isVisible(key)) {
            mutableState.value = PanelReadingState(
                enabled = true,
                key = key,
                status = PanelDetectionStatus.Unavailable,
            )
        }
    }

    fun onVisiblePagesChanged(keys: Collection<PanelPageKey>) {
        visibleKeys = keys.toList()
        if (!isEnabled()) return

        val current = mutableState.value
        if (current.key != null && visibleKeys.any { it.hasSameLogicalPage(current.key) }) {
            startNextDetection()
            return
        }

        val cachedEntry = cachedRawPanels.entries.firstOrNull { entry ->
            isVisible(entry.key)
        }
        if (cachedEntry != null) {
            activatePage(cachedEntry.key, cachedEntry.value, preferredPanelIndex = -1)
            startNextDetection()
            return
        }

        val pendingKey = runningKey?.takeIf(::isVisible)
            ?: queuedInputs.keys.firstOrNull(::isVisible)
        mutableState.value = if (pendingKey != null) {
            PanelReadingState(
                enabled = true,
                key = pendingKey,
                status = PanelDetectionStatus.Pending,
            )
        } else {
            PanelReadingState(enabled = true)
        }

        startNextDetection()
    }

    private fun startNextDetection() {
        if (!isEnabled() || runningJob != null) return

        val input = nextQueuedInput() ?: return
        queuedInputs.remove(input.key)
        runningKey = input.key

        if (isVisible(input.key)) {
            mutableState.value = PanelReadingState(
                enabled = true,
                key = input.key,
                status = PanelDetectionStatus.Pending,
            )
        }

        runningJob = scope.launch {
            try {
                val rawPanels = withContext(dispatcher) {
                    val result = detector.detect(input)
                    result.panels
                }

                if (runningJob != this.coroutineContext[Job]) {
                    return@launch
                }

                cachedRawPanels[input.key] = rawPanels
                trimCachedPanels()
                if (!isEnabled()) {
                    resetIfCurrent(input.key)
                    return@launch
                }
                if (isVisible(input.key)) {
                    activatePage(input.key, rawPanels, preferredPanelIndex = -1)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (!isEnabled()) {
                    resetIfCurrent(input.key)
                    return@launch
                }
                if (!isVisible(input.key) || runningJob != this.coroutineContext[Job]) {
                    return@launch
                }

                mutableState.value = PanelReadingState(
                    enabled = true,
                    key = input.key,
                    status = PanelDetectionStatus.Failed(e.message.orEmpty()),
                )
                logcat(LogPriority.WARN, e) { "Panel detection failed for ${input.key}" }
            } finally {
                if (runningJob == this.coroutineContext[Job]) {
                    runningJob = null
                    runningKey = null
                    startNextDetection()
                }
            }
        }
    }

    fun moveToNextPanel(key: PanelPageKey): PanelMoveResult {
        if (!isEnabled()) return PanelMoveResult.Disabled

        val current = mutableState.value
        if (current.key == key && current.status is PanelDetectionStatus.Pending) return PanelMoveResult.Pending
        if (current.key != key || current.panels.isEmpty()) return PanelMoveResult.NoPanelStep

        val nextIndex = current.panelIndex + 1
        if (nextIndex !in current.panels.indices) return PanelMoveResult.NoPanelStep

        mutableState.value = current.copy(panelIndex = nextIndex)
        return PanelMoveResult.Moved
    }

    fun moveToPreviousPanel(key: PanelPageKey): PanelMoveResult {
        if (!isEnabled()) return PanelMoveResult.Disabled

        val current = mutableState.value
        if (current.key == key && current.status is PanelDetectionStatus.Pending) return PanelMoveResult.Pending
        if (current.key != key || current.panels.isEmpty()) return PanelMoveResult.NoPanelStep

        val previousIndex = current.panelIndex - 1
        if (previousIndex !in current.panels.indices) return PanelMoveResult.NoPanelStep

        mutableState.value = current.copy(panelIndex = previousIndex)
        return PanelMoveResult.Moved
    }

    fun selectPanel(key: PanelPageKey, panelIndex: Int): Boolean {
        if (!isEnabled()) return false

        val current = mutableState.value
        if (current.key != key || panelIndex !in current.panels.indices) return false

        mutableState.value = current.copy(panelIndex = panelIndex)
        return true
    }

    fun activePanelFor(key: PanelPageKey): ReaderPanel? {
        val current = mutableState.value
        return current.activePanel.takeIf { current.key == key }
    }

    fun cancel() {
        runningJob?.cancel()
        runningJob = null
        runningKey = null
        queuedInputs.clear()
    }

    private fun isVisible(key: PanelPageKey): Boolean {
        return visibleKeys.any { it.hasSameLogicalPage(key) }
    }

    private fun nextQueuedInput(): PanelDetectionInput? {
        queuedInputs.values.firstOrNull { isVisible(it.key) }?.let { return it }

        val current = mutableState.value
        val canPrefetchOffscreen = visibleKeys.isEmpty() ||
            (current.key != null && isVisible(current.key) && current.status !is PanelDetectionStatus.Pending)
        return if (canPrefetchOffscreen) {
            queuedInputs.values.firstOrNull()
        } else {
            null
        }
    }

    private fun trimQueuedInputs() {
        while (queuedInputs.size > MAX_QUEUED_INPUTS || queuedImageBytes() > MAX_QUEUED_IMAGE_BYTES) {
            val removableKey = queuedInputs.keys.firstOrNull { !isVisible(it) }
                ?: return
            queuedInputs.remove(removableKey)
        }
    }

    private fun trimCachedPanels() {
        while (cachedRawPanels.size > MAX_CACHED_PANEL_PAGES) {
            val removableKey = cachedRawPanels.keys.firstOrNull { !isVisible(it) }
                ?: cachedRawPanels.keys.firstOrNull()
                ?: return
            cachedRawPanels.remove(removableKey)
        }
    }

    private fun queuedImageBytes(): Long {
        return queuedInputs.values.sumOf { it.image.byteCount.toLong() }
    }

    private fun resetIfCurrent(key: PanelPageKey) {
        if (mutableState.value.key?.hasSameLogicalPage(key) == true) {
            mutableState.value = PanelReadingState(enabled = false)
        }
    }

    private fun activatePage(
        key: PanelPageKey,
        rawPanels: List<ReaderPanel>,
        preferredPanelIndex: Int,
    ) {
        val panels = PanelSorter.sort(rawPanels, readingDirection())
        val panelIndex = when {
            panels.isEmpty() -> -1
            preferredPanelIndex < 0 -> 0
            else -> preferredPanelIndex.coerceIn(panels.indices)
        }
        mutableState.value = PanelReadingState(
            enabled = true,
            key = key,
            panels = panels,
            panelIndex = panelIndex,
            status = if (panels.isNotEmpty()) {
                PanelDetectionStatus.Ready(panels)
            } else {
                PanelDetectionStatus.Unavailable
            },
        )
    }
}

enum class PanelMoveResult {
    Moved,
    Pending,
    NoPanelStep,
    Disabled,
}

private const val MAX_QUEUED_INPUTS = 4
private const val MAX_QUEUED_IMAGE_BYTES = 16L * 1024L * 1024L
private const val MAX_CACHED_PANEL_PAGES = 32
