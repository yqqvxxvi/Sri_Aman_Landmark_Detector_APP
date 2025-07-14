package com.example.landmarkdetector.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color


import androidx.camera.core.ImageProxy
import java.nio.ByteBuffer

object ImageUtils {
    fun imageProxyToByteArray(imageProxy: ImageProxy): ByteArray {
        val yBuffer = imageProxy.planes[0].buffer
        val uBuffer = imageProxy.planes[1].buffer
        val vBuffer = imageProxy.planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)

        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        return nv21
    }

    fun letterbox(bitmap: Bitmap, targetSize: Int): Bitmap {
        val ratio = minOf(
            targetSize.toFloat() / bitmap.width,
            targetSize.toFloat() / bitmap.height
        )

        val newWidth = (bitmap.width * ratio).toInt()
        val newHeight = (bitmap.height * ratio).toInt()
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)

        val paddedBitmap = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(paddedBitmap)
        val dx = (targetSize - newWidth) / 2f
        val dy = (targetSize - newHeight) / 2f
        canvas.drawColor(Color.BLACK)
        canvas.drawBitmap(resizedBitmap, dx, dy, null)

        return paddedBitmap
    }
}