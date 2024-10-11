package com.github.xingray.kotlinandroidbase.ext

import android.graphics.Matrix
import kotlin.math.atan2

private val TAG = "MatrixExtension"

typealias MatrixValues = FloatArray

fun Matrix.values(): MatrixValues {
    val matrixValues = FloatArray(9)
    getValues(matrixValues)
    return matrixValues
}


fun MatrixValues.translationX(): Float {
    // 解析矩阵值以获取平移和缩放信息
    return this[Matrix.MTRANS_X]
}

fun MatrixValues.translationY(): Float {
    return this[Matrix.MTRANS_Y]
}

fun MatrixValues.scaleX(): Float {
    return this[Matrix.MSCALE_X]
}

fun MatrixValues.scaleY(): Float {
    return this[Matrix.MSCALE_Y]
}

// 计算旋转角度（弧度）
fun MatrixValues.radian(): Double {
    // 解析矩阵值以获取旋转角度
    val cosTheta = this[Matrix.MSCALE_X]
    val sinTheta = this[Matrix.MSKEW_Y]
    return atan2(sinTheta.toDouble(), cosTheta.toDouble())
}

// 计算旋转角度（弧度）
fun MatrixValues.degree(): Double {
    // 解析矩阵值以获取旋转角度
    val cosTheta = this[Matrix.MSCALE_X]
    val sinTheta = this[Matrix.MSKEW_Y]

    // 计算旋转角度（弧度）
    val rotationRadians = atan2(sinTheta.toDouble(), cosTheta.toDouble())

    // 将弧度转换为角度（度）
    return Math.toDegrees(rotationRadians)
}