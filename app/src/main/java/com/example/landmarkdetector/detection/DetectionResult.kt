package com.example.landmarkdetector.detection

data class DetectionResult(
    val boundingBox: BoundingBox,
    val confidence: Float,
    val classIndex: Int,
    val className: String
)

data class BoundingBox(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
)