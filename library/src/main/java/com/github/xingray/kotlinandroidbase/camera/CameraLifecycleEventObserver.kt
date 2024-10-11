package com.github.xingray.kotlinandroidbase.camera

import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner

private val TAG = CameraLifecycleEventObserver::class.simpleName

class CameraLifecycleEventObserver(
    private val mCamera: Camera?,
    private val mPreviewView: View,
    private val mPreviewWidth: Int = 1080,
    private val mPreviewHeight: Int = 1920,
) : LifecycleEventObserver {

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        Log.d(TAG, "onStateChanged: event.targetState:${event.targetState}, event.name:${event.name}")
        when (event) {
            Lifecycle.Event.ON_CREATE -> onCreated()
            Lifecycle.Event.ON_START -> onStart()
            Lifecycle.Event.ON_RESUME -> onResume()
            Lifecycle.Event.ON_PAUSE -> onPause()
            Lifecycle.Event.ON_STOP -> onStop()
            Lifecycle.Event.ON_DESTROY -> onDestroy()
            else -> {}
        }
    }

    private fun onCreated() {
        Log.d(TAG, "onCreated: ")
    }

    private fun onResume() {
        Log.d(TAG, "onResume: ")
        val width = mPreviewView.width
        val height = mPreviewView.height

        if (width <= 0 || height <= 0) {
            mPreviewView.viewTreeObserver.addOnGlobalLayoutListener(mOnGlobalLayoutListener)
        } else {
            openCameraAndStartPreview()
        }
    }

    private fun onPause() {
        Log.d(TAG, "onPause: ")
        mCamera?.stopPreview()
        mCamera?.close()
    }

    private fun onStart() {
        Log.d(TAG, "onStart: ")
    }

    private fun onStop() {
        Log.d(TAG, "onStop: ")
    }

    private fun onDestroy() {
        Log.d(TAG, "onDestroy: ")
    }

    private val mOnGlobalLayoutListener: ViewTreeObserver.OnGlobalLayoutListener = ViewTreeObserver.OnGlobalLayoutListener {
        Log.d(TAG, "mOnGlobalLayoutListener: ViewTreeObserver.OnGlobalLayoutListener")
        removeListener()
        openCameraAndStartPreview()
    }

    private fun openCameraAndStartPreview() {
        Log.d(TAG, "mCamera?.setPreviewViewSize: width: $mPreviewWidth, height: $mPreviewHeight")
        mCamera?.setPreviewViewSize(mPreviewWidth, mPreviewHeight)
        mCamera?.open()
        mCamera?.startPreview()
    }

    private fun removeListener() {
        mPreviewView.viewTreeObserver.removeOnGlobalLayoutListener(mOnGlobalLayoutListener)
    }
}