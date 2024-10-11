package com.github.xingray.kotlinandroidbase.ext

import androidx.exifinterface.media.ExifInterface
import java.io.File

fun File.updateExifOrientation(rotate: Int, mirror: Boolean) {
    val exif = ExifInterface(this)

    val orientation = when (rotate) {
        0 -> {
            if (mirror) {
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL
            } else {
                ExifInterface.ORIENTATION_NORMAL
            }
        }

        90 -> {
            if (mirror) {
                ExifInterface.ORIENTATION_TRANSVERSE
            } else {
                ExifInterface.ORIENTATION_ROTATE_90
            }
        }

        180 -> {
            if (mirror) {
                ExifInterface.ORIENTATION_FLIP_VERTICAL
            } else {
                ExifInterface.ORIENTATION_ROTATE_180
            }
        }

        270 -> {
            if (mirror) {
                ExifInterface.ORIENTATION_TRANSPOSE
            } else {
                ExifInterface.ORIENTATION_ROTATE_270
            }
        }

        else -> {
            ExifInterface.ORIENTATION_UNDEFINED
        }
    }

    exif.setAttribute(ExifInterface.TAG_ORIENTATION, orientation.toString())
    exif.saveAttributes()
}
