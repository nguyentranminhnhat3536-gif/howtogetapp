package com.ethanstudio.snapsheet.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ethanstudio.snapsheet.billing.ProKind
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class ProState(
    val isPro: Boolean = false,
    val kind: ProKind? = null,
    val usage: FreeLimits.Usage = FreeLimits.Usage(),
)

private val Context.proStore: DataStore<Preferences> by preferencesDataStore(name = "pro")

/**
 * Lưu trên máy: đã mua Pro chưa (để dùng được khi không có mạng) và số lần xuất hôm nay.
 * Mỗi lần mở app, BillingManager hỏi Google Play lại và cập nhật ở đây.
 */
class ProStore(context: Context) {
    private val store = context.proStore

    val state: Flow<ProState> = store.data.map { p ->
        ProState(
            isPro = p[IS_PRO] == true,
            kind = p[KIND]?.let { runCatching { ProKind.valueOf(it) }.getOrNull() },
            usage = FreeLimits.Usage(p[DAY] ?: 0L, p[COUNT] ?: 0),
        )
    }

    suspend fun setPro(isPro: Boolean, kind: ProKind?) {
        store.edit { p ->
            p[IS_PRO] = isPro
            if (kind != null) p[KIND] = kind.name else p.remove(KIND)
        }
    }

    /** Trả về true nếu được phép xuất (và đã ghi nhận một lần với bản miễn phí). */
    suspend fun tryConsumeExport(today: Long): Boolean = tryConsumeExports(1, today)

    /**
     * Xuất [count] file một lần: trả về true nếu còn đủ lượt (bản miễn phí thì ghi nhận cả [count] lượt).
     * Không đủ thì không trừ lượt nào. Kiểm tra và ghi trong cùng một lần sửa nên không bị đếm sai khi bấm nhanh.
     */
    suspend fun tryConsumeExports(count: Int, today: Long): Boolean {
        var allowed = false
        store.edit { p ->
            val pro = p[IS_PRO] == true
            val usage = FreeLimits.Usage(p[DAY] ?: 0L, p[COUNT] ?: 0)
            if (FreeLimits.canConsume(pro, usage, today, count)) {
                allowed = true
                if (!pro && count > 0) {
                    val next = FreeLimits.consumeMany(usage, today, count)
                    p[DAY] = next.day
                    p[COUNT] = next.count
                }
            }
        }
        return allowed
    }

    private companion object {
        val IS_PRO = booleanPreferencesKey("is_pro")
        val KIND = stringPreferencesKey("kind")
        val DAY = longPreferencesKey("export_day")
        val COUNT = intPreferencesKey("export_count")
    }
}
