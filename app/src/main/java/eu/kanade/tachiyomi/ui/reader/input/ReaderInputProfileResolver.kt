package eu.kanade.tachiyomi.ui.reader.input

object ReaderInputProfileResolver {

    fun resolveResult(
        profile: ReaderInputProfile,
        defaultOptions: ReaderInputDefaultOptions,
        layer: ReaderInputLayer?,
        binding: InputBinding,
        trigger: ReaderInputTrigger = ReaderInputTrigger.PRESS,
    ): ReaderInputRuntimeResolution {
        val overrideMappings = layer
            ?.let { effectiveMappingsForLayer(profile, defaultOptions, it) }
            .orEmpty()
        val overrideMatch = overrideMappings.lastOrNull { bindingsMatch(it.binding, binding) && it.trigger == trigger }
        if (overrideMatch != null) {
            return ReaderInputRuntimeResolution(
                action = overrideMatch.action,
                isOwnedBinding = true,
            )
        }

        val globalMappings = effectiveGlobalMappings(profile, defaultOptions)
        val globalMatch = globalMappings.lastOrNull { bindingsMatch(it.binding, binding) && it.trigger == trigger }
        if (globalMatch != null) {
            return ReaderInputRuntimeResolution(
                action = globalMatch.action,
                isOwnedBinding = true,
            )
        }

        val isDisabledDefaultMatch = disabledDefaultMappings(profile, defaultOptions, layer)
            .any { bindingsMatch(it.binding, binding) && it.trigger == trigger }
        val isOtherTriggerMatch = overrideMappings.any { bindingsMatch(it.binding, binding) } ||
            globalMappings.any { bindingsMatch(it.binding, binding) }

        return ReaderInputRuntimeResolution(
            action = null,
            isOwnedBinding = isDisabledDefaultMatch || isOtherTriggerMatch,
        )
    }

    fun resolve(
        profile: ReaderInputProfile,
        defaultOptions: ReaderInputDefaultOptions,
        layer: ReaderInputLayer?,
        binding: InputBinding,
        trigger: ReaderInputTrigger = ReaderInputTrigger.PRESS,
    ): ReaderAction? {
        return resolveResult(profile, defaultOptions, layer, binding, trigger).action
    }

    fun effectiveGlobalMappings(
        profile: ReaderInputProfile,
        defaultOptions: ReaderInputDefaultOptions,
    ): List<ReaderInputMapping> {
        val defaults = ReaderInputDefaults.global(defaultOptions)
            .filterNot { it.id in profile.disabledDefaultIds }
        return defaults + profile.customGlobal
    }

    fun effectiveMappingsForLayer(
        profile: ReaderInputProfile,
        defaultOptions: ReaderInputDefaultOptions,
        layer: ReaderInputLayer,
    ): List<ReaderInputMapping> {
        val defaultOverrides = ReaderInputDefaults.overrides(defaultOptions)
            .firstOrNull { it.layer == layer }
            ?.mappings
            .orEmpty()
            .filterNot { it.id in profile.disabledDefaultIds }
        val customOverrides = profile.customOverrides
            .firstOrNull { it.layer == layer }
            ?.mappings
            .orEmpty()
        return defaultOverrides + customOverrides
    }

    fun hasConflict(
        mappings: List<ReaderInputMapping>,
        binding: InputBinding,
        trigger: ReaderInputTrigger,
        action: ReaderAction,
    ): ReaderInputMapping? {
        return mappings.firstOrNull {
            bindingsMatch(it.binding, binding) &&
                it.trigger == trigger &&
                it.action != action
        }
    }

    private fun disabledDefaultMappings(
        profile: ReaderInputProfile,
        defaultOptions: ReaderInputDefaultOptions,
        layer: ReaderInputLayer?,
    ): List<ReaderInputMapping> {
        val disabledGlobal = ReaderInputDefaults.global(defaultOptions)
            .filter { it.id in profile.disabledDefaultIds }
        val disabledLayerMappings = layer
            ?.let {
                ReaderInputDefaults.overrides(defaultOptions)
                    .firstOrNull { defaults -> defaults.layer == it }
                    ?.mappings
                    .orEmpty()
                    .filter { mapping -> mapping.id in profile.disabledDefaultIds }
            }
            .orEmpty()
        return disabledLayerMappings + disabledGlobal
    }

    fun bindingsMatch(
        expected: InputBinding,
        actual: InputBinding,
    ): Boolean {
        return when {
            expected.type != actual.type -> false
            expected.type == InputBindingType.KEY -> {
                expected.keyCode == actual.keyCode &&
                    expected.metaState == actual.metaState
            }
            else -> {
                expected.axis == actual.axis &&
                    expected.direction == actual.direction
            }
        }
    }
}
