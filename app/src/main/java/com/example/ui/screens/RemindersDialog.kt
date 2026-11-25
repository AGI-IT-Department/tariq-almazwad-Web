package com.example.ui.screens

import android.Manifest
import android.app.TimePickerDialog
import android.content.Context
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.BibleBook
import com.example.data.BibleReadingPlan
import com.example.data.BibleTestament
import com.example.data.ReminderSettings
import com.example.notifications.ReminderScheduler
import com.example.ui.components.formatTimeArabic
import com.example.ui.theme.MidnightTeal
import com.example.ui.theme.MidnightTealDark
import com.example.ui.theme.PendingAmberBg
import com.example.ui.theme.WarmGold

private fun openTimePicker(
    context: Context,
    initialHour: Int,
    initialMinute: Int,
    onPicked: (Int, Int) -> Unit
) {
    TimePickerDialog(
        context,
        { _, hourOfDay, minute -> onPicked(hourOfDay, minute) },
        initialHour,
        initialMinute,
        false
    ).show()
}

@Composable
fun RemindersDialog(
    settings: ReminderSettings,
    onUpdateSettings: (ReminderSettings) -> Unit,
    onSendTestBible: () -> Unit,
    onSendTest: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(ReminderScheduler.hasNotificationPermission(context)) }
    var showBookPicker by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
    }

    val selectedBook = remember(settings.bibleBookId) {
        BibleReadingPlan.getBookById(settings.bibleBookId)
    }
    val todayReading = remember(settings.bibleBookId, settings.bibleStartDateIso) {
        BibleReadingPlan.getTodayReading(settings.bibleBookId, settings.bibleStartDateIso)
    }

    if (showBookPicker) {
        BibleBookPickerDialog(
            selectedBookId = settings.bibleBookId,
            onSelectBook = { book ->
                onUpdateSettings(settings.copy(bibleBookId = book.id))
                showBookPicker = false
            },
            onDismiss = { showBookPicker = false }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Filled.NotificationsActive, contentDescription = null, tint = WarmGold)
                Text("الإشعارات وورد الكتاب المقدس", style = MaterialTheme.typography.titleMedium)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (!hasPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "يرجى السماح بصلاحية الإشعارات لاستلام تنبيه الإنجازات اليومية وإصحاح اليوم واعتماد نقاطك الجديدة.",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Button(
                                onClick = { launcher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                                colors = ButtonDefaults.buttonColors(containerColor = MidnightTeal)
                            ) {
                                Text("تفعيل الإشعارات الآن", color = Color.White)
                            }
                        }
                    }
                }

                // 1. Daily Bible Reading Plan (Book + Custom Time)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = PendingAmberBg.copy(alpha = 0.55f),
                    border = BorderStroke(1.dp, WarmGold.copy(alpha = 0.55f)),
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
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Filled.MenuBook,
                                    contentDescription = null,
                                    tint = MidnightTeal,
                                    modifier = Modifier.size(18.dp)
                                )
                                Column {
                                    Text(
                                        text = "ورد الكتاب المقدس اليومي 📖",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "تنبيه يومي بالإصحاح الذي عليه الدور",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = settings.bibleReadingReminderEnabled,
                                onCheckedChange = {
                                    onUpdateSettings(settings.copy(bibleReadingReminderEnabled = it))
                                }
                            )
                        }

                        // Book selector button
                        OutlinedButton(
                            onClick = { showBookPicker = true },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MidnightTeal.copy(alpha = 0.35f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("choose_bible_book_button")
                        ) {
                            Icon(
                                Icons.Filled.MenuBook,
                                contentDescription = null,
                                tint = MidnightTeal,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "السفر المختار: ${selectedBook.nameAr} (${selectedBook.chapters} إصحاح)",
                                style = MaterialTheme.typography.labelMedium,
                                color = MidnightTealDark
                            )
                        }

                        // Custom time picker row for Bible reading
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "وقت تنبيه الإصحاح:",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            TimePickerChip(
                                hour = settings.bibleReadingHour,
                                minute = settings.bibleReadingMinute,
                                tag = "bible_time_picker_chip",
                                onClick = {
                                    openTimePicker(
                                        context,
                                        settings.bibleReadingHour,
                                        settings.bibleReadingMinute
                                    ) { h, m ->
                                        onUpdateSettings(
                                            settings.copy(
                                                bibleReadingHour = h,
                                                bibleReadingMinute = m
                                            )
                                        )
                                    }
                                }
                            )
                        }

                        // Live preview of today's chapter
                        Surface(
                            color = Color.White.copy(alpha = 0.85f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(
                                    text = "إصحاح اليوم: ${todayReading.bookNameAr} — الإصحاح ${todayReading.chapterNumber}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MidnightTealDark
                                )
                                Text(
                                    text = "يبدأ تلقائياً من الإصحاح 1 مع بداية التحدي (30 نوفمبر) ويكمل السفر التالي تلقائياً.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // 2. Daily Achievements Reminder at user-chosen time
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MidnightTeal.copy(alpha = 0.06f),
                    border = BorderStroke(1.dp, MidnightTeal.copy(alpha = 0.25f)),
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
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Checklist,
                                    contentDescription = null,
                                    tint = MidnightTeal,
                                    modifier = Modifier.size(18.dp)
                                )
                                Column {
                                    Text(
                                        text = "تنبيه الإنجازات اليومية ✅",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "يذكّرك بالمهام اليومية المتبقية في وقتك المفضل",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = settings.dailyTasksReminderEnabled,
                                onCheckedChange = {
                                    onUpdateSettings(settings.copy(dailyTasksReminderEnabled = it))
                                }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "وقت التنبيه اليومي:",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            TimePickerChip(
                                hour = settings.dailyTasksReminderHour,
                                minute = settings.dailyTasksReminderMinute,
                                tag = "daily_tasks_time_picker_chip",
                                onClick = {
                                    openTimePicker(
                                        context,
                                        settings.dailyTasksReminderHour,
                                        settings.dailyTasksReminderMinute
                                    ) { h, m ->
                                        onUpdateSettings(
                                            settings.copy(
                                                dailyTasksReminderHour = h,
                                                dailyTasksReminderMinute = m
                                            )
                                        )
                                    }
                                }
                            )
                        }
                    }
                }

                // 3. Points Approval Alerts
                ReminderToggleRow(
                    title = "إشعارات اعتماد النقاط الجديدة 🎉",
                    timeLabel = "تنبيه فوري عند اعتماد خدام التحدي لمهامك أو إضافة نقاط بونص",
                    checked = settings.pointsApprovalEnabled,
                    onCheckedChange = { onUpdateSettings(settings.copy(pointsApprovalEnabled = it)) }
                )

                HorizontalDivider()

                Text(
                    text = "تذكيرات روحية إضافية (اضغط على الوقت للتعديل)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                ReminderToggleRow(
                    title = "صلاة باكر",
                    timeLabel = formatTimeArabic(settings.morningPrayerHour, settings.morningPrayerMinute),
                    checked = settings.morningPrayerEnabled,
                    onCheckedChange = { onUpdateSettings(settings.copy(morningPrayerEnabled = it)) },
                    onEditTime = {
                        openTimePicker(context, settings.morningPrayerHour, settings.morningPrayerMinute) { h, m ->
                            onUpdateSettings(settings.copy(morningPrayerHour = h, morningPrayerMinute = m))
                        }
                    }
                )
                ReminderToggleRow(
                    title = "صلاة النوم والأجبية",
                    timeLabel = formatTimeArabic(settings.nightPrayerHour, settings.nightPrayerMinute),
                    checked = settings.nightPrayerEnabled,
                    onCheckedChange = { onUpdateSettings(settings.copy(nightPrayerEnabled = it)) },
                    onEditTime = {
                        openTimePicker(context, settings.nightPrayerHour, settings.nightPrayerMinute) { h, m ->
                            onUpdateSettings(settings.copy(nightPrayerHour = h, nightPrayerMinute = m))
                        }
                    }
                )
                ReminderToggleRow(
                    title = "القداس والتناول الأسبوعي",
                    timeLabel = "الجمعة • ${formatTimeArabic(settings.liturgyHour, settings.liturgyMinute)}",
                    checked = settings.liturgyEnabled,
                    onCheckedChange = { onUpdateSettings(settings.copy(liturgyEnabled = it)) },
                    onEditTime = {
                        openTimePicker(context, settings.liturgyHour, settings.liturgyMinute) { h, m ->
                            onUpdateSettings(settings.copy(liturgyHour = h, liturgyMinute = m))
                        }
                    }
                )
                ReminderToggleRow(
                    title = "اجتماع الخريجين والتسبحة",
                    timeLabel = "الخميس • ${formatTimeArabic(settings.meetingHour, settings.meetingMinute)}",
                    checked = settings.meetingEnabled,
                    onCheckedChange = { onUpdateSettings(settings.copy(meetingEnabled = it)) },
                    onEditTime = {
                        openTimePicker(context, settings.meetingHour, settings.meetingMinute) { h, m ->
                            onUpdateSettings(settings.copy(meetingHour = h, meetingMinute = m))
                        }
                    }
                )
                ReminderToggleRow(
                    title = "تشجيع وآية اليوم",
                    timeLabel = formatTimeArabic(settings.dailyEncouragementHour, settings.dailyEncouragementMinute),
                    checked = settings.dailyEncouragementEnabled,
                    onCheckedChange = { onUpdateSettings(settings.copy(dailyEncouragementEnabled = it)) },
                    onEditTime = {
                        openTimePicker(context, settings.dailyEncouragementHour, settings.dailyEncouragementMinute) { h, m ->
                            onUpdateSettings(settings.copy(dailyEncouragementHour = h, dailyEncouragementMinute = m))
                        }
                    }
                )

                Button(
                    onClick = onSendTestBible,
                    colors = ButtonDefaults.buttonColors(containerColor = MidnightTeal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("test_bible_notification_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("تجربة إشعار إصحاح اليوم الآن 📖", color = Color.White)
                }

                OutlinedButton(
                    onClick = onSendTest,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("test_encouragement_notification_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("تجربة إشعار تشجيعي الآن ✨")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("تم")
            }
        }
    )
}

@Composable
private fun TimePickerChip(
    hour: Int,
    minute: Int,
    tag: String,
    onClick: () -> Unit
) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, MidnightTeal.copy(alpha = 0.35f)),
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .testTag(tag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.AccessTime,
                contentDescription = "تغيير الوقت",
                tint = WarmGold,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = formatTimeArabic(hour, minute),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MidnightTealDark
            )
        }
    }
}

@Composable
private fun ReminderToggleRow(
    title: String,
    timeLabel: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onEditTime: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .then(
                    if (onEditTime != null) Modifier.clickable(onClick = onEditTime) else Modifier
                )
        ) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            Text(
                text = if (onEditTime != null) "$timeLabel 🕒" else timeLabel,
                style = MaterialTheme.typography.bodySmall,
                color = if (onEditTime != null) MidnightTeal else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun BibleBookPickerDialog(
    selectedBookId: String,
    onSelectBook: (BibleBook) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTestament by remember { mutableStateOf(BibleTestament.NT) }
    val books = remember(selectedTestament) {
        BibleReadingPlan.allBooks.filter { it.testament == selectedTestament }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("اختر السفر للبدء من أول التحدي 📖", style = MaterialTheme.typography.titleMedium)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedTestament == BibleTestament.NT,
                        onClick = { selectedTestament = BibleTestament.NT },
                        label = { Text("العهد الجديد") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MidnightTeal,
                            selectedLabelColor = Color.White
                        )
                    )
                    FilterChip(
                        selected = selectedTestament == BibleTestament.OT,
                        onClick = { selectedTestament = BibleTestament.OT },
                        label = { Text("العهد القديم") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MidnightTeal,
                            selectedLabelColor = Color.White
                        )
                    )
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(books, key = { it.id }) { book ->
                        val isSelected = book.id == selectedBookId
                        Surface(
                            color = if (isSelected) MidnightTeal.copy(alpha = 0.12f) else Color.Transparent,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onSelectBook(book) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = book.nameAr,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) MidnightTeal else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${book.chapters} إصحاح",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = "مختار",
                                        tint = MidnightTeal
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("إغلاق")
            }
        }
    )
}
