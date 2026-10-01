package com.ethanstudio.snapsheet.i18n

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.LocaleList
import java.util.Locale

/**
 * Ngôn ngữ riêng của app (Account › Language), không cần thư viện appcompat.
 *  - Android 13+: dùng LocaleManager của hệ thống (tự lưu, tự tạo lại Activity, khớp với Cài đặt › Ứng dụng › Ngôn ngữ).
 *  - Android 8–12: lưu mã vào SharedPreferences (đọc đồng bộ được trong attachBaseContext) và bọc Context.
 */
object AppLocale {
    private const val PREFS = "app_locale"
    private const val KEY_TAG = "tag"

    /** Mã ngôn ngữ app đang chọn, đã chuẩn hóa; "" = theo hệ thống. */
    fun current(context: Context): String {
        val raw = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val locales = context.getSystemService(LocaleManager::class.java)?.applicationLocales
            if (locales == null || locales.isEmpty) "" else locales[0].toLanguageTag()
        } else {
            prefs(context).getString(KEY_TAG, "").orEmpty()
        }
        return AppLanguages.normalize(raw)
    }

    /** Đặt ngôn ngữ cho app. [tag] rỗng = theo hệ thống. */
    fun apply(activity: Activity, tag: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            activity.getSystemService(LocaleManager::class.java)?.applicationLocales =
                if (tag.isEmpty()) LocaleList.getEmptyLocaleList() else LocaleList.forLanguageTags(tag)
        } else {
            prefs(activity).edit().putString(KEY_TAG, tag).commit()
            activity.recreate()
        }
    }

    /** Bọc Context theo ngôn ngữ đã chọn (chỉ Android 8–12; từ 13 hệ thống tự làm). */
    fun wrap(newBase: Context): Context {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return newBase
        val tag = AppLanguages.normalize(prefs(newBase).getString(KEY_TAG, "").orEmpty())
        if (tag.isEmpty()) {
            Locale.setDefault(Resources.getSystem().configuration.locales[0])
            return newBase
        }
        val locales = LocaleList.forLanguageTags(tag)
        Locale.setDefault(locales[0])
        val config = Configuration(newBase.resources.configuration)
        config.setLocales(locales)
        return newBase.createConfigurationContext(config)
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
