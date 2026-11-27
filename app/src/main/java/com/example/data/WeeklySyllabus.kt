package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

data class WeeklySyllabusItem(
    val weekNumber: Int,
    val titleAr: String,
    val dateRangeAr: String,
    val startIso: String,
    val endIso: String,
    val bibleRangeAr: String,
    val incarnationBookAr: String,
    val pdfStartPage: Int,
    val pdfEndPage: Int,
    val examLabelAr: String,
    val examDateAr: String,
    val isFinalWeek: Boolean = false
)

enum class SyllabusPhase {
    UPCOMING,
    ACTIVE,
    COMPLETED
}

data class ActiveSyllabusStatus(
    val activeWeek: WeeklySyllabusItem,
    val phase: SyllabusPhase
)

data class OfficialBibleDayPlan(
    val dayNumber: Int,
    val weekNumber: Int,
    val dateIso: String,
    val dateLabelAr: String,
    val bookId: Int,
    val bookNameAr: String,
    val chapters: List<Int>,
    val shortLabelAr: String,
    val fullLabelAr: String
)

data class BibleVerseItem(
    val chapter: Int,
    val verse: Int,
    val text: String
)

data class WeeklyQuizQuestion(
    val id: String,
    val type: QuizQuestionType,
    val prompt: String,
    val options: List<String>,
    val correctIndex: Int = 0
)

data class DecodedQuizConfig(
    val introText: String,
    val questions: List<WeeklyQuizQuestion>
)

object WeeklySyllabusSchedule {
    const val INCARNATION_BOOK_ASSET_NAME = "on_the_incarnation.pdf"
    private const val QUIZ_JSON_MARKER = "\n<!--QUIZ_JSON-->\n"

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    private val verseCache = ConcurrentHashMap<String, List<BibleVerseItem>>()

    val weeks: List<WeeklySyllabusItem> = listOf(
        WeeklySyllabusItem(
            weekNumber = 1,
            titleAr = "الأسبوع 1",
            dateRangeAr = "30 نوفمبر – 6 ديسمبر",
            startIso = "2026-11-30",
            endIso = "2026-12-06",
            bibleRangeAr = "إنجيل لوقا (إصحاح 1 – 7) — إصحاح يومياً",
            incarnationBookAr = "سيرة القديس أثناسيوس + التمهيد + الفصل الأول (ص 7 – 20 | الفقرات 1 : 7)",
            pdfStartPage = 7,
            pdfEndPage = 19,
            examLabelAr = "موعد المسابقة",
            examDateAr = "الأحد 6 ديسمبر"
        ),
        WeeklySyllabusItem(
            weekNumber = 2,
            titleAr = "الأسبوع 2",
            dateRangeAr = "7 ديسمبر – 13 ديسمبر",
            startIso = "2026-12-07",
            endIso = "2026-12-13",
            bibleRangeAr = "إنجيل لوقا (إصحاح 8 – 14) — إصحاح يومياً",
            incarnationBookAr = "الفصل الثاني والفصل الثالث (ص 21 – 32 | الفقرات 8 : 16)",
            pdfStartPage = 20,
            pdfEndPage = 31,
            examLabelAr = "موعد المسابقة",
            examDateAr = "الأحد 13 ديسمبر"
        ),
        WeeklySyllabusItem(
            weekNumber = 3,
            titleAr = "الأسبوع 3",
            dateRangeAr = "14 ديسمبر – 20 ديسمبر",
            startIso = "2026-12-14",
            endIso = "2026-12-20",
            bibleRangeAr = "إنجيل لوقا (إصحاح 15 – 21) — إصحاح يومياً",
            incarnationBookAr = "الفصل الرابع كاملاً (ص 33 – 53 | الفقرات 17 : 32)",
            pdfStartPage = 32,
            pdfEndPage = 52,
            examLabelAr = "موعد المسابقة",
            examDateAr = "الأحد 20 ديسمبر"
        ),
        WeeklySyllabusItem(
            weekNumber = 4,
            titleAr = "الأسبوع 4",
            dateRangeAr = "21 ديسمبر – 27 ديسمبر",
            startIso = "2026-12-21",
            endIso = "2026-12-27",
            bibleRangeAr = "لوقا (22 – 24) + أفسس (1 – 4) — إصحاح يومياً",
            incarnationBookAr = "الفصل الخامس كاملاً (ص 55 – 66 | الفقرات 33 : 40)",
            pdfStartPage = 53,
            pdfEndPage = 64,
            examLabelAr = "موعد المسابقة",
            examDateAr = "الأحد 27 ديسمبر"
        ),
        WeeklySyllabusItem(
            weekNumber = 5,
            titleAr = "الأسبوع 5 والختامي",
            dateRangeAr = "28 ديسمبر – 5 يناير",
            startIso = "2026-12-28",
            endIso = "2027-01-05",
            bibleRangeAr = "أفسس (5 – 6) + فيلبي (1 – 4) + كولوسي (1 – 4)",
            incarnationBookAr = "الفصل السادس كاملاً (ص 67 – 87 | الفقرات 41 : 57)",
            pdfStartPage = 65,
            pdfEndPage = 85,
            examLabelAr = "موعد المسابقة الختامية الكبرى",
            examDateAr = "الثلاثاء 5 يناير",
            isFinalWeek = true
        )
    )

    val officialDailyBiblePlan: List<OfficialBibleDayPlan> by lazy {
        val monthAr = mapOf(1 to "يناير", 11 to "نوفمبر", 12 to "ديسمبر")
        val raw = buildList {
            for (ch in 1..24) {
                add(Triple(42, "إنجيل لوقا", listOf(ch)))
            }
            for (ch in 1..6) {
                add(Triple(49, "رسالة أفسس", listOf(ch)))
            }
            for (ch in 1..4) {
                add(Triple(50, "رسالة فيلبي", listOf(ch)))
            }
            add(Triple(51, "رسالة كولوسي", listOf(1)))
            add(Triple(51, "رسالة كولوسي", listOf(2)))
            add(Triple(51, "رسالة كولوسي", listOf(3, 4)))
        }
        raw.mapIndexed { idx, (bookId, bookNameAr, chapters) ->
            val cal = Calendar.getInstance().apply {
                set(Calendar.YEAR, 2026)
                set(Calendar.MONTH, Calendar.NOVEMBER)
                set(Calendar.DAY_OF_MONTH, 30)
                add(Calendar.DAY_OF_MONTH, idx)
            }
            val y = cal.get(Calendar.YEAR)
            val m = cal.get(Calendar.MONTH) + 1
            val d = cal.get(Calendar.DAY_OF_MONTH)
            val iso = String.format("%04d-%02d-%02d", y, m, d)
            val dateLabel = "$d ${monthAr[m] ?: m}"
            val shortBook = when (bookId) {
                42 -> "لوقا"
                49 -> "أفسس"
                50 -> "فيلبي"
                else -> "كولوسي"
            }
            val shortLabel = if (chapters.size > 1) {
                "$shortBook ${chapters.first()} – ${chapters.last()}"
            } else {
                "$shortBook ${chapters.first()}"
            }
            val fullLabel = if (chapters.size > 1) {
                "$bookNameAr — الإصحاحان ${chapters.first()} و ${chapters.last()}"
            } else {
                "$bookNameAr — الإصحاح ${chapters.first()}"
            }
            OfficialBibleDayPlan(
                dayNumber = idx + 1,
                weekNumber = (idx / 7 + 1).coerceIn(1, 5),
                dateIso = iso,
                dateLabelAr = dateLabel,
                bookId = bookId,
                bookNameAr = bookNameAr,
                chapters = chapters,
                shortLabelAr = shortLabel,
                fullLabelAr = fullLabel
            )
        }
    }

    fun getTodayOfficialBiblePlan(today: FastDate = FastDate.today()): OfficialBibleDayPlan {
        val iso = today.toIsoString()
        val first = officialDailyBiblePlan.first()
        val last = officialDailyBiblePlan.last()
        return when {
            iso <= first.dateIso -> first
            iso >= last.dateIso -> last
            else -> officialDailyBiblePlan.firstOrNull { it.dateIso == iso } ?: first
        }
    }

    private fun cleanVerseHtml(raw: String): String {
        return raw
            .replace(Regex("<S>.*?</S>", RegexOption.IGNORE_CASE), "")
            .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), " ")
            .replace(Regex("<[^>]+>"), "")
            .replace("&nbsp;", " ", ignoreCase = true)
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    suspend fun fetchArabicBibleChapters(
        bookId: Int,
        chapters: List<Int>
    ): Result<List<BibleVerseItem>> = withContext(Dispatchers.IO) {
        runCatching {
            val result = mutableListOf<BibleVerseItem>()
            for (chapter in chapters) {
                val key = "$bookId:$chapter"
                val cached = verseCache[key]
                if (cached != null) {
                    result.addAll(cached)
                    continue
                }
                val req = Request.Builder()
                    .url("https://bolls.life/get-text/SVD/$bookId/$chapter/")
                    .get()
                    .build()
                val bodyStr = httpClient.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) error("HTTP ${resp.code}")
                    resp.body?.string().orEmpty()
                }
                val arr = JSONArray(bodyStr)
                val parsed = buildList {
                    for (i in 0 until arr.length()) {
                        val obj = arr.optJSONObject(i) ?: continue
                        val cleaned = cleanVerseHtml(obj.optString("text", ""))
                        if (cleaned.isNotEmpty()) {
                            add(
                                BibleVerseItem(
                                    chapter = chapter,
                                    verse = obj.optInt("verse", i + 1),
                                    text = cleaned
                                )
                            )
                        }
                    }
                }
                if (parsed.isEmpty()) error("Empty chapter")
                verseCache[key] = parsed
                result.addAll(parsed)
            }
            result
        }
    }

    fun getActiveWeek(today: FastDate = FastDate.today()): ActiveSyllabusStatus {
        val iso = today.toIsoString()
        val first = weeks.first()
        val last = weeks.last()
        return when {
            iso < first.startIso -> ActiveSyllabusStatus(first, SyllabusPhase.UPCOMING)
            iso > last.endIso -> ActiveSyllabusStatus(last, SyllabusPhase.COMPLETED)
            else -> {
                val matched = weeks.firstOrNull { iso >= it.startIso && iso <= it.endIso } ?: first
                ActiveSyllabusStatus(matched, SyllabusPhase.ACTIVE)
            }
        }
    }

    fun decodeQuizConfig(raw: String): DecodedQuizConfig {
        val idx = raw.indexOf(QUIZ_JSON_MARKER)
        if (idx == -1) {
            return DecodedQuizConfig(introText = raw.trim(), questions = emptyList())
        }
        val intro = raw.substring(0, idx).trim()
        val jsonPart = raw.substring(idx + QUIZ_JSON_MARKER.length).trim()
        return DecodedQuizConfig(introText = intro, questions = parseQuestionsJson(jsonPart))
    }

    fun parseQuestionsJson(jsonStr: String): List<WeeklyQuizQuestion> {
        if (jsonStr.isBlank() || jsonStr.trim() == "[]") return emptyList()
        return runCatching {
            val arr = JSONArray(jsonStr)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val prompt = obj.optString("prompt", "").trim()
                    if (prompt.isEmpty()) continue
                    val type = QuizQuestionType.fromKey(obj.optString("type", "mcq"))
                    val options = if (type == QuizQuestionType.TF) {
                        listOf("صح", "خطأ")
                    } else {
                        val optArr = obj.optJSONArray("options")
                        buildList {
                            if (optArr != null) {
                                for (j in 0 until optArr.length()) {
                                    val opt = optArr.optString(j, "").trim()
                                    if (opt.isNotEmpty()) add(opt)
                                }
                            }
                        }
                    }
                    if (options.size < 2) continue
                    val correctIdx = obj.optInt("correctIndex", 0).coerceIn(0, options.lastIndex)
                    add(
                        WeeklyQuizQuestion(
                            id = obj.optString("id", "q_${i + 1}").ifBlank { "q_${i + 1}" },
                            type = type,
                            prompt = prompt,
                            options = options,
                            correctIndex = correctIdx
                        )
                    )
                }
            }
        }.getOrElse { emptyList() }
    }

    fun serializeQuestionsJson(questions: List<WeeklyQuizQuestion>): String {
        val valid = questions.mapNotNull { q ->
            val prompt = q.prompt.trim()
            if (prompt.isEmpty()) return@mapNotNull null
            val options = if (q.type == QuizQuestionType.TF) {
                listOf("صح", "خطأ")
            } else {
                q.options.map { it.trim() }.filter { it.isNotEmpty() }
            }
            if (options.size < 2) return@mapNotNull null
            q.copy(
                prompt = prompt,
                options = options,
                correctIndex = q.correctIndex.coerceIn(0, options.lastIndex)
            )
        }
        val arr = JSONArray()
        valid.forEachIndexed { idx, q ->
            val optArr = JSONArray()
            q.options.forEach { optArr.put(it) }
            arr.put(
                JSONObject().apply {
                    put("id", q.id.ifBlank { "q_${idx + 1}" })
                    put("type", q.type.key)
                    put("prompt", q.prompt)
                    put("options", optArr)
                    put("correctIndex", q.correctIndex)
                }
            )
        }
        return arr.toString()
    }

    fun encodeQuizConfig(introText: String, questions: List<WeeklyQuizQuestion>): String {
        val cleanIntro = introText.trim()
        val json = serializeQuestionsJson(questions)
        if (json == "[]") return cleanIntro
        return "$cleanIntro$QUIZ_JSON_MARKER$json"
    }
}
