package com.example.landmarkdetector.detection

import android.content.Context
import android.util.Log
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.YuvImage
import androidx.camera.core.ImageProxy
import com.example.landmarkdetector.utils.Constants
import com.example.landmarkdetector.utils.ImageUtils
import org.tensorflow.lite.Interpreter
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel




class YOLOv11Detector(context: Context) : Detector {
    private val interpreter: Interpreter
    private val inputSize = 512
    private val confidenceThreshold = 0.5f
    private val nmsThreshold = 0.5f

    init {
        val assetFileDescriptor = context.assets.openFd("best_float32.tflite")
        val fileInputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = fileInputStream.channel
        val startOffset = assetFileDescriptor.startOffset
        val declaredLength = assetFileDescriptor.declaredLength

        val modelBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
        interpreter = Interpreter(modelBuffer)

    }

    override fun detect(imageProxy: ImageProxy): List<DetectionResult> {
        val bitmap = imageProxyToBitmap(imageProxy)
        val resizedBitmap: Bitmap = ImageUtils.letterbox(bitmap, inputSize)



        val inputBuffer = ByteBuffer.allocateDirect(4 * inputSize * inputSize * 3)
        inputBuffer.order(ByteOrder.nativeOrder())

        // Preprocess image
        val pixels = IntArray(inputSize * inputSize)
        resizedBitmap.getPixels(pixels, 0, inputSize, 0, 0, inputSize, inputSize)

        for (pixel in pixels) {
            inputBuffer.putFloat(((pixel shr 16) and 0xFF) / 255.0f) // R
            inputBuffer.putFloat(((pixel shr 8) and 0xFF) / 255.0f)  // G
            inputBuffer.putFloat((pixel and 0xFF) / 255.0f)          // B
        }

        // Run inference



        val outputArray = Array(1) { Array(14) { FloatArray(5376) } }


        val outputTensor = interpreter.getOutputTensor(0)
        Log.d("YOLOv11", "Output shape: ${outputTensor.shape().joinToString()}, type: ${outputTensor.dataType()}")

        try {
            interpreter.run(inputBuffer, outputArray)
            Log.d("YOLOv11", "Interpreter ran successfully.")
        } catch (e: Exception) {
            Log.e("YOLOv11", "Interpreter.run failed", e)
        }



        return parseOutput(outputArray, imageProxy.width, imageProxy.height)
    }

    private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap {
        val yuvImage = YuvImage(
            ImageUtils.imageProxyToByteArray(imageProxy),
            ImageFormat.NV21,
            imageProxy.width,
            imageProxy.height,
            null
        )
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(android.graphics.Rect(0, 0, imageProxy.width, imageProxy.height), 100, out)
        return BitmapFactory.decodeByteArray(out.toByteArray(), 0, out.size())
    }

    private fun parseOutput(
        output: Array<Array<FloatArray>>,
        imageWidth: Int,
        imageHeight: Int
    ): List<DetectionResult> {
        val detections = mutableListOf<DetectionResult>()

        for (i in 0 until 5376) {
            val x = output[0][0][i]
            val y = output[0][1][i]
            val w = output[0][2][i]
            val h = output[0][3][i]

            // Filter out invalid boxes
            if (w <= 0f || h <= 0f) continue

            val confidences = FloatArray(Constants.NUM_CLASSES)
            for (j in 0 until Constants.NUM_CLASSES) {
                confidences[j] = output[0][4 + j][i]
            }

            val maxConfidence = confidences.maxOrNull() ?: 0f
            if (maxConfidence > confidenceThreshold) {
                val classIndex = confidences.indexOfFirst { it == maxConfidence }
                val className = Constants.CLASS_NAMES[classIndex]

                // Normalize and clamp to [0, 1]
                val left = (x - w / 2f).coerceIn(0f, 1f)
                val top = (y - h / 2f).coerceIn(0f, 1f)
                val right = (x + w / 2f).coerceIn(0f, 1f)
                val bottom = (y + h / 2f).coerceIn(0f, 1f)
                // Optional sanity check: box must have area
                if ((right - left) <= 0f || (bottom - top) <= 0f) continue

                detections.add(
                    DetectionResult(
                        BoundingBox(left, top, right, bottom),
                        maxConfidence,
                        classIndex,
                        className
                    )
                )
            }
        }

        return applyNMS(detections)
    }



    private fun applyNMS(detections: List<DetectionResult>): List<DetectionResult> {
        val sortedDetections = detections.sortedByDescending { it.confidence }
        val results = mutableListOf<DetectionResult>()

        for (detection in sortedDetections) {
            var shouldAdd = true
            for (result in results) {
                if (calculateIoU(detection.boundingBox, result.boundingBox) > nmsThreshold) {
                    shouldAdd = false
                    break
                }
            }
            if (shouldAdd) {
                results.add(detection)
            }
        }

        return results
    }

    private fun calculateIoU(box1: BoundingBox, box2: BoundingBox): Float {
        val intersectionLeft = maxOf(box1.left, box2.left)
        val intersectionTop = maxOf(box1.top, box2.top)
        val intersectionRight = minOf(box1.right, box2.right)
        val intersectionBottom = minOf(box1.bottom, box2.bottom)

        val intersectionArea = maxOf(0f, intersectionRight - intersectionLeft) *
                maxOf(0f, intersectionBottom - intersectionTop)

        val box1Area = (box1.right - box1.left) * (box1.bottom - box1.top)
        val box2Area = (box2.right - box2.left) * (box2.bottom - box2.top)

        return intersectionArea / (box1Area + box2Area - intersectionArea)
    }

    override fun close() {
        interpreter.close()
    }
}