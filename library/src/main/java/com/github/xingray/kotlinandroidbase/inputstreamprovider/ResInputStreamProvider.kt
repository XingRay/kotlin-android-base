package com.github.xingray.kotlinandroidbase.inputstreamprovider

import android.content.res.Resources
import com.github.xingray.kotlinandroidbase.inputstreamprovider.InputStreamProvider
import java.io.InputStream

class ResInputStreamProvider(private val resources: Resources, private val resId: Int) : InputStreamProvider {
    override fun openInputStream(): InputStream? {
        return resources.openRawResource(resId)
    }
}