package com.ethanstudio.snapsheet.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.sessionStore: DataStore<Preferences> by preferencesDataStore(name = "session")

/** Ghi nhớ người dùng đã qua màn chào (đã đăng nhập hoặc chọn dùng không cần tài khoản). */
class SessionStore(context: Context) {
    private val store = context.sessionStore

    val onboarded: Flow<Boolean> = store.data.map { it[ONBOARDED] == true }

    suspend fun setOnboarded() {
        store.edit { it[ONBOARDED] = true }
    }

    private companion object {
        val ONBOARDED = booleanPreferencesKey("onboarded")
    }
}
