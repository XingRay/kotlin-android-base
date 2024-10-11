package com.github.xingray.kotlinandroidbase.executor

import android.os.Handler
import java.util.concurrent.Executor

class HandlerExecutor(handler: Handler) : Executor {

    private val mHandler = handler

    override fun execute(command: Runnable?) {
        command?.let {
            mHandler.post(command)
        }
    }
}