# Manual Order Correction & Global Layout Memory

You can now directly "teach" the app how to read complex manga pages. Instead of fighting with the geometric guessing, you can manually fix any page, and the app will remember that layout pattern globally.

## How to use Correction Mode

1.  **Enter Mode**: Open the reader menu and tap the **Pencil (Edit)** icon in the bottom bar.
2.  **Reorder Panels**:
    *   **Press and Hold** on a panel number (e.g., the "3").
    *   **Drag it** over the number you want to swap with (e.g., the "2").
    *   The target panel will highlight in **Yellow**. Release to swap the order.
3.  **Confirm Training**: A toast message **"Layout Memory: Correction saved!"** will appear.

## How the "Learning" Works

-   **Layout DNA**: When you save a fix, the app calculates a unique fingerprint based on the *shape* of the panels, not the manga title.
-   **Fuzzy Matching**: It uses a high-tolerance "Fuzzy Memory" that rounds coordinates to 10%. This means even if the AI detection is slightly jittery on a different page, it will recognize the pattern.
-   **Global Memory**: **Yes, it works across different manga.** If you train a "Sidebar on the Left" pattern in one title, the reader will use that same logic for a similar sidebar in any other title you read.
-   **Persistence**: Your training is saved permanently to your device and survives app restarts. It is only cleared if you uninstall the app.

## Diagnostic Verification
To see exactly what the AI is matching or missing in real-time, run:
`adb logcat -d | grep "AI TRAINING"`
