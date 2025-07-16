package com.example.objectdetection

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import android.util.Log
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder

class ObjectDetector(private val context: Context, modelPath: String) {

    private var interpreter: Interpreter
    private val imageSizeX: Int
    private val imageSizeY: Int
    private val labels: List<String>
    private var outputShape: IntArray? = null

    // You'll need to adjust these based on your specific model
    companion object {
        private const val TAG = "ObjectDetector"
        private const val DETECTION_THRESHOLD = 0.5f
        private const val MAX_DETECTION_RESULTS = 10
    }

    data class DetectionResult(
        val boundingBox: RectF,
        val label: String,
        val confidence: Float
    )

    init {
        // Load model with proper options to avoid buffer size issues
        val options = Interpreter.Options()
        options.setNumThreads(4)

        val model = FileUtil.loadMappedFile(context, modelPath)
        interpreter = Interpreter(model, options)

        // Get input dimensions
        val inputShape = interpreter.getInputTensor(0).shape()
        Log.d(TAG, "Model input shape: ${inputShape.contentToString()}")

        // The input shape for TFLite object detection models is typically [1, height, width, 3]
        imageSizeY = inputShape[1]
        imageSizeX = inputShape[2]

        Log.d(TAG, "Model expects input dimensions: $imageSizeX x $imageSizeY")

        // Log output shape
        for (i in 0 until interpreter.outputTensorCount) {
            val shape = interpreter.getOutputTensor(i).shape()
            Log.d(TAG, "Output tensor $i shape: ${shape.contentToString()}")
            if (i == 0) {
                outputShape = shape
            }
        }

        // Load labels - with better error handling
        labels = try {
            context.assets.open("labels.txt").bufferedReader().readLines()
        } catch (e: IOException) {
            Log.e(TAG, "Error loading labels: ${e.message}")
            // Provide default labels if file is missing
            listOf(
                "labels.txt is missing btw"
            )
        }
    }

    fun detectObjects(bitmap: Bitmap): List<DetectionResult> {
        try {
            Log.d(TAG, "Input bitmap dimensions: ${bitmap.width} x ${bitmap.height}")

            // Create ImageProcessor to resize and normalize the image
            val imageProcessor = ImageProcessor.Builder()
                .add(ResizeOp(imageSizeY, imageSizeX, ResizeOp.ResizeMethod.BILINEAR))
                .add(NormalizeOp(0f, 255f))  // Normalize pixel values
                .build()

            // Create a TensorImage object from the bitmap
            val tensorImage = TensorImage.fromBitmap(bitmap)

            // Process the image to match model input requirements
            val processedImage = imageProcessor.process(tensorImage)

            // Check the processed image size
            val processedBuffer = processedImage.buffer
            Log.d(TAG, "Processed buffer capacity: ${processedBuffer.capacity()} bytes")

            // Get the actual output shape from the model
            val outputShape = outputShape ?: interpreter.getOutputTensor(0).shape()
            Log.d(TAG, "Using output shape: ${outputShape.contentToString()}")

            // Dynamically create output buffer based on actual model output shape
            val outputBuffer = ByteBuffer.allocateDirect(outputShape.fold(1) { acc, i -> acc * i } * 4)
                .order(ByteOrder.nativeOrder())

            // Run inference with dynamic output
            interpreter.run(processedImage.buffer, outputBuffer)

            // Reset buffer position for reading
            outputBuffer.rewind()

            // Now parse the output buffer based on the model's format
            // This is a generic parsing that needs to be adjusted for your specific model
            val results = mutableListOf<DetectionResult>()

            // For SSD and similar models, the output is often in the format:
            // [1, num_detections, box_data]
            // where box_data contains [ymin, xmin, ymax, xmax, score, class_id]

            // Assuming the model outputs detection boxes with scores and class IDs
            // We need to parse based on the specific model format
            if (outputShape.size >= 2) {
                val numDetections = outputShape[1]

                // Calculate how many values per detection
                // For many models it's 4 (box) + 1 (score) + 1 (class) = 6
                // But could be different for your model
                val valuesPerDetection = if (outputShape.size >= 3) {
                    outputShape[2] / 6 // Assuming standard format
                } else {
                    6 // Default if we can't determine
                }

                for (i in 0 until Math.min(numDetections, MAX_DETECTION_RESULTS)) {
                    // Read values based on the model's output format
                    try {
                        // Read directly from buffer - adjust indices based on your model
                        val score = outputBuffer.getFloat((i * valuesPerDetection + 4) * 4)

                        if (score >= DETECTION_THRESHOLD) {
                            val classId = outputBuffer.getFloat((i * valuesPerDetection + 5) * 4).toInt()
                            val label = if (classId < labels.size && classId >= 0) labels[classId] else "Unknown"

                            // Read bounding box - format may vary by model
                            // Some models use [ymin, xmin, ymax, xmax], others use [xmin, ymin, xmax, ymax]
                            val ymin = outputBuffer.getFloat((i * valuesPerDetection + 0) * 4)
                            val xmin = outputBuffer.getFloat((i * valuesPerDetection + 1) * 4)
                            val ymax = outputBuffer.getFloat((i * valuesPerDetection + 2) * 4)
                            val xmax = outputBuffer.getFloat((i * valuesPerDetection + 3) * 4)

                            // Convert normalized coordinates (0-1) to pixel coordinates
                            val boundingBox = RectF(
                                xmin * bitmap.width,
                                ymin * bitmap.height,
                                xmax * bitmap.width,
                                ymax * bitmap.height
                            )

                            results.add(DetectionResult(boundingBox, label, score))
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing detection $i: ${e.message}")
                        // Continue with next detection
                    }
                }
            } else {
                Log.e(TAG, "Unexpected output shape: ${outputShape.contentToString()}")
            }

            Log.d(TAG, "Detected ${results.size} objects")
            return results
        } catch (e: Exception) {
            Log.e(TAG, "Error in object detection: ${e.message}", e)
            return emptyList()
        }
    }

    fun close() {
        try {
            interpreter.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing interpreter: ${e.message}")
        }
    }
}