package eu.kanade.tachiyomi.ui.reader.setting

import android.view.KeyEvent
import eu.kanade.tachiyomi.ui.reader.input.InputBinding
import eu.kanade.tachiyomi.ui.reader.input.ReaderAction
import eu.kanade.tachiyomi.ui.reader.input.ReaderInputDefaultOptions
import eu.kanade.tachiyomi.ui.reader.input.ReaderInputLayer
import eu.kanade.tachiyomi.ui.reader.input.ReaderInputLayerMappings
import eu.kanade.tachiyomi.ui.reader.input.ReaderInputMapping
import eu.kanade.tachiyomi.ui.reader.input.ReaderInputProfile
import eu.kanade.tachiyomi.ui.reader.input.ReaderInputTrigger
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class ReaderInputSettingsScreenModelTest {

    @Test
    fun `add global binding appends custom global mapping`() {
        val profile = ReaderInputProfile()

        val updated = ReaderInputSettingsScreenModel.addMappingToProfile(
            profile = profile,
            layer = null,
            mapping = ReaderInputMapping(
                id = "custom_key_103_next",
                binding = InputBinding.key(KeyEvent.KEYCODE_BUTTON_R1),
                action = ReaderAction.NEXT,
            ),
        )

        updated.customGlobal.size shouldBe 1
        updated.customGlobal.first().action shouldBe ReaderAction.NEXT
    }

    @Test
    fun `add override binding appends mapping for selected layer`() {
        val updated = ReaderInputSettingsScreenModel.addMappingToProfile(
            profile = ReaderInputProfile(),
            layer = ReaderInputLayer.WEBTOON,
            mapping = ReaderInputMapping(
                id = "custom_webtoon_key_103_fast_scroll_down",
                binding = InputBinding.key(KeyEvent.KEYCODE_BUTTON_R1),
                action = ReaderAction.FAST_SCROLL_DOWN,
            ),
        )

        updated.customOverrides.single().layer shouldBe ReaderInputLayer.WEBTOON
        updated.customOverrides.single().mappings.single().action shouldBe ReaderAction.FAST_SCROLL_DOWN
    }

    @Test
    fun `replace global binding removes conflicting mapping and keeps only new mapping`() {
        val binding = InputBinding.key(KeyEvent.KEYCODE_BUTTON_R1)
        val updated = ReaderInputSettingsScreenModel.replaceMappingInProfile(
            profile = ReaderInputProfile(
                customGlobal = listOf(
                    ReaderInputMapping(
                        id = "old",
                        binding = binding,
                        action = ReaderAction.NEXT,
                    ),
                ),
            ),
            layer = null,
            mapping = ReaderInputMapping(
                id = "new",
                binding = binding,
                action = ReaderAction.PREVIOUS,
            ),
        )

        updated.customGlobal shouldBe listOf(
            ReaderInputMapping(
                id = "new",
                binding = binding,
                action = ReaderAction.PREVIOUS,
            ),
        )
    }

    @Test
    fun `replace global binding preserves mapping with different trigger`() {
        val binding = InputBinding.key(KeyEvent.KEYCODE_BUTTON_R1)
        val holdMapping = ReaderInputMapping(
            id = "hold",
            binding = binding,
            trigger = ReaderInputTrigger.HOLD,
            action = ReaderAction.HOLD_SCROLL_DOWN,
        )
        val updated = ReaderInputSettingsScreenModel.replaceMappingInProfile(
            profile = ReaderInputProfile(
                customGlobal = listOf(
                    ReaderInputMapping(
                        id = "press-old",
                        binding = binding,
                        trigger = ReaderInputTrigger.PRESS,
                        action = ReaderAction.NEXT,
                    ),
                    holdMapping,
                ),
            ),
            layer = null,
            mapping = ReaderInputMapping(
                id = "press-new",
                binding = binding,
                trigger = ReaderInputTrigger.PRESS,
                action = ReaderAction.PREVIOUS,
            ),
        )

        updated.customGlobal shouldBe listOf(
            holdMapping,
            ReaderInputMapping(
                id = "press-new",
                binding = binding,
                trigger = ReaderInputTrigger.PRESS,
                action = ReaderAction.PREVIOUS,
            ),
        )
    }

    @Test
    fun `replace override binding only replaces selected layer mapping`() {
        val binding = InputBinding.key(KeyEvent.KEYCODE_BUTTON_R1)
        val pagedMapping = ReaderInputMapping(
            id = "paged",
            binding = binding,
            action = ReaderAction.NEXT,
        )

        val updated = ReaderInputSettingsScreenModel.replaceMappingInProfile(
            profile = ReaderInputProfile(
                customOverrides = listOf(
                    ReaderInputLayerMappings(
                        layer = ReaderInputLayer.WEBTOON,
                        mappings = listOf(
                            ReaderInputMapping(
                                id = "webtoon-old",
                                binding = binding,
                                action = ReaderAction.FAST_SCROLL_DOWN,
                            ),
                        ),
                    ),
                    ReaderInputLayerMappings(
                        layer = ReaderInputLayer.PAGED,
                        mappings = listOf(pagedMapping),
                    ),
                ),
            ),
            layer = ReaderInputLayer.WEBTOON,
            mapping = ReaderInputMapping(
                id = "webtoon-new",
                binding = binding,
                action = ReaderAction.FAST_SCROLL_UP,
            ),
        )

        updated.customOverrides shouldBe listOf(
            ReaderInputLayerMappings(
                layer = ReaderInputLayer.WEBTOON,
                mappings = listOf(
                    ReaderInputMapping(
                        id = "webtoon-new",
                        binding = binding,
                        action = ReaderAction.FAST_SCROLL_UP,
                    ),
                ),
            ),
            ReaderInputLayerMappings(
                layer = ReaderInputLayer.PAGED,
                mappings = listOf(pagedMapping),
            ),
        )
    }

    @Test
    fun `replace captured mapping disables conflicting default binding`() {
        val binding = InputBinding.key(KeyEvent.KEYCODE_BUTTON_R2)

        val updated = ReaderInputSettingsScreenModel.replaceCapturedMappingInProfile(
            profile = ReaderInputProfile(),
            defaultOptions = ReaderInputDefaultOptions(
                volumeKeysEnabled = false,
                volumeKeysInverted = false,
            ),
            layer = null,
            mapping = ReaderInputMapping(
                id = "custom_global_toggle_menu_key_105",
                binding = binding,
                action = ReaderAction.TOGGLE_MENU,
            ),
        )

        updated.disabledDefaultIds shouldBe setOf("default_key_r2_next_panel")
        updated.customGlobal shouldBe listOf(
            ReaderInputMapping(
                id = "custom_global_toggle_menu_key_105",
                binding = binding,
                action = ReaderAction.TOGGLE_MENU,
            ),
        )
    }

    @Test
    fun `replace captured mapping keeps default enabled when replacing visible custom mapping`() {
        val binding = InputBinding.key(KeyEvent.KEYCODE_BUTTON_R2)
        val updated = ReaderInputSettingsScreenModel.replaceCapturedMappingInProfile(
            profile = ReaderInputProfile(
                customGlobal = listOf(
                    ReaderInputMapping(
                        id = "old-custom",
                        binding = binding,
                        action = ReaderAction.TOGGLE_MENU,
                    ),
                ),
            ),
            defaultOptions = ReaderInputDefaultOptions(
                volumeKeysEnabled = false,
                volumeKeysInverted = false,
            ),
            layer = null,
            mapping = ReaderInputMapping(
                id = "new-custom",
                binding = binding,
                action = ReaderAction.PREVIOUS,
            ),
        )

        updated.disabledDefaultIds shouldBe emptySet()
        updated.customGlobal shouldBe listOf(
            ReaderInputMapping(
                id = "new-custom",
                binding = binding,
                action = ReaderAction.PREVIOUS,
            ),
        )
    }

    @Test
    fun `replace captured mapping is a no op when binding already matches visible custom action`() {
        val binding = InputBinding.key(KeyEvent.KEYCODE_BUTTON_R2)
        val profile = ReaderInputProfile(
            customGlobal = listOf(
                ReaderInputMapping(
                    id = "custom",
                    binding = binding,
                    action = ReaderAction.TOGGLE_MENU,
                ),
            ),
        )

        val updated = ReaderInputSettingsScreenModel.replaceCapturedMappingInProfile(
            profile = profile,
            defaultOptions = ReaderInputDefaultOptions(
                volumeKeysEnabled = false,
                volumeKeysInverted = false,
            ),
            layer = null,
            mapping = ReaderInputMapping(
                id = "new-custom",
                binding = binding,
                action = ReaderAction.TOGGLE_MENU,
            ),
        )

        updated shouldBe profile
    }

    @Test
    fun `replace captured mapping is a no op when binding already matches default action`() {
        val updated = ReaderInputSettingsScreenModel.replaceCapturedMappingInProfile(
            profile = ReaderInputProfile(),
            defaultOptions = ReaderInputDefaultOptions(
                volumeKeysEnabled = false,
                volumeKeysInverted = false,
            ),
            layer = null,
            mapping = ReaderInputMapping(
                id = "custom_global_next_panel_key_105",
                binding = InputBinding.key(KeyEvent.KEYCODE_BUTTON_R2),
                action = ReaderAction.NEXT_PANEL,
            ),
        )

        updated shouldBe ReaderInputProfile()
    }

    @Test
    fun `delete custom mapping removes only matching id`() {
        val kept = ReaderInputMapping(
            id = "keep",
            binding = InputBinding.key(KeyEvent.KEYCODE_BUTTON_L1),
            action = ReaderAction.PREVIOUS,
        )
        val removed = ReaderInputMapping(
            id = "remove",
            binding = InputBinding.key(KeyEvent.KEYCODE_BUTTON_R1),
            action = ReaderAction.NEXT,
        )

        val updated = ReaderInputSettingsScreenModel.deleteCustomMapping(
            profile = ReaderInputProfile(customGlobal = listOf(kept, removed)),
            layer = null,
            mappingId = "remove",
        )

        updated.customGlobal shouldBe listOf(kept)
    }

    @Test
    fun `delete layer mapping removes only matching override mapping`() {
        val kept = ReaderInputMapping(
            id = "keep",
            binding = InputBinding.key(KeyEvent.KEYCODE_BUTTON_L1),
            action = ReaderAction.FAST_SCROLL_UP,
        )
        val removed = ReaderInputMapping(
            id = "remove",
            binding = InputBinding.key(KeyEvent.KEYCODE_BUTTON_R1),
            action = ReaderAction.FAST_SCROLL_DOWN,
        )
        val otherLayer = ReaderInputLayerMappings(
            layer = ReaderInputLayer.PAGED,
            mappings = listOf(
                ReaderInputMapping(
                    id = "paged",
                    binding = InputBinding.key(KeyEvent.KEYCODE_DPAD_RIGHT),
                    action = ReaderAction.NEXT,
                ),
            ),
        )

        val updated = ReaderInputSettingsScreenModel.deleteCustomMapping(
            profile = ReaderInputProfile(
                customOverrides = listOf(
                    ReaderInputLayerMappings(
                        layer = ReaderInputLayer.WEBTOON,
                        mappings = listOf(kept, removed),
                    ),
                    otherLayer,
                ),
            ),
            layer = ReaderInputLayer.WEBTOON,
            mappingId = "remove",
        )

        updated.customOverrides shouldBe listOf(
            ReaderInputLayerMappings(
                layer = ReaderInputLayer.WEBTOON,
                mappings = listOf(kept),
            ),
            otherLayer,
        )
    }
}
