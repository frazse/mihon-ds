package eu.kanade.tachiyomi.ui.reader.input

import android.view.KeyEvent
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class ReaderInputRuntimeResolverTest {

    @Test
    fun `runtime maps key binding through current layer`() {
        val runtime = ReaderInputRuntimeResolver(
            profileProvider = {
                ReaderInputProfile(
                    customOverrides = listOf(
                        ReaderInputLayerMappings(
                            layer = ReaderInputLayer.WEBTOON,
                            mappings = listOf(
                                ReaderInputMapping(
                                    id = "webtoon_r1_fast_scroll_down",
                                    binding = InputBinding.key(KeyEvent.KEYCODE_BUTTON_R1),
                                    action = ReaderAction.FAST_SCROLL_DOWN,
                                ),
                            ),
                        ),
                    ),
                )
            },
            defaultOptionsProvider = {
                ReaderInputDefaultOptions(
                    volumeKeysEnabled = false,
                    volumeKeysInverted = false,
                )
            },
            layerProvider = { ReaderInputLayer.WEBTOON },
        )

        runtime.resolve(InputBinding.key(KeyEvent.KEYCODE_BUTTON_R1)) shouldBe ReaderAction.FAST_SCROLL_DOWN
    }

    @Test
    fun `disabled default key remains owned even when action is filtered out`() {
        val runtime = ReaderInputRuntimeResolver(
            profileProvider = {
                ReaderInputProfile(
                    disabledDefaultIds = setOf("default_key_n_next_chapter"),
                )
            },
            defaultOptionsProvider = {
                ReaderInputDefaultOptions(
                    volumeKeysEnabled = false,
                    volumeKeysInverted = false,
                )
            },
            layerProvider = { ReaderInputLayer.PAGED },
        )

        runtime.resolveResult(InputBinding.key(KeyEvent.KEYCODE_N)) shouldBe ReaderInputRuntimeResolution(
            action = null,
            isOwnedBinding = true,
        )
    }

    @Test
    fun `menu visible skips volume-key runtime resolution only`() {
        ReaderInputRuntimeDispatchPolicy.shouldResolve(
            binding = InputBinding.key(KeyEvent.KEYCODE_VOLUME_DOWN),
            menuVisible = true,
        ) shouldBe false
        ReaderInputRuntimeDispatchPolicy.shouldResolve(
            binding = InputBinding.key(KeyEvent.KEYCODE_N),
            menuVisible = true,
        ) shouldBe true
        ReaderInputRuntimeDispatchPolicy.shouldResolve(
            binding = InputBinding.key(KeyEvent.KEYCODE_VOLUME_DOWN),
            menuVisible = false,
        ) shouldBe true
    }
}
