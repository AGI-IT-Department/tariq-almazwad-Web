package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
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
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.BrightGold
import com.example.ui.theme.MidnightTeal
import com.example.ui.theme.MidnightTealDark
import com.example.ui.theme.WarmGold

@Composable
fun WelcomeScreen(
    onStartJourney: () -> Unit,
    modifier: Modifier = Modifier
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val contentAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 750, easing = FastOutSlowInEasing),
        label = "welcome_alpha"
    )
    val emblemScale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.86f,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "welcome_emblem_scale"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "nativity_stars")
    val haloAlpha by infiniteTransition.animateFloat(
        initialValue = 0.22f,
        targetValue = 0.46f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "halo_pulse"
    )
    val starTwinkle by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "star_twinkle"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MidnightTealDark,
                        MidnightTeal,
                        MidnightTealDark
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Subtle animated Bethlehem starry backdrop
        Canvas(modifier = Modifier.fillMaxSize()) {
            val starPositions = listOf(
                0.12f to 0.10f,
                0.86f to 0.12f,
                0.22f to 0.24f,
                0.78f to 0.22f,
                0.08f to 0.44f,
                0.92f to 0.48f,
                0.16f to 0.72f,
                0.84f to 0.76f,
                0.50f to 0.07f
            )
            starPositions.forEachIndexed { idx, (fx, fy) ->
                val alpha = if (idx % 2 == 0) starTwinkle else (1.1f - starTwinkle).coerceIn(0.2f, 0.85f)
                val radius = if (idx % 3 == 0) 3.dp.toPx() else 2.dp.toPx()
                drawCircle(
                    color = BrightGold.copy(alpha = alpha * 0.75f),
                    radius = radius,
                    center = Offset(size.width * fx, size.height * fy)
                )
            }
        }

        Column(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 22.dp)
                .alpha(contentAlpha),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Official Church & Meeting Logo Card
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.5.dp, WarmGold.copy(alpha = 0.85f)),
                shadowElevation = 6.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.logo_church_meeting),
                        contentDescription = "شعار أسرة أبو سيفين للشباب الخريجين",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .size(58.dp)
                            .clip(RoundedCornerShape(14.dp))
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "أسرة أبو سيفين للشباب الخريجين",
                            style = MaterialTheme.typography.titleSmall,
                            color = MidnightTealDark,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "كنيسة الشهيد أبي سيفين بالشارقة",
                            style = MaterialTheme.typography.labelMedium,
                            color = WarmGold,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Centerpiece Challenge Emblem with Golden Glow
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(246.dp)
                        .scale(emblemScale)
                ) {
                    Box(
                        modifier = Modifier
                            .size(246.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        BrightGold.copy(alpha = haloAlpha),
                                        WarmGold.copy(alpha = haloAlpha * 0.35f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                    Image(
                        painter = painterResource(id = R.drawable.logo_challenge_emblem),
                        contentDescription = "شعار تحدي طريق المزود صوم الميلاد 2026",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .size(218.dp)
                            .clip(CircleShape)
                    )
                }

                Text(
                    text = "تحدي طريق المزود",
                    fontFamily = AmiriFontFamily,
                    fontSize = 34.sp,
                    lineHeight = 42.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrightGold,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "رحلتك الروحية اليومية في صوم الميلاد المجيد\nمن 30 نوفمبر 2026 حتى 5 يناير 2027",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                // 3 Compact Navy & Gold Highlights
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    WelcomeHighlightChip(
                        icon = Icons.Filled.AutoAwesome,
                        title = "37 يوماً",
                        subtitle = "5 أسابيع صوم",
                        modifier = Modifier.weight(1f)
                    )
                    WelcomeHighlightChip(
                        icon = Icons.Filled.Star,
                        title = "10 مهام",
                        subtitle = "يومية وأسبوعية",
                        modifier = Modifier.weight(1f)
                    )
                    WelcomeHighlightChip(
                        icon = Icons.Filled.CardGiftcard,
                        title = "أول 10",
                        subtitle = "جوائز مميزة",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Primary CTA Button: "ابدأ رحلتك"
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onStartJourney,
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WarmGold,
                        contentColor = MidnightTealDark
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .testTag("welcome_start_journey_button")
                ) {
                    Text(
                        text = "ابدأ رحلتك",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MidnightTealDark
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "ابدأ رحلتك",
                        tint = MidnightTealDark,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Text(
                    text = "«المجد لله في الأعالي، وعلى الأرض السلام، وبالناس المسرة»",
                    fontFamily = AmiriFontFamily,
                    fontSize = 15.sp,
                    color = BrightGold.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun WelcomeHighlightChip(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color.White.copy(alpha = 0.08f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, WarmGold.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = BrightGold,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = BrightGold.copy(alpha = 0.88f),
                textAlign = TextAlign.Center
            )
        }
    }
}
