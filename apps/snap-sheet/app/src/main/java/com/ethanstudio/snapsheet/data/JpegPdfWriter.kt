package com.ethanstudio.snapsheet.data

import java.io.OutputStream

/** Nguồn byte của một ảnh JPEG; chỉ đọc khi tới lượt trang đó để không giữ mọi ảnh trong bộ nhớ. */
fun interface JpegSource {
    fun read(): ByteArray
}

/**
 * Ghi PDF 1.4 nhúng thẳng byte JPEG (/DCTDecode), mỗi ảnh một trang rộng 595 điểm (khổ A4).
 * Khác PdfDocument của Android (nhúng ảnh không nén mất dữ liệu nên file to), file ra gần bằng tổng dung lượng ảnh.
 * Chỉ dùng java.io nên chạy được trong test JVM.
 */
object JpegPdfWriter {
    const val PAGE_WIDTH = 595

    /** Chiều cao trang (điểm), giống PdfBuilder. */
    fun pageHeight(widthPx: Int, heightPx: Int): Int =
        (PAGE_WIDTH.toLong() * heightPx / widthPx).toInt().coerceAtLeast(1)

    /**
     * Ghi PDF, mỗi JPEG một trang. Đọc từng ảnh một.
     * Danh sách rỗng, ảnh không đọc được kích thước, hoặc số kênh màu khác 1 và 3 → IllegalArgumentException.
     */
    fun write(pages: List<JpegSource>, out: OutputStream) {
        require(pages.isNotEmpty()) { "No pages" }
        val pdf = CountingStream(out)
        val total = 2 + pages.size * 3
        val offsets = LongArray(total + 1)

        pdf.ascii("%PDF-1.4\n")
        pdf.bytes(byteArrayOf('%'.code.toByte(), 0xE2.toByte(), 0xE3.toByte(), 0xCF.toByte(), 0xD3.toByte(), '\n'.code.toByte()))

        offsets[1] = pdf.count
        pdf.obj(1, "<< /Type /Catalog /Pages 2 0 R >>")
        offsets[2] = pdf.count
        val kids = pages.indices.joinToString(" ") { "${3 + 3 * it} 0 R" }
        pdf.obj(2, "<< /Type /Pages /Kids [$kids] /Count ${pages.size} >>")

        pages.forEachIndexed { i, source ->
            val jpeg = source.read()
            val info = readJpegInfo(jpeg) ?: throw IllegalArgumentException("Not a JPEG (page ${i + 1})")
            val colorSpace = when (info.components) {
                1 -> "/DeviceGray"
                3 -> "/DeviceRGB"
                else -> throw IllegalArgumentException("Unsupported JPEG colors (page ${i + 1})")
            }
            val pageObj = 3 + 3 * i
            val contentObj = pageObj + 1
            val imageObj = pageObj + 2
            val height = pageHeight(info.width, info.height)

            offsets[pageObj] = pdf.count
            pdf.obj(
                pageObj,
                "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 $PAGE_WIDTH $height] " +
                    "/Resources << /XObject << /Im0 $imageObj 0 R >> >> /Contents $contentObj 0 R >>",
            )

            val content = "q\n$PAGE_WIDTH 0 0 $height 0 0 cm\n/Im0 Do\nQ\n".toByteArray(Charsets.ISO_8859_1)
            offsets[contentObj] = pdf.count
            pdf.streamObj(contentObj, "<< /Length ${content.size} >>", content)

            offsets[imageObj] = pdf.count
            pdf.streamObj(
                imageObj,
                "<< /Type /XObject /Subtype /Image /Width ${info.width} /Height ${info.height} " +
                    "/ColorSpace $colorSpace /BitsPerComponent 8 /Filter /DCTDecode /Length ${jpeg.size} >>",
                jpeg,
            )
        }

        val xref = pdf.count
        pdf.ascii("xref\n0 ${total + 1}\n0000000000 65535 f \n")
        for (n in 1..total) pdf.ascii(String.format(java.util.Locale.ROOT, "%010d 00000 n \n", offsets[n]))
        pdf.ascii("trailer\n<< /Size ${total + 1} /Root 1 0 R >>\nstartxref\n$xref\n%%EOF\n")
        pdf.flush()
    }

    private fun CountingStream.obj(n: Int, body: String) {
        ascii("$n 0 obj\n$body\nendobj\n")
    }

    private fun CountingStream.streamObj(n: Int, dict: String, data: ByteArray) {
        ascii("$n 0 obj\n$dict\nstream\n")
        bytes(data)
        ascii("\nendstream\nendobj\n")
    }

    /** Bọc OutputStream, đếm số byte đã ghi để tính vị trí từng object cho bảng xref. */
    private class CountingStream(private val out: OutputStream) {
        var count = 0L
            private set

        fun bytes(data: ByteArray) {
            out.write(data)
            count += data.size
        }

        fun ascii(text: String) = bytes(text.toByteArray(Charsets.ISO_8859_1))

        fun flush() = out.flush()
    }
}
