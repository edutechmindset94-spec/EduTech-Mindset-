package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.LocalAppStrings
import com.example.ui.auth.signOut
import com.example.ui.theme.AmberWarning
import com.example.ui.viewmodel.MainViewModel

enum class AppTab(val route: String, val icon: ImageVector) {
    DASHBOARD("dashboard", Icons.Default.Dashboard),
    COURSES("courses", Icons.AutoMirrored.Filled.MenuBook),
    ANALYTICS("analytics", Icons.Default.Analytics),
    CHAT("chat", Icons.AutoMirrored.Filled.Chat),
    PROFILE("profile", Icons.Default.Person),
}

@Composable
fun EduTechAppShell(
    currentUserId: String,
    viewModel: MainViewModel,
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var activeTab by rememberSaveable { mutableStateOf(AppTab.DASHBOARD) }
    val snackbarHostState = remember { SnackbarHostState() }

    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    val prefs by viewModel.localPreferences.collectAsStateWithLifecycle()
    val profileState by viewModel.profileState.collectAsStateWithLifecycle()
    val coursesState by viewModel.coursesState.collectAsStateWithLifecycle()
    val studentsState by viewModel.studentRecordsState.collectAsStateWithLifecycle()
    val lessonPlansState by viewModel.lessonPlansState.collectAsStateWithLifecycle()
    val notificationsState by viewModel.notificationsState.collectAsStateWithLifecycle()
    val paymentsState by viewModel.paymentsState.collectAsStateWithLifecycle()
    val backupsState by viewModel.backupsState.collectAsStateWithLifecycle()
    val academyMessagesState by viewModel.academyMessagesState.collectAsStateWithLifecycle()

    val supportHistory by viewModel.supportHistory.collectAsStateWithLifecycle()
    val supportAiState by viewModel.supportAiState.collectAsStateWithLifecycle()
    val lessonPlanAiState by viewModel.lessonPlanAiState.collectAsStateWithLifecycle()
    val analyticsReportState by viewModel.analyticsReportState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    BackHandler(enabled = activeTab != AppTab.DASHBOARD) {
        activeTab = AppTab.DASHBOARD
    }

    fun labelFor(tab: AppTab): String = when (tab) {
        AppTab.DASHBOARD -> strings.navDashboard
        AppTab.COURSES -> strings.navCourses
        AppTab.ANALYTICS -> strings.navAnalytics
        AppTab.CHAT -> strings.navChat
        AppTab.PROFILE -> strings.navProfile
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 840.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (!useNavigationRail) {
                    NavigationBar {
                        AppTab.entries.forEach { tab ->
                            val label = labelFor(tab)
                            NavigationBarItem(
                                selected = activeTab == tab,
                                onClick = { activeTab = tab },
                                icon = { Icon(tab.icon, contentDescription = label) },
                                label = { Text(label, maxLines = 1) },
                                modifier = Modifier.testTag("nav_${tab.route}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavigationRail) {
                    NavigationRail(modifier = Modifier.fillMaxHeight()) {
                        AppTab.entries.forEach { tab ->
                            val label = labelFor(tab)
                            NavigationRailItem(
                                selected = activeTab == tab,
                                onClick = { activeTab = tab },
                                icon = { Icon(tab.icon, contentDescription = label) },
                                label = { Text(label) },
                                modifier = Modifier.testTag("rail_${tab.route}")
                            )
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    AnimatedVisibility(visible = !isOnline) {
                        Surface(
                            color = AmberWarning.copy(alpha = 0.2f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.CloudOff,
                                    contentDescription = null,
                                    tint = AmberWarning,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = strings.offlineBanner,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        when (activeTab) {
                            AppTab.DASHBOARD -> DashboardScreen(
                                profileState = profileState,
                                coursesState = coursesState,
                                studentsState = studentsState,
                                notificationsState = notificationsState,
                                darkMode = prefs.darkMode,
                                onToggleDarkMode = viewModel::toggleDarkMode,
                                onSwitchRole = viewModel::switchRole,
                                onAddStudent = viewModel::addStudentRecord,
                                onIncrementStudent = viewModel::incrementStudentProgress,
                                onSendNotification = viewModel::sendNotification,
                                onMarkNotificationRead = viewModel::markNotificationRead,
                                onNavigateToCourses = { activeTab = AppTab.COURSES }
                            )

                            AppTab.COURSES -> CoursesScreen(
                                coursesState = coursesState,
                                lessonPlansState = lessonPlansState,
                                lessonPlanAiState = lessonPlanAiState,
                                onCreateCourse = viewModel::createCourse,
                                onAdvanceLesson = viewModel::advanceCourseLesson,
                                onCompletePayment = viewModel::completePaymentAndEnroll,
                                onSubmitEvaluation = viewModel::submitCourseEvaluation,
                                onGenerateAiPlan = viewModel::generateAiLessonPlan,
                                onSaveManualPlan = viewModel::saveManualLessonPlan
                            )

                            AppTab.ANALYTICS -> AnalyticsScreen(
                                profileState = profileState,
                                coursesState = coursesState,
                                studentsState = studentsState,
                                backupsState = backupsState,
                                analyticsReportState = analyticsReportState,
                                onGenerateAiReport = viewModel::generateAiAcademicReport,
                                onCreateBackupNow = viewModel::createCloudBackup
                            )

                            AppTab.CHAT -> ChatScreen(
                                currentUserId = currentUserId,
                                messagesState = academyMessagesState,
                                supportHistory = supportHistory,
                                supportAiState = supportAiState,
                                onSendPeerMessage = viewModel::sendAcademyMessage,
                                onDeletePeerMessage = viewModel::deleteAcademyMessage,
                                onAskAiSupport = viewModel::askAiSupport
                            )

                            AppTab.PROFILE -> ProfileScreen(
                                profileState = profileState,
                                paymentsState = paymentsState,
                                backupsState = backupsState,
                                darkMode = prefs.darkMode,
                                language = prefs.language,
                                autoBackup = prefs.autoBackup,
                                onSaveProfile = viewModel::saveProfile,
                                onSignOut = {
                                    signOut(credentialManager = credentialManager, scope = scope)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
