package com.example.absensiapps.utils

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import java.io.Closeable
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.acos
import kotlin.math.sqrt

data class FaceComparison(
    val cosineSimilarity: Float,
    val angleDegrees: Float
)

class MobileFaceNetEngine(
    context: Context
) : Closeable {

    companion object {

        const val EMBEDDING_SIZE = 128

        private const val MODEL_FILE =
            "mobilefacenet.tflite"

        private const val INPUT_SIZE = 112
    }

    private val interpreter: Interpreter

    init {

        // =====================================
        // LOAD MODEL
        // =====================================

        val modelBytes = context.assets
            .open(MODEL_FILE)
            .use { it.readBytes() }

        val modelBuffer = ByteBuffer
            .allocateDirect(modelBytes.size)
            .order(ByteOrder.nativeOrder())

        modelBuffer.put(modelBytes)
        modelBuffer.rewind()

        // =====================================
        // INITIALIZE INTERPRETER
        // =====================================

        val options = Interpreter.Options().apply {
            setNumThreads(4)
        }

        interpreter = Interpreter(
            modelBuffer,
            options
        )

        validateModel()
    }

    // =====================================
    // VALIDATE MODEL
    // =====================================

    private fun validateModel() {

        check(interpreter.inputTensorCount == 2) {
            "Model harus mempunyai dua input."
        }

        check(interpreter.outputTensorCount == 1) {
            "Model harus mempunyai satu output."
        }

        val expectedInput = intArrayOf(
            1, 3, INPUT_SIZE, INPUT_SIZE
        )

        val expectedOutput = intArrayOf(
            2, EMBEDDING_SIZE
        )

        for (i in 0..1) {

            val tensor = interpreter.getInputTensor(i)

            check(tensor.shape().contentEquals(expectedInput)) {
                "Input $i tidak sesuai: ${tensor.shape().contentToString()}"
            }

            check(tensor.dataType() == DataType.FLOAT32) {
                "Input $i harus FLOAT32."
            }
        }

        val output = interpreter.getOutputTensor(0)

        check(output.shape().contentEquals(expectedOutput)) {
            "Output tidak sesuai: ${output.shape().contentToString()}"
        }

        check(output.dataType() == DataType.FLOAT32) {
            "Output harus FLOAT32."
        }
    }

    // =====================================
    // EXTRACT EMBEDDING
    // =====================================

    fun extractEmbedding(
        alignedFace: Bitmap
    ): FloatArray {

        // Menggunakan gambar yang sama sebagai dua input.
        // Output pertama menjadi embedding gambar tersebut.
        val result = runInference(
            alignedFace,
            alignedFace
        )

        return result.first
    }

    // =====================================
    // COMPARE TWO FACES
    // =====================================

    fun compareFaces(
        firstFace: Bitmap,
        secondFace: Bitmap
    ): FaceComparison {

        val result = runInference(
            firstFace,
            secondFace
        )

        val first = result.first
        val second = result.second

        var cosine = 0.0

        for (i in 0 until EMBEDDING_SIZE) {

            cosine += first[i] * second[i]
        }

        // Membatasi nilai agar tidak melewati -1 hingga 1.
        val safeCosine = cosine.coerceIn(-1.0, 1.0)

        val angle = Math.toDegrees(
            acos(safeCosine)
        )

        return FaceComparison(
            cosineSimilarity = safeCosine.toFloat(),
            angleDegrees = angle.toFloat()
        )
    }

    // =====================================
    // RUN INFERENCE
    // =====================================

    private fun runInference(
        firstFace: Bitmap,
        secondFace: Bitmap
    ): Pair<FloatArray, FloatArray> {

        validateBitmap(firstFace)
        validateBitmap(secondFace)

        val firstInput = bitmapToInput(firstFace)
        val secondInput = bitmapToInput(secondFace)

        // Output:
        // [0] = embedding gambar pertama
        // [1] = embedding gambar kedua

        val output = Array(2) {
            FloatArray(EMBEDDING_SIZE)
        }

        val outputs = mutableMapOf<Int, Any>()

        outputs[0] = output

        interpreter.runForMultipleInputsOutputs(
            arrayOf(firstInput, secondInput),
            outputs
        )

        return Pair(
            normalize(output[0]),
            normalize(output[1])
        )
    }

    // =====================================
    // BITMAP TO MODEL INPUT
    // =====================================

    private fun bitmapToInput(
        bitmap: Bitmap
    ): ByteBuffer {

        val size =
            INPUT_SIZE * INPUT_SIZE * 3

        val buffer = ByteBuffer
            .allocateDirect(size * 4)
            .order(ByteOrder.nativeOrder())

        val pixels = IntArray(
            INPUT_SIZE * INPUT_SIZE
        )

        bitmap.getPixels(
            pixels,
            0,
            INPUT_SIZE,
            0,
            0,
            INPUT_SIZE,
            INPUT_SIZE
        )

        // Model menggunakan NCHW:
        // [1, 3, 112, 112]
        //
        // Urutan channel:
        // R -> G -> B

        for (channel in 0..2) {

            for (y in 0 until INPUT_SIZE) {

                for (x in 0 until INPUT_SIZE) {

                    val pixel = pixels[
                        y * INPUT_SIZE + x
                    ]

                    val value = when (channel) {

                        0 -> (pixel shr 16) and 0xFF

                        1 -> (pixel shr 8) and 0xFF

                        else -> pixel and 0xFF
                    }

                    // Input model: 0.0 sampai 1.0
                    buffer.putFloat(
                        value / 255f
                    )
                }
            }
        }

        buffer.rewind()

        return buffer
    }

    // =====================================
    // NORMALIZE EMBEDDING
    // =====================================

    private fun normalize(
        embedding: FloatArray
    ): FloatArray {

        require(embedding.size == EMBEDDING_SIZE) {
            "Dimensi embedding tidak sesuai."
        }

        var sum = 0.0

        for (value in embedding) {

            require(value.isFinite()) {
                "Embedding mengandung nilai tidak valid."
            }

            sum += value * value
        }

        val norm = sqrt(sum)

        require(norm > 1e-12 && norm.isFinite()) {
            "Embedding tidak dapat dinormalisasi."
        }

        return FloatArray(EMBEDDING_SIZE) { index ->

            (embedding[index] / norm).toFloat()

        }
    }

    // =====================================
    // VALIDATE BITMAP
    // =====================================

    private fun validateBitmap(bitmap: Bitmap) {

        require(
            bitmap.width == INPUT_SIZE &&
                    bitmap.height == INPUT_SIZE
        ) {
            "Gambar wajah harus berukuran 112 x 112. " +
                    "Ukuran sekarang: ${bitmap.width} x ${bitmap.height}"
        }
    }

    // =====================================
    // MODEL INFORMATION
    // =====================================

    fun getModelInfo(): String {

        return """
            MobileFaceNet Ready

            Input  : [1, 3, 112, 112]
            Output : [2, 128]

            Embedding: 128 dimensions
            Data: FLOAT32
        """.trimIndent()
    }

    // =====================================
    // CLOSE
    // =====================================

    override fun close() {

        interpreter.close()
    }
}
