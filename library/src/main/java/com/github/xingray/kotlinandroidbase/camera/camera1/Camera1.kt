@file:Suppress("DEPRECATION")

package com.github.xingray.kotlinandroidbase.camera.camera1

import android.annotation.SuppressLint
import android.graphics.ImageFormat
import android.graphics.PixelFormat
import android.graphics.SurfaceTexture
import android.hardware.Camera.CameraInfo
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import com.github.xingray.kotlinandroidbase.camera.Camera
import com.github.xingray.kotlinandroidbase.camera.CameraApi
import com.github.xingray.kotlinandroidbase.camera.CameraImage
import com.github.xingray.kotlinandroidbase.camera.CameraLifecycle
import com.github.xingray.kotlinandroidbase.camera.CameraPreviewCallback
import com.github.xingray.kotlinandroidbase.camera.CameraPreviewSizeSelectStrategy
import com.github.xingray.kotlinandroidbase.camera.Format
import com.github.xingray.kotlinandroidbase.camera.LensFacing
import com.github.xingray.kotlinandroidbase.camera.Photo
import com.github.xingray.kotlinandroidbase.camera.PhotoFormat
import com.github.xingray.kotlinandroidbase.camera.contentToString
import com.github.xingray.kotlinandroidbase.camera.contentToStringWithBuilder
import com.github.xingray.kotlinandroidbase.camera.selectSize
import com.github.xingray.kotlinandroidbase.camera.sortDescend
import com.github.xingray.kotlinandroidbase.camera.toImageFormat

private val TAG = Camera1::class.simpleName

class Camera1(private val mCameraId: Int, private val mLensFacing: LensFacing, private val mCameraApi: CameraApi) : Camera {

    private var mCamera: android.hardware.Camera? = null

    private var mPreviewWidth = 0
    private var mPreviewHeight = 0

    private val mCameraInfo = CameraInfo()
    private var mOutputThread: HandlerThread? = null
    private var mOutputTexture: SurfaceTexture? = null

    private var mCurrentState = CameraLifecycle.CREATED
    private var mStartPreviewWhenOpen = false
    private var mCloseWhenStopPreview = false

    var autoRecycleBuffer = true

    init {
        android.hardware.Camera.getCameraInfo(mCameraId, mCameraInfo)
    }

    override val lensFacing: LensFacing
        get() = mLensFacing

    override val cameraApi: CameraApi
        get() = mCameraApi

    private var mPreviewCallback: ((CameraImage, Camera) -> Unit)? = null

    var mCameraPreviewWidth = 0
    var mCameraPreviewHeight = 0

    var mCameraPictureWidth = 0
    var mCameraPictureHeight = 0

    override fun setPreviewViewSize(width: Int, height: Int) {
        mPreviewWidth = width
        mPreviewHeight = height
    }

    override fun setPreviewCallback(callback: CameraPreviewCallback) {
        mPreviewCallback = callback::onPreviewFrame
    }

    override fun setPreviewCallback(callback: (CameraImage, Camera) -> Unit) {
        mPreviewCallback = callback
    }

    override fun open() {
        Log.d(TAG, "Camera1 open: ")
        if (mCurrentState == CameraLifecycle.CREATED || mCurrentState == CameraLifecycle.CLOSED) {
            doOpen()
            mCurrentState = CameraLifecycle.OPENED
        }
        if (mStartPreviewWhenOpen) {
            startPreview()
            mStartPreviewWhenOpen = false
        }
    }

    override fun startPreview() {
        Log.d(TAG, "Camera1 startPreview: ")
        if (mCurrentState == CameraLifecycle.OPENED) {
            Log.d(TAG, "Camera1 doStartPreview: ")
            val camera = mCamera ?: let {
                Log.e(TAG, "Camera1 startPreview: mCamera is null")
                return
            }
            camera.startPreview()
            mCurrentState = CameraLifecycle.PREVIEWING
        } else {
            mStartPreviewWhenOpen = true
        }
    }

    override fun stopPreview() {
        Log.d(TAG, "Camera1 stopPreview: ")
        if (mCurrentState == CameraLifecycle.PREVIEWING) {
            Log.d(TAG, "Camera1 doStopPreview: ")
            val camera = mCamera ?: let {
                Log.e(TAG, "Camera1 stopPreview: mCamera is null")
                return
            }
            camera.stopPreview()
            mCurrentState = CameraLifecycle.OPENED
        }
        if (mCloseWhenStopPreview) {
            close()
            mCloseWhenStopPreview = false
        }
    }

    override fun close() {
        Log.d(TAG, "Camera1 close: ")
        if (mCurrentState == CameraLifecycle.OPENED) {
            doClose()
            mCurrentState = CameraLifecycle.CLOSED
        } else {
            mCloseWhenStopPreview = true
        }
    }

    override fun takePhoto(failedCallback: ((errorCode: Int, errorMessage: String) -> Unit)?, successCallback: ((photo: Photo, camera: Camera) -> Unit)?) {
        val camera = mCamera ?: let {
            Log.e(TAG, "takePicture: mCamera is null")
            failedCallback?.invoke(-1, "camera is null")
            return
        }
        camera.takePicture(
            {

            },
            { raw, c ->
                Log.d(TAG, "takePicture: raw:${raw}")
            },
            { postview, c ->
                Log.d(TAG, "takePicture: postview:${postview}")
            },
            { jpeg, c ->
                Log.d(TAG, "takePicture: jpeg:${jpeg}")
                if (jpeg == null) {
                    failedCallback?.invoke(-2, "jpeg is null")
                    return@takePicture
                }

                val photo = Photo(jpeg, mCameraPictureWidth, mCameraPictureHeight, PhotoFormat.JPEG, mCameraInfo.orientation, mLensFacing == LensFacing.FRONT)
                successCallback?.invoke(photo, this@Camera1)
            }
        )
    }


    private fun doOpen() {
        Log.d(TAG, "Camera1 doOpen: ")
        if (mPreviewWidth == 0 || mPreviewHeight == 0) {
            throw IllegalStateException("must call setPreviewViewSize")
        }

        Log.d(TAG, "doOpen: Camera.open(mCameraId), mCameraId:$mCameraId")
        val camera = android.hardware.Camera.open(mCameraId) ?: let {
            Log.e(TAG, "open camera failed: mCameraId:$mCameraId")
            return
        }
        mCamera = camera

        val parameters: android.hardware.Camera.Parameters = mCamera?.parameters ?: return
        val sizes = parameters.getSupportedPreviewSizes().also {
            val s = it.contentToStringWithBuilder { size, stringBuilder -> stringBuilder.append(size.width).append('x').append(size.height) }
            Log.d(TAG, "open: sizes:${s}")
        }.map { Size(it.width, it.height) }.sortDescend().also { Log.d(TAG, "open: sizes:${it}") }

        Log.d(TAG, "doOpen: mPreviewWidth:${mPreviewWidth}, mPreviewHeight:${mPreviewHeight}")
        val previewSize = sizes.selectSize(mPreviewWidth, mPreviewHeight, CameraPreviewSizeSelectStrategy.Larger, true)
        Log.d(TAG, "open: previewSize:${previewSize}")
        mCameraPreviewWidth = previewSize.width
        mCameraPreviewHeight = previewSize.height

        val pictureSize = sizes.selectSize(mPreviewWidth, mPreviewHeight, CameraPreviewSizeSelectStrategy.Max, true)
        Log.i(TAG, "open: pictureSize:${pictureSize}")
        mCameraPictureWidth = pictureSize.width
        mCameraPictureHeight = pictureSize.height

        parameters.setPreviewSize(previewSize.width, previewSize.height)
        parameters.setPictureSize(pictureSize.width, pictureSize.height)
        parameters.jpegQuality = 100

        val supportedPreviewFormats = parameters.supportedPreviewFormats
        Log.d(TAG, "open: supportedPreviewFormats: ${supportedPreviewFormats.contentToString { it.toImageFormat() }}")
        if (supportedPreviewFormats.contains(ImageFormat.NV21)) {
            parameters.previewFormat = ImageFormat.NV21
        }
        val previewFormat = parameters.previewFormat
        val pixelFormat = PixelFormat();
        PixelFormat.getPixelFormatInfo(previewFormat, pixelFormat)
        //        Log.d(TAG, "open: bufferSize: ${previewSize.width} * ${previewSize.height} * ${pixelFormat.bitsPerPixel}")
        val bufferSize = (previewSize.width * previewSize.height * pixelFormat.bitsPerPixel) / 8
        for (i in 0 until 3) {
            val buffer = ByteArray(bufferSize)
            camera.addCallbackBuffer(buffer)
            Log.d(TAG, "Add callback buffer ${i + 1} with size: $bufferSize")
        }
        Log.d(TAG, "Add three callback buffers with size: $bufferSize")

        mOutputTexture = createDetachedSurfaceTexture()

        camera.setPreviewTexture(mOutputTexture)
        camera.setPreviewCallbackWithBuffer(mCameraPreviewCallback)
        camera.parameters = parameters
    }

    private fun doClose() {
        Log.d(TAG, "Camera1 doClose: ")
        mCamera ?: let {
            Log.e(TAG, "camera 1 doClose: mCamera is null")
        }
        mCamera?.setPreviewCallbackWithBuffer(null)

        mCamera?.release()
        mCamera = null

        releaseSurfaceTexture()
    }

    private val mCameraPreviewCallback = android.hardware.Camera.PreviewCallback { data, camera ->
//        Log.d(TAG, "mCameraPreviewCallback: $data, camera:$camera")
        // TODO: 优化 使用缓存而不是每次new对象
        val cameraImage = CameraImage(
            Format.NV21, mCameraPreviewWidth, mCameraPreviewHeight, data,
            mCameraInfo.orientation, mLensFacing == LensFacing.FRONT
        )
        Log.d(TAG, "android.hardware.Camera.PreviewCallback CameraImage.size: ${mCameraPreviewWidth}x${mCameraPreviewHeight}, data.size:${data.size}")
        mPreviewCallback?.invoke(cameraImage, this@Camera1)
        // 在使用完 Buffer 之后记得回收复用
        if (autoRecycleBuffer) {
            camera.addCallbackBuffer(data)
        }
    }

    override fun addCallbackBuffer(data: ByteArray) {
        if (autoRecycleBuffer) {
            throw IllegalStateException("do not recycle buffer when autoRecycleBuffer is true")
        }
        mCamera?.addCallbackBuffer(data)
    }

    /**
     * 创建一个SurfaceTexture并
     */
    @SuppressLint("ObsoleteSdkInt")
    private fun createDetachedSurfaceTexture(): SurfaceTexture {
        // 创建一个新的SurfaceTexture并从解绑GL上下文
        val surfaceTexture = SurfaceTexture(0)
        surfaceTexture.detachFromGLContext()
        if (Build.VERSION.SDK_INT >= 21) {
            var outputThread = mOutputThread
            outputThread?.quit()
            outputThread = HandlerThread("FrameAvailableThread")
            outputThread.start()
            surfaceTexture.setOnFrameAvailableListener({ texture: SurfaceTexture? ->
                Log.d(TAG, "createDetachedSurfaceTexture-01: texture:$texture")
            }, Handler(outputThread.getLooper()))
            mOutputThread = outputThread
        } else {
            surfaceTexture.setOnFrameAvailableListener { texture: SurfaceTexture? ->
                Log.d(TAG, "createDetachedSurfaceTexture-02: texture:$texture")
            }
        }
        return surfaceTexture
    }

    /**
     * 释放资源
     */
    private fun releaseSurfaceTexture() {
        mOutputThread?.quitSafely()
        mOutputThread = null

        mOutputTexture?.release()
        mOutputTexture = null
    }
}
