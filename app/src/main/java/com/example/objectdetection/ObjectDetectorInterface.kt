package com.example.objectdetection

import android.graphics.Bitmap
import android.graphics.RectF

/**
 * Interface for object detection implementations
 * This allows us to easily swap between real and mock implementations
 */
interface ObjectDetectorInterface {

    /**
     * Detect objects in the given bitmap
     * @param bitmap The image to detect objects in
     * @return List of detection results
     */
    fun detectObjects(bitmap: Bitmap): List<ObjectDetector.DetectionResult>

    /**
     * Close the detector and release resources
     */
    fun close()
}