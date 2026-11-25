package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Church
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.ChallengeTask
import com.example.data.FastCalendar
import com.example.data.FastDate
import com.example.data.SubmissionStatus
import com.example.data.TaskFrequency
import com.example.data.UrlValidator
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.ApprovedEmerald
import com.example.ui.theme.ApprovedEmeraldBg
import com.example.ui.theme.BrightGold
import com.example.ui.theme.MidnightTeal
import com.example.ui.theme.MidnightTealDark
import com.example.ui.theme.NativityBurgundy
import com.example.ui.theme.PendingAmber
import com.example.ui.theme.PendingAmberBg
import com.example.ui.theme.RejectedRed
import com.example.ui.theme.RejectedRedBg
import com.example.ui.theme.WarmGold
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun taskIconForKey(key: String): ImageVector = when (key) {
    "book" -> Icons.Filled.MenuBook
    "quiz" -> Icons.Filled.Quiz
    "confession" -> Icons.Filled.VerifiedUser
    "church" -> Icons.Filled.Church
    "music" -> Icons.Filled.MusicNote
    "groups" -> Icons.Filled.Groups
    "prayer" -> Icons.Filled.SelfImprovement
    "heart" -> Icons.Filled.Favorite
    "flame" -> Icons.Filled.LocalFireDepartment
    else -> Icons.Filled.Star
}

fun openWhatsAppChat(context: Context, rawPhone: String, message: String = "") {
    val digits = rawPhone.filter { it.isDigit() }
    val normalized = when {
        digits.startsWith("00") -> digits.removePrefix("00")
        digits.startsWith("05") && digits.length == 10 -> "971${digits.removePrefix("0")}"
        digits.startsWith("01") && digits.length == 11 -> "20${digits.removePrefix("0")}"
        else -> digits
    }
    if (normalized.isEmpty()) return

    val encodedMsg = if (message.isBlank()) "" else "&text=${Uri.encode(message)}"
    val directUri = Uri.parse("https://api.whatsapp.com/send?phone=$normalized$encodedMsg")

    // Open regular WhatsApp (com.whatsapp) directly, NOT WhatsApp Business (com.whatsapp.w4b)
    val regularWhatsAppIntent = Intent(Intent.ACTION_VIEW, directUri).apply {
        setPackage("com.whatsapp")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    val openedRegular = runCatching {
        context.startActivity(regularWhatsAppIntent)
        true
    }.getOrDefault(false)

    if (!openedRegular) {
        val fallbackUri = Uri.parse(
            if (message.isBlank()) "https://wa.me/$normalized"
            else "https://wa.me/$normalized?text=${Uri.encode(message)}"
        )
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, fallbackUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        }
    }
}

fun openSafeExternalUrl(context: Context, url: String) {
    if (!UrlValidator.isSafeHttpUrl(url)) return
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url.trim())).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }
}

fun sharePlainText(context: Context, text: String, title: String = "مشاركة") {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    runCatching {
        context.startActivity(Intent.createChooser(intent, title).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }
}

fun formatTimestampArabic(epochMillis: Long): String {
    val sdf = SimpleDateFormat("dd/MM/yyyy • hh:mm a", Locale("ar"))
    return sdf.format(Date(epochMillis))
}

fun formatTimeArabic(hour: Int, minute: Int): String {
    val period = if (hour < 12) "ص" else "م"
    val h12 = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    return String.format(Locale.US, "%02d:%02d %s", h12, minute, period)
}

@Composable
fun NativityAppHeader(
    isSupabaseLive: Boolean,
    pendingAdminCount: Int,
    onOpenAdmin: () -> Unit,
    onOpenReminders: (() -> Unit)? = null,
    onRefresh: (() -> Unit)? = null,
    onLogout: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MidnightTeal,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.logo_challenge_emblem),
                        contentDescription = "شعار تحدي طريق المزود",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                    )
                    Column {
                        Text(
                            text = "تحدي طريق المزود",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "صوم الميلاد المجيد • اجتماع أبو سيفين للشباب الخريجين",
                            style = MaterialTheme.typography.labelSmall,
                            color = BrightGold
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    if (onRefresh != null && isSupabaseLive) {
                        IconButton(
                            onClick = onRefresh,
                            modifier = Modifier.testTag("header_refresh_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = "تحديث البيانات",
                                tint = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }
                    if (onOpenReminders != null) {
                        IconButton(
                            onClick = onOpenReminders,
                            modifier = Modifier.testTag("header_reminders_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.NotificationsActive,
                                contentDescription = "التنبيهات اليومية",
                                tint = BrightGold
                            )
                        }
                    }
                    IconButton(
                        onClick = onOpenAdmin,
                        modifier = Modifier.testTag("header_admin_login_button")
                    ) {
                        BadgedBox(
                            badge = {
                                if (pendingAdminCount > 0) {
                                    Badge(containerColor = NativityBurgundy) {
                                        Text(
                                            text = "$pendingAdminCount",
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AdminPanelSettings,
                                contentDescription = "دخول الخادم / الأدمن",
                                tint = Color.White
                            )
                        }
                    }
                    if (onLogout != null) {
                        IconButton(
                            onClick = onLogout,
                            modifier = Modifier.testTag("header_logout_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Logout,
                                contentDescription = "تسجيل الخروج",
                                tint = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = if (isSupabaseLive) Icons.Filled.CloudDone else Icons.Filled.CloudOff,
                    contentDescription = null,
                    tint = if (isSupabaseLive) ApprovedEmeraldBg else BrightGold.copy(alpha = 0.85f),
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = if (isSupabaseLive) {
                        "متصل بقاعدة بيانات Supabase الحية"
                    } else {
                        "وضع محلي فوري • أضف مفاتيح Supabase للمزامنة بين الموبايلات"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.78f)
                )
            }
        }
    }
}

@Composable
fun StatusBadge(
    status: SubmissionStatus,
    modifier: Modifier = Modifier
) {
    val (bg, fg, icon) = when (status) {
        SubmissionStatus.PENDING -> Triple(PendingAmberBg, PendingAmber, Icons.Filled.HourglassTop)
        SubmissionStatus.APPROVED -> Triple(ApprovedEmeraldBg, ApprovedEmerald, Icons.Filled.CheckCircle)
        SubmissionStatus.REJECTED -> Triple(RejectedRedBg, RejectedRed, Icons.Filled.ErrorOutline)
    }
    Surface(
        modifier = modifier,
        color = bg,
        shape = RoundedCornerShape(50)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = status.labelAr,
                tint = fg,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = when (status) {
                    SubmissionStatus.PENDING -> "قيد المراجعة ⏳"
                    SubmissionStatus.APPROVED -> "تم الاعتماد ✅"
                    SubmissionStatus.REJECTED -> "مرفوض ❌"
                },
                style = MaterialTheme.typography.labelMedium,
                color = fg
            )
        }
    }
}

@Composable
fun DailyVerseBanner(
    verseText: String,
    reference: String,
    encouragement: String,
    onShare: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (verseText.isBlank()) return
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, WarmGold.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "آية اليوم في طريق المزود",
                    style = MaterialTheme.typography.labelLarge,
                    color = WarmGold
                )
                IconButton(
                    onClick = {
                        onShare("$verseText\n($reference)\n$encouragement\n— تحدي طريق المزود • اجتماع أبو سيفين للشباب الخريجين")
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Share,
                        contentDescription = "مشاركة الآية",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Text(
                text = verseText,
                fontFamily = AmiriFontFamily,
                fontSize = 20.sp,
                lineHeight = 30.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "— $reference",
                style = MaterialTheme.typography.labelMedium,
                color = NativityBurgundy,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            if (encouragement.isNotBlank()) {
                Text(
                    text = encouragement,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskSubmissionBottomSheet(
    task: ChallengeTask,
    initialNote: String = "",
    onDismiss: () -> Unit,
    onConfirmSubmit: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var note by remember(task.id, initialNote) { mutableStateOf(initialNote) }
    val freq = TaskFrequency.fromKey(task.frequency)
    val periodLabel = FastCalendar.periodLabelForTask(freq, FastDate.today())

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MidnightTeal.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = taskIconForKey(task.iconKey),
                        contentDescription = null,
                        tint = MidnightTeal,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "$periodLabel • +${task.points} نقطة بعد الاعتماد",
                        style = MaterialTheme.typography.labelMedium,
                        color = WarmGold
                    )
                }
            }

            if (task.description.isNotBlank()) {
                Text(
                    text = task.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("ملاحظة قصيرة للخادم (اختياري)") },
                placeholder = { Text("مثال: قرأت إنجيل لوقا الأصحاح الأول") },
                maxLines = 3,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("bottom_sheet_note_input")
            )

            Surface(
                color = PendingAmberBg,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "سيتم إرسال الإنجاز بحالة «قيد المراجعة ⏳» وستُضاف ${task.points} نقطة لرصيدك فور اعتماد الخادم.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MidnightTealDark,
                    modifier = Modifier.padding(10.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = { onConfirmSubmit(note) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("bottom_sheet_submit_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MidnightTeal)
            ) {
                Text(
                    text = "تأكيد وإرسال الإنجاز (+${task.points} نقطة)",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
            }
        }
    }
}
