package com.example.objectdetection

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.RectF
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * Gallery detection activity with explicit permission handling
 */
class GalleryDetectionActivity : AppCompatActivity() {
    private lateinit var imageView: ImageView
    private lateinit var detectionOverlay: DetectionOverlay
    private lateinit var selectImageButton: Button
    private lateinit var detectButton: Button
    private lateinit var backButton: Button
    private var objectDetector: ObjectDetectorInterface? = null
    private var selectedImageBitmap: Bitmap? = null

    companion object {
        private const val TAG = "GalleryPermissions"
        private const val MODEL_PATH = "model.tflite"
        private const val IMAGE_PICK_REQUEST = 1001
        private const val PERMISSION_REQUEST_CODE = 1002
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate called")

        setContentView(R.layout.activity_gallery_detection)

        // Initialize views
        imageView = findViewById(R.id.imageView)
        detectionOverlay = findViewById(R.id.detectionOverlay)
        selectImageButton = findViewById(R.id.selectImageButton)
        detectButton = findViewById(R.id.detectButton)
        backButton = findViewById(R.id.backButton)

        // Make sure the overlay is visible and on top
        detectionOverlay.visibility = View.VISIBLE
        detectionOverlay.bringToFront()

        // Initialize object detector using the factory
        try {
            objectDetector = ObjectDetectorFactory.createDetector(this, MODEL_PATH)
            Log.d(TAG, "Object detector initialized: ${objectDetector?.javaClass?.simpleName}")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing detector: ${e.message}", e)
            Toast.makeText(this, "Failed to initialize detector: ${e.message}", Toast.LENGTH_LONG).show()
        }

        // Set up UI listeners
        selectImageButton.setOnClickListener {
            Log.d(TAG, "Select image button clicked")
            checkAndRequestPermissions()
        }

        detectButton.setOnClickListener {
            Log.d(TAG, "Detect button clicked")

            if (selectedImageBitmap != null) {
                Log.d(TAG, "Processing selected image")
                processImage(selectedImageBitmap!!)
            } else {
                Log.d(TAG, "No image selected")
                Toast.makeText(this, "Please select an image first", Toast.LENGTH_SHORT).show()

                // For testing: Create a test bitmap
                Log.d(TAG, "Creating test bitmap for debug purposes")
                val testBitmap = Bitmap.createBitmap(500, 500, Bitmap.Config.ARGB_8888)
                imageView.setImageBitmap(testBitmap)
                processImage(testBitmap)
            }
        }

        backButton.setOnClickListener {
            Log.d(TAG, "Back button clicked")
            finish()
        }
    }

    private fun checkAndRequestPermissions() {
        Log.d(TAG, "Checking permissions...")

        // Determine which permission to request based on Android version
        val permissionToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        Log.d(TAG, "Permission to request: $permissionToRequest for SDK: ${Build.VERSION.SDK_INT}")

        // Check if we have the permission
        if (ContextCompat.checkSelfPermission(this, permissionToRequest) == PackageManager.PERMISSION_GRANTED) {
            // Permission already granted
            Log.d(TAG, "Permission already granted")
            openGallery()
        } else {
            // Request the permission
            Log.d(TAG, "Requesting permission")

            // Show permission rationale if needed
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, permissionToRequest)) {
                Toast.makeText(
                    this,
                    "Gallery access permission is needed to select images",
                    Toast.LENGTH_LONG
                ).show()
            }

            // Request the permission
            ActivityCompat.requestPermissions(
                this,
                arrayOf(permissionToRequest),
                PERMISSION_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        Log.d(TAG, "onRequestPermissionsResult: requestCode=$requestCode, grantResults=${grantResults.joinToString()}")

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted
                Log.d(TAG, "Permission granted in onRequestPermissionsResult")
                openGallery()
            } else {
                // Permission denied
                Log.d(TAG, "Permission denied in onRequestPermissionsResult")
                Toast.makeText(
                    this,
                    "Cannot access gallery without permission",
                    Toast.LENGTH_SHORT
                ).show()

                // For testing: Create a test bitmap
                val testBitmap = Bitmap.createBitmap(500, 500, Bitmap.Config.ARGB_8888)
                imageView.setImageBitmap(testBitmap)
                selectedImageBitmap = testBitmap
            }
        }
    }

    private fun openGallery() {
        try {
            Log.d(TAG, "Opening gallery")
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)

            if (intent.resolveActivity(packageManager) != null) {
                Log.d(TAG, "Starting gallery intent")
                startActivityForResult(intent, IMAGE_PICK_REQUEST)
            } else {
                Log.e(TAG, "No gallery app found")
                Toast.makeText(this, "No gallery app found on device", Toast.LENGTH_LONG).show()

                // For testing: Create a test bitmap
                val testBitmap = Bitmap.createBitmap(500, 500, Bitmap.Config.ARGB_8888)
                imageView.setImageBitmap(testBitmap)
                selectedImageBitmap = testBitmap
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error opening gallery: ${e.message}", e)
            Toast.makeText(this, "Error opening gallery: ${e.message}", Toast.LENGTH_SHORT).show()

            // For testing: Create a test bitmap
            val testBitmap = Bitmap.createBitmap(500, 500, Bitmap.Config.ARGB_8888)
            imageView.setImageBitmap(testBitmap)
            selectedImageBitmap = testBitmap
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        Log.d(TAG, "onActivityResult: requestCode=$requestCode, resultCode=$resultCode, data=${data != null}")

        if (requestCode == IMAGE_PICK_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            val selectedImageUri: Uri? = data.data
            Log.d(TAG, "Selected image URI: $selectedImageUri")

            try {
                selectedImageUri?.let { uri ->
                    val inputStream = contentResolver.openInputStream(uri)
                    selectedImageBitmap = BitmapFactory.decodeStream(inputStream)

                    selectedImageBitmap?.let { bitmap ->
                        Log.d(TAG, "Loaded bitmap: ${bitmap.width} x ${bitmap.height}")
                        imageView.setImageBitmap(bitmap)

                        // Clear previous results
                        detectionOverlay.setDetectionResults(emptyList())
                    } ?: run {
                        Log.e(TAG, "Failed to decode bitmap from stream")
                        Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading image: ${e.message}", e)
                Toast.makeText(this, "Failed to load image: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun processImage(bitmap: Bitmap) {
        try {
            Log.d(TAG, "Processing image: ${bitmap.width} x ${bitmap.height}")

            // Check if detector is initialized
            if (objectDetector == null) {
                Log.e(TAG, "Object detector is null!")
                Toast.makeText(this, "Detector not initialized, creating test detections", Toast.LENGTH_SHORT).show()

                // Create test detections
                val results = createTestDetections(bitmap)
                showDetections(bitmap, results)
                return
            }

            // Process the image with our detector
            val results = objectDetector?.detectObjects(bitmap) ?: emptyList()
            showDetections(bitmap, results)

        } catch (e: Exception) {
            Log.e(TAG, "Error processing image: ${e.message}", e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()

            // Show test detections as fallback
            val results = createTestDetections(bitmap)
            showDetections(bitmap, results)
        }
    }

    private fun showDetections(bitmap: Bitmap, results: List<ObjectDetector.DetectionResult>) {
        try {
            // Log results
            Log.d(TAG, "Detection results count: ${results.size}")
            for (result in results) {
                Log.d(TAG, "Detected: ${result.label} (${result.confidence}) at ${result.boundingBox}")
            }

            // Make sure the overlay is visible
            detectionOverlay.visibility = View.VISIBLE

            // Check imageView dimensions
            val viewWidth = imageView.width.toFloat()
            val viewHeight = imageView.height.toFloat()
            Log.d(TAG, "ImageView dimensions: $viewWidth x $viewHeight")

            if (viewWidth <= 0 || viewHeight <= 0) {
                Log.e(TAG, "ImageView has zero dimensions, using default scale")

                // Use a default scale factor
                detectionOverlay.setDetectionResults(results, 1.0f, 0f, 0f)
                return
            }

            val imageWidth = bitmap.width.toFloat()
            val imageHeight = bitmap.height.toFloat()

            // Calculate scale factor
            val scaleFactor = if (imageWidth / imageHeight > viewWidth / viewHeight) {
                // Image is wider, so it's scaled to fit width
                viewWidth / imageWidth
            } else {
                // Image is taller, so it's scaled to fit height
                viewHeight / imageHeight
            }

            Log.d(TAG, "Scale factor: $scaleFactor")

            // Calculate offset for centering
            val offsetX = (viewWidth - (imageWidth * scaleFactor)) / 2.0f
            val offsetY = (viewHeight - (imageHeight * scaleFactor)) / 2.0f
            Log.d(TAG, "Offsets: $offsetX, $offsetY")

            // Update the overlay
            detectionOverlay.setDetectionResults(results, scaleFactor, offsetX, offsetY)
            detectionOverlay.invalidate() // Force redraw

            // Show toast with results
            if (results.isEmpty()) {
                Toast.makeText(this, "No objects detected", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Detected ${results.size} objects", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error showing detections: ${e.message}", e)
        }
    }

    private fun createTestDetections(bitmap: Bitmap): List<ObjectDetector.DetectionResult> {
        val results = mutableListOf<ObjectDetector.DetectionResult>()

        // Create 3 test detections
        for (i in 0 until 3) {
            val left = (0.2f + i * 0.2f) * bitmap.width
            val top = (0.2f + i * 0.15f) * bitmap.height
            val right = left + 0.2f * bitmap.width
            val bottom = top + 0.2f * bitmap.height

            val boundingBox = RectF(left, top, right, bottom)
            val label = "Test_Object ${i+1}"
            val confidence = 0.9f - (i * 0.1f)

            results.add(ObjectDetector.DetectionResult(boundingBox, label, confidence))
        }

        Log.d(TAG, "Created ${results.size} test detections")
        return results
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy called")
        objectDetector?.close()
    }
}