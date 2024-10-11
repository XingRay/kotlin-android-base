package com.github.xingray.kotlinandroidbase.ext

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import java.io.File

fun Uri.getPathFromUri(context: Context): String? {
    val uri = this
    val contentResolver = context.contentResolver
    var filePath: String? = null
    val projection = arrayOf(MediaStore.MediaColumns.DATA)
    val cursor = contentResolver.query(uri, projection, null, null, null)
    cursor?.use {
        if (it.moveToFirst()) {
            val columnIndex = it.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)
            filePath = it.getString(columnIndex)
        }
    }
    return filePath
}
