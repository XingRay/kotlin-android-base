package com.github.xingray.kotlinandroidbase.camera

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import java.io.File


private val TAG = "Photo"

data class Photo(var data: ByteArray, var width: Int, var height: Int, var format: PhotoFormat, var rotate: Int, var mirror: Boolean)

enum class PhotoFormat {
    JPEG, PNG
}

fun Photo.saveAs(file: File) {
    val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)

    // Rotate the bitmap if necessary
    val matrix = Matrix()
    matrix.postRotate(360.0f - rotate.toFloat())
    if (mirror) {
        matrix.preScale(-1.0f, 1.0f)
    }

    val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)

    val outputStream = file.outputStream()
    outputStream.use {
        rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
    }
}