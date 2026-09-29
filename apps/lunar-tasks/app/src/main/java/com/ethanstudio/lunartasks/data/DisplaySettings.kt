package com.ethanstudio.lunartasks.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Tùy chọn hiển thị cho người lớn tuổi / thị lực kém. */
data class DisplaySettings(
    val largeText: Boolean = false,
    val highContrast: Boolean = false,
)

private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(context: Context) {
    private val store = context.settingsStore

    val display: Flow<DisplaySettings> = store.data.map { prefs ->
        DisplaySettings(
            largeText = prefs[LARGE_TEXT] ?: false,
            highContrast = prefs[HIGH_CONTRAST] ?: false,
        )
    }

    suspend fun setLargeText(value: Boolean) {
        store.edit { it[LARGE_TEXT] = value }
    }

    suspend fun setHighContrast(value: Boolean) {
        store.edit { it[HIGH_CONTRAST] = value }
    }

    private companion object {
        val LARGE_TEXT = booleanPreferencesKey("large_text")
        val HIGH_CONTRAST = booleanPreferencesKey("high_contrast")
    }
}
