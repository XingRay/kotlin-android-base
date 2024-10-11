package com.github.xingray.kotlinandroidbase.ext

import android.graphics.Point
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import android.util.Log
import kotlin.math.abs

private val TAG = "GraphicExtension"

fun Rect.margin(margin: Int) {
    left = Math.max(0, left - margin)
    right = right + margin
    top = Math.max(0, top - margin)
    bottom = bottom + margin
}

fun RectF.margin(margin: Int) {
    left = Math.max(0.0f, left - margin)
    right = right + margin
    top = Math.max(0.0f, top - margin)
    bottom = bottom + margin
}

fun RectF.margin(margin: Float) {
    left = Math.max(0.0f, left - margin)
    right = right + margin
    top = Math.max(0.0f, top - margin)
    bottom = bottom + margin
}

fun List<Point>.calcRect(): Rect? {
    if (isEmpty()) {
        return null
    }

    val p0 = this[0]
    val rect = Rect(p0.x, p0.y, p0.x, p0.y)

    for (i in 1 until size) {
        val point = this[i]
        val x = point.x
        val y = point.y

        if (x < rect.left) {
            rect.left = x
        } else if (x > rect.right) {
            rect.right = x
        }

        if (y < rect.top) {
            rect.top = y
        } else if (y > rect.bottom) {
            rect.bottom = y
        }
    }

    return rect
}

// [ x0, y0, x1, y1, ... ]
fun FloatArray.calcRectF(startIndex: Int = 0): RectF? {
    if (isEmpty()) {
        Log.w(TAG, "calcRectF: FloatArray isEmpty")
        return null
    }
    if (startIndex >= size) {
        Log.w(TAG, "calcRectF: FloatArray startIndex >= size")
        return null
    }

    val p0x = this[startIndex]
    val p0y = this[startIndex + 1]
    val rect = RectF(p0x, p0y, p0x, p0y)

    for (i in startIndex + 2 until size step 2) {
        val x = this[i]
        val y = this[i + 1]

        if (x < rect.left) {
            rect.left = x
        } else if (x > rect.right) {
            rect.right = x
        }

        if (y < rect.top) {
            rect.top = y
        } else if (y > rect.bottom) {
            rect.bottom = y
        }
    }

    return rect
}

fun RectF.toRect(): Rect {
    return Rect(this.left.toInt(), this.top.toInt(), this.right.toInt(), this.bottom.toInt())
}

fun RectF.toRect(width: Int, height: Int): Rect {
    return Rect((this.left * width).toInt(), (this.top * height).toInt(), (this.right * width).toInt(), (this.bottom * height).toInt())
}

fun IntArray.calcRect(): Rect? {
    if (isEmpty()) {
        return null
    }

    val p0x = this[0]
    val p0y = this[0 + 1]
    val rect = Rect(p0x, p0y, p0x, p0y)

    for (i in 1 until size step 2) {
        val x = this[i]
        val y = this[i + 1]

        if (x < rect.left) {
            rect.left = x
        } else if (x > rect.right) {
            rect.right = x
        }

        if (y < rect.top) {
            rect.top = y
        } else if (y > rect.bottom) {
            rect.bottom = y
        }
    }

    return rect
}

// points = [x0,y0, x1,y1, ...]
fun FloatArray.toPointFs(width: Int, height: Int, startIndex: Int = 0): List<PointF> {
    val points = mutableListOf<PointF>()
    for (i in startIndex until size step 2) {
        points.add(PointF(this[i] * width, this[i + 1] * height))
    }
    return points
}

// points = [x0,y0, x1,y1, ...]
fun FloatArray.toPoints(width: Int, height: Int, startIndex: Int = 0): List<Point> {
    val points = mutableListOf<Point>()
    for (i in startIndex until size step 2) {
        points.add(Point((this[i] * width).toInt(), (this[i + 1] * height).toInt()))
    }
    return points
}

// points = [x0,y0, x1,y1, ...]
fun FloatArray.toPoints(startIndex: Int = 0): List<Point> {
    val points = mutableListOf<Point>()
    for (i in startIndex until size step 2) {
        points.add(Point(this[i].toInt(), this[i + 1].toInt()))
    }
    return points
}

// points = [x0,y0, x1,y1, ...] ,  [0.3, 0.5, ...] map (100, 200) => [30, 100, ...]
fun FloatArray.mapPoints(width: Int, height: Int, startIndex: Int = 0): FloatArray {
    val points = FloatArray(this.size - startIndex)
    for (i in startIndex until size step 2) {
        points[i - startIndex + 0] = this[i + 0] * width
        points[i - startIndex + 1] = this[i + 1] * height
    }
    return points
}

// points = [x0,y0, x1,y1, ...]
fun IntArray.toPoints(width: Int, height: Int, startIndex: Int = 0): List<Point> {
    val points = mutableListOf<Point>()
    for (i in startIndex until size step 2) {
        points.add(Point(this[i] * width, this[i + 1] * height))
    }
    return points
}

// [x0,y0, x1,y1, ... ]
fun FloatArray.findNearestPointIndex(x: Float, y: Float, searchRadius: Float): Int {
    var nearestDistance2: Double = Double.MAX_VALUE
    var nearestIndex = -1
    val searchRadius2 = searchRadius * searchRadius

    for (i in this.indices step 2) {
        val pointX = this[i + 0]
        val pointY = this[i + 1]
        val dx = abs(x - pointX).toDouble()
        val dy = abs(y - pointY).toDouble()
        if (dx < searchRadius && dy < searchRadius && (dx * dx + dy * dy) < searchRadius2) {
            val distance2 = dx * dx + dy * dy
            if (distance2 < nearestDistance2) {
                nearestDistance2 = distance2
                nearestIndex = i / 2
            }
        }
    }

    return nearestIndex
}

/**
 * landmark : [x0,y0, x1,y1 ... ] 坐标点序列
 * landmark 待调整的序列, 其中部分点已经调整过了,
 * beforeUpdateLandmark: 未调整前的原始点序列
 *
 * IntArray: [
 * startIndex0, endIndex0, valueIndex0,
 * startIndex1, endIndex1, valueIndex1,
 * ...
 * ]
 * 三个一组, 表示已经调整好的 start和end的下标,及要调整的 value 的下标
 */
fun IntArray.adjust(landmark: FloatArray, beforeUpdateLandmark: FloatArray, dimensions: Int = 2) {
    val indexArray = this
    for (i in indexArray.indices step 3) {
        // start
        val index0 = indexArray[i + 0]

        // end
        val index1 = indexArray[i + 1]

        // value
        val index2 = indexArray[i + 2]

//        Log.d(TAG, "adjust: index0:${index0}, index1:${index1}, index2:${index2}")
//        Log.d(
//            TAG,
//            "adjust: before:(${beforeUpdateLandmark[index0 * 2 + 0]},${beforeUpdateLandmark[index0 * 2 + 1]}),(${beforeUpdateLandmark[index1 * 2 + 0]},${beforeUpdateLandmark[index1 * 2 + 1]}),(${beforeUpdateLandmark[index2 * 2 + 0]},${beforeUpdateLandmark[index2 * 2 + 1]})"
//        )
//        Log.d(
//            TAG,
//            "adjust: update:(${landmark[index0 * 2 + 0]},${landmark[index0 * 2 + 1]}),(${landmark[index1 * 2 + 0]},${landmark[index1 * 2 + 1]}),(${landmark[index2 * 2 + 0]},${landmark[index2 * 2 + 1]})"
//        )

        for (dimension in 0 until dimensions) {
            landmark[index2 * 2 + dimension] = linearTransform(
                beforeUpdateLandmark[index0 * 2 + dimension], beforeUpdateLandmark[index1 * 2 + dimension], beforeUpdateLandmark[index2 * 2 + dimension],
                landmark[index0 * 2 + dimension], landmark[index1 * 2 + dimension]
            )
        }

//        Log.d(
//            TAG,
//            "adjust: adjust:(${landmark[index0 * 2 + 0]},${landmark[index0 * 2 + 1]}),(${landmark[index1 * 2 + 0]},${landmark[index1 * 2 + 1]}),(${landmark[index2 * 2 + 0]},${landmark[index2 * 2 + 1]})"
//        )
    }
}

/**
 * beforeStart是变化前的起点, beforeEnd是变化前的终点, beforeValue 是变化前的中间值,
 * updatedStart是变化后的起点, updatedEnd是变化后的终点,
 * 求变化后的中间值, 中间值 = 起点*w1+终点*w2 其中w1+w2=1 , 要求变化前后 w1 w2不变
 */
fun linearTransform(beforeStart: Float, beforeEnd: Float, beforeValue: Float, updatedStart: Float, updatedEnd: Float): Float {
    // 计算变换前的范围
    val beforeRange = beforeEnd - beforeStart
    if (abs(beforeRange) < 0.005) {
        return (updatedStart + updatedEnd) * 0.5f
    }
    // 计算变换后的范围
    val updatedRange = updatedEnd - updatedStart
    // 计算变换前中间值的比例
    val ratio = (beforeValue - beforeStart) / beforeRange
    // 计算变换后的中间值
    val updatedValue = updatedStart + (beforeValue - beforeStart) * updatedRange / beforeRange
    if (updatedValue.isNaN()) {
        // beforeEnd - beforeStart 接近0时, 计算结果可能会为 NaN,此时无法计算原数值在  [beforeStart,beforeEnd] 中的权重, 按 0.5处理
        // 取 [ updatedStart updatedEnd ] 中点
        return (updatedStart + updatedEnd) * 0.5f
    }
    return updatedValue
}

// 将多个大小不一样的矩形合并成一个矩形, 互不重叠
// 流程: 先将所有矩形按高度降序排序, 合并后的高度取最高的矩形的高度, 然后从左到右, 从上到下分别放置矩形, 并修改矩形的坐标
fun List<Rect>.merge(): Rect {
    if (this.isEmpty()) {
        return Rect(0, 0, 0, 0)
    }

    val sorted = this.sortedBy { it.height() }.reversed().toMutableList()
    val height = sorted[0].height()
    var width = sorted[0].width()

    var currentX = 0
    var currentY = 0

    while (sorted.isNotEmpty()) {
        val rect = sorted.removeFirst()
        val remainHeight = height - currentY

        if (rect.height() <= remainHeight) {
            // 该列剩余的高度可以放置一个矩形
            rect.moveTo(currentX, currentY)

            // 放置了一个矩形后, 当前位置下移
            currentY += rect.height()

            if (currentX + rect.width() > width) {
                // 如果这个矩形的范围超出当前整体范围, 那么需要将整体范围扩大
                width = currentX + rect.width()
            }
        } else {
            // 该列剩余高度不能再放矩形, 那么需要再创建一列空间
            currentX = width
            currentY = 0

            // 扩大整体范围的宽度
            width = currentX + rect.width()

            //放置在当前位置
            rect.moveTo(currentX, currentY)

            // 当前位置向下移动
            currentY += rect.height()
        }
    }

    return Rect(0, 0, width, height)
}

fun Rect.moveTo(x: Int, y: Int) {
    val width = width()
    val height = height()
    left = x
    top = y
    right = left + width
    bottom = top + height
}