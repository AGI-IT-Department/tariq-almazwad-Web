package com.example.data

import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

enum class QuizQuestionType(val key: String) {
    MCQ("mcq"),
    TF("tf");

    companion object {
        fun fromKey(key: String?): QuizQuestionType =
            if (key.equals("tf", ignoreCase = true)) TF else MCQ
    }
}

data class QuizQuestion(
    val id: String,
    val prompt: String,
    val type: QuizQuestionType,
    val options: List<String>,
    val correctIndex: Int
)

data class WeeklySyllabus(
    val weekNumber: Int, // 1..5
    val titleAr: String,
    val shortLabelAr: String,
    val dateRangeAr: String,
    val bibleTitleAr: String,
    val bibleDetailAr: String,
    val patristicTitleAr: String,
    val patristicDetailAr: String,
    val quizTitleAr: String,
    val quizDateAr: String,
    val quizPoints: Int = 200
)

data class OfficialDailyChapter(
    val dayNumber: Int,
    val weekNumber: Int,
    val bookNameAr: String,
    val chapterLabelAr: String,
    val shortLabelAr: String,
    val fullLabelAr: String,
    val isBeforeChallenge: Boolean,
    val isAfterChallenge: Boolean
)

object OfficialReadingPlan {
    private const val META_START = "\n<!--TARIQ_META:"
    private const val META_END = "-->"

    val weeks: List<WeeklySyllabus> = listOf(
        WeeklySyllabus(
            weekNumber = 1,
            titleAr = "الأسبوع الأول",
            shortLabelAr = "الأسبوع 1",
            dateRangeAr = "30 نوفمبر – 6 ديسمبر",
            bibleTitleAr = "إنجيل لوقا (1 – 7)",
            bibleDetailAr = "إصحاح يومياً من إنجيل معلمنا لوقا البشير",
            patristicTitleAr = "المقدمة + التمهيد + الفصل الأول",
            patristicDetailAr = "ص 7 – ص 20 • فقرات 1 – 7",
            quizTitleAr = "مسابقة الأسبوع الأول",
            quizDateAr = "الأحد 6 ديسمبر"
        ),
        WeeklySyllabus(
            weekNumber = 2,
            titleAr = "الأسبوع الثاني",
            shortLabelAr = "الأسبوع 2",
            dateRangeAr = "7 ديسمبر – 13 ديسمبر",
            bibleTitleAr = "إنجيل لوقا (8 – 14)",
            bibleDetailAr = "إصحاح يومياً من إنجيل معلمنا لوقا البشير",
            patristicTitleAr = "الفصل الثاني + الفصل الثالث",
            patristicDetailAr = "ص 21 – ص 32 • فقرات 8 – 16",
            quizTitleAr = "مسابقة الأسبوع الثاني",
            quizDateAr = "الأحد 13 ديسمبر"
        ),
        WeeklySyllabus(
            weekNumber = 3,
            titleAr = "الأسبوع الثالث",
            shortLabelAr = "الأسبوع 3",
            dateRangeAr = "14 ديسمبر – 20 ديسمبر",
            bibleTitleAr = "إنجيل لوقا (15 – 21)",
            bibleDetailAr = "إصحاح يومياً من إنجيل معلمنا لوقا البشير",
            patristicTitleAr = "الفصل الرابع كاملاً",
            patristicDetailAr = "ص 33 – ص 53 • فقرات 17 – 32",
            quizTitleAr = "مسابقة الأسبوع الثالث",
            quizDateAr = "الأحد 20 ديسمبر"
        ),
        WeeklySyllabus(
            weekNumber = 4,
            titleAr = "الأسبوع الرابع",
            shortLabelAr = "الأسبوع 4",
            dateRangeAr = "21 ديسمبر – 27 ديسمبر",
            bibleTitleAr = "لوقا (22 – 24) + أفسس (1 – 4)",
            bibleDetailAr = "إصحاح يومياً: ختام لوقا وبداية رسالة أفسس",
            patristicTitleAr = "الفصل الخامس كاملاً",
            patristicDetailAr = "ص 55 – ص 66 • فقرات 33 – 40",
            quizTitleAr = "مسابقة الأسبوع الرابع",
            quizDateAr = "الأحد 27 ديسمبر"
        ),
        WeeklySyllabus(
            weekNumber = 5,
            titleAr = "الأسبوع الخامس والختامي",
            shortLabelAr = "الأسبوع 5 (الختامي)",
            dateRangeAr = "28 ديسمبر – 5 يناير",
            bibleTitleAr = "أفسس (5 – 6) + فيلبي (1 – 4) + كولوسي (1 – 4)",
            bibleDetailAr = "ختام رسائل الأسر وفرح الميلاد المجيد",
            patristicTitleAr = "الفصل السادس كاملاً",
            patristicDetailAr = "ص 67 – ص 87 • فقرات 41 – 57",
            quizTitleAr = "المسابقة الختامية الكبرى",
            quizDateAr = "الثلاثاء 5 يناير"
        )
    )

    private data class DayChapterEntry(
        val bookNameAr: String,
        val chapterLabelAr: String,
        val shortBook: String
    )

    private val dailySchedule: List<DayChapterEntry> = buildList {
        // Luke 1..24 (Days 1..24)
        for (ch in 1..24) {
            add(DayChapterEntry("إنجيل لوقا", "الإصحاح $ch", "لوقا $ch"))
        }
        // Ephesians 1..6 (Days 25..30)
        for (ch in 1..6) {
            add(DayChapterEntry("رسالة أفسس", "الإصحاح $ch", "أفسس $ch"))
        }
        // Philippians 1..4 (Days 31..34)
        for (ch in 1..4) {
            add(DayChapterEntry("رسالة فيلبي", "الإصحاح $ch", "فيلبي $ch"))
        }
        // Colossians 1..2 + 3-4 (Days 35..37)
        add(DayChapterEntry("رسالة كولوسي", "الإصحاح 1", "كولوسي 1"))
        add(DayChapterEntry("رسالة كولوسي", "الإصحاح 2", "كولوسي 2"))
        add(DayChapterEntry("رسالة كولوسي", "الإصحاحان 3 و 4", "كولوسي 3 – 4"))
    }

    fun getSyllabusForWeek(weekNumber: Int): WeeklySyllabus {
        val safe = weekNumber.coerceIn(1, 5)
        return weeks[safe - 1]
    }

    fun getTodayChapter(today: FastDate = FastDate.today()): OfficialDailyChapter {
        val isBefore = today < FastCalendar.START_DATE
        val isAfter = today > FastCalendar.END_DATE
        val dayNum = when {
            isBefore -> 1
            isAfter -> dailySchedule.size
            else -> (FastDate.daysBetween(FastCalendar.START_DATE, today).toInt() + 1)
                .coerceIn(1, dailySchedule.size)
        }
        val entry = dailySchedule[dayNum - 1]
        val weekNum = ((dayNum - 1) / 7 + 1).coerceIn(1, 5)
        return OfficialDailyChapter(
            dayNumber = dayNum,
            weekNumber = weekNum,
            bookNameAr = entry.bookNameAr,
            chapterLabelAr = entry.chapterLabelAr,
            shortLabelAr = entry.shortBook,
            fullLabelAr = "${entry.bookNameAr} — ${entry.chapterLabelAr}",
            isBeforeChallenge = isBefore,
            isAfterChallenge = isAfter
        )
    }

    fun isQuizHighlightDay(today: FastDate = FastDate.today()): Boolean {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, today.year)
            set(Calendar.MONTH, today.month - 1)
            set(Calendar.DAY_OF_MONTH, today.day)
        }
        val isSunday = cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY
        val isFinale = today.year == 2027 && today.month == 1 && today.day == 5
        return isSunday || isFinale
    }

    fun parseQuizQuestionsJson(jsonStr: String?): List<QuizQuestion> {
        if (jsonStr.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(jsonStr)
            parseQuizQuestionsArray(arr)
        }.getOrDefault(emptyList())
    }

    private fun parseQuizQuestionsArray(arr: JSONArray): List<QuizQuestion> {
        val list = mutableListOf<QuizQuestion>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val prompt = o.optString("prompt", "").trim()
            if (prompt.isEmpty()) continue
            val type = QuizQuestionType.fromKey(o.optString("type", "mcq"))
            val optionsArr = o.optJSONArray("options")
            val rawOptions = buildList {
                if (optionsArr != null) {
                    for (j in 0 until optionsArr.length()) {
                        val opt = optionsArr.optString(j, "").trim()
                        if (opt.isNotEmpty()) add(opt)
                    }
                }
            }
            val options = if (type == QuizQuestionType.TF) {
                listOf("صح", "خطأ")
            } else if (rawOptions.size >= 2) {
                rawOptions.take(4)
            } else {
                listOf("الإجابة الأولى", "الإجابة الثانية")
            }
            val correctIdx = o.optInt("correctIndex", 0).coerceIn(0, options.lastIndex)
            list.add(
                QuizQuestion(
                    id = o.optString("id", "q_${i + 1}").ifBlank { "q_${i + 1}" },
                    prompt = prompt,
                    type = type,
                    options = options,
                    correctIndex = correctIdx
                )
            )
        }
        return list
    }

    fun serializeQuizQuestionsJson(questions: List<QuizQuestion>): String {
        val arr = JSONArray()
        questions.forEach { q ->
            val obj = JSONObject().apply {
                put("id", q.id)
                put("prompt", q.prompt)
                put("type", q.type.key)
                put("options", JSONArray(q.options))
                put("correctIndex", q.correctIndex.coerceIn(0, (q.options.size - 1).coerceAtLeast(0)))
            }
            arr.put(obj)
        }
        return arr.toString()
    }

    fun stripMetaFromQuestion(rawQuestion: String): String {
        val idx = rawQuestion.indexOf(META_START)
        return if (idx >= 0) rawQuestion.substring(0, idx).trim() else rawQuestion
    }

    fun encodeQuestionWithMeta(
        cleanQuestion: String,
        activeWeek: Int,
        questionsJson: String
    ): String {
        val base = stripMetaFromQuestion(cleanQuestion).trim()
        val questions = parseQuizQuestionsJson(questionsJson)
        val payload = JSONObject().apply {
            put("w", activeWeek.coerceIn(1, 5))
            put("q", JSONArray(serializeQuizQuestionsJson(questions)))
        }
        return "$base$META_START$payload$META_END"
    }

    data class ExtractedQuizMeta(
        val cleanQuestion: String,
        val activeWeek: Int?,
        val questionsJson: String?
    )

    fun extractMetaFromQuestion(rawQuestion: String): ExtractedQuizMeta {
        val startIdx = rawQuestion.indexOf(META_START)
        if (startIdx < 0) return ExtractedQuizMeta(rawQuestion, null, null)
        val clean = rawQuestion.substring(0, startIdx).trim()
        val endIdx = rawQuestion.indexOf(META_END, startIdx + META_START.length)
        if (endIdx < 0) return ExtractedQuizMeta(clean, null, null)
        val jsonPart = rawQuestion.substring(startIdx + META_START.length, endIdx)
        return runCatching {
            val obj = JSONObject(jsonPart)
            val w = if (obj.has("w")) obj.optInt("w", 1).coerceIn(1, 5) else null
            val qArr = obj.optJSONArray("q")
            val qJson = if (qArr != null) serializeQuizQuestionsJson(parseQuizQuestionsArray(qArr)) else null
            ExtractedQuizMeta(clean, w, qJson)
        }.getOrDefault(ExtractedQuizMeta(clean, null, null))
    }
}
