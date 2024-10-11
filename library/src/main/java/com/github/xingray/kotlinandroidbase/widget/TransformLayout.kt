package com.github.xingray.kotlinandroidbase.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.github.xingray.kotlinandroidbase.R
import com.github.xingray.kotlinandroidbase.ext.containsPoint
import kotlin.math.max

class TransformLayout(context: Context, attrs: AttributeSet?) : ViewGroup(context, attrs) {

    private var mTargetView: View? = null
    var targetView: View?
        get() = mTargetView
        set(value) {
            // 移除之前的 targetView（如果有的话）
            if (mTargetView != null) {
                removeView(mTargetView)
            }
            // 设置新的 targetView
            mTargetView = value
            addView(mTargetView)

            requestLayout() // 重新布局，确保 targetView 被正确显示
        }

    /**
     * targetView 的位置（相对于父容器）变化后的回调
     * 回调参数 targetView 左上角在父容器中的位置
     *
     */
    var onPositionUpdatedListener: ((targetX: Float, targetY: Float) -> Unit)? = null

    /**
     * targetView 旋转角度发生变化的回调
     */
    var onRotateUpdatedListener: ((Float) -> Unit)? = null

    /**
     * targetView 的xy缩放变化的回调
     */
    var onScaleUpdatedListener: ((Float, Float) -> Unit)? = null

    // 添加缩放图标
    private val mScaleIcon: ImageView = ImageView(context)
    private val mRotateIcon: ImageView
    private val mOpenIcon: ImageView
    private val mTranslateIcon: ImageView

    private var mIsTouchScaleIcon = false
    private var mIsTouchRotateIcon = false
    private var mIsTouchCloseIcon = false
    private var mIsTouchTranslateIcon = false

    private var mLastTouchX: Float = 0f
    private var mLastTouchY: Float = 0f

    private var mTargetViewWidthMin = 20
    private var mTargetViewHeightMin = 20

    private var mOperatorIconWidth = 80
    private var mOperatorIconHeight = 80

    private var mEnableOperator = true
    private var mRotationRatio = 0.25f
    private var mCloseClickListener: ((View) -> Unit)? = null

    private var mOpen = true

    var mInitWidth = 0
    var mInitHeight = 0

    private var expandBitmap: Bitmap? = null
    private var translateBitmap: Bitmap? = null
    private var rotateBitmap: Bitmap? = null
    private var eyeOpenBitmap: Bitmap? = null
    private var eyeCloseBitmap: Bitmap? = null


    init {
        attrs?.let {
            val typedArray = context.obtainStyledAttributes(it, R.styleable.TransformLayout)

            val expandResId = typedArray.getResourceId(R.styleable.TransformLayout_iconExpand, 0)
            if (expandResId != 0) {
                setExpandResId(expandResId)
            }

            val translateResId = typedArray.getResourceId(R.styleable.TransformLayout_iconTranslate, 0)
            if (translateResId != 0) {
                setTranslateResId(translateResId)
            }

            val rotateResId = typedArray.getResourceId(R.styleable.TransformLayout_iconRotate, 0)
            if (rotateResId != 0) {
                setRotateResId(rotateResId)
            }

            val eyeOpenResId = typedArray.getResourceId(R.styleable.TransformLayout_iconEyeOpen, 0)
            if (eyeOpenResId != 0) {
                setEyeOpenResId(eyeOpenResId)
            }

            val eyeCloseResId = typedArray.getResourceId(R.styleable.TransformLayout_iconEyeClose, 0)
            if (eyeCloseResId != 0) {
                setEyeCloseResId(eyeCloseResId)
            }

            typedArray.recycle()
        }

        mScaleIcon.setImageBitmap(expandBitmap)
        mScaleIcon.layoutParams = LayoutParams(mOperatorIconWidth, mOperatorIconHeight)
        addView(mScaleIcon)

        // 添加旋转图标
        mRotateIcon = ImageView(context)
        mRotateIcon.setImageBitmap(rotateBitmap)
        mRotateIcon.layoutParams = LayoutParams(mOperatorIconWidth, mOperatorIconHeight)
        addView(mRotateIcon)

        // 添加旋转图标
        mOpenIcon = ImageView(context)
        showOpenIcon()
        mOpenIcon.layoutParams = LayoutParams(mOperatorIconWidth, mOperatorIconHeight)
        addView(mOpenIcon)

        // 添加旋转图标
        mTranslateIcon = ImageView(context)
        mTranslateIcon.setImageBitmap(translateBitmap)
        mTranslateIcon.layoutParams = LayoutParams(mOperatorIconWidth, mOperatorIconHeight)
        addView(mTranslateIcon)
    }

    // Set bitmap via resource ID
    fun setExpandResId(resId: Int) {
        val bitmap = BitmapFactory.decodeResource(resources, resId)
        setExpandBitmap(bitmap)
    }

    fun setTranslateResId(resId: Int) {
        val bitmap = BitmapFactory.decodeResource(resources, resId)
        setTranslateBitmap(bitmap)
    }

    fun setRotateResId(resId: Int) {
        val bitmap = BitmapFactory.decodeResource(resources, resId)
        setRotateBitmap(bitmap)
    }

    fun setEyeOpenResId(resId: Int) {
        val bitmap = BitmapFactory.decodeResource(resources, resId)
        setEyeOpenBitmap(bitmap)
    }

    fun setEyeCloseResId(resId: Int) {
        val bitmap = BitmapFactory.decodeResource(resources, resId)
        setEyeCloseBitmap(bitmap)
    }

    // Set bitmap directly
    fun setExpandBitmap(bitmap: Bitmap?) {
        expandBitmap = bitmap
        invalidate()  // Redraw view with new bitmap
    }

    fun setTranslateBitmap(bitmap: Bitmap?) {
        translateBitmap = bitmap
        invalidate()
    }

    fun setRotateBitmap(bitmap: Bitmap?) {
        rotateBitmap = bitmap
        invalidate()
    }

    fun setEyeOpenBitmap(bitmap: Bitmap?) {
        eyeOpenBitmap = bitmap
        invalidate()
    }

    fun setEyeCloseBitmap(bitmap: Bitmap?) {
        eyeCloseBitmap = bitmap
        invalidate()
    }

    fun setInitSize(width: Int, height: Int) {
        mInitWidth = width
        mInitHeight = height
    }

    public fun setOpen(open: Boolean) {
        mOpen = open
        showOpenIcon()
        if (open) {
            mTargetView?.visibility = View.VISIBLE
        } else {
            mTargetView?.visibility = View.INVISIBLE
        }
    }

    public fun toggleShowTarget() {
        setOpen(!mOpen)
    }

    private fun showOpenIcon() {
        if (mOpen) {
            mOpenIcon.setImageBitmap(eyeOpenBitmap)
        } else {
            mOpenIcon.setImageBitmap(eyeCloseBitmap)
        }
    }

    fun setCloseClickListener(listener: OnClickListener) {
        mCloseClickListener = listener::onClick
    }

    fun setCloseClickListener(listener: (View) -> Unit) {
        mCloseClickListener = listener
    }

    var enableOperator
        get() = mEnableOperator
        set(value) {
            if (mEnableOperator == value) {
                return
            }
            mEnableOperator = value
            if (value) {
                mScaleIcon.visibility = View.VISIBLE
                mRotateIcon.visibility = View.VISIBLE
                mOpenIcon.visibility = View.VISIBLE
                mTranslateIcon.visibility = View.VISIBLE
            } else {
                mScaleIcon.visibility = View.INVISIBLE
                mRotateIcon.visibility = View.INVISIBLE
                mOpenIcon.visibility = View.INVISIBLE
                mTranslateIcon.visibility = View.INVISIBLE
            }
        }

    var operatorIconHeight
        get() = mOperatorIconHeight
        set(value) {
            mOperatorIconHeight = value
        }

    var operatorIconWidth
        get() = mOperatorIconWidth
        set(value) {
            mOperatorIconWidth = value
        }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
//        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        // 获取子视图数量
        val childCount = childCount

        // 如果没有设置 targetView 并且有子视图，则将第一个子视图作为 targetView
        if (mTargetView == null && childCount > 4) {
            mTargetView = getChildAt(4)
        }

        // 手动测量子视图
        measureChild(mTargetView, MeasureSpec.EXACTLY, MeasureSpec.EXACTLY)

        measureChild(mScaleIcon, MeasureSpec.EXACTLY, MeasureSpec.EXACTLY)
        measureChild(mRotateIcon, MeasureSpec.EXACTLY, MeasureSpec.EXACTLY)
        measureChild(mOpenIcon, MeasureSpec.EXACTLY, MeasureSpec.EXACTLY)
        measureChild(mTranslateIcon, MeasureSpec.EXACTLY, MeasureSpec.EXACTLY)

        val targetViewWidth = max(mTargetView?.measuredWidth ?: 0, mTargetViewWidthMin)
        val targetViewHeight = max(mTargetView?.measuredHeight ?: 0, mTargetViewHeightMin)

        setMeasuredDimension(
            targetViewWidth + 2 * mOperatorIconWidth,
            targetViewHeight + 2 * mOperatorIconHeight
        )
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        mTargetView?.layout(
            mOperatorIconWidth,
            mOperatorIconHeight,
            r - mOperatorIconWidth,
            b - mOperatorIconHeight
        )

        // 关闭图标在左上角
//        Log.d(TAG, "onLayout: mRotateIcon.measuredWidth:${mCloseIcon.measuredWidth}, mRotateIcon.measuredHeight:${mCloseIcon.measuredHeight}")
        mOpenIcon.layout(
            0,
            0,
            mOpenIcon.measuredWidth,
            mOpenIcon.measuredHeight
        )

        // 旋转图标在右上角
//        Log.d(TAG, "onLayout: mCloseIcon.measuredWidth:${mRotateIcon.measuredWidth}, mCloseIcon.measuredHeight:${mRotateIcon.measuredHeight}")
        mRotateIcon.layout(
            r - mRotateIcon.measuredWidth,
            0,
            r,
            mRotateIcon.measuredHeight
        )

        // 平移图标在左下角
//        Log.d(TAG, "onLayout: mCloseIcon.measuredWidth:${mTranslateIcon.measuredWidth}, mCloseIcon.measuredHeight:${mTranslateIcon.measuredHeight}")
        mTranslateIcon.layout(
            0,
            b - mTranslateIcon.measuredHeight,
            mTranslateIcon.measuredWidth,
            b
        )

        // 缩放图标在右下角
//        Log.d(TAG, "onLayout: mScaleIcon.measuredWidth:${mScaleIcon.measuredWidth}, mScaleIcon.measuredHeight:${mScaleIcon.measuredHeight}")
        mScaleIcon.layout(
            r - mScaleIcon.measuredWidth,
            b - mScaleIcon.measuredHeight,
            r,
            b
        )

    }

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        // 拦截触摸事件，确保父视图能够收到触摸事件
        return true
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (!mEnableOperator) {
            return super.onTouchEvent(event)
        }
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                // 记录触摸起始点
                mLastTouchX = event.rawX
                mLastTouchY = event.rawY

                mIsTouchCloseIcon = mOpenIcon.containsPoint(event.x, event.y)
                mIsTouchRotateIcon = mRotateIcon.containsPoint(event.x, event.y)
//                mIsTouchTranslateIcon = mTranslateIcon.containsPoint(event.x, event.y)
                mIsTouchScaleIcon = mScaleIcon.containsPoint(event.x, event.y)
            }

            MotionEvent.ACTION_MOVE -> {
                if (mIsTouchCloseIcon) {
                    return true
                }
                if (mIsTouchRotateIcon) {
                    rotateOnTouchEvent(event)
                    return true
                }
//                if (mIsTouchTranslateIcon) {
//                    return true
//                }
                if (mIsTouchScaleIcon) {
                    scaleOnTouchEvent(event)
                    return true
                }

                val rawX = event.rawX
                val rawY = event.rawY

                // 根据触摸移动的距离，调整组合控件的位置
                val deltaX = rawX - mLastTouchX
                val deltaY = rawY - mLastTouchY

                x += deltaX
                y += deltaY

                mLastTouchX = rawX
                mLastTouchY = rawY

                onPositionUpdatedListener?.invoke(x + mOperatorIconWidth, y + mOperatorIconHeight)

                // 请求重新布局
                requestLayout()
            }

            MotionEvent.ACTION_UP -> {
                if (mIsTouchCloseIcon && mOpenIcon.containsPoint(event.x, event.y)) {
                    mCloseClickListener?.invoke(this)
                }
                mIsTouchCloseIcon = false
                mIsTouchRotateIcon = false
                mIsTouchTranslateIcon = false
                mIsTouchScaleIcon = false
            }
        }
        return true
    }

    private fun rotateOnTouchEvent(event: MotionEvent) {
        val targetView = mTargetView ?: let {
            return
        }

        val rawX = event.rawX
        val rawY = event.rawY

        // 根据触摸移动的距离，调整组合控件的位置
        val deltaX = rawX - mLastTouchX
        val deltaY = rawY - mLastTouchY

        mLastTouchX = rawX
        mLastTouchY = rawY

        val rotate = mRotationRatio * (deltaX + deltaY)
        targetView.rotation += rotate
        onRotateUpdatedListener?.invoke(targetView.rotation)
        // 请求重新布局
//        requestLayout()
    }

    private fun scaleOnTouchEvent(event: MotionEvent) {
        val targetView = mTargetView ?: let {
            return
        }

        val rawX = event.rawX
        val rawY = event.rawY

        // 根据触摸移动的距离，调整组合控件的位置
        val deltaX = rawX - mLastTouchX
        val deltaY = rawY - mLastTouchY

        mLastTouchX = rawX
        mLastTouchY = rawY

        // 计算布局宽高, 不能小于最小值
        val targetViewNewWidth = max(targetView.width + deltaX.toInt(), mTargetViewWidthMin)
        val targetViewNewHeight = max(targetView.height + deltaY.toInt(), mTargetViewHeightMin)

        // 更新 targetView 的宽高
        val layoutParams = targetView.layoutParams
        layoutParams.width = targetViewNewWidth
        layoutParams.height = targetViewNewHeight
        targetView.layoutParams = layoutParams

        onScaleUpdatedListener?.invoke(targetViewNewWidth / mInitWidth.toFloat(), targetViewNewHeight / mInitHeight.toFloat())

        // 请求重新布局
        requestLayout()
    }

    fun setTargetViewSize(width: Int, height: Int) {
        val targetView = mTargetView ?: let {
            return
        }

        val targetViewNewWidth = max(width, mTargetViewWidthMin)
        val targetViewNewHeight = max(height, mTargetViewHeightMin)

        val targetViewLayoutParams = targetView.layoutParams
        // 设置控件的宽高，确保不小于一个最小值（例如50）
        targetViewLayoutParams.width = targetViewNewWidth
        targetViewLayoutParams.height = targetViewNewHeight

        targetView.layoutParams = targetViewLayoutParams

        val layoutParams = this.layoutParams
        layoutParams.width = targetViewNewWidth + operatorIconWidth * 2;
        layoutParams.height = targetViewNewHeight + operatorIconHeight * 2;
        this.layoutParams = layoutParams
    }

    fun setPositionByTargetViewInParent(x: Float, y: Float) {
        this.x = x - mOperatorIconWidth
        this.y = y - mOperatorIconHeight
        requestLayout()
    }
}