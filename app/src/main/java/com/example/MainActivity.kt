package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.NativityAppHeader
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.DailyTasksScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.MyLogScreen
import com.example.ui.screens.MyRankScreen
import com.example.ui.screens.RemindersDialog
import com.example.ui.screens.WelcomeScreen
import com.example.ui.state.ChallengeViewModel
import com.example.ui.state.YouthTab
import androidx.compose.ui.graphics.Color
import com.example.ui.theme.MidnightTeal
import com.example.ui.theme.NativityBurgundy
import com.example.ui.theme.NativityFastTheme
import com.example.ui.theme.WarmGold
import com.example.ui.theme.WarmGoldContainer
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NativityFastTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    TariqAlMazwadApp()
                }
            }
        }
    }
}

@Composable
fun TariqAlMazwadApp(
    viewModel: ChallengeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var showRemindersDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.snackbarMessages.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    if (showRemindersDialog) {
        RemindersDialog(
            settings = uiState.reminderSettings,
            onUpdateSettings = viewModel::updateReminders,
            onSendTest = viewModel::sendTestEncouragement,
            onDismiss = { showRemindersDialog = false }
        )
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("تسجيل الخروج") },
            text = {
                Text(
                    "هل تريد تسجيل الخروج من حساب «${uiState.activeUser?.fullName.orEmpty()}»؟ يمكنك تسجيل الدخول مرة أخرى في أي وقت بنفس الاسم والرقم السري."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        viewModel.logoutYouth()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NativityBurgundy),
                    modifier = Modifier.testTag("confirm_logout_button")
                ) {
                    Text("تسجيل الخروج", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    when {
        uiState.isLoading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = WarmGold)
            }
        }

        uiState.isAdminOpen -> {
            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) }
            ) { innerPadding ->
                AdminDashboardScreen(
                    uiState = uiState,
                    onUnlock = viewModel::unlockAdmin,
                    onClose = viewModel::closeAdmin,
                    onLock = viewModel::lockAdmin,
                    onReviewSubmission = viewModel::reviewSubmission,
                    onApproveAllPending = viewModel::approveAllPending,
                    onAdjustBonus = viewModel::adjustUserBonus,
                    onAdminAddUser = viewModel::adminAddUser,
                    onAdminUpdateUser = viewModel::adminUpdateUser,
                    onAdminDeleteUser = viewModel::adminDeleteUser,
                    onUpdateTask = viewModel::updateTask,
                    onUpdateConfig = viewModel::updateAppConfig,
                    onExportCsv = viewModel::exportLeaderboardCsv,
                    onChangeAdminPin = viewModel::changeAdminPin,
                    onFeedback = { msg ->
                        coroutineScope.launch { snackbarHostState.showSnackbar(msg) }
                    },
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }

        !uiState.hasSeenWelcome && uiState.activeUser == null -> {
            WelcomeScreen(
                onStartJourney = viewModel::completeWelcome
            )
        }

        uiState.activeUser == null -> {
            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) }
            ) { innerPadding ->
                LoginScreen(
                    isSyncing = uiState.isSyncing,
                    isSupabaseLive = uiState.isSupabaseLive,
                    onLoginOrRegister = viewModel::loginOrRegister,
                    onOpenAdmin = viewModel::openAdmin,
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }

        else -> {
            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = {
                    NativityAppHeader(
                        isSupabaseLive = uiState.isSupabaseLive,
                        pendingAdminCount = uiState.pendingSubmissions.size,
                        onOpenAdmin = viewModel::openAdmin,
                        onOpenReminders = { showRemindersDialog = true },
                        onRefresh = viewModel::refreshNow,
                        onLogout = { showLogoutDialog = true },
                        modifier = Modifier.statusBarsPadding()
                    )
                },
                bottomBar = {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        YouthTab.entries.forEach { tab ->
                            val selected = uiState.selectedTab == tab
                            val (selectedIcon, unselectedIcon) = when (tab) {
                                YouthTab.DAILY -> Icons.Filled.TaskAlt to Icons.Outlined.TaskAlt
                                YouthTab.MY_LOG -> Icons.Filled.History to Icons.Outlined.History
                                YouthTab.MY_RANK -> Icons.Filled.EmojiEvents to Icons.Outlined.EmojiEvents
                            }
                            NavigationBarItem(
                                selected = selected,
                                onClick = { viewModel.selectTab(tab) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) selectedIcon else unselectedIcon,
                                        contentDescription = tab.titleAr
                                    )
                                },
                                label = {
                                    Text(
                                        text = tab.titleAr,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MidnightTeal,
                                    selectedTextColor = MidnightTeal,
                                    indicatorColor = WarmGoldContainer
                                ),
                                modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                            )
                        }
                    }
                }
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    Crossfade(
                        targetState = uiState.selectedTab,
                        label = "youth_tab_crossfade"
                    ) { tab ->
                        when (tab) {
                            YouthTab.DAILY -> DailyTasksScreen(
                                uiState = uiState,
                                onSubmitTask = viewModel::submitTask,
                                onOpenMyRank = { viewModel.selectTab(YouthTab.MY_RANK) },
                                onLogout = { showLogoutDialog = true }
                            )
                            YouthTab.MY_LOG -> MyLogScreen(
                                uiState = uiState
                            )
                            YouthTab.MY_RANK -> MyRankScreen(
                                uiState = uiState
                            )
                        }
                    }
                }
            }
        }
    }
}
