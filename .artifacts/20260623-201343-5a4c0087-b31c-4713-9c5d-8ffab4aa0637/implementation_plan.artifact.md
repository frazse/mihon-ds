# On-Device Training via Manual Panel Reordering

Implement a "Reinforcement-by-Correction" system that allows users to manually re-order panels when the AI makes a mistake. These corrections are persisted and used to "train" the reader's intuition for similar layouts in the future.

## User Review Required

> [!IMPORTANT]
> **Training vs. Personalization**: True backpropagation (weight adjustment) on a YOLO model is extremely battery-heavy on mobile. This plan uses a "Correction Map" (Personalization) which effectively "teaches" the app your preference for specific layout geometries.

## Proposed Changes

### Core Logic
Implement the storage and retrieval of manual overrides.

#### [NEW] [PanelCorrectionStore.kt](file:///home/azu/git/mihon-ds/app/src/main/java/eu/kanade/tachiyomi/ui/reader/panel/PanelCorrectionStore.kt)
- Key-Value store using the page's "Visual Hash" (panel coordinates) to store the corrected order.
- Provides a way for the AI to "Check History" before guessing.

#### [PanelReadingController.kt](file:///home/azu/git/mihon-ds/app/src/main/java/eu/kanade/tachiyomi/ui/reader/panel/PanelReadingController.kt)
- Integrate with `PanelCorrectionStore`.
- Update `reSortCurrentPage()` to handle manual re-ordering events from the UI.

---

### UI Components
Enable the user to actually perform the corrections.

#### [PanelHighlightOverlay.kt](file:///home/azu/git/mihon-ds/app/src/main/java/eu/kanade/tachiyomi/ui/reader/viewer/PanelHighlightOverlay.kt)
- Add "Correction Mode" state.
- Implement Drag-and-Drop for panel numbers.
- When a user drags "3" onto "2", it triggers a swap and notifies the Controller.

#### [ReaderActivity.kt](file:///home/azu/git/mihon-ds/app/src/main/java/eu/kanade/tachiyomi/ui/reader/ReaderActivity.kt)
- Add a "Correct Panel Order" button to the reader menu.
- Manage the transition between "Reading" and "Correction" modes.

## Verification Plan

### Manual Verification
1.  Open the volleyball page (incorrect order).
2.  Enter "Correction Mode" via the menu.
3.  Drag the number "3" to the position of "2" (Correcting the Right-Column-First order).
4.  Exit/Restart the chapter—Verify the app **remembers** the correction.
5.  Open a similar layout in a **different manga**—Verify the "Intuition" improvement (if applicable to the geometric similarity).
