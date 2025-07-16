package com.example.objectdetection

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Log
import android.view.View

class DetectionOverlay @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val TAG = "DetectionOverlay"

    private val boxPaint = Paint().apply {
        color = Color.RED
        style = Paint.Style.STROKE
        strokeWidth = 8f
    }

    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 48f
        style = Paint.Style.FILL
    }

    private val textBackgroundPaint = Paint().apply {
        color = Color.RED
        alpha = 160
        style = Paint.Style.FILL
    }

    private var detectionResults: List<ObjectDetector.DetectionResult> = listOf()
    private var scaleFactor = 1f
    private var offsetX = 0f
    private var offsetY = 0f

    fun setDetectionResults(
        results: List<ObjectDetector.DetectionResult>,
        scaleFactor: Float = 1f,
        offsetX: Float = 0f,
        offsetY: Float = 0f
    ) {
        Log.d(TAG, "Setting detection results: ${results.size} items, scale=$scaleFactor, offset=($offsetX,$offsetY)")
        this.detectionResults = results
        this.scaleFactor = scaleFactor
        this.offsetX = offsetX
        this.offsetY = offsetY
        invalidate() // Request redraw

        // Force another invalidate after a short delay in case the first one doesn't work
        postDelayed({ invalidate() }, 100)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        Log.d(TAG, "onDraw called with ${detectionResults.size} results")

        // Draw a test box if there are no results (for debugging)
        if (detectionResults.isEmpty()) {
            // Draw a test box in the center for debugging
            val testBox = RectF(
                width * 0.25f,
                height * 0.25f,
                width * 0.75f,
                height * 0.75f
            )
            canvas.drawRect(testBox, boxPaint)
            Log.d(TAG, "Drew test box because no results were available")
        }

        detectionResults.forEach { result ->
            try {
                // Scale and transform the bounding box to match the view
                val scaledBox = RectF(
                    result.boundingBox.left * scaleFactor + offsetX,
                    result.boundingBox.top * scaleFactor + offsetY,
                    result.boundingBox.right * scaleFactor + offsetX,
                    result.boundingBox.bottom * scaleFactor + offsetY
                )

                Log.d(TAG, "Drawing box at ${scaledBox.left},${scaledBox.top} - ${scaledBox.right},${scaledBox.bottom}")

                // Draw bounding box
                canvas.drawRect(scaledBox, boxPaint)

                // Prepare label text
                val label = "${result.label} (${String.format("%.1f", result.confidence * 100)}%)"
                val textWidth = textPaint.measureText(label)

                // Draw label background
                canvas.drawRect(
                    scaledBox.left,
                    scaledBox.top - 60f,
                    scaledBox.left + textWidth + 16f,
                    scaledBox.top,
                    textBackgroundPaint
                )

                // Draw label text
                canvas.drawText(
                    label,
                    scaledBox.left + 8f,
                    scaledBox.top - 16f,
                    textPaint
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error drawing detection result: ${e.message}")
            }
        }
    }

    // Override onSizeChanged to know when the view size changes
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        Log.d(TAG, "onSizeChanged: $w x $h (was $oldw x $oldh)")
    }
}