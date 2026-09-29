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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Nhận báo thức và hiện thông báo nhắc việc. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
        if (taskId < 0) return
        val repository = (context.applicationContext as LunarTasksApp).repository
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val task = repository.get(taskId)
                if (task != null && task.remind && !task.done) {
                    showNotification(context, taskId, task.title, task.note)
                }
            } finally {
                pending.finish()
            }
        }
    }

    private fun showNotification(context: Context, taskId: Long, title: String, note: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        ensureChannel(context)
        val openApp = PendingIntent.getActivity(
            context,
            taskId.toInt(),
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(note.ifBlank { context.getString(R.string.notification_due_now) })
            .setStyle(NotificationCompat.BigTextStyle().bigText(note.ifBlank { context.getString(R.string.notification_due_now) }))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(taskId.toInt(), notification)
        } catch (e: SecurityException) {
            // Người dùng vừa tắt quyền thông báo, bỏ qua.
        }
    }

    companion object {
        const val EXTRA_TASK_ID = "task_id"
        private const val CHANNEL_ID = "reminders"

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
