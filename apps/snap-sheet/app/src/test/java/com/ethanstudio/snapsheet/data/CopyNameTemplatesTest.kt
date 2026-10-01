package com.ethanstudio.snapsheet.data

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * Mẫu tên bản sao ("%1$s (signed)", "%1$s (watermark)") ở cả 12 ngôn ngữ phải có đúng một chỗ "%1$s".
 * Thiếu thì copyName báo lỗi và người dùng ở ngôn ngữ đó không lưu được bản đã ký / có chữ mờ.
 * Đọc thẳng file strings.xml (Gradle chạy unit test với thư mục làm việc là module app).
 */
class CopyNameTemplatesTest {
    private val keys = listOf("sign_copy_name", "wm_copy_name")

    private fun resDir(): File {
        val candidates = listOf("src/main/res", "app/src/main/res", "apps/snap-sheet/app/src/main/res")
        return candidates.map(::File).firstOrNull { File(it, "values/strings.xml").isFile }
            ?: run {
                fail("Không tìm thấy src/main/res từ thư mục ${File(".").absolutePath}")
                error("unreachable")
            }
    }

    private fun template(xml: String, key: String): String? =
        Regex("<string name=\"$key\"[^>]*>(.*?)</string>").find(xml)?.groupValues?.get(1)

    @Test
    fun everyLanguageHasBothTemplatesWithOneNameSlot() {
        val files = resDir().listFiles { f -> f.isDirectory && f.name.startsWith("values") }
            .orEmpty()
            .map { File(it, "strings.xml") }
            .filter { it.isFile }
        assertTrue("Chỉ thấy ${files.size} file strings.xml", files.size >= 12)
        for (file in files) {
            val xml = file.readText()
            for (key in keys) {
                val t = template(xml, key)
                val where = "${file.parentFile.name}/$key"
                assertNotNull("$where bị thiếu", t)
                assertEquals("$where = $t", 1, Regex("%1\\\$s").findAll(t!!).count())
                val name = copyName("Invoice", t)
                assertTrue("$where → $name", name.contains("Invoice"))
                assertTrue("$where → $name", name.length <= 80)
            }
        }
    }
}
