package com.github.xingray.kotlinandroidbase.ext

import android.content.res.AssetManager
import android.text.TextUtils
import android.util.Log
import com.github.xingray.kotlinbase.ext.io.readFloatArray
import com.github.xingray.kotlinbase.ext.io.readIntArray
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.util.LinkedList


private val TAG = "AssetManagerExtension"

fun AssetManager.readIntArray(fileName: String, numbersPerLine: Int = 3): IntArray {
    val inputStream = this.open(fileName)
    return BufferedReader(InputStreamReader(inputStream)).use { reader ->
        reader.readIntArray(numbersPerLine)
    }
}

fun AssetManager.readFloatArray(fileName: String, numbersPerLine: Int = 2): FloatArray {
    val inputStream = this.open(fileName)
    return BufferedReader(InputStreamReader(inputStream)).use { reader ->
        reader.readFloatArray(numbersPerLine)
    }
}

fun AssetManager.copyAssetFolder(
    srcPath: String, dstPath: String
): Boolean {
    val fileList = this.list(srcPath) ?: run {
        Log.e(TAG, "AssetUtil#AssetManager.copyAssetFolder: srcPath: ${srcPath} is empty")
        return false
    }

    if (fileList.isEmpty()) {
        Log.i(TAG, "copyAssetFolder: fileList of ${srcPath} is empty, try copy as file")
        return copyAssetFile(srcPath, dstPath)
    }

    val dstDir = File(dstPath)
    if (dstDir.exists()) {
        Log.i(TAG, "copyAssetFolder: dstDir:${dstDir} exists")
        if (dstDir.isFile) {
            Log.i(TAG, "copyAssetFolder: dstDir:${dstDir} is file")
            val deleteFileSuccess = dstDir.delete()
            if (!deleteFileSuccess) {
                Log.e(TAG, "copyAssetFolder: delete file:${dstDir} failed")
                return false
            }
        } else if (dstDir.isDirectory) {
            Log.i(TAG, "copyAssetFolder: dstDir:${dstDir} is folder")
        }
    } else {
        val createSuccess = dstDir.mkdirs()
        if (!createSuccess) {
            Log.e(TAG, "copyAssetFolder: mkdirs: ${dstPath} failed")
            return false
        }
    }

    for (filename in fileList) {
        val copySuccess = copyAssetFolder(
            srcPath + File.separator.toString() + filename,
            dstPath + File.separator.toString() + filename
        )
        if (!copySuccess) {
            return false
        }
    }

    return true
}

fun AssetManager.copyAssetFile(srcName: String, dstName: String): Boolean {
    if (isFileExists(dstName)) {
        return true
    }

    try {
        this.open(srcName).use { inStream ->
            File(dstName).outputStream().use { out ->
                val buffer = ByteArray(4096)
                var read: Int
                while (inStream.read(buffer).also { read = it } != -1) {
                    out.write(buffer, 0, read)
                }
            }
        }
        return true
    } catch (e: IOException) {
        // 处理异常
        Log.e(TAG, "copyAssetFile: copy srcFile:${srcName} to dstFile:$dstName failed")
        return false
    }
}

fun AssetManager.extractAssetFile(assetFilePath: String, dstFile: File) {
    this.open(assetFilePath).use { inputStream ->
        dstFile.outputStream().use { outputStream ->
            inputStream.copyTo(outputStream)
        }
    }
}

fun String.getFileName():String{
    return this.substring(this.lastIndexOf("/")+1)
}

fun AssetManager.copyAssetFile(srcName: String, dstFile: File): Boolean {
    if (isFileExists(dstFile)) {
        return true
    }

    try {
        this.open(srcName).use { inStream ->
            dstFile.outputStream().use { out ->
                val buffer = ByteArray(4096)
                var read: Int
                while (inStream.read(buffer).also { read = it } != -1) {
                    out.write(buffer, 0, read)
                }
            }
        }
        return true
    } catch (e: IOException) {
        // 处理异常
        Log.e(TAG, "copyAssetFile: copy srcFile:${srcName} to dstFile:${dstFile.name} failed")
        return false
    }
}

fun isFileExists(path: String?): Boolean {
    if (path == null) {
        return false
    }
    return TextUtils.isEmpty(path) || File(path).exists()
}

fun isFileExists(file: File?): Boolean {
    return file != null && file.exists()
}

fun AssetManager.copy(srcDirPath: String, dstDir: File, progressCallback: ((progress: Int, total: Int, file: File) -> Unit)? = null): Boolean {
    val fileList = this.list(srcDirPath) ?: run {
        Log.e(TAG, "AssetUtil#AssetManager.copyAssetFolder: srcPath: ${srcDirPath} is empty")
        return false
    }

    if (fileList.isEmpty()) {
        Log.i(TAG, "copyAssetFolder: fileList of ${srcDirPath} is empty, try copy as file")
        return copyAssetFile(srcDirPath, dstDir)
    }

    if (dstDir.exists()) {
        Log.i(TAG, "copyAssetFolder: dstDir:${dstDir} exists")
        if (dstDir.isFile) {
            Log.i(TAG, "copyAssetFolder: dstDir:${dstDir} is file")
            val deleteFileSuccess = dstDir.delete()
            if (!deleteFileSuccess) {
                Log.e(TAG, "copyAssetFolder: delete file:${dstDir} failed")
                return false
            }
        } else if (dstDir.isDirectory) {
            Log.i(TAG, "copyAssetFolder: dstDir:${dstDir} is folder")
        }
    } else {
        val createSuccess = dstDir.mkdirs()
        if (!createSuccess) {
            Log.e(TAG, "copyAssetFolder: mkdirs: ${dstDir.name} failed")
            return false
        }
    }

    val pathList = listRecursively(srcDirPath)
    if (pathList.isEmpty()) {
        return false
    }

    val total = pathList.size
    pathList.forEachIndexed { index, path ->
        val target = File(dstDir, path)
        copyFileOrMakeDir(path, target)
        progressCallback?.invoke(index + 1, total, target)
    }

    return true
}

fun AssetManager.copyFileOrMakeDir(path: String, target: File) {
    if (target.exists()) {
        return
    }

    val inputStream: InputStream
    try {
        inputStream = this.open(path)
    } catch (e: IOException) {
        // 可能是目录
        target.mkdirs()
        return
    }

    try {
        inputStream.use { inStream ->
            target.outputStream().use { out ->
                val buffer = ByteArray(4096)
                var read: Int
                while (inStream.read(buffer).also { read = it } != -1) {
                    out.write(buffer, 0, read)
                }
            }
        }
    } catch (e: IOException) {
        Log.e(TAG, "copyAssetFile: copy srcFile:${path} to dstFile:${target.name} failed")
    }
}

fun AssetManager.listRecursively(path: String): List<String> {
    val srcPathList = LinkedList<String>()
    val targetPathList = mutableListOf<String>()
    srcPathList.add(path)

    do {
        val srcPath = srcPathList.pop()
        targetPathList.add(srcPath)
        val subPathList = list(srcPath)?.map { subPath ->
            srcPath + File.separator + subPath
        }?.toList()
        if (!subPathList.isNullOrEmpty()) {
            srcPathList.addAll(subPathList)
        }
    } while (srcPathList.isNotEmpty())

    return targetPathList
}