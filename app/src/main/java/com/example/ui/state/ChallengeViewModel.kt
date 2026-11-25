package com.example.ui.state

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppConfigEntity
import com.example.data.AppDatabase
import com.example.data.ChallengeRepository
import com.example.data.ChallengeTask
import com.example.data.CsvExporter
import com.example.data.FastDate
import com.example.data.LeaderboardEntry
import com.example.data.PreferencesManager
import com.example.data.ReminderSettings
import com.example.data.SubmissionStatus
import com.example.data.SupabaseClient
import com.example.data.TaskSubmission
import com.example.data.YouthUser
import com.example.notifications.ReminderScheduler
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class YouthTab(val titleAr: String) {
    DAILY("اليوميات"),
    MY_LOG("سجلي"),
    MY_RANK("مركزي")
}

data class AppUiState(
    val isLoading: Boolean = true,
    val isSyncing: Boolean = false,
    val isSupabaseLive: Boolean = false,
    val activeUser: YouthUser? = null,
    val allUsers: List<YouthUser> = emptyList(),
    val allTasks: List<ChallengeTask> = emptyList(),
    val allSubmissions: List<TaskSubmission> = emptyList(),
    val userSubmissions: List<TaskSubmission> = emptyList(),
    val pendingSubmissions: List<TaskSubmission> = emptyList(),
    val leaderboard: List<LeaderboardEntry> = emptyList(),
    val activeUserRankEntry: LeaderboardEntry? = null,
    val appConfig: AppConfigEntity = AppConfigEntity(),
    val reminderSettings: ReminderSettings = ReminderSettings(),
    val selectedTab: YouthTab = YouthTab.DAILY,
    val isAdminOpen: Boolean = false,
    val isAdminUnlocked: Boolean = false,
    val hasSeenWelcome: Boolean = true
)

class ChallengeViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val preferences = PreferencesManager(appContext)
    private val database = AppDatabase.getInstance(appContext)
    private val supabase = SupabaseClient()
    private val repository = ChallengeRepository(database, preferences, supabase)

    private val _selectedTab = MutableStateFlow(YouthTab.DAILY)
    private val _isAdminOpen = MutableStateFlow(false)
    private val _isAdminUnlocked = MutableStateFlow(false)
    private val _isSyncing = MutableStateFlow(false)
    private val _hasSeenWelcome = MutableStateFlow(false)

    private val _snackbarMessages = MutableSharedFlow<String>(extraBufferCapacity = 6)
    val snackbarMessages: SharedFlow<String> = _snackbarMessages.asSharedFlow()

    private val dataFlow = combine(
        repository.allUsers,
        repository.allTasks,
        repository.allSubmissions,
        repository.appConfig,
        repository.activeUserId
    ) { users, tasks, submissions, config, activeId ->
        DataSnapshot(users, tasks, submissions, config, activeId)
    }

    private val uiControlsFlow = combine(
        _selectedTab,
        _isAdminOpen,
        _isAdminUnlocked,
        _isSyncing
    ) { tab, adminOpen, adminUnlocked, syncing ->
        UiControls(tab, adminOpen, adminUnlocked, syncing)
    }

    val uiState: StateFlow<AppUiState> = combine(
        dataFlow,
        repository.reminderSettings,
        _hasSeenWelcome,
        uiControlsFlow
    ) { data, reminders, seenWelcome, controls ->
        val activeUser = data.users.firstOrNull { it.id == data.activeId }
        val leaderboard = repository.computeLeaderboard(data.users, data.submissions)
        val activeRank = activeUser?.let { u -> leaderboard.firstOrNull { it.user.id == u.id } }
        val userSubs = activeUser?.let { u ->
            data.submissions.filter { it.userId == u.id }
        }.orEmpty()
        val isTrialNow = com.example.data.FastCalendar.isTrialMode()
        val pendingSubs = data.submissions
            .filter {
                it.status == SubmissionStatus.PENDING.key &&
                    (isTrialNow || com.example.data.FastCalendar.isOfficialSubmission(it))
            }
            .sortedBy { it.submittedAt }

        AppUiState(
            isLoading = false,
            isSyncing = controls.isSyncing,
            isSupabaseLive = repository.isSupabaseLive,
            activeUser = activeUser,
            allUsers = data.users,
            allTasks = data.tasks,
            allSubmissions = data.submissions,
            userSubmissions = userSubs,
            pendingSubmissions = pendingSubs,
            leaderboard = leaderboard,
            activeUserRankEntry = activeRank,
            appConfig = data.config,
            reminderSettings = reminders,
            selectedTab = controls.selectedTab,
            isAdminOpen = controls.isAdminOpen,
            isAdminUnlocked = controls.isAdminUnlocked,
            hasSeenWelcome = seenWelcome
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppUiState(isLoading = true, isSupabaseLive = repository.isSupabaseLive)
    )

    init {
        viewModelScope.launch {
            _isSyncing.value = true
            repository.initializeAndSync()
            _isSyncing.value = false
            val settings = preferences.getReminderSettingsSnapshot()
            ReminderScheduler.applyAllReminders(appContext, settings)
        }

        // Background real-time polling when Supabase is configured
        if (repository.isSupabaseLive) {
            viewModelScope.launch {
                while (isActive) {
                    delay(20_000L)
                    repository.syncFromSupabase()
                }
            }
        }
    }

    fun selectTab(tab: YouthTab) {
        _selectedTab.value = tab
    }

    fun completeWelcome() {
        _hasSeenWelcome.value = true
    }

    fun refreshNow() {
        viewModelScope.launch {
            _isSyncing.value = true
            val res = repository.syncFromSupabase()
            _isSyncing.value = false
            res.onSuccess {
                if (repository.isSupabaseLive) emitMessage("تم تحديث البيانات من Supabase")
            }.onFailure {
                emitMessage("تعذر التحديث: ${it.message}")
            }
        }
    }

    fun loginOrRegister(fullName: String, pin: String, phone: String) {
        viewModelScope.launch {
            _isSyncing.value = true
            val result = repository.loginOrRegisterYouth(fullName, pin, phone)
            _isSyncing.value = false
            result.onSuccess { emitMessage(it) }
                .onFailure { emitMessage(it.message ?: "تعذر تسجيل الدخول") }
        }
    }

    fun logoutYouth() {
        viewModelScope.launch {
            repository.logoutYouth()
            _isAdminUnlocked.value = false
            _isAdminOpen.value = false
            _selectedTab.value = YouthTab.DAILY
            emitMessage("تم تسجيل الخروج بنجاح")
        }
    }

    fun submitTask(task: ChallengeTask, note: String) {
        val user = uiState.value.activeUser ?: return
        viewModelScope.launch {
            val result = repository.submitTask(user, task, note, FastDate.today())
            result.onSuccess { emitMessage(it) }
                .onFailure { emitMessage(it.message ?: "تعذر تسجيل الإنجاز") }
        }
    }

    fun openAdmin() {
        _isAdminOpen.value = true
    }

    fun closeAdmin() {
        _isAdminOpen.value = false
    }

    fun unlockAdmin(pin: String, onError: (String) -> Unit) {
        viewModelScope.launch {
            val res = repository.verifyAdminPin(pin)
            res.onSuccess { ok ->
                if (ok) {
                    _isAdminUnlocked.value = true
                    emitMessage("أهلاً بك في لوحة تحكم خدام التحدي")
                    if (repository.isSupabaseLive) refreshNow()
                } else {
                    onError("كلمة مرور الأدمن غير صحيحة")
                }
            }.onFailure { err ->
                val msg = err.message.orEmpty()
                onError(
                    if (msg.contains("404")) "لم يتم تفعيل كلمة مرور الأدمن في قاعدة البيانات بعد — شغّل كود SQL في Supabase أولاً"
                    else "تعذر التحقق من كلمة المرور عبر قاعدة البيانات"
                )
            }
        }
    }

    fun lockAdmin() {
        _isAdminUnlocked.value = false
        _isAdminOpen.value = false
    }

    fun reviewSubmission(submissionId: String, approve: Boolean, note: String = "") {
        viewModelScope.launch {
            val result = repository.reviewSubmission(submissionId, approve, note)
            result.onSuccess { emitMessage(it) }
                .onFailure { emitMessage(it.message ?: "حدث خطأ أثناء المراجعة") }
        }
    }

    fun approveAllPending() {
        viewModelScope.launch {
            val result = repository.approveAllPending()
            result.onSuccess { emitMessage(it) }
                .onFailure { emitMessage(it.message ?: "تعذر تنفيذ الموافقة الجماعية") }
        }
    }

    fun adjustUserBonus(userId: String, delta: Int, reason: String) {
        if (delta == 0) {
            emitMessage("يرجى إدخال عدد نقاط أكبر أو أقل من صفر")
            return
        }
        viewModelScope.launch {
            val result = repository.adjustUserBonusPoints(userId, delta, reason)
            result.onSuccess { emitMessage(it) }
                .onFailure { emitMessage(it.message ?: "تعذر تعديل النقاط") }
        }
    }

    fun resetUserPin(userId: String, newPin: String) {
        viewModelScope.launch {
            val result = repository.resetUserPin(userId, newPin)
            result.onSuccess { emitMessage(it) }
                .onFailure { emitMessage(it.message ?: "تعذر إعادة تعيين الرقم السري") }
        }
    }

    fun adminAddUser(fullName: String, phone: String, pin: String, initialBonus: Int) {
        viewModelScope.launch {
            val result = repository.adminAddUser(fullName, phone, pin, initialBonus)
            result.onSuccess { emitMessage(it) }
                .onFailure { emitMessage(it.message ?: "تعذر إضافة المشارك") }
        }
    }

    fun adminUpdateUser(userId: String, fullName: String, phone: String, newPinOptional: String) {
        viewModelScope.launch {
            val result = repository.adminUpdateUser(userId, fullName, phone, newPinOptional)
            result.onSuccess { emitMessage(it) }
                .onFailure { emitMessage(it.message ?: "تعذر تحديث بيانات المشارك") }
        }
    }

    fun adminDeleteUser(userId: String) {
        viewModelScope.launch {
            val result = repository.adminDeleteUser(userId)
            result.onSuccess { emitMessage(it) }
                .onFailure { emitMessage(it.message ?: "تعذر حذف المشارك") }
        }
    }

    fun updateTask(task: ChallengeTask) {
        viewModelScope.launch {
            val result = repository.saveTask(task)
            result.onSuccess { emitMessage(it) }
                .onFailure { emitMessage(it.message ?: "تعذر حفظ المهمة") }
        }
    }

    fun updateAppConfig(config: AppConfigEntity) {
        viewModelScope.launch {
            val result = repository.saveAppConfig(config)
            result.onSuccess { emitMessage(it) }
                .onFailure { emitMessage(it.message ?: "تعذر حفظ إعدادات المسابقة والآية") }
        }
    }

    fun changeAdminPin(currentPin: String, newPin: String) {
        if (!_isAdminUnlocked.value) {
            emitMessage("غير مسموح — يجب تسجيل الدخول كأدمن أولاً")
            return
        }
        viewModelScope.launch {
            val result = repository.updateAdminPin(currentPin, newPin)
            result.onSuccess { emitMessage(it) }
                .onFailure { emitMessage(it.message ?: "تعذر تغيير كلمة المرور") }
        }
    }

    fun exportLeaderboardCsv() {
        val entries = uiState.value.leaderboard
        if (entries.isEmpty()) {
            emitMessage("لا يوجد مشاركون لتصدير التقرير بعد")
            return
        }
        val result = CsvExporter.exportAndShareCsv(appContext, entries)
        result.onSuccess { emitMessage(it) }
            .onFailure { emitMessage("تعذر تصدير ملف التقرير: ${it.message}") }
    }

    fun updateReminders(settings: ReminderSettings) {
        viewModelScope.launch {
            repository.saveReminderSettings(settings)
            ReminderScheduler.applyAllReminders(appContext, settings)
            emitMessage("تم حفظ مواعيد التنبيهات اليومية")
        }
    }

    fun sendTestEncouragement() {
        val cfg = uiState.value.appConfig
        val ok = ReminderScheduler.showNotificationNow(
            context = appContext,
            notificationId = ReminderScheduler.ID_INSTANT_TEST,
            title = "تحدي طريق المزود ✨",
            message = "${cfg.dailyVerseText} (${cfg.dailyVerseRef}) — ${cfg.dailyEncouragement}"
        )
        emitMessage(
            if (ok) "تم إرسال إشعار تشجيعي الآن!"
            else "يرجى تفعيل صلاحية الإشعارات أولاً"
        )
    }

    private fun emitMessage(msg: String) {
        _snackbarMessages.tryEmit(msg)
    }

    private data class DataSnapshot(
        val users: List<YouthUser>,
        val tasks: List<ChallengeTask>,
        val submissions: List<TaskSubmission>,
        val config: AppConfigEntity,
        val activeId: String?
    )

    private data class UiControls(
        val selectedTab: YouthTab,
        val isAdminOpen: Boolean,
        val isAdminUnlocked: Boolean,
        val isSyncing: Boolean
    )
}
