package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.AutoContentBank
import com.example.data.ChallengeTask
import com.example.data.FastCalendar
import com.example.data.FastDate
import com.example.data.OfficialReadingPlan
import com.example.data.SubmissionStatus
import com.example.data.TaskFrequency
import com.example.ui.components.AutoWeeklyQuizDialog
import com.example.ui.components.StatusBadge
import com.example.ui.components.TaskSubmissionBottomSheet
import com.example.ui.state.AppUiState
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.ApprovedEmerald
import com.example.ui.theme.ApprovedEmeraldBg
import com.example.ui.theme.BrightGold
import com.example.ui.theme.MidnightTeal
import com.example.ui.theme.MidnightTealDark
import com.example.ui.theme.NativityBurgundy
import com.example.ui.theme.PendingAmberBg
import com.example.ui.theme.WarmGold

private data class PlanAutoQuizState(
    val weekNumber: Int,
    val completedPoints: Int?
)

@Composable
fun ReadingPlanScreen(
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
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val userId = uiState.activeUser?.id.orEmpty()
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val accentTeal = if (isDark) BrightGold else MidnightTeal
    val accentBurgundy = if (isDark) Color(0xFFF3A7B4) else NativityBurgundy

    val today = remember { FastDate.today() }
    val isAfterEnd = remember(today) { FastCalendar.isAfterEnd(today) }
    val isQuizToday = remember(today) { OfficialReadingPlan.isQuizHighlightDay(today) }
    val officialChapter = remember(today) { OfficialReadingPlan.getTodayChapter(today) }
    val activeWeekNum = uiState.appConfig.activeWeek.coerceIn(1, 5)
    val activeSyllabus = remember(activeWeekNum) { OfficialReadingPlan.getSyllabusForWeek(activeWeekNum) }
    val enabledQuizWeeks = remember(uiState.appConfig.weeklyQuizQuestion) {
        OfficialReadingPlan.getEnabledQuizWeeks(uiState.appConfig)
    }
    val activeAutoQuizData = remember(activeWeekNum) {
        AutoContentBank.getAutoWeeklyQuiz(context, activeWeekNum)
    }
    val isActiveWeekQuizEnabled = enabledQuizWeeks.contains(activeWeekNum)
    val activeWeekQuizSub = remember(uiState.userSubmissions, userId, activeWeekNum, today) {
        AutoContentBank.findUserAutoQuizSubmission(uiState.userSubmissions, userId, activeWeekNum, today)
    }

    var expandedWeek by remember(activeWeekNum) { mutableIntStateOf(activeWeekNum) }
    var selectedTaskForSheet by remember { mutableStateOf<ChallengeTask?>(null) }
    var sheetInitialNote by remember { mutableStateOf("") }
    var activeAutoQuiz by remember { mutableStateOf<PlanAutoQuizState?>(null) }

    val incarnationTask = remember(uiState.allTasks) {
        uiState.allTasks.firstOrNull { it.id == "task_incarnation_book" && it.isActive }
    }

    val weeklyPeriodKey = remember(today) { FastCalendar.periodKeyForTask(TaskFrequency.WEEKLY, today) }
    val incarnationSub = remember(incarnationTask, uiState.userSubmissions, weeklyPeriodKey) {
        incarnationTask?.let { t ->
            uiState.userSubmissions.firstOrNull { it.taskId == t.id && it.periodKey == weeklyPeriodKey }
        }
    }

    val incarnationStatus = incarnationSub?.let { SubmissionStatus.fromKey(it.status) }
    val isIncarnationLocked =
        isAfterEnd || incarnationStatus == SubmissionStatus.PENDING || incarnationStatus == SubmissionStatus.APPROVED

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
            userId = userId,
            completedPoints = quizState.completedPoints,
            onSubmitResult = { weekNum, correct, total, points, onDone ->
                onSubmitAutoQuiz(weekNum, correct, total, points, "task_weekly_exam_incarnation", onDone)
            },
            onDismiss = { activeAutoQuiz = null }
        )
    }

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
            // Header Banner
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 5.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    listOf(MidnightTealDark, MidnightTeal, MidnightTealDark)
                                )
                            )
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Book,
                                contentDescription = "خطة القراءة",
                                tint = BrightGold,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "خطة القراءة والمسابقة الأسبوعية",
                                style = MaterialTheme.typography.labelLarge,
                                color = BrightGold,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "الكتاب المقدس وكتاب «في تجسد الكلمة»",
                            fontFamily = AmiriFontFamily,
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "للقديس أثناسيوس الرسولي • الإثنين 30 نوفمبر 2026 حتى الثلاثاء 5 يناير 2027 (37 يوماً / 5 أسابيع)",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.88f)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                color = Color.White.copy(alpha = 0.14f),
                                border = BorderStroke(1.dp, BrightGold.copy(alpha = 0.45f)),
                                shape = RoundedCornerShape(50),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = if (officialChapter.isBeforeChallenge) {
                                        "بداية الخطة: ${officialChapter.shortLabelAr}"
                                    } else {
                                        "ورد اليوم: ${officialChapter.shortLabelAr}"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = BrightGold,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                            Surface(
                                color = WarmGold,
                                shape = RoundedCornerShape(50)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.AutoAwesome,
                                        contentDescription = null,
                                        tint = MidnightTealDark,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = activeSyllabus.shortLabelAr,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MidnightTealDark,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Active Week Featured Card
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.8.dp, WarmGold),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Surface(
                                    color = WarmGold.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(50)
                                ) {
                                    Text(
                                        text = "المقرر الحالي النشط ✨",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MidnightTeal,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = activeSyllabus.titleAr,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MidnightTeal,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.CalendarMonth,
                                        contentDescription = null,
                                        tint = WarmGold,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = activeSyllabus.dateRangeAr,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            if (isQuizToday) {
                                Surface(
                                    color = NativityBurgundy,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = "موعد المسابقة اليوم! 🎯",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }

                        // Bible Box
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = "📖 الكتاب المقدس",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MidnightTeal,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = activeSyllabus.bibleTitleAr,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = activeSyllabus.bibleDetailAr,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Patristic Book Box + Quick Log Button
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = PendingAmberBg.copy(alpha = 0.7f),
                            border = BorderStroke(1.dp, WarmGold.copy(alpha = 0.45f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "📚 كتاب «في تجسد الكلمة» — القديس أثناسيوس الرسولي",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = WarmGold,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = activeSyllabus.patristicTitleAr,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = activeSyllabus.patristicDetailAr,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                if (incarnationTask != null) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 6.dp),
                                        color = WarmGold.copy(alpha = 0.25f)
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (incarnationStatus != null) {
                                            StatusBadge(status = incarnationStatus)
                                        } else {
                                            Text(
                                                text = "+${incarnationTask.points} نقطة أسبوعياً",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = WarmGold,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Button(
                                            onClick = {
                                                sheetInitialNote =
                                                    "أتممت قراءة ورد ${activeSyllabus.titleAr} من كتاب في تجسد الكلمة (${activeSyllabus.patristicTitleAr} • ${activeSyllabus.patristicDetailAr})"
                                                selectedTaskForSheet = incarnationTask
                                            },
                                            enabled = !isIncarnationLocked,
                                            colors = ButtonDefaults.buttonColors(containerColor = MidnightTeal),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.testTag("plan_log_incarnation_button")
                                        ) {
                                            Text(
                                                text = when (incarnationStatus) {
                                                    SubmissionStatus.APPROVED -> "تم الاعتماد ✓"
                                                    SubmissionStatus.PENDING -> "قيد المراجعة ⏳"
                                                    else -> "تسجيل قراءة الكتاب"
                                                },
                                                style = MaterialTheme.typography.labelMedium,
                                                color = if (isIncarnationLocked) MaterialTheme.colorScheme.onSurfaceVariant else Color.White
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Automatic Weekly Quiz Box
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = NativityBurgundy.copy(alpha = if (isDark) 0.2f else 0.08f),
                            border = BorderStroke(1.dp, accentBurgundy.copy(alpha = 0.35f)),
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
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.EmojiEvents,
                                            contentDescription = null,
                                            tint = accentBurgundy,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "${activeAutoQuizData.titleAr} • ${activeSyllabus.quizDateAr}",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = accentBurgundy,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Surface(
                                        color = NativityBurgundy,
                                        shape = RoundedCornerShape(50)
                                    ) {
                                        Text(
                                            text = "${activeAutoQuizData.totalItems} سؤال • ${activeAutoQuizData.maxPoints} نقطة",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = "${activeAutoQuizData.subtitleAr} — تصحيح تلقائي فوري وإضافة النقاط مباشرة لرصيدك (2 نقطة لكل سؤال).",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                when {
                                    activeWeekQuizSub != null -> {
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
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.CheckCircle,
                                                        contentDescription = null,
                                                        tint = ApprovedEmerald,
                                                        modifier = Modifier.size(15.dp)
                                                    )
                                                    Text(
                                                        text = "تم الحل: ${activeWeekQuizSub.points} / ${activeAutoQuizData.maxPoints} نقطة",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = ApprovedEmerald,
                                                        fontWeight = FontWeight.ExtraBold
                                                    )
                                                }
                                            }
                                            OutlinedButton(
                                                onClick = {
                                                    activeAutoQuiz = PlanAutoQuizState(
                                                        weekNumber = activeWeekNum,
                                                        completedPoints = activeWeekQuizSub.points
                                                    )
                                                },
                                                shape = RoundedCornerShape(10.dp),
                                                border = BorderStroke(1.dp, accentTeal.copy(alpha = 0.45f)),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Visibility,
                                                    contentDescription = null,
                                                    tint = accentTeal,
                                                    modifier = Modifier.size(15.dp)
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
                                    }
                                    isActiveWeekQuizEnabled -> {
                                        Button(
                                            onClick = {
                                                activeAutoQuiz = PlanAutoQuizState(
                                                    weekNumber = activeWeekNum,
                                                    completedPoints = null
                                                )
                                            },
                                            enabled = !isAfterEnd,
                                            colors = ButtonDefaults.buttonColors(containerColor = NativityBurgundy),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("plan_start_auto_quiz_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.PlayCircle,
                                                contentDescription = null,
                                                tint = BrightGold,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (isAfterEnd) {
                                                    "انتهت فترة التحدي"
                                                } else {
                                                    "ابدأ ${activeAutoQuizData.titleAr} (${activeAutoQuizData.totalItems} سؤال • تصحيح فوري)"
                                                },
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    else -> {
                                        Surface(
                                            color = MaterialTheme.colorScheme.surface,
                                            shape = RoundedCornerShape(12.dp),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Lock,
                                                    contentDescription = null,
                                                    tint = WarmGold,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Text(
                                                    text = "بانتظار تفعيل هذه المسابقة من خدام التحدي",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
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

            // Section Title for All 5 Weeks
            item {
                Text(
                    text = "جدول الأسابيع الخمسة بالكامل (30 نوفمبر – 5 يناير)",
                    style = MaterialTheme.typography.titleMedium,
                    color = MidnightTeal,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            items(OfficialReadingPlan.weeks, key = { it.weekNumber }) { week ->
                val isActive = week.weekNumber == activeWeekNum
                val isOpen = expandedWeek == week.weekNumber
                val weekQuiz = remember(week.weekNumber) {
                    AutoContentBank.getAutoWeeklyQuiz(context, week.weekNumber)
                }
                val isWeekQuizEnabled = enabledQuizWeeks.contains(week.weekNumber)
                val weekQuizSub = AutoContentBank.findUserAutoQuizSubmission(
                    submissions = uiState.userSubmissions,
                    userId = userId,
                    weekNumber = week.weekNumber,
                    today = today
                )

                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(
                        if (isActive) 1.5.dp else 1.dp,
                        if (isActive) WarmGold else MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expandedWeek = if (isOpen) 0 else week.weekNumber }
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (isActive) WarmGold else accentTeal.copy(alpha = 0.14f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${week.weekNumber}",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (isActive) MidnightTealDark else accentTeal,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = week.titleAr,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (isActive) {
                                        Surface(
                                            color = MidnightTeal,
                                            shape = RoundedCornerShape(50)
                                        ) {
                                            Text(
                                                text = "الحالي",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = BrightGold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    if (weekQuizSub != null) {
                                        Surface(
                                            color = if (isDark) ApprovedEmerald.copy(alpha = 0.2f) else ApprovedEmeraldBg,
                                            shape = RoundedCornerShape(50)
                                        ) {
                                            Text(
                                                text = "${weekQuizSub.points}/200 ✓",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = ApprovedEmerald,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                            )
                                        }
                                    } else if (isWeekQuizEnabled) {
                                        Surface(
                                            color = accentBurgundy.copy(alpha = 0.16f),
                                            shape = RoundedCornerShape(50)
                                        ) {
                                            Text(
                                                text = "مفعّلة ✨",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = accentBurgundy,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = week.dateRangeAr,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = if (isOpen) "إخفاء ▲" else "التفاصيل ▼",
                                style = MaterialTheme.typography.labelMedium,
                                color = accentTeal,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (isOpen) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.MenuBook,
                                        contentDescription = null,
                                        tint = accentTeal,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "الكتاب المقدس: ${week.bibleTitleAr} — ${week.bibleDetailAr}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Book,
                                        contentDescription = null,
                                        tint = WarmGold,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "كتاب «في تجسد الكلمة»: ${week.patristicTitleAr} (${week.patristicDetailAr})",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Quiz,
                                        contentDescription = null,
                                        tint = accentBurgundy,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "${week.quizTitleAr}: ${week.quizDateAr} (${weekQuiz.totalItems} سؤال • ${weekQuiz.maxPoints} نقطة)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = accentBurgundy,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                when {
                                    weekQuizSub != null -> {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "نتيجتك المعتمدة: ${weekQuizSub.points} / ${weekQuiz.maxPoints} نقطة ✅",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = ApprovedEmerald,
                                                fontWeight = FontWeight.Bold
                                            )
                                            OutlinedButton(
                                                onClick = {
                                                    activeAutoQuiz = PlanAutoQuizState(
                                                        weekNumber = week.weekNumber,
                                                        completedPoints = weekQuizSub.points
                                                    )
                                                },
                                                shape = RoundedCornerShape(10.dp),
                                                border = BorderStroke(1.dp, accentTeal.copy(alpha = 0.45f)),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = "مراجعة الإجابات",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = accentTeal,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                    isWeekQuizEnabled -> {
                                        Button(
                                            onClick = {
                                                activeAutoQuiz = PlanAutoQuizState(
                                                    weekNumber = week.weekNumber,
                                                    completedPoints = null
                                                )
                                            },
                                            enabled = !isAfterEnd,
                                            colors = ButtonDefaults.buttonColors(containerColor = NativityBurgundy),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.PlayCircle,
                                                contentDescription = null,
                                                tint = BrightGold,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "ابدأ ${weekQuiz.titleAr} الآن (${weekQuiz.totalItems} سؤال)",
                                                color = Color.White,
                                                style = MaterialTheme.typography.labelMedium
                                            )
                                        }
                                    }
                                    else -> {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Lock,
                                                contentDescription = null,
                                                tint = WarmGold,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = "المسابقة مغلقة حالياً بانتظار التفعيل من خدام التحدي",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
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
    }
}
