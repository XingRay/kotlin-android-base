package com.github.xingray.kotlinandroidbase.ext

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.view.View

// 判断点是否在控件范围内
fun View.containsPoint(x: Float, y: Float): Boolean {
    val scaleIconRect = Rect()
    this.getHitRect(scaleIconRect)
    return scaleIconRect.contains(x.toInt(), y.toInt())
}

// 将指定子控件导出为 Bitmap
fun List<View>.exportAsBitmap(width: Int, height: Int, backgroundColor: Int = Color.TRANSPARENT): Bitmap {
    val outputBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val outputCanvas = Canvas(outputBitmap)
    outputCanvas.drawColor(backgroundColor)

    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    forEach {
        canvas.drawColor(backgroundColor)
        it.draw(canvas)
        outputCanvas.drawBitmap(bitmap, it.x, it.y, Paint())
    }

    return outputBitmap
}

fun View.show() {
    if (visibility != View.VISIBLE) {
        visibility = View.VISIBLE
    }
}

fun View.hide() {
    if (visibility != View.INVISIBLE) {
        visibility = View.INVISIBLE
    }
}

fun View.gone() {
    if (visibility != View.GONE) {
        visibility = View.GONE
    }
}