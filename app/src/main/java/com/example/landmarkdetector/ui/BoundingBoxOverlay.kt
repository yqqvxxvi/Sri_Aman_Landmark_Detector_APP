package com.example.landmarkdetector.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.example.landmarkdetector.detection.DetectionResult

class BoundingBoxOverlay @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint().apply {
        color = Color.RED
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    private val textPaint = Paint().apply {
        color = Color.RED
        textSize = 40f
        isAntiAlias = true
    }

    private var detections: List<DetectionResult> = emptyList()

    fun updateDetections(newDetections: List<DetectionResult>) {
        detections = newDetections
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        for (detection in detections) {
            val box = detection.boundingBox

            // Scale from normalized [0.0–1.0] to actual screen pixels
            val left = box.left * width
            val top = box.top * height
            val right = box.right * width
            val bottom = box.bottom * height

            canvas.drawRect(left, top, right, bottom, paint)

            val label = "${detection.className} ${(detection.confidence * 100).toInt()}%"
            canvas.drawText(label, left, top - 10, textPaint)
        }
    }

}