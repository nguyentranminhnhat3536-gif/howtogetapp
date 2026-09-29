package com.ethanstudio.lunartasks.reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.ethanstudio.lunartasks.LunarTasksApp
import com.ethanstudio.lunartasks.MainActivity
import com.ethanstudio.lunartasks.R
import com.ethanstudio.lunartasks.data.Task
import com.ethanstudio.lunartasks.speech.Speaker
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Nhận báo thức để hiện thông báo nhắc việc, và nhận nút "Xong" trên thông báo. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
        if (taskId < 0) return
        val repository = (context.applicationContext as LunarTasksApp).repository
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val task = repository.get(taskId) ?: return@launch
                when (intent.action) {
                    ACTION_DONE -> {
                        NotificationManagerCompat.from(context).cancel(taskId.toInt())
                        if (!task.done) repository.toggleDone(task, LocalDate.now())
                    }
                    ACTION_REMIND_EARLY -> if (task.remindDayBefore && !task.done) remind(context, task, early = true)
                    else -> if (task.remind && !task.done) remind(context, task, early = false)
                }
            } finally {
                pending.finish()
            }
        }
    }

    /** Hiện thông báo, và đọc to nếu người dùng bật "Đọc to lời nhắc". */
    private suspend fun remind(context: Context, task: Task, early: Boolean) {
        val (title, body) = showNotification(context, task, early) ?: return
        val app = context.applicationContext as LunarTasksApp
        if (!app.settings.settings.first().speakReminders) return
        val speaker = Speaker(context)
        try {
            speaker.speakAndWait("$title. $body", timeoutMs = SPEAK_TIMEOUT_MS)
        } finally {
            speaker.shutdown()
        }
    }

    /** Trả về (tiêu đề, nội dung) đã hiện, hoặc null nếu không được phép hiện thông báo. */
    private fun showNotification(context: Context, task: Task, early: Boolean): Pair<String, String>? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return null
        }
        ensureChannel(context)
        val requestCode = task.id.toInt()
        val openApp = PendingIntent.getActivity(
            context,
            requestCode,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val markDone = PendingIntent.getBroadcast(
            context,
            requestCode,
            Intent(context, ReminderReceiver::class.java).setAction(ACTION_DONE).putExtra(EXTRA_TASK_ID, task.id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val title = when {
            early -> context.getString(R.string.notification_tomorrow, task.title)
            task.isMedicine -> context.getString(R.string.notification_medicine, task.title)
            else -> task.title
        }
        val body = task.note.ifBlank {
            context.getString(if (early) R.string.notification_due_tomorrow else R.string.notification_due_now)
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .addAction(R.drawable.ic_check, context.getString(R.string.notification_action_done), markDone)
            .build()
        return try {
            NotificationManagerCompat.from(context).notify(requestCode, notification)
            title to body
        } catch (e: SecurityException) {
            // Người dùng vừa tắt quyền thông báo, bỏ qua.
            null
        }
    }

    companion object {
        const val EXTRA_TASK_ID = "task_id"
        const val ACTION_REMIND = "com.ethanstudio.lunartasks.action.REMIND"
        const val ACTION_REMIND_EARLY = "com.ethanstudio.lunartasks.action.REMIND_EARLY"
        const val ACTION_DONE = "com.ethanstudio.lunartasks.action.DONE"
        private const val CHANNEL_ID = "reminders"

        /** goAsync() chỉ cho khoảng 10 giây, nên đọc tối đa 7 giây. */
        private const val SPEAK_TIMEOUT_MS = 7_000L

        fun ensureChannel(context: Context) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH,
            )
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
}
