package com.ethanstudio.snapsheet.util

import android.content.Context
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

/** Tên file gửi cho dịch vụ in: "<tên>.pdf", bỏ ký tự không hợp lệ, tên tối đa 80 ký tự; tên rỗng → "document.pdf". */
fun printJobFileName(name: String): String {
    val clean = name.trim()
        .map { c -> if (c in INVALID_NAME_CHARS || c.isISOControl()) '_' else c }
        .joinToString("")
        .take(80)
        .trim()
    return if (clean.isEmpty()) "document.pdf" else "$clean.pdf"
}

private const val INVALID_NAME_CHARS = "/\\:*?\"<>|"

/**
 * In file PDF có sẵn bằng PrintManager của hệ thống (không cần quyền).
 * Trả về false nếu không có Activity/PrintManager hoặc file không còn.
 */
fun Context.printPdf(file: File, docName: String): Boolean {
    if (!file.isFile) return false
    val activity = findActivity() ?: return false
    return try {
        val manager = activity.getSystemService(PrintManager::class.java) ?: return false
        manager.print(docName, PdfFilePrintAdapter(file, printJobFileName(docName)), null)
        true
    } catch (e: Exception) {
        false
    }
}

/** Đưa nguyên file PDF cho dịch vụ in; việc chép file chạy ngoài luồng chính. */
class PdfFilePrintAdapter(private val file: File, private val fileName: String) : PrintDocumentAdapter() {
    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes?,
        cancellationSignal: CancellationSignal?,
        callback: LayoutResultCallback,
        extras: Bundle?,
    ) {
        if (cancellationSignal?.isCanceled == true) {
            callback.onLayoutCancelled()
            return
        }
        val info = PrintDocumentInfo.Builder(fileName)
            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
            .setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
            .build()
        callback.onLayoutFinished(info, true)
    }

    override fun onWrite(
        pages: Array<out PageRange>?,
        destination: ParcelFileDescriptor,
        cancellationSignal: CancellationSignal?,
        callback: WriteResultCallback,
    ) {
        Thread {
            try {
                FileInputStream(file).use { input ->
                    FileOutputStream(destination.fileDescriptor).use { output -> input.copyTo(output) }
                }
                if (cancellationSignal?.isCanceled == true) {
                    callback.onWriteCancelled()
                } else {
                    callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                }
            } catch (e: IOException) {
                callback.onWriteFailed(e.message)
            }
        }.start()
    }
}
