package eu.kanade.presentation.reader.settings

import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import eu.kanade.tachiyomi.ui.reader.input.ReaderInputCaptureState
import eu.kanade.tachiyomi.ui.reader.input.ReaderInputMapping
import eu.kanade.tachiyomi.ui.reader.input.ReaderInputTrigger
import eu.kanade.tachiyomi.ui.reader.setting.ReaderInputSettingsScreenModel
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun ReaderInputCaptureDialog(
    captureState: ReaderInputCaptureState,
    conflict: ReaderInputMapping?,
    screenModel: ReaderInputSettingsScreenModel,
) {
    val capturedBinding = captureState.capturedBinding
    val capturedAction = captureState.capturedAction
    val isConflict = conflict != null && capturedBinding != null && capturedAction != null

    DisposableEffect(screenModel) {
        val registration = screenModel.registerInputCapture()
        onDispose { registration.close() }
    }

    AlertDialog(
        onDismissRequest = screenModel::cancelInputCapture,
        title = {
            Text(
                stringResource(
                    if (isConflict) {
                        MR.strings.reader_input_conflict_title
                    } else {
                        MR.strings.reader_input_capture_title
                    },
                ),
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ReaderInputCaptureEventTarget(screenModel)
                Text(
                    text = if (isConflict) {
                        stringResource(
                            MR.strings.reader_input_conflict_body,
                            capturedBinding.readerInputLabel(),
                            conflict.action.readerInputLabel(),
                            capturedAction.readerInputLabel(),
                        )
                    } else {
                        stringResource(
                            if (captureState.request?.trigger == ReaderInputTrigger.HOLD) {
                                MR.strings.reader_input_capture_body_buttons_only
                            } else {
                                MR.strings.reader_input_capture_body
                            },
                        )
                    },
                )
                if (capturedBinding != null) {
                    Text(
                        text = capturedBinding.readerInputLabel(),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    capturedAction?.let { action ->
                        Text(
                            text = action.readerInputLabel(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = capturedBinding != null,
                onClick = {
                    if (capturedBinding != null) {
                        if (isConflict) {
                            screenModel.replaceCapturedInput()
                        } else {
                            screenModel.consumeCapturedInput()
                        }
                    }
                },
            ) {
                Text(
                    stringResource(
                        if (isConflict) {
                            MR.strings.reader_input_replace
                        } else {
                            MR.strings.reader_input_capture_save
                        },
                    ),
                )
            }
        },
        dismissButton = {
            TextButton(onClick = screenModel::cancelInputCapture) {
                Text(stringResource(MR.strings.reader_input_capture_cancel))
            }
        },
    )
}

@Composable
private fun ReaderInputCaptureEventTarget(screenModel: ReaderInputSettingsScreenModel) {
    AndroidView(
        modifier = Modifier.size(1.dp),
        factory = { context ->
            object : View(context) {
                init {
                    isFocusable = true
                    isFocusableInTouchMode = true
                }

                override fun dispatchKeyEvent(event: KeyEvent): Boolean {
                    return screenModel.captureInputKeyEvent(event) || super.dispatchKeyEvent(event)
                }

                override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
                    return screenModel.captureInputMotionEvent(event) || super.dispatchGenericMotionEvent(event)
                }
            }
        },
        update = { it.requestFocus() },
    )
}
