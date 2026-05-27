package eu.kanade.tachiyomi.ui.reader.panel

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readBytes

class PanelDetectorModelContractTest {

    @Test
    fun `bundled detector model matches runtime preprocessing and postprocessing contract`() {
        val contract = TfliteModelContract.read(resolveModelPath())

        assertEquals(
            TensorContract(
                name = "images",
                shape = listOf(1, LiteRtPanelDetector.MODEL_INPUT_SIZE, LiteRtPanelDetector.MODEL_INPUT_SIZE, 3),
                type = TensorType.FLOAT32,
            ),
            contract.input,
        )
        assertEquals(
            TensorContract(
                name = "Identity",
                shape = listOf(1, 300, 6),
                type = TensorType.FLOAT32,
            ),
            contract.output,
        )
    }

    private fun resolveModelPath(): Path {
        val candidates = listOf(
            Path.of("src/main/assets").resolve(LiteRtPanelDetector.MODEL_ASSET_PATH),
            Path.of("app/src/main/assets").resolve(LiteRtPanelDetector.MODEL_ASSET_PATH),
        )
        return candidates.firstOrNull { it.exists() }
            ?: error("Unable to find ${LiteRtPanelDetector.MODEL_ASSET_PATH} in ${candidates.joinToString()}")
    }
}

private data class ModelContract(
    val input: TensorContract,
    val output: TensorContract,
)

private data class TensorContract(
    val name: String,
    val shape: List<Int>,
    val type: TensorType,
)

private enum class TensorType {
    FLOAT32,
    FLOAT16,
    INT32,
    UINT8,
    INT64,
    STRING,
    BOOL,
    INT16,
    COMPLEX64,
    INT8,
    FLOAT64,
    COMPLEX128,
    UINT64,
    RESOURCE,
    VARIANT,
    UINT32,
    UINT16,
    INT4,
    BFLOAT16,
}

private object TfliteModelContract {
    fun read(path: Path): ModelContract {
        val buffer = ByteBuffer.wrap(path.readBytes()).order(ByteOrder.LITTLE_ENDIAN)
        check(buffer.ascii(4, 8) == "TFL3") { "Not a TFLite flatbuffer: ${path.fileName}" }

        val model = buffer.int(0)
        val subgraphs = buffer.vectorRef(model, MODEL_SUBGRAPHS_FIELD)
        check(buffer.vectorLength(subgraphs) > 0) { "TFLite model has no subgraphs: ${path.fileName}" }

        val subgraph = buffer.tableVectorElement(subgraphs, 0)
        val tensors = buffer.vectorRef(subgraph, SUBGRAPH_TENSORS_FIELD)
        val inputs = buffer.intVector(buffer.vectorRef(subgraph, SUBGRAPH_INPUTS_FIELD))
        val outputs = buffer.intVector(buffer.vectorRef(subgraph, SUBGRAPH_OUTPUTS_FIELD))

        return ModelContract(
            input = buffer.tensorContract(tensors, inputs.single()),
            output = buffer.tensorContract(tensors, outputs.single()),
        )
    }

    private fun ByteBuffer.tensorContract(tensors: Int, index: Int): TensorContract {
        val tensor = tableVectorElement(tensors, index)
        val typeId = byte(field(tensor, TENSOR_TYPE_FIELD)).toInt()
        return TensorContract(
            name = stringRef(tensor, TENSOR_NAME_FIELD),
            shape = intVector(vectorRef(tensor, TENSOR_SHAPE_FIELD)),
            type = TensorType.entries[typeId],
        )
    }

    private fun ByteBuffer.field(table: Int, fieldIndex: Int): Int {
        val vtable = table - int(table)
        val entry = 4 + fieldIndex * 2
        if (entry >= ushort(vtable)) return 0

        val offset = ushort(vtable + entry)
        return if (offset == 0) 0 else table + offset
    }

    private fun ByteBuffer.vectorRef(table: Int, fieldIndex: Int): Int {
        val position = field(table, fieldIndex)
        return if (position == 0) 0 else position + int(position)
    }

    private fun ByteBuffer.stringRef(table: Int, fieldIndex: Int): String {
        val position = field(table, fieldIndex)
        if (position == 0) return ""

        val start = position + int(position)
        val length = int(start)
        val bytes = ByteArray(length)
        duplicate().apply {
            position(start + Int.SIZE_BYTES)
            get(bytes)
        }
        return bytes.decodeToString()
    }

    private fun ByteBuffer.intVector(vector: Int): List<Int> {
        return List(vectorLength(vector)) { index ->
            int(vector + Int.SIZE_BYTES + index * Int.SIZE_BYTES)
        }
    }

    private fun ByteBuffer.tableVectorElement(vector: Int, index: Int): Int {
        val position = vector + Int.SIZE_BYTES + index * Int.SIZE_BYTES
        return position + int(position)
    }

    private fun ByteBuffer.vectorLength(vector: Int): Int {
        return if (vector == 0) 0 else int(vector)
    }

    private fun ByteBuffer.int(offset: Int): Int {
        return getInt(offset)
    }

    private fun ByteBuffer.byte(offset: Int): Byte {
        return if (offset == 0) 0 else get(offset)
    }

    private fun ByteBuffer.ushort(offset: Int): Int {
        return getShort(offset).toInt() and 0xffff
    }

    private fun ByteBuffer.ascii(start: Int, end: Int): String {
        val bytes = ByteArray(end - start)
        duplicate().apply {
            position(start)
            get(bytes)
        }
        return bytes.decodeToString()
    }

    private const val MODEL_SUBGRAPHS_FIELD = 2
    private const val SUBGRAPH_TENSORS_FIELD = 0
    private const val SUBGRAPH_INPUTS_FIELD = 1
    private const val SUBGRAPH_OUTPUTS_FIELD = 2
    private const val TENSOR_SHAPE_FIELD = 0
    private const val TENSOR_TYPE_FIELD = 1
    private const val TENSOR_NAME_FIELD = 3
}
