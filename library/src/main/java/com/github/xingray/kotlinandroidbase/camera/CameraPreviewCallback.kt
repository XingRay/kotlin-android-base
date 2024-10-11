package com.github.xingray.kotlinandroidbase.camera

import com.github.xingray.kotlinandroidbase.camera.Camera
import com.github.xingray.kotlinandroidbase.camera.CameraImage

interface CameraPreviewCallback {
    fun onPreviewFrame(cameraImage: CameraImage, camera: Camera)
}