package com.example.capstone

import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import androidx.camera.core.ImageProxy
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer


fun imageProxyToBitmap(image: ImageProxy): Bitmap {
    val nv21 = yuv420888ToNv21(image)
    val yuvImage = YuvImage(
        nv21,
        ImageFormat.NV21,
        image.width,
        image.height,
        null
    )
    val out = ByteArrayOutputStream()
    yuvImage.compressToJpeg(Rect(0, 0, image.width, image.height), 100, out)
    val jpegBytes = out.toByteArray()
    return android.graphics.BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size)
}


private fun yuv420888ToNv21(image: ImageProxy): ByteArray {
    val yBuffer: ByteBuffer = image.planes[0].buffer
    val uBuffer: ByteBuffer = image.planes[1].buffer
    val vBuffer: ByteBuffer = image.planes[2].buffer

    val ySize = yBuffer.remaining()
    val uSize = uBuffer.remaining()
    val vSize = vBuffer.remaining()

    val nv21 = ByteArray(ySize + uSize + vSize)

    // Copy Y data
    yBuffer.get(nv21, 0, ySize)

    // V and U are swapped
    val uv = ByteArray(uSize + vSize)
    vBuffer.get(uv, 0, vSize)
    uBuffer.get(uv, vSize, uSize)

    System.arraycopy(uv, 0, nv21, ySize, uv.size)

    return nv21
}
