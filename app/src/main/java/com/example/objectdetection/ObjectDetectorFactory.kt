package com.example.objectdetection

import android.content.Context
import android.util.Log

/**
 * Factory for creating object detectors
 */
object ObjectDetectorFactory {

    private const val TAG = "ObjectDetectorFactory"

    // Change these flags to control which detector implementation is used
    private const val USE_MOCK_DETECTOR = false // Set to false to use real detector

    /**
     * Create an object detector
     * @param context Application context
     * @param modelPath Path to TensorFlow Lite model file
     * @return An implementation of ObjectDetectorInterface
     */
    fun createDetector(context: Context, modelPath: String): ObjectDetectorInterface {
        // If mock detector is requested, use it
        if (USE_MOCK_DETECTOR) {
            Log.d(TAG, "Creating mock object detector")
            return MockObjectDetector(context)
        }

        // Try to use TFLite detector (with safety fallback)
        try {
            Log.d(TAG, "Creating TFLite YOLO object detector with model: $modelPath")
            return TFLiteObjectDetector(context, modelPath)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create TFLite detector, falling back to mock: ${e.message}")
            // Fall back to mock detector if TFLite fails
            return MockObjectDetector(context)
        }
    }
}