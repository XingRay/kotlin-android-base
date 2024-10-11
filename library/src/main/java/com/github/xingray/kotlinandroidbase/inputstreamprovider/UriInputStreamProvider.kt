package com.github.xingray.kotlinandroidbase.inputstreamprovider

import android.content.ContentResolver
import android.net.Uri
import com.github.xingray.kotlinandroidbase.inputstreamprovider.InputStreamProvider
import java.io.InputStream

class UriInputStreamProvider(private val contentResolver: ContentResolver, private val uri: Uri) : InputStreamProvider {
    override fun openInputStream(): InputStream? {
        return contentResolver.openInputStream(uri)
    }
}