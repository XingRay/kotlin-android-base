package com.github.xingray.kotlinandroidbase.ext

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.util.Log

private val TAG = "CanvasExtension"

fun Canvas.draw2dPoints(
    points: FloatArray,
    width: Int,
    height: Int,
    paint: Paint,
    colors: IntArray
) {
    for (i in 0 until points.size / 2) {
        val x = points[i * 2 + 0]
        val y = points[i * 2 + 1]
//        Log.d(TAG, "${x}, ${y},")
        paint.color = colors[i % colors.size]
        this.drawPoint(x * width, y * height, paint)
    }
}

fun Canvas.draw2dPoints(
    points: FloatArray,
    width: Int,
    height: Int,
    paint: Paint
) {
    for (i in 0 until points.size / 2) {
        val x = points[i * 2 + 0]
        val y = points[i * 2 + 1]
//        Log.d(TAG, "${x}, ${y},")
        this.drawPoint(x * width, y * height, paint)
    }
}

fun Canvas.drawBitmap(bitmap: Bitmap, paint: Paint) {
    this.drawBitmap(bitmap, 0.0f, 0.0f, paint)
}