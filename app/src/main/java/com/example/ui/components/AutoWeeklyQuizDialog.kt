package com.example.ui.components

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.AutoContentBank
import com.example.data.AutoQuizDraft
import com.example.data.AutoQuizGradeResult
import com.example.data.AutoWeeklyQuiz
import com.example.ui.theme.ApprovedEmerald
import com.example.ui.theme.ApprovedEmeraldBg
import com.example.ui.theme.BrightGold
import com.example.ui.theme.MidnightTeal
import com.example.ui.theme.MidnightTealDark
import com.example.ui.theme.NativityBurgundy
import com.example.ui.theme.RejectedRed
import com.example.ui.theme.RejectedRedBg
import com.example.ui.theme.WarmGold
import com.example.ui.theme.WarmGoldContainer

private enum class AutoQuizSectionTab(val labelAr: String) {
    MCQ("اختيار من متعدد"),
    TF("صح وخطأ"),
    MATCH("توصيل")
}

private val ARABIC_LETTERS = listOf("أ", "ب", "ج", "د")

@Composable
fun AutoWeeklyQuizDialog(
    quiz: AutoWeeklyQuiz,
    userId: String,
    adminPreview: Boolean = false,
    completedPoints: Int? = null,
    onSubmitResult: ((weekNumber: Int, correctCount: Int, totalItems: Int, earnedPoints: Int, onDone: (Boolean) -> Unit) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isReadOnly = adminPreview || completedPoints != null
    var section by remember { mutableStateOf(AutoQuizSectionTab.MCQ) }
    var onlyUnanswered by remember { mutableStateOf(false) }
    var draft by remember(userId, quiz.weekNumber, isReadOnly) {
        mutableStateOf(
            if (isReadOnly) AutoQuizDraft()
            else AutoContentBank.loadDraft(context, userId, quiz.weekNumber)
        )
    }
    var activeMatchLeft by remember { mutableStateOf<Map<String, Int?>>(emptyMap()) }
    var submittedResult by remember { mutableStateOf<AutoQuizGradeResult?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }
    var showIncompleteConfirm by remember { mutableStateOf(false) }

    val showAnswers = isReadOnly || submittedResult != null

    LaunchedEffect(draft, isReadOnly, submittedResult) {
        if (!isReadOnly && submittedResult == null && userId.isNotBlank()) {
            AutoContentBank.saveDraft(context, userId, quiz.weekNumber, draft)
        }
    }

    val mcqAnswered = remember(draft.mcqAnswers, quiz.mcq) {
        quiz.mcq.count { draft.mcqAnswers.containsKey(it.id) }
    }
    val tfAnswered = remember(draft.tfAnswers, quiz.tf) {
        quiz.tf.count { draft.tfAnswers.containsKey(it.id) }
    }
    val matchTotal = remember(quiz.match) { quiz.match.sumOf { it.pairs.size } }
    val matchAnswered = remember(draft.matchAnswers, quiz.match) {
        var count = 0
        quiz.match.forEach { g ->
            g.pairs.indices.forEach { idx ->
                if (draft.matchAnswers.containsKey("${g.id}:$idx")) count++
            }
        }
        count
    }
    val totalItems = quiz.mcq.size + quiz.tf.size + matchTotal
    val totalAnswered = mcqAnswered + tfAnswered + matchAnswered
    val progressFraction = if (totalItems > 0) totalAnswered.toFloat() / totalItems.toFloat() else 0f

    val shuffledByGroup = remember(quiz) {
        quiz.match.associate { g ->
            g.id to AutoContentBank.getShuffledRightIndices(g.id, g.pairs.size)
        }
    }

    fun assignMatchPair(groupId: String, leftIdx: Int, originalRightIdx: Int) {
        val nextMatch = draft.matchAnswers.toMutableMap()
        val keysToClear = nextMatch.entries
            .filter { it.key.startsWith("$groupId:") && it.value == originalRightIdx }
            .map { it.key }
        keysToClear.forEach { nextMatch.remove(it) }
        nextMatch["$groupId:$leftIdx"] = originalRightIdx
        draft = draft.copy(matchAnswers = nextMatch)

        val group = quiz.match.firstOrNull { it.id == groupId }
        if (group != null) {
            val nextUnmatched = group.pairs.indices.firstOrNull { idx ->
                idx != leftIdx && !nextMatch.containsKey("$groupId:$idx")
            }
            activeMatchLeft = activeMatchLeft + (groupId to nextUnmatched)
        }
    }

    fun finalizeSubmit() {
        val submitCb = onSubmitResult ?: return
        if (isSubmitting) return
        isSubmitting = true
        showIncompleteConfirm = false
        val grade = AutoContentBank.gradeAutoQuiz(quiz, draft)
        submitCb(quiz.weekNumber, grade.correctCount, grade.totalItems, grade.earnedPoints) { ok ->
            isSubmitting = false
            if (ok) {
                submittedResult = grade
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Sticky Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(MidnightTealDark, MidnightTeal)
                            )
                        )
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = WarmGold.copy(alpha = 0.24f),
                                        border = BorderStroke(1.dp, WarmGold.copy(alpha = 0.55f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.AutoAwesome,
                                                contentDescription = null,
                                                tint = BrightGold,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Text(
                                                text = "${quiz.totalItems} سؤال • ${quiz.maxPoints} نقطة",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = BrightGold,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    if (adminPreview) {
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = Color.White.copy(alpha = 0.16f)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Visibility,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Text(
                                                    text = "معاينة الخدام",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    } else if (completedPoints != null) {
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = ApprovedEmerald.copy(alpha = 0.3f),
                                            border = BorderStroke(1.dp, ApprovedEmerald)
                                        ) {
                                            Text(
                                                text = "نتيجتك: $completedPoints / ${quiz.maxPoints} نقطة ✅",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = quiz.titleAr,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = quiz.subtitleAr,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }

                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.testTag("close_auto_quiz_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "إغلاق المسابقة",
                                    tint = Color.White
                                )
                            }
                        }

                        if (!isReadOnly && submittedResult == null) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "تمت الإجابة على $totalAnswered / $totalItems سؤال (حفظ تلقائي)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.9f)
                                    )
                                    Text(
                                        text = "${(progressFraction * 100).toInt()}%",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = BrightGold,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { progressFraction },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(50)),
                                    color = BrightGold,
                                    trackColor = Color.White.copy(alpha = 0.2f)
                                )
                            }
                        }

                        // Section Tabs
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            AutoQuizSectionTab.entries.forEach { tab ->
                                val selected = section == tab
                                val countLabel = when (tab) {
                                    AutoQuizSectionTab.MCQ ->
                                        if (isReadOnly) "${quiz.mcq.size}" else "$mcqAnswered/${quiz.mcq.size}"
                                    AutoQuizSectionTab.TF ->
                                        if (isReadOnly) "${quiz.tf.size}" else "$tfAnswered/${quiz.tf.size}"
                                    AutoQuizSectionTab.MATCH ->
                                        if (isReadOnly) "$matchTotal" else "$matchAnswered/$matchTotal"
                                }
                                Surface(
                                    onClick = { section = tab },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (selected) WarmGold else Color.White.copy(alpha = 0.14f),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = "${tab.labelAr}\n($countLabel)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (selected) MidnightTealDark else Color.White,
                                        fontWeight = FontWeight.ExtraBold,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Body
                val filteredMcq = remember(quiz.mcq, draft.mcqAnswers, onlyUnanswered, showAnswers) {
                    if (onlyUnanswered && !showAnswers) quiz.mcq.filter { !draft.mcqAnswers.containsKey(it.id) }
                    else quiz.mcq
                }
                val filteredTf = remember(quiz.tf, draft.tfAnswers, onlyUnanswered, showAnswers) {
                    if (onlyUnanswered && !showAnswers) quiz.tf.filter { !draft.tfAnswers.containsKey(it.id) }
                    else quiz.tf
                }

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Instant Score Summary Card
                    submittedResult?.let { grade ->
                        item(key = "score_summary") {
                            Card(
                                shape = RoundedCornerShape(22.dp),
                                colors = CardDefaults.cardColors(containerColor = MidnightTeal),
                                border = BorderStroke(1.5.dp, WarmGold)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(16.dp),
                                            color = WarmGold.copy(alpha = 0.22f),
                                            border = BorderStroke(1.dp, WarmGold)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.EmojiEvents,
                                                contentDescription = null,
                                                tint = BrightGold,
                                                modifier = Modifier
                                                    .padding(10.dp)
                                                    .size(28.dp)
                                            )
                                        }
                                        Column {
                                            Text(
                                                text = "تم التصحيح التلقائي وإضافة النقاط مباشرة لرصيدك ✅",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = BrightGold,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "${grade.earnedPoints} / ${grade.maxPoints} نقطة",
                                                style = MaterialTheme.typography.headlineSmall,
                                                color = Color.White,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                            Text(
                                                text = "${grade.correctCount} إجابة صحيحة من ${grade.totalItems} (${grade.percent}%)",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color.White.copy(alpha = 0.85f)
                                            )
                                        }
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        ScoreChip("اختيار متعدد", "${grade.mcqCorrect}/${grade.mcqTotal}", Modifier.weight(1f))
                                        ScoreChip("صح وخطأ", "${grade.tfCorrect}/${grade.tfTotal}", Modifier.weight(1f))
                                        ScoreChip("توصيل", "${grade.matchCorrect}/${grade.matchTotal}", Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }

                    // Filter toggle for unanswered
                    if (!showAnswers && section != AutoQuizSectionTab.MATCH) {
                        item(key = "unanswered_filter") {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.FilterList,
                                            contentDescription = null,
                                            tint = WarmGold,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "عرض غير المجاب عليها فقط",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Surface(
                                        onClick = { onlyUnanswered = !onlyUnanswered },
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (onlyUnanswered) MidnightTeal else MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        Text(
                                            text = if (onlyUnanswered) "مفعّل ✓" else "الكل",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (onlyUnanswered) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    when (section) {
                        AutoQuizSectionTab.MCQ -> {
                            if (filteredMcq.isEmpty()) {
                                item(key = "mcq_done") {
                                    AllAnsweredCard("أحسنت! أجبت على جميع أسئلة الاختيار من متعدد (${quiz.mcq.size}/${quiz.mcq.size}) ✅")
                                }
                            } else {
                                items(filteredMcq, key = { it.id }) { item ->
                                    val originalNum = quiz.mcq.indexOfFirst { it.id == item.id } + 1
                                    val picked = draft.mcqAnswers[item.id]
                                    Card(
                                        shape = RoundedCornerShape(18.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(14.dp),
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.Top,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Surface(
                                                    shape = RoundedCornerShape(10.dp),
                                                    color = MidnightTeal
                                                ) {
                                                    Text(
                                                        text = "$originalNum",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = BrightGold,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                                    )
                                                }
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = item.q,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Text(
                                                        text = "(${quiz.pointsPerItem} نقطتان)",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = WarmGold,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                item.options.forEachIndexed { optIdx, optText ->
                                                    val isPicked = picked == optIdx
                                                    val isCorrect = item.correctIndex == optIdx
                                                    val bgColor = when {
                                                        showAnswers && isCorrect -> ApprovedEmeraldBg
                                                        showAnswers && isPicked && !isCorrect -> RejectedRedBg
                                                        !showAnswers && isPicked -> MidnightTeal
                                                        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                                                    }
                                                    val borderColor = when {
                                                        showAnswers && isCorrect -> ApprovedEmerald
                                                        showAnswers && isPicked && !isCorrect -> RejectedRed
                                                        !showAnswers && isPicked -> MidnightTeal
                                                        else -> MaterialTheme.colorScheme.outlineVariant
                                                    }
                                                    val textColor = when {
                                                        showAnswers && isCorrect -> ApprovedEmerald
                                                        showAnswers && isPicked && !isCorrect -> RejectedRed
                                                        !showAnswers && isPicked -> Color.White
                                                        else -> MaterialTheme.colorScheme.onSurface
                                                    }
                                                    Surface(
                                                        shape = RoundedCornerShape(12.dp),
                                                        color = bgColor,
                                                        border = BorderStroke(1.dp, borderColor),
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clickable(enabled = !showAnswers) {
                                                                draft = draft.copy(
                                                                    mcqAnswers = draft.mcqAnswers + (item.id to optIdx)
                                                                )
                                                            }
                                                    ) {
                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                        ) {
                                                            Surface(
                                                                shape = RoundedCornerShape(8.dp),
                                                                color = when {
                                                                    !showAnswers && isPicked -> WarmGold
                                                                    showAnswers && isCorrect -> ApprovedEmerald
                                                                    else -> MaterialTheme.colorScheme.surface
                                                                }
                                                            ) {
                                                                Text(
                                                                    text = ARABIC_LETTERS.getOrElse(optIdx) { "${optIdx + 1}" },
                                                                    style = MaterialTheme.typography.labelMedium,
                                                                    color = when {
                                                                        !showAnswers && isPicked -> MidnightTealDark
                                                                        showAnswers && isCorrect -> Color.White
                                                                        else -> MaterialTheme.colorScheme.onSurface
                                                                    },
                                                                    fontWeight = FontWeight.ExtraBold,
                                                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                                                )
                                                            }
                                                            Text(
                                                                text = optText,
                                                                style = MaterialTheme.typography.bodyMedium,
                                                                color = textColor,
                                                                fontWeight = if (isPicked || (showAnswers && isCorrect)) FontWeight.Bold else FontWeight.Normal,
                                                                modifier = Modifier.weight(1f)
                                                            )
                                                            if (showAnswers && isCorrect) {
                                                                Icon(
                                                                    imageVector = Icons.Filled.CheckCircle,
                                                                    contentDescription = "إجابة صحيحة",
                                                                    tint = ApprovedEmerald,
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
                        }

                        AutoQuizSectionTab.TF -> {
                            if (filteredTf.isEmpty()) {
                                item(key = "tf_done") {
                                    AllAnsweredCard("أحسنت! أجبت على جميع أسئلة الصح والخطأ (${quiz.tf.size}/${quiz.tf.size}) ✅")
                                }
                            } else {
                                items(filteredTf, key = { it.id }) { item ->
                                    val originalNum = quiz.mcq.size + quiz.tf.indexOfFirst { it.id == item.id } + 1
                                    val picked = draft.tfAnswers[item.id]
                                    Card(
                                        shape = RoundedCornerShape(18.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(14.dp),
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.Top,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Surface(
                                                    shape = RoundedCornerShape(10.dp),
                                                    color = NativityBurgundy
                                                ) {
                                                    Text(
                                                        text = "$originalNum",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = Color.White,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                                    )
                                                }
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = item.q,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Text(
                                                        text = "(${quiz.pointsPerItem} نقطتان)",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = WarmGold,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                listOf(true to "صح ✓", false to "خطأ ✗").forEach { (choiceVal, label) ->
                                                    val isPicked = picked == choiceVal
                                                    val isCorrect = item.correctAnswer == choiceVal
                                                    val bgColor = when {
                                                        showAnswers && isCorrect -> ApprovedEmeraldBg
                                                        showAnswers && isPicked && !isCorrect -> RejectedRedBg
                                                        !showAnswers && isPicked -> MidnightTeal
                                                        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                                                    }
                                                    val borderColor = when {
                                                        showAnswers && isCorrect -> ApprovedEmerald
                                                        showAnswers && isPicked && !isCorrect -> RejectedRed
                                                        !showAnswers && isPicked -> MidnightTeal
                                                        else -> MaterialTheme.colorScheme.outlineVariant
                                                    }
                                                    val textColor = when {
                                                        showAnswers && isCorrect -> ApprovedEmerald
                                                        showAnswers && isPicked && !isCorrect -> RejectedRed
                                                        !showAnswers && isPicked -> Color.White
                                                        else -> MaterialTheme.colorScheme.onSurface
                                                    }
                                                    Surface(
                                                        shape = RoundedCornerShape(12.dp),
                                                        color = bgColor,
                                                        border = BorderStroke(1.dp, borderColor),
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .clickable(enabled = !showAnswers) {
                                                                draft = draft.copy(
                                                                    tfAnswers = draft.tfAnswers + (item.id to choiceVal)
                                                                )
                                                            }
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.padding(vertical = 12.dp),
                                                            horizontalArrangement = Arrangement.Center,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text(
                                                                text = label,
                                                                style = MaterialTheme.typography.labelLarge,
                                                                color = textColor,
                                                                fontWeight = FontWeight.ExtraBold
                                                            )
                                                            if (showAnswers && isCorrect) {
                                                                Spacer(Modifier.width(4.dp))
                                                                Icon(
                                                                    imageVector = Icons.Filled.Check,
                                                                    contentDescription = null,
                                                                    tint = ApprovedEmerald,
                                                                    modifier = Modifier.size(16.dp)
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

                        AutoQuizSectionTab.MATCH -> {
                            if (!showAnswers) {
                                item(key = "match_tip") {
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = WarmGoldContainer,
                                        border = BorderStroke(1.dp, WarmGold.copy(alpha = 0.55f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Link,
                                                contentDescription = null,
                                                tint = MidnightTeal,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = "اضغط على العبارة في العمود (أ) ثم اضغط على الإجابة المناسبة في العمود (ب) للتوصيل (كل زوج صحيح = نقطتان).",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MidnightTealDark,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                            }
                            itemsIndexed(quiz.match, key = { _, g -> g.id }) { groupIdx, group ->
                                val shuffledRight = shuffledByGroup[group.id] ?: group.pairs.indices.toList()
                                val activeLeft = activeMatchLeft[group.id]
                                val usedRightToLeft = buildMap {
                                    group.pairs.indices.forEach { lIdx ->
                                        val chosenR = draft.matchAnswers["${group.id}:$lIdx"]
                                        if (chosenR != null) put(chosenR, lIdx)
                                    }
                                }
                                Card(
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                modifier = Modifier.weight(1f),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Surface(
                                                    shape = RoundedCornerShape(10.dp),
                                                    color = MidnightTeal
                                                ) {
                                                    Text(
                                                        text = "مجموعة ${groupIdx + 1}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = BrightGold,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                    )
                                                }
                                                Text(
                                                    text = group.title,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    fontWeight = FontWeight.ExtraBold
                                                )
                                            }
                                            Text(
                                                text = "${group.pairs.size * quiz.pointsPerItem} نقاط",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = WarmGold,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        Text(
                                            text = "العمود (أ) — اختر العنصر:",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = FontWeight.Bold
                                        )

                                        group.pairs.forEachIndexed { leftIdx, pair ->
                                            val chosenR = draft.matchAnswers["${group.id}:$leftIdx"]
                                            val isMatched = chosenR != null
                                            val isSelected = activeLeft == leftIdx
                                            val isCorrect = chosenR == leftIdx
                                            val matchedLetter = chosenR?.let { r ->
                                                val pos = shuffledRight.indexOf(r)
                                                ARABIC_LETTERS.getOrElse(pos) { "" }
                                            }.orEmpty()

                                            val bgColor = when {
                                                showAnswers && (adminPreview || completedPoints != null) -> ApprovedEmeraldBg.copy(alpha = 0.65f)
                                                showAnswers && isCorrect -> ApprovedEmeraldBg
                                                showAnswers -> RejectedRedBg
                                                isSelected -> MidnightTeal
                                                isMatched -> WarmGoldContainer
                                                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                            }
                                            val borderColor = when {
                                                showAnswers && (adminPreview || completedPoints != null || isCorrect) -> ApprovedEmerald
                                                showAnswers -> RejectedRed
                                                isSelected -> MidnightTeal
                                                isMatched -> WarmGold
                                                else -> MaterialTheme.colorScheme.outlineVariant
                                            }
                                            val primaryText = when {
                                                !showAnswers && isSelected -> Color.White
                                                !showAnswers && isMatched -> MidnightTealDark
                                                showAnswers -> MidnightTealDark
                                                else -> MaterialTheme.colorScheme.onSurface
                                            }

                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = bgColor,
                                                border = BorderStroke(1.dp, borderColor),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable(enabled = !showAnswers) {
                                                        activeMatchLeft = activeMatchLeft + (
                                                            group.id to if (isSelected) null else leftIdx
                                                        )
                                                    }
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(11.dp),
                                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.weight(1f),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                        ) {
                                                            Surface(
                                                                shape = RoundedCornerShape(8.dp),
                                                                color = if (!showAnswers && isSelected) WarmGold else MidnightTeal.copy(alpha = 0.14f)
                                                            ) {
                                                                Text(
                                                                    text = "${leftIdx + 1}",
                                                                    style = MaterialTheme.typography.labelMedium,
                                                                    color = MidnightTealDark,
                                                                    fontWeight = FontWeight.ExtraBold,
                                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                                )
                                                            }
                                                            Text(
                                                                text = pair.left,
                                                                style = MaterialTheme.typography.bodyMedium,
                                                                color = primaryText,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        }
                                                        if (!showAnswers && isMatched) {
                                                            Surface(
                                                                onClick = {
                                                                    val next = draft.matchAnswers.toMutableMap()
                                                                    next.remove("${group.id}:$leftIdx")
                                                                    draft = draft.copy(matchAnswers = next)
                                                                },
                                                                shape = RoundedCornerShape(8.dp),
                                                                color = WarmGold
                                                            ) {
                                                                Text(
                                                                    text = "← $matchedLetter ✕",
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    color = MidnightTealDark,
                                                                    fontWeight = FontWeight.ExtraBold,
                                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                                )
                                                            }
                                                        }
                                                    }
                                                    if (!showAnswers && chosenR != null) {
                                                        Text(
                                                            text = "تم توصيله بـ: ${group.pairs.getOrNull(chosenR)?.right.orEmpty()}",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = if (isSelected) BrightGold else MidnightTeal,
                                                            fontWeight = FontWeight.SemiBold
                                                        )
                                                    }
                                                    if (showAnswers) {
                                                        if (submittedResult != null && chosenR != null && !isCorrect) {
                                                            Text(
                                                                text = "✗ إجابتك: ${group.pairs.getOrNull(chosenR)?.right.orEmpty()}",
                                                                style = MaterialTheme.typography.bodySmall,
                                                                color = RejectedRed,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        }
                                                        Text(
                                                            text = "✓ الإجابة الصحيحة: ${pair.right}",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = ApprovedEmerald,
                                                            fontWeight = FontWeight.ExtraBold
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        if (!showAnswers) {
                                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                            Text(
                                                text = if (activeLeft != null)
                                                    "العمود (ب) — اضغط على الإجابة المناسبة للعنصر (${activeLeft + 1}):"
                                                else
                                                    "العمود (ب) — اختر عنصراً من الأعلى ثم اضغط على الإجابة المناسبة:",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontWeight = FontWeight.Bold
                                            )
                                            shuffledRight.forEachIndexed { displayIdx, origIdx ->
                                                val pairedLeft = usedRightToLeft[origIdx]
                                                val isUsed = pairedLeft != null
                                                Surface(
                                                    shape = RoundedCornerShape(12.dp),
                                                    color = if (isUsed) WarmGoldContainer.copy(alpha = 0.6f)
                                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                                    border = BorderStroke(
                                                        1.dp,
                                                        if (activeLeft != null && !isUsed) WarmGold
                                                        else MaterialTheme.colorScheme.outlineVariant
                                                    ),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable {
                                                            val targetLeft = activeLeft ?: group.pairs.indices.firstOrNull { i ->
                                                                !draft.matchAnswers.containsKey("${group.id}:$i")
                                                            }
                                                            if (targetLeft != null) {
                                                                assignMatchPair(group.id, targetLeft, origIdx)
                                                            }
                                                        }
                                                ) {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(horizontal = 11.dp, vertical = 10.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        Surface(
                                                            shape = RoundedCornerShape(8.dp),
                                                            color = MaterialTheme.colorScheme.surface
                                                        ) {
                                                            Text(
                                                                text = ARABIC_LETTERS.getOrElse(displayIdx) { "${displayIdx + 1}" },
                                                                style = MaterialTheme.typography.labelMedium,
                                                                color = MaterialTheme.colorScheme.onSurface,
                                                                fontWeight = FontWeight.ExtraBold,
                                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                                            )
                                                        }
                                                        Text(
                                                            text = group.pairs.getOrNull(origIdx)?.right.orEmpty(),
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            color = if (isUsed) MidnightTealDark else MaterialTheme.colorScheme.onSurface,
                                                            modifier = Modifier.weight(1f)
                                                        )
                                                        if (pairedLeft != null) {
                                                            Surface(
                                                                shape = RoundedCornerShape(8.dp),
                                                                color = MidnightTeal
                                                            ) {
                                                                Text(
                                                                    text = "مربوط بـ ${pairedLeft + 1}",
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    color = BrightGold,
                                                                    fontWeight = FontWeight.Bold,
                                                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
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
                }

                // Sticky Bottom Bar
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        if (!isReadOnly && submittedResult == null) {
                            Column {
                                Text(
                                    text = "أجبت على $totalAnswered / $totalItems",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = "التصحيح والإضافة للرصيد فوريين",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Button(
                                onClick = {
                                    if (totalAnswered < totalItems) {
                                        showIncompleteConfirm = true
                                    } else {
                                        finalizeSubmit()
                                    }
                                },
                                enabled = !isSubmitting && totalAnswered > 0,
                                colors = ButtonDefaults.buttonColors(containerColor = MidnightTeal),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.testTag("submit_auto_quiz_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = BrightGold,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = if (isSubmitting) "جارٍ التصحيح..." else "إنهاء وتصحيح المسابقة",
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        } else {
                            Button(
                                onClick = onDismiss,
                                colors = ButtonDefaults.buttonColors(containerColor = MidnightTeal),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "العودة إلى التطبيق",
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showIncompleteConfirm) {
        AlertDialog(
            onDismissRequest = { showIncompleteConfirm = false },
            title = { Text("تأكيد تسليم المسابقة", fontWeight = FontWeight.ExtraBold) },
            text = {
                Text(
                    "لقد أجبت على $totalAnswered سؤالاً من أصل $totalItems، ومتبقي ${totalItems - totalAnswered} سؤال بدون إجابة. بعد التسليم يتم التصحيح التلقائي وإضافة النقاط مباشرة لرصيدك ولا يمكن إعادة المسابقة لنفس الأسبوع."
                )
            },
            confirmButton = {
                Button(
                    onClick = { finalizeSubmit() },
                    colors = ButtonDefaults.buttonColors(containerColor = MidnightTeal)
                ) {
                    Text("تسليم وتصحيح الآن", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showIncompleteConfirm = false }) {
                    Text("إكمال الأسئلة أولاً")
                }
            }
        )
    }
}

@Composable
private fun ScoreChip(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White.copy(alpha = 0.12f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.8f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                color = BrightGold,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
private fun AllAnsweredCard(text: String) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = ApprovedEmeraldBg,
        border = BorderStroke(1.dp, ApprovedEmerald),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = ApprovedEmerald,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(18.dp)
        )
    }
}
