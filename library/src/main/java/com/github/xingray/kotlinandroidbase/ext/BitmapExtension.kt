package com.github.xingray.kotlinandroidbase.ext

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.util.Log
import androidx.annotation.ColorInt
import androidx.core.graphics.alpha
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.math.ceil
import kotlin.math.floor

private val TAG = "BitmapExtension"

fun Bitmap.merge(bitmap: Bitmap): Bitmap {
    val output = this.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(output)
    canvas.drawBitmap(bitmap, 0.0f, 0.0f, Paint())
    return output
}

fun Bitmap.newEmpty(): Bitmap {
    return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
}

fun Bitmap.setAllPixels(@ColorInt pixels: IntArray, width: Int, height: Int) {
    this.setPixels(pixels, 0, width, 0, 0, width, height)
}

fun Bitmap.saveAs(saveFile: File) {
    FileOutputStream(saveFile).use {
        this.compress(Bitmap.CompressFormat.PNG, 100, it)
        it.flush()
    }
}

fun Bitmap.newClip(rect: Rect): Bitmap {
    return newClip(rect.left, rect.top, rect.width(), rect.height())
}

fun Bitmap.newClip(x: Int, y: Int, width: Int, height: Int): Bitmap {
    return Bitmap.createBitmap(this, x, y, width, height)
}

fun Bitmap.newScaleToFit(targetWidth: Int, targetHeight: Int): Bitmap {
    val matrix = calcMatrixScaleToFit(targetWidth, targetHeight)
    return newApplyMatrix(targetWidth, targetHeight, matrix)
}

fun Bitmap.newApplyMatrix(targetWidth: Int, targetHeight: Int, matrix: Matrix): Bitmap {
    // 创建目标大小的 Bitmap，并将缩放后的 Bitmap 居中绘制在其中
    val resultBitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(resultBitmap)
    canvas.drawBitmap(this, matrix, null)

    return resultBitmap
}

fun Bitmap.calcMatrixScaleToFit(targetWidth: Int, targetHeight: Int): Matrix {
    return calcMatrixScaleToFit(this.width, this.height, targetWidth, targetHeight)
}

fun calcMatrixScaleToFit(srcWidth: Int, srcHeight: Int, targetWidth: Int, targetHeight: Int): Matrix {
    // 计算目标尺寸与原始尺寸的宽高比
    val targetRatio = targetWidth.toFloat() / targetHeight.toFloat()
    val srcRatio = srcWidth.toFloat() / srcHeight.toFloat()
    Log.d(TAG, "Bitmap.newScaleToFit: targetRatio:${targetRatio}, srcRatio:${srcRatio}")

    // 计算实际需要读取的图片尺寸
    var actualWidth = targetWidth
    var actualHeight = targetHeight
    if (actualWidth * targetHeight != actualHeight * targetWidth) {
        if (srcRatio > targetRatio) {
            actualHeight = (targetWidth.toFloat() / srcRatio).toInt()
        } else {
            actualWidth = (targetHeight.toFloat() * srcRatio).toInt()
        }
    }
    Log.d(TAG, "Bitmap.newScaleToFit: actualWidth:${actualWidth}, actualHeight:${actualHeight}")

    val scale = calculateScaleMin(srcWidth, srcHeight, actualWidth, actualHeight)
    Log.d(TAG, "readBitmap: scale:${scale}")

    val matrix = Matrix()
    matrix.postTranslate(-srcWidth * 0.5f, -srcHeight * 0.5f)
    matrix.postScale(scale, scale)
    matrix.postTranslate(targetWidth * 0.5f, targetHeight * 0.5f)

    return matrix
}

fun calculateInSampleSize(srcWidth: Int, srcHeight: Int, targetWidth: Int, targetHeight: Int): Int {
    var inSampleSize = 1
    if (srcHeight > targetHeight || srcWidth > targetWidth) {
        val heightRatio = (srcHeight.toFloat() / targetHeight).toInt()
        val widthRatio = (srcWidth.toFloat() / targetWidth).toInt()
        inSampleSize = if (heightRatio < widthRatio) heightRatio else widthRatio
    }
    return inSampleSize
}

fun calculateScaleMin(srcWidth: Int, srcHeight: Int, targetWidth: Int, targetHeight: Int): Float {
    Log.d(TAG, "calculateScaleMin: srcWidth:${srcWidth}, srcHeight:${srcHeight}, targetWidth:${targetWidth}, targetHeight:${targetHeight}")
    val heightRatio = targetHeight.toFloat() / srcHeight.toFloat()
    val widthRatio = targetWidth.toFloat() / srcWidth.toFloat()
    Log.d(TAG, "calculateScaleMin: heightRatio:${heightRatio}, widthRatio:${widthRatio}")
    return Math.min(heightRatio, widthRatio)
}

fun calculateScaleMax(srcWidth: Int, srcHeight: Int, targetWidth: Int, targetHeight: Int): Float {
    Log.d(TAG, "calculateScaleMax: srcWidth:${srcWidth}, srcHeight:${srcHeight}, targetWidth:${targetWidth}, targetHeight:${targetHeight}")
    val heightRatio = targetHeight.toFloat() / srcHeight.toFloat()
    val widthRatio = targetWidth.toFloat() / srcWidth.toFloat()
    Log.d(TAG, "calculateScaleMax: heightRatio:${heightRatio}, widthRatio:${widthRatio}")
    return Math.max(heightRatio, widthRatio)
}

fun Bitmap.toPngByteArray(): ByteArray {
    ByteArrayOutputStream().use { byteArrayOutputStream ->
        this.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
        return byteArrayOutputStream.toByteArray()
    }
}

// 裁剪 Bitmap，仅保留不透明的部分，并记录裁剪部分在原始图片中的位置及大小
fun Bitmap.crop(): Triple<Bitmap, Int, Int> {
    val bitmap = this
    val width = bitmap.width
    val height = bitmap.height

    var top = -1
    var bottom = -1
    var left = -1
    var right = -1

    // 找到不透明区域的边界
    for (y in 0 until height) {
        for (x in 0 until width) {
            val alpha = bitmap.getPixel(x, y).alpha
            if (alpha != 0) { // 如果像素不透明
                if (left == -1 || x < left) {
                    left = x
                }
                if (top == -1 || y < top) {
                    top = y
                }
                if (right == -1 || x > right) {
                    right = x
                }
                if (bottom == -1 || y > bottom) {
                    bottom = y
                }
            }
        }
    }

    // 计算裁剪后的矩形宽度和高度
    val cropWidth = right - left + 1
    val cropHeight = bottom - top + 1

    // 创建一个新的 Bitmap 作为裁剪结果
    val croppedBitmap = Bitmap.createBitmap(cropWidth, cropHeight, Bitmap.Config.ARGB_8888)

    // 创建一个 Canvas，并在其上绘制裁剪结果
    val canvas = Canvas(croppedBitmap)
    val srcRect = Rect(left, top, right + 1, bottom + 1) // 注意边界的处理
    val destRect = Rect(0, 0, cropWidth, cropHeight)
    canvas.drawBitmap(bitmap, srcRect, destRect, null)

    return Triple(croppedBitmap, left, top)
}


fun Bitmap.changeColor(color: Int, weight: Double): Bitmap {
    val width = width
    val height = height

    val pixels = IntArray(width * height)
    getPixels(pixels, 0, width, 0, 0, width, height)

    val newPixels = IntArray(width * height)
    handlePixels(pixels, newPixels, width, height, color, weight)

    val newBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    newBitmap.setAllPixels(newPixels, width, height)
    return newBitmap
}

data class Pixel(val alpha: Int, val red: Int, val green: Int, val blue: Int)

fun Int.toPixel(): Pixel {
    return Pixel((this shr 24) and 0xFF, (this shr 16) and 0xFF, (this shr 8) and 0xFF, this and 0xFF)
}

fun IntArray.getPixel(row: Int, col: Int, width: Int): Pixel {
    return (this[row * width + col]).toPixel()
}

private fun handlePixels(pixels: IntArray, newPixels: IntArray, width: Int, height: Int, color: Int, weight: Double) {
    for (y in 0 until height) {
        for (x in 0 until width) {
            handlePixel(pixels, newPixels, x, y, width, height, color, weight)
        }
    }
}


private fun handlePixel(pixels: IntArray, newPixels: IntArray, x: Int, y: Int, width: Int, height: Int, color: Int, weight: Double) {
    newPixels[y * width + x] = blendColor(pixels[y * width + x], 1.0, color, weight)
}

fun blendColor(color0: Int, weight0: Double, color1: Int, weight1: Double): Int {

    val alpha0 = (color0 shr 24) and 0xFF
    val red0 = (color0 shr 16) and 0xFF
    val green0 = (color0 shr 8) and 0xFF
    val blue0 = color0 and 0xFF


    val alpha1 = (color1 shr 24) and 0xFF
    val red1 = (color1 shr 16) and 0xFF
    val green1 = (color1 shr 8) and 0xFF
    val blue1 = color1 and 0xFF

    if (alpha0 < 100) {
        return color0
    }
    if (red0 == 255 && green0 == 255 && blue0 == 255) {
        return color0
    }

    val hsv0 = FloatArray(3)
    Color.RGBToHSV(
        ((red0 * weight0 + red1 * weight1) / (weight0 + weight1)).toInt(),
        ((green0 * weight0 + green1 * weight1) / (weight0 + weight1)).toInt(),
        ((blue0 * weight0 + blue1 * weight1) / (weight0 + weight1)).toInt(),
        hsv0
    )


    val hsv1 = FloatArray(3)
    Color.RGBToHSV(red1, green1, blue1, hsv1)

    val blendColor = Color.HSVToColor(floatArrayOf(hsv1[0], hsv1[1], hsv0[2]))

    return changeAlpha(blendColor, alpha0);
}

// 修改颜色的alpha通道值
fun changeAlpha(color: Int, newAlpha: Int): Int {
    val originalColorWithoutAlpha = color and 0x00FFFFFF
    return (newAlpha shl 24) or originalColorWithoutAlpha
}

fun Bitmap.translate(dx: Float, dy: Float): Bitmap {
    val originalBitmap = this
    val width: Int = originalBitmap.getWidth()
    val height: Int = originalBitmap.getHeight()

    // 创建一个空的Bitmap
    val translatedBitmap = Bitmap.createBitmap(width, height, originalBitmap.getConfig())

    // 使用Canvas和Matrix进行平移
    val canvas = Canvas(translatedBitmap)
    val matrix = Matrix()
    matrix.setTranslate(dx, dy)
    canvas.drawBitmap(originalBitmap, matrix, Paint())

    return translatedBitmap
}

fun Bitmap.scaleAndMove(scaleX: Float, scaleY: Float, centerX: Float, centerY: Float): Bitmap {
    val originalBitmap = this
    val width: Int = originalBitmap.getWidth()
    val height: Int = originalBitmap.getHeight()

    val targetBitmap = Bitmap.createBitmap(width, height, originalBitmap.getConfig())
    val canvas = Canvas(targetBitmap)
    val matrix = Matrix()

    matrix.postTranslate(-width.toFloat() / 2, -height.toFloat() / 2)
    matrix.postScale(scaleX, scaleY)
    matrix.postTranslate(centerX, centerY)

    canvas.drawBitmap(originalBitmap, matrix, Paint())

    return targetBitmap
}

fun Bitmap.createMapped(pixelMapTable: DoubleArray, useLinearInterpolator: Boolean = false): Bitmap {
    val srcBitmap = this
    val pixels = IntArray(width * height)
    srcBitmap.getPixels(pixels, 0, width, 0, 0, width, height)

    val newPixels = IntArray(width * height)
    if (useLinearInterpolator) {
        handlePixelsUseLinearInterpolator(pixels, newPixels, width, height, pixelMapTable)
    } else {
        handlePixels(pixels, newPixels, width, height, pixelMapTable)
    }
    val newBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    newBitmap.setAllPixels(newPixels, width, height)

    return newBitmap
}

private fun handlePixels(pixels: IntArray, newPixels: IntArray, width: Int, height: Int, pixelMapTable: DoubleArray) {
    for (y in 0 until height) {
        for (x in 0 until width) {
            handlePixel(pixels, newPixels, x, y, width, height, pixelMapTable)
        }
    }
}


private fun handlePixel(pixels: IntArray, newPixels: IntArray, x: Int, y: Int, width: Int, height: Int, pixelMapTable: DoubleArray) {
    val offset = x + width * y
    val srcX = pixelMapTable[offset * 2 + 0]
    val srcY = pixelMapTable[offset * 2 + 1]

    // 简单取整
    val nearestSrcX = srcX.toInt()
    val nearestSrcY = srcY.toInt()
    if ((nearestSrcX in 0..<width) && (nearestSrcY in 0..<height)) {
        val srcOffset = nearestSrcX + nearestSrcY * width
        newPixels[offset] = pixels[srcOffset]
    }
}

private fun handlePixelsUseLinearInterpolator(pixels: IntArray, newPixels: IntArray, width: Int, height: Int, pixelMapTable: DoubleArray) {
    for (y in 0 until height) {
        for (x in 0 until width) {
            handlePixelUseLinearInterpolator(pixels, newPixels, x, y, width, height, pixelMapTable)
        }
    }
}

// 使用线性插值器
private fun handlePixelUseLinearInterpolator(pixels: IntArray, newPixels: IntArray, x: Int, y: Int, width: Int, height: Int, pixelMapTable: DoubleArray) {
    val offset = x + width * y
    val srcX = pixelMapTable[offset * 2 + 0]
    val srcY = pixelMapTable[offset * 2 + 1]


//         使用双线性差值
    val srcX0 = floor(srcX).toInt()
    val srcY0 = floor(srcY).toInt()
    val srcX1 = ceil(srcX).toInt()
    val srcY1 = ceil(srcY).toInt()

    Log.d(TAG, "handlePixel: srcX0:${srcX0}, srcY0:${srcY0}, srcX1:${srcX1}, srcY1:${srcY1}")


    /**
     *  p00--------p01
     *  |    |      |
     *  |----p------|
     *  |    |      |
     *  |    |      |
     *  p10--------p11
     *
     *  rx = (x - x0) / (x1 - x0)
     *  ry = (y - y0) / (y1 - y0)
     *
     *  p = ( p00 * (1-ry) + p10 * ry ) * (1 - rx) + ( p01*(1-ry) + p11*ry )*rx
     *    = (1-rx-ry+rx*ry)*p00 + rx*(1-ry)*p01 + (1-rx)*ry*p10 + rx*ry*p11
     */
    val p00 = if (srcX0 < 0 || srcX0 >= width || srcY0 < 0 || srcY0 >= height) {
        //throw IllegalStateException("srcX0:${srcX0}, srcY0:${srcY0}")
        0
    } else {
        pixels[srcY0 * width + srcX0]
    }

    val p01 = if (srcX1 < 0 || srcX1 >= width || srcY0 < 0 || srcY0 >= height) {
        //throw IllegalStateException("srcX1:${srcX1}, srcY0:${srcY0}")
        0
    } else {
        pixels[srcY0 * width + srcX1]
    }

    val p10 = if (srcX0 < 0 || srcX0 >= width || srcY1 < 0 || srcY1 >= height) {
        //throw IllegalStateException("srcX0:${srcX0}, srcY1:${srcY1}")
        0
    } else {
        pixels[srcY1 * width + srcX0]
    }

    val p11 = if (srcX1 < 0 || srcX1 >= width || srcY1 < 0 || srcY1 >= height) {
        //throw IllegalStateException("srcX1:${srcX1}, srcY1:${srcY1}")
        0
    } else {
        pixels[srcY1 * width + srcX1]
    }

    val rx: Double = if (srcX0 == srcX1) {
        0.0
    } else {
        (srcX - srcX0) / (srcX1 - srcX0)
    }
    val ry: Double = if (srcY0 == srcY1) {
        0.0
    } else {
        (srcY - srcY0) / (srcY1 - srcY0)
    }

    newPixels[offset] = blendColor(p00, (1 - rx - ry + rx * ry), p01, rx * (1 - ry), p10, (1 - rx) * ry, p11, rx * ry)
}

fun blendColor(color0: Int, weight0: Double, color1: Int, weight1: Double, color2: Int, weight2: Double, color3: Int, weight3: Double): Int {

    val alpha0 = (color0 shr 24) and 0xFF
    val red0 = (color0 shr 16) and 0xFF
    val green0 = (color0 shr 8) and 0xFF
    val blue0 = color0 and 0xFF


    val alpha1 = (color1 shr 24) and 0xFF
    val red1 = (color1 shr 16) and 0xFF
    val green1 = (color1 shr 8) and 0xFF
    val blue1 = color1 and 0xFF

    val alpha2 = (color2 shr 24) and 0xFF
    val red2 = (color2 shr 16) and 0xFF
    val green2 = (color2 shr 8) and 0xFF
    val blue2 = color2 and 0xFF

    val alpha3 = (color3 shr 24) and 0xFF
    val red3 = (color3 shr 16) and 0xFF
    val green3 = (color3 shr 8) and 0xFF
    val blue3 = color3 and 0xFF

    return Color.argb(
        (alpha0 * weight0 + alpha1 * weight1 + alpha2 * weight2 + alpha3 * weight3).toInt(),
        (red0 * weight0 + red1 * weight1 + red2 * weight2 + red3 * weight3).toInt(),
        (green0 * weight0 + green1 * weight1 + green2 * weight2 + green3 * weight3).toInt(),
        (blue0 * weight0 + blue1 * weight1 + blue2 * weight2 + blue3 * weight3).toInt()
    )
}

/**
 * 将Bitmap的上半部分进行缩放
 * @param partHeight 上半部分的高度
 * @param scale 缩放比例
 */
fun Bitmap.scaleTop(partHeight: Int, scale: Float): Bitmap {
    // 创建目标Bitmap，大小与原图相同
    val targetBitmap = Bitmap.createBitmap(width, height, config)
    val canvas = Canvas(targetBitmap)

    // 绘制未被压缩的部分（保留部分）
    val keepRect = Rect(0, partHeight, width, height)
    canvas.drawBitmap(this, keepRect, keepRect, null)

    // 绘制压缩后的部分（压缩部分）
    val scaleHeight = (partHeight * scale).toInt()
    val srcRect = Rect(0, 0, width, partHeight)
    val destRect = Rect(0, partHeight - scaleHeight, width, partHeight)

    // 将缩放后的上半部分绘制到目标Bitmap上
    canvas.drawBitmap(this, srcRect, destRect, null)

    return targetBitmap
}

fun Bitmap.scaleBottom(partHeight: Int, scale: Float): Bitmap {
    return this
}

fun Bitmap.scaleLeft(partWidth: Int, scale: Float): Bitmap {
    return this
}

fun Bitmap.scaleRight(partWidth: Int, scale: Float): Bitmap {
    return this
}