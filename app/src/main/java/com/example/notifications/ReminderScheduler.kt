package com.example.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.ReminderSettings
import java.util.Calendar

object ReminderScheduler {
    const val CHANNEL_ID = "nativity_fast_reminders_channel"
    const val CHANNEL_POINTS_APPROVAL = "nativity_points_approval_channel"

    const val EXTRA_TITLE = "extra_reminder_title"
    const val EXTRA_MESSAGE = "extra_reminder_message"
    const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    const val EXTRA_REMINDER_TYPE = "extra_reminder_type"

    const val TYPE_STATIC = "static"
    const val TYPE_DAILY_TASKS = "daily_tasks"
    const val TYPE_BIBLE_CHAPTER = "bible_chapter"

    const val ID_MORNING_PRAYER = 1001
    const val ID_NIGHT_PRAYER = 1002
    const val ID_WEEKLY_LITURGY = 1003
    const val ID_YOUTH_MEETING = 1004
    const val ID_DAILY_ENCOURAGEMENT = 1005
    const val ID_DAILY_TASKS_ACHIEVEMENTS = 1006
    const val ID_BIBLE_CHAPTER_DAILY = 1007
    const val ID_INSTANT_BIBLE_TEST = 1098
    const val ID_INSTANT_TEST = 1099
    const val ID_POINTS_APPROVED_BASE = 2000

    fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val remindersChannel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.notification_channel_desc)
            }
            manager.createNotificationChannel(remindersChannel)

            val approvalChannel = NotificationChannel(
                CHANNEL_POINTS_APPROVAL,
                context.getString(R.string.points_approval_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.points_approval_channel_desc)
                enableVibration(true)
            }
            manager.createNotificationChannel(approvalChannel)
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun applyAllReminders(context: Context, settings: ReminderSettings) {
        ensureNotificationChannel(context)

        if (settings.morningPrayerEnabled) {
            scheduleDailyReminder(
                context = context,
                requestCode = ID_MORNING_PRAYER,
                hour = settings.morningPrayerHour,
                minute = settings.morningPrayerMinute,
                title = "صباح الخير • صلاة باكر",
                message = "ابدأ يومك في صوم الميلاد بصلاة باكر وسجّل مشاركتك في تحدي شباب أبي سيفين!"
            )
        } else {
            cancelReminder(context, ID_MORNING_PRAYER)
        }

        if (settings.nightPrayerEnabled) {
            scheduleDailyReminder(
                context = context,
                requestCode = ID_NIGHT_PRAYER,
                hour = settings.nightPrayerHour,
                minute = settings.nightPrayerMinute,
                title = "تذكير صلاة النوم",
                message = "قبل ما تنام، اختم يومك بصلاة النوم واشكر ربنا على يومك وسجلها للمراجعة."
            )
        } else {
            cancelReminder(context, ID_NIGHT_PRAYER)
        }

        if (settings.liturgyEnabled) {
            scheduleWeeklyReminder(
                context = context,
                requestCode = ID_WEEKLY_LITURGY,
                dayOfWeek = settings.liturgyDayOfWeek,
                hour = settings.liturgyHour,
                minute = settings.liturgyMinute,
                title = "القداس الإلهي الأسبوعي",
                message = "تذكير بحضور القداس الإلهي والتناول هذا الأسبوع ضمن تحدي صوم الميلاد."
            )
        } else {
            cancelReminder(context, ID_WEEKLY_LITURGY)
        }

        if (settings.meetingEnabled) {
            scheduleWeeklyReminder(
                context = context,
                requestCode = ID_YOUTH_MEETING,
                dayOfWeek = settings.meetingDayOfWeek,
                hour = settings.meetingHour,
                minute = settings.meetingMinute,
                title = "اجتماع الشباب الجامعي • أبي سيفين",
                message = "مستنيينك في اجتماع الشباب الجامعي بكنيسة أبي سيفين بالشارقة! لا تنسَ تسجيل حضورك."
            )
        } else {
            cancelReminder(context, ID_YOUTH_MEETING)
        }

        if (settings.dailyEncouragementEnabled) {
            scheduleDailyReminder(
                context = context,
                requestCode = ID_DAILY_ENCOURAGEMENT,
                hour = settings.dailyEncouragementHour,
                minute = settings.dailyEncouragementMinute,
                title = "آية وتشجيع اليوم • تحدي الميلاد",
                message = "افتح التطبيق واقرأ آية اليوم وتابع تقدمك في تحديات صوم الميلاد وسر الاعتراف."
            )
        } else {
            cancelReminder(context, ID_DAILY_ENCOURAGEMENT)
        }

        if (settings.dailyTasksReminderEnabled) {
            scheduleDailyReminder(
                context = context,
                requestCode = ID_DAILY_TASKS_ACHIEVEMENTS,
                hour = settings.dailyTasksReminderHour,
                minute = settings.dailyTasksReminderMinute,
                title = "تذكير بإنجازاتك اليومية ✨",
                message = "افتح التطبيق وسجّل إنجازاتك ومهامك اليومية في تحدي طريق المزود!",
                reminderType = TYPE_DAILY_TASKS
            )
        } else {
            cancelReminder(context, ID_DAILY_TASKS_ACHIEVEMENTS)
        }

        if (settings.bibleReadingReminderEnabled) {
            scheduleDailyReminder(
                context = context,
                requestCode = ID_BIBLE_CHAPTER_DAILY,
                hour = settings.bibleReadingHour,
                minute = settings.bibleReadingMinute,
                title = "ورد الكتاب المقدس اليوم 📖",
                message = "حان وقت قراءة إصحاح اليوم في تحدي طريق المزود!",
                reminderType = TYPE_BIBLE_CHAPTER
            )
        } else {
            cancelReminder(context, ID_BIBLE_CHAPTER_DAILY)
        }
    }

    private fun scheduleDailyReminder(
        context: Context,
        requestCode: Int,
        hour: Int,
        minute: Int,
        title: String,
        message: String,
        reminderType: String = TYPE_STATIC
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        val pendingIntent = buildPendingIntent(context, requestCode, title, message, reminderType)
        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            triggerCal.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }

    private fun scheduleWeeklyReminder(
        context: Context,
        requestCode: Int,
        dayOfWeek: Int,
        hour: Int,
        minute: Int,
        title: String,
        message: String
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerCal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, dayOfWeek)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.WEEK_OF_YEAR, 1)
            }
        }
        val pendingIntent = buildPendingIntent(context, requestCode, title, message)
        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            triggerCal.timeInMillis,
            AlarmManager.INTERVAL_DAY * 7L,
            pendingIntent
        )
    }

    private fun cancelReminder(context: Context, requestCode: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    private fun buildPendingIntent(
        context: Context,
        requestCode: Int,
        title: String,
        message: String,
        reminderType: String = TYPE_STATIC
    ): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_MESSAGE, message)
            putExtra(EXTRA_NOTIFICATION_ID, requestCode)
            putExtra(EXTRA_REMINDER_TYPE, reminderType)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun showNotificationNow(
        context: Context,
        notificationId: Int,
        title: String,
        message: String
    ): Boolean {
        ensureNotificationChannel(context)
        if (!hasNotificationPermission(context)) return false

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)
            .build()

        return try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
            true
        } catch (_: SecurityException) {
            false
        }
    }

    fun showPointsApprovedNotification(
        context: Context,
        title: String,
        message: String,
        notificationId: Int = ID_POINTS_APPROVED_BASE + (System.currentTimeMillis() % 1000).toInt()
    ): Boolean {
        ensureNotificationChannel(context)
        if (!hasNotificationPermission(context)) return false

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_POINTS_APPROVAL)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)
            .build()

        return try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
            true
        } catch (_: SecurityException) {
            false
        }
    }
}
