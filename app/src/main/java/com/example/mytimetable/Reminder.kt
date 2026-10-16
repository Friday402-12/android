package com.example.mytimetable

import android.Manifest
import android.app.AlarmManager
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
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Calendar

/**
 * 课前提醒：为未来若干天的每一节课排一个闹钟。
 * 不使用 WorkManager，避免额外的依赖与初始化开销。
 */
object Reminder {

    const val CHANNEL_ID = "class_reminder"
    private const val SCHEDULE_DAYS = 7

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val mgr = context.getSystemService(NotificationManager::class.java) ?: return
        if (mgr.getNotificationChannel(CHANNEL_ID) != null) return
        val ch = NotificationChannel(
            CHANNEL_ID,
            "课前提醒",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "每节课开始前提醒你上课时间和地点"
            enableVibration(true)
        }
        mgr.createNotificationChannel(ch)
    }

    fun canPostNotifications(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }

    /** 所有需要提醒的闹钟 PendingIntent，用于整体取消 */
    private fun allPendingIntents(context: Context, t: Timetable): List<PendingIntent> {
        val list = mutableListOf<PendingIntent>()
        val today = LocalDate.now()
        for (d in 0 until SCHEDULE_DAYS) {
            val date = today.plusDays(d.toLong())
            val w = t.weekOf(date)
            if (w < 1 || w > t.weeks) continue
            val dayIdx = (date.dayOfWeek.value + 6) % 7
            for (c in t.coursesOfDay(dayIdx, w)) {
                for (sec in DefaultData.SECTIONS) {
                    if (sec.id != c.sec) continue
                    list.add(pendingIntent(context, c, sec, date, PendingIntent.FLAG_NO_CREATE))
                }
            }
        }
        return list.filterNotNull()
    }

    private fun pendingIntent(
        context: Context,
        course: Course,
        sec: Section,
        date: LocalDate,
        extraFlags: Int
    ): PendingIntent? {
        val lead = context.getSharedPreferences("timetable", Context.MODE_PRIVATE)
            .getInt("lead", 10)
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("title", "还有 $lead 分钟上课")
            putExtra("name", course.name)
            putExtra("room", course.room)
            putExtra("sec", sec.name + " " + sec.time)
        }
        val requestCode = (course.id.hashCode() * 31 + date.hashCode()) * 31 + sec.id
        var flags = PendingIntent.FLAG_IMMUTABLE or extraFlags
        if (extraFlags == 0) flags = flags or PendingIntent.FLAG_UPDATE_CURRENT
        return PendingIntent.getBroadcast(context, requestCode, intent, flags)
    }

    private fun alarmManager(context: Context): AlarmManager? =
        context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    private fun exactAllowed(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val am = alarmManager(context) ?: return false
        return am.canScheduleExactAlarms()
    }

    private fun setAt(context: Context, am: AlarmManager, atMillis: Long, pi: PendingIntent) {
        if (exactAllowed(context)) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pi)
        } else {
            // 未授予「闹钟与提醒」权限时退化为非精确闹钟，仍能提醒（可能晚几分钟）
            am.set(AlarmManager.RTC_WAKEUP, atMillis, pi)
        }
    }

    /** 取消全部提醒（应用退出/关闭开关时调用） */
    fun cancelAll(context: Context) {
        val repo = Repo(context)
        val am = alarmManager(context) ?: return
        allPendingIntents(context, repo.timetable).forEach { am.cancel(it) }
    }

    /** 重排未来一周的提醒 */
    fun reschedule(context: Context) {
        val repo = Repo(context)
        val am = alarmManager(context) ?: return
        cancelAll(context)

        if (!repo.remindersOn || !canPostNotifications(context)) return

        val t = repo.timetable
        val lead = repo.leadMinutes
        val now = LocalDateTime.now()
        val today = now.toLocalDate()

        for (d in 0 until SCHEDULE_DAYS) {
            val date = today.plusDays(d.toLong())
            val w = t.weekOf(date)
            if (w < 1 || w > t.weeks) continue
            val dayIdx = (date.dayOfWeek.value + 6) % 7
            for (c in t.coursesOfDay(dayIdx, w)) {
                val sec = DefaultData.section(c.sec)
                val at = sec.startDateTime(date).minusMinutes(lead.toLong())
                val atMillis = at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                if (atMillis <= System.currentTimeMillis()) continue
                val pi = pendingIntent(context, c, sec, date, 0) ?: continue
                setAt(context, am, atMillis, pi)
            }
        }
    }
}

/** 闹钟触发 → 弹通知 */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val repo = Repo(context)
        if (!repo.remindersOn) return
        if (!Reminder.canPostNotifications(context)) return

        Reminder.createChannel(context)

        val name = intent.getStringExtra("name") ?: return
        val room = intent.getStringExtra("room").orEmpty()
        val sec = intent.getStringExtra("sec").orEmpty()
        val title = intent.getStringExtra("title") ?: "课前提醒"

        val tap = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, Reminder.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText("$name · $sec · @$room")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$name\n$sec\n上课地点：$room")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(tap)

        try {
            NotificationManagerCompat.from(context).notify(name.hashCode(), builder.build())
        } catch (e: SecurityException) {
            // 权限被回收，静默忽略
        }
    }
}

/** 开机 / 应用更新后重排提醒 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                Reminder.createChannel(context)
                Reminder.reschedule(context)
            }
        }
    }
}
