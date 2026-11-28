package com.example.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import java.util.TimeZone

data class DailyDevotion(
    val dayNumber: Int,
    val chapterLabel: String,
    val verseText: String,
    val verseRef: String,
    val fatherQuote: String,
    val fatherName: String
)

data class AutoMcqQuestion(
    val id: String,
    val q: String,
    val options: List<String>,
    val correctIndex: Int
)

data class AutoTfQuestion(
    val id: String,
    val q: String,
    val correctAnswer: Boolean
)

data class AutoMatchPair(
    val left: String,
    val right: String
)

data class AutoMatchGroup(
    val id: String,
    val title: String,
    val pairs: List<AutoMatchPair>
)

data class AutoWeeklyQuiz(
    val weekNumber: Int,
    val titleAr: String,
    val subtitleAr: String,
    val pointsPerItem: Int,
    val totalItems: Int,
    val maxPoints: Int,
    val mcq: List<AutoMcqQuestion>,
    val tf: List<AutoTfQuestion>,
    val match: List<AutoMatchGroup>
)

data class AutoQuizDraft(
    val mcqAnswers: Map<String, Int> = emptyMap(),
    val tfAnswers: Map<String, Boolean> = emptyMap(),
    /** Keyed as "${groupId}:${leftIdx}" -> original rightIdx */
    val matchAnswers: Map<String, Int> = emptyMap()
)

data class AutoQuizGradeResult(
    val mcqCorrect: Int,
    val mcqTotal: Int,
    val tfCorrect: Int,
    val tfTotal: Int,
    val matchCorrect: Int,
    val matchTotal: Int,
    val correctCount: Int,
    val totalItems: Int,
    val answeredCount: Int,
    val earnedPoints: Int,
    val maxPoints: Int,
    val percent: Int
)

object AutoContentBank {
    val DEFAULT_ENABLED_QUIZ_WEEKS: List<Int> = listOf(1)
    private const val PREFS_DRAFTS = "tariq_auto_quiz_drafts_v1"

    @Volatile
    private var cachedDevotions: List<DailyDevotion>? = null

    private val cachedQuizzes = mutableMapOf<Int, AutoWeeklyQuiz>()

    private fun readAsset(context: Context, fileName: String): String {
        return context.assets.open(fileName).bufferedReader(Charsets.UTF_8).use { it.readText() }
    }

    fun getAllDevotions(context: Context): List<DailyDevotion> {
        cachedDevotions?.let { return it }
        val loaded = runCatching {
            val arr = JSONArray(readAsset(context, "daily_devotions.json"))
            List(arr.length()) { idx ->
                val o = arr.getJSONObject(idx)
                DailyDevotion(
                    dayNumber = o.optInt("dayNumber", idx + 1),
                    chapterLabel = o.optString("chapterLabel", ""),
                    verseText = o.optString("verseText", ""),
                    verseRef = o.optString("verseRef", ""),
                    fatherQuote = o.optString("fatherQuote", ""),
                    fatherName = o.optString("fatherName", "")
                )
            }
        }.getOrDefault(emptyList())
        if (loaded.isNotEmpty()) {
            cachedDevotions = loaded
        }
        return loaded
    }

    private fun utcEpochDay(date: FastDate): Long {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(date.year, date.month - 1, date.day, 0, 0, 0)
        }
        return cal.timeInMillis / 86_400_000L
    }

    fun getTodayAutoDevotion(context: Context, today: FastDate = FastDate.today()): DailyDevotion {
        val list = getAllDevotions(context)
        if (list.isEmpty()) {
            return DailyDevotion(
                dayNumber = 1,
                chapterLabel = "إنجيل لوقا — الإصحاح 2",
                verseText = "«الْمَجْدُ للهِ فِي الأَعَالِي، وَعَلَى الأَرْضِ السَّلاَمُ، وَبِالنَّاسِ الْمَسَرَّةُ»",
                verseRef = "إنجيل لوقا 2: 14",
                fatherQuote = "أخلى الكلمة ذاته وُلد في مزود حقير ليعلمنا أن التواضع هو الطريق الملكي لمعاينة مجد الله.",
                fatherName = "القديس كيرلس عمود الدين"
            )
        }
        val isBefore = today < FastCalendar.START_DATE
        val isAfter = today > FastCalendar.END_DATE
        if (!isBefore && !isAfter) {
            val dayIdx = FastDate.daysBetween(FastCalendar.START_DATE, today)
                .toInt()
                .coerceIn(0, list.lastIndex)
            return list[dayIdx]
        }
        val epoch = utcEpochDay(today)
        val count = list.size
        val rotatedIdx = (((epoch % count) + count) % count).toInt()
        return list[rotatedIdx]
    }

    fun getAutoWeeklyQuiz(context: Context, weekNumber: Int): AutoWeeklyQuiz {
        val safeWeek = weekNumber.coerceIn(1, 5)
        synchronized(cachedQuizzes) {
            cachedQuizzes[safeWeek]?.let { return it }
        }
        val parsed = runCatching {
            val obj = JSONObject(readAsset(context, "quiz_week_$safeWeek.json"))
            val mcqArr = obj.optJSONArray("mcq") ?: JSONArray()
            val mcqList = List(mcqArr.length()) { i ->
                val item = mcqArr.getJSONObject(i)
                val optsArr = item.optJSONArray("o") ?: JSONArray()
                val opts = List(optsArr.length()) { j -> optsArr.optString(j, "") }
                AutoMcqQuestion(
                    id = item.optString("id", "w${safeWeek}_m${i + 1}"),
                    q = item.optString("q", ""),
                    options = opts,
                    correctIndex = item.optInt("a", 0).coerceIn(0, (opts.size - 1).coerceAtLeast(0))
                )
            }
            val tfArr = obj.optJSONArray("tf") ?: JSONArray()
            val tfList = List(tfArr.length()) { i ->
                val item = tfArr.getJSONObject(i)
                AutoTfQuestion(
                    id = item.optString("id", "w${safeWeek}_t${i + 1}"),
                    q = item.optString("q", ""),
                    correctAnswer = item.optBoolean("a", true)
                )
            }
            val matchArr = obj.optJSONArray("match") ?: JSONArray()
            val matchList = List(matchArr.length()) { i ->
                val g = matchArr.getJSONObject(i)
                val pairsArr = g.optJSONArray("pairs") ?: JSONArray()
                val pairs = List(pairsArr.length()) { j ->
                    val p = pairsArr.getJSONObject(j)
                    AutoMatchPair(
                        left = p.optString("l", ""),
                        right = p.optString("r", "")
                    )
                }
                AutoMatchGroup(
                    id = g.optString("id", "w${safeWeek}_g${i + 1}"),
                    title = g.optString("title", ""),
                    pairs = pairs
                )
            }
            val totalItems = mcqList.size + tfList.size + matchList.sumOf { it.pairs.size }
            val ptsPerItem = obj.optInt("pointsPerItem", 2)
            AutoWeeklyQuiz(
                weekNumber = obj.optInt("weekNumber", safeWeek),
                titleAr = obj.optString("titleAr", "مسابقة الأسبوع $safeWeek"),
                subtitleAr = obj.optString("subtitleAr", ""),
                pointsPerItem = ptsPerItem,
                totalItems = totalItems,
                maxPoints = totalItems * ptsPerItem,
                mcq = mcqList,
                tf = tfList,
                match = matchList
            )
        }.getOrElse {
            AutoWeeklyQuiz(
                weekNumber = safeWeek,
                titleAr = "مسابقة الأسبوع $safeWeek",
                subtitleAr = "",
                pointsPerItem = 2,
                totalItems = 0,
                maxPoints = 0,
                mcq = emptyList(),
                tf = emptyList(),
                match = emptyList()
            )
        }
        synchronized(cachedQuizzes) {
            cachedQuizzes[safeWeek] = parsed
        }
        return parsed
    }

    fun getAllAutoWeeklyQuizzes(context: Context): List<AutoWeeklyQuiz> =
        (1..5).map { getAutoWeeklyQuiz(context, it) }

    fun autoQuizPeriodKey(weekNumber: Int, today: FastDate = FastDate.today()): String {
        val safe = weekNumber.coerceIn(1, 5)
        return if (FastCalendar.isTrialMode(today)) "TRIAL_QUIZ_WEEK_$safe" else "QUIZ_WEEK_$safe"
    }

    fun findUserAutoQuizSubmission(
        submissions: List<TaskSubmission>,
        userId: String,
        weekNumber: Int,
        today: FastDate = FastDate.today()
    ): TaskSubmission? {
        val key = autoQuizPeriodKey(weekNumber, today)
        return submissions.firstOrNull {
            it.userId == userId &&
                it.periodKey == key &&
                it.status == SubmissionStatus.APPROVED.key
        }
    }

    fun getShuffledRightIndices(groupId: String, length: Int): List<Int> {
        val indices = MutableList(length) { it }
        var seed: Long = 2166136261L
        for (ch in groupId) {
            seed = seed xor ch.code.toLong()
            seed = (seed * 16777619L) and 0xFFFFFFFFL
        }
        fun nextRand(): Double {
            seed = ((seed * 1664525L) + 1013904223L) and 0xFFFFFFFFL
            return seed.toDouble() / 4294967296.0
        }
        for (i in indices.lastIndex downTo 1) {
            val j = (nextRand() * (i + 1)).toInt().coerceIn(0, i)
            val tmp = indices[i]
            indices[i] = indices[j]
            indices[j] = tmp
        }
        if (length >= 2 && indices.indices.all { indices[it] == it }) {
            val tmp = indices[0]
            indices[0] = indices[1]
            indices[1] = tmp
        }
        return indices
    }

    fun gradeAutoQuiz(quiz: AutoWeeklyQuiz, draft: AutoQuizDraft): AutoQuizGradeResult {
        var mcqCorrect = 0
        var answeredCount = 0
        for (item in quiz.mcq) {
            val picked = draft.mcqAnswers[item.id]
            if (picked != null) {
                answeredCount++
                if (picked == item.correctIndex) mcqCorrect++
            }
        }
        var tfCorrect = 0
        for (item in quiz.tf) {
            val picked = draft.tfAnswers[item.id]
            if (picked != null) {
                answeredCount++
                if (picked == item.correctAnswer) tfCorrect++
            }
        }
        var matchCorrect = 0
        var matchTotal = 0
        for (group in quiz.match) {
            group.pairs.indices.forEach { leftIdx ->
                matchTotal++
                val picked = draft.matchAnswers["${group.id}:$leftIdx"]
                if (picked != null) {
                    answeredCount++
                    if (picked == leftIdx) matchCorrect++
                }
            }
        }
        val totalItems = quiz.mcq.size + quiz.tf.size + matchTotal
        val correctCount = mcqCorrect + tfCorrect + matchCorrect
        val earnedPoints = correctCount * quiz.pointsPerItem
        val maxPoints = totalItems * quiz.pointsPerItem
        val percent = if (totalItems > 0) ((correctCount * 100.0) / totalItems).toInt() else 0
        return AutoQuizGradeResult(
            mcqCorrect = mcqCorrect,
            mcqTotal = quiz.mcq.size,
            tfCorrect = tfCorrect,
            tfTotal = quiz.tf.size,
            matchCorrect = matchCorrect,
            matchTotal = matchTotal,
            correctCount = correctCount,
            totalItems = totalItems,
            answeredCount = answeredCount,
            earnedPoints = earnedPoints,
            maxPoints = maxPoints,
            percent = percent
        )
    }

    private fun draftKey(userId: String, weekNumber: Int): String = "draft_${userId}_w${weekNumber}"

    fun loadDraft(context: Context, userId: String, weekNumber: Int): AutoQuizDraft {
        val raw = context.getSharedPreferences(PREFS_DRAFTS, Context.MODE_PRIVATE)
            .getString(draftKey(userId, weekNumber), null) ?: return AutoQuizDraft()
        return runCatching {
            val root = JSONObject(raw)
            val mcqMap = mutableMapOf<String, Int>()
            root.optJSONObject("mcq")?.let { o ->
                val keys = o.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    mcqMap[k] = o.optInt(k, 0)
                }
            }
            val tfMap = mutableMapOf<String, Boolean>()
            root.optJSONObject("tf")?.let { o ->
                val keys = o.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    tfMap[k] = o.optBoolean(k, false)
                }
            }
            val matchMap = mutableMapOf<String, Int>()
            root.optJSONObject("match")?.let { o ->
                val keys = o.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    matchMap[k] = o.optInt(k, 0)
                }
            }
            AutoQuizDraft(mcqMap, tfMap, matchMap)
        }.getOrDefault(AutoQuizDraft())
    }

    fun saveDraft(context: Context, userId: String, weekNumber: Int, draft: AutoQuizDraft) {
        runCatching {
            val root = JSONObject().apply {
                put("mcq", JSONObject().apply {
                    draft.mcqAnswers.forEach { (k, v) -> put(k, v) }
                })
                put("tf", JSONObject().apply {
                    draft.tfAnswers.forEach { (k, v) -> put(k, v) }
                })
                put("match", JSONObject().apply {
                    draft.matchAnswers.forEach { (k, v) -> put(k, v) }
                })
            }
            context.getSharedPreferences(PREFS_DRAFTS, Context.MODE_PRIVATE)
                .edit()
                .putString(draftKey(userId, weekNumber), root.toString())
                .apply()
        }
    }

    fun clearDraft(context: Context, userId: String, weekNumber: Int) {
        context.getSharedPreferences(PREFS_DRAFTS, Context.MODE_PRIVATE)
            .edit()
            .remove(draftKey(userId, weekNumber))
            .apply()
    }
}
