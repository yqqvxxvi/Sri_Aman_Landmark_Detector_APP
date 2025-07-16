package com.example.objectdetection

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import android.util.Log

/**
 * A completely safe implementation that doesn't use TensorFlow Lite at all
 * This avoids any initialization or processing crashes
 */
class UltraSafeObjectDetector(private val context: Context) : ObjectDetectorInterface {

    companion object {
        private const val TAG = "UltraSafeDetector"
    }

    init {
        Log.d(TAG, "Initialized ultra-safe detector (no TensorFlow)")
    }

    override fun detectObjects(bitmap: Bitmap): List<ObjectDetector.DetectionResult> {
        Log.d(TAG, "Creating test detections for bitmap ${bitmap.width}x${bitmap.height}")

        val results = mutableListOf<ObjectDetector.DetectionResult>()

        // Create 3 test detections
        for (i in 0 until 3) {
            val left = (0.2f + i * 0.2f) * bitmap.width
            val top = (0.2f + i * 0.15f) * bitmap.height
            val right = left + 0.2f * bitmap.width
            val bottom = top + 0.2f * bitmap.height

            val boundingBox = RectF(left, top, right, bottom)
            val label = "Test Object ${i+1}"
            val confidence = 0.9f - (i * 0.1f)

            results.add(ObjectDetector.DetectionResult(boundingBox, label, confidence))
        }

        Log.d(TAG, "Created ${results.size} test detections")
        return results
    }

    override fun close() {
        // Nothing to close
        Log.d(TAG, "Closed ultra-safe detector")
    }
}