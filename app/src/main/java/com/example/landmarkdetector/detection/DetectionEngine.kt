package com.example.landmarkdetector.detection

import androidx.camera.core.ImageProxy

interface DetectionEngine {
    fun detect(imageProxy: ImageProxy): List<DetectionResult>
    fun close()
}

class DetectionEngineImpl(private val detector: Detector) : DetectionEngine {
    override fun detect(imageProxy: ImageProxy): List<DetectionResult> {
        return detector.detect(imageProxy)
    }

    override fun close() {
        detector.close()
    }
}

interface Detector {
    fun detect(imageProxy: ImageProxy): List<DetectionResult>
    fun close()
}