package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.AppConfigEntity
import com.example.data.ChallengeTask
import com.example.data.FastDate
import com.example.data.SubmissionStatus
import com.example.data.SyllabusPhase
import com.example.data.TaskSubmission
import com.example.data.UrlValidator
import com.example.data.WeeklyQuizQuestion
import com.example.data.WeeklySyllabusItem
import com.example.data.WeeklySyllabusSchedule
import com.example.ui.theme.ApprovedEmerald
import com.example.ui.theme.BrightGold
import com.example.ui.theme.MidnightTeal
import com.example.ui.theme.MidnightTealDark
import com.example.ui.theme.NativityBurgundy
import com.example.ui.theme.PendingAmberBg
import com.example.ui.theme.WarmGold

@Composable
fun WeeklyExamBannerCard(
    today: FastDate,
    appConfig: AppConfigEntity,
    examTask: ChallengeTask?,
    examSubmission: TaskSubmission?,
    isAfterEnd: Boolean,
    onStartInAppQuiz: (List<WeeklyQuizQuestion>, WeeklySyllabusItem) -> Unit,
    onLogExamParticipation: (ChallengeTask, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activeStatus = remember(today) { WeeklySyllabusSchedule.getActiveWeek(today) }
    val activeWeek = activeStatus.activeWeek
    val decodedQuiz = remember(appConfig.weeklyQuizQuestion, appConfig.weeklyQuizQuestionsJson) {
        val decoded = WeeklySyllabusSchedule.decodeQuizConfig(appConfig.weeklyQuizQuestion)
        val fromJsonCol = WeeklySyllabusSchedule.parseQuestionsJson(appConfig.weeklyQuizQuestionsJson)
        val mergedQuestions = if (fromJsonCol.isNotEmpty()) fromJsonCol else decoded.questions
        decoded.copy(questions = mergedQuestions)
    }

    val hasQuestions = decodedQuiz.questions.isNotEmpty()
    val hasExternalUrl = UrlValidator.isSafeHttpUrl(appConfig.weeklyQuizUrl)
    val status = examSubmission?.let { SubmissionStatus.fromKey(it.status) }
    val isLocked = isAfterEnd || status == SubmissionStatus.PENDING || status == SubmissionStatus.APPROVED
    val rewardPoints = examTask?.points ?: 200

    val phaseLabel = when (activeStatus.phase) {
        SyllabusPhase.UPCOMING -> "القادم • يبدأ 30 نوفمبر"
        SyllabusPhase.ACTIVE -> "الأسبوع الحالي"
        SyllabusPhase.COMPLETED -> "الأسبوع الختامي"
    }
    val defaultExamNote = "${activeWeek.titleAr} (${activeWeek.examDateAr}) — إنجيل + كتاب في تجسد الكلمة"

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.2.dp, NativityBurgundy.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("weekly_exam_banner_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            NativityBurgundy.copy(alpha = 0.08f),
                            PendingAmberBg.copy(alpha = 0.35f)
                        )
                    )
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(NativityBurgundy),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Quiz,
                            contentDescription = "المسابقة الأسبوعية",
                            tint = BrightGold,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = NativityBurgundy,
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = phaseLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                            Surface(
                                color = PendingAmberBg,
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = "+$rewardPoints نقطة أسبوعياً",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = WarmGold,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = appConfig.weeklyQuizTitle.ifBlank { "المسابقة الأسبوعية (إنجيل + كتاب تجسد الكلمة)" },
                            style = MaterialTheme.typography.titleMedium,
                            color = NativityBurgundy,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                if (status != null) {
                    StatusBadge(status = status)
                }
            }

            // Current Week Exam Topic
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, NativityBurgundy.copy(alpha = 0.2f)),
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
                            text = "${activeWeek.titleAr} (${activeWeek.dateRangeAr})",
                            style = MaterialTheme.typography.labelLarge,
                            color = MidnightTeal,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            color = NativityBurgundy.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = "${activeWeek.examLabelAr}: ${activeWeek.examDateAr}",
                                style = MaterialTheme.typography.labelSmall,
                                color = NativityBurgundy,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = "الكتاب المقدس: ${activeWeek.bibleRangeAr}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "كتاب «في تجسد الكلمة»: ${activeWeek.incarnationBookAr}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            if (decodedQuiz.introText.isNotBlank()) {
                Text(
                    text = decodedQuiz.introText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Action Buttons
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (hasQuestions) {
                    Button(
                        onClick = { onStartInAppQuiz(decodedQuiz.questions, activeWeek) },
                        colors = ButtonDefaults.buttonColors(containerColor = NativityBurgundy),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("start_in_app_weekly_quiz_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Quiz,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isLocked) {
                                "مراجعة أسئلة المسابقة (${decodedQuiz.questions.size} أسئلة)"
                            } else {
                                "ابدأ المسابقة داخل التطبيق (${decodedQuiz.questions.size} أسئلة)"
                            },
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (hasExternalUrl) {
                        OutlinedButton(
                            onClick = { openSafeExternalUrl(context, appConfig.weeklyQuizUrl) },
                            border = BorderStroke(1.dp, NativityBurgundy),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("open_weekly_quiz_link_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.OpenInNew,
                                contentDescription = null,
                                tint = NativityBurgundy,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "رابط المسابقة",
                                style = MaterialTheme.typography.labelMedium,
                                color = NativityBurgundy
                            )
                        }
                    }

                    if (examTask != null && !isLocked) {
                        Button(
                            onClick = { onLogExamParticipation(examTask, defaultExamNote) },
                            colors = ButtonDefaults.buttonColors(containerColor = MidnightTeal),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("log_weekly_exam_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.EmojiEvents,
                                contentDescription = null,
                                tint = BrightGold,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "تسجيل المسابقة (+${examTask.points})",
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

@Composable
fun WeeklySyllabusCard(
    today: FastDate,
    incarnationTask: ChallengeTask?,
    incarnationSubmission: TaskSubmission?,
    isAfterEnd: Boolean,
    onLogIncarnationReading: (ChallengeTask, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    val activeStatus = remember(today) { WeeklySyllabusSchedule.getActiveWeek(today) }
    val activeWeek = activeStatus.activeWeek
    val status = incarnationSubmission?.let { SubmissionStatus.fromKey(it.status) }
    val isLocked = isAfterEnd || status == SubmissionStatus.PENDING || status == SubmissionStatus.APPROVED

    val activeBadgeText = when (activeStatus.phase) {
        SyllabusPhase.UPCOMING -> "يبدأ 30 نوفمبر"
        SyllabusPhase.ACTIVE -> "الأسبوع النشط الآن"
        SyllabusPhase.COMPLETED -> "الأسبوع الختامي"
    }

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.2.dp, WarmGold.copy(alpha = 0.55f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("weekly_syllabus_card")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Collapsible Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .background(MidnightTeal.copy(alpha = 0.05f))
                    .padding(16.dp)
                    .testTag("toggle_weekly_syllabus_button"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MidnightTeal),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Book,
                            contentDescription = "منهج القراءة والمسابقة",
                            tint = BrightGold,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "منهج القراءة والمسابقة",
                                style = MaterialTheme.typography.titleMedium,
                                color = MidnightTeal,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                color = WarmGold.copy(alpha = 0.22f),
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = "5 أسابيع",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MidnightTealDark,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "${activeWeek.titleAr} (${activeWeek.dateRangeAr}) • 30 نوفمبر – 5 يناير",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (isExpanded) "إخفاء المنهج" else "عرض المنهج",
                    tint = MidnightTeal
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))

            // Compact Preview when collapsed
            if (!isExpanded) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = PendingAmberBg.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, WarmGold.copy(alpha = 0.45f)),
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
                                Surface(
                                    color = MidnightTeal,
                                    shape = RoundedCornerShape(50)
                                ) {
                                    Text(
                                        text = "$activeBadgeText • ${activeWeek.titleAr}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = BrightGold,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                                Text(
                                    text = "${activeWeek.examLabelAr}: ${activeWeek.examDateAr}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NativityBurgundy,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "الكتاب المقدس: ${activeWeek.bibleRangeAr}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "في تجسد الكلمة: ${activeWeek.incarnationBookAr}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    if (incarnationTask != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (status != null) {
                                StatusBadge(status = status)
                            } else {
                                Text(
                                    text = "كتاب «في تجسد الكلمة» (+${incarnationTask.points} نقطة)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (!isLocked) {
                                Button(
                                    onClick = {
                                        onLogIncarnationReading(
                                            incarnationTask,
                                            "${activeWeek.titleAr}: ${activeWeek.incarnationBookAr}"
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MidnightTeal),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                    modifier = Modifier
                                        .height(44.dp)
                                        .testTag("log_incarnation_book_button")
                                ) {
                                    Text(
                                        text = "تسجيل القراءة (+${incarnationTask.points})",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Full 5-Week Expanded View
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    WeeklySyllabusSchedule.weeks.forEach { week ->
                        val isCurrent = week.weekNumber == activeWeek.weekNumber
                        Surface(
                            color = if (isCurrent) PendingAmberBg.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(
                                width = if (isCurrent) 1.5.dp else 1.dp,
                                color = if (isCurrent) WarmGold else MaterialTheme.colorScheme.outlineVariant
                            ),
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
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            color = when {
                                                isCurrent -> MidnightTeal
                                                week.isFinalWeek -> NativityBurgundy
                                                else -> MaterialTheme.colorScheme.surfaceVariant
                                            },
                                            shape = RoundedCornerShape(50)
                                        ) {
                                            Text(
                                                text = week.titleAr,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = if (isCurrent) BrightGold else if (week.isFinalWeek) Color.White else MidnightTeal,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                            )
                                        }
                                        Text(
                                            text = "(${week.dateRangeAr})",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    if (isCurrent) {
                                        Surface(
                                            color = WarmGold,
                                            shape = RoundedCornerShape(50)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.AutoAwesome,
                                                    contentDescription = null,
                                                    tint = MidnightTealDark,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Text(
                                                    text = activeBadgeText,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MidnightTealDark,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.MenuBook,
                                        contentDescription = null,
                                        tint = MidnightTeal,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "الكتاب المقدس: ${week.bibleRangeAr}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Book,
                                        contentDescription = null,
                                        tint = NativityBurgundy,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "كتاب في تجسد الكلمة: ${week.incarnationBookAr}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.CalendarMonth,
                                        contentDescription = null,
                                        tint = WarmGold,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "${week.examLabelAr}: ${week.examDateAr}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = NativityBurgundy,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    if (incarnationTask != null && !isLocked) {
                        Button(
                            onClick = {
                                onLogIncarnationReading(
                                    incarnationTask,
                                    "${activeWeek.titleAr}: ${activeWeek.incarnationBookAr}"
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MidnightTeal),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("log_incarnation_book_expanded_button")
                        ) {
                            Text(
                                text = "تسجيل قراءة «في تجسد الكلمة» (+${incarnationTask.points} نقطة)",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InAppWeeklyQuizDialog(
    title: String,
    week: WeeklySyllabusItem,
    questions: List<WeeklyQuizQuestion>,
    alreadySubmitted: Boolean,
    onDismiss: () -> Unit,
    onSubmitQuizResult: (String) -> Unit
) {
    val selectedAnswers = remember(questions) { mutableStateMapOf<String, Int>() }
    var isGraded by remember { mutableStateOf(false) }

    val allAnswered = questions.isNotEmpty() && selectedAnswers.size == questions.size
    val correctCount = questions.count { q -> selectedAnswers[q.id] == q.correctIndex }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "${week.titleAr} • ${week.examDateAr}",
                    style = MaterialTheme.typography.labelSmall,
                    color = WarmGold,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = title.ifBlank { "المسابقة الأسبوعية (إنجيل + كتاب تجسد الكلمة)" },
                    style = MaterialTheme.typography.titleMedium,
                    color = MidnightTeal,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isGraded) {
                    Surface(
                        color = PendingAmberBg,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, WarmGold),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "نتيجتك في المسابقة الأسبوعية 🎉",
                                style = MaterialTheme.typography.labelMedium,
                                color = MidnightTeal,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "$correctCount / ${questions.size}",
                                style = MaterialTheme.typography.headlineMedium,
                                color = NativityBurgundy,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (alreadySubmitted) {
                                    "تم تسجيل مشاركتك في المسابقة لهذا الأسبوع بالفعل."
                                } else {
                                    "اضغط على إرسال النتيجة بالأسفل ليعتمد خدام التحدي +200 نقطة."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                questions.forEachIndexed { idx, q ->
                    val picked = selectedAnswers[q.id]
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(MidnightTeal),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${idx + 1}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = BrightGold,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = q.prompt,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            q.options.forEachIndexed { optIdx, opt ->
                                val isPicked = picked == optIdx
                                val isCorrectOpt = optIdx == q.correctIndex

                                val bgColor = when {
                                    isGraded && isCorrectOpt -> ApprovedEmerald.copy(alpha = 0.14f)
                                    isGraded && isPicked && !isCorrectOpt -> NativityBurgundy.copy(alpha = 0.14f)
                                    !isGraded && isPicked -> MidnightTeal
                                    else -> MaterialTheme.colorScheme.surface
                                }
                                val borderColor = when {
                                    isGraded && isCorrectOpt -> ApprovedEmerald
                                    isGraded && isPicked && !isCorrectOpt -> NativityBurgundy
                                    !isGraded && isPicked -> MidnightTeal
                                    else -> MaterialTheme.colorScheme.outlineVariant
                                }
                                val textColor = when {
                                    isGraded && isCorrectOpt -> ApprovedEmerald
                                    isGraded && isPicked && !isCorrectOpt -> NativityBurgundy
                                    !isGraded && isPicked -> Color.White
                                    else -> MaterialTheme.colorScheme.onSurface
                                }

                                Surface(
                                    color = bgColor,
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, borderColor),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable(enabled = !isGraded) {
                                            selectedAnswers[q.id] = optIdx
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = opt,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = textColor,
                                            fontWeight = if (isPicked || (isGraded && isCorrectOpt)) FontWeight.Bold else FontWeight.Normal,
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (isGraded && isCorrectOpt) {
                                            Icon(
                                                imageVector = Icons.Filled.CheckCircle,
                                                contentDescription = "إجابة صحيحة",
                                                tint = ApprovedEmerald,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        } else if (isGraded && isPicked && !isCorrectOpt) {
                                            Icon(
                                                imageVector = Icons.Filled.Cancel,
                                                contentDescription = "إجابة خاطئة",
                                                tint = NativityBurgundy,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (!isGraded) {
                Button(
                    onClick = { isGraded = true },
                    enabled = allAnswered,
                    colors = ButtonDefaults.buttonColors(containerColor = NativityBurgundy),
                    modifier = Modifier.testTag("grade_weekly_quiz_button")
                ) {
                    Text(
                        text = if (allAnswered) "تصحيح الإجابات" else "أجب (${selectedAnswers.size}/${questions.size})",
                        color = Color.White
                    )
                }
            } else if (!alreadySubmitted) {
                Button(
                    onClick = {
                        val pct = if (questions.isNotEmpty()) (correctCount * 100) / questions.size else 0
                        val summary = "تم حل ${title.ifBlank { "المسابقة الأسبوعية" }} (${week.titleAr}) داخل التطبيق — النتيجة: $correctCount من ${questions.size} ($pct%)"
                        onSubmitQuizResult(summary)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MidnightTeal),
                    modifier = Modifier.testTag("submit_weekly_quiz_result_button")
                ) {
                    Text("إرسال النتيجة (+200 نقطة)", color = Color.White)
                }
            } else {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = MidnightTeal)
                ) {
                    Text("تم", color = Color.White)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إغلاق")
            }
        }
    )
}
