package com.github.xingray.kotlinandroidbase.ext

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import androidx.annotation.ColorRes
import androidx.annotation.RawRes
import androidx.exifinterface.media.ExifInterface
import com.github.xingray.kotlinbase.ext.io.readFloatArray
import com.github.xingray.kotlinbase.ext.io.readIntArray
import com.github.xingray.kotlinandroidbase.inputstreamprovider.FileInputStreamProvider
import com.github.xingray.kotlinandroidbase.inputstreamprovider.InputStreamProvider
import com.github.xingray.kotlinandroidbase.inputstreamprovider.ResInputStreamProvider
import com.github.xingray.kotlinandroidbase.inputstreamprovider.UriInputStreamProvider
import java.io.BufferedReader
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader

private val TAG = "ContextExtension"
fun Context.readBitmapAsSize(@RawRes id: Int): Bitmap? {
    resources.openRawResource(id).use {
        val opt = BitmapFactory.Options()
        opt.inPreferredConfig = Bitmap.Config.ARGB_8888
        opt.inMutable = true
        return BitmapFactory.decodeStream(it, null, opt) ?: run {
            Log.e(TAG, "readBitmap: bitmap is null")
            return null
        }
    }
}

fun Context.readBitmapFromAsset(path: String): Bitmap? {
    val inputStream = assets.open(path)
    inputStream.use {
        return inputStream.readBitmap()
    }
}

fun Context.readBitmap(uri: Uri): Bitmap? {
    val inputStream = contentResolver.openInputStream(uri) ?: return null
    inputStream.use {
        return inputStream.readBitmap()
    }
}

fun Context.readBitmap(path: String): Bitmap? {
    val file = File(path).takeIf { it.exists() && it.isFile } ?: return null
    file.inputStream().use { inputStream ->
        return inputStream.readBitmap()
    }
}

fun InputStream.readBitmap(): Bitmap? {
    val opt = BitmapFactory.Options()
    opt.inPreferredConfig = Bitmap.Config.ARGB_8888
    opt.inMutable = true
    return BitmapFactory.decodeStream(this, null, opt)
}


fun Context.readBitmapAsSize(uri: Uri, width: Int, height: Int): Bitmap? {
    return UriInputStreamProvider(contentResolver, uri).readBitmapAsSize(width, height)
}

fun Context.readBitmapAsSize(resId: Int, width: Int, height: Int): Bitmap? {
    return ResInputStreamProvider(resources, resId).readBitmapAsSize(width, height)
}

fun File.readBitmapAsSize(width: Int, height: Int): Bitmap? {
    return FileInputStreamProvider(this).readBitmapAsSize(width, height)
}

fun InputStreamProvider.readBitmapAsSize(width: Int, height: Int): Bitmap? {
    Log.d(TAG, "InputStreamProvider.readBitmap: width:${width}, height:${height}")

    val exifInterface = this.openInputStream()?.use { inputStream ->
        ExifInterface(inputStream)
    } ?: let {
        Log.e(TAG, "readBitmap: can not open inputStream, InputStreamProvider:${this}")
        return null
    }

    val rotation = exifInterface.rotationDegrees
    val orientation: Int = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
    val mirror = orientation == ExifInterface.ORIENTATION_TRANSVERSE || orientation == ExifInterface.ORIENTATION_FLIP_VERTICAL
            || orientation == ExifInterface.ORIENTATION_TRANSPOSE || orientation == ExifInterface.ORIENTATION_FLIP_HORIZONTAL
    val exifWidth = exifInterface.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, 0)
    val exifHeight = exifInterface.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, 0)
    Log.d(TAG, "InputStream.readBitmap: exifInterface: rotation:${rotation}, mirror:${mirror}, exifWidth:${exifWidth}, exifHeight:${exifHeight}")

    val size = if (exifWidth <= 0 || exifHeight <= 0) {
        this.openInputStream()?.use { inputStream ->
            inputStream.readImageSize()
        } ?: let {
            Log.e(TAG, "readBitmap: can not open inputStream, InputStreamProvider:${this}")
            return null
        }
    } else {
        Size(exifWidth, exifHeight)
    }

    val rotateSize = size.rotate(rotation)
    val srcWidth = rotateSize.width
    val srcHeight = rotateSize.height
    Log.d(TAG, "readBitmap: srcWidth:${srcWidth}, srcHeight:${srcHeight}")

    // 计算目标尺寸与原始尺寸的宽高比
    val targetRatio = width.toFloat() / height.toFloat()
    val srcRatio = srcWidth.toFloat() / srcHeight.toFloat()
    Log.d(TAG, "readBitmap: targetRatio:${targetRatio}, srcRatio:${srcRatio}")

    // 计算实际需要读取的图片尺寸
    var actualWidth = width
    var actualHeight = height
    if (targetRatio != srcRatio) {
        if (srcRatio > targetRatio) {
            actualHeight = (width.toFloat() / srcRatio).toInt()
        } else {
            actualWidth = (height.toFloat() * srcRatio).toInt()
        }
    }
    Log.d(TAG, "readBitmap: actualWidth:${actualWidth}, actualHeight:${actualHeight}")

    // 重新读取图片并缩放
    val options = BitmapFactory.Options()
    val sampleSize = calculateInSampleSize(srcWidth, srcHeight, actualWidth, actualHeight)
    options.inSampleSize = sampleSize
    Log.d(TAG, "readBitmap: options.inSampleSize:${options.inSampleSize}")

    val scale = calculateScaleMin(srcWidth, srcHeight, actualWidth, actualHeight) * sampleSize
    Log.d(TAG, "readBitmap: scale:${scale}")

    val readBitmap = this.openInputStream()?.use { inputStream ->
        BitmapFactory.decodeStream(inputStream, null, options) ?: return null
    } ?: let {
        Log.e(TAG, "readBitmap: can not open inputStream, InputStreamProvider:${this}")
        return null
    }
    Log.d(TAG, "readBitmap: readBitmap, width:${readBitmap.width}, height:${readBitmap.height}")

    val matrix = Matrix()
    matrix.postTranslate(-size.width * 0.5f / sampleSize, -size.height * 0.5f / sampleSize)
    matrix.postRotate(rotation.toFloat())
    if (mirror) {
        matrix.postScale(-scale, scale)
    } else {
        matrix.postScale(scale, scale)
    }
    matrix.postTranslate(width * 0.5f, height * 0.5f)

    // 创建目标大小的 Bitmap，并将缩放后的 Bitmap 居中绘制在其中
    val resultBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(resultBitmap)
    canvas.drawBitmap(readBitmap, matrix, null)

    return resultBitmap
}

fun Size.rotate(degree: Int): Size {
    return if (degree == 90 || degree == 270) {
        Size(height, width)
    } else {
        this
    }
}

fun InputStream.readImageSize(): Size {
    val inputStream = this
    // 获取原始图片尺寸
    val options = BitmapFactory.Options()
    options.inJustDecodeBounds = true
    BitmapFactory.decodeStream(inputStream, null, options)
    return Size(options.outWidth, options.outHeight)
}

//private fun calculateInSampleSize(srcWidth: Int, srcHeight: Int, targetWidth: Int, targetHeight: Int): Int {
//    var inSampleSize = 1
//    if (srcHeight > targetHeight || srcWidth > targetWidth) {
//        val heightRatio = (srcHeight.toFloat() / targetHeight).toInt()
//        val widthRatio = (srcWidth.toFloat() / targetWidth).toInt()
//        inSampleSize = if (heightRatio < widthRatio) heightRatio else widthRatio
//    }
//    return inSampleSize
//}
//
//private fun calculateScaleMin(srcWidth: Int, srcHeight: Int, targetWidth: Int, targetHeight: Int): Float {
//    Log.d(TAG, "calculateScaleMin: srcWidth:${srcWidth}, srcHeight:${srcHeight}, targetWidth:${targetWidth}, targetHeight:${targetHeight}")
//    val heightRatio = targetHeight.toFloat() / srcHeight.toFloat()
//    val widthRatio = targetWidth.toFloat() / srcWidth.toFloat()
//    Log.d(TAG, "calculateScaleMin: heightRatio:${heightRatio}, widthRatio:${widthRatio}")
//    return Math.min(heightRatio, widthRatio)
//}
//
//private fun calculateScaleMax(srcWidth: Int, srcHeight: Int, targetWidth: Int, targetHeight: Int): Float {
//    Log.d(TAG, "calculateScaleMax: srcWidth:${srcWidth}, srcHeight:${srcHeight}, targetWidth:${targetWidth}, targetHeight:${targetHeight}")
//    val heightRatio = targetHeight.toFloat() / srcHeight.toFloat()
//    val widthRatio = targetWidth.toFloat() / srcWidth.toFloat()
//    Log.d(TAG, "calculateScaleMax: heightRatio:${heightRatio}, widthRatio:${widthRatio}")
//    return Math.max(heightRatio, widthRatio)
//}

enum class ImageConvertType {
    CLIP,
    EXPAND,
    NONE
}

private fun processBitmap(
    bitmap: Bitmap,
    targetAspectRatio: Float,
    imageConvertType: ImageConvertType,
    extendColor: Int
): Bitmap {
    return when (imageConvertType) {
        ImageConvertType.CLIP -> cropToAspectRatio(bitmap, targetAspectRatio)
        ImageConvertType.EXPAND -> extendToAspectRatio(bitmap, targetAspectRatio, extendColor)
        ImageConvertType.NONE -> bitmap
    }
}

private fun cropToAspectRatio(bitmap: Bitmap, targetAspectRatio: Float): Bitmap {
    val currentAspectRatio = bitmap.width.toFloat() / bitmap.height
    val newWidth: Int
    val newHeight: Int

    if (currentAspectRatio > targetAspectRatio) {
        newHeight = bitmap.height
        newWidth = (bitmap.height * targetAspectRatio).toInt()
    } else {
        newWidth = bitmap.width
        newHeight = (bitmap.width / targetAspectRatio).toInt()
    }

    val startX = (bitmap.width - newWidth) / 2
    val startY = (bitmap.height - newHeight) / 2

    return Bitmap.createBitmap(bitmap, startX, startY, newWidth, newHeight)
}

private fun extendToAspectRatio(bitmap: Bitmap, targetAspectRatio: Float, extendColor: Int): Bitmap {
    val currentAspectRatio = bitmap.width.toFloat() / bitmap.height

    if (currentAspectRatio == targetAspectRatio) {
        return bitmap
    }

    val newWidth: Int
    val newHeight: Int

    if (currentAspectRatio > targetAspectRatio) {
        newWidth = bitmap.width
        newHeight = (bitmap.width / targetAspectRatio).toInt()
    } else {
        newHeight = bitmap.height
        newWidth = (bitmap.height * targetAspectRatio).toInt()
    }

    val startX = (newWidth - bitmap.width) / 2
    val startY = (newHeight - bitmap.height) / 2

    val extendedBitmap = Bitmap.createBitmap(newWidth, newHeight, Bitmap.Config.ARGB_8888)
    extendedBitmap.eraseColor(extendColor)

    val canvas = Canvas(extendedBitmap)
    canvas.drawBitmap(bitmap, startX.toFloat(), startY.toFloat(), null)

    return extendedBitmap
}


fun Context.readTextAsString(@RawRes id: Int): String? {
    val stringBuilder = StringBuilder()

    try {
        resources.openRawResource(id).use { inputStream ->
            val bufferedReader = inputStream.reader().buffered()
            bufferedReader.useLines { lines ->
                lines.forEach { line ->
                    stringBuilder.append(line).append('\n')
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        return null
    }

    return stringBuilder.toString()
}

@Suppress("unused")
fun Context.readTextAsLines(@RawRes id: Int): List<String>? {
    val lines = mutableListOf<String>()

    try {
        resources.openRawResource(id).use { inputStream ->
            val bufferedReader = inputStream.reader().buffered()
            bufferedReader.useLines { lines.addAll(it) }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        return null
    }

    return lines
}

fun Context.getCameraManager(): CameraManager {
    return this.getSystemService(Context.CAMERA_SERVICE) as CameraManager
}

fun Context.getColorCompat(@ColorRes id: Int): Int {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        getColor(id)
    } else {
        @Suppress("DEPRECATION")
        resources.getColor(id)
    }
}

fun Context.readIntArray(fileName: String): IntArray {
    val assetManager = assets

    // 打开指定的asset文件
    val inputStream = assetManager.open(fileName)
    val reader = BufferedReader(InputStreamReader(inputStream))
    reader.use {
        return it.readIntArray()
    }
}

fun Context.readIntArray(resourceId: Int): IntArray {
    val resources: Resources = resources
    val inputStream = resources.openRawResource(resourceId)
    val reader = BufferedReader(InputStreamReader(inputStream))
    reader.use {
        return it.readIntArray()
    }
}

fun Context.readFloatArray(fileName: String): FloatArray {
    val assetManager = assets

    // 打开指定的asset文件
    val inputStream = assetManager.open(fileName)
    val reader = BufferedReader(InputStreamReader(inputStream))
    reader.use {
        return it.readFloatArray()
    }
}

fun Context.readFloatArray(resourceId: Int): FloatArray {
    val resources: Resources = resources
    val inputStream = resources.openRawResource(resourceId)
    val reader = BufferedReader(InputStreamReader(inputStream))
    reader.use {
        return it.readFloatArray()
    }
}

fun Context.getBrotherFileList(uri: Uri): List<File>? {
    // Get the file path from the Uri
    val filePathColumn = arrayOf(MediaStore.Images.Media.DATA)
    val cursor = contentResolver.query(uri, filePathColumn, null, null, null) ?: let {
        Log.d(TAG, "getBrotherFileList: uri:${uri} can not found")
        return null
    }
    val parentFile: File = cursor.use {
        it.moveToFirst()
        val columnIndex = it.getColumnIndex(filePathColumn[0])
        val filePath = it.getString(columnIndex)
        // Get the file name without extension
        File(filePath).parentFile
    } ?: let {
        Log.d(TAG, "getBrotherFileList: parentFile not found")
        return null
    }

    return parentFile.listFiles { file ->
        file.isFile
    }?.toList()
}


fun Context.getFileNameWithoutExtension(uri: Uri): String? {
    // Get the file path from the Uri
    val filePathColumn = arrayOf(MediaStore.Images.Media.DATA)
    val cursor = contentResolver.query(uri, filePathColumn, null, null, null)
    cursor?.use {
        it.moveToFirst()
        val columnIndex = it.getColumnIndex(filePathColumn[0])
        val filePath = it.getString(columnIndex)
        // Get the file name without extension
        return File(filePath).nameWithoutExtension
    }
    return null
}

fun Context.dpToPx(dp: Int): Float {
    val displayMetrics = resources.displayMetrics
    return dp * displayMetrics.density
}