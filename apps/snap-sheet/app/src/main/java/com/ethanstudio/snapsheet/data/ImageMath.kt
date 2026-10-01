package com.ethanstudio.snapsheet.data

/** Hệ số thu nhỏ (lũy thừa của 2) để cạnh dài nhất của ảnh không vượt quá [target] quá nhiều. */
fun sampleSizeFor(longSide: Int, target: Int): Int {
    var sample = 1
    while (longSide / (sample * 2) >= target) sample *= 2
    return sample
}

/** Góc cần xoay (độ) theo giá trị EXIF Orientation: 6 = 90°, 3 = 180°, 8 = 270°, còn lại 0. */
fun exifDegrees(orientation: Int): Int = when (orientation) {
    6 -> 90
    3 -> 180
    8 -> 270
    else -> 0
}
