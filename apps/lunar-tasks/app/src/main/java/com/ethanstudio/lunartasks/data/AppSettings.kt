package com.ethanstudio.lunartasks.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Cài đặt của app: hiển thị cho người lớn tuổi / thị lực kém, và người thân để gọi nhanh. */
data class AppSettings(
    /** Hệ số cỡ chữ, một trong [TEXT_SCALES]. */
    val textScale: Float = 1f,
    val highContrast: Boolean = false,
    /** Đọc to nội dung thông báo nhắc việc bằng giọng nói của máy. */
    val speakReminders: Boolean = false,
    /** Rung nhẹ khi bấm nút. */
    val haptics: Boolean = true,
    val contactName: String = "",
    val contactPhone: String = "",
) {
    val hasContact: Boolean get() = contactPhone.isNotBlank()

    companion object {
        val TEXT_SCALES = listOf(1f, 1.15f, 1.3f, 1.5f, 1.75f, 2f)

        /** Mức cỡ chữ kế tiếp (step = +1 hoặc -1), dừng ở hai đầu. */
        fun nextScale(current: Float, step: Int): Float {
            val index = TEXT_SCALES.indexOfFirst { it >= current - 0.01f }.let { if (it < 0) TEXT_SCALES.lastIndex else it }
            return TEXT_SCALES[(index + step).coerceIn(0, TEXT_SCALES.lastIndex)]
        }
    }
}

private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** Lưu trong DataStore trên máy, không gửi đi đâu. */
class SettingsRepository(context: Context) {
    private val store = context.settingsStore

    val settings: Flow<AppSettings> = store.data.map { prefs ->
        AppSettings(
            // Bản cũ chỉ có công tắc "Chữ to" (×1.3); giữ nguyên lựa chọn đó cho người đã bật.
            textScale = prefs[TEXT_SCALE] ?: if (prefs[LARGE_TEXT] == true) 1.3f else 1f,
            highContrast = prefs[HIGH_CONTRAST] ?: false,
            speakReminders = prefs[SPEAK_REMINDERS] ?: false,
            haptics = prefs[HAPTICS] ?: true,
            contactName = prefs[CONTACT_NAME] ?: "",
            contactPhone = prefs[CONTACT_PHONE] ?: "",
        )
    }

    suspend fun setTextScale(value: Float) {
        store.edit { it[TEXT_SCALE] = value }
    }

    suspend fun setSpeakReminders(value: Boolean) {
        store.edit { it[SPEAK_REMINDERS] = value }
    }

    suspend fun setHaptics(value: Boolean) {
        store.edit { it[HAPTICS] = value }
    }

    suspend fun setHighContrast(value: Boolean) {
        store.edit { it[HIGH_CONTRAST] = value }
    }

    suspend fun setContact(name: String, phone: String) {
        store.edit {
            it[CONTACT_NAME] = name.trim()
            it[CONTACT_PHONE] = phone.trim()
        }
    }

    private companion object {
        val LARGE_TEXT = booleanPreferencesKey("large_text")
        val TEXT_SCALE = floatPreferencesKey("text_scale")
        val HIGH_CONTRAST = booleanPreferencesKey("high_contrast")
        val SPEAK_REMINDERS = booleanPreferencesKey("speak_reminders")
        val HAPTICS = booleanPreferencesKey("haptics")
        val CONTACT_NAME = stringPreferencesKey("contact_name")
        val CONTACT_PHONE = stringPreferencesKey("contact_phone")
    }
}
