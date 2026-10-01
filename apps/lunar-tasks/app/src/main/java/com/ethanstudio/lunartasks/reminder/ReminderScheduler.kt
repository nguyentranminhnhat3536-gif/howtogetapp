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
 * Mỗi việc có tối đa 2 báo thức: đúng hạn, và (nếu bật) 19:00 hôm trước.
 */
class ReminderScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun schedule(task: Task) {
        cancel(task.id)
        val now = System.currentTimeMillis()
        triggerAtMillis(task)?.takeIf { it > now }?.let {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, it, pendingIntent(task.id, early = false))
        }
        dayBeforeTriggerAtMillis(task)?.takeIf { it > now }?.let {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, it, pendingIntent(task.id, early = true))
        }
    }

    fun cancel(taskId: Long) {
        alarmManager.cancel(pendingIntent(taskId, early = false))
        alarmManager.cancel(pendingIntent(taskId, early = true))
    }

    private fun pendingIntent(taskId: Long, early: Boolean): PendingIntent {
        // Action khác nhau để hai PendingIntent của cùng một việc không đè lên nhau.
        val intent = Intent(context, ReminderReceiver::class.java)
            .setAction(if (early) ReminderReceiver.ACTION_REMIND_EARLY else ReminderReceiver.ACTION_REMIND)
            .putExtra(ReminderReceiver.EXTRA_TASK_ID, taskId)
        return PendingIntent.getBroadcast(
            context,
            taskId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        /** Việc có ngày nhưng không có giờ thì nhắc lúc 7:00 sáng. */
        const val DEFAULT_REMINDER_MINUTE = 7 * 60

        /** Nhắc trước một ngày lúc 19:00, đủ thời gian chuẩn bị cho hôm sau. */
        const val DAY_BEFORE_MINUTE = 19 * 60

        fun triggerAtMillis(task: Task, zone: ZoneId = ZoneId.systemDefault()): Long? {
            if (!task.remind || task.done) return null
            val day = task.dueEpochDay ?: return null
            return millisAt(LocalDate.ofEpochDay(day), task.dueMinute ?: DEFAULT_REMINDER_MINUTE, zone)
        }

        fun dayBeforeTriggerAtMillis(task: Task, zone: ZoneId = ZoneId.systemDefault()): Long? {
            if (!task.remindDayBefore || task.done) return null
            val day = task.dueEpochDay ?: return null
            return millisAt(LocalDate.ofEpochDay(day).minusDays(1), DAY_BEFORE_MINUTE, zone)
        }

        private fun millisAt(date: LocalDate, minute: Int, zone: ZoneId): Long =
            date.atTime(minute / 60, minute % 60).atZone(zone).toInstant().toEpochMilli()
    }
}
