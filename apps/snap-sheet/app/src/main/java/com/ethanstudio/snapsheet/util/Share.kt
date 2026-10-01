package com.ethanstudio.snapsheet.util

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/** Địa chỉ (URI) để app khác đọc file trong thư mục riêng của app. */
fun Context.shareUri(file: File): Uri = FileProvider.getUriForFile(this, "$packageName.files", file)

/** Mở file PDF bằng app xem PDF của máy. Trả về false nếu máy không có app nào mở được. */
fun Context.viewPdf(file: File): Boolean {
    if (!file.isFile) return false
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(shareUri(file), "application/pdf")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    return startSafely(intent)
}

/** Chia sẻ một hoặc nhiều file (PDF hoặc ảnh JPEG). */
fun Context.shareFiles(files: List<File>, mime: String, chooserTitle: String): Boolean {
    if (files.isEmpty() || files.any { !it.isFile }) return false
    val uris = ArrayList(files.map { shareUri(it) })
    val intent = if (uris.size == 1) {
        Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_STREAM, uris[0])
    } else {
        Intent(Intent.ACTION_SEND_MULTIPLE).putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
    }
    intent.type = mime
    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    intent.clipData = ClipData.newRawUri(null, uris[0]).also { clip -> uris.drop(1).forEach { clip.addItem(ClipData.Item(it)) } }
    return startSafely(Intent.createChooser(intent, chooserTitle))
}

/** Chia sẻ một đoạn chữ. */
fun Context.shareText(text: String, chooserTitle: String): Boolean {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    return startSafely(Intent.createChooser(intent, chooserTitle))
}
