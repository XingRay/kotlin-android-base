package com.github.xingray.kotlinandroidbase.inputstreamprovider

import java.io.InputStream

interface InputStreamProvider {
    fun openInputStream(): InputStream?
}