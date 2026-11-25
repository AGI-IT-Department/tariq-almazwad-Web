package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "tariq_almazwad_prefs"
)

data class ApprovalSnapshot(
    val seenApprovedIds: Set<String>,
    val lastBonusPoints: Int
)

data class ReminderSettings(
    val pointsApprovalEnabled: Boolean = true,
    val dailyTasksReminderEnabled: Boolean = true,
    val dailyTasksReminderHour: Int = 20,
    val dailyTasksReminderMinute: Int = 0,
    val bibleReadingReminderEnabled: Boolean = true,
    val bibleBookId: String = BibleReadingPlan.DEFAULT_BOOK_ID,
    val bibleReadingHour: Int = 8,
    val bibleReadingMinute: Int = 0,
    val bibleStartDateIso: String = "",
    val morningPrayerEnabled: Boolean = true,
    val morningPrayerHour: Int = 7,
    val morningPrayerMinute: Int = 30,
    val nightPrayerEnabled: Boolean = true,
    val nightPrayerHour: Int = 22,
    val nightPrayerMinute: Int = 0,
    val liturgyEnabled: Boolean = true,
    val liturgyDayOfWeek: Int = java.util.Calendar.FRIDAY,
    val liturgyHour: Int = 7,
    val liturgyMinute: Int = 0,
    val meetingEnabled: Boolean = true,
    val meetingDayOfWeek: Int = java.util.Calendar.THURSDAY,
    val meetingHour: Int = 18,
    val meetingMinute: Int = 0,
    val dailyEncouragementEnabled: Boolean = true,
    val dailyEncouragementHour: Int = 14,
    val dailyEncouragementMinute: Int = 0
)

class PreferencesManager(private val context: Context) {

    private object Keys {
        val ACTIVE_USER_ID = stringPreferencesKey("active_user_id")
        val ADMIN_PIN_HASH = stringPreferencesKey("admin_pin_hash")
        val HAS_SEEN_WELCOME = booleanPreferencesKey("has_seen_welcome")

        val POINTS_APPROVAL_ENABLED = booleanPreferencesKey("points_approval_enabled")
        val DAILY_TASKS_ENABLED = booleanPreferencesKey("daily_tasks_reminder_enabled")
        val DAILY_TASKS_HOUR = intPreferencesKey("daily_tasks_reminder_hour")
        val DAILY_TASKS_MIN = intPreferencesKey("daily_tasks_reminder_min")
        val BIBLE_READING_ENABLED = booleanPreferencesKey("bible_reading_reminder_enabled")
        val BIBLE_BOOK_ID = stringPreferencesKey("bible_book_id")
        val BIBLE_READING_HOUR = intPreferencesKey("bible_reading_hour")
        val BIBLE_READING_MIN = intPreferencesKey("bible_reading_min")
        val BIBLE_START_DATE_ISO = stringPreferencesKey("bible_start_date_iso")
        val MORNING_ENABLED = booleanPreferencesKey("morning_enabled")
        val MORNING_HOUR = intPreferencesKey("morning_hour")
        val MORNING_MIN = intPreferencesKey("morning_min")
        val NIGHT_ENABLED = booleanPreferencesKey("night_enabled")
        val NIGHT_HOUR = intPreferencesKey("night_hour")
        val NIGHT_MIN = intPreferencesKey("night_min")
        val LITURGY_ENABLED = booleanPreferencesKey("liturgy_enabled")
        val LITURGY_DOW = intPreferencesKey("liturgy_dow")
        val LITURGY_HOUR = intPreferencesKey("liturgy_hour")
        val LITURGY_MIN = intPreferencesKey("liturgy_min")
        val MEETING_ENABLED = booleanPreferencesKey("meeting_enabled")
        val MEETING_DOW = intPreferencesKey("meeting_dow")
        val MEETING_HOUR = intPreferencesKey("meeting_hour")
        val MEETING_MIN = intPreferencesKey("meeting_min")
        val ENCOURAGEMENT_ENABLED = booleanPreferencesKey("encouragement_enabled")
        val ENCOURAGEMENT_HOUR = intPreferencesKey("encouragement_hour")
        val ENCOURAGEMENT_MIN = intPreferencesKey("encouragement_min")
    }

    val activeUserIdFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[Keys.ACTIVE_USER_ID]?.takeIf { it.isNotBlank() }
    }

    val hasSeenWelcomeFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.HAS_SEEN_WELCOME] ?: false
    }

    val reminderSettingsFlow: Flow<ReminderSettings> = context.dataStore.data.map { prefs ->
        ReminderSettings(
            pointsApprovalEnabled = prefs[Keys.POINTS_APPROVAL_ENABLED] ?: true,
            dailyTasksReminderEnabled = prefs[Keys.DAILY_TASKS_ENABLED] ?: true,
            dailyTasksReminderHour = prefs[Keys.DAILY_TASKS_HOUR] ?: 20,
            dailyTasksReminderMinute = prefs[Keys.DAILY_TASKS_MIN] ?: 0,
            bibleReadingReminderEnabled = prefs[Keys.BIBLE_READING_ENABLED] ?: true,
            bibleBookId = prefs[Keys.BIBLE_BOOK_ID]?.takeIf { it.isNotBlank() }
                ?: BibleReadingPlan.DEFAULT_BOOK_ID,
            bibleReadingHour = prefs[Keys.BIBLE_READING_HOUR] ?: 8,
            bibleReadingMinute = prefs[Keys.BIBLE_READING_MIN] ?: 0,
            bibleStartDateIso = prefs[Keys.BIBLE_START_DATE_ISO] ?: "",
            morningPrayerEnabled = prefs[Keys.MORNING_ENABLED] ?: true,
            morningPrayerHour = prefs[Keys.MORNING_HOUR] ?: 7,
            morningPrayerMinute = prefs[Keys.MORNING_MIN] ?: 30,
            nightPrayerEnabled = prefs[Keys.NIGHT_ENABLED] ?: true,
            nightPrayerHour = prefs[Keys.NIGHT_HOUR] ?: 22,
            nightPrayerMinute = prefs[Keys.NIGHT_MIN] ?: 0,
            liturgyEnabled = prefs[Keys.LITURGY_ENABLED] ?: true,
            liturgyDayOfWeek = prefs[Keys.LITURGY_DOW] ?: java.util.Calendar.FRIDAY,
            liturgyHour = prefs[Keys.LITURGY_HOUR] ?: 7,
            liturgyMinute = prefs[Keys.LITURGY_MIN] ?: 0,
            meetingEnabled = prefs[Keys.MEETING_ENABLED] ?: true,
            meetingDayOfWeek = prefs[Keys.MEETING_DOW] ?: java.util.Calendar.THURSDAY,
            meetingHour = prefs[Keys.MEETING_HOUR] ?: 18,
            meetingMinute = prefs[Keys.MEETING_MIN] ?: 0,
            dailyEncouragementEnabled = prefs[Keys.ENCOURAGEMENT_ENABLED] ?: true,
            dailyEncouragementHour = prefs[Keys.ENCOURAGEMENT_HOUR] ?: 14,
            dailyEncouragementMinute = prefs[Keys.ENCOURAGEMENT_MIN] ?: 0
        )
    }

    suspend fun getReminderSettingsSnapshot(): ReminderSettings = reminderSettingsFlow.first()

    suspend fun setActiveUserId(userId: String?) {
        context.dataStore.edit { prefs ->
            if (userId.isNullOrBlank()) prefs.remove(Keys.ACTIVE_USER_ID)
            else prefs[Keys.ACTIVE_USER_ID] = userId
        }
    }

    suspend fun markWelcomeSeen() {
        context.dataStore.edit { prefs ->
            prefs[Keys.HAS_SEEN_WELCOME] = true
        }
    }

    suspend fun verifyAdminPin(pin: String): Boolean {
        val storedHash = context.dataStore.data.first()[Keys.ADMIN_PIN_HASH]
            ?: PinHasher.hashPin(DEFAULT_ADMIN_PIN)
        return PinHasher.matches(pin, storedHash)
    }

    suspend fun updateAdminPin(newPin: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.ADMIN_PIN_HASH] = PinHasher.hashPin(newPin)
        }
    }

    suspend fun saveReminderSettings(settings: ReminderSettings) {
        context.dataStore.edit { prefs ->
            prefs[Keys.POINTS_APPROVAL_ENABLED] = settings.pointsApprovalEnabled
            prefs[Keys.DAILY_TASKS_ENABLED] = settings.dailyTasksReminderEnabled
            prefs[Keys.DAILY_TASKS_HOUR] = settings.dailyTasksReminderHour
            prefs[Keys.DAILY_TASKS_MIN] = settings.dailyTasksReminderMinute
            prefs[Keys.BIBLE_READING_ENABLED] = settings.bibleReadingReminderEnabled
            prefs[Keys.BIBLE_BOOK_ID] = settings.bibleBookId
            prefs[Keys.BIBLE_READING_HOUR] = settings.bibleReadingHour
            prefs[Keys.BIBLE_READING_MIN] = settings.bibleReadingMinute
            prefs[Keys.BIBLE_START_DATE_ISO] = settings.bibleStartDateIso
            prefs[Keys.MORNING_ENABLED] = settings.morningPrayerEnabled
            prefs[Keys.MORNING_HOUR] = settings.morningPrayerHour
            prefs[Keys.MORNING_MIN] = settings.morningPrayerMinute
            prefs[Keys.NIGHT_ENABLED] = settings.nightPrayerEnabled
            prefs[Keys.NIGHT_HOUR] = settings.nightPrayerHour
            prefs[Keys.NIGHT_MIN] = settings.nightPrayerMinute
            prefs[Keys.LITURGY_ENABLED] = settings.liturgyEnabled
            prefs[Keys.LITURGY_DOW] = settings.liturgyDayOfWeek
            prefs[Keys.LITURGY_HOUR] = settings.liturgyHour
            prefs[Keys.LITURGY_MIN] = settings.liturgyMinute
            prefs[Keys.MEETING_ENABLED] = settings.meetingEnabled
            prefs[Keys.MEETING_DOW] = settings.meetingDayOfWeek
            prefs[Keys.MEETING_HOUR] = settings.meetingHour
            prefs[Keys.MEETING_MIN] = settings.meetingMinute
            prefs[Keys.ENCOURAGEMENT_ENABLED] = settings.dailyEncouragementEnabled
            prefs[Keys.ENCOURAGEMENT_HOUR] = settings.dailyEncouragementHour
            prefs[Keys.ENCOURAGEMENT_MIN] = settings.dailyEncouragementMinute
        }
    }

    suspend fun getApprovalSnapshot(userId: String): ApprovalSnapshot? {
        val key = stringPreferencesKey("approval_snapshot_$userId")
        val raw = context.dataStore.data.first()[key] ?: return null
        return runCatching {
            val obj = JSONObject(raw)
            val idsArr = obj.optJSONArray("ids") ?: JSONArray()
            val ids = buildSet {
                for (i in 0 until idsArr.length()) {
                    val id = idsArr.optString(i)
                    if (id.isNotBlank()) add(id)
                }
            }
            ApprovalSnapshot(
                seenApprovedIds = ids,
                lastBonusPoints = obj.optInt("bonus", 0)
            )
        }.getOrNull()
    }

    suspend fun saveApprovalSnapshot(userId: String, snapshot: ApprovalSnapshot) {
        val key = stringPreferencesKey("approval_snapshot_$userId")
        val arr = JSONArray()
        snapshot.seenApprovedIds.forEach { arr.put(it) }
        val payload = JSONObject().apply {
            put("ids", arr)
            put("bonus", snapshot.lastBonusPoints)
        }.toString()
        context.dataStore.edit { prefs ->
            prefs[key] = payload
        }
    }

    companion object {
        const val DEFAULT_ADMIN_PIN = "2026"
    }
}
