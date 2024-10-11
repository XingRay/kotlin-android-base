package com.github.xingray.kotlinandroidbase.camera.camera2

import android.annotation.SuppressLint
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.media.ImageReader
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.view.Surface
import androidx.annotation.RequiresApi
import com.github.xingray.kotlinandroidbase.camera.Camera
import com.github.xingray.kotlinandroidbase.camera.CameraApi
import com.github.xingray.kotlinandroidbase.camera.CameraImage
import com.github.xingray.kotlinandroidbase.camera.CameraLifecycle
import com.github.xingray.kotlinandroidbase.camera.CameraPreviewCallback
import com.github.xingray.kotlinandroidbase.camera.CameraPreviewSizeSelectStrategy
import com.github.xingray.kotlinandroidbase.camera.LensFacing
import com.github.xingray.kotlinandroidbase.camera.Photo
import com.github.xingray.kotlinandroidbase.camera.contentToStringWithBuilder
import com.github.xingray.kotlinandroidbase.camera.imageToCameraImage
import com.github.xingray.kotlinandroidbase.camera.selectSize
import com.github.xingray.kotlinandroidbase.executor.HandlerExecutor
import java.io.Closeable
import kotlin.math.max
import kotlin.math.min

private val TAG = Camera2::class.simpleName

class Camera2(
    private val mCameraManager: CameraManager,
    private val mCameraId: String,
    private val mLensFacing: LensFacing,
    private val mCameraApi: CameraApi,
    private val mCameraCharacteristics: CameraCharacteristics
) : Closeable, Camera {

    private var mCameraDevice: CameraDevice? = null
    private var mCameraCaptureSession: CameraCaptureSession? = null

    private var mTargetList = mutableListOf<Surface>()
    private val mFpsRanges = mCameraCharacteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES)
    private val mSensorOrientation = mCameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 0
    private var mBackgroundThread: HandlerThread? = null
    private var mBackgroundHandler: Handler? = null
    private var mBackgroundExecutor: HandlerExecutor? = null

    private var mPreviewSize: Size? = null
    private var mPreviewCallback: ((CameraImage, Camera) -> Unit)? = null
    private var mImageReader: ImageReader? = null

    private var mCurrentState = CameraLifecycle.CREATED
    private var mStartPreviewWhenOpen = false
    private var mCloseWhenStopPreview = false

    @Suppress("unused")
    val previewSize: Size?
        get() = mPreviewSize

    override val lensFacing: LensFacing
        get() = mLensFacing
    override val cameraApi: CameraApi
        get() = mCameraApi

    override fun setPreviewViewSize(width: Int, height: Int) {
        Log.i(TAG, "setPreviewViewSize: width:$width, height:$height")

        val map = mCameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        val supportPictureSizes = map?.getOutputSizes(ImageFormat.YUV_420_888)
        if (supportPictureSizes.isNullOrEmpty()) {
            mPreviewSize = Size(width, height)
            return
        }

        mPreviewSize = supportPictureSizes.map { Size(it.width, it.height) }.also {
            Log.d(
                TAG, "setPreviewViewSize: supportPictureSizes:${
                    it.contentToStringWithBuilder(stringMapper = { size, stringBuilder ->
                        stringBuilder.append(size.width).append('x').append(size.height).append(' ')
                    })
                }"
            )
        }.selectSize(max(width, height), min(width, height), CameraPreviewSizeSelectStrategy.Smaller)
        Log.i(TAG, "setPreviewViewSize: selected previewSize:$mPreviewSize")
    }

    @Suppress("unused")
    fun addTargetSurface(surface: Surface) {
        mTargetList.add(surface)
    }

    @Suppress("unused")
    fun addTargetSurfaceList(surfaceList: List<Surface>) {
        mTargetList.addAll(surfaceList)
    }

    override fun open() {
        Log.d(TAG, "open: ")
        if (mCurrentState == CameraLifecycle.CREATED || mCurrentState == CameraLifecycle.CLOSED) {
            doOpen()
        }
    }

    override fun takePhoto(failedCallback: ((errorCode: Int, errorMessage: String) -> Unit)?, successCallback: ((photo: Photo, camera: Camera) -> Unit)?) {

    }

    @SuppressLint("MissingPermission")
    private fun doOpen() {
        Log.d(TAG, "doOpen: ")
        addInnerImagerReaderAsTarget()
        startBackgroundThread()
        Log.d(TAG, "mCameraManager.openCamera: $mCameraId")
        mCameraManager.openCamera(mCameraId, mCameraDeviceStateCallback, mBackgroundHandler)
    }

    override
    fun startPreview() {
        Log.d(TAG, "startPreview: ")
        if (mCurrentState == CameraLifecycle.OPENED) {
            doStartPreview()
            mCurrentState = CameraLifecycle.PREVIEWING
        } else {
            mStartPreviewWhenOpen = true
        }
    }

    private fun doStartPreview() {
        //        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//            createCaptureSessionUseSessionConfiguration()
//        } else {
        createCaptureSessionUseLegacy()
//        }
    }

    override fun stopPreview() {
        if (mCurrentState == CameraLifecycle.PREVIEWING) {
            mCameraCaptureSession?.abortCaptures()
            mCurrentState = CameraLifecycle.OPENED
        }
        if (mCloseWhenStopPreview) {
            close()
            mCloseWhenStopPreview = false
        }
    }

    override fun close() {
        Log.d(TAG, "close: ")
        if (mCurrentState == CameraLifecycle.OPENED) {
            doClose()
            mCurrentState = CameraLifecycle.CLOSED
        } else {
            mCloseWhenStopPreview = true
        }
    }

    private fun doClose() {
        Log.d(TAG, "doClose: ")
        mCameraCaptureSession?.abortCaptures()
        mCameraCaptureSession = null

        mCameraDevice?.close()
        mCameraDevice = null

        mImageReader?.close()
        mImageReader = null

        stopBackgroundThread()
    }

    private fun addInnerImagerReaderAsTarget() {
        startBackgroundThread()
        val previewSize = mPreviewSize ?: return
        mImageReader = ImageReader.newInstance(previewSize.width, previewSize.height, ImageFormat.YUV_420_888, 2)
        mImageReader?.apply {
            setOnImageAvailableListener(mOnImageAvailableListener, mBackgroundHandler)
            mTargetList.add(this.surface)
        }
    }

    private val mOnImageAvailableListener = ImageReader.OnImageAvailableListener { reader ->
        val image = reader.acquireNextImage()
        if (image != null) {
            val cameraImage = imageToCameraImage(image, mSensorOrientation, mLensFacing == LensFacing.FRONT)
            mPreviewCallback?.invoke(cameraImage, this)
            image.close()
        }
    }

    override fun setPreviewCallback(callback: CameraPreviewCallback) {
        mPreviewCallback = callback::onPreviewFrame
    }

    override fun setPreviewCallback(callback: (CameraImage, Camera) -> Unit) {
        mPreviewCallback = callback
    }

    override fun addCallbackBuffer(data: ByteArray) {

    }

    val mCameraDeviceStateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            Log.i(TAG, "mCameraDeviceStateCallback#onOpened: ${camera}")
            mCameraDevice = camera
            mCurrentState = CameraLifecycle.OPENED

            if (mStartPreviewWhenOpen) {
                startPreview()
                mStartPreviewWhenOpen = false
            }
        }

        override fun onClosed(camera: CameraDevice) {
            super.onClosed(camera)
            Log.i(TAG, "mCameraDeviceStateCallback#onClosed: camera:${camera}")
        }

        override fun onDisconnected(camera: CameraDevice) {
            Log.i(TAG, "mCameraDeviceStateCallback#onDisconnected: camera:$camera")
        }

        override fun onError(camera: CameraDevice, error: Int) {
            Log.e(TAG, "mCameraDeviceStateCallback#onError: camera:$camera, error:$error")
        }
    }

    @Suppress("unused")
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun createCaptureSessionUseSessionConfiguration() {
        Log.i(TAG, "${::createCaptureSessionUseSessionConfiguration.name}: ")
        val cameraDevice = mCameraDevice ?: run {
            Log.e(TAG, "createCaptureSession: cameraDevice is null, must call ${::open.name}")
            return
        }
        val executor = mBackgroundExecutor ?: run {
            Log.e(TAG, "mBackgroundExecutor is null, call startBackgroundThread() before here")
            return
        }

        val targetList = mTargetList.takeIf { mTargetList.isNotEmpty() } ?: run {
            Log.e(TAG, "createCaptureSessionUseSessionConfiguration: mTargetList is null")
            return
        }
        Log.i(TAG, "createCaptureSessionUseSessionConfiguration: targetList.size:${targetList.size}")

        val sessionConfiguration = SessionConfiguration(
            SessionConfiguration.SESSION_REGULAR,
            targetList.map { OutputConfiguration(it) },
            executor,
            mCameraCaptureSessionStateCallback
        )

        val builder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
            targetList.forEach(this::addTarget)
        }

        sessionConfiguration.sessionParameters = builder.build()
        try {
            if (cameraDevice.isSessionConfigurationSupported(sessionConfiguration)) {
                cameraDevice.createCaptureSession(sessionConfiguration)
            } else {
                createCaptureSessionUseLegacy()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            createCaptureSessionUseLegacy()
        }
    }

    @Suppress("DEPRECATION")
    private fun createCaptureSessionUseLegacy() {
        Log.i(TAG, "createCaptureSessionUseLegacy: ")
        val cameraDevice = mCameraDevice ?: run {
            Log.e(TAG, "createCaptureSession: cameraDevice is null, call open() before here")
            return
        }

        val backgroundHandler = mBackgroundHandler ?: run {
            Log.e(TAG, "mBackgroundHandler is null, call startBackgroundThread() before here")
            return
        }

        val targetList = mTargetList.takeIf { mTargetList.isNotEmpty() } ?: run {
            Log.e(TAG, "createCaptureSessionUseLegacy: mTargetList is null or empty")
            return
        }

        cameraDevice.createCaptureSession(
            targetList,
            mCameraCaptureSessionStateCallback,
            backgroundHandler
        )
    }

    private val mCameraCaptureSessionStateCallback = object : CameraCaptureSession.StateCallback() {
        override fun onConfigured(session: CameraCaptureSession) {
            Log.d(TAG, "mCameraCaptureSessionStateCallback#onConfigured: session:$session")
            mCameraCaptureSession = session
            val cameraDevice = mCameraDevice ?: run {
                Log.e(TAG, "mCameraCaptureSessionStateCallback#onConfigured: mCameraDevice is null")
                session.abortCaptures()
                return
            }
            val targetList = mTargetList.takeIf { mTargetList.isNotEmpty() } ?: run {
                Log.e(TAG, "createCaptureSessionUseSessionConfiguration: mTargetList is null")
                session.abortCaptures()
                return
            }
            val request = createCaptureRequest(cameraDevice, targetList)
            session.setRepeatingRequest(request, null/*mCaptureCallback*/, mBackgroundHandler)
        }

        override fun onConfigureFailed(session: CameraCaptureSession) {
            Log.d(TAG, "mCameraCaptureSessionStateCallback#onConfigureFailed: session:$session")
        }
    }

//    private val mCaptureCallback = object : CameraCaptureSession.CaptureCallback() {
//        override fun onCaptureStarted(session: CameraCaptureSession, request: CaptureRequest, timestamp: Long, frameNumber: Long) {
//            super.onCaptureStarted(session, request, timestamp, frameNumber)
//            Log.d(TAG, "onCaptureStarted: ")
//        }
//
//        override fun onReadoutStarted(session: CameraCaptureSession, request: CaptureRequest, timestamp: Long, frameNumber: Long) {
//            super.onReadoutStarted(session, request, timestamp, frameNumber)
//            Log.d(TAG, "onReadoutStarted: ")
//        }
//
//        override fun onCaptureProgressed(session: CameraCaptureSession, request: CaptureRequest, partialResult: CaptureResult) {
//            super.onCaptureProgressed(session, request, partialResult)
//            Log.d(TAG, "onCaptureProgressed: ")
//        }
//
//        override fun onCaptureCompleted(session: CameraCaptureSession, request: CaptureRequest, result: TotalCaptureResult) {
//            super.onCaptureCompleted(session, request, result)
//            Log.d(TAG, "onCaptureCompleted: ")
//        }
//
//        override fun onCaptureFailed(session: CameraCaptureSession, request: CaptureRequest, failure: CaptureFailure) {
//            super.onCaptureFailed(session, request, failure)
//            Log.d(TAG, "onCaptureFailed: ")
//        }
//
//        override fun onCaptureSequenceCompleted(session: CameraCaptureSession, sequenceId: Int, frameNumber: Long) {
//            super.onCaptureSequenceCompleted(session, sequenceId, frameNumber)
//            Log.d(TAG, "onCaptureSequenceCompleted: ")
//        }
//
//        override fun onCaptureSequenceAborted(session: CameraCaptureSession, sequenceId: Int) {
//            super.onCaptureSequenceAborted(session, sequenceId)
//            Log.d(TAG, "onCaptureSequenceAborted: ")
//        }
//
//        override fun onCaptureBufferLost(session: CameraCaptureSession, request: CaptureRequest, target: Surface, frameNumber: Long) {
//            super.onCaptureBufferLost(session, request, target, frameNumber)
//            Log.d(TAG, "onCaptureBufferLost: ")
//        }
//    }

    fun createCaptureRequest(
        cameraDevice: CameraDevice,
        targetList: List<Surface>
    ): CaptureRequest {

        val builder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)

        mFpsRanges?.let {
            builder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, it[(it.size - 1).coerceAtMost(2)])
        }

        targetList.forEach {
            builder.addTarget(it)
        }

        return builder.build()
    }


    private fun startBackgroundThread() {
        if (mBackgroundHandler != null) {
            return
        }
        Log.d(TAG, "startBackgroundThread: ")
        val backgroundThread = HandlerThread("t-Camera2")
        backgroundThread.start()
        val handler = Handler(backgroundThread.looper)
        val handlerExecutor = HandlerExecutor(handler)

        mBackgroundHandler = handler
        mBackgroundExecutor = handlerExecutor
        mBackgroundThread = backgroundThread
    }

    private fun stopBackgroundThread() {
        mBackgroundThread?.quitSafely()
        mBackgroundThread?.join()

        mBackgroundThread = null
        mBackgroundHandler = null
        mBackgroundExecutor = null
    }
}