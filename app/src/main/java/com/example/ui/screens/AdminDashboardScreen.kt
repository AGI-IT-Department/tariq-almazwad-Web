package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.AppConfigEntity
import com.example.data.AutoContentBank
import com.example.data.AutoWeeklyQuiz
import com.example.data.ChallengeTask
import com.example.data.FastCalendar
import com.example.data.LeaderboardEntry
import com.example.data.OfficialReadingPlan
import com.example.data.TaskFrequency
import com.example.data.TaskSubmission
import com.example.ui.components.AutoWeeklyQuizDialog
import com.example.ui.components.formatTimestampArabic
import com.example.ui.components.openWhatsAppChat
import com.example.ui.state.AppUiState
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.ApprovedEmerald
import com.example.ui.theme.BrightGold
import com.example.ui.theme.MedalBronze
import com.example.ui.theme.MedalGold
import com.example.ui.theme.MedalSilver
import com.example.ui.theme.MidnightTeal
import com.example.ui.theme.MidnightTealDark
import com.example.ui.theme.NativityBurgundy
import com.example.ui.theme.RejectedRed
import com.example.ui.theme.WarmGold
import com.example.ui.theme.WhatsAppGreen

@Composable
fun AdminDashboardScreen(
    uiState: AppUiState,
    onUnlock: (String, (String) -> Unit) -> Unit,
    onClose: () -> Unit,
    onLock: () -> Unit,
    onReviewSubmission: (String, Boolean, String) -> Unit,
    onApproveAllPending: () -> Unit,
    onAdjustBonus: (String, Int, String) -> Unit,
    onAdminAddUser: (fullName: String, phone: String, pin: String, initialBonus: Int) -> Unit,
    onAdminUpdateUser: (userId: String, fullName: String, phone: String, newPinOptional: String) -> Unit,
    onAdminDeleteUser: (userId: String) -> Unit,
    onUpdateTask: (ChallengeTask) -> Unit,
    onUpdateConfig: (AppConfigEntity) -> Unit,
    onToggleQuizWeek: (Int, Boolean) -> Unit,
    onExportCsv: () -> Unit,
    onChangeAdminPin: (String, String) -> Unit,
    onFeedback: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onClose() }

    if (!uiState.isAdminUnlocked) {
        AdminPinGate(
            onUnlock = onUnlock,
            onBack = onClose,
            modifier = modifier
        )
        return
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        "طلبات الاعتماد (${uiState.pendingSubmissions.size})",
        "لوحة الأبطال والشباب (${uiState.allUsers.size})",
        "المهام والمسابقة",
        "التصدير والإعدادات"
    )

    // Dialog states
    var rejectingSubmission by remember { mutableStateOf<TaskSubmission?>(null) }
    var bonusTargetEntry by remember { mutableStateOf<LeaderboardEntry?>(null) }
    var editingUserEntry by remember { mutableStateOf<LeaderboardEntry?>(null) }
    var deletingUserEntry by remember { mutableStateOf<LeaderboardEntry?>(null) }
    var showAddUserDialog by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<ChallengeTask?>(null) }

    // 1) Add New Youth Dialog
    if (showAddUserDialog) {
        var name by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        var pin by remember { mutableStateOf("") }
        var bonusStr by remember { mutableStateOf("0") }
        AlertDialog(
            onDismissRequest = { showAddUserDialog = false },
            title = { Text("إضافة مشارك جديد") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("الاسم بالكامل *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("admin_add_user_name")
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("رقم الواتساب / الموبايل") },
                        placeholder = { Text("مثال: 0501234567") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("admin_add_user_phone")
                    )
                    OutlinedTextField(
                        value = pin,
                        onValueChange = { pin = it.filter { c -> c.isDigit() }.take(4) },
                        label = { Text("الرقم السري (4 أرقام PIN) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("admin_add_user_pin")
                    )
                    OutlinedTextField(
                        value = bonusStr,
                        onValueChange = { bonusStr = it.filter { c -> c.isDigit() || c == '-' } },
                        label = { Text("نقاط بداية / بونص (اختياري)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val bonus = bonusStr.toIntOrNull() ?: 0
                        onAdminAddUser(name, phone, pin, bonus)
                        showAddUserDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MidnightTeal),
                    modifier = Modifier.testTag("admin_add_user_confirm")
                ) {
                    Text("إضافة المشارك", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddUserDialog = false }) { Text("إلغاء") }
            }
        )
    }

    // 2) Edit User Info Dialog (Name + WhatsApp Phone + Optional PIN Reset)
    editingUserEntry?.let { entry ->
        var name by remember(entry) { mutableStateOf(entry.user.fullName) }
        var phone by remember(entry) { mutableStateOf(entry.user.phone) }
        var newPin by remember(entry) { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { editingUserEntry = null },
            title = { Text("تعديل بيانات المشارك") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("الاسم بالكامل *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("رقم الواتساب / الموبايل") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newPin,
                        onValueChange = { newPin = it.filter { c -> c.isDigit() }.take(4) },
                        label = { Text("رقم سري جديد (اختياري - اتركه فارغاً للإبقاء)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onAdminUpdateUser(entry.user.id, name, phone, newPin)
                        editingUserEntry = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MidnightTeal)
                ) {
                    Text("حفظ التعديلات", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingUserEntry = null }) { Text("إلغاء") }
            }
        )
    }

    // 3) Delete User Confirmation Dialog
    deletingUserEntry?.let { entry ->
        AlertDialog(
            onDismissRequest = { deletingUserEntry = null },
            title = { Text("حذف المشارك نهائياً") },
            text = {
                Text(
                    text = "هل أنت متأكد من حذف «${entry.user.fullName}» وجميع مشاركاته ونقاطه (${entry.totalApprovedPoints} نقطة)؟ لا يمكن التراجع عن هذا الإجراء.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onAdminDeleteUser(entry.user.id)
                        deletingUserEntry = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RejectedRed)
                ) {
                    Text("نعم، احذف", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingUserEntry = null }) { Text("إلغاء") }
            }
        )
    }

    // 4) Reject Reason Dialog
    rejectingSubmission?.let { sub ->
        var reason by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { rejectingSubmission = null },
            title = { Text("رفض مشاركة ${sub.userName}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "المهمة: ${sub.taskTitle} (+${sub.points} نقطة)",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        label = { Text("سبب الرفض (اختياري)") },
                        placeholder = { Text("مثال: يرجى التأكد من إتمام المهمة وإعادة التسجيل") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onReviewSubmission(sub.id, false, reason)
                        rejectingSubmission = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RejectedRed)
                ) {
                    Text("تأكيد الرفض", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { rejectingSubmission = null }) { Text("إلغاء") }
            }
        )
    }

    // 5) Manual Bonus / Deduction Dialog
    bonusTargetEntry?.let { entry ->
        var deltaText by remember { mutableStateOf("100") }
        var reasonText by remember { mutableStateOf("فائز في المسابقة الأسبوعية") }
        AlertDialog(
            onDismissRequest = { bonusTargetEntry = null },
            title = { Text("إضافة / خصم نقاط • ${entry.user.fullName}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "استخدم رقم موجب للإضافة (مثل 100) أو رقم سالب للخصم (مثل -50).",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = deltaText,
                        onValueChange = { deltaText = it },
                        label = { Text("عدد النقاط (+ أو -)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = reasonText,
                        onValueChange = { reasonText = it },
                        label = { Text("السبب / الملاحظة") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val delta = deltaText.trim().toIntOrNull() ?: 0
                        onAdjustBonus(entry.user.id, delta, reasonText)
                        bonusTargetEntry = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MidnightTeal)
                ) {
                    Text("حفظ التعديل", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { bonusTargetEntry = null }) { Text("إلغاء") }
            }
        )
    }

    // 6) Edit Task Dialog
    editingTask?.let { task ->
        var title by remember { mutableStateOf(task.title) }
        var pointsStr by remember { mutableStateOf(task.points.toString()) }
        AlertDialog(
            onDismissRequest = { editingTask = null },
            title = { Text("تعديل المهمة والنقاط") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("اسم المهمة") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = pointsStr,
                        onValueChange = { pointsStr = it.filter { c -> c.isDigit() } },
                        label = { Text("قيمة النقاط") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val pts = pointsStr.toIntOrNull()?.coerceIn(10, 2000) ?: task.points
                        onUpdateTask(task.copy(title = title.trim().ifEmpty { task.title }, points = pts))
                        editingTask = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MidnightTeal)
                ) {
                    Text("حفظ", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingTask = null }) { Text("إلغاء") }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        // Admin Header
        Surface(color = MidnightTeal, shadowElevation = 4.dp) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.testTag("admin_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع",
                            tint = Color.White
                        )
                    }
                    Column {
                        Text(
                            text = "لوحة تحكم خدام التحدي",
                            style = MaterialTheme.typography.titleMedium,
                            color = BrightGold,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "لوحة الأبطال الحصرية وإدارة الشباب والموافقات",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }
                TextButton(onClick = onLock) {
                    Icon(Icons.Filled.Lock, contentDescription = null, tint = BrightGold, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("قفل", color = Color.White)
                }
            }
        }

        // 4 Admin Tabs
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            edgePadding = 12.dp
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    selectedContentColor = MaterialTheme.colorScheme.primary,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    text = { Text(title, style = MaterialTheme.typography.labelLarge) },
                    modifier = Modifier.testTag("admin_tab_$index")
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            contentAlignment = Alignment.TopCenter
        ) {
            when (selectedTab) {
                0 -> AdminPendingQueueTab(
                    pending = uiState.pendingSubmissions,
                    onApprove = { sub -> onReviewSubmission(sub.id, true, "") },
                    onReject = { sub -> rejectingSubmission = sub },
                    onApproveAll = onApproveAllPending
                )
                1 -> AdminLeaderboardAndUsersTab(
                    entries = uiState.leaderboard,
                    onAddUserClick = { showAddUserDialog = true },
                    onBonusClick = { bonusTargetEntry = it },
                    onEditUserClick = { editingUserEntry = it },
                    onDeleteUserClick = { deletingUserEntry = it },
                    onNoPhoneFeedback = {
                        onFeedback("لا يوجد رقم واتساب مسجل لهذا المشارك — اضغط «تعديل» لإضافته")
                    }
                )
                2 -> AdminTasksAndQuizTab(
                    tasks = uiState.allTasks,
                    config = uiState.appConfig,
                    onToggleTask = { task, active -> onUpdateTask(task.copy(isActive = active)) },
                    onEditTask = { editingTask = it },
                    onToggleQuizWeek = onToggleQuizWeek
                )
                3 -> AdminExportAndSetupTab(
                    usersCount = uiState.allUsers.size,
                    approvedCount = uiState.allSubmissions.count { it.status == "approved" },
                    onExportCsv = onExportCsv,
                    onChangeAdminPin = onChangeAdminPin
                )
            }
        }
    }
}

@Composable
private fun AdminPinGate(
    onUnlock: (String, (String) -> Unit) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var pin by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.2.dp, WarmGold.copy(alpha = 0.6f)),
            modifier = Modifier
                .widthIn(max = 440.dp)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.AdminPanelSettings,
                    contentDescription = "دخول خدام التحدي",
                    tint = NativityBurgundy,
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    text = "دخول خدام التحدي",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MidnightTeal
                )
                Text(
                    text = "هذه اللوحة خاصة بخدام التحدي فقط — أدخل كلمة مرور الأدمن للدخول",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = pin,
                    onValueChange = {
                        pin = it
                        errorMsg = null
                    },
                    label = { Text("كلمة مرور الأدمن") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    isError = errorMsg != null,
                    supportingText = errorMsg?.let { msg -> { Text(msg) } },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_pin_input")
                )
                Button(
                    onClick = {
                        onUnlock(pin) { msg -> errorMsg = msg }
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MidnightTeal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("admin_unlock_button")
                ) {
                    Text("دخول لوحة التحكم", color = Color.White)
                }
                TextButton(onClick = onBack) {
                    Text("العودة للتطبيق")
                }
            }
        }
    }
}

@Composable
private fun AdminPendingQueueTab(
    pending: List<TaskSubmission>,
    onApprove: (TaskSubmission) -> Unit,
    onReject: (TaskSubmission) -> Unit,
    onApproveAll: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .widthIn(max = 640.dp)
            .fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (pending.isNotEmpty()) {
            item {
                Button(
                    onClick = onApproveAll,
                    colors = ButtonDefaults.buttonColors(containerColor = ApprovedEmerald),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("admin_approve_all_button")
                ) {
                    Icon(Icons.Filled.DoneAll, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "موافقة على الكل (${pending.size} طلب)",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White
                    )
                }
            }
        }

        if (pending.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = ApprovedEmerald,
                            modifier = Modifier.size(44.dp)
                        )
                        Text("لا توجد طلبات معلقة حالياً", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "كل مشاركات الشباب تمت مراجعتها بنجاح!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(pending, key = { it.id }) { sub ->
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, WarmGold.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = sub.userName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MidnightTeal
                            )
                            Surface(
                                color = WarmGold.copy(alpha = 0.18f),
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = "+${sub.points} نقطة",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = WarmGold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                        Text(text = sub.taskTitle, style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = "التاريخ: ${formatTimestampArabic(sub.submittedAt)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (sub.note.isNotBlank()) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "ملاحظة الشاب: ${sub.note}",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { onApprove(sub) },
                                colors = ButtonDefaults.buttonColors(containerColor = ApprovedEmerald),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("approve_sub_${sub.id}")
                            ) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("موافقة ✅", color = Color.White)
                            }
                            OutlinedButton(
                                onClick = { onReject(sub) },
                                border = BorderStroke(1.2.dp, RejectedRed),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("reject_sub_${sub.id}")
                            ) {
                                Icon(Icons.Filled.Cancel, contentDescription = null, tint = RejectedRed, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("رفض ❌", color = RejectedRed)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminLeaderboardAndUsersTab(
    entries: List<LeaderboardEntry>,
    onAddUserClick: () -> Unit,
    onBonusClick: (LeaderboardEntry) -> Unit,
    onEditUserClick: (LeaderboardEntry) -> Unit,
    onDeleteUserClick: (LeaderboardEntry) -> Unit,
    onNoPhoneFeedback: () -> Unit
) {
    val context = LocalContext.current
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val pointsTextColor = if (isDark) BrightGold else MidnightTeal
    val prizeAccentColor = if (isDark) Color(0xFFF3A7B4) else NativityBurgundy
    val deleteAccentColor = if (isDark) Color(0xFFFF8A96) else RejectedRed

    var query by remember { mutableStateOf("") }
    var expandedUserId by remember { mutableStateOf<String?>(null) }

    val filtered = remember(entries, query) {
        if (query.isBlank()) entries
        else entries.filter {
            it.user.fullName.contains(query.trim(), ignoreCase = true) ||
                it.user.phone.contains(query.trim())
        }
    }

    LazyColumn(
        modifier = Modifier
            .widthIn(max = 640.dp)
            .fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Exclusive Admin Leaderboard Header + Add User Button
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MidnightTeal),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "لوحة الأبطال الحصرية وإدارة الشباب 🏆",
                        style = MaterialTheme.typography.titleMedium,
                        color = BrightGold,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (FastCalendar.isTrialMode()) {
                            "الوضع التجريبي مفعل حالياً 🧪 • اضغط على أي اسم لفتح التفاصيل والإجراءات."
                        } else {
                            "الترتيب الرسمي الحصري (30/11/2026 — 5/1/2027) • اضغط على أي اسم لفتح التفاصيل."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.88f)
                    )
                    Button(
                        onClick = onAddUserClick,
                        colors = ButtonDefaults.buttonColors(containerColor = WarmGold),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("admin_add_new_user_button")
                    ) {
                        Icon(Icons.Filled.PersonAdd, contentDescription = null, tint = MidnightTealDark)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "إضافة مشارك جديد +",
                            style = MaterialTheme.typography.titleSmall,
                            color = MidnightTealDark,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("بحث بالاسم أو رقم الموبايل...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_search_users_input")
            )
        }

        if (filtered.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "لا يوجد مشاركون مطابقون للبحث.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(20.dp)
                    )
                }
            }
        } else {
            items(filtered, key = { it.user.id }) { entry ->
                val isTop10 = entry.rank in 1..FastCalendar.TOP_PRIZE_RANKS
                val isExpanded = expandedUserId == entry.user.id
                val medalColor = when (entry.rank) {
                    1 -> MedalGold
                    2 -> MedalSilver
                    3 -> MedalBronze
                    else -> if (isTop10) MidnightTeal else MaterialTheme.colorScheme.outline
                }

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(
                        width = if (isExpanded) 1.5.dp else if (entry.rank <= 3) 1.2.dp else 0.8.dp,
                        color = if (isExpanded) {
                            WarmGold
                        } else if (entry.rank <= 3) {
                            medalColor
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        }
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (isExpanded) 3.dp else 1.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable {
                            expandedUserId = if (isExpanded) null else entry.user.id
                        }
                        .testTag("admin_user_row_${entry.user.id}")
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Compact Single-Line Summary Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(medalColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "#${entry.rank}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Text(
                                text = entry.user.fullName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )

                            Surface(
                                color = WarmGold.copy(alpha = if (isDark) 0.22f else 0.18f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "${entry.totalApprovedPoints} نقطة",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = pointsTextColor,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            Icon(
                                imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                contentDescription = if (isExpanded) "إخفاء التفاصيل" else "عرض التفاصيل",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Expandable Details & Actions
                        AnimatedVisibility(visible = isExpanded) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            text = if (entry.user.phone.isNotBlank()) "📱 ${entry.user.phone}" else "بدون رقم واتساب",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "بونص: ${entry.user.bonusPoints} • معلق: ${entry.pendingPoints} • مهام معتمدة: ${entry.approvedTasksCount}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    if (isTop10 && entry.totalApprovedPoints > 0) {
                                        Surface(
                                            color = prizeAccentColor.copy(alpha = 0.14f),
                                            shape = RoundedCornerShape(50)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.CardGiftcard,
                                                    contentDescription = null,
                                                    tint = prizeAccentColor,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Text(
                                                    text = "مؤهل للجائزة",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = prizeAccentColor,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }

                                // Action Row 1: Direct WhatsApp + Bonus/Deduction
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            if (entry.user.phone.isBlank()) {
                                                onNoPhoneFeedback()
                                            } else {
                                                openWhatsAppChat(
                                                    context,
                                                    entry.user.phone,
                                                    "سلام ونعمة يا ${entry.user.fullName} ✨\nربنا يبارك جهادك في تحدي طريق المزود (مركزك الحالي #${entry.rank} برصيد ${entry.totalApprovedPoints} نقطة) — اجتماع أبو سيفين للشباب الخريجين"
                                                )
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                            .testTag("admin_whatsapp_${entry.user.id}")
                                    ) {
                                        Icon(Icons.Filled.Chat, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("واتساب مباشر", style = MaterialTheme.typography.labelMedium, color = Color.White)
                                    }

                                    Button(
                                        onClick = { onBonusClick(entry) },
                                        colors = ButtonDefaults.buttonColors(containerColor = MidnightTeal),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                            .testTag("admin_bonus_${entry.user.id}")
                                    ) {
                                        Icon(Icons.Filled.AddCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("إضافة / خصم", style = MaterialTheme.typography.labelMedium, color = Color.White)
                                    }
                                }

                                // Action Row 2: Edit Info + Delete User
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { onEditUserClick(entry) },
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                            .testTag("admin_edit_user_${entry.user.id}")
                                    ) {
                                        Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("تعديل البيانات / PIN", style = MaterialTheme.typography.labelMedium)
                                    }

                                    OutlinedButton(
                                        onClick = { onDeleteUserClick(entry) },
                                        border = BorderStroke(1.dp, deleteAccentColor.copy(alpha = 0.7f)),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                        modifier = Modifier
                                            .weight(0.7f)
                                            .height(44.dp)
                                            .testTag("admin_delete_user_${entry.user.id}")
                                    ) {
                                        Icon(Icons.Filled.Delete, contentDescription = null, tint = deleteAccentColor, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("حذف", style = MaterialTheme.typography.labelMedium, color = deleteAccentColor)
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

@Composable
private fun AdminTasksAndQuizTab(
    tasks: List<ChallengeTask>,
    config: AppConfigEntity,
    onToggleTask: (ChallengeTask, Boolean) -> Unit,
    onEditTask: (ChallengeTask) -> Unit,
    onToggleQuizWeek: (Int, Boolean) -> Unit
) {
    val context = LocalContext.current
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val accentTeal = if (isDark) BrightGold else MidnightTeal
    val accentBurgundy = if (isDark) Color(0xFFF3A7B4) else NativityBurgundy

    val allQuizzes = remember { AutoContentBank.getAllAutoWeeklyQuizzes(context) }
    val enabledWeeks = remember(config.weeklyQuizQuestion) {
        OfficialReadingPlan.getEnabledQuizWeeks(config)
    }
    val todayDevotion = remember { AutoContentBank.getTodayAutoDevotion(context) }
    var previewQuiz by remember { mutableStateOf<AutoWeeklyQuiz?>(null) }

    previewQuiz?.let { quiz ->
        AutoWeeklyQuizDialog(
            quiz = quiz,
            userId = "admin_preview",
            adminPreview = true,
            onDismiss = { previewQuiz = null }
        )
    }

    LazyColumn(
        modifier = Modifier
            .widthIn(max = 640.dp)
            .fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Automatic Weekly Quizzes Control Panel
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.2.dp, WarmGold.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = null,
                                tint = WarmGold,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "المسابقات الأسبوعية الأوتوماتيكية",
                                style = MaterialTheme.typography.titleMedium,
                                color = accentTeal,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        Surface(
                            color = accentTeal.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = "مفعّل: ${enabledWeeks.size} / ${allQuizzes.size}",
                                style = MaterialTheme.typography.labelSmall,
                                color = accentTeal,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Text(
                        text = "جميع المسابقات جاهزة تلقائياً (100 سؤال لكل أسبوع: 40 اختيار من متعدد + 40 صح وخطأ + 20 توصيل × 2 نقطة = 200 نقطة). فعّل أي أسبوع ليظهر فوراً للمشاركين وتُصحّح إجاباتهم وتُضاف لرصيدهم مباشرة.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    allQuizzes.forEach { quiz ->
                        val isEnabled = enabledWeeks.contains(quiz.weekNumber)
                        val matchCount = quiz.match.sumOf { it.pairs.size }
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isEnabled) {
                                ApprovedEmerald.copy(alpha = if (isDark) 0.16f else 0.08f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                            },
                            border = BorderStroke(
                                1.dp,
                                if (isEnabled) ApprovedEmerald.copy(alpha = 0.55f)
                                else MaterialTheme.colorScheme.outlineVariant
                            ),
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
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = quiz.titleAr,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Surface(
                                                color = if (isEnabled) ApprovedEmerald else MaterialTheme.colorScheme.surfaceVariant,
                                                shape = RoundedCornerShape(50)
                                            ) {
                                                Text(
                                                    text = if (isEnabled) "مفعّلة ✓" else "موقوفة",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (isEnabled) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = quiz.subtitleAr,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${quiz.mcq.size} اختيار • ${quiz.tf.size} صح وخطأ • $matchCount توصيل = ${quiz.totalItems} سؤال (${quiz.maxPoints} نقطة)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = accentBurgundy,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Switch(
                                        checked = isEnabled,
                                        onCheckedChange = { checked ->
                                            onToggleQuizWeek(quiz.weekNumber, checked)
                                        },
                                        modifier = Modifier.testTag("admin_toggle_quiz_week_${quiz.weekNumber}")
                                    )
                                }

                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (isEnabled) {
                                            "ظاهرة للشباب • التصحيح فوري"
                                        } else {
                                            "مخفية حتى التفعيل"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedButton(
                                        onClick = { previewQuiz = quiz },
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, accentTeal.copy(alpha = 0.45f)),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        modifier = Modifier.testTag("admin_preview_quiz_week_${quiz.weekNumber}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Visibility,
                                            contentDescription = null,
                                            tint = accentTeal,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "معاينة الأسئلة",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = accentTeal,
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

        // Automatic Daily Verse + Coptic Orthodox Church Fathers Quote Live Preview
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(MidnightTealDark, MidnightTeal, MidnightTealDark)
                            )
                        )
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.MenuBook,
                                contentDescription = null,
                                tint = BrightGold,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "آية اليوم وأقوال آباء الكنيسة (تلقائي 100%)",
                                style = MaterialTheme.typography.titleSmall,
                                color = BrightGold,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        Surface(
                            color = Color.White.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = "يتجدد يومياً ✨",
                                style = MaterialTheme.typography.labelSmall,
                                color = BrightGold,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Text(
                        text = "تُختار آية اليوم تلقائياً من منهج الإنجيل اليومي مع قول لآباء الكنيسة القبطية الأرثوذكسية عن صوم الميلاد والتجسد، وتظهر فوراً للشباب وفي إشعارات التشجيع.",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.85f)
                    )

                    Surface(
                        color = Color.White.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, BrightGold.copy(alpha = 0.35f)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                color = WarmGold.copy(alpha = 0.25f),
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = "منهج اليوم: ${todayDevotion.chapterLabel}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = BrightGold,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                            Text(
                                text = todayDevotion.verseText,
                                fontFamily = AmiriFontFamily,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "— ${todayDevotion.verseRef}",
                                style = MaterialTheme.typography.labelMedium,
                                color = BrightGold,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                color = Color.Black.copy(alpha = 0.22f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "«${todayDevotion.fatherQuote}»",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.95f)
                                    )
                                    Text(
                                        text = "— ${todayDevotion.fatherName}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = BrightGold,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "إدارة المهام الـ ${tasks.size} وقيمة النقاط",
                style = MaterialTheme.typography.titleMedium,
                color = accentTeal,
                fontWeight = FontWeight.Bold
            )
        }

        items(tasks, key = { it.id }) { task ->
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(task.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(
                            text = "${TaskFrequency.fromKey(task.frequency).labelAr} • ${task.points} نقطة",
                            style = MaterialTheme.typography.bodySmall,
                            color = WarmGold
                        )
                    }
                    TextButton(onClick = { onEditTask(task) }) {
                        Text("تعديل")
                    }
                    Switch(
                        checked = task.isActive,
                        onCheckedChange = { onToggleTask(task, it) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AdminExportAndSetupTab(
    usersCount: Int,
    approvedCount: Int,
    onExportCsv: () -> Unit,
    onChangeAdminPin: (String, String) -> Unit
) {
    var currentPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .widthIn(max = 640.dp)
            .fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.2.dp, WarmGold.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "تصدير التقرير النهائي لجوائز عيد الميلاد 🎁",
                        style = MaterialTheme.typography.titleMedium,
                        color = MidnightTeal,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "عدد المشاركين: $usersCount • المهام المعتمدة: $approvedCount.\nيُصدّر ملف Excel (.xlsx) منسّق بالألوان والفلاتر ومؤشر النقاط جاهز للفتح في Microsoft Excel باللغة العربية.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = onExportCsv,
                        colors = ButtonDefaults.buttonColors(containerColor = NativityBurgundy),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("admin_export_csv_button")
                    ) {
                        Icon(Icons.Filled.Download, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("تصدير لوحة الأبطال والنقاط (Excel منسّق)", color = Color.White)
                    }
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "تغيير كلمة مرور الأدمن",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "يتم تحديث كلمة المرور مباشرة في قاعدة البيانات وتُطبّق فوراً على جميع الأجهزة.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = currentPin,
                        onValueChange = { currentPin = it },
                        label = { Text("كلمة المرور الحالية") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newPin,
                        onValueChange = { newPin = it },
                        label = { Text("كلمة المرور الجديدة (4 أحرف أو أرقام فأكثر)") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            onChangeAdminPin(currentPin, newPin)
                            currentPin = ""
                            newPin = ""
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MidnightTeal),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("تحديث كلمة مرور الأدمن", color = Color.White)
                    }
                }
            }
        }
    }
}
