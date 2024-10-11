package com.github.xingray.kotlinandroidbase.camera

import android.media.Image

data class YuvImage(
    val width: Int,
    val height: Int,

    val yBuffer: ByteArray,
    val yPixelStride: Int,
    val yRowStride: Int,

    val uBuffer: ByteArray,
    val uPixelStride: Int,
    val uRowStride: Int,

    val vBuffer: ByteArray,
    val vPixelStride: Int,
    val vRowStride: Int,

    )

fun Image.toYuvImage(): YuvImage {
    val image = this

    val yBuffer = image.planes[0].buffer
    val uBuffer = image.planes[1].buffer
    val vBuffer = image.planes[2].buffer
    val width = image.width
    val height = image.height
    val yPixelStride = image.planes[0].pixelStride
    val yRowStride = image.planes[0].rowStride
    val uPixelStride = image.planes[1].pixelStride
    val uRowStride = image.planes[1].rowStride
    val vPixelStride = image.planes[2].pixelStride
    val vRowStride = image.planes[2].rowStride

    val yByteArray = ByteArray(height * yRowStride)
    yBuffer.get(yByteArray)

    val uByteArray = ByteArray(height * uRowStride)
    uBuffer.get(uByteArray)

    val vByteArray = ByteArray(height * vRowStride)
    vBuffer.get(vByteArray)

    return YuvImage(
        width, height,
        yByteArray, yPixelStride, yRowStride,
        uByteArray, uPixelStride, uRowStride,
        vByteArray, vPixelStride, vRowStride
    )
}