package eu.kanade.tachiyomi.ui.reader.input

import android.view.KeyEvent
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test

class ReaderInputProfileJsonTest {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Test
    fun `tolerant decode preserves valid mapping and drops unknown action`() {
        val payload = """
            {
              "version": 1,
              "disabledDefaultIds": ["default_key_r1_next"],
              "customGlobal": [
                {
                  "id": "valid_r1_next",
                  "binding": {
                    "type": "KEY",
                    "keyCode": ${KeyEvent.KEYCODE_BUTTON_R1}
                  },
                  "action": "NEXT"
                },
                {
                  "id": "future_action",
                  "binding": {
                    "type": "KEY",
                    "keyCode": ${KeyEvent.KEYCODE_BUTTON_L1}
                  },
                  "action": "SOME_FUTURE_ACTION"
                }
              ]
            }
        """.trimIndent()

        val profile = ReaderInputProfileJson.decodeOrDefault(json, payload)

        profile.disabledDefaultIds.shouldContainExactly("default_key_r1_next")
        profile.customGlobal shouldContainExactly listOf(
            ReaderInputMapping(
                id = "valid_r1_next",
                binding = InputBinding.key(KeyEvent.KEYCODE_BUTTON_R1),
                trigger = ReaderInputTrigger.PRESS,
                action = ReaderAction.NEXT,
            ),
        )
    }

    @Test
    fun `malformed json falls back to default profile`() {
        ReaderInputProfileJson.decodeOrDefault(json, "{not valid json") shouldBe ReaderInputProfile()
    }

    @Test
    fun `tolerant decode preserves valid axis mapping and drops unknown direction`() {
        val payload = """
            {
              "version": 1,
              "customGlobal": [
                {
                  "id": "valid_axis",
                  "binding": {
                    "type": "AXIS",
                    "axis": 17,
                    "direction": "POSITIVE",
                    "threshold": 0.7
                  },
                  "trigger": "HOLD",
                  "action": "SCROLL_DOWN"
                },
                {
                  "id": "future_axis_direction",
                  "binding": {
                    "type": "AXIS",
                    "axis": 18,
                    "direction": "DIAGONAL",
                    "threshold": 0.4
                  },
                  "action": "SCROLL_UP"
                }
              ]
            }
        """.trimIndent()

        val profile = ReaderInputProfileJson.decodeOrDefault(json, payload)

        profile.customGlobal shouldContainExactly listOf(
            ReaderInputMapping(
                id = "valid_axis",
                binding = InputBinding.axis(
                    axis = 17,
                    direction = AxisDirection.POSITIVE,
                    threshold = 0.7f,
                ),
                trigger = ReaderInputTrigger.HOLD,
                action = ReaderAction.SCROLL_DOWN,
            ),
        )
    }
}
