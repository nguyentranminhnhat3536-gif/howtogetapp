package com.ethanstudio.snapsheet.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.ethanstudio.snapsheet.R
import com.ethanstudio.snapsheet.i18n.AppLanguages

/** DM Serif Display (SIL OFL 1.1), nhúng trong res/font. Chỉ có chữ Latin. */
private val DmSerifDisplay = FontFamily(Font(R.font.dm_serif_display))

/**
 * Font tiêu đề lớn (màn chào, màn mua). DM Serif khi ngôn ngữ đang hiện dùng chữ Latin không dấu phức tạp;
 * tiếng Việt, Nga, Nhật, Hàn, Hindi dùng font có chân của hệ thống để không vỡ dấu.
 */
@Composable
fun displaySerif(): FontFamily {
    val tag = LocalConfiguration.current.locales[0].toLanguageTag()
    return if (AppLanguages.usesBundledSerif(tag)) DmSerifDisplay else FontFamily.Serif
}
