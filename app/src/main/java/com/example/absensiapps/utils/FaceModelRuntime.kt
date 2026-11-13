package com.example.absensiapps.utils

import android.content.Context
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import java.io.Closeable
import java.nio.ByteBuffer
import java.nio.ByteOrder

class FaceModelRuntime(
    context: Context
) : Closeable {

    private val interpreter: Interpreter

    init {

        // =====================================
        // 1. LOAD MODEL FROM ASSETS
        // =====================================

        val modelBytes = context.assets
            .open("mobilefacenet.tflite")
            .use { it.readBytes() }

        val modelBuffer = ByteBuffer
            .allocateDirect(modelBytes.size)
            .order(ByteOrder.nativeOrder())
            .apply {

                put(modelBytes)

                rewind()
            }

        // =====================================
        // 2. INITIALIZE INTERPRETER
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
    // 3. VALIDATE MODEL INPUT / OUTPUT
    // =====================================

    private fun validateModel() {

        // Model menerima dua gambar
        check(interpreter.inputTensorCount == 2) {
            "Model harus memiliki 2 input."
        }

        check(interpreter.outputTensorCount == 1) {
            "Model harus memiliki 1 output."
        }

        val input1 = interpreter.getInputTensor(0)
        val input2 = interpreter.getInputTensor(1)
        val output = interpreter.getOutputTensor(0)

        // Bentuk input: [1, 3, 112, 112]
        val expectedInput = intArrayOf(
            1, 3, 112, 112
        )

        // Bentuk output: [2, 128]
        val expectedOutput = intArrayOf(
            2, 128
        )

        check(
            input1.shape().contentEquals(expectedInput)
        ) {
            "Bentuk input pertama tidak sesuai: ${input1.shape().contentToString()}"
        }

        check(
            input2.shape().contentEquals(expectedInput)
        ) {
            "Bentuk input kedua tidak sesuai: ${input2.shape().contentToString()}"
        }

        check(
            output.shape().contentEquals(expectedOutput)
        ) {
            "Bentuk output tidak sesuai: ${output.shape().contentToString()}"
        }

        check(input1.dataType() == DataType.FLOAT32) {
            "Input pertama harus FLOAT32."
        }

        check(input2.dataType() == DataType.FLOAT32) {
            "Input kedua harus FLOAT32."
        }

        check(output.dataType() == DataType.FLOAT32) {
            "Output harus FLOAT32."
        }
    }

    // =====================================
    // 4. MODEL INFORMATION
    // =====================================

    fun getModelInfo(): String {

        val input = interpreter.getInputTensor(0).shape()

        val output = interpreter.getOutputTensor(0).shape()

        return """
            MobileFaceNet berhasil dimuat.

            Input  : ${input.contentToString()}
            Output : ${output.contentToString()}

            Embedding: 128 dimensi
        """.trimIndent()
    }

    // =====================================
    // 5. RELEASE MODEL
    // =====================================

    override fun close() {

        interpreter.close()
    }
}
