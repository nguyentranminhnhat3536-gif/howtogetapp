package com.ethanstudio.lunartasks.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ethanstudio.lunartasks.LunarTasksApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Báo thức bị xóa khi tắt máy hoặc cập nhật app, nên hẹn lại toàn bộ. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        val repository = (context.applicationContext as LunarTasksApp).repository
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                repository.rescheduleAll()
            } finally {
                pending.finish()
            }
        }
    }
}
