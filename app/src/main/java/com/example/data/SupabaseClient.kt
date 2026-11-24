package com.example.data

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class SupabaseClient {
    private val baseUrl: String = sanitizeSupabaseUrl(BuildConfig.SUPABASE_URL)
    private val anonKey: String = sanitizeAnonKey(BuildConfig.SUPABASE_ANON_KEY)

    private fun sanitizeSupabaseUrl(raw: String): String {
        val cleaned = raw
            .trim()
            .removePrefix("NEXT_PUBLIC_SUPABASE_URL=")
            .removePrefix("SUPABASE_URL=")
            .trim('"', '\'', ' ')
            .replace("\n", "")
            .replace("\r", "")

        val supabaseHostMatch = Regex("https?://\\s*([a-zA-Z0-9-]+\\.supabase\\.co)", RegexOption.IGNORE_CASE)
            .findAll(cleaned)
            .lastOrNull()
        if (supabaseHostMatch != null) {
            return "https://${supabaseHostMatch.groupValues[1]}"
        }

        return cleaned
            .replace(Regex("^https?://\\s*https?://", RegexOption.IGNORE_CASE), "https://")
            .replace(" ", "")
            .trimEnd('/')
            .removeSuffix("/rest/v1")
    }

    private fun sanitizeAnonKey(raw: String): String {
        return raw
            .trim()
            .removePrefix("NEXT_PUBLIC_SUPABASE_PUBLISHABLE_KEY=")
            .removePrefix("NEXT_PUBLIC_SUPABASE_ANON_KEY=")
            .removePrefix("SUPABASE_ANON_KEY=")
            .trim('"', '\'', ' ')
            .replace("\n", "")
            .replace("\r", "")
            .trim()
    }

    val isConfigured: Boolean
        get() = baseUrl.startsWith("https://") &&
            !baseUrl.contains("YOUR_SUPABASE_URL") &&
            anonKey.isNotBlank() &&
            !anonKey.contains("YOUR_SUPABASE_ANON_KEY")

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private fun buildUrl(table: String, params: Map<String, String> = emptyMap()): String {
        val builder = "$baseUrl/rest/v1/$table".toHttpUrlOrNull()?.newBuilder()
            ?: throw IllegalStateException("رابط Supabase غير صالح")
        params.forEach { (k, v) -> builder.addQueryParameter(k, v) }
        return builder.build().toString()
    }

    private fun Request.Builder.withSupabaseHeaders(prefer: String? = null): Request.Builder {
        header("apikey", anonKey)
        if (anonKey.startsWith("eyJ")) {
            header("Authorization", "Bearer $anonKey")
        }
        header("Content-Type", "application/json")
        if (prefer != null) header("Prefer", prefer)
        return this
    }

    private fun formatHttpError(action: String, code: Int, body: String?): String {
        if (code == 404) {
            return "HTTP 404 ($action): تأكد من تشغيل كود SQL لإنشاء الجداول في Supabase SQL Editor ثم أعد المحاولة"
        }
        val detail = body?.take(120)?.trim().orEmpty()
        return if (detail.isNotEmpty()) "HTTP $code ($action): $detail" else "HTTP $code ($action)"
    }

    suspend fun fetchUsers(): Result<List<YouthUser>> = withContext(Dispatchers.IO) {
        runCatching {
            val req = Request.Builder()
                .url(buildUrl("users", mapOf("select" to "*", "order" to "created_at.asc")))
                .withSupabaseHeaders()
                .get()
                .build()
            client.newCall(req).execute().use { resp ->
                val bodyStr = resp.body?.string()
                if (!resp.isSuccessful) error(formatHttpError("users", resp.code, bodyStr))
                val arr = JSONArray(bodyStr ?: "[]")
                List(arr.length()) { i ->
                    val o = arr.getJSONObject(i)
                    YouthUser(
                        id = o.getString("id"),
                        fullName = o.getString("full_name"),
                        phone = o.optString("phone", ""),
                        pinHash = o.getString("pin_hash"),
                        bonusPoints = o.optInt("bonus_points", 0),
                        createdAt = o.optLong("created_at", System.currentTimeMillis())
                    )
                }
            }
        }
    }

    suspend fun upsertUser(user: YouthUser): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = JSONObject().apply {
                put("id", user.id)
                put("full_name", user.fullName)
                put("phone", user.phone)
                put("pin_hash", user.pinHash)
                put("bonus_points", user.bonusPoints)
                put("created_at", user.createdAt)
            }
            val req = Request.Builder()
                .url(buildUrl("users"))
                .withSupabaseHeaders("resolution=merge-duplicates,return=minimal")
                .post(payload.toString().toRequestBody(jsonMediaType))
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) error(formatHttpError("upsert user", resp.code, resp.body?.string()))
            }
        }
    }

    suspend fun deleteUser(userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val req = Request.Builder()
                .url(buildUrl("users", mapOf("id" to "eq.$userId")))
                .withSupabaseHeaders("return=minimal")
                .delete()
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) error(formatHttpError("delete user", resp.code, resp.body?.string()))
            }
        }
    }

    suspend fun updateSubmissionUserName(userId: String, newName: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = JSONObject().apply {
                put("user_name", newName)
            }
            val req = Request.Builder()
                .url(buildUrl("submissions", mapOf("user_id" to "eq.$userId")))
                .withSupabaseHeaders("return=minimal")
                .patch(payload.toString().toRequestBody(jsonMediaType))
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) error(formatHttpError("update submission name", resp.code, resp.body?.string()))
            }
        }
    }

    suspend fun fetchTasks(): Result<List<ChallengeTask>> = withContext(Dispatchers.IO) {
        runCatching {
            val req = Request.Builder()
                .url(buildUrl("tasks", mapOf("select" to "*", "order" to "sort_order.asc")))
                .withSupabaseHeaders()
                .get()
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) error("Supabase tasks HTTP ${resp.code}")
                val arr = JSONArray(resp.body?.string() ?: "[]")
                List(arr.length()) { i ->
                    val o = arr.getJSONObject(i)
                    ChallengeTask(
                        id = o.getString("id"),
                        title = o.getString("title"),
                        description = o.optString("description", ""),
                        points = o.optInt("points", 100),
                        frequency = o.optString("frequency", TaskFrequency.DAILY.key),
                        iconKey = o.optString("icon_key", "star"),
                        isActive = o.optBoolean("is_active", true),
                        sortOrder = o.optInt("sort_order", i + 1)
                    )
                }
            }
        }
    }

    suspend fun upsertTask(task: ChallengeTask): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = JSONObject().apply {
                put("id", task.id)
                put("title", task.title)
                put("description", task.description)
                put("points", task.points)
                put("frequency", task.frequency)
                put("icon_key", task.iconKey)
                put("is_active", task.isActive)
                put("sort_order", task.sortOrder)
            }
            val req = Request.Builder()
                .url(buildUrl("tasks"))
                .withSupabaseHeaders("resolution=merge-duplicates,return=minimal")
                .post(payload.toString().toRequestBody(jsonMediaType))
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) error("Supabase upsert task HTTP ${resp.code}")
            }
        }
    }

    suspend fun fetchSubmissions(): Result<List<TaskSubmission>> = withContext(Dispatchers.IO) {
        runCatching {
            val req = Request.Builder()
                .url(buildUrl("submissions", mapOf("select" to "*", "order" to "submitted_at.desc")))
                .withSupabaseHeaders()
                .get()
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) error("Supabase submissions HTTP ${resp.code}")
                val arr = JSONArray(resp.body?.string() ?: "[]")
                List(arr.length()) { i ->
                    val o = arr.getJSONObject(i)
                    TaskSubmission(
                        id = o.getString("id"),
                        userId = o.getString("user_id"),
                        userName = o.getString("user_name"),
                        taskId = o.getString("task_id"),
                        taskTitle = o.getString("task_title"),
                        points = o.optInt("points", 0),
                        periodKey = o.getString("period_key"),
                        submissionDate = o.optString("submission_date", ""),
                        note = o.optString("note", ""),
                        status = o.optString("status", SubmissionStatus.PENDING.key),
                        adminNote = o.optString("admin_note", ""),
                        submittedAt = o.optLong("submitted_at", System.currentTimeMillis()),
                        reviewedAt = if (o.isNull("reviewed_at")) null else o.optLong("reviewed_at")
                    )
                }
            }
        }
    }

    suspend fun upsertSubmission(submission: TaskSubmission): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = JSONObject().apply {
                put("id", submission.id)
                put("user_id", submission.userId)
                put("user_name", submission.userName)
                put("task_id", submission.taskId)
                put("task_title", submission.taskTitle)
                put("points", submission.points)
                put("period_key", submission.periodKey)
                put("submission_date", submission.submissionDate)
                put("note", submission.note)
                put("status", submission.status)
                put("admin_note", submission.adminNote)
                put("submitted_at", submission.submittedAt)
                if (submission.reviewedAt != null) put("reviewed_at", submission.reviewedAt)
                else put("reviewed_at", JSONObject.NULL)
            }
            val req = Request.Builder()
                .url(buildUrl("submissions"))
                .withSupabaseHeaders("resolution=merge-duplicates,return=minimal")
                .post(payload.toString().toRequestBody(jsonMediaType))
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) error("Supabase upsert submission HTTP ${resp.code}")
            }
        }
    }

    suspend fun approveAllPendingRemote(reviewedAt: Long): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = JSONObject().apply {
                put("status", SubmissionStatus.APPROVED.key)
                put("admin_note", "تمت الموافقة الجماعية من الخادم")
                put("reviewed_at", reviewedAt)
            }
            val req = Request.Builder()
                .url(buildUrl("submissions", mapOf("status" to "eq.pending")))
                .withSupabaseHeaders("return=minimal")
                .patch(payload.toString().toRequestBody(jsonMediaType))
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) error("Supabase approve all HTTP ${resp.code}")
            }
        }
    }

    suspend fun fetchAppConfig(): Result<AppConfigEntity?> = withContext(Dispatchers.IO) {
        runCatching {
            val req = Request.Builder()
                .url(buildUrl("app_config", mapOf("select" to "*", "id" to "eq.1")))
                .withSupabaseHeaders()
                .get()
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) error("Supabase config HTTP ${resp.code}")
                val arr = JSONArray(resp.body?.string() ?: "[]")
                if (arr.length() == 0) return@use null
                val o = arr.getJSONObject(0)
                AppConfigEntity(
                    id = 1,
                    weeklyQuizTitle = o.optString("weekly_quiz_title", ""),
                    weeklyQuizQuestion = o.optString("weekly_quiz_question", ""),
                    weeklyQuizUrl = o.optString("weekly_quiz_url", ""),
                    dailyVerseText = o.optString("daily_verse_text", ""),
                    dailyVerseRef = o.optString("daily_verse_ref", ""),
                    dailyEncouragement = o.optString("daily_encouragement", "")
                )
            }
        }
    }

    suspend fun upsertAppConfig(config: AppConfigEntity): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = JSONObject().apply {
                put("id", 1)
                put("weekly_quiz_title", config.weeklyQuizTitle)
                put("weekly_quiz_question", config.weeklyQuizQuestion)
                put("weekly_quiz_url", config.weeklyQuizUrl)
                put("daily_verse_text", config.dailyVerseText)
                put("daily_verse_ref", config.dailyVerseRef)
                put("daily_encouragement", config.dailyEncouragement)
            }
            val req = Request.Builder()
                .url(buildUrl("app_config"))
                .withSupabaseHeaders("resolution=merge-duplicates,return=minimal")
                .post(payload.toString().toRequestBody(jsonMediaType))
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) error("Supabase upsert config HTTP ${resp.code}")
            }
        }
    }
}
