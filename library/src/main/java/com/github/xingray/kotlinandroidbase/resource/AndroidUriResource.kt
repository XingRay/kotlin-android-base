package com.github.xingray.kotlinandroidbase.resource

import android.content.ContentResolver
import android.net.Uri
import com.github.xingray.kotlinbase.resource.Resource
import java.io.InputStream

/**
 * Android uri, 例如通过 intent 打开文件选择器返回的 content provider 的 uri
 */
class AndroidUriResource(private val contentResolver: ContentResolver, val uri: Uri) : Resource {
    override fun <R> use(consumer: (InputStream) -> R): R? {
        return contentResolver.openInputStream(uri)?.use(consumer)
    }
}