package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

data class LeaderboardEntry(
    val rank: Int,
    val user: YouthUser,
    val approvedTaskPoints: Int,
    val totalApprovedPoints: Int, // approvedTaskPoints + user.bonusPoints
    val pendingPoints: Int,
    val approvedTasksCount: Int,
    val pendingTasksCount: Int
)

class ChallengeRepository(
    private val database: AppDatabase,
    private val preferences: PreferencesManager,
    private val supabase: SupabaseClient
) {
    private val userDao = database.youthUserDao()
    private val taskDao = database.challengeTaskDao()
    private val submissionDao = database.taskSubmissionDao()
    private val configDao = database.appConfigDao()

    val isSupabaseLive: Boolean get() = supabase.isConfigured

    val allUsers: Flow<List<YouthUser>> = userDao.observeAllUsers()
    val allTasks: Flow<List<ChallengeTask>> = taskDao.observeAllTasks()
    val allSubmissions: Flow<List<TaskSubmission>> = submissionDao.observeAllSubmissions()
    val appConfig: Flow<AppConfigEntity> = configDao.observeConfig().map { it ?: AppConfigEntity() }
    val activeUserId: Flow<String?> = preferences.activeUserIdFlow
    val hasSeenWelcome: Flow<Boolean> = preferences.hasSeenWelcomeFlow
    val reminderSettings: Flow<ReminderSettings> = preferences.reminderSettingsFlow

    suspend fun initializeAndSync(): Result<Unit> {
        if (taskDao.countTasks() == 0) {
            taskDao.upsertAll(DefaultChallengeTasks.all)
        }
        if (configDao.getConfig() == null) {
            configDao.upsert(AppConfigEntity())
        }
        return if (supabase.isConfigured) {
            syncFromSupabase()
        } else {
            Result.success(Unit)
        }
    }

    suspend fun syncFromSupabase(): Result<Unit> {
        if (!supabase.isConfigured) return Result.success(Unit)
        return runCatching {
            val remoteTasks = supabase.fetchTasks().getOrThrow()
            if (remoteTasks.isNotEmpty()) {
                taskDao.upsertAll(remoteTasks)
            }
            val remoteUsers = supabase.fetchUsers().getOrThrow()
            if (remoteUsers.isNotEmpty()) {
                userDao.upsertAll(remoteUsers)
            }
            val remoteSubs = supabase.fetchSubmissions().getOrThrow()
            if (remoteSubs.isNotEmpty()) {
                submissionDao.upsertAll(remoteSubs)
            }
            val remoteConfig = supabase.fetchAppConfig().getOrNull()
            if (remoteConfig != null) {
                configDao.upsert(remoteConfig)
            }
        }
    }

    suspend fun loginOrRegisterYouth(
        fullName: String,
        pin: String,
        phone: String
    ): Result<String> {
        val cleanName = fullName.trim()
        val cleanPin = pin.trim()
        val cleanPhone = phone.trim()

        if (cleanName.length < 3) {
            return Result.failure(IllegalArgumentException("يرجى كتابة الاسم بالكامل (3 أحرف على الأقل)"))
        }
        if (cleanPin.length != 4 || !cleanPin.all { it.isDigit() }) {
            return Result.failure(IllegalArgumentException("الرقم السري (PIN) يجب أن يتكون من 4 أرقام فقط"))
        }

        if (supabase.isConfigured) {
            syncFromSupabase()
        }

        val existing = userDao.findByName(cleanName)
        return if (existing != null) {
            if (PinHasher.matches(cleanPin, existing.pinHash)) {
                preferences.setActiveUserId(existing.id)
                Result.success("مرحباً بعودتك يا ${existing.fullName}!")
            } else {
                Result.failure(IllegalArgumentException("الرقم السري غير صحيح لهذا الاسم. تواصل مع الخادم لإعادة تعيينه."))
            }
        } else {
            val newUser = YouthUser(
                id = UUID.randomUUID().toString(),
                fullName = cleanName,
                phone = cleanPhone,
                pinHash = PinHasher.hashPin(cleanPin),
                bonusPoints = 0,
                createdAt = System.currentTimeMillis()
            )
            if (supabase.isConfigured) {
                supabase.upsertUser(newUser).onFailure {
                    return Result.failure(IllegalStateException("تعذر الاتصال بقاعدة البيانات: ${it.message}"))
                }
            }
            userDao.upsert(newUser)
            preferences.setActiveUserId(newUser.id)
            Result.success("تم تسجيلك بنجاح يا $cleanName! أهلاً بك في تحدي طريق المزود")
        }
    }

    suspend fun logoutYouth() {
        preferences.setActiveUserId(null)
    }

    suspend fun markWelcomeSeen() {
        preferences.markWelcomeSeen()
    }

    suspend fun submitTask(
        user: YouthUser,
        task: ChallengeTask,
        note: String,
        date: FastDate = FastDate.today()
    ): Result<String> {
        if (FastCalendar.isAfterEnd(date)) {
            return Result.failure(
                IllegalStateException("انتهت فترة التحدي يوم 5 يناير 2027 — تم إغلاق تسجيل المهام")
            )
        }

        val isTrial = FastCalendar.isTrialMode(date)
        val freq = TaskFrequency.fromKey(task.frequency)
        val periodKey = FastCalendar.periodKeyForTask(freq, date)
        val existing = submissionDao.findForPeriod(user.id, task.id, periodKey)

        if (existing != null && existing.status != SubmissionStatus.REJECTED.key) {
            val statusLabel = SubmissionStatus.fromKey(existing.status).labelAr
            return Result.failure(IllegalStateException("تم تسجيل «${task.title}» بالفعل لهذه الفترة ($statusLabel)"))
        }

        val submission = TaskSubmission(
            id = if (existing?.status == SubmissionStatus.REJECTED.key) existing.id else UUID.randomUUID().toString(),
            userId = user.id,
            userName = user.fullName,
            taskId = task.id,
            taskTitle = task.title,
            points = task.points,
            periodKey = periodKey,
            submissionDate = date.toIsoString(),
            note = note.trim(),
            status = SubmissionStatus.PENDING.key,
            adminNote = "",
            submittedAt = System.currentTimeMillis(),
            reviewedAt = null
        )

        if (supabase.isConfigured) {
            supabase.upsertSubmission(submission).onFailure {
                return Result.failure(IllegalStateException("تعذر إرسال المشاركة لـ Supabase: ${it.message}"))
            }
        }
        submissionDao.upsert(submission)
        return Result.success(
            if (isTrial) {
                "تم إرسال «${task.title}» (وضع تجريبي 🧪 — النتيجة الرسمية تبدأ 30/11/2026)"
            } else {
                "تم إرسال «${task.title}» للمراجعة ⏳ (+${task.points} نقطة بعد موافقة الخادم)"
            }
        )
    }

    suspend fun reviewSubmission(
        submissionId: String,
        approve: Boolean,
        adminNote: String
    ): Result<String> {
        val existing = submissionDao.findById(submissionId)
            ?: return Result.failure(IllegalStateException("المشاركة غير موجودة"))
        val updated = existing.copy(
            status = if (approve) SubmissionStatus.APPROVED.key else SubmissionStatus.REJECTED.key,
            adminNote = adminNote.trim().ifEmpty {
                if (approve) "أحسنت! ربنا يبارك تعبك" else "يرجى مراجعة المهمة وإعادة الإرسال"
            },
            reviewedAt = System.currentTimeMillis()
        )
        if (supabase.isConfigured) {
            supabase.upsertSubmission(updated).onFailure {
                return Result.failure(IllegalStateException("تعذر تحديث حالة المشاركة في Supabase"))
            }
        }
        submissionDao.upsert(updated)
        return Result.success(if (approve) "تم الاعتماد وإضافة ${updated.points} نقطة!" else "تم رفض المشاركة")
    }

    suspend fun approveAllPending(): Result<String> {
        val now = System.currentTimeMillis()
        if (supabase.isConfigured) {
            supabase.approveAllPendingRemote(now).onFailure {
                return Result.failure(IllegalStateException("تعذر تنفيذ الموافقة الجماعية على Supabase"))
            }
        }
        submissionDao.approveAllPendingLocal(now)
        return Result.success("تمت الموافقة على جميع الطلبات المعلقة وإضافة النقاط!")
    }

    suspend fun adjustUserBonusPoints(userId: String, delta: Int, reason: String): Result<String> {
        val user = userDao.findById(userId)
            ?: return Result.failure(IllegalStateException("المستخدم غير موجود"))
        val updatedUser = user.copy(bonusPoints = (user.bonusPoints + delta).coerceAtLeast(-5000))

        if (supabase.isConfigured) {
            supabase.upsertUser(updatedUser).onFailure {
                return Result.failure(IllegalStateException("تعذر تعديل النقاط على Supabase"))
            }
        }
        userDao.upsert(updatedUser)

        // Also record a transparent approved log entry for the user's activity history
        val noteText = reason.trim().ifEmpty {
            if (delta >= 0) "نقاط مكافأة إضافية من الخادم" else "خصم نقاط من الخادم"
        }
        val adjustmentRecord = TaskSubmission(
            id = UUID.randomUUID().toString(),
            userId = user.id,
            userName = user.fullName,
            taskId = "task_weekly_quiz",
            taskTitle = if (delta >= 0) "مكافأة من الخادم (+$delta)" else "تعديل نقاط من الخادم ($delta)",
            points = 0, // Bonus already counted in user.bonusPoints so we don't double-count
            periodKey = "BONUS_${System.currentTimeMillis()}",
            submissionDate = FastDate.today().toIsoString(),
            note = noteText,
            status = SubmissionStatus.APPROVED.key,
            adminNote = "تعديل يدوي بواسطة الخادم ($delta نقطة)",
            submittedAt = System.currentTimeMillis(),
            reviewedAt = System.currentTimeMillis()
        )
        if (supabase.isConfigured) {
            supabase.upsertSubmission(adjustmentRecord)
        }
        submissionDao.upsert(adjustmentRecord)

        return Result.success("تم تحديث نقاط ${user.fullName} بمقدار ${if (delta >= 0) "+$delta" else "$delta"} نقطة")
    }

    suspend fun resetUserPin(userId: String, newPin: String): Result<String> {
        val cleanPin = newPin.trim()
        if (cleanPin.length != 4 || !cleanPin.all { it.isDigit() }) {
            return Result.failure(IllegalArgumentException("الرقم السري الجديد يجب أن يكون 4 أرقام"))
        }
        val user = userDao.findById(userId)
            ?: return Result.failure(IllegalStateException("المستخدم غير موجود"))
        val updated = user.copy(pinHash = PinHasher.hashPin(cleanPin))
        if (supabase.isConfigured) {
            supabase.upsertUser(updated).onFailure {
                return Result.failure(IllegalStateException("تعذر تحديث الرقم السري على Supabase"))
            }
        }
        userDao.upsert(updated)
        return Result.success("تم إعادة تعيين الرقم السري لـ ${user.fullName} بنجاح")
    }

    suspend fun adminAddUser(
        fullName: String,
        phone: String,
        pin: String,
        initialBonus: Int
    ): Result<String> {
        val cleanName = fullName.trim()
        val cleanPin = pin.trim()
        val cleanPhone = phone.trim()
        if (cleanName.length < 3) {
            return Result.failure(IllegalArgumentException("يرجى إدخال الاسم بالكامل (3 أحرف على الأقل)"))
        }
        if (cleanPin.length != 4 || !cleanPin.all { it.isDigit() }) {
            return Result.failure(IllegalArgumentException("الرقم السري (PIN) يجب أن يتكون من 4 أرقام"))
        }
        if (userDao.findByName(cleanName) != null) {
            return Result.failure(IllegalArgumentException("يوجد مشارك مسجل بنفس الاسم بالفعل"))
        }
        val newUser = YouthUser(
            id = UUID.randomUUID().toString(),
            fullName = cleanName,
            phone = cleanPhone,
            pinHash = PinHasher.hashPin(cleanPin),
            bonusPoints = initialBonus,
            createdAt = System.currentTimeMillis()
        )
        if (supabase.isConfigured) {
            supabase.upsertUser(newUser).onFailure {
                return Result.failure(IllegalStateException("تعذر إضافة المشارك على Supabase: ${it.message}"))
            }
        }
        userDao.upsert(newUser)
        return Result.success("تمت إضافة «$cleanName» إلى تحدي طريق المزود بنجاح")
    }

    suspend fun adminUpdateUser(
        userId: String,
        fullName: String,
        phone: String,
        newPinOptional: String
    ): Result<String> {
        val cleanName = fullName.trim()
        val cleanPhone = phone.trim()
        val cleanPin = newPinOptional.trim()
        if (cleanName.length < 3) {
            return Result.failure(IllegalArgumentException("الاسم بالكامل يجب أن يكون 3 أحرف على الأقل"))
        }
        if (cleanPin.isNotEmpty() && (cleanPin.length != 4 || !cleanPin.all { it.isDigit() })) {
            return Result.failure(IllegalArgumentException("الرقم السري الجديد يجب أن يكون 4 أرقام"))
        }
        val existing = userDao.findById(userId)
            ?: return Result.failure(IllegalStateException("المستخدم غير موجود"))

        val updated = existing.copy(
            fullName = cleanName,
            phone = cleanPhone,
            pinHash = if (cleanPin.isNotEmpty()) PinHasher.hashPin(cleanPin) else existing.pinHash
        )
        if (supabase.isConfigured) {
            supabase.upsertUser(updated).onFailure {
                return Result.failure(IllegalStateException("تعذر تحديث البيانات على Supabase: ${it.message}"))
            }
            if (existing.fullName != cleanName) {
                supabase.updateSubmissionUserName(userId, cleanName)
            }
        }
        userDao.upsert(updated)
        if (existing.fullName != cleanName) {
            submissionDao.updateUserName(userId, cleanName)
        }
        return Result.success("تم تحديث بيانات «$cleanName» بنجاح")
    }

    suspend fun adminDeleteUser(userId: String): Result<String> {
        val existing = userDao.findById(userId)
            ?: return Result.failure(IllegalStateException("المستخدم غير موجود"))
        if (supabase.isConfigured) {
            supabase.deleteUser(userId).onFailure {
                return Result.failure(IllegalStateException("تعذر حذف المشارك من Supabase: ${it.message}"))
            }
        }
        submissionDao.deleteByUserId(userId)
        userDao.deleteById(userId)
        return Result.success("تم حذف المشارك «${existing.fullName}» وجميع مشاركاته")
    }

    suspend fun saveTask(task: ChallengeTask): Result<String> {
        if (supabase.isConfigured) {
            supabase.upsertTask(task).onFailure {
                return Result.failure(IllegalStateException("تعذر حفظ المهمة على Supabase"))
            }
        }
        taskDao.upsert(task)
        return Result.success("تم حفظ تحديثات المهمة «${task.title}»")
    }

    suspend fun saveAppConfig(config: AppConfigEntity): Result<String> {
        if (config.weeklyQuizUrl.isNotBlank() && !UrlValidator.isSafeHttpUrl(config.weeklyQuizUrl)) {
            return Result.failure(IllegalArgumentException("رابط المسابقة يجب أن يبدأ بـ https:// أو http://"))
        }
        if (supabase.isConfigured) {
            supabase.upsertAppConfig(config).onFailure {
                return Result.failure(IllegalStateException("تعذر حفظ الإعدادات على Supabase"))
            }
        }
        configDao.upsert(config)
        return Result.success("تم تحديث المسابقة الأسبوعية وآية اليوم بنجاح")
    }

    suspend fun verifyAdminPin(pin: String): Result<Boolean> {
        val cleanPin = pin.trim()
        if (cleanPin.isEmpty()) return Result.success(false)
        if (supabase.isConfigured) {
            return supabase.verifyAdminPin(cleanPin)
        }
        return Result.success(preferences.verifyAdminPin(cleanPin))
    }

    suspend fun updateAdminPin(currentPin: String, newPin: String): Result<String> {
        val cleanCurrent = currentPin.trim()
        val cleanNew = newPin.trim()
        if (cleanNew.length < 4) {
            return Result.failure(IllegalArgumentException("كلمة المرور الجديدة يجب أن تكون 4 أحرف أو أرقام على الأقل"))
        }
        if (supabase.isConfigured) {
            val updated = supabase.updateAdminPin(cleanCurrent, cleanNew).getOrElse {
                val msg = it.message.orEmpty()
                return Result.failure(
                    IllegalStateException(
                        if (msg.contains("404")) "يرجى تشغيل كود SQL لتفعيل حفظ كلمة مرور الأدمن في Supabase أولاً"
                        else "تعذر تحديث كلمة المرور في قاعدة البيانات"
                    )
                )
            }
            if (!updated) {
                return Result.failure(IllegalArgumentException("كلمة المرور الحالية غير صحيحة"))
            }
            preferences.updateAdminPin(cleanNew)
            return Result.success("تم تحديث كلمة مرور الأدمن في قاعدة البيانات بنجاح ✅")
        }
        if (!preferences.verifyAdminPin(cleanCurrent)) {
            return Result.failure(IllegalArgumentException("كلمة المرور الحالية غير صحيحة"))
        }
        preferences.updateAdminPin(cleanNew)
        return Result.success("تم تغيير كلمة مرور الأدمن بنجاح")
    }

    suspend fun saveReminderSettings(settings: ReminderSettings) {
        preferences.saveReminderSettings(settings)
    }

    fun computeLeaderboard(
        users: List<YouthUser>,
        submissions: List<TaskSubmission>,
        today: FastDate = FastDate.today()
    ): List<LeaderboardEntry> {
        val isTrialNow = FastCalendar.isTrialMode(today)
        // Before 30/11/2026: show trial points so users & admin can practice end-to-end.
        // Starting 30/11/2026: automatically count ONLY official submissions (30/11/2026 - 5/1/2027).
        val countedSubmissions = if (isTrialNow) {
            submissions
        } else {
            submissions.filter { FastCalendar.isOfficialSubmission(it) }
        }
        val grouped = countedSubmissions.groupBy { it.userId }
        val unsorted = users.map { user ->
            val userSubs = grouped[user.id].orEmpty()
            val approvedSubs = userSubs.filter { it.status == SubmissionStatus.APPROVED.key }
            val pendingSubs = userSubs.filter { it.status == SubmissionStatus.PENDING.key }
            val approvedTaskPts = approvedSubs.sumOf { it.points }
            val totalApproved = (approvedTaskPts + user.bonusPoints).coerceAtLeast(0)
            LeaderboardEntry(
                rank = 0,
                user = user,
                approvedTaskPoints = approvedTaskPts,
                totalApprovedPoints = totalApproved,
                pendingPoints = pendingSubs.sumOf { it.points },
                approvedTasksCount = approvedSubs.count { it.points > 0 },
                pendingTasksCount = pendingSubs.size
            )
        }.sortedWith(
            compareByDescending<LeaderboardEntry> { it.totalApprovedPoints }
                .thenByDescending { it.approvedTasksCount }
                .thenBy { it.user.fullName }
        )
        return unsorted.mapIndexed { idx, entry -> entry.copy(rank = idx + 1) }
    }
}
