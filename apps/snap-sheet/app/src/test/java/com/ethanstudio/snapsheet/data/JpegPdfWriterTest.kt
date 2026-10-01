package com.ethanstudio.snapsheet.data

import java.io.ByteArrayOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class JpegPdfWriterTest {

    /** JPEG giả: SOI + SOF0 + vài byte "ảnh" (giá trị [filler]) + EOI. Đủ để writer đọc kích thước. */
    private fun fakeJpeg(width: Int, height: Int, components: Int, filler: Int): ByteArray {
        val out = ByteArrayOutputStream()
        fun put(vararg values: Int) = values.forEach { out.write(it) }
        val length = 8 + 3 * components
        put(0xFF, 0xD8)
        put(0xFF, 0xC0, length shr 8, length and 0xFF, 8)
        put(height shr 8, height and 0xFF, width shr 8, width and 0xFF, components)
        repeat(components) { put(it + 1, 0x11, 0x00) }
        repeat(6) { put(filler) }
        put(0xFF, 0xD9)
        return out.toByteArray()
    }

    private val rgb = fakeJpeg(640, 480, 3, 0x41)
    private val gray = fakeJpeg(300, 600, 1, 0x42)

    private fun pdfBytes(pages: List<ByteArray>): ByteArray {
        val out = ByteArrayOutputStream()
        JpegPdfWriter.write(pages.map { bytes -> JpegSource { bytes } }, out)
        return out.toByteArray()
    }

    /** ISO-8859-1 đổi 1 byte thành đúng 1 ký tự, nên vị trí trong chuỗi = vị trí byte. */
    private fun latin1(bytes: ByteArray): String = String(bytes, Charsets.ISO_8859_1)

    private fun pdfText(pages: List<ByteArray>): String = latin1(pdfBytes(pages))

    private fun startXref(pdf: String): Int {
        val marker = "startxref\n"
        val at = pdf.lastIndexOf(marker)
        assertTrue("thiếu startxref", at >= 0)
        val end = pdf.indexOf('\n', at + marker.length)
        return pdf.substring(at + marker.length, end).toInt()
    }

    @Test
    fun pageHeightKeepsImageRatioOnA4Width() {
        assertEquals(1190, JpegPdfWriter.pageHeight(1000, 2000))
        assertEquals(842, JpegPdfWriter.pageHeight(595, 842))
        // 595 × 3000 / 4000 = 446,25 → 446
        assertEquals(446, JpegPdfWriter.pageHeight(4000, 3000))
    }

    @Test
    fun pageHeightIsAtLeastOnePoint() {
        assertEquals(1, JpegPdfWriter.pageHeight(100_000, 1))
    }

    @Test
    fun twoPagePdfHasHeaderPageCountAndEnd() {
        val bytes = pdfBytes(listOf(rgb, gray))
        val pdf = latin1(bytes)
        assertTrue(pdf.startsWith("%PDF-1.4\n"))
        // Dòng chú thích nhị phân ngay sau header.
        assertEquals('%'.code.toByte(), bytes[9])
        assertEquals(0xE2.toByte(), bytes[10])
        assertEquals(0xE3.toByte(), bytes[11])
        assertEquals(0xCF.toByte(), bytes[12])
        assertEquals(0xD3.toByte(), bytes[13])
        assertEquals('\n'.code.toByte(), bytes[14])
        assertTrue(pdf.contains("/Type /Catalog /Pages 2 0 R"))
        assertTrue(pdf.contains("/Kids [3 0 R 6 0 R]"))
        assertTrue(pdf.contains("/Count 2"))
        assertTrue(pdf.endsWith("%%EOF\n"))
    }

    @Test
    fun embedsEachJpegUnchangedInPageOrder() {
        val pdf = pdfText(listOf(rgb, gray))
        var previous = -1
        for (jpeg in listOf(rgb, gray)) {
            val raw = latin1(jpeg)
            val at = pdf.indexOf(raw)
            assertTrue("không thấy nguyên byte JPEG", at >= 0)
            assertTrue("sai thứ tự trang", at > previous)
            val dictTail = "/Filter /DCTDecode /Length ${jpeg.size} >>\nstream\n"
            assertTrue("thiếu /DCTDecode hoặc /Length sai", pdf.substring(0, at).endsWith(dictTail))
            assertTrue("thiếu endstream", pdf.startsWith("\nendstream\nendobj\n", at + raw.length))
            previous = at
        }
    }

    @Test
    fun pageSizeAndColorFollowEachImage() {
        val pdf = pdfText(listOf(rgb, gray))
        // Trang 1: 640 × 480 → cao 595 × 480 / 640 = 446.
        assertTrue(pdf.contains("/MediaBox [0 0 595 446]"))
        assertTrue(pdf.contains("/Width 640 /Height 480 /ColorSpace /DeviceRGB /BitsPerComponent 8"))
        assertTrue(pdf.contains("/XObject << /Im0 5 0 R >>"))
        assertTrue(pdf.contains("/Contents 4 0 R"))
        // Trang 2: 300 × 600 → cao 1190, ảnh xám.
        assertTrue(pdf.contains("/MediaBox [0 0 595 1190]"))
        assertTrue(pdf.contains("/Width 300 /Height 600 /ColorSpace /DeviceGray /BitsPerComponent 8"))
        assertTrue(pdf.contains("/XObject << /Im0 8 0 R >>"))
        assertTrue(pdf.contains("/Contents 7 0 R"))
    }

    @Test
    fun contentStreamDrawsImageOverWholePageWithRightLength() {
        val pdf = pdfText(listOf(rgb))
        val content = "q\n595 0 0 446 0 0 cm\n/Im0 Do\nQ\n"
        assertTrue(pdf.contains("<< /Length ${content.length} >>\nstream\n$content\nendstream\nendobj\n"))
    }

    @Test
    fun xrefOffsetsPointToEachObject() {
        val pdf = pdfText(listOf(rgb, gray))
        val total = 2 + 3 * 2 // catalog + pages + (trang, nội dung, ảnh) mỗi trang
        val xref = startXref(pdf)
        val head = "xref\n0 ${total + 1}\n"
        assertTrue("startxref không trỏ tới xref", pdf.startsWith(head, xref))
        val first = xref + head.length
        assertEquals("0000000000 65535 f \n", pdf.substring(first, first + 20))
        val entry = Regex("\\d{10} 00000 n \n")
        for (n in 1..total) {
            val line = pdf.substring(first + 20 * n, first + 20 * (n + 1))
            assertTrue("dòng xref $n sai dạng: $line", entry.matches(line))
            val offset = line.take(10).toInt()
            assertTrue("object $n không nằm ở $offset", pdf.startsWith("$n 0 obj\n", offset))
        }
        // Mỗi dòng đúng 20 byte thì "trailer" nằm ngay sau dòng cuối.
        assertTrue(pdf.startsWith("trailer\n<< /Size ${total + 1} /Root 1 0 R >>\n", first + 20 * (total + 1)))
    }

    @Test
    fun singlePagePdfHasSixXrefEntries() {
        val pdf = pdfText(listOf(gray))
        assertTrue(pdf.contains("/Kids [3 0 R]"))
        assertTrue(pdf.contains("/Count 1"))
        assertTrue(pdf.startsWith("xref\n0 6\n", startXref(pdf)))
    }

    @Test
    fun readsEachPageOnceInOrder() {
        val reads = mutableListOf<Int>()
        val sources = listOf(rgb, gray, rgb).mapIndexed { i, bytes ->
            JpegSource {
                reads.add(i)
                bytes
            }
        }
        JpegPdfWriter.write(sources, ByteArrayOutputStream())
        assertEquals(listOf(0, 1, 2), reads)
    }

    // --- Ca phải thất bại ---

    @Test
    fun emptyPageListIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            JpegPdfWriter.write(emptyList(), ByteArrayOutputStream())
        }
    }

    @Test
    fun cmykJpegIsRejected() {
        val cmyk = fakeJpeg(100, 100, 4, 0x43)
        assertThrows(IllegalArgumentException::class.java) { pdfBytes(listOf(cmyk)) }
    }

    @Test
    fun nonJpegBytesAreRejected() {
        val notJpeg = "not a jpeg at all".toByteArray(Charsets.ISO_8859_1)
        assertThrows(IllegalArgumentException::class.java) { pdfBytes(listOf(notJpeg)) }
        // Trang hỏng nằm sau một trang tốt cũng bị bắt.
        assertThrows(IllegalArgumentException::class.java) { pdfBytes(listOf(rgb, notJpeg)) }
    }
}
