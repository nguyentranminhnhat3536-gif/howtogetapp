package com.ethanstudio.snapsheet.data

import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import java.io.File

/** Ghép các ảnh JPEG thành một file PDF, mỗi ảnh một trang rộng 595 điểm (khổ A4). */
object PdfBuilder {
    private const val PAGE_WIDTH = 595

    fun build(pages: List<File>, out: File) {
        val pdf = PdfDocument()
        try {
            val paint = Paint(Paint.FILTER_BITMAP_FLAG)
            pages.forEachIndexed { index, file ->
                val bitmap = BitmapFactory.decodeFile(file.path) ?: error("Cannot read ${file.name}")
                try {
                    val height = (PAGE_WIDTH.toLong() * bitmap.height / bitmap.width).toInt().coerceAtLeast(1)
                    val info = PdfDocument.PageInfo.Builder(PAGE_WIDTH, height, index + 1).create()
                    val page = pdf.startPage(info)
                    page.canvas.drawBitmap(bitmap, null, Rect(0, 0, PAGE_WIDTH, height), paint)
                    pdf.finishPage(page)
                } finally {
                    bitmap.recycle()
                }
            }
            out.outputStream().use { pdf.writeTo(it) }
        } finally {
            pdf.close()
        }
    }
}
