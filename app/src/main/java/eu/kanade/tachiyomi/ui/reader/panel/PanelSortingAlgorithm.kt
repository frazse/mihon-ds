package eu.kanade.tachiyomi.ui.reader.panel

import dev.icerock.moko.resources.StringResource
import tachiyomi.i18n.MR

enum class PanelSortingAlgorithm(val titleRes: StringResource) {
    ROW_BASED(MR.strings.panel_sort_row_based),
    XY_CUT(MR.strings.panel_sort_xy_cut),
}
