package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.QuizQuestion
import com.example.data.QuizQuestionType
import com.example.data.SubmissionStatus
import com.example.ui.theme.ApprovedEmerald
import com.example.ui.theme.BrightGold
import com.example.ui.theme.MidnightTeal
import com.example.ui.theme.MidnightTealDark
import com.example.ui.theme.NativityBurgundy
import com.example.ui.theme.PendingAmberBg
import com.example.ui.theme.RejectedRed
import com.example.ui.theme.WarmGold

private data class QuizEvaluation(
    val correctCount: Int,
    val total: Int,
    val percent: Int,
    val noteSummary: String
)

@Composable
fun WeeklyQuizDialog(
    title: String,
    weekLabel: String,
    questions: List<QuizQuestion>,
    points: Int,
    alreadySubmittedStatus: SubmissionStatus?,
    onSubmitAnswers: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val selectedAnswers = remember(questions) { mutableStateMapOf<String, Int>() }
    var evaluation by remember { mutableStateOf<QuizEvaluation?>(null) }
    val answeredCount = questions.count { selectedAnswers.containsKey(it.id) }
    val allAnswered = questions.isNotEmpty() && answeredCount == questions.size
    val isLocked =
        alreadySubmittedStatus == SubmissionStatus.PENDING ||
            alreadySubmittedStatus == SubmissionStatus.APPROVED

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .heightIn(max = 660.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MidnightTeal)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            color = WarmGold.copy(alpha = 0.25f),
                            shape = RoundedCornerShape(50)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Quiz,
                                    contentDescription = null,
                                    tint = BrightGold,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "$weekLabel • +$points نقطة",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = BrightGold,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "إغلاق",
                            tint = Color.White
                        )
                    }
                }

                // Scrollable Quiz Questions
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    evaluation?.let { res ->
                        Surface(
                            color = PendingAmberBg,
                            border = BorderStroke(1.dp, WarmGold),
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.EmojiEvents,
                                    contentDescription = "النتيجة",
                                    tint = WarmGold,
                                    modifier = Modifier.size(34.dp)
                                )
                                Text(
                                    text = "نتيجتك: ${res.correctCount} من ${res.total} (${res.percent}%)",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MidnightTealDark,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = if (res.percent >= 70) {
                                        "أحسنت! نتيجة رائعة في الإنجيل وكتاب «في تجسد الكلمة» ✨"
                                    } else {
                                        "شكراً لمشاركتك! راجع الإجابات الصحيحة وأرسل نتيجتك للخادم."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    questions.forEachIndexed { qIdx, q ->
                        val picked = selectedAnswers[q.id]
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
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
                                            text = "${qIdx + 1}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = BrightGold,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = q.prompt,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = if (q.type == QuizQuestionType.TF) "صح أم خطأ" else "اختيار من متعدد",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                q.options.forEachIndexed { oIdx, opt ->
                                    val isSelected = picked == oIdx
                                    val isCorrect = oIdx == q.correctIndex
                                    val (bg, border, textColor) = when {
                                        evaluation != null && isCorrect -> Triple(
                                            ApprovedEmerald.copy(alpha = 0.12f),
                                            ApprovedEmerald,
                                            ApprovedEmerald
                                        )
                                        evaluation != null && isSelected && !isCorrect -> Triple(
                                            RejectedRed.copy(alpha = 0.12f),
                                            RejectedRed,
                                            RejectedRed
                                        )
                                        isSelected -> Triple(
                                            MidnightTeal.copy(alpha = 0.12f),
                                            MidnightTeal,
                                            MidnightTeal
                                        )
                                        else -> Triple(
                                            MaterialTheme.colorScheme.surface,
                                            MaterialTheme.colorScheme.outlineVariant,
                                            MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = bg,
                                        border = BorderStroke(1.2.dp, border),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable(enabled = evaluation == null) {
                                                selectedAnswers[q.id] = oIdx
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = opt,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = textColor,
                                                fontWeight = if (isSelected || (evaluation != null && isCorrect)) FontWeight.Bold else FontWeight.Normal,
                                                modifier = Modifier.weight(1f)
                                            )
                                            if (evaluation != null) {
                                                if (isCorrect) {
                                                    Icon(
                                                        imageVector = Icons.Filled.CheckCircle,
                                                        contentDescription = "صحيح",
                                                        tint = ApprovedEmerald,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                } else if (isSelected) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Cancel,
                                                        contentDescription = "خطأ",
                                                        tint = RejectedRed,
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
                }

                // Footer CTA
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    val res = evaluation
                    when {
                        res == null -> {
                            Button(
                                onClick = {
                                    var correct = 0
                                    val parts = mutableListOf<String>()
                                    questions.forEachIndexed { idx, q ->
                                        val pickedIdx = selectedAnswers[q.id] ?: -1
                                        val isRight = pickedIdx == q.correctIndex
                                        if (isRight) correct++
                                        val label = q.options.getOrNull(pickedIdx) ?: "-"
                                        parts.add("س${idx + 1}: $label (${if (isRight) "صحيح" else "خطأ"})")
                                    }
                                    val total = questions.size
                                    val pct = if (total == 0) 0 else (correct * 100) / total
                                    val summary =
                                        "نتيجة مسابقة $weekLabel داخل التطبيق: $correct/$total ($pct%) • ${parts.joinToString(" | ")}"
                                    evaluation = QuizEvaluation(correct, total, pct, summary)
                                },
                                enabled = allAnswered,
                                colors = ButtonDefaults.buttonColors(containerColor = MidnightTeal),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("evaluate_weekly_quiz_button")
                            ) {
                                Text(
                                    text = if (allAnswered) "تصحيح الإجابات وعرض النتيجة" else "أجب على جميع الأسئلة ($answeredCount/${questions.size})",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        isLocked -> {
                            Button(
                                onClick = onDismiss,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                Text(
                                    text = "تم تسجيل المسابقة بالفعل لهذا الأسبوع — إغلاق",
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        else -> {
                            Button(
                                onClick = {
                                    onSubmitAnswers(res.noteSummary)
                                    onDismiss()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = NativityBurgundy),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("submit_weekly_quiz_answers_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "إرسال الإجابات والنتيجة للخادم (+$points نقطة)",
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
}
