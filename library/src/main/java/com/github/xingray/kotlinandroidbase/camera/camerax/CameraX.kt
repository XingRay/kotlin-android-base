package com.github.xingray.kotlinandroidbase.camera.camerax

import com.github.xingray.kotlinandroidbase.camera.Camera
import com.github.xingray.kotlinandroidbase.camera.CameraApi
import com.github.xingray.kotlinandroidbase.camera.CameraImage
import com.github.xingray.kotlinandroidbase.camera.CameraPreviewCallback
import com.github.xingray.kotlinandroidbase.camera.LensFacing
import com.github.xingray.kotlinandroidbase.camera.Photo

class CameraX(
    private val mCameraId: String,
    private val mLensFacing: LensFacing,
    private val mCameraApi: CameraApi
) : Camera {

    private var mPreviewCallback: ((CameraImage, Camera) -> Unit)? = null

    override val lensFacing: LensFacing
        get() = mLensFacing
    override val cameraApi: CameraApi
        get() = mCameraApi

    override fun open() {
    }


    override fun startPreview() {

    }

    override fun stopPreview() {

    }

    override fun close() {
    }

    override fun setPreviewViewSize(width: Int, height: Int) {
    }

    override fun setPreviewCallback(callback: CameraPreviewCallback) {
        mPreviewCallback = callback::onPreviewFrame
    }

    override fun setPreviewCallback(callback: (CameraImage, Camera) -> Unit) {
        mPreviewCallback = callback
    }

    override fun addCallbackBuffer(data: ByteArray) {

    }

    override fun takePhoto(failedCallback: ((errorCode: Int, errorMessage: String) -> Unit)?, successCallback: ((photo: Photo, camera: Camera) -> Unit)?) {

    }
}