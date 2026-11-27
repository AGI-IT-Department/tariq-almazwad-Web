package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.BibleVerseItem
import com.example.data.OfficialBibleDayPlan
import com.example.data.WeeklySyllabusItem
import com.example.data.WeeklySyllabusSchedule
import com.example.ui.theme.ApprovedEmerald
import com.example.ui.theme.BrightGold
import com.example.ui.theme.MidnightTeal
import com.example.ui.theme.MidnightTealDark
import com.example.ui.theme.NativityBurgundy
import com.example.ui.theme.PendingAmberBg
import com.example.ui.theme.WarmGold
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/* ---------------- 1. Native Offline PDF Reader ("في تجسد الكلمة") ---------------- */

@Composable
fun InAppBookPdfReaderDialog(
    initialWeekNumber: Int,
    activeWeekNumber: Int,
    points: Int,
    isLocked: Boolean,
    statusLabel: String? = null,
    onDismiss: () -> Unit,
    onLogWeekReading: ((WeeklySyllabusItem) -> Unit)? = null
) {
    val context = LocalContext.current
    var selectedWeekNum by remember { mutableIntStateOf(initialWeekNumber.coerceIn(1, 5)) }
    val selectedWeek = remember(selectedWeekNum) {
        WeeklySyllabusSchedule.weeks.firstOrNull { it.weekNumber == selectedWeekNum }
            ?: WeeklySyllabusSchedule.weeks.first()
    }

    var currentPageOneBased by remember { mutableIntStateOf(selectedWeek.pdfStartPage) }
    var totalPages by remember { mutableIntStateOf(0) }
    var renderer by remember { mutableStateOf<PdfRenderer?>(null) }
    var pfd by remember { mutableStateOf<ParcelFileDescriptor?>(null) }
    var pageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var isRendering by remember { mutableStateOf(true) }

    // Open PDF file from assets once (100% offline)
    LaunchedEffect(Unit) {
        val opened = withContext(Dispatchers.IO) {
            runCatching {
                val cachedFile = File(context.cacheDir, WeeklySyllabusSchedule.INCARNATION_BOOK_ASSET_NAME)
                if (!cachedFile.exists() || cachedFile.length() == 0L) {
                    context.assets.open(WeeklySyllabusSchedule.INCARNATION_BOOK_ASSET_NAME).use { input ->
                        cachedFile.outputStream().use { output -> input.copyTo(output) }
                    }
                }
                val descriptor = ParcelFileDescriptor.open(cachedFile, ParcelFileDescriptor.MODE_READ_ONLY)
                val pdfRenderer = PdfRenderer(descriptor)
                descriptor to pdfRenderer
            }
        }
        opened.onSuccess { (descriptor, pdfRenderer) ->
            pfd = descriptor
            renderer = pdfRenderer
            totalPages = pdfRenderer.pageCount
        }.onFailure {
            loadError = "تعذر فتح ملف الكتاب داخل التطبيق."
            isRendering = false
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            runCatching { renderer?.close() }
            runCatching { pfd?.close() }
        }
    }

    // Render current page whenever page index or renderer changes
    LaunchedEffect(renderer, currentPageOneBased) {
        val activeRenderer = renderer ?: return@LaunchedEffect
        val maxPage = activeRenderer.pageCount
        if (maxPage <= 0) return@LaunchedEffect
        val zeroBased = (currentPageOneBased - 1).coerceIn(0, maxPage - 1)
        isRendering = true
        val bmp = withContext(Dispatchers.IO) {
            runCatching {
                activeRenderer.openPage(zeroBased).use { page ->
                    val targetWidth = (page.width * 2).coerceIn(720, 1440)
                    val targetHeight = (page.height.toFloat() * targetWidth / page.width.toFloat()).toInt().coerceAtLeast(1)
                    val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
                    bitmap.eraseColor(AndroidColor.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    bitmap
                }
            }.getOrNull()
        }
        if (bmp != null) {
            pageBitmap = bmp
            loadError = null
        } else {
            loadError = "تعذر عرض هذه الصفحة."
        }
        isRendering = false
    }

    val isWithinWeekRange =
        currentPageOneBased in selectedWeek.pdfStartPage..selectedWeek.pdfEndPage
    val reachedWeekEnd = currentPageOneBased == selectedWeek.pdfEndPage

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 14.dp),
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.background,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MidnightTeal)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Book,
                                contentDescription = "كتاب في تجسد الكلمة",
                                tint = BrightGold,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "كتاب «في تجسد الكلمة» للقديس أثناسيوس",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "مقسّم حسب خطة الأسابيع الخمسة",
                                style = MaterialTheme.typography.labelSmall,
                                color = BrightGold
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_book_reader_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "إغلاق",
                            tint = Color.White
                        )
                    }
                }

                // 5-Week Selector Chips + Weekly Reading Info
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(WeeklySyllabusSchedule.weeks, key = { it.weekNumber }) { week ->
                            val selected = week.weekNumber == selectedWeek.weekNumber
                            val isCurrent = week.weekNumber == activeWeekNumber
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    selectedWeekNum = week.weekNumber
                                    currentPageOneBased = week.pdfStartPage
                                },
                                label = {
                                    Text(
                                        text = if (isCurrent) "الأسبوع ${week.weekNumber} (الحالي)" else "الأسبوع ${week.weekNumber}",
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NativityBurgundy,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    Surface(
                        color = PendingAmberBg,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, NativityBurgundy.copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${selectedWeek.titleAr} (${selectedWeek.dateRangeAr})",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = NativityBurgundy
                                )
                                Surface(
                                    color = if (reachedWeekEnd) ApprovedEmerald else NativityBurgundy,
                                    shape = RoundedCornerShape(50)
                                ) {
                                    Text(
                                        text = if (reachedWeekEnd) {
                                            "نهاية ورد الأسبوع 🎉"
                                        } else {
                                            "ص $currentPageOneBased • ورد: ${selectedWeek.pdfStartPage} – ${selectedWeek.pdfEndPage}"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = selectedWeek.incarnationBookAr,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                HorizontalDivider()

                // PDF Page Display Area
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color(0xFFECEAE4)),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        loadError != null -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ErrorOutline,
                                    contentDescription = null,
                                    tint = NativityBurgundy,
                                    modifier = Modifier.size(40.dp)
                                )
                                Text(
                                    text = loadError ?: "",
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                        pageBitmap != null -> {
                            val bmp = pageBitmap!!
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Card(
                                    shape = RoundedCornerShape(14.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Image(
                                        bitmap = bmp.asImageBitmap(),
                                        contentDescription = "صفحة $currentPageOneBased من كتاب في تجسد الكلمة",
                                        contentScale = ContentScale.FillWidth,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                if (!isWithinWeekRange && totalPages > 0) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedButton(
                                        onClick = { currentPageOneBased = selectedWeek.pdfStartPage },
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("العودة لبداية ورد ${selectedWeek.titleAr} (ص ${selectedWeek.pdfStartPage})")
                                    }
                                }
                            }
                            if (isRendering) {
                                CircularProgressIndicator(
                                    color = MidnightTeal,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                        else -> {
                            CircularProgressIndicator(color = MidnightTeal)
                        }
                    }
                }

                HorizontalDivider()

                // Page Controls + Log Reading CTA
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { if (currentPageOneBased > 1) currentPageOneBased-- },
                            enabled = currentPageOneBased > 1,
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "الصفحة السابقة",
                                modifier = Modifier.size(18.dp)
                            )
                            Text("السابقة", style = MaterialTheme.typography.labelMedium)
                        }

                        Text(
                            text = if (totalPages > 0) "صفحة $currentPageOneBased من $totalPages" else "صفحة $currentPageOneBased",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MidnightTealDark
                        )

                        OutlinedButton(
                            onClick = {
                                if (totalPages == 0 || currentPageOneBased < totalPages) {
                                    currentPageOneBased++
                                }
                            },
                            enabled = totalPages == 0 || currentPageOneBased < totalPages,
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("التالية", style = MaterialTheme.typography.labelMedium)
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = "الصفحة التالية",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    if (onLogWeekReading != null) {
                        Button(
                            onClick = { onLogWeekReading(selectedWeek) },
                            enabled = !isLocked,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MidnightTeal,
                                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("log_book_reading_from_reader")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isLocked) {
                                    statusLabel ?: "تم تسجيل قراءة هذا الأسبوع"
                                } else {
                                    "سجّل قراءة ورد الأسبوع (+$points نقطة)"
                                },
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
        }
    }
}

/* ---------------- 2. Native In-App Daily Bible Chapter Reader ---------------- */

@Composable
fun InAppBibleChapterReaderDialog(
    initialDayNumber: Int,
    todayDayNumber: Int,
    points: Int,
    isLocked: Boolean,
    statusLabel: String? = null,
    onDismiss: () -> Unit,
    onLogBibleReading: ((OfficialBibleDayPlan) -> Unit)? = null
) {
    val allDays = remember { WeeklySyllabusSchedule.officialDailyBiblePlan }
    var selectedDayNum by remember { mutableIntStateOf(initialDayNumber.coerceIn(1, allDays.size)) }
    var fontSizeSp by remember { mutableIntStateOf(19) }
    var verses by remember { mutableStateOf<List<BibleVerseItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var reloadTick by remember { mutableIntStateOf(0) }

    val selectedDay = remember(selectedDayNum) {
        allDays.firstOrNull { it.dayNumber == selectedDayNum } ?: allDays.first()
    }
    val weekDays = remember(selectedDay.weekNumber) {
        allDays.filter { it.weekNumber == selectedDay.weekNumber }
    }
    val selectedWeekSyllabus = remember(selectedDay.weekNumber) {
        WeeklySyllabusSchedule.weeks.firstOrNull { it.weekNumber == selectedDay.weekNumber }
            ?: WeeklySyllabusSchedule.weeks.first()
    }

    LaunchedEffect(selectedDay.bookId, selectedDay.chapters, reloadTick) {
        isLoading = true
        errorMsg = null
        val res = WeeklySyllabusSchedule.fetchArabicBibleChapters(
            bookId = selectedDay.bookId,
            chapters = selectedDay.chapters
        )
        res.onSuccess { loaded ->
            verses = loaded
            isLoading = false
        }.onFailure {
            errorMsg = "تعذر تحميل نص الإصحاح حالياً. تأكد من اتصالك بالإنترنت ثم أعد المحاولة."
            isLoading = false
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 14.dp),
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.background,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MidnightTeal)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.MenuBook,
                                contentDescription = "الكتاب المقدس",
                                tint = BrightGold,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = selectedDay.fullLabelAr,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "اليوم ${selectedDay.dayNumber} (${selectedDay.dateLabelAr}) • ${selectedWeekSyllabus.titleAr}",
                                style = MaterialTheme.typography.labelSmall,
                                color = BrightGold
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { fontSizeSp = (fontSizeSp - 2).coerceAtLeast(15) }) {
                            Icon(
                                imageVector = Icons.Filled.Remove,
                                contentDescription = "تصغير الخط",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(onClick = { fontSizeSp = (fontSizeSp + 2).coerceAtMost(27) }) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = "تكبير الخط",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("close_bible_reader_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "إغلاق",
                                tint = Color.White
                            )
                        }
                    }
                }

                // Week & Day Selectors
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(WeeklySyllabusSchedule.weeks, key = { it.weekNumber }) { w ->
                            FilterChip(
                                selected = w.weekNumber == selectedDay.weekNumber,
                                onClick = {
                                    allDays.firstOrNull { it.weekNumber == w.weekNumber }?.let {
                                        selectedDayNum = it.dayNumber
                                    }
                                },
                                label = { Text("الأسبوع ${w.weekNumber}", style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MidnightTeal,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(weekDays, key = { it.dayNumber }) { day ->
                            val isSelected = day.dayNumber == selectedDay.dayNumber
                            val isToday = day.dayNumber == todayDayNumber
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedDayNum = day.dayNumber },
                                label = {
                                    Text(
                                        text = "${day.shortLabelAr} (${if (isToday) "اليوم" else day.dateLabelAr})",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PendingAmberBg,
                                    selectedLabelColor = MidnightTealDark
                                )
                            )
                        }
                    }
                }

                HorizontalDivider()

                // Verses Body
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        isLoading -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CircularProgressIndicator(color = MidnightTeal)
                                Text(
                                    text = "جارٍ تحميل ${selectedDay.fullLabelAr}...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        errorMsg != null -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.padding(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ErrorOutline,
                                    contentDescription = null,
                                    tint = NativityBurgundy,
                                    modifier = Modifier.size(40.dp)
                                )
                                Text(
                                    text = errorMsg ?: "",
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center
                                )
                                Button(
                                    onClick = { reloadTick++ },
                                    colors = ButtonDefaults.buttonColors(containerColor = MidnightTeal),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Refresh,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("إعادة المحاولة")
                                }
                            }
                        }
                        else -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                item {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 6.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "الكتاب المقدس — ترجمة فانديك",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = WarmGold,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = selectedDay.fullLabelAr,
                                            style = MaterialTheme.typography.titleLarge,
                                            color = MidnightTeal,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                itemsIndexed(verses, key = { _, v -> "${v.chapter}_${v.verse}" }) { idx, verse ->
                                    val showChapterDivider =
                                        selectedDay.chapters.size > 1 &&
                                            (idx == 0 || verses[idx - 1].chapter != verse.chapter)
                                    if (showChapterDivider) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 6.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Surface(
                                                color = MidnightTeal.copy(alpha = 0.1f),
                                                shape = RoundedCornerShape(50)
                                            ) {
                                                Text(
                                                    text = "${selectedDay.bookNameAr} — الإصحاح ${verse.chapter}",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MidnightTeal,
                                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Surface(
                                            color = PendingAmberBg,
                                            shape = CircleShape,
                                            modifier = Modifier.padding(top = 4.dp)
                                        ) {
                                            Text(
                                                text = "${verse.verse}",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = WarmGold,
                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                            )
                                        }
                                        Text(
                                            text = verse.text,
                                            fontSize = fontSizeSp.sp,
                                            lineHeight = (fontSizeSp * 1.65f).sp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider()

                // Footer Controls
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { if (selectedDayNum > 1) selectedDayNum-- },
                            enabled = selectedDayNum > 1,
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "الإصحاح السابق",
                                modifier = Modifier.size(18.dp)
                            )
                            Text("السابق", style = MaterialTheme.typography.labelMedium)
                        }

                        Text(
                            text = "اليوم $selectedDayNum من ${allDays.size}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MidnightTealDark
                        )

                        OutlinedButton(
                            onClick = { if (selectedDayNum < allDays.size) selectedDayNum++ },
                            enabled = selectedDayNum < allDays.size,
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("التالي", style = MaterialTheme.typography.labelMedium)
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = "الإصحاح التالي",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    if (onLogBibleReading != null) {
                        Button(
                            onClick = { onLogBibleReading(selectedDay) },
                            enabled = !isLocked,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MidnightTeal,
                                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("log_bible_reading_from_reader")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isLocked) {
                                    statusLabel ?: "تم تسجيل قراءة اليوم"
                                } else {
                                    "سجّل قراءة الإصحاح (+$points نقطة)"
                                },
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
        }
    }
}
