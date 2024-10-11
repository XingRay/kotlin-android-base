package com.github.xingray.kotlinandroidbase.opengl

import android.opengl.GLSurfaceView
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner

class RendererLifecycleEventObserver(gLSurfaceView: GLSurfaceView, openGLRenderer: OpenGLRenderer) : LifecycleEventObserver {

    companion object {
        @JvmStatic
        private val TAG = RendererLifecycleEventObserver::class.java.simpleName
    }

    private val mGLSurfaceView: GLSurfaceView = gLSurfaceView
    private val mOpenGLRenderer: OpenGLRenderer = openGLRenderer

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
        mGLSurfaceView.setEGLContextClientVersion(2)
        mGLSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0)
        mGLSurfaceView.setRenderer(mOpenGLRenderer)
        mGLSurfaceView.renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
    }

    private fun onStart() {
        Log.d(TAG, "onStart: ")
    }

    private fun onResume() {
        Log.d(TAG, "onResume: ")
        mGLSurfaceView.onResume()
    }

    private fun onPause() {
        Log.d(TAG, "onPause: ")
        mGLSurfaceView.queueEvent {
            mOpenGLRenderer.onSurfaceDestroy()
        }
        mGLSurfaceView.onPause()
    }

    private fun onStop() {
        Log.d(TAG, "onStop: ")
    }

    private fun onDestroy() {
        Log.d(TAG, "onDestroy: ")
        mOpenGLRenderer.release()
    }
}