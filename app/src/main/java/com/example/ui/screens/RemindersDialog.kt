package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.ReminderSettings
import com.example.notifications.ReminderScheduler
import com.example.ui.components.formatTimeArabic
import com.example.ui.theme.MidnightTeal
import com.example.ui.theme.WarmGold

@Composable
fun RemindersDialog(
    settings: ReminderSettings,
    onUpdateSettings: (ReminderSettings) -> Unit,
    onSendTest: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(ReminderScheduler.hasNotificationPermission(context)) }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Filled.NotificationsActive, contentDescription = null, tint = WarmGold)
                Text("التنبيهات اليومية للتشجيع", style = MaterialTheme.typography.titleLarge)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
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
                                text = "يرجى السماح بصلاحية الإشعارات لاستلام التذكيرات اليومية.",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Button(
                                onClick = { launcher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                                colors = ButtonDefaults.buttonColors(containerColor = MidnightTeal)
                            ) {
                                Text("السماح بالإشعارات", color = Color.White)
                            }
                        }
                    }
                }

                ReminderToggleRow(
                    title = "صلاة باكر والإنجيل",
                    timeLabel = formatTimeArabic(settings.morningPrayerHour, settings.morningPrayerMinute),
                    checked = settings.morningPrayerEnabled,
                    onCheckedChange = { onUpdateSettings(settings.copy(morningPrayerEnabled = it)) }
                )
                ReminderToggleRow(
                    title = "صلاة النوم والأجبية",
                    timeLabel = formatTimeArabic(settings.nightPrayerHour, settings.nightPrayerMinute),
                    checked = settings.nightPrayerEnabled,
                    onCheckedChange = { onUpdateSettings(settings.copy(nightPrayerEnabled = it)) }
                )
                ReminderToggleRow(
                    title = "القداس والتناول الأسبوعي",
                    timeLabel = "الجمعة • ${formatTimeArabic(settings.liturgyHour, settings.liturgyMinute)}",
                    checked = settings.liturgyEnabled,
                    onCheckedChange = { onUpdateSettings(settings.copy(liturgyEnabled = it)) }
                )
                ReminderToggleRow(
                    title = "اجتماع الخريجين والتسبحة",
                    timeLabel = "الخميس • ${formatTimeArabic(settings.meetingHour, settings.meetingMinute)}",
                    checked = settings.meetingEnabled,
                    onCheckedChange = { onUpdateSettings(settings.copy(meetingEnabled = it)) }
                )
                ReminderToggleRow(
                    title = "تشجيع وآية اليوم",
                    timeLabel = formatTimeArabic(settings.dailyEncouragementHour, settings.dailyEncouragementMinute),
                    checked = settings.dailyEncouragementEnabled,
                    onCheckedChange = { onUpdateSettings(settings.copy(dailyEncouragementEnabled = it)) }
                )

                OutlinedButton(
                    onClick = onSendTest,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("test_encouragement_notification_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("تجربة إشعار تشجيعي الآن")
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
private fun ReminderToggleRow(
    title: String,
    timeLabel: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            Text(
                text = timeLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
