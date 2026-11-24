package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra(ReminderScheduler.EXTRA_TITLE)
            ?: "تحدي صوم الميلاد • أبي سيفين"
        val message = intent.getStringExtra(ReminderScheduler.EXTRA_MESSAGE)
            ?: "تذكير بصلواتك اليومية ومتابعة سكورك في تحدي صوم الميلاد!"
        val id = intent.getIntExtra(
            ReminderScheduler.EXTRA_NOTIFICATION_ID,
            ReminderScheduler.ID_DAILY_ENCOURAGEMENT
        )
        ReminderScheduler.showNotificationNow(context, id, title, message)
    }
}
