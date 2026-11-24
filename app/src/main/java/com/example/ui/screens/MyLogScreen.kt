package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.FastCalendar
import com.example.data.SubmissionStatus
import com.example.ui.components.StatusBadge
import com.example.ui.components.formatTimestampArabic
import com.example.ui.state.AppUiState
import com.example.ui.theme.ApprovedEmerald
import com.example.ui.theme.ApprovedEmeraldBg
import com.example.ui.theme.MidnightTeal
import com.example.ui.theme.PendingAmber
import com.example.ui.theme.PendingAmberBg
import com.example.ui.theme.RejectedRed
import com.example.ui.theme.RejectedRedBg
import com.example.ui.theme.WarmGold

private enum class LogFilter(val label: String) {
    ALL("الكل"),
    APPROVED("مقبول"),
    PENDING("قيد المراجعة"),
    REJECTED("مرفوض")
}

@Composable
fun MyLogScreen(
    uiState: AppUiState,
    modifier: Modifier = Modifier
) {
    val submissions = uiState.userSubmissions
    val approvedCount = submissions.count { it.status == SubmissionStatus.APPROVED.key }
    val pendingCount = submissions.count { it.status == SubmissionStatus.PENDING.key }
    val rejectedCount = submissions.count { it.status == SubmissionStatus.REJECTED.key }

    var selectedFilterIdx by remember { mutableIntStateOf(0) }
    val filtered = remember(submissions, selectedFilterIdx) {
        when (LogFilter.entries[selectedFilterIdx]) {
            LogFilter.ALL -> submissions
            LogFilter.APPROVED -> submissions.filter { it.status == SubmissionStatus.APPROVED.key }
            LogFilter.PENDING -> submissions.filter { it.status == SubmissionStatus.PENDING.key }
            LogFilter.REJECTED -> submissions.filter { it.status == SubmissionStatus.REJECTED.key }
        }
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "سجلي الروحي",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "متابعة حالة كل المهام التي سجلتها وملاحظات الخادم",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LogStatChip("مقبول", approvedCount, ApprovedEmeraldBg, ApprovedEmerald, Modifier.weight(1f))
                    LogStatChip("قيد المراجعة", pendingCount, PendingAmberBg, PendingAmber, Modifier.weight(1f))
                    LogStatChip("مرفوض", rejectedCount, RejectedRedBg, RejectedRed, Modifier.weight(1f))
                }
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(LogFilter.entries.indices.toList()) { idx ->
                        val item = LogFilter.entries[idx]
                        FilterChip(
                            selected = selectedFilterIdx == idx,
                            onClick = { selectedFilterIdx = idx },
                            label = { Text(item.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MidnightTeal,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            if (filtered.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.History,
                                contentDescription = null,
                                tint = WarmGold,
                                modifier = Modifier.size(44.dp)
                            )
                            Text(
                                text = "لا توجد مشاركات في هذا التصنيف",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "سجّل مهامك اليومية من تبويب «اليوميات» وستظهر هنا مباشرة مع حالة الاعتماد.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(filtered, key = { it.id }) { sub ->
                    val status = SubmissionStatus.fromKey(sub.status)
                    val isTrialSub = FastCalendar.isTrialSubmission(sub)
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = sub.taskTitle,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (isTrialSub) {
                                        Surface(
                                            color = PendingAmberBg,
                                            shape = RoundedCornerShape(50)
                                        ) {
                                            Text(
                                                text = "تجريبي 🧪",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = PendingAmber,
                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                StatusBadge(status = status)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = formatTimestampArabic(sub.submittedAt),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (sub.points > 0) {
                                    Text(
                                        text = "+${sub.points} نقطة",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = WarmGold
                                    )
                                }
                            }

                            if (sub.note.isNotBlank()) {
                                Text(
                                    text = "ملاحظتك: ${sub.note}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            if (sub.adminNote.isNotBlank()) {
                                Surface(
                                    color = if (status == SubmissionStatus.REJECTED) RejectedRedBg else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "ملاحظة الخادم: ${sub.adminNote}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (status == SubmissionStatus.REJECTED) RejectedRed else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }
}

@Composable
private fun LogStatChip(
    label: String,
    count: Int,
    bg: Color,
    fg: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = bg,
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$count",
                style = MaterialTheme.typography.titleLarge,
                color = fg,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = fg
            )
        }
    }
}
