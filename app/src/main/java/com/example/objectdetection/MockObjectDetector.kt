package com.example.objectdetection

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import android.util.Log
import java.util.Random

/**
 * A mock implementation of object detection that doesn't use TensorFlow Lite.
 * This is useful for testing the UI and camera pipeline without model issues.
 */
class MockObjectDetector(private val context: Context) : ObjectDetectorInterface {

    companion object {
        private const val TAG = "MockObjectDetector"
    }

    // Sample labels
    private val labels = listOf(
        "person", "bicycle", "car", "motorcycle", "airplane", "bus", "train", "truck", "boat",
        "traffic light", "fire hydrant", "stop sign", "parking meter"
    )

    // Random number generator for mock detections
    private val random = Random()

    init {
        Log.d(TAG, "Initialized mock object detector")
    }

    /**
     * Generate mock detection results instead of using TensorFlow Lite
     */
    override fun detectObjects(bitmap: Bitmap): List<ObjectDetector.DetectionResult> {
        // Log the bitmap dimensions
        Log.d(TAG, "Processing image: ${bitmap.width} x ${bitmap.height}")

        // Create 1-3 random detections
        val numDetections = random.nextInt(3) + 1
        val results = mutableListOf<ObjectDetector.DetectionResult>()

        for (i in 0 until numDetections) {
            // Create a random bounding box
            val left = random.nextFloat() * (bitmap.width * 0.7f)
            val top = random.nextFloat() * (bitmap.height * 0.7f)
            val width = (bitmap.width * 0.1f) + (random.nextFloat() * bitmap.width * 0.2f)
            val height = (bitmap.height * 0.1f) + (random.nextFloat() * bitmap.height * 0.2f)

            val boundingBox = RectF(
                left,
                top,
                left + width,
                top + height
            )

            // Pick a random label
            val label = labels[random.nextInt(labels.size)]

            // Generate a random confidence between 0.5 and 1.0
            val confidence = 0.5f + (random.nextFloat() * 0.5f)

            results.add(ObjectDetector.DetectionResult(boundingBox, label, confidence))
        }

        Log.d(TAG, "Generated ${results.size} mock detections")
        return results
    }

    /**
     * No-op close method
     */
    override fun close() {
        Log.d(TAG, "Closed mock object detector")
    }
}