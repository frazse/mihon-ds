package eu.kanade.tachiyomi.ui.reader.input

data class ReaderInputRuntimeResolution(
    val action: ReaderAction?,
    val isOwnedBinding: Boolean,
)

object ReaderInputRuntimeDispatchPolicy {

    fun shouldResolve(binding: InputBinding, menuVisible: Boolean): Boolean {
        return !(menuVisible && binding.isVolumeKeyBinding())
    }

    private fun InputBinding.isVolumeKeyBinding(): Boolean {
        return type == InputBindingType.KEY &&
            (keyCode == android.view.KeyEvent.KEYCODE_VOLUME_UP || keyCode == android.view.KeyEvent.KEYCODE_VOLUME_DOWN)
    }
}

class ReaderInputRuntimeResolver(
    private val profileProvider: () -> ReaderInputProfile,
    private val defaultOptionsProvider: () -> ReaderInputDefaultOptions,
    private val layerProvider: () -> ReaderInputLayer?,
) {
    fun resolveResult(
        binding: InputBinding,
        trigger: ReaderInputTrigger = ReaderInputTrigger.PRESS,
    ): ReaderInputRuntimeResolution {
        return ReaderInputProfileResolver.resolveResult(
            profile = profileProvider(),
            defaultOptions = defaultOptionsProvider(),
            layer = layerProvider(),
            binding = binding,
            trigger = trigger,
        )
    }

    fun resolve(
        binding: InputBinding,
        trigger: ReaderInputTrigger = ReaderInputTrigger.PRESS,
    ): ReaderAction? {
        return resolveResult(binding, trigger).action
    }
}
