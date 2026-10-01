package com.ethanstudio.snapsheet.ocr

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Nhận dạng chữ (bảng chữ Latin, gồm tiếng Việt và tiếng Anh) trên máy, không gửi ảnh đi đâu. */
object TextOcr {
    /**
     * Đọc lần lượt từng trang, trả về chữ của từng trang (cùng thứ tự). Gọi [onPage] (chỉ số từ 0)
     * trước khi đọc trang đó. File không còn thì trang đó là "". Hủy được giữa các trang.
     */
    suspend fun recognizePages(
        context: Context,
        pages: List<File>,
        onPage: suspend (index: Int) -> Unit,
    ): List<String> {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        try {
            return pages.mapIndexed { index, file ->
                currentCoroutineContext().ensureActive()
                onPage(index)
                recognizePage(context, recognizer, file)
            }
        } finally {
            recognizer.close()
        }
    }

    private suspend fun recognizePage(context: Context, recognizer: TextRecognizer, file: File): String {
        val image = withContext(Dispatchers.IO) {
            if (file.isFile) InputImage.fromFilePath(context, Uri.fromFile(file)) else null
        } ?: return ""
        return suspendCancellableCoroutine { cont ->
            recognizer.process(image)
                .addOnSuccessListener { if (cont.isActive) cont.resume(it.text) }
                .addOnFailureListener { if (cont.isActive) cont.resumeWithException(it) }
        }
    }
}
