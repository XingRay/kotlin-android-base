package com.github.xingray.kotlinandroidbase.inputstreamprovider

import java.io.File
import java.io.InputStream

class FileInputStreamProvider(private val file: File) : InputStreamProvider {
    override fun openInputStream(): InputStream? {
        return file.takeIf { it.exists() && it.isFile }?.inputStream()
    }
}