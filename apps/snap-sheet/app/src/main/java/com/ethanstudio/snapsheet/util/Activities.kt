package com.ethanstudio.snapsheet.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.ethanstudio.snapsheet.data.sampleSizeFor
import java.io.File

/** Tìm Activity chứa Context này (Context trong Compose có thể bị bọc nhiều lớp). */
tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
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
