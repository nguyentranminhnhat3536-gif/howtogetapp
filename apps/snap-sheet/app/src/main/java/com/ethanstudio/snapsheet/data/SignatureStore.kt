package com.ethanstudio.snapsheet.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/** Chữ ký đã vẽ: một ảnh PNG nền trong suốt trong bộ nhớ riêng của app (filesDir/signature/signature.png). */
class SignatureStore(context: Context) {
    val file: File = File(context.filesDir, "signature/signature.png")

    private val _version = MutableStateFlow(0L)

    /** Tăng sau mỗi lần lưu hoặc xóa chữ ký, để giao diện đọc lại ảnh. */
    val version: StateFlow<Long> = _version.asStateFlow()

    fun exists(): Boolean = file.isFile

    /** Cao/rộng của ảnh chữ ký đã lưu; chưa có hoặc không đọc được → null. */
    fun aspect(): Float? {
        if (!file.isFile) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        return bounds.outHeight.toFloat() / bounds.outWidth
    }

    /** Dựng ảnh từ các nét rồi lưu. Ghi ra file tạm rồi mới đổi tên, nên lỗi giữa chừng không làm hỏng chữ ký cũ. */
    suspend fun save(strokes: List<List<StrokePoint>>) {
        require(isSignatureUsable(strokes)) { "Signature is empty" }
        withContext(Dispatchers.IO) {
            val dir = file.parentFile ?: throw IOException("No signature folder")
            val tmp = File(dir, "${file.name}.tmp")
            try {
                if (!dir.isDirectory && !dir.mkdirs()) throw IOException("Cannot create ${dir.name}")
                val bitmap = renderSignature(strokes) ?: throw IOException("Signature is empty")
                try {
                    tmp.outputStream().use { out ->
                        if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) throw IOException("Cannot encode signature")
                    }
                } finally {
                    bitmap.recycle()
                }
                if (!tmp.renameTo(file)) {
                    file.delete()
                    if (!tmp.renameTo(file)) throw IOException("Cannot save signature")
                }
                _version.update { it + 1 }
            } catch (e: OutOfMemoryError) {
                throw IOException("Out of memory", e)
            } finally {
                tmp.delete()
            }
        }
    }

    /** Xóa chữ ký đã lưu. */
    suspend fun delete() {
        withContext(Dispatchers.IO) {
            file.delete()
            _version.update { it + 1 }
        }
    }
}
