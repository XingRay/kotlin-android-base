@file:Suppress("DEPRECATION")

package com.github.xingray.kotlinandroidbase.camera

import android.hardware.Camera
import android.hardware.camera2.CameraMetadata
import android.util.Log

private val TAG = "Camera2Extension"
fun LensFacing.toCamera2MataDataValue(): Int {
    return when (this) {
        LensFacing.FRONT -> CameraMetadata.LENS_FACING_FRONT
        LensFacing.BACK -> CameraMetadata.LENS_FACING_BACK
        LensFacing.EXTERNAL -> CameraMetadata.LENS_FACING_EXTERNAL
    }
}

@Suppress("DEPRECATION")
fun LensFacing.toCamera1CameraFacing(): Int? {
    return when (this) {
        LensFacing.FRONT -> Camera.CameraInfo.CAMERA_FACING_FRONT
        LensFacing.BACK -> Camera.CameraInfo.CAMERA_FACING_BACK
        LensFacing.EXTERNAL -> {
            Log.e(TAG, "$this is not supported by android camera api")
            return null
        }
    }
}