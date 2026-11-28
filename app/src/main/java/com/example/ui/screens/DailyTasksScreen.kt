package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.AutoContentBank
import com.example.data.ChallengeTask
import com.example.data.FastCalendar
import com.example.data.FastDate
import com.example.data.OfficialBibleDayPlan
import com.example.data.OfficialReadingPlan
import com.example.data.SubmissionStatus
import com.example.data.TaskFrequency
import com.example.data.TaskSubmission
import com.example.data.WeeklySyllabusItem
import com.example.data.WeeklySyllabusSchedule
import com.example.ui.components.AutoWeeklyQuizDialog
import com.example.ui.components.DailyVerseBanner
import com.example.ui.components.InAppBibleChapterReaderDialog
import com.example.ui.components.InAppBookPdfReaderDialog
import com.example.ui.components.StatusBadge
import com.example.ui.components.TaskSubmissionBottomSheet
import com.example.ui.components.formatTimeArabic
import com.example.ui.components.sharePlainText
import com.example.ui.components.taskIconForKey
import com.example.ui.state.AppUiState
import com.example.ui.theme.ApprovedEmerald
import com.example.ui.theme.ApprovedEmeraldBg
import com.example.ui.theme.BrightGold
import com.example.ui.theme.MidnightTeal
import com.example.ui.theme.MidnightTealDark
import com.example.ui.theme.NativityBurgundy
import com.example.ui.theme.PendingAmberBg
import com.example.ui.theme.WarmGold

private enum class TaskFilter(val labelAr: String) {
    ALL("كل المهام"),
    DAILY("يومي"),
    WEEKLY("أسبوعي"),
    ONCE("مرة في التحدي")
}

private data class TaskGroupSection(
    val frequency: TaskFrequency,
    val titleAr: String,
    val subtitleAr: String,
    val tasks: List<ChallengeTask>
)

private data class ActiveAutoQuizDialogState(
    val weekNumber: Int,
    val completedPoints: Int?,
    val taskId: String
)

@Composable
fun DailyTasksScreen(
    uiState: AppUiState,
    onSubmitTask: (ChallengeTask, String) -> Unit,
    onSubmitAutoQuiz: (
        weekNumber: Int,
        correctCount: Int,
        totalItems: Int,
        earnedPoints: Int,
        preferredTaskId: String,
        onDone: (Boolean) -> Unit
    ) -> Unit,
    onOpenReadingPlan: () -> Unit = {},
    onOpenMyRank: () -> Unit,
    onOpenReminders: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val user = uiState.activeUser ?: return
    val rankEntry = uiState.activeUserRankEntry
    val today = remember { FastDate.today() }
    val currentDay = remember(today) { FastCalendar.currentFastDayIndex(today) }
    val isBeforeStart = remember(today) { FastCalendar.isBeforeStart(today) }
    val isAfterEnd = remember(today) { FastCalendar.isAfterEnd(today) }
    val isWithinWindow = !isBeforeStart && !isAfterEnd
    val daysToStart = remember(today) { FastCalendar.daysUntilStart(today) }
    val daysToEnd = remember(today) { FastCalendar.daysRemainingToFeast(today) }
    val fastProgress = (currentDay.toFloat() / FastCalendar.FAST_DAYS_COUNT.toFloat()).coerceIn(0f, 1f)

    var selectedFilterIdx by remember { mutableIntStateOf(0) }
    var selectedTaskForSheet by remember { mutableStateOf<ChallengeTask?>(null) }
    var sheetInitialNote by remember { mutableStateOf("") }
    var activeAutoQuiz by remember { mutableStateOf<ActiveAutoQuizDialogState?>(null) }
    var showBookReader by remember { mutableStateOf(false) }
    var showBibleReader by remember { mutableStateOf(false) }

    val activeWeek = remember(today) { WeeklySyllabusSchedule.getActiveWeek(today).activeWeek }
    val todayOfficialBible = remember(today) { WeeklySyllabusSchedule.getTodayOfficialBiblePlan(today) }
    val todayDevotion = remember(today) { AutoContentBank.getTodayAutoDevotion(context, today) }
    val enabledQuizWeeks = remember(uiState.appConfig.weeklyQuizQuestion) {
        OfficialReadingPlan.getEnabledQuizWeeks(uiState.appConfig)
    }

    val activeTasks = remember(uiState.allTasks) {
        uiState.allTasks.filter { it.isActive }.sortedBy { it.sortOrder }
    }

    val bibleTask = remember(activeTasks) { activeTasks.firstOrNull { it.id == "task_bible_reading" } }
    val incarnationTask = remember(activeTasks) { activeTasks.firstOrNull { it.id == "task_incarnation_book" } }

    fun submissionForTask(task: ChallengeTask?): TaskSubmission? {
        if (task == null) return null
        val pk = FastCalendar.periodKeyForTask(TaskFrequency.fromKey(task.frequency), today)
        return uiState.userSubmissions.firstOrNull { it.taskId == task.id && it.periodKey == pk }
    }

    val bibleSubToday = submissionForTask(bibleTask)
    val isBibleLocked = isAfterEnd ||
        bibleSubToday?.status == SubmissionStatus.PENDING.key ||
        bibleSubToday?.status == SubmissionStatus.APPROVED.key

    val incarnationSubThisWeek = submissionForTask(incarnationTask)
    val isIncarnationLocked = isAfterEnd ||
        incarnationSubThisWeek?.status == SubmissionStatus.PENDING.key ||
        incarnationSubThisWeek?.status == SubmissionStatus.APPROVED.key

    fun defaultNoteForTask(task: ChallengeTask): String = when (task.id) {
        "task_bible_reading" -> "تمت قراءة ${todayOfficialBible.fullLabelAr}"
        "task_incarnation_book" -> "تمت قراءة ورد ${activeWeek.titleAr} من كتاب «في تجسد الكلمة»"
        "task_weekly_exam_incarnation", "task_weekly_quiz" ->
            "${activeWeek.titleAr} (${activeWeek.examDateAr}) — إنجيل + كتاب في تجسد الكلمة"
        else -> ""
    }

    fun openTaskSheet(task: ChallengeTask, customNote: String? = null) {
        sheetInitialNote = customNote ?: defaultNoteForTask(task)
        selectedTaskForSheet = task
    }

    selectedTaskForSheet?.let { task ->
        TaskSubmissionBottomSheet(
            task = task,
            initialNote = sheetInitialNote,
            onDismiss = {
                selectedTaskForSheet = null
                sheetInitialNote = ""
            },
            onConfirmSubmit = { note ->
                onSubmitTask(task, note)
                selectedTaskForSheet = null
                sheetInitialNote = ""
            }
        )
    }

    activeAutoQuiz?.let { quizState ->
        val quiz = remember(quizState.weekNumber) {
            AutoContentBank.getAutoWeeklyQuiz(context, quizState.weekNumber)
        }
        AutoWeeklyQuizDialog(
            quiz = quiz,
            userId = user.id,
            completedPoints = quizState.completedPoints,
            onSubmitResult = { weekNum, correct, total, points, onDone ->
                onSubmitAutoQuiz(weekNum, correct, total, points, quizState.taskId, onDone)
            },
            onDismiss = { activeAutoQuiz = null }
        )
    }

    if (showBookReader) {
        InAppBookPdfReaderDialog(
            initialWeekNumber = activeWeek.weekNumber,
            activeWeekNumber = activeWeek.weekNumber,
            points = incarnationTask?.points ?: 200,
            isLocked = isIncarnationLocked,
            statusLabel = when (incarnationSubThisWeek?.status) {
                SubmissionStatus.APPROVED.key -> "تم اعتماد قراءة هذا الأسبوع ✅"
                SubmissionStatus.PENDING.key -> "تم إرسال قراءة الأسبوع للمراجعة ⏳"
                else -> null
            },
            onDismiss = { showBookReader = false },
            onLogWeekReading = incarnationTask?.let { task ->
                { week ->
                    showBookReader = false
                    openTaskSheet(task, "تمت قراءة ورد ${week.titleAr} من كتاب «في تجسد الكلمة»")
                }
            }
        )
    }

    if (showBibleReader) {
        InAppBibleChapterReaderDialog(
            initialDayNumber = todayOfficialBible.dayNumber,
            todayDayNumber = todayOfficialBible.dayNumber,
            points = bibleTask?.points ?: 100,
            isLocked = isBibleLocked,
            statusLabel = when (bibleSubToday?.status) {
                SubmissionStatus.APPROVED.key -> "تم اعتماد قراءة اليوم ✅"
                SubmissionStatus.PENDING.key -> "تم إرسال قراءة اليوم للمراجعة ⏳"
                else -> null
            },
            onDismiss = { showBibleReader = false },
            onLogBibleReading = bibleTask?.let { task ->
                { dayPlan ->
                    showBibleReader = false
                    openTaskSheet(task, "تمت قراءة ${dayPlan.fullLabelAr}")
                }
            }
        )
    }

    val completedNowCount = remember(activeTasks, uiState.userSubmissions, today, enabledQuizWeeks, user.id) {
        activeTasks.count { task ->
            if (task.id == "task_weekly_exam_incarnation" || task.id == "task_weekly_quiz") {
                val solvedAnyEnabled = enabledQuizWeeks.any { w ->
                    AutoContentBank.findUserAutoQuizSubmission(uiState.userSubmissions, user.id, w, today) != null
                }
                if (solvedAnyEnabled) return@count true
            }
            val periodKey = FastCalendar.periodKeyForTask(TaskFrequency.fromKey(task.frequency), today)
            val sub = uiState.userSubmissions.firstOrNull { it.taskId == task.id && it.periodKey == periodKey }
            sub != null && (sub.status == SubmissionStatus.APPROVED.key || sub.status == SubmissionStatus.PENDING.key)
        }
    }
    val completionRatio by animateFloatAsState(
        targetValue = if (activeTasks.isEmpty()) 0f else completedNowCount.toFloat() / activeTasks.size.toFloat(),
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 220f),
        label = "today_completion_ring"
    )
    val animatedApproved by animateIntAsState(
        targetValue = rankEntry?.totalApprovedPoints ?: 0,
        animationSpec = tween(500),
        label = "approved_pts"
    )
    val animatedPending by animateIntAsState(
        targetValue = rankEntry?.pendingPoints ?: 0,
        animationSpec = tween(500),
        label = "pending_pts"
    )

    // Strictly ordered sections: Daily -> Weekly -> Once
    val visibleSections = remember(activeTasks, selectedFilterIdx) {
        val allGroups = listOf(
            TaskGroupSection(
                frequency = TaskFrequency.DAILY,
                titleAr = "المهام اليومية",
                subtitleAr = "تُجدَّد كل يوم",
                tasks = activeTasks.filter { TaskFrequency.fromKey(it.frequency) == TaskFrequency.DAILY }
            ),
            TaskGroupSection(
                frequency = TaskFrequency.WEEKLY,
                titleAr = "المهام الأسبوعية",
                subtitleAr = "حسب منهج كل أسبوع",
                tasks = activeTasks.filter { TaskFrequency.fromKey(it.frequency) == TaskFrequency.WEEKLY }
            ),
            TaskGroupSection(
                frequency = TaskFrequency.ONCE,
                titleAr = "مهام مرة في التحدي",
                subtitleAr = "مرة واحدة خلال الصوم",
                tasks = activeTasks.filter { TaskFrequency.fromKey(it.frequency) == TaskFrequency.ONCE }
            )
        )
        val filtered = when (TaskFilter.entries[selectedFilterIdx]) {
            TaskFilter.ALL -> allGroups
            TaskFilter.DAILY -> allGroups.filter { it.frequency == TaskFrequency.DAILY }
            TaskFilter.WEEKLY -> allGroups.filter { it.frequency == TaskFrequency.WEEKLY }
            TaskFilter.ONCE -> allGroups.filter { it.frequency == TaskFrequency.ONCE }
        }
        filtered.filter { it.tasks.isNotEmpty() }
    }
    val visibleTaskCount = visibleSections.sumOf { it.tasks.size }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Interactive Nativity Hero Card
            item {
                Card(
                    shape = RoundedCornerShape(26.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(228.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_nativity_hero),
                            contentDescription = "نجمة بيت لحم لصوم الميلاد المجيد",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            MidnightTealDark.copy(alpha = 0.58f),
                                            MidnightTealDark.copy(alpha = 0.94f)
                                        )
                                    )
                                )
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "أهلاً يا ${user.fullName} ✨",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                    val daySubtitle = when {
                                        isBeforeStart -> "وضع تجريبي 🧪 • التحدي الرسمي يبدأ خلال $daysToStart يوم (30/11)"
                                        isAfterEnd -> "انتهى التحدي يوم 5 يناير 2027 🎉"
                                        else -> "اليوم $currentDay من ${FastCalendar.FAST_DAYS_COUNT} • باقي $daysToEnd يوم على النهاية"
                                    }
                                    Text(
                                        text = daySubtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = BrightGold
                                    )
                                    if (rankEntry != null) {
                                        Surface(
                                            color = NativityBurgundy,
                                            shape = RoundedCornerShape(50),
                                            border = BorderStroke(1.dp, BrightGold.copy(alpha = 0.45f)),
                                            modifier = Modifier
                                                .padding(top = 4.dp)
                                                .clip(RoundedCornerShape(50))
                                                .clickable { onOpenMyRank() }
                                                .testTag("hero_my_rank_pill")
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.EmojiEvents,
                                                    contentDescription = "مركزك الحالي",
                                                    tint = BrightGold,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                                Text(
                                                    text = "مركزك الآن: #${rankEntry.rank}",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = Color.White
                                                )
                                            }
                                        }
                                    }
                                }

                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.size(76.dp)
                                ) {
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        val stroke = 7.dp.toPx()
                                        val inset = stroke / 2f
                                        val arcSize = Size(size.width - stroke, size.height - stroke)
                                        drawArc(
                                            color = Color.White.copy(alpha = 0.18f),
                                            startAngle = -90f,
                                            sweepAngle = 360f,
                                            useCenter = false,
                                            topLeft = Offset(inset, inset),
                                            size = arcSize,
                                            style = Stroke(width = stroke, cap = StrokeCap.Round)
                                        )
                                        drawArc(
                                            color = BrightGold,
                                            startAngle = -90f,
                                            sweepAngle = 360f * completionRatio,
                                            useCenter = false,
                                            topLeft = Offset(inset, inset),
                                            size = arcSize,
                                            style = Stroke(width = stroke, cap = StrokeCap.Round)
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "$completedNowCount/${activeTasks.size}",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "مُنجَز",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = BrightGold
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    color = WarmGold,
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "$animatedApproved",
                                            style = MaterialTheme.typography.headlineSmall,
                                            color = MidnightTealDark,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = if (isBeforeStart) "نقاط تجريبية 🧪" else "نقاطك المعتمدة ✅",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MidnightTealDark
                                        )
                                    }
                                }

                                Surface(
                                    color = Color.White.copy(alpha = 0.14f),
                                    border = BorderStroke(1.dp, BrightGold.copy(alpha = 0.45f)),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "$animatedPending",
                                            style = MaterialTheme.typography.headlineSmall,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "قيد المراجعة ⏳",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = BrightGold
                                        )
                                    }
                                }
                            }

                            LinearProgressIndicator(
                                progress = { fastProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(50)),
                                color = BrightGold,
                                trackColor = Color.White.copy(alpha = 0.24f),
                                strokeCap = StrokeCap.Round
                            )
                        }
                    }
                }
            }

            // Trial Mode Banner — automatically hidden starting Nov 30, 2026
            if (!isWithinWindow) {
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isBeforeStart) PendingAmberBg else NativityBurgundy.copy(alpha = 0.1f)
                        ),
                        border = BorderStroke(
                            1.2.dp,
                            if (isBeforeStart) WarmGold else NativityBurgundy.copy(alpha = 0.45f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("challenge_window_banner")
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = if (isBeforeStart) {
                                    "الوضع التجريبي مفعل حتى 30 نوفمبر 🧪 (متبقي $daysToStart يوم)"
                                } else {
                                    "انتهت فترة تحدي طريق المزود 🎉"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                color = if (isBeforeStart) MidnightTealDark else NativityBurgundy,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isBeforeStart) {
                                    "جرّب تسجيل المهام الآن بحرية لتتعود على التطبيق! هذه النقاط تجريبية فقط، ومن يوم 30 نوفمبر 2026 يختفي هذا التنويه وتبدأ النتائج الرسمية من الصفر حتى 5 يناير 2027."
                                } else {
                                    "انتهت فترة تسجيل المهام يوم 5 يناير 2027. يمكنك متابعة مركزك وسجلك بانتظار إعلان الفائزين الـ 10!"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Automatic Daily Verse + Coptic Orthodox Church Fathers Quote Banner
            item {
                DailyVerseBanner(
                    verseText = todayDevotion.verseText,
                    reference = todayDevotion.verseRef,
                    encouragement = "«${todayDevotion.fatherQuote}» — ${todayDevotion.fatherName}",
                    onShare = { text -> sharePlainText(context, text, "مشاركة آية اليوم") }
                )
            }

            // Interactive Category Filter Chips
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "مهام طريق المزود ($visibleTaskCount)",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(TaskFilter.entries.indices.toList()) { idx ->
                            val filter = TaskFilter.entries[idx]
                            FilterChip(
                                selected = selectedFilterIdx == idx,
                                onClick = { selectedFilterIdx = idx },
                                label = { Text(filter.labelAr) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MidnightTeal,
                                    selectedLabelColor = Color.White
                                ),
                                modifier = Modifier.testTag("task_filter_${filter.name.lowercase()}")
                            )
                        }
                    }
                }
            }

            // Strictly Ordered Sections: Daily -> Weekly -> Once
            visibleSections.forEach { section ->
                if (TaskFilter.entries[selectedFilterIdx] == TaskFilter.ALL) {
                    item(key = "header_${section.frequency.key}") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp, start = 4.dp, end = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(WarmGold)
                                )
                                Text(
                                    text = section.titleAr,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Surface(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                                    shape = RoundedCornerShape(50)
                                ) {
                                    Text(
                                        text = "${section.tasks.size}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = section.subtitleAr,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                items(section.tasks, key = { it.id }) { task ->
                    val freq = TaskFrequency.fromKey(task.frequency)
                    val periodKey = FastCalendar.periodKeyForTask(freq, today)
                    val existingSub = uiState.userSubmissions.firstOrNull {
                        it.taskId == task.id && it.periodKey == periodKey
                    }

                    InteractiveTaskCard(
                        task = task,
                        frequency = freq,
                        existingSubmission = existingSub,
                        isBeforeStart = isBeforeStart,
                        isAfterEnd = isAfterEnd,
                        activeWeek = activeWeek,
                        todayOfficialBible = todayOfficialBible,
                        bibleReminderTimeLabel = formatTimeArabic(
                            uiState.reminderSettings.bibleReadingHour,
                            uiState.reminderSettings.bibleReadingMinute
                        ),
                        enabledQuizWeeks = enabledQuizWeeks,
                        userId = user.id,
                        userSubmissions = uiState.userSubmissions,
                        today = today,
                        onOpenBibleReader = { showBibleReader = true },
                        onOpenReminders = onOpenReminders,
                        onOpenBookReader = { showBookReader = true },
                        onOpenAutoQuiz = { weekNum, completedPts ->
                            activeAutoQuiz = ActiveAutoQuizDialogState(
                                weekNumber = weekNum,
                                completedPoints = completedPts,
                                taskId = task.id
                            )
                        },
                        onSubmitClick = { openTaskSheet(task) }
                    )
                }
            }

            // Clean Logout Footer
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    TextButton(
                        onClick = onLogout,
                        modifier = Modifier.testTag("logout_youth_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "تسجيل الخروج",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "تسجيل الخروج / التبديل لحساب آخر",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InteractiveTaskCard(
    task: ChallengeTask,
    frequency: TaskFrequency,
    existingSubmission: TaskSubmission?,
    isBeforeStart: Boolean,
    isAfterEnd: Boolean,
    activeWeek: WeeklySyllabusItem,
    todayOfficialBible: OfficialBibleDayPlan,
    bibleReminderTimeLabel: String,
    enabledQuizWeeks: List<Int>,
    userId: String,
    userSubmissions: List<TaskSubmission>,
    today: FastDate,
    onOpenBibleReader: () -> Unit,
    onOpenReminders: () -> Unit,
    onOpenBookReader: () -> Unit,
    onOpenAutoQuiz: (weekNumber: Int, completedPoints: Int?) -> Unit,
    onSubmitClick: () -> Unit
) {
    val context = LocalContext.current
    val status = existingSubmission?.let { SubmissionStatus.fromKey(it.status) }
    val isLocked = isAfterEnd || status == SubmissionStatus.PENDING || status == SubmissionStatus.APPROVED

    val borderColor by animateColorAsState(
        targetValue = when (status) {
            SubmissionStatus.APPROVED -> ApprovedEmerald.copy(alpha = 0.7f)
            SubmissionStatus.PENDING -> WarmGold.copy(alpha = 0.75f)
            SubmissionStatus.REJECTED -> NativityBurgundy.copy(alpha = 0.65f)
            null -> MaterialTheme.colorScheme.outlineVariant
        },
        label = "task_card_border"
    )

    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val accentTeal = if (isDark) BrightGold else MidnightTeal
    val accentBurgundy = if (isDark) Color(0xFFF3A7B4) else NativityBurgundy

    val iconBg by animateColorAsState(
        targetValue = when (status) {
            SubmissionStatus.APPROVED -> ApprovedEmerald.copy(alpha = 0.14f)
            SubmissionStatus.PENDING -> WarmGold.copy(alpha = 0.16f)
            else -> accentTeal.copy(alpha = 0.14f)
        },
        label = "task_icon_bg"
    )

    val iconTint by animateColorAsState(
        targetValue = when (status) {
            SubmissionStatus.APPROVED -> ApprovedEmerald
            SubmissionStatus.PENDING -> WarmGold
            else -> accentTeal
        },
        label = "task_icon_tint"
    )

    val isBibleTask = task.id == "task_bible_reading"
    val isIncarnationTask = task.id == "task_incarnation_book"
    val isWeeklyExamTask =
        task.id == "task_weekly_exam_incarnation" || task.id == "task_weekly_quiz"

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(width = if (status != null) 1.4.dp else 1.dp, color = borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isLocked) 1.dp else 3.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable(enabled = !isLocked && !isWeeklyExamTask) { onSubmitClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (status) {
                            SubmissionStatus.APPROVED -> Icons.Filled.CheckCircle
                            SubmissionStatus.PENDING -> Icons.Filled.HourglassTop
                            else -> taskIconForKey(task.iconKey)
                        },
                        contentDescription = task.title,
                        tint = iconTint,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = frequency.labelAr,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                        Surface(
                            color = PendingAmberBg,
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = "+${task.points} نقطة",
                                style = MaterialTheme.typography.labelMedium,
                                color = WarmGold,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            if (task.description.isNotBlank()) {
                Text(
                    text = task.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // INLINE: Daily Bible Plan + In-App Chapter Reader
            if (isBibleTask) {
                Surface(
                    color = MaterialTheme.colorScheme.background,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, accentTeal.copy(alpha = 0.28f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "📖 ورد اليوم (${activeWeek.titleAr} • ${todayOfficialBible.dateLabelAr})",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = accentTeal
                            )
                            Surface(
                                color = accentTeal.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = todayOfficialBible.shortLabelAr,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = accentTeal,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = todayOfficialBible.fullLabelAr,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "منهج الأسبوع: ${activeWeek.bibleRangeAr}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = onOpenBibleReader,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MidnightTeal),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("open_inapp_bible_reader_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.MenuBook,
                                    contentDescription = null,
                                    tint = BrightGold,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "اقرأ إصحاح اليوم",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White
                                )
                            }
                            OutlinedButton(
                                onClick = onOpenReminders,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, accentTeal.copy(alpha = 0.4f)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                modifier = Modifier.testTag("inline_bible_reminder_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AccessTime,
                                    contentDescription = "وقت التذكير",
                                    tint = accentTeal,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = bibleReminderTimeLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = accentTeal
                                )
                            }
                        }
                    }
                }
            }

            // INLINE: Weekly "On the Incarnation" Reading + In-App PDF Reader
            if (isIncarnationTask) {
                Surface(
                    color = if (isDark) NativityBurgundy.copy(alpha = 0.22f) else PendingAmberBg.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, accentBurgundy.copy(alpha = 0.32f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "📚 ورد ${activeWeek.titleAr} (${activeWeek.dateRangeAr})",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = accentBurgundy
                            )
                            Surface(
                                color = accentBurgundy.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = "ص ${activeWeek.pdfStartPage} – ${activeWeek.pdfEndPage}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = accentBurgundy,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = activeWeek.incarnationBookAr,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Button(
                            onClick = onOpenBookReader,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NativityBurgundy),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("open_inapp_book_reader_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Book,
                                contentDescription = null,
                                tint = BrightGold,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "اقرأ ورد هذا الأسبوع من الكتاب داخل التطبيق",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // INLINE: Automatic 100-Question Weekly Quizzes (Direct Auto-Grading & Credit)
            if (isWeeklyExamTask) {
                Surface(
                    color = if (isDark) NativityBurgundy.copy(alpha = 0.2f) else NativityBurgundy.copy(alpha = 0.06f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, accentBurgundy.copy(alpha = 0.32f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${activeWeek.titleAr} (${activeWeek.dateRangeAr})",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = accentTeal
                            )
                            Surface(
                                color = accentBurgundy.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(50)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.CalendarToday,
                                        contentDescription = null,
                                        tint = accentBurgundy,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Text(
                                        text = "${activeWeek.examLabelAr}: ${activeWeek.examDateAr}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = accentBurgundy
                                    )
                                }
                            }
                        }
                        Text(
                            text = "الإنجيل: ${activeWeek.bibleRangeAr}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "كتاب تجسد الكلمة: ${activeWeek.incarnationBookAr}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        if (enabledQuizWeeks.isEmpty()) {
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Lock,
                                        contentDescription = null,
                                        tint = WarmGold,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "لا توجد مسابقة مفعّلة حالياً — ستظهر هنا تلقائياً فور تفعيلها من خدام التحدي",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            enabledQuizWeeks.forEach { weekNum ->
                                val quiz = remember(weekNum) {
                                    AutoContentBank.getAutoWeeklyQuiz(context, weekNum)
                                }
                                val solvedSub = AutoContentBank.findUserAutoQuizSubmission(
                                    submissions = userSubmissions,
                                    userId = userId,
                                    weekNumber = weekNum,
                                    today = today
                                )
                                Surface(
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, accentBurgundy.copy(alpha = 0.25f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = quiz.titleAr,
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = accentTeal,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Surface(
                                                color = WarmGold.copy(alpha = 0.2f),
                                                shape = RoundedCornerShape(50)
                                            ) {
                                                Text(
                                                    text = "${quiz.totalItems} سؤال • ${quiz.maxPoints} نقطة",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = accentTeal,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = quiz.subtitleAr,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (solvedSub != null) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Surface(
                                                    color = if (isDark) ApprovedEmerald.copy(alpha = 0.2f) else ApprovedEmeraldBg,
                                                    shape = RoundedCornerShape(50)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Filled.CheckCircle,
                                                            contentDescription = null,
                                                            tint = ApprovedEmerald,
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                        Text(
                                                            text = "تم الحل: ${solvedSub.points} / ${quiz.maxPoints} نقطة",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = ApprovedEmerald,
                                                            fontWeight = FontWeight.ExtraBold
                                                        )
                                                    }
                                                }
                                                OutlinedButton(
                                                    onClick = { onOpenAutoQuiz(weekNum, solvedSub.points) },
                                                    shape = RoundedCornerShape(10.dp),
                                                    border = BorderStroke(1.dp, accentTeal.copy(alpha = 0.45f)),
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                                    modifier = Modifier.testTag("review_auto_quiz_w$weekNum")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Visibility,
                                                        contentDescription = null,
                                                        tint = accentTeal,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = "مراجعة الإجابات",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = accentTeal,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        } else {
                                            Button(
                                                onClick = { onOpenAutoQuiz(weekNum, null) },
                                                enabled = !isAfterEnd,
                                                shape = RoundedCornerShape(12.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = NativityBurgundy),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .testTag("start_auto_quiz_w$weekNum")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Quiz,
                                                    contentDescription = null,
                                                    tint = BrightGold,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = if (isAfterEnd) {
                                                        "انتهت فترة التحدي"
                                                    } else {
                                                        "ابدأ ${quiz.titleAr} (${quiz.totalItems} سؤال • تصحيح فوري)"
                                                    },
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = Color.White
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (status == SubmissionStatus.REJECTED && !existingSubmission?.adminNote.isNullOrBlank()) {
                Text(
                    text = "ملاحظة الخادم: ${existingSubmission?.adminNote}",
                    style = MaterialTheme.typography.bodySmall,
                    color = accentBurgundy
                )
            }

            if (isWeeklyExamTask) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = WarmGold,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "تصحيح وإضافة النقاط تلقائياً لرصيدك فور إنهاء المسابقة",
                            style = MaterialTheme.typography.labelSmall,
                            color = accentTeal,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (status != null) {
                        StatusBadge(status = status)
                    } else {
                        Text(
                            text = when {
                                isAfterEnd -> "انتهت فترة التحدي (5/1/2027)"
                                isBeforeStart -> "اضغط للتجربة في الوضع التجريبي 🧪"
                                else -> "اضغط لتسجيل إنجازك الآن"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = onSubmitClick,
                        enabled = !isLocked,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MidnightTeal,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier
                            .height(44.dp)
                            .testTag("submit_task_${task.id}")
                    ) {
                        Text(
                            text = when {
                                status == SubmissionStatus.APPROVED -> "تم الاعتماد"
                                status == SubmissionStatus.PENDING -> "تم الإرسال"
                                isAfterEnd -> "انتهى التحدي"
                                status == SubmissionStatus.REJECTED -> "إعادة التسجيل"
                                isBeforeStart -> "تجربة التسجيل 🧪"
                                else -> "تسجيل الإنجاز"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isLocked) MaterialTheme.colorScheme.onSurfaceVariant else Color.White
                        )
                    }
                }
            }
        }
    }
}
