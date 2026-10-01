package com.ethanstudio.snapsheet.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Nút "Mở liên kết" sau khi quét mã chỉ hiện với link web http/https an toàn. */
class QrTextTest {

    @Test
    fun httpsLinkWithPathAndQueryIsAccepted() {
        assertEquals("https://example.com/a?b=1", safeWebUrl("https://example.com/a?b=1"))
    }

    @Test
    fun schemeIgnoresCase() {
        assertEquals("HTTP://EXAMPLE.COM", safeWebUrl("HTTP://EXAMPLE.COM"))
        assertEquals("HtTpS://Example.com", safeWebUrl("HtTpS://Example.com"))
    }

    @Test
    fun spacesAroundLinkAreTrimmed() {
        assertEquals("https://x.vn", safeWebUrl("  https://x.vn  "))
        assertEquals("https://x.vn", safeWebUrl("\nhttps://x.vn\t"))
    }

    @Test
    fun hostWithPortAndFragmentIsAccepted() {
        assertEquals("http://192.168.1.1:8080/x", safeWebUrl("http://192.168.1.1:8080/x"))
        assertEquals("https://example.com#top", safeWebUrl("https://example.com#top"))
    }

    @Test
    fun dangerousSchemesAreRejected() {
        assertNull(safeWebUrl("javascript:alert(1)"))
        assertNull(safeWebUrl("intent://scan/#Intent;end"))
        assertNull(safeWebUrl("file:///sdcard/a.pdf"))
        assertNull(safeWebUrl("ftp://example.com"))
        assertNull(safeWebUrl("JAVASCRIPT:alert(1)"))
    }

    @Test
    fun linkWithoutHostIsRejected() {
        assertNull(safeWebUrl("https://"))
        assertNull(safeWebUrl("https:///path"))
        assertNull(safeWebUrl("https://?q=1"))
        assertNull(safeWebUrl("http://#x"))
    }

    @Test
    fun spaceInsideLinkIsRejected() {
        assertNull(safeWebUrl("https://a b.com"))
        assertNull(safeWebUrl("https://example.com/a b"))
        assertNull(safeWebUrl("https://example.com\nhttps://evil.example"))
    }

    @Test
    fun emptyOrPlainTextIsRejected() {
        assertNull(safeWebUrl(""))
        assertNull(safeWebUrl("   "))
        assertNull(safeWebUrl("www.example.com"))
        assertNull(safeWebUrl("http:/example.com"))
        assertNull(safeWebUrl("8934563138165"))
    }

    @Test
    fun linkUpTo2048CharsIsAccepted() {
        val url = "https://" + "a".repeat(2040)
        assertEquals(2048, url.length)
        assertEquals(url, safeWebUrl(url))
    }

    @Test
    fun linkOver2048CharsIsRejected() {
        assertNull(safeWebUrl("https://" + "a".repeat(2041)))
        assertNull(safeWebUrl("https://" + "a".repeat(3000)))
    }
}
