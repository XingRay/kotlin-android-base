package com.github.xingray.kotlinandroidbase.camera

import com.github.xingray.kotlinbase.image.Image
import com.github.xingray.kotlinbase.image.ImageFormat

fun CameraImage.getInputImageFormat(): ImageFormat {
    return when (this.format) {
        Format.NV21 -> ImageFormat.NV21
        else -> throw IllegalArgumentException("unknown format")
    }
}

fun CameraImage.toInputImage(): Image {
    return Image(
        this.width,
        this.height,
        this.data,
        this.rotate,
        this.mirror,
        this.getInputImageFormat()
    )
}

fun CameraImage.createNewInputImage(): Image {
    return Image(
        this.width,
        this.height,
        this.data.copyOf(),
        this.rotate,
        this.mirror,
        this.getInputImageFormat()
    )
}


fun CameraImage.updateInputImage(image: Image) {
    image.width = this.width
    image.height = this.height
    image.data = this.data
    image.rotate = this.rotate
    image.mirror = this.mirror
    image.format = this.getInputImageFormat()
}

fun CameraImage.copyToInputImage(image: Image) {
    image.width = this.width
    image.height = this.height
    System.arraycopy(this.data, 0, image.data, 0, this.data.size)
    image.rotate = this.rotate
    image.mirror = this.mirror
    image.format = this.getInputImageFormat()
}

fun CameraImage.fillInputImage(image: Image) {
    image.width = this.width
    image.height = this.height
    image.data = this.data
    image.rotate = this.rotate
    image.mirror = this.mirror
    image.format = this.getInputImageFormat()
}

fun CameraImage.fillInputImageData(image: Image) {
    image.data = this.data
}