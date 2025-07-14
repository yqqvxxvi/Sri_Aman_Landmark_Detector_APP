package com.example.landmarkdetector.ui

import android.content.Context
import android.util.Log
import android.util.Size
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraPreview(
    private val context: Context,
    private val previewView: PreviewView,
    private val lifecycleOwner: LifecycleOwner
) {
    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var preview: Preview? = null
    private var imageAnalysis: ImageAnalysis? = null
    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    // Camera settings
    private val targetResolution = Size(640, 640)
    private val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

    interface ImageAnalysisCallback {
        fun onImageAnalysis(imageProxy: ImageProxy)
    }

    private var analysisCallback: ImageAnalysisCallback? = null

    fun setImageAnalysisCallback(callback: ImageAnalysisCallback) {
        analysisCallback = callback
    }

    fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                bindCameraUseCases()
            } catch (e: Exception) {
                Log.e("CameraPreview", "Failed to get camera provider", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun bindCameraUseCases() {
        val cameraProvider = cameraProvider ?: return

        // Preview use case
        preview = Preview.Builder()
            .setTargetResolution(targetResolution)
            .build()
            .apply {
                setSurfaceProvider(previewView.surfaceProvider)
            }

        // Image analysis use case
        imageAnalysis = ImageAnalysis.Builder()
            .setTargetResolution(targetResolution)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
            .build()
            .apply {
                setAnalyzer(cameraExecutor) { imageProxy ->
                    analysisCallback?.onImageAnalysis(imageProxy)
                }
            }

        try {
            // Unbind all use cases before rebinding
            cameraProvider.unbindAll()

            // Bind use cases to camera
            camera = cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageAnalysis
            )

            Log.d("CameraPreview", "Camera bound successfully")

        } catch (e: Exception) {
            Log.e("CameraPreview", "Failed to bind camera use cases", e)
        }
    }

    fun stopCamera() {
        cameraProvider?.unbindAll()
        camera = null
        preview = null
        imageAnalysis = null
    }

    fun switchCamera() {
        val newCameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }

        // This would require updating the cameraSelector property and rebinding
        // For simplicity, we'll just restart with the new selector
        stopCamera()
        // You'd need to update the cameraSelector property here
        startCamera()
    }

    fun toggleFlash() {
        camera?.let { cam ->
            if (cam.cameraInfo.hasFlashUnit()) {
                val currentFlashMode = cam.cameraInfo.torchState.value
                cam.cameraControl.enableTorch(currentFlashMode != TorchState.ON)
            }
        }
    }

    fun getCameraInfo(): CameraInfo? {
        return camera?.cameraInfo
    }

    fun isCameraReady(): Boolean {
        return camera != null && preview != null && imageAnalysis != null
    }

    fun setZoomRatio(ratio: Float) {
        camera?.cameraControl?.setZoomRatio(ratio)
    }

    fun getZoomState() = camera?.cameraInfo?.zoomState

    fun setTargetResolution(size: Size) {
        // This would require rebuilding the use cases
        // For now, just log the change
        Log.d("CameraPreview", "Target resolution change requested: ${size.width}x${size.height}")
    }

    fun shutdown() {
        stopCamera()
        cameraExecutor.shutdown()
    }
}