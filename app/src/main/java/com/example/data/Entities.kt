package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Calendar
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit

data class FastDate(
    val year: Int,
    val month: Int, // 1..12
    val day: Int    // 1..31
) : Comparable<FastDate> {
    fun toIsoString(): String =
        String.format(Locale.US, "%04d-%02d-%02d", year, month, day)

    fun toDisplayString(): String =
        String.format(Locale.US, "%02d/%02d/%04d", day, month, year)

    fun toShortArabicLabel(): String {
        val monthAr = when (month) {
            1 -> "يناير"
            2 -> "فبراير"
            3 -> "مارس"
            4 -> "أبريل"
            5 -> "مايو"
            6 -> "يونيو"
            7 -> "يوليو"
            8 -> "أغسطس"
            9 -> "سبتمبر"
            10 -> "أكتوبر"
            11 -> "نوفمبر"
            12 -> "ديسمبر"
            else -> "$month"
        }
        return "$day $monthAr"
    }

    fun toEpochDayMillis(): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    fun plusDays(days: Int): FastDate {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_YEAR, days)
        }
        return FastDate(
            year = cal.get(Calendar.YEAR),
            month = cal.get(Calendar.MONTH) + 1,
            day = cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    override fun compareTo(other: FastDate): Int {
        if (year != other.year) return year.compareTo(other.year)
        if (month != other.month) return month.compareTo(other.month)
        return day.compareTo(other.day)
    }

    companion object {
        fun today(): FastDate {
            val cal = Calendar.getInstance()
            return FastDate(
                year = cal.get(Calendar.YEAR),
                month = cal.get(Calendar.MONTH) + 1,
                day = cal.get(Calendar.DAY_OF_MONTH)
            )
        }

        fun daysBetween(start: FastDate, end: FastDate): Long {
            val diff = end.toEpochDayMillis() - start.toEpochDayMillis()
            return TimeUnit.MILLISECONDS.toDays(diff)
        }
    }
}

object FastCalendar {
    val START_DATE = FastDate(2026, 11, 30)
    val END_DATE = FastDate(2027, 1, 5)
    const val FAST_DAYS_COUNT: Int = 37
    const val TOTAL_WEEKS: Int = 6
    const val TOP_PRIZE_RANKS: Int = 10

    fun currentFastDayIndex(today: FastDate = FastDate.today()): Int {
        return when {
            today < START_DATE -> 0
            today > END_DATE -> FAST_DAYS_COUNT
            else -> (FastDate.daysBetween(START_DATE, today).toInt() + 1).coerceIn(1, FAST_DAYS_COUNT)
        }
    }

    fun isBeforeStart(today: FastDate = FastDate.today()): Boolean = today < START_DATE

    fun isTrialMode(today: FastDate = FastDate.today()): Boolean = today < START_DATE

    fun isAfterEnd(today: FastDate = FastDate.today()): Boolean = today > END_DATE

    fun isWithinChallengeWindow(today: FastDate = FastDate.today()): Boolean =
        today >= START_DATE && today <= END_DATE

    fun isTrialSubmission(submission: TaskSubmission): Boolean {
        if (submission.periodKey.startsWith("TRIAL_")) return true
        val iso = submission.submissionDate.trim()
        if (iso.length == 10 && iso < START_DATE.toIsoString()) return true
        return false
    }

    fun isOfficialSubmission(submission: TaskSubmission): Boolean {
        if (isTrialSubmission(submission)) return false
        val iso = submission.submissionDate.trim()
        if (iso.length == 10) {
            return iso >= START_DATE.toIsoString() && iso <= END_DATE.toIsoString()
        }
        return true
    }

    fun daysUntilStart(today: FastDate = FastDate.today()): Long =
        FastDate.daysBetween(today, START_DATE).coerceAtLeast(0L)

    fun daysRemainingToFeast(today: FastDate = FastDate.today()): Long =
        FastDate.daysBetween(today, END_DATE).coerceAtLeast(0L)

    fun weekIndexForDate(date: FastDate): Int {
        val dayIndex = when {
            date < START_DATE -> 1
            date > END_DATE -> FAST_DAYS_COUNT
            else -> (FastDate.daysBetween(START_DATE, date).toInt() + 1).coerceIn(1, FAST_DAYS_COUNT)
        }
        return ((dayIndex - 1) / 7 + 1).coerceIn(1, TOTAL_WEEKS)
    }

    fun periodKeyForTask(frequency: TaskFrequency, date: FastDate = FastDate.today()): String {
        if (isTrialMode(date)) {
            return when (frequency) {
                TaskFrequency.DAILY -> "TRIAL_${date.toIsoString()}"
                TaskFrequency.WEEKLY -> "TRIAL_WEEK_${date.toIsoString()}"
                TaskFrequency.ONCE -> "TRIAL_ONCE"
            }
        }
        return when (frequency) {
            TaskFrequency.DAILY -> date.toIsoString()
            TaskFrequency.WEEKLY -> "WEEK_${weekIndexForDate(date)}"
            TaskFrequency.ONCE -> "FAST_2026"
        }
    }

    fun periodLabelForTask(frequency: TaskFrequency, date: FastDate = FastDate.today()): String {
        val base = when (frequency) {
            TaskFrequency.DAILY -> "يوم ${date.toShortArabicLabel()}"
            TaskFrequency.WEEKLY -> if (isTrialMode(date)) "أسبوع تجريبي" else "الأسبوع ${weekIndexForDate(date)} من الصوم"
            TaskFrequency.ONCE -> "مرة خلال صوم الميلاد"
        }
        return if (isTrialMode(date)) "$base (تجريبي)" else base
    }
}

enum class TaskFrequency(val key: String, val labelAr: String) {
    DAILY("daily", "يومي"),
    WEEKLY("weekly", "أسبوعي"),
    ONCE("once", "مرة خلال الصوم");

    companion object {
        fun fromKey(key: String): TaskFrequency =
            entries.firstOrNull { it.key.equals(key, ignoreCase = true) || it.name.equals(key, ignoreCase = true) }
                ?: DAILY
    }
}

enum class SubmissionStatus(val key: String, val labelAr: String) {
    PENDING("pending", "قيد المراجعة"),
    APPROVED("approved", "مقبول"),
    REJECTED("rejected", "مرفوض");

    companion object {
        fun fromKey(key: String): SubmissionStatus =
            entries.firstOrNull { it.key.equals(key, ignoreCase = true) || it.name.equals(key, ignoreCase = true) }
                ?: PENDING
    }
}

@Entity(tableName = "users")
data class YouthUser(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val fullName: String,
    val phone: String = "",
    val pinHash: String,
    val bonusPoints: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "tasks")
data class ChallengeTask(
    @PrimaryKey val id: String,
    val title: String,
    val description: String = "",
    val points: Int,
    val frequency: String, // "daily", "weekly", "once"
    val iconKey: String = "star",
    val isActive: Boolean = true,
    val sortOrder: Int = 0
)

@Entity(tableName = "submissions")
data class TaskSubmission(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val userName: String,
    val taskId: String,
    val taskTitle: String,
    val points: Int,
    val periodKey: String,       // e.g. "2026-11-25" | "WEEK_1" | "FAST_2026"
    val submissionDate: String,  // "yyyy-MM-dd"
    val note: String = "",
    val status: String = SubmissionStatus.PENDING.key,
    val adminNote: String = "",
    val submittedAt: Long = System.currentTimeMillis(),
    val reviewedAt: Long? = null
)

@Entity(tableName = "app_config")
data class AppConfigEntity(
    @PrimaryKey val id: Int = 1,
    val activeWeek: Int = 1,
    val weeklyQuizTitle: String = "مسابقة الأسبوع الأول • بشارة الميلاد",
    val weeklyQuizQuestion: String = "اقرأ أصحاح 1 و 2 من إنجيل معلمنا لوقا وشارك في المسابقة الأسبوعية للحصول على 200 نقطة!",
    val weeklyQuizUrl: String = "",
    val weeklyQuizQuestionsJson: String = "[]",
    val dailyVerseText: String = "«المجد لله في الأعالي، وعلى الأرض السلام، وبالناس المسرة»",
    val dailyVerseRef: String = "إنجيل لوقا 2: 14",
    val dailyEncouragement: String = "كل خطوة في طريق المزود تقربنا لملك السلام. سجّل إنجازاتك اليومية ليعتمدها الخادم!"
)

object DefaultChallengeTasks {
    val all: List<ChallengeTask> = listOf(
        ChallengeTask(
            id = "task_bible_reading",
            title = "قراءة الإنجيل اليومية",
            description = "قراءة أصحاح يومي من الكتاب المقدس بتأمل",
            points = 100,
            frequency = TaskFrequency.DAILY.key,
            iconKey = "book",
            isActive = true,
            sortOrder = 1
        ),
        ChallengeTask(
            id = "task_weekly_quiz",
            title = "مسابقة نهاية الأسبوع",
            description = "المشاركة والإجابة في المسابقة الأسبوعية للاجتماع",
            points = 200,
            frequency = TaskFrequency.WEEKLY.key,
            iconKey = "quiz",
            isActive = true,
            sortOrder = 2
        ),
        ChallengeTask(
            id = "task_confession",
            title = "سر الاعتراف",
            description = "ممارسة سر التوبة والاعتراف خلال صوم الميلاد",
            points = 200,
            frequency = TaskFrequency.ONCE.key,
            iconKey = "confession",
            isActive = true,
            sortOrder = 3
        ),
        ChallengeTask(
            id = "task_liturgy",
            title = "حضور القداس والتناول",
            description = "حضور القداس الإلهي والتناول من الأسرار المقدسة",
            points = 100,
            frequency = TaskFrequency.WEEKLY.key,
            iconKey = "church",
            isActive = true,
            sortOrder = 4
        ),
        ChallengeTask(
            id = "task_kiahk_tasbeha",
            title = "حضور تسبحة كيهك",
            description = "المشاركة في سهرة التسبحة الكيهكية الأسبوعية",
            points = 150,
            frequency = TaskFrequency.WEEKLY.key,
            iconKey = "music",
            isActive = true,
            sortOrder = 5
        ),
        ChallengeTask(
            id = "task_meeting",
            title = "حضور الاجتماع أو التلمذة",
            description = "حضور اجتماع أبو سيفين للشباب الخريجين أو لقاء التلمذة",
            points = 100,
            frequency = TaskFrequency.WEEKLY.key,
            iconKey = "groups",
            isActive = true,
            sortOrder = 6
        ),
        ChallengeTask(
            id = "task_agpeya",
            title = "صلاة الأجبية (ساعة على الأقل)",
            description = "صلاة ساعة على الأقل يومياً من كتاب الأجبية المقدسة",
            points = 100,
            frequency = TaskFrequency.DAILY.key,
            iconKey = "prayer",
            isActive = true,
            sortOrder = 7
        ),
        ChallengeTask(
            id = "task_jesus_prayer",
            title = "صلاة يسوع",
            description = "ترديد صلاة يسوع «يا ربي يسوع المسيح ارحمني أنا الخاطئ» بتركيز",
            points = 100,
            frequency = TaskFrequency.DAILY.key,
            iconKey = "heart",
            isActive = true,
            sortOrder = 8
        ),
        ChallengeTask(
            id = "task_fasting_metanoias",
            title = "الصوم الانقطاعي والميطانيات",
            description = "الالتزام بالصوم الانقطاعي حسب إرشاد أب الاعتراف مع الميطانيات",
            points = 100,
            frequency = TaskFrequency.DAILY.key,
            iconKey = "flame",
            isActive = true,
            sortOrder = 9
        )
    )

    val addonWeeklyTasks: List<ChallengeTask> = listOf(
        ChallengeTask(
            id = "task_incarnation_book",
            title = "قراءة كتاب \"في تجسد الكلمة\" الأسبوعية",
            description = "قراءة الجزء المقرر أسبوعياً من كتاب \"في تجسد الكلمة\" للقديس أثناسيوس الرسولي حسب منهج الأسبوع (200 نقطة أسبوعياً)",
            points = 200,
            frequency = TaskFrequency.WEEKLY.key,
            iconKey = "book",
            isActive = true,
            sortOrder = 10
        ),
        ChallengeTask(
            id = "task_weekly_exam_incarnation",
            title = "المسابقة الأسبوعية (إنجيل + كتاب تجسد الكلمة)",
            description = "المسابقة الأسبوعية كل أحد وتشمل قراءات الكتاب المقدس وفصول كتاب \"في تجسد الكلمة\" المقررة للأسبوع (200 نقطة أسبوعياً)",
            points = 200,
            frequency = TaskFrequency.WEEKLY.key,
            iconKey = "quiz",
            isActive = true,
            sortOrder = 11
        )
    )

    val allWithAddons: List<ChallengeTask> = all + addonWeeklyTasks
}
