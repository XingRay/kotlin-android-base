package com.github.xingray.kotlinandroidbase.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PointF
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatImageView
import com.github.xingray.kotlinandroidbase.ext.changeColor
import com.github.xingray.kotlinandroidbase.ext.createMapped

class LiquidEditorImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    companion object {
        @JvmStatic
        private val TAG = LiquidEditorImageView::class.java.simpleName
    }

    var liquidEditEnable: Boolean = false

    // 作用范围半径， 默认 400px
    private var mRadius = 400

    // 半径平方
    private var mRadius2 = mRadius * mRadius

    private var mUseLinearInterpolator = false

    // 使用归一化坐标， 坐标范围为 [0.0-1.0]
    private var mCenter = PointF(0.0f, 0.0f)
    private var mVector = PointF(0.0f, 0.0f)

    private var mSrcBitmap: Bitmap? = null
    private var mActualBitmap: Bitmap? = null
    private var mColor: Int = Color.RED
    private var mColorWeight = 0.2
    private var mChangeColor = false

    private var mPixelMapTable = doubleArrayOf()

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event == null) {
            return super.onTouchEvent(event)
        }
        if (!liquidEditEnable) {
            return super.onTouchEvent(event)
        }
        Log.d(TAG, "onTouchEvent: ${event.x}, ${event.y}")
        val action = event.action
        when (action) {
            MotionEvent.ACTION_DOWN -> {
                Log.d(TAG, "onCreate: MotionEvent.ACTION_DOWN")
                mCenter = PointF(event.x / width.toFloat(), event.y / height.toFloat())
            }

            MotionEvent.ACTION_MOVE -> {
                Log.d(TAG, "onCreate: MotionEvent.ACTION_MOVE")
                mVector = PointF((event.x / width.toFloat() - mCenter.x), (event.y / height.toFloat() - mCenter.y))
                updateLiquidEdit()
                mCenter = PointF(event.x / width.toFloat(), event.y / height.toFloat())
            }

            MotionEvent.ACTION_UP -> {
                Log.d(TAG, "onCreate: MotionEvent.ACTION_UP")
//                vector = Point((event.x - center.x).toInt(), (event.y - center.y).toInt())
//                doLiquify()
            }

            else -> Log.d(TAG, "onCreate: action:${action}")
        }
        return true
    }

    fun setRadius(radius: Int) {
        this.mRadius = radius
        this.mRadius2 = radius * radius
    }

    fun initPixelMapTable(width: Int, height: Int) {
        val coordinateSize = width * height * 2
        if (coordinateSize <= 0) {
            mPixelMapTable = doubleArrayOf()
        } else {
            mPixelMapTable = DoubleArray(coordinateSize) { index ->
                val offset = index / 2
                val c = index % 2

                if (c == 0) {
                    val x = offset % width
                    x.toDouble()
                } else {
                    val y = offset / width
                    y.toDouble()
                }
            }
        }
    }

    override fun setImageBitmap(bitmap: Bitmap?) {
        mSrcBitmap = bitmap
        if (mPixelMapTable.isNotEmpty()) {
            doLiquidEdit()
        } else {
            updateActualBitmap(bitmap)
        }
    }

    fun setPixelMapTable(mapTable: DoubleArray) {
        mPixelMapTable = mapTable
        val bitmap = mSrcBitmap
        if (bitmap != null) {
            doLiquidEdit()
        }
    }

    fun getPixelMapTable(): DoubleArray {
        return mPixelMapTable
    }

    private fun updateActualBitmap(bitmap: Bitmap?) {
        if (bitmap == null) {
            mActualBitmap = null
            super.setImageBitmap(null)
            return
        }

        if (!mChangeColor) {
            mActualBitmap = bitmap
            super.setImageBitmap(mActualBitmap)
            return
        }
        mActualBitmap = bitmap.changeColor(mColor, mColorWeight)
        super.setImageBitmap(mActualBitmap)
    }

    fun getSrcBitmap(): Bitmap? {
        return mSrcBitmap
    }

    fun getActualBitmap(): Bitmap? {
        return mActualBitmap
    }

    fun moveImage(x: Float, y: Float) {
        this.x += x
        this.y += y
    }

    fun setImagePosition(x: Float, y: Float) {
        this.x = x
        this.y = y
    }

    fun doLiquidEdit() {
        val srcBitmap = mSrcBitmap ?: return
        val newBitmap = srcBitmap.createMapped(mPixelMapTable, mUseLinearInterpolator)
        updateActualBitmap(newBitmap)
    }

    private fun updateLiquidEdit() {
        val srcBitmap = mSrcBitmap ?: return

        val centerX = (mCenter.x * srcBitmap.width).toInt()
        val centerY = (mCenter.y * srcBitmap.height).toInt()
        val vectorX = (mVector.x * srcBitmap.width).toInt()
        val vectorY = (mVector.y * srcBitmap.height).toInt()

        updatePixelMapTableOnLiquidEdit(mPixelMapTable, srcBitmap.width, srcBitmap.height, centerX, centerY, vectorX, vectorY)
        doLiquidEdit()
    }

    data class Pixel(val alpha: Int, val red: Int, val green: Int, val blue: Int)

    private fun Int.toPixel(): Pixel {
        return Pixel(
            (this shr 24) and 0xFF,
            (this shr 16) and 0xFF,
            (this shr 8) and 0xFF,
            this and 0xFF
        )
    }

    fun IntArray.getPixel(row: Int, col: Int, width: Int): Pixel {
        return (this[row * width + col]).toPixel()
    }

    fun changeColor(color: Int, weight: Double) {
        mColor = color
        mColorWeight = weight
        mChangeColor = true

        setImageBitmap(mSrcBitmap)
    }

    fun clearColor() {
        mChangeColor = false
        setImageBitmap(mSrcBitmap)
    }

    private fun updatePixelMapTableOnLiquidEdit(coordinates: DoubleArray, width: Int, height: Int, centerX: Int, centerY: Int, vectorX: Int, vectorY: Int) {
        for (y in 0 until height) {
            for (x in 0 until width) {
                handleCoordinateOnLiquidEdit(coordinates, width, height, x, y, centerX, centerY, vectorX, vectorY)
            }
        }
    }

    private fun handleCoordinateOnLiquidEdit(
        coordinates: DoubleArray, width: Int, height: Int, x: Int, y: Int,
        centerX: Int, centerY: Int, vectorX: Int, vectorY: Int
    ) {
        if (x < centerX - mRadius || x > centerX + mRadius || y < centerY - mRadius || y > centerY + mRadius) {
            // 在矩形范围之外不受影响， 矩形框加速判断
            return
        }
        val distance2 = (centerX - x) * (centerX - x) + (centerY - y) * (centerY - y)
//        Log.d(TAG, "handlePixel: distance2:${distance2}, width:${width}, height:${height}")
        if (distance2 > mRadius2) {
            // 在圆形范围外不受影响， d^2 > r^2 => d>r
            return
        }

        // distance <= radius
        val distance = Math.sqrt(distance2.toDouble())
//        Log.d(TAG, "handlePixel: distance:${distance}")
        if (distance > mRadius) {
            throw IllegalStateException("distance:${distance}")
        }

        // 影响强度根据距离从圆形到边界取值为 1~0
        val strength = 1 - distance / mRadius
//        Log.d(TAG, "handlePixel: strength:${strength}")
        if (strength < 0 || strength > 1.0) {
            throw IllegalStateException("strength:${strength}")
        }
        val offsetX = vectorX * strength
        val offsetY = vectorY * strength
//        Log.d(TAG, "handlePixel: offsetX:${offsetX}, offsetY:${offsetY}")

        val offset = y * width + x
        coordinates[offset * 2 + 0] -= offsetX
        coordinates[offset * 2 + 1] -= offsetY
    }

    private fun handleCoordinatesOnMove(coordinates: DoubleArray, width: Int, height: Int, deltaX: Float, deltaY: Float) {
        for (y in 0 until height) {
            for (x in 0 until width) {
                handleCoordinateOnMove(coordinates, width, height, x, y, deltaX, deltaY)
            }
        }
    }

    private fun handleCoordinateOnMove(coordinates: DoubleArray, width: Int, height: Int, x: Int, y: Int, deltaX: Float, deltaY: Float) {
        val offset = y * width + x
        coordinates[offset * 2 + 0] -= deltaX.toDouble()
        coordinates[offset * 2 + 1] -= deltaY.toDouble()
    }
}