package com.github.xingray.kotlinandroidbase.camera

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.graphics.ImageFormat
import android.graphics.PixelFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.Image
import android.os.Build
import android.util.Log
import android.util.Size
import android.view.Surface
import com.github.xingray.kotlinandroidbase.camera.camera1.Camera1
import com.github.xingray.kotlinandroidbase.camera.camera2.Camera2
import com.github.xingray.kotlinandroidbase.camera.camerax.CameraX
import com.github.xingray.kotlinandroidbase.ext.getCameraManager
import java.util.Collections


private val TAG = "CameraHelper"

fun Context.autoFindCamera(): Camera? {
    val cameraApi: CameraApi = if (supportCamera2()) {
        CameraApi.API_2
    } else {
        CameraApi.API_1
    }
    var camera = findCamera(LensFacing.FRONT, cameraApi)
    camera?.let {
        return it
    }
    camera = findCamera(LensFacing.BACK, cameraApi)
    camera?.let {
        return it
    }

    camera = findCamera(LensFacing.EXTERNAL, cameraApi)
    camera?.let {
        return it
    }

    return null
}

fun Context.autoFindCamera(cameraApi: CameraApi): Camera? {
    var camera = findCamera(LensFacing.FRONT, cameraApi)
    camera?.let {
        return it
    }
    camera = findCamera(LensFacing.BACK, cameraApi)
    camera?.let {
        return it
    }

    camera = findCamera(LensFacing.EXTERNAL, cameraApi)
    camera?.let {
        return it
    }

    return null
}

fun Context.autoFindCamera(lensFacing: LensFacing): Camera? {
    val cameraApi: CameraApi = if (supportCamera2()) {
        CameraApi.API_2
    } else {
        CameraApi.API_1
    }
    val camera = findCamera(lensFacing, cameraApi)
    camera?.let {
        return it
    }
    return null
}


fun Context.findCamera(lensFacing: LensFacing, cameraApi: CameraApi = CameraApi.API_2): Camera? {
    when (cameraApi) {
        CameraApi.API_1 -> {
            return findCamera1(lensFacing)
        }

        CameraApi.API_2 -> {
            return findCamera2(this, lensFacing)
        }

        CameraApi.API_X -> {
            return findCameraX(lensFacing)
        }

        else -> {
            throw IllegalArgumentException("unknown cameraApi:$cameraApi")
        }
    }
}


@Suppress("DEPRECATION")
private fun findCamera1(lensFacing: LensFacing): Camera? {
    // 获取摄像头个数
    val numberOfCameras = android.hardware.Camera.getNumberOfCameras()
    val facing = lensFacing.toCamera1CameraFacing()
    for (cameraId in 0 until numberOfCameras) {
        val cameraInfo = android.hardware.Camera.CameraInfo()
        android.hardware.Camera.getCameraInfo(cameraId, cameraInfo)
        if (cameraInfo.facing == facing) {
            return Camera1(cameraId, lensFacing, CameraApi.API_1)
        }
    }
    return null
}

private fun findCamera2(context: Context, lensFacing: LensFacing): Camera? {
    val cameraManager = context.getCameraManager()
    val pair = findCameraIdByLensFacing(cameraManager, lensFacing)
    if (pair == null) {
        Log.w(TAG, "can not find camera by LensFacing:${lensFacing}")
        return null
    }
    val cameraId = pair.first
    Log.d(TAG, "findCamera: cameraId:${cameraId}")

    val cameraCharacteristics = pair.second
    Log.d(TAG, "openCamera: cameraCharacteristics:$cameraCharacteristics")
//            val fpsRanges =
//                cameraCharacteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES)
    val configurationMap = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
    val sizes = configurationMap?.getOutputSizes(ImageFormat.YUV_420_888)

    Log.d(TAG, "openCamera: sizes:${sizes.contentToString()}")

    return Camera2(cameraManager, cameraId, lensFacing, CameraApi.API_2, cameraCharacteristics)
}

private fun findCameraX(lensFacing: LensFacing): Camera? {
    return CameraX("camera_x", lensFacing, CameraApi.API_X)
}

private fun findCameraIdByLensFacing(
    cameraManager: CameraManager, lensFacing: LensFacing
): Pair<String, CameraCharacteristics>? {
    for (cameraId in cameraManager.cameraIdList) {
        val cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId)
        val lensFacingValue = cameraCharacteristics.get(CameraCharacteristics.LENS_FACING)
        if (lensFacingValue == lensFacing.toCamera2MataDataValue()) {
            return Pair(cameraId, cameraCharacteristics)
        }
    }
    return null
}


/**
 * 判断能否使用Camera2 的API
 * @param context
 * @return
 */
@SuppressLint("ObsoleteSdkInt")
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
fun Context.supportCamera2(): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
        return false
    }
    try {
        val manager = (getSystemService(Context.CAMERA_SERVICE) as CameraManager)
        val cameraIdList = manager.cameraIdList
        if (cameraIdList.isEmpty()) {
            return false
        }

        for (cameraId in cameraIdList) {
            if (cameraId == null || cameraId.trim { it <= ' ' }.isEmpty()) {
                return false
            }
            val characteristics = manager.getCameraCharacteristics(cameraId)
            val iSupportLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
            if (iSupportLevel != null && (iSupportLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY || iSupportLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED)) {
                return false
            }
        }
        return true
    } catch (e: Exception) {
        e.printStackTrace()
        Log.e(TAG, "supportCamera2: ${e.message}")
        return false
    }
}

/**
 * 计算最完美的Size
 * @param sizes
 * @param expectWidth
 * @param expectHeight
 * @return
 */
fun List<Size>.selectSize(
    expectWidth: Int, expectHeight: Int, cameraPreviewSizeSelectStrategy: CameraPreviewSizeSelectStrategy, rotate: Boolean = false
): Size {

    // 返回完全匹配的
    if (rotate) {
        // 需要旋转的， 宽高比较时互换
        for (size in this) {
            if (size.width == expectHeight && size.height == expectWidth) {
                return size
            }
        }
    } else {
        for (size in this) {
            if (size.width == expectWidth && size.height == expectHeight) {
                return size
            }
        }
    }


    // 根据当前期望的宽高判定
    val bigEnough: MutableList<Size> = ArrayList()
    val noBigEnough: MutableList<Size> = ArrayList()
    for (size in this) {
        if (size.height * expectWidth == size.width * expectHeight) {
            if (size.width > expectWidth && size.height > expectHeight) {
                bigEnough.add(size)
            } else {
                noBigEnough.add(size)
            }
        }
    }
    // 根据计算类型判断怎么如何计算尺寸
    var perfectSize: Size? = null
    when (cameraPreviewSizeSelectStrategy) {
        CameraPreviewSizeSelectStrategy.Min ->                 // 不大于期望值的分辨率列表有可能为空或者只有一个的情况，
            // Collections.min会因越界报NoSuchElementException
            if (noBigEnough.size > 1) {
                perfectSize = Collections.min(noBigEnough, SizeComparator())
            } else if (noBigEnough.size == 1) {
                perfectSize = noBigEnough[0]
            }

        CameraPreviewSizeSelectStrategy.Max ->                 // 如果bigEnough只有一个元素，使用Collections.max就会因越界报NoSuchElementException
            // 因此，当只有一个元素时，直接使用该元素
            if (bigEnough.size > 1) {
                perfectSize = Collections.max(bigEnough, SizeComparator())
            } else if (bigEnough.size == 1) {
                perfectSize = bigEnough[0]
            }

        CameraPreviewSizeSelectStrategy.Smaller ->                 // 优先查找比期望尺寸小一点的，否则找大一点的，接受范围在0.8左右
            if (noBigEnough.size > 0) {
                val size = Collections.max(noBigEnough, SizeComparator())
                if (size.width.toFloat() / expectWidth >= 0.8 && size.height.toFloat() / expectHeight > 0.8) {
                    perfectSize = size
                }
            } else if (bigEnough.size > 0) {
                val size = Collections.min(bigEnough, SizeComparator())
                if (expectWidth.toFloat() / size.width >= 0.8 && (expectHeight / size.height).toFloat() >= 0.8) {
                    perfectSize = size
                }
            }

        CameraPreviewSizeSelectStrategy.Larger ->                 // 优先查找比期望尺寸大一点的，否则找小一点的，接受范围在0.8左右
            if (bigEnough.size > 0) {
                val size = Collections.min(bigEnough, SizeComparator())
                if (expectWidth.toFloat() / size.width >= 0.8 && (expectHeight / size.height).toFloat() >= 0.8) {
                    perfectSize = size
                }
            } else if (noBigEnough.size > 0) {
                val size = Collections.max(noBigEnough, SizeComparator())
                if (size.width.toFloat() / expectWidth >= 0.8 && size.height.toFloat() / expectHeight > 0.8) {
                    perfectSize = size
                }
            }
    }
    // 如果经过前面的步骤没找到合适的尺寸，则计算最接近expectWidth * expectHeight的值
    if (perfectSize != null) {
        return perfectSize
    }

    var result = this[0]
    var widthOrHeight = false // 判断存在宽或高相等的Size
    val currentRatio: Float = expectWidth.toFloat() / expectHeight.toFloat()

    // 辗转计算宽高最接近的值
    for (size in this) {
        // 如果宽高相等，则直接返回
        if (size.width == expectWidth && size.height == expectHeight && size.height.toFloat() / size.width.toFloat() == currentRatio) {
            result = size
            break
        }
        // 仅仅是宽度相等，计算高度最接近的size
        if (size.width == expectWidth) {
            widthOrHeight = true
            if (Math.abs(result.height - expectHeight) > Math.abs(size.height - expectHeight) && size.height.toFloat() / size.width.toFloat() == currentRatio) {
                result = size
                break
            }
        } else if (size.height == expectHeight) {
            widthOrHeight = true
            if (Math.abs(result.width - expectWidth) > Math.abs(size.width - expectWidth) && size.height.toFloat() / size.width.toFloat() == currentRatio) {
                result = size
                break
            }
        } else if (!widthOrHeight) {
            if (Math.abs(result.width - expectWidth) > Math.abs(size.width - expectWidth) && Math.abs(result.height - expectHeight) > Math.abs(
                    size.height - expectHeight
                ) && size.height.toFloat() / size.width.toFloat() == currentRatio
            ) {
                result = size
            }
        }
    }
    perfectSize = result

    return perfectSize
}

/**
 * 分辨率由小到大排序
 */
fun List<Size>.sortAscend(): List<Size> {
    Collections.sort(this, SizeComparator())
    return this
}

/**
 * 分辨率由大到小排序
 */
fun List<Size>.sortDescend(): List<Size> {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        Collections.sort(this, SizeComparator().reversed())
    } else {
        Collections.sort(this, SizeComparator())
        reversed()
    }
    return this
}

fun calculateCameraPreviewOrientation(activityRotation: Int, cameraOrientation: Int, lensFacing: LensFacing): Int {
    Log.d(TAG, "calculateCameraPreviewOrientation: activityRotation:$activityRotation, cameraOrientation:$cameraOrientation, lensFacing:$lensFacing")
    var degrees = 0
    when (activityRotation) {
        Surface.ROTATION_0 -> degrees = 0
        Surface.ROTATION_90 -> degrees = 90
        Surface.ROTATION_180 -> degrees = 180
        Surface.ROTATION_270 -> degrees = 270
    }
    var result = 0
    when (lensFacing) {
        LensFacing.FRONT -> {
            result = (cameraOrientation + degrees) % 360
            result = (360 - result) % 360
        }

        LensFacing.BACK -> {
            result = (cameraOrientation - degrees + 360) % 360
        }

        LensFacing.EXTERNAL -> {
            throw UnsupportedOperationException("LensFacing.EXTERNAL is not yet supported")
        }
    }
    return result
}

fun Activity.getDisplayRotation(): Int {
    val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        @Suppress("DEPRECATION")
        this.display ?: this.getWindowManager().defaultDisplay
    } else {
        @Suppress("DEPRECATION")
        this.getWindowManager().defaultDisplay
    }
    return display.getRotation()
}

fun <T> List<T>.contentToString(stringMapper: ((T) -> String)? = null): String {
    val stringBuilder = StringBuilder()
    stringBuilder.append('[')
    for ((index, item) in this.withIndex()) {
        if (index > 0) {
            stringBuilder.append(',').append(' ')
        }
        stringBuilder.append(stringMapper?.invoke(item) ?: item.toString())
    }
    stringBuilder.append(']')
    return stringBuilder.toString()
}

fun <T> List<T>.contentToStringWithBuilder(stringMapper: ((T, StringBuilder) -> Unit)? = null): String {
    val stringBuilder = StringBuilder()
    stringBuilder.append('[')
    for ((index, item) in this.withIndex()) {
        if (index > 0) {
            stringBuilder.append(',').append(' ')
        }
        stringMapper?.invoke(item, stringBuilder) ?: stringBuilder.append(item.toString())
    }
    stringBuilder.append(']')
    return stringBuilder.toString()
}

fun Int.toImageFormat(): String {
    return when (this) {
        ImageFormat.UNKNOWN -> "ImageFormat.UNKNOWN"
        PixelFormat.RGBA_8888 -> "PixelFormat.RGBA_8888"
        PixelFormat.RGBX_8888 -> "PixelFormat.RGBX_8888"
        PixelFormat.RGB_888 -> "PixelFormat.RGB_888"
        ImageFormat.RGB_565 -> "ImageFormat.RGB_565"
        ImageFormat.YV12 -> "ImageFormat.YV12"
        ImageFormat.Y8 -> "ImageFormat.Y8"
        ImageFormat.YCBCR_P010 -> "ImageFormat.YCBCR_P010"
        ImageFormat.NV16 -> "ImageFormat.NV16"
        ImageFormat.NV21 -> "ImageFormat.NV21"
        ImageFormat.YUY2 -> "ImageFormat.YUY2"
        ImageFormat.JPEG -> "ImageFormat.JPEG"
        ImageFormat.DEPTH_JPEG -> "ImageFormat.DEPTH_JPEG"
        ImageFormat.YUV_420_888 -> "ImageFormat.YUV_420_888"
        ImageFormat.YUV_422_888 -> "ImageFormat.YUV_422_888"
        ImageFormat.YUV_444_888 -> "ImageFormat.YUV_444_888"
        ImageFormat.FLEX_RGB_888 -> "ImageFormat.FLEX_RGB_888"
        ImageFormat.FLEX_RGBA_8888 -> "ImageFormat.FLEX_RGBA_8888"
        ImageFormat.RAW_SENSOR -> "ImageFormat.RAW_SENSOR"
        ImageFormat.RAW_PRIVATE -> "ImageFormat.RAW_PRIVATE"
        ImageFormat.RAW10 -> "ImageFormat.RAW10"
        ImageFormat.RAW12 -> "ImageFormat.RAW12"
        ImageFormat.DEPTH16 -> "ImageFormat.DEPTH16"
        ImageFormat.DEPTH_POINT_CLOUD -> "ImageFormat.DEPTH_POINT_CLOUD"
        ImageFormat.PRIVATE -> "ImageFormat.PRIVATE"
        ImageFormat.HEIC -> "ImageFormat.HEIC"
        else -> "unknown format, value: ${this}"
    }
}

fun imageToCameraImage(image: Image, rotate: Int = 0, mirror: Boolean = false): CameraImage {
    val data = ImageUtil.YUV_420_888toNV21(image)
    return CameraImage(Format.NV21, image.width, image.height, data, rotate, mirror)
}

fun imageToCameraImage(image: Image, rotate: Int = 0, mirror: Boolean = false, cameraImage: CameraImage?): CameraImage {
    if (cameraImage == null || !cameraImage.isSameAttributes(Format.NV21, image.width, image.height, rotate, mirror)) {
        val data = ImageUtil.YUV_420_888toNV21(image)
        return CameraImage(Format.NV21, image.width, image.height, data, rotate, mirror)
    }

    cameraImage.update(image)
    return cameraImage
}

fun CameraImage.isSameAttributes(
    format: Format,
    width: Int,
    height: Int,
    rotate: Int,
    mirror: Boolean,
): Boolean {
    return this.format == format
            && this.width == width
            && this.height == height
            && this.rotate == rotate
            && this.mirror == mirror
}

fun CameraImage.update(image: Image) {
    ImageUtil.YUV_420_888toNV21(image, this.data)
}