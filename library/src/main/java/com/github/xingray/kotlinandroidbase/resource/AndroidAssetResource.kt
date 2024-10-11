package com.github.xingray.kotlinandroidbase.resource

import android.content.res.AssetManager
import com.github.xingray.kotlinbase.resource.Resource
import java.io.InputStream

/**
 * assets 目录下的文件, 通过 AssetManager 访问
 */
class AndroidAssetResource(val assets: AssetManager, val assetPath: String) : Resource {
    override fun <T> use(consumer: (InputStream) -> T): T? {
        return assets.open(assetPath).use(consumer)
    }
}