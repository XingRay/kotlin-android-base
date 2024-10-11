package com.github.xingray.kotlinandroidbase.opengl

public interface OpenGLRenderer : android.opengl.GLSurfaceView.Renderer {
    public abstract fun onSurfaceDestroy(): kotlin.Unit

    public abstract fun release(): kotlin.Unit
}