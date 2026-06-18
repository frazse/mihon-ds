package eu.kanade.tachiyomi.ui.reader.input

import android.view.KeyEvent
import android.view.MotionEvent
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class ReaderInputProfileResolverTest {

    private val options = ReaderInputDefaultOptions(
        volumeKeysEnabled = false,
        volumeKeysInverted = false,
    )

    @Test
    fun `global custom mapping resolves when mode has no override`() {
        val binding = InputBinding.key(KeyEvent.KEYCODE_BUTTON_R1)
        val profile = ReaderInputProfile(
            customGlobal = listOf(
                ReaderInputMapping(
                    id = "custom_r1_toggle_menu",
                    binding = binding,
                    action = ReaderAction.TOGGLE_MENU,
                ),
            ),
        )

        ReaderInputProfileResolver.resolve(
            profile = profile,
            defaultOptions = options,
            layer = ReaderInputLayer.WEBTOON,
            binding = binding,
        ) shouldBe ReaderAction.TOGGLE_MENU
    }

    @Test
    fun `mode override wins over global mapping`() {
        val binding = InputBinding.key(KeyEvent.KEYCODE_BUTTON_R1)
        val profile = ReaderInputProfile(
            customGlobal = listOf(
                ReaderInputMapping(
                    id = "custom_r1_next",
                    binding = binding,
                    action = ReaderAction.NEXT,
                ),
            ),
            customOverrides = listOf(
                ReaderInputLayerMappings(
                    layer = ReaderInputLayer.WEBTOON,
                    mappings = listOf(
                        ReaderInputMapping(
                            id = "webtoon_r1_fast_scroll_down",
                            binding = binding,
                            action = ReaderAction.FAST_SCROLL_DOWN,
                        ),
                    ),
                ),
            ),
        )

        ReaderInputProfileResolver.resolve(
            profile = profile,
            defaultOptions = options,
            layer = ReaderInputLayer.WEBTOON,
            binding = binding,
        ) shouldBe ReaderAction.FAST_SCROLL_DOWN
    }

    @Test
    fun `press and hold mappings can share the same binding`() {
        val binding = InputBinding.key(KeyEvent.KEYCODE_BUTTON_R1)
        val profile = ReaderInputProfile(
            customOverrides = listOf(
                ReaderInputLayerMappings(
                    layer = ReaderInputLayer.WEBTOON,
                    mappings = listOf(
                        ReaderInputMapping(
                            id = "webtoon_r1_scroll_down",
                            binding = binding,
                            trigger = ReaderInputTrigger.PRESS,
                            action = ReaderAction.SCROLL_DOWN,
                        ),
                        ReaderInputMapping(
                            id = "webtoon_r1_hold_scroll_down",
                            binding = binding,
                            trigger = ReaderInputTrigger.HOLD,
                            action = ReaderAction.HOLD_SCROLL_DOWN,
                        ),
                    ),
                ),
            ),
        )

        ReaderInputProfileResolver.resolve(
            profile = profile,
            defaultOptions = options,
            layer = ReaderInputLayer.WEBTOON,
            binding = binding,
            trigger = ReaderInputTrigger.PRESS,
        ) shouldBe ReaderAction.SCROLL_DOWN

        ReaderInputProfileResolver.resolve(
            profile = profile,
            defaultOptions = options,
            layer = ReaderInputLayer.WEBTOON,
            binding = binding,
            trigger = ReaderInputTrigger.HOLD,
        ) shouldBe ReaderAction.HOLD_SCROLL_DOWN
    }

    @Test
    fun `conflict detection is scoped to trigger type`() {
        val binding = InputBinding.key(KeyEvent.KEYCODE_BUTTON_R1)
        val mappings = listOf(
            ReaderInputMapping(
                id = "press",
                binding = binding,
                trigger = ReaderInputTrigger.PRESS,
                action = ReaderAction.SCROLL_DOWN,
            ),
            ReaderInputMapping(
                id = "hold",
                binding = binding,
                trigger = ReaderInputTrigger.HOLD,
                action = ReaderAction.HOLD_SCROLL_DOWN,
            ),
        )

        ReaderInputProfileResolver.hasConflict(
            mappings = mappings,
            binding = binding,
            trigger = ReaderInputTrigger.HOLD,
            action = ReaderAction.HOLD_SCROLL_UP,
        )?.id shouldBe "hold"

        ReaderInputProfileResolver.hasConflict(
            mappings = mappings,
            binding = binding,
            trigger = ReaderInputTrigger.HOLD,
            action = ReaderAction.HOLD_SCROLL_DOWN,
        ) shouldBe null
    }

    @Test
    fun `default R2 mapping changes by reader layer`() {
        val binding = InputBinding.key(KeyEvent.KEYCODE_BUTTON_R2)

        ReaderInputProfileResolver.resolve(
            profile = ReaderInputProfile(),
            defaultOptions = options,
            layer = ReaderInputLayer.PAGED,
            binding = binding,
        ) shouldBe ReaderAction.NEXT_PANEL

        ReaderInputProfileResolver.resolve(
            profile = ReaderInputProfile(),
            defaultOptions = options,
            layer = ReaderInputLayer.WEBTOON,
            binding = binding,
            trigger = ReaderInputTrigger.PRESS,
        ) shouldBe ReaderAction.SCROLL_DOWN

        ReaderInputProfileResolver.resolve(
            profile = ReaderInputProfile(),
            defaultOptions = options,
            layer = ReaderInputLayer.WEBTOON,
            binding = binding,
            trigger = ReaderInputTrigger.HOLD,
        ) shouldBe ReaderAction.HOLD_SCROLL_DOWN
    }

    @Test
    fun `default L2 mapping changes by reader layer`() {
        val binding = InputBinding.key(KeyEvent.KEYCODE_BUTTON_L2)

        ReaderInputProfileResolver.resolve(
            profile = ReaderInputProfile(),
            defaultOptions = options,
            layer = ReaderInputLayer.PAGED,
            binding = binding,
        ) shouldBe ReaderAction.PREVIOUS_PANEL

        ReaderInputProfileResolver.resolve(
            profile = ReaderInputProfile(),
            defaultOptions = options,
            layer = ReaderInputLayer.WEBTOON,
            binding = binding,
            trigger = ReaderInputTrigger.PRESS,
        ) shouldBe ReaderAction.SCROLL_UP

        ReaderInputProfileResolver.resolve(
            profile = ReaderInputProfile(),
            defaultOptions = options,
            layer = ReaderInputLayer.WEBTOON,
            binding = binding,
            trigger = ReaderInputTrigger.HOLD,
        ) shouldBe ReaderAction.HOLD_SCROLL_UP
    }

    @Test
    fun `ctrl dpad right is not a visible default mapping`() {
        val binding = InputBinding.key(KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.META_CTRL_ON)

        ReaderInputProfileResolver.resolve(
            profile = ReaderInputProfile(),
            defaultOptions = options,
            layer = ReaderInputLayer.PAGED,
            binding = binding,
        ) shouldBe null

        ReaderInputProfileResolver.resolve(
            profile = ReaderInputProfile(),
            defaultOptions = options,
            layer = ReaderInputLayer.WEBTOON,
            binding = binding,
        ) shouldBe null
    }

    @Test
    fun `trigger axis has no default mapping but custom mapping still resolves`() {
        val runtimeBinding = InputBinding.axis(MotionEvent.AXIS_RTRIGGER, AxisDirection.POSITIVE)
        val customBinding = InputBinding.axis(
            axis = MotionEvent.AXIS_RTRIGGER,
            direction = AxisDirection.POSITIVE,
            threshold = 0.9f,
        )

        ReaderInputProfileResolver.resolve(
            profile = ReaderInputProfile(),
            defaultOptions = options,
            layer = ReaderInputLayer.PAGED,
            binding = runtimeBinding,
        ) shouldBe null

        ReaderInputProfileResolver.resolve(
            profile = ReaderInputProfile(
                customGlobal = listOf(
                    ReaderInputMapping(
                        id = "custom_axis_rtrigger_next_panel",
                        binding = customBinding,
                        action = ReaderAction.NEXT_PANEL,
                    ),
                ),
            ),
            defaultOptions = options,
            layer = ReaderInputLayer.PAGED,
            binding = runtimeBinding,
        ) shouldBe ReaderAction.NEXT_PANEL
    }

    @Test
    fun `key binding still requires exact meta state match`() {
        val profile = ReaderInputProfile(
            customGlobal = listOf(
                ReaderInputMapping(
                    id = "custom_ctrl_right_next_page",
                    binding = InputBinding.key(KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.META_CTRL_ON),
                    action = ReaderAction.NEXT_PAGE,
                ),
            ),
        )

        ReaderInputProfileResolver.resolve(
            profile = profile,
            defaultOptions = options,
            layer = ReaderInputLayer.PAGED,
            binding = InputBinding.key(KeyEvent.KEYCODE_DPAD_RIGHT),
        ) shouldBe ReaderAction.NEXT

        ReaderInputProfileResolver.resolve(
            profile = profile,
            defaultOptions = options,
            layer = ReaderInputLayer.PAGED,
            binding = InputBinding.key(KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.META_CTRL_ON),
        ) shouldBe ReaderAction.NEXT_PAGE
    }

    @Test
    fun `paged semantic controls override global physical next and previous mappings`() {
        val pagedLayer = ReaderInputLayer.PAGED

        ReaderInputProfileResolver.resolve(
            profile = ReaderInputProfile(),
            defaultOptions = options,
            layer = pagedLayer,
            binding = InputBinding.key(KeyEvent.KEYCODE_DPAD_RIGHT),
        ) shouldBe ReaderAction.NEXT

        listOf(
            KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_PAGE_DOWN,
            KeyEvent.KEYCODE_BUTTON_R1,
        ).forEach { keyCode ->
            ReaderInputProfileResolver.resolve(
                profile = ReaderInputProfile(),
                defaultOptions = options,
                layer = pagedLayer,
                binding = InputBinding.key(keyCode),
            ) shouldBe ReaderAction.NEXT_PAGE
        }

        listOf(
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_PAGE_UP,
            KeyEvent.KEYCODE_BUTTON_L1,
        ).forEach { keyCode ->
            ReaderInputProfileResolver.resolve(
                profile = ReaderInputProfile(),
                defaultOptions = options,
                layer = pagedLayer,
                binding = InputBinding.key(keyCode),
            ) shouldBe ReaderAction.PREVIOUS_PAGE
        }
    }

    @Test
    fun `paged volume defaults use semantic next and previous page actions`() {
        val pagedLayer = ReaderInputLayer.PAGED

        ReaderInputProfileResolver.resolve(
            profile = ReaderInputProfile(),
            defaultOptions = options.copy(volumeKeysEnabled = true),
            layer = pagedLayer,
            binding = InputBinding.key(KeyEvent.KEYCODE_VOLUME_DOWN),
        ) shouldBe ReaderAction.NEXT_PAGE

        ReaderInputProfileResolver.resolve(
            profile = ReaderInputProfile(),
            defaultOptions = options.copy(volumeKeysEnabled = true),
            layer = pagedLayer,
            binding = InputBinding.key(KeyEvent.KEYCODE_VOLUME_UP),
        ) shouldBe ReaderAction.PREVIOUS_PAGE

        ReaderInputProfileResolver.resolve(
            profile = ReaderInputProfile(),
            defaultOptions = options.copy(volumeKeysEnabled = true, volumeKeysInverted = true),
            layer = pagedLayer,
            binding = InputBinding.key(KeyEvent.KEYCODE_VOLUME_DOWN),
        ) shouldBe ReaderAction.PREVIOUS_PAGE

        ReaderInputProfileResolver.resolve(
            profile = ReaderInputProfile(),
            defaultOptions = options.copy(volumeKeysEnabled = true, volumeKeysInverted = true),
            layer = pagedLayer,
            binding = InputBinding.key(KeyEvent.KEYCODE_VOLUME_UP),
        ) shouldBe ReaderAction.NEXT_PAGE
    }

    @Test
    fun `axis conflict ignores threshold while key conflict remains exact`() {
        val axisConflict = ReaderInputProfileResolver.hasConflict(
            mappings = listOf(
                ReaderInputMapping(
                    id = "custom_axis_rtrigger_next_panel",
                    binding = InputBinding.axis(
                        axis = MotionEvent.AXIS_RTRIGGER,
                        direction = AxisDirection.POSITIVE,
                        threshold = 0.9f,
                    ),
                    action = ReaderAction.NEXT_PANEL,
                ),
            ),
            binding = InputBinding.axis(MotionEvent.AXIS_RTRIGGER, AxisDirection.POSITIVE),
            trigger = ReaderInputTrigger.PRESS,
            action = ReaderAction.PREVIOUS_PANEL,
        )

        axisConflict?.id shouldBe "custom_axis_rtrigger_next_panel"

        ReaderInputProfileResolver.hasConflict(
            mappings = listOf(
                ReaderInputMapping(
                    id = "custom_ctrl_right_next_page",
                    binding = InputBinding.key(KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.META_CTRL_ON),
                    action = ReaderAction.NEXT_PAGE,
                ),
            ),
            binding = InputBinding.key(KeyEvent.KEYCODE_DPAD_RIGHT),
            trigger = ReaderInputTrigger.PRESS,
            action = ReaderAction.PREVIOUS_PAGE,
        ) shouldBe null
    }

    @Test
    fun `disabled default does not resolve`() {
        val binding = InputBinding.key(KeyEvent.KEYCODE_DPAD_RIGHT)
        val profile = ReaderInputProfile(disabledDefaultIds = setOf("default_key_dpad_right_next"))

        ReaderInputProfileResolver.resolve(
            profile = profile,
            defaultOptions = options,
            layer = ReaderInputLayer.PAGED,
            binding = binding,
        ) shouldBe null
    }

    @Test
    fun `volume defaults follow existing volume key preference`() {
        val binding = InputBinding.key(KeyEvent.KEYCODE_VOLUME_DOWN)

        ReaderInputProfileResolver.resolve(
            profile = ReaderInputProfile(),
            defaultOptions = options.copy(volumeKeysEnabled = false),
            layer = ReaderInputLayer.WEBTOON,
            binding = binding,
        ) shouldBe null

        ReaderInputProfileResolver.resolve(
            profile = ReaderInputProfile(),
            defaultOptions = options.copy(volumeKeysEnabled = true),
            layer = ReaderInputLayer.WEBTOON,
            binding = binding,
        ) shouldBe ReaderAction.NEXT

        ReaderInputProfileResolver.resolve(
            profile = ReaderInputProfile(),
            defaultOptions = options.copy(volumeKeysEnabled = true, volumeKeysInverted = true),
            layer = ReaderInputLayer.WEBTOON,
            binding = binding,
        ) shouldBe ReaderAction.PREVIOUS
    }
}
