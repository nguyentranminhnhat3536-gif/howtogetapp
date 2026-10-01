package com.ethanstudio.snapsheet.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import com.ethanstudio.snapsheet.data.sampleSizeFor
import java.io.File

/** Tìm Activity chứa Context này (Context trong Compose có thể bị bọc nhiều lớp). */
tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/** Bộ nhớ đệm ảnh đã thu nhỏ, tối đa khoảng 1/8 bộ nhớ của app. Khóa gồm đường dẫn, cỡ và lần sửa file. */
private val bitmapCache = object : LruCache<String, Bitmap>((Runtime.getRuntime().maxMemory() / 8 / 1024).toInt()) {
    override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / 1024
}

/** Như [decodeSampled] nhưng dùng lại ảnh đã đọc. */
fun decodeSampledCached(file: File, target: Int): Bitmap? {
    val key = "${file.path}|$target|${file.lastModified()}"
    bitmapCache.get(key)?.let { return it }
    return decodeSampled(file, target)?.also { bitmapCache.put(key, it) }
}

/** Đọc ảnh đã thu nhỏ để hiển thị nhẹ máy; cạnh dài nhất xấp xỉ [target] điểm ảnh. Null nếu đọc lỗi. */
fun decodeSampled(file: File, target: Int): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.path, bounds)
    if (bounds.outWidth <= 0) return null
    val options = BitmapFactory.Options().apply {
        inSampleSize = sampleSizeFor(maxOf(bounds.outWidth, bounds.outHeight), target)
    }
    return BitmapFactory.decodeFile(file.path, options)
}
