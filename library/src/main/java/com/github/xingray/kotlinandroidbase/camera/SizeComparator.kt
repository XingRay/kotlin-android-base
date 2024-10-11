package com.github.xingray.kotlinandroidbase.camera

import android.util.Size

class SizeComparator : Comparator<Size> {
    override fun compare(size1: Size, size2: Size): Int {
        return if (size1.width > size2.width) {
            if (size1.height >= size2.height) {
                1
            } else {
                java.lang.Long.signum(size1.width.toLong() * size1.height - size2.width.toLong() * size2.height)
            }
        } else if (size1.width < size2.width) {
            if (size1.height <= size2.height) {
                -1
            } else {
                java.lang.Long.signum(size1.width.toLong() * size1.height - size2.width.toLong() * size2.height)
            }
        } else {
            size1.height - size2.height
        }
    }
}