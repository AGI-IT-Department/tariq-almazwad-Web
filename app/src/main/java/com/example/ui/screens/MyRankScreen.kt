package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.FastCalendar
import com.example.ui.state.AppUiState
import com.example.ui.theme.ApprovedEmerald
import com.example.ui.theme.BrightGold
import com.example.ui.theme.MedalBronze
import com.example.ui.theme.MedalGold
import com.example.ui.theme.MedalSilver
import com.example.ui.theme.MidnightTeal
import com.example.ui.theme.MidnightTealDark
import com.example.ui.theme.NativityBurgundy
import com.example.ui.theme.PendingAmber
import com.example.ui.theme.WarmGold

@Composable
fun MyRankScreen(
    uiState: AppUiState,
    modifier: Modifier = Modifier
) {
    val user = uiState.activeUser ?: return
    val entry = uiState.activeUserRankEntry
    val totalParticipants = uiState.leaderboard.size.coerceAtLeast(1)
    val myRank = entry?.rank ?: totalParticipants
    val approvedPoints = entry?.totalApprovedPoints ?: 0
    val pendingPoints = entry?.pendingPoints ?: 0
    val bonusPoints = user.bonusPoints
    val approvedTasksCount = entry?.approvedTasksCount ?: 0

    val isTop10 = myRank in 1..FastCalendar.TOP_PRIZE_RANKS && approvedPoints > 0
    val ranksToTop10 = (myRank - FastCalendar.TOP_PRIZE_RANKS).coerceAtLeast(0)

    val animatedPoints by animateIntAsState(
        targetValue = approvedPoints,
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "approved_points_anim"
    )
    val rankBadgeColor = when (myRank) {
        1 -> MedalGold
        2 -> MedalSilver
        3 -> MedalBronze
        else -> WarmGold
    }

    val fastDay = FastCalendar.currentFastDayIndex()
    val fastProgress by animateFloatAsState(
        targetValue = (fastDay.toFloat() / FastCalendar.FAST_DAYS_COUNT.toFloat()).coerceIn(0.05f, 1f),
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 200f),
        label = "fast_ring_anim"
    )

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Private Personal Rank Crest Card (Hides full leaderboard!)
            item {
                Card(
                    shape = RoundedCornerShape(28.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(MidnightTeal, MidnightTealDark)
                                )
                            )
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                color = WarmGold.copy(alpha = 0.18f),
                                shape = RoundedCornerShape(50),
                                border = BorderStroke(1.dp, BrightGold.copy(alpha = 0.45f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.AutoAwesome,
                                        contentDescription = null,
                                        tint = BrightGold,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = if (FastCalendar.isTrialMode()) {
                                            "الوضع التجريبي 🧪 • الترتيب الرسمي يبدأ 30/11"
                                        } else {
                                            "مركزك الشخصي في تحدي طريق المزود"
                                        },
                                        style = MaterialTheme.typography.labelMedium,
                                        color = BrightGold
                                    )
                                }
                            }

                            // Animated Rank Ring
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.size(152.dp)
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val stroke = 10.dp.toPx()
                                    val inset = stroke / 2f
                                    val arcSize = Size(size.width - stroke, size.height - stroke)
                                    drawArc(
                                        color = Color.White.copy(alpha = 0.14f),
                                        startAngle = -90f,
                                        sweepAngle = 360f,
                                        useCenter = false,
                                        topLeft = Offset(inset, inset),
                                        size = arcSize,
                                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                                    )
                                    drawArc(
                                        brush = Brush.sweepGradient(
                                            colors = listOf(BrightGold, rankBadgeColor, BrightGold)
                                        ),
                                        startAngle = -90f,
                                        sweepAngle = 360f * fastProgress,
                                        useCenter = false,
                                        topLeft = Offset(inset, inset),
                                        size = arcSize,
                                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                                    )
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = if (myRank <= 3) Icons.Filled.WorkspacePremium else Icons.Filled.EmojiEvents,
                                        contentDescription = "مركزك",
                                        tint = rankBadgeColor,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Text(
                                        text = "#$myRank",
                                        style = MaterialTheme.typography.displaySmall,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "من $totalParticipants مشارك",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.78f)
                                    )
                                }
                            }

                            Text(
                                text = user.fullName,
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )

                            if (isTop10) {
                                Surface(
                                    color = NativityBurgundy,
                                    shape = RoundedCornerShape(50),
                                    border = BorderStroke(1.dp, BrightGold.copy(alpha = 0.6f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.CardGiftcard,
                                            contentDescription = null,
                                            tint = BrightGold,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "أنت ضمن أفضل 10 مراكز • مؤهل للجائزة 🎁",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = Color.White
                                        )
                                    }
                                }
                            } else {
                                Surface(
                                    color = Color.White.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Text(
                                        text = if (approvedPoints == 0) {
                                            "سجّل أول مهمة اليوم لتبدأ رحلتك نحو المراكز الـ 10 الأولى! ✨"
                                        } else {
                                            "أنت قريب جداً! باقي $ranksToTop10 مركز لدخول قائمة الجوائز (أفضل 10) 💪"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = BrightGold,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Personal Stats Grid (2x2)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PersonalStatCard(
                        icon = Icons.Filled.CheckCircle,
                        value = "$animatedPoints",
                        label = "نقاطك المعتمدة",
                        accent = ApprovedEmerald,
                        modifier = Modifier.weight(1f)
                    )
                    PersonalStatCard(
                        icon = Icons.Filled.HourglassTop,
                        value = "$pendingPoints",
                        label = "قيد المراجعة ⏳",
                        accent = PendingAmber,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PersonalStatCard(
                        icon = Icons.Filled.Star,
                        value = "$approvedTasksCount",
                        label = "مهام معتمدة",
                        accent = MidnightTeal,
                        modifier = Modifier.weight(1f)
                    )
                    PersonalStatCard(
                        icon = Icons.Filled.CardGiftcard,
                        value = if (bonusPoints >= 0) "+$bonusPoints" else "$bonusPoints",
                        label = "نقاط بونص الخادم",
                        accent = NativityBurgundy,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Spiritual Encouragement Note about Privacy of Leaderboard
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, WarmGold.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "المنافسة الحقيقية هي مع نفسك ✨",
                            style = MaterialTheme.typography.titleMedium,
                            color = MidnightTeal,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "يظهر لك هنا مركزك الشخصي وتقدمك فقط للتركيز على نموك الروحي خلال صوم الميلاد، بينما يتابع خادم الاجتماع لوحة الأبطال الكاملة لإعلان الفائزين الـ 10 في احتفال عيد الميلاد المجيد.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(14.dp)) }
        }
    }
}

@Composable
private fun PersonalStatCard(
    icon: ImageVector,
    value: String,
    label: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = accent,
                    modifier = Modifier.size(21.dp)
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
