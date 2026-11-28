package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.AppDatabase
import com.example.data.AutoContentBank
import com.example.data.BibleReadingPlan
import com.example.data.FastCalendar
import com.example.data.FastDate
import com.example.data.PreferencesManager
import com.example.data.SubmissionStatus
import com.example.data.TaskFrequency
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val appContext = context.applicationContext
        val reminderType = intent.getStringExtra(ReminderScheduler.EXTRA_REMINDER_TYPE)
            ?: ReminderScheduler.TYPE_STATIC
        val id = intent.getIntExtra(
            ReminderScheduler.EXTRA_NOTIFICATION_ID,
            ReminderScheduler.ID_DAILY_ENCOURAGEMENT
        )

        if (reminderType == ReminderScheduler.TYPE_BIBLE_CHAPTER ||
            reminderType == ReminderScheduler.TYPE_DAILY_TASKS
        ) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val prefs = PreferencesManager(appContext)
                    val settings = prefs.getReminderSettingsSnapshot()
                    val today = FastDate.today()

                    if (reminderType == ReminderScheduler.TYPE_BIBLE_CHAPTER) {
                        if (!settings.bibleReadingReminderEnabled) return@launch
                        val reading = BibleReadingPlan.getTodayReading(
                            startBookId = settings.bibleBookId,
                            startDateIso = settings.bibleStartDateIso,
                            today = today
                        )
                        val (title, body) = BibleReadingPlan.formatReminderNotification(reading)
                        ReminderScheduler.showNotificationNow(appContext, id, title, body)
                    } else {
                        if (!settings.dailyTasksReminderEnabled) return@launch
                        val activeUid = prefs.activeUserIdFlow.first()
                        val db = AppDatabase.getInstance(appContext)
                        val allTasks = db.challengeTaskDao().observeAllTasks().first()
                        val allSubs = db.taskSubmissionDao().observeAllSubmissions().first()

                        val dailyTasks = allTasks.filter {
                            it.isActive && TaskFrequency.fromKey(it.frequency) == TaskFrequency.DAILY
                        }
                        val periodKey = FastCalendar.periodKeyForTask(TaskFrequency.DAILY, today)
                        val remaining = if (activeUid != null) {
                            dailyTasks.count { task ->
                                val sub = allSubs.firstOrNull {
                                    it.userId == activeUid &&
                                        it.taskId == task.id &&
                                        it.periodKey == periodKey &&
                                        (it.status == SubmissionStatus.APPROVED.key ||
                                            it.status == SubmissionStatus.PENDING.key)
                                }
                                sub == null
                            }
                        } else {
                            dailyTasks.size
                        }

                        val (title, body) = if (remaining > 0) {
                            "تذكير بإنجازاتك اليومية ✨" to
                                "متبقي لك $remaining من المهام اليومية اليوم في تحدي طريق المزود! خصص دقائق الآن لإنجازها وتسجيل نقاطك."
                        } else {
                            "أحسنت! أتممت إنجازاتك اليومية 🎉" to
                                "رائع! سجلت جميع مهامك اليومية اليوم. تابع مركزك واستعد لورد الغد في تحدي طريق المزود."
                        }
                        ReminderScheduler.showNotificationNow(appContext, id, title, body)
                    }
                } catch (_: Exception) {
                    val fallbackTitle = intent.getStringExtra(ReminderScheduler.EXTRA_TITLE)
                        ?: "تحدي طريق المزود ✨"
                    val fallbackMsg = intent.getStringExtra(ReminderScheduler.EXTRA_MESSAGE)
                        ?: "تذكير بوردك اليومي وإنجازاتك في تحدي طريق المزود!"
                    ReminderScheduler.showNotificationNow(appContext, id, fallbackTitle, fallbackMsg)
                } finally {
                    pendingResult.finish()
                }
            }
            return
        }

        if (id == ReminderScheduler.ID_DAILY_ENCOURAGEMENT) {
            val devotion = AutoContentBank.getTodayAutoDevotion(appContext)
            val title = "آية اليوم • ${devotion.verseRef} ✨"
            val message = "${devotion.verseText} — ${devotion.fatherQuote} (${devotion.fatherName})"
            ReminderScheduler.showNotificationNow(appContext, id, title, message)
            return
        }

        val title = intent.getStringExtra(ReminderScheduler.EXTRA_TITLE)
            ?: "تحدي صوم الميلاد • أبي سيفين"
        val message = intent.getStringExtra(ReminderScheduler.EXTRA_MESSAGE)
            ?: "تذكير بصلواتك اليومية ومتابعة سكورك في تحدي صوم الميلاد!"
        ReminderScheduler.showNotificationNow(appContext, id, title, message)
    }
}
