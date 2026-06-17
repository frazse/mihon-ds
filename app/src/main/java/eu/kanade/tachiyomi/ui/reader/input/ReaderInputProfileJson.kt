package eu.kanade.tachiyomi.ui.reader.input

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object ReaderInputProfileJson {

    fun decodeOrDefault(
        json: Json,
        raw: String,
    ): ReaderInputProfile {
        return runCatching { json.decodeFromString<ReaderInputProfile>(raw) }
            .getOrElse { decodeTolerantOrDefault(json, raw) }
    }

    private fun decodeTolerantOrDefault(
        json: Json,
        raw: String,
    ): ReaderInputProfile {
        val root = runCatching { json.parseToJsonElement(raw).jsonObject }
            .getOrElse { return ReaderInputProfile() }

        return ReaderInputProfile(
            version = root.int("version") ?: 1,
            disabledDefaultIds = root.array("disabledDefaultIds")
                ?.mapNotNull { it.jsonPrimitive.contentOrNull }
                ?.toSet()
                .orEmpty(),
            customGlobal = root.array("customGlobal")
                ?.mapNotNull { parseMapping(it) }
                .orEmpty(),
            customOverrides = root.array("customOverrides")
                ?.mapNotNull { parseLayerMappings(it) }
                .orEmpty(),
        )
    }

    private fun parseLayerMappings(element: JsonElement): ReaderInputLayerMappings? {
        val obj = element as? JsonObject ?: return null
        val layer = obj.enum<ReaderInputLayer>("layer") ?: return null
        val mappings = obj.array("mappings")
            ?.mapNotNull { parseMapping(it) }
            .orEmpty()
        return ReaderInputLayerMappings(
            layer = layer,
            mappings = mappings,
        )
    }

    private fun parseMapping(element: JsonElement): ReaderInputMapping? {
        val obj = element as? JsonObject ?: return null
        val id = obj.string("id") ?: return null
        val binding = obj.objAt("binding")?.let { parseBinding(it) } ?: return null
        val action = obj.enum<ReaderAction>("action") ?: return null
        return ReaderInputMapping(
            id = id,
            binding = binding,
            trigger = obj.enum<ReaderInputTrigger>("trigger") ?: ReaderInputTrigger.PRESS,
            action = action,
        )
    }

    private fun parseBinding(obj: JsonObject): InputBinding? {
        return when (obj.enum<InputBindingType>("type")) {
            InputBindingType.KEY -> {
                val keyCode = obj.int("keyCode") ?: return null
                InputBinding.key(
                    keyCode = keyCode,
                    metaState = obj.int("metaState") ?: 0,
                )
            }
            InputBindingType.AXIS -> {
                val axis = obj.int("axis") ?: return null
                val direction = when (val directionValue = obj.string("direction")) {
                    null -> AxisDirection.POSITIVE
                    else -> enumValueOrNull<AxisDirection>(directionValue) ?: return null
                }
                InputBinding.axis(
                    axis = axis,
                    direction = direction,
                    threshold = obj.float("threshold") ?: InputBinding.DEFAULT_AXIS_THRESHOLD,
                )
            }
            null -> null
        }
    }

    private fun JsonObject.objAt(key: String): JsonObject? = this[key] as? JsonObject

    private fun JsonObject.array(key: String): JsonArray? = this[key] as? JsonArray

    private fun JsonObject.string(key: String): String? = this[key]?.jsonPrimitive?.contentOrNull

    private fun JsonObject.int(key: String): Int? = this[key]?.jsonPrimitive?.intOrNull

    private fun JsonObject.float(key: String): Float? = this[key]?.jsonPrimitive?.floatOrNull

    private inline fun <reified T : Enum<T>> JsonObject.enum(key: String): T? {
        val value = string(key) ?: return null
        return enumValueOrNull(value)
    }

    private inline fun <reified T : Enum<T>> enumValueOrNull(value: String): T? {
        return enumValues<T>().firstOrNull { it.name == value }
    }
}
