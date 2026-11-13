package com.example.absensiapps.ui.employee

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.graphics.PointF

class FaceLandmarkOverlay @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val pointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        strokeWidth = 5f
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 30f
        textAlign = Paint.Align.CENTER
        setShadowLayer(5f, 1f, 1f, Color.BLACK)
    }

    private var landmarks: List<PointF> = emptyList()
    private var sourceWidth = 0
    private var sourceHeight = 0

    fun updateLandmarks(
        points: List<PointF>,
        imageWidth: Int,
        imageHeight: Int
    ) {
        post {
            landmarks = points.map {
                PointF(it.x, it.y)
            }

            sourceWidth = imageWidth
            sourceHeight = imageHeight

            invalidate()
        }
    }

    fun clear() {
        post {
            landmarks = emptyList()
            invalidate()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (landmarks.size != 5 ||
            sourceWidth <= 0 ||
            sourceHeight <= 0
        ) return

        // Mengikuti skala centerCrop pada preview.
        val scale = maxOf(
            width.toFloat() / sourceWidth,
            height.toFloat() / sourceHeight
        )

        val offsetX = (width - sourceWidth * scale) / 2f
        val offsetY = (height - sourceHeight * scale) / 2f

        val labels = listOf(
            "L Eye",
            "R Eye",
            "Nose",
            "L Mouth",
            "R Mouth"
        )

        val colors = listOf(
            Color.GREEN,
            Color.GREEN,
            Color.CYAN,
            Color.YELLOW,
            Color.YELLOW
        )

        landmarks.forEachIndexed { index, point ->

            // Preview kamera depan biasanya mirror.
            val mirroredX = sourceWidth - point.x

            val x = mirroredX * scale + offsetX
            val y = point.y * scale + offsetY

            pointPaint.color = colors[index]

            canvas.drawCircle(x, y, 10f, pointPaint)

            canvas.drawText(
                labels[index],
                x,
                y - 18f,
                textPaint
            )
        }
    }
}
