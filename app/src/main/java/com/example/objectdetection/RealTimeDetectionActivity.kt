package com.example.objectdetection

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class RealTimeDetectionActivity : AppCompatActivity() {
    private lateinit var previewView: PreviewView
    private lateinit var detectionOverlay: DetectionOverlay
    private lateinit var backButton: Button
    private var cameraExecutor: ExecutorService? = null
    private var objectDetector: ObjectDetectorInterface? = null

    companion object {
        private const val TAG = "RealTimeDetection"
        private const val MODEL_PATH = "model.tflite"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_real_time_detection)

        // Initialize views
        previewView = findViewById(R.id.previewView)
        detectionOverlay = findViewById(R.id.detectionOverlay)
        backButton = findViewById(R.id.backButton)

        // Initialize camera executor first to avoid NPE
        cameraExecutor = Executors.newSingleThreadExecutor()

        // Initialize the object detector using the factory
        try {
            objectDetector = ObjectDetectorFactory.createDetector(this, MODEL_PATH)
            startCamera() // Only start camera if initialization succeeds
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing detector: ${e.message}")
            Toast.makeText(this, "Failed to initialize detector: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // Back button
        backButton.setOnClickListener {
            finish()
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()

                // Set up the preview
                val preview = Preview.Builder()
                    .build()
                    .also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                // Set up the image analyzer with a smaller resolution to improve performance
                val imageAnalyzer = ImageAnalysis.Builder()
                    .setTargetResolution(Size(512, 512)) // Smaller resolution
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(cameraExecutor!!, ObjectDetectionAnalyzer())
                    }

                // Select back camera
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                // Unbind any bound use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalyzer
                )

            } catch (e: Exception) {
                Log.e(TAG, "Use case binding failed", e)
                Toast.makeText(this, "Failed to start camera: ${e.message}", Toast.LENGTH_SHORT).show()
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private inner class ObjectDetectionAnalyzer : ImageAnalysis.Analyzer {
        private val matrix = Matrix().apply {
            postRotate(90f) // Most phones need 90 degree rotation for portrait mode
        }

        @SuppressLint("UnsafeOptInUsageError")
        override fun analyze(imageProxy: ImageProxy) {
            try {
                val mediaImage = imageProxy.image
                if (mediaImage != null && objectDetector != null) {
                    // Convert the image to bitmap
                    val bitmap = imageProxy.toBitmap()

                    // Rotate the bitmap if needed
                    val rotatedBitmap = Bitmap.createBitmap(
                        bitmap,
                        0, 0,
                        bitmap.width, bitmap.height,
                        matrix,
                        false
                    )

                    // Process the image
                    val results = objectDetector!!.detectObjects(rotatedBitmap)

                    // Calculate scale factors for mapping detection coordinates to overlay view
                    val scaleFactor = detectionOverlay.width.toFloat() / rotatedBitmap.width.toFloat()

                    // Update the overlay on the main thread
                    runOnUiThread {
                        detectionOverlay.setDetectionResults(
                            results,
                            scaleFactor,
                            0f,
                            0f
                        )
                    }

                    // Must close the image when done
                    bitmap.recycle()
                    rotatedBitmap.recycle()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error analyzing image: ${e.message}")
            } finally {
                imageProxy.close()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor?.shutdown()
        objectDetector?.close()
    }
}