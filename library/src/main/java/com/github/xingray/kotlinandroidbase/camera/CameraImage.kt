package com.github.xingray.kotlinandroidbase.camera


data class CameraImage(
    var format: Format,
    var width: Int,
    var height: Int,
    var data: ByteArray,

    // 逆时针旋转角度
    var rotate: Int,

    // 镜像, 水平方向左右互换, 即沿Y轴对称
    var mirror: Boolean,
)

enum class Format(id: Int, title: String) {
    NV21(0, "nv21")
}