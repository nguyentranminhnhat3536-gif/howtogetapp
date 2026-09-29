package com.ethanstudio.lunartasks.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.ethanstudio.lunartasks.data.Task
import java.time.LocalDate
import java.time.ZoneId

/**
 * Hẹn giờ nhắc việc bằng AlarmManager (loại không chính xác tuyệt đối, nên không cần quyền
 * SCHEDULE_EXACT_ALARM; thông báo có thể trễ vài phút khi máy đang tiết kiệm pin).
 */
class ReminderScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun schedule(task: Task) {
        cancel(task.id)
        val triggerAt = triggerAtMillis(task) ?: return
        if (triggerAt <= System.currentTimeMillis()) return
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent(task.id))
    }

    fun cancel(taskId: Long) {
        alarmManager.cancel(pendingIntent(taskId))
    }

    private fun pendingIntent(taskId: Long): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).putExtra(ReminderReceiver.EXTRA_TASK_ID, taskId)
        return PendingIntent.getBroadcast(
            context,
            taskId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        fun triggerAtMillis(task: Task, zone: ZoneId = ZoneId.systemDefault()): Long? {
            if (!task.remind || task.done) return null
            val day = task.dueEpochDay ?: return null
            val minute = task.dueMinute ?: DEFAULT_REMINDER_MINUTE
            return LocalDate.ofEpochDay(day).atTime(minute / 60, minute % 60).atZone(zone).toInstant().toEpochMilli()
        }

        /** Việc có ngày nhưng không có giờ thì nhắc lúc 7:00 sáng. */
        const val DEFAULT_REMINDER_MINUTE = 7 * 60
    }
}
