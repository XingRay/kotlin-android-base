package com.github.xingray.kotlinandroidbase.camera

interface Camera {
    val lensFacing: LensFacing
    val cameraApi: CameraApi

    fun open()

    fun close()

    fun startPreview()

    fun stopPreview()

    fun setPreviewViewSize(width: Int, height: Int)

    fun setPreviewCallback(callback: CameraPreviewCallback)

    fun setPreviewCallback(callback: (CameraImage, Camera) -> Unit)

    fun takePhoto(
        failedCallback: ((errorCode: Int, errorMessage: String) -> Unit)? = null,
        successCallback: ((photo: Photo, camera: Camera) -> Unit)?
    )

    fun addCallbackBuffer(data:ByteArray)
}