package com.ethanstudio.snapsheet.ocr

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Nhận dạng chữ (bảng chữ Latin, gồm tiếng Việt và tiếng Anh) trên máy, không gửi ảnh đi đâu. */
object TextOcr {
    suspend fun recognize(context: Context, pages: List<File>): String =
        pages.map { recognizePage(context, it) }.filter { it.isNotBlank() }.joinToString("\n\n")

    private suspend fun recognizePage(context: Context, file: File): String =
        suspendCancellableCoroutine { cont ->
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            val image = InputImage.fromFilePath(context, Uri.fromFile(file))
            recognizer.process(image)
                .addOnSuccessListener { if (cont.isActive) cont.resume(it.text) }
                .addOnFailureListener { if (cont.isActive) cont.resumeWithException(it) }
                .addOnCompleteListener { recognizer.close() }
        }
}
