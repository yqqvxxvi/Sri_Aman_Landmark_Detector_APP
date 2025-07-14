package com.example.landmarkdetector

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.ImageProxy
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.example.landmarkdetector.detection.DetectionEngine
import com.example.landmarkdetector.detection.DetectionEngineImpl
import com.example.landmarkdetector.detection.YOLOv11Detector
import com.example.landmarkdetector.ui.BoundingBoxOverlay
import com.example.landmarkdetector.ui.CameraPreview

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts.GetContent



class MainActivity : AppCompatActivity(), CameraPreview.ImageAnalysisCallback {
    private lateinit var previewView: PreviewView
    private lateinit var boundingBoxOverlay: BoundingBoxOverlay
    private lateinit var cameraPreview: CameraPreview
    private lateinit var detectionEngine: DetectionEngine

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            initializeCamera()
        } else {
            Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        previewView = findViewById(R.id.preview_view)
        boundingBoxOverlay = findViewById(R.id.bounding_box_overlay)

        detectionEngine = DetectionEngineImpl(YOLOv11Detector(this))

        checkCameraPermission()
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED) {
            initializeCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun initializeCamera() {
        cameraPreview = CameraPreview(this, previewView, this)
        cameraPreview.setImageAnalysisCallback(this)
        cameraPreview.startCamera()
    }

    override fun onImageAnalysis(imageProxy: ImageProxy) {
        try {
            val results = detectionEngine.detect(imageProxy)
            Log.d("MainActivity", "Detections size: ${results.size}")

            for (r in results) {
                Log.d("MainActivity", "Detected ${r.className} with ${r.confidence} at ${r.boundingBox}")
            }

            runOnUiThread {
                boundingBoxOverlay.updateDetections(results)
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error during image analysis", e)
        } finally {
            imageProxy.close()
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        cameraPreview.shutdown()
        detectionEngine.close()
    }

    override fun onPause() {
        super.onPause()
        cameraPreview.stopCamera()
    }

    override fun onResume() {
        super.onResume()
        if (::cameraPreview.isInitialized) {
            cameraPreview.startCamera()
        }
    }
}