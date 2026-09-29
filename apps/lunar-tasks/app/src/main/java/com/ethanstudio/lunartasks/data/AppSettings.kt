package com.ethanstudio.lunartasks.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Cài đặt của app: hiển thị cho người lớn tuổi / thị lực kém, và người thân để gọi nhanh. */
data class AppSettings(
    val largeText: Boolean = false,
    val highContrast: Boolean = false,
    val contactName: String = "",
    val contactPhone: String = "",
) {
    val hasContact: Boolean get() = contactPhone.isNotBlank()
}

private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** Lưu trong DataStore trên máy, không gửi đi đâu. */
class SettingsRepository(context: Context) {
    private val store = context.settingsStore

    val settings: Flow<AppSettings> = store.data.map { prefs ->
        AppSettings(
            largeText = prefs[LARGE_TEXT] ?: false,
            highContrast = prefs[HIGH_CONTRAST] ?: false,
            contactName = prefs[CONTACT_NAME] ?: "",
            contactPhone = prefs[CONTACT_PHONE] ?: "",
        )
    }

    suspend fun setLargeText(value: Boolean) {
        store.edit { it[LARGE_TEXT] = value }
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
        val HIGH_CONTRAST = booleanPreferencesKey("high_contrast")
        val CONTACT_NAME = stringPreferencesKey("contact_name")
        val CONTACT_PHONE = stringPreferencesKey("contact_phone")
    }
}
