package com.example.absensiapps.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PointF
import kotlin.math.abs

object FaceAligner {

    const val INPUT_SIZE = 112

    /*
     * Titik referensi wajah MobileFaceNet Qualcomm.
     *
     * Urutan:
     * 1. Mata kiri subjek
     * 2. Mata kanan subjek
     * 3. Pangkal hidung
     * 4. Sudut mulut kiri subjek
     * 5. Sudut mulut kanan subjek
     */

    private val referencePoints = listOf(
        PointF(44.1964f, 53.1309f),
        PointF(67.6879f, 53.0009f),
        PointF(56.0168f, 66.4911f),
        PointF(46.3662f, 80.2437f),
        PointF(65.8199f, 80.1361f)
    )

    // =====================================
    // ALIGN FACE
    // =====================================

    fun align(
        source: Bitmap,
        landmarks: List<PointF>
    ): Bitmap {

        require(landmarks.size == 5) {
            "Diperlukan tepat 5 titik landmark wajah."
        }

        require(source.width > 0 && source.height > 0) {
            "Bitmap tidak valid."
        }

        val sourceMeanX =
            landmarks.sumOf { it.x.toDouble() } / 5.0

        val sourceMeanY =
            landmarks.sumOf { it.y.toDouble() } / 5.0

        val targetMeanX =
            referencePoints.sumOf { it.x.toDouble() } / 5.0

        val targetMeanY =
            referencePoints.sumOf { it.y.toDouble() } / 5.0

        var denominator = 0.0
        var numeratorA = 0.0
        var numeratorB = 0.0

        // =====================================
        // SIMILARITY TRANSFORM
        // =====================================

        for (i in 0 until 5) {

            val sx = landmarks[i].x - sourceMeanX
            val sy = landmarks[i].y - sourceMeanY

            val tx = referencePoints[i].x - targetMeanX
            val ty = referencePoints[i].y - targetMeanY

            denominator += sx * sx + sy * sy

            numeratorA += sx * tx + sy * ty

            numeratorB += sx * ty - sy * tx
        }

        require(denominator > 1e-6) {
            "Landmark wajah tidak valid."
        }

        val a = numeratorA / denominator
        val b = numeratorB / denominator

        val translateX =
            targetMeanX - (a * sourceMeanX - b * sourceMeanY)

        val translateY =
            targetMeanY - (b * sourceMeanX + a * sourceMeanY)

        // =====================================
        // CREATE MATRIX
        // =====================================

        val matrix = Matrix()

        matrix.setValues(
            floatArrayOf(
                a.toFloat(),
                (-b).toFloat(),
                translateX.toFloat(),

                b.toFloat(),
                a.toFloat(),
                translateY.toFloat(),

                0f,
                0f,
                1f
            )
        )

        // =====================================
        // CROP AND ALIGN
        // =====================================

        val alignedBitmap = Bitmap.createBitmap(
            INPUT_SIZE,
            INPUT_SIZE,
            Bitmap.Config.ARGB_8888
        )

        val canvas = Canvas(alignedBitmap)

        val paint = Paint(
            Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG
        )

        canvas.drawBitmap(
            source,
            matrix,
            paint
        )

        return alignedBitmap
    }
}
