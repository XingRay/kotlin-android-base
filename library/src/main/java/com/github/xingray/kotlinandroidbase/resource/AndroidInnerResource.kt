package com.github.xingray.kotlinandroidbase.resource

import android.content.res.Resources
import com.github.xingray.kotlinbase.resource.Resource
import java.io.InputStream

/**
 * res 目录下的资源, 通过 R.id.xxx 访问
 */
class AndroidInnerResource(val resources: Resources, val resId: Int) : Resource {
    override fun <R> use(consumer: (InputStream) -> R): R? {
        return resources.openRawResource(resId).use(consumer)
    }
}