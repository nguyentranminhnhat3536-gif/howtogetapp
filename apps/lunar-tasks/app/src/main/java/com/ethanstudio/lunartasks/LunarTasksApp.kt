package com.ethanstudio.lunartasks

import android.app.Application
import com.ethanstudio.lunartasks.data.AppDatabase
import com.ethanstudio.lunartasks.data.SettingsRepository
import com.ethanstudio.lunartasks.data.TaskRepository
import com.ethanstudio.lunartasks.reminder.ReminderReceiver
import com.ethanstudio.lunartasks.reminder.ReminderScheduler
import com.ethanstudio.lunartasks.speech.Speaker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Giữ các đối tượng dùng chung (thay cho thư viện DI vì app nhỏ). */
class LunarTasksApp : Application() {
    val repository: TaskRepository by lazy {
        TaskRepository(AppDatabase.create(this).taskDao(), ReminderScheduler(this))
    }

    val settings: SettingsRepository by lazy { SettingsRepository(this) }

    /** Bộ đọc to dùng chung cho giao diện (khởi động khi cần lần đầu). */
    val speaker: Speaker by lazy { Speaker(this) }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        ReminderReceiver.ensureChannel(this)
        appScope.launch { repository.rescheduleAll() }
    }
}
