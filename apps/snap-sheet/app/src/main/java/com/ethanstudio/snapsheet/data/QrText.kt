package com.ethanstudio.snapsheet.data

/**
 * Link web được phép mở từ mã vừa quét: chỉ "http://" hoặc "https://" (không phân biệt hoa thường),
 * có tên miền, không có khoảng trắng, dài tối đa 2048 ký tự. Trả về chuỗi đã bỏ khoảng trắng hai đầu;
 * còn lại (javascript:, intent:, file:, "www.…" không có http) → null.
 */
fun safeWebUrl(raw: String): String? {
    val url = raw.trim()
    if (url.isEmpty() || url.length > QR_MAX_URL_LENGTH || url.any { it.isWhitespace() }) return null
    val rest = when {
        url.startsWith(HTTP, ignoreCase = true) -> url.substring(HTTP.length)
        url.startsWith(HTTPS, ignoreCase = true) -> url.substring(HTTPS.length)
        else -> return null
    }
    val host = rest.takeWhile { it != '/' && it != '?' && it != '#' }
    return if (host.isEmpty()) null else url
}

private const val QR_MAX_URL_LENGTH = 2048
private const val HTTP = "http://"
private const val HTTPS = "https://"
