package com.github.xingray.kotlinandroidbase.camera

import android.content.Context

fun CameraApi.isSupported(context: Context): Boolean {
    return when (this) {
        CameraApi.API_1 -> true
        CameraApi.API_2 -> context.supportCamera2()
        CameraApi.API_X -> false
    }
}