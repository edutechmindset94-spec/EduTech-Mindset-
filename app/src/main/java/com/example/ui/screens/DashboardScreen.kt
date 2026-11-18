package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.LocalAppStrings
import com.example.data.model.Course
import com.example.data.model.ExternalPlatform
import com.example.data.model.NotificationCategory
import com.example.data.model.SmartNotification
import com.example.data.model.StudentRecord
import com.example.data.model.StudentStatus
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.data.model.UserRole
import com.example.ui.theme.AcademyNavy
import com.example.ui.theme.AcademyNavyDeep
import com.example.ui.theme.AcademyTeal
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CoralAlert
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.ScholarGoldLight

private val ExternalPlatforms = listOf(
    ExternalPlatform(
        id = "classroom",
        nameAr = "Google Classroom",
        nameEn = "Google Classroom",
        descAr = "مزامنة الفصول والواجبات",
        descEn = "Sync classes & tasks",
        url = "https://classroom.google.com"
    ),
    ExternalPlatform(
        id = "teams",
        nameAr = "Microsoft Teams",
        nameEn = "Microsoft Teams",
        descAr = "الحصص المباشرة والاجتماعات",
        descEn = "Live classes & meetings",
        url = "https://teams.microsoft.com"
    ),
    ExternalPlatform(
        id = "moodle",
        nameAr = "Moodle LMS",
        nameEn = "Moodle LMS",
        descAr = "مستودع المناهج والاختبارات",
        descEn = "Curriculum & quizzes",
        url = "https://moodle.org"
    ),
    ExternalPlatform(
        id = "khan",
        nameAr = "Khan Academy",
        nameEn = "Khan Academy",
        descAr = "مكتبة الموارد التفاعلية",
        descEn = "Interactive study library",
        url = "https://www.khanacademy.org"
    ),
)

@Composable
fun DashboardScreen(
    profileState: UiState<UserProfile?>,
    coursesState: UiState<List<Course>>,
    studentsState: UiState<List<StudentRecord>>,
    notificationsState: UiState<List<SmartNotification>>,
    darkMode: Boolean,
    onToggleDarkMode: () -> Unit,
    onSwitchRole: (UserRole) -> Unit,
    onAddStudent: (String, String, Long, Long, Long, String) -> Unit,
    onIncrementStudent: (StudentRecord) -> Unit,
    onSendNotification: (String, String, NotificationCategory, Boolean) -> Unit,
    onMarkNotificationRead: (String) -> Unit,
    onNavigateToCourses: () -> Unit,
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current

    val profile = (profileState as? UiState.Success)?.data
    val courses = (coursesState as? UiState.Success)?.data.orEmpty()
    val students = (studentsState as? UiState.Success)?.data.orEmpty()
    val notifications = (notificationsState as? UiState.Success)?.data.orEmpty()

    val currentRole = UserRole.fromRaw(profile?.role)
    val unreadCount = notifications.count { !it.read }
    val enrolledCount = courses.count { it.enrolled }

    var showAddStudentDialog by remember { mutableStateOf(false) }
    var showAddAlertDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Academy Hero Header Card
        item {
            Card(
                modifier = Modifier
                    .widthIn(max = 680.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Image(
                        painter = painterResource(id = R.drawable.img_hero_edutech),
                        contentDescription = strings.brandTitle,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(190.dp),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        AcademyNavyDeep.copy(alpha = 0.35f),
                                        AcademyNavy.copy(alpha = 0.92f)
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.img_educator_emblem),
                                    contentDescription = strings.directorBadge,
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                                Column {
                                    Text(
                                        text = strings.brandTitle,
                                        style = MaterialTheme.typography.titleLarge,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = strings.directorBadge,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = ScholarGoldLight
                                    )
                                }
                            }

                            IconButton(
                                onClick = onToggleDarkMode,
                                modifier = Modifier.testTag("dashboard_night_mode_btn")
                            ) {
                                Icon(
                                    imageVector = if (darkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                                    contentDescription = strings.nightModeLabel,
                                    tint = Color.White
                                )
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        Text(
                            text = if (strings.isArabic) {
                                "مرحباً، ${profile?.displayName ?: "عضو الأكاديمية"}"
                            } else {
                                "Welcome, ${profile?.displayName ?: "Academy Member"}"
                            },
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White
                        )

                        Spacer(Modifier.height(10.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = currentRole == UserRole.TEACHER,
                                onClick = { onSwitchRole(UserRole.TEACHER) },
                                label = { Text(strings.roleTeacher) },
                                modifier = Modifier.testTag("role_teacher_chip")
                            )
                            FilterChip(
                                selected = currentRole == UserRole.STUDENT,
                                onClick = { onSwitchRole(UserRole.STUDENT) },
                                label = { Text(strings.roleStudent) },
                                modifier = Modifier.testTag("role_student_chip")
                            )
                        }
                    }
                }
            }
        }

        // 2. KPI Summary Grid
        item {
            Column(
                modifier = Modifier
                    .widthIn(max = 680.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KpiCard(
                        modifier = Modifier.weight(1f),
                        title = strings.academicScoreLabel,
                        value = "${(profile?.overallScore ?: 92.0).toInt()}%",
                        icon = Icons.AutoMirrored.Filled.TrendingUp,
                        accent = EmeraldSuccess
                    )
                    KpiCard(
                        modifier = Modifier.weight(1f),
                        title = strings.activeCoursesLabel,
                        value = "$enrolledCount / ${courses.size}",
                        icon = Icons.Default.School,
                        accent = AcademyTeal
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KpiCard(
                        modifier = Modifier.weight(1f),
                        title = strings.completedLessonsLabel,
                        value = "${profile?.completedLessonsCount ?: 0}",
                        icon = Icons.Default.TaskAlt,
                        accent = MaterialTheme.colorScheme.secondary
                    )
                    KpiCard(
                        modifier = Modifier.weight(1f),
                        title = strings.unreadAlertsLabel,
                        value = "$unreadCount",
                        icon = Icons.Default.NotificationsActive,
                        accent = if (unreadCount > 0) AmberWarning else AcademyTeal
                    )
                }
            }
        }

        // 3. Real-Time Student Progress Tracking (Prominently shown in Teacher Mode, collapsible/viewable for Students)
        item {
            AnimatedVisibility(visible = currentRole == UserRole.TEACHER || students.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 680.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = strings.studentProgressTitle,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        AssistChip(
                            onClick = { showAddStudentDialog = true },
                            label = { Text(strings.addStudentBtn) },
                            leadingIcon = { Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            modifier = Modifier.testTag("add_student_button")
                        )
                    }

                    when (studentsState) {
                        is UiState.Loading -> CircularProgressIndicator(Modifier.padding(16.dp))
                        is UiState.Error -> ErrorCard(studentsState.message)
                        is UiState.Success -> {
                            if (studentsState.data.isEmpty()) {
                                EmptyStateCard(strings.emptyStudentsMsg)
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    studentsState.data.forEach { student ->
                                        StudentProgressCard(
                                            record = student,
                                            isArabic = strings.isArabic,
                                            onAdvance = { onIncrementStudent(student) },
                                            onNotifyParent = {
                                                onSendNotification(
                                                    if (strings.isArabic) "تحديث لولي أمر: ${student.studentName}" else "Parent Update: ${student.studentName}",
                                                    if (strings.isArabic) "نسبة الإنجاز ${student.progressPercent}% والاختبار ${student.quizScore}%." else "Progress ${student.progressPercent}%, Quiz ${student.quizScore}%.",
                                                    NotificationCategory.PARENT_SCHOOL,
                                                    true
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Smart Notifications & Home-School Communication
        item {
            Column(
                modifier = Modifier
                    .widthIn(max = 680.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.notificationsTitle,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    AssistChip(
                        onClick = { showAddAlertDialog = true },
                        label = { Text(strings.sendAlertBtn) },
                        leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        modifier = Modifier.testTag("send_smart_alert_button")
                    )
                }

                when (notificationsState) {
                    is UiState.Loading -> CircularProgressIndicator(Modifier.padding(12.dp))
                    is UiState.Error -> ErrorCard(notificationsState.message)
                    is UiState.Success -> {
                        if (notificationsState.data.isEmpty()) {
                            EmptyStateCard(strings.emptyNotificationsMsg)
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                notificationsState.data.take(6).forEach { notif ->
                                    NotificationCard(
                                        notification = notif,
                                        isArabic = strings.isArabic,
                                        onMarkRead = { onMarkNotificationRead(notif.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. E-Learning Platform Integrations & Online Resources
        item {
            Column(
                modifier = Modifier
                    .widthIn(max = 680.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.externalPlatformsTitle,
                        style = MaterialTheme.typography.titleMedium
                    )
                    TextButton(onClick = onNavigateToCourses) {
                        Text(strings.navCourses)
                    }
                }

                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(ExternalPlatforms, key = { it.id }) { platform ->
                        Card(
                            modifier = Modifier
                                .width(205.dp)
                                .clickable {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(platform.url))
                                    runCatching { context.startActivity(intent) }
                                },
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (strings.isArabic) platform.nameAr else platform.nameEn,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                        contentDescription = platform.nameEn,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = if (strings.isArabic) platform.descAr else platform.descEn,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }

    if (showAddStudentDialog) {
        AddStudentDialog(
            isArabic = strings.isArabic,
            onDismiss = { showAddStudentDialog = false },
            onConfirm = { name, course, progress, quiz, attendance, note ->
                onAddStudent(name, course, progress, quiz, attendance, note)
                showAddStudentDialog = false
            }
        )
    }

    if (showAddAlertDialog) {
        SendAlertDialog(
            isArabic = strings.isArabic,
            onDismiss = { showAddAlertDialog = false },
            onConfirm = { title, body, category, urgent ->
                onSendNotification(title, body, category, urgent)
                showAddAlertDialog = false
            }
        )
    }
}

@Composable
private fun KpiCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: ImageVector,
    accent: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                color = accent.copy(alpha = 0.14f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
                }
            }
            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun StudentProgressCard(
    record: StudentRecord,
    isArabic: Boolean,
    onAdvance: () -> Unit,
    onNotifyParent: () -> Unit
) {
    val statusEnum = StudentStatus.fromRaw(record.status)
    val badgeColor = when (statusEnum) {
        StudentStatus.EXCELLENT -> EmeraldSuccess
        StudentStatus.ON_TRACK -> AcademyTeal
        StudentStatus.NEEDS_SUPPORT -> CoralAlert
    }
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(record.studentName, style = MaterialTheme.typography.titleMedium)
                    Text(
                        record.courseTitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    color = badgeColor.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Text(
                        text = if (isArabic) statusEnum.labelAr else statusEnum.labelEn,
                        color = badgeColor,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { (record.progressPercent / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = badgeColor
            )
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isArabic) {
                        "التقدم ${record.progressPercent}% • الاختبار ${record.quizScore}% • الحضور ${record.attendanceRate}%"
                    } else {
                        "Progress ${record.progressPercent}% • Quiz ${record.quizScore}% • Att ${record.attendanceRate}%"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (record.parentContactNote.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = record.parentContactNote,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(
                    onClick = onAdvance,
                    label = { Text(if (isArabic) "تحديث التقدم +10%" else "Update +10%") },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.TrendingUp, null, Modifier.size(16.dp)) }
                )
                AssistChip(
                    onClick = onNotifyParent,
                    label = { Text(if (isArabic) "إشعار ولي الأمر" else "Notify Home") },
                    leadingIcon = { Icon(Icons.Default.FamilyRestroom, null, Modifier.size(16.dp)) },
                    colors = AssistChipDefaults.assistChipColors()
                )
            }
        }
    }
}

@Composable
private fun NotificationCard(
    notification: SmartNotification,
    isArabic: Boolean,
    onMarkRead: () -> Unit
) {
    val cat = NotificationCategory.fromRaw(notification.category)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { if (!notification.read) onMarkRead() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (notification.read) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = if (cat == NotificationCategory.PARENT_SCHOOL) {
                    Icons.Default.FamilyRestroom
                } else {
                    Icons.Default.AutoAwesome
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = notification.title,
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f)
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (isArabic) cat.labelAr else cat.labelEn,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = notification.body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!notification.read) {
                IconButton(onClick = onMarkRead) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = if (isArabic) "تحديد كمقروء" else "Mark read",
                        tint = AcademyTeal
                    )
                }
            }
        }
    }
}

@Composable
fun ErrorCard(message: String) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.onErrorContainer,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(14.dp)
        )
    }
}

@Composable
fun EmptyStateCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.School,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AddStudentDialog(
    isArabic: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String, String, Long, Long, Long, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var course by remember { mutableStateOf("هندسة التعليم الذكي") }
    var progress by remember { mutableFloatStateOf(70f) }
    var quiz by remember { mutableFloatStateOf(85f) }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isArabic) "إضافة سجل طالب للمتابعة" else "Add Student Record") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(if (isArabic) "اسم الطالب" else "Student Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("dialog_student_name")
                )
                OutlinedTextField(
                    value = course,
                    onValueChange = { course = it },
                    label = { Text(if (isArabic) "الدورة" else "Course") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(if (isArabic) "التقدم: ${progress.toInt()}%" else "Progress: ${progress.toInt()}%")
                Slider(value = progress, onValueChange = { progress = it }, valueRange = 0f..100f)
                Text(if (isArabic) "درجة الاختبار: ${quiz.toInt()}%" else "Quiz Score: ${quiz.toInt()}%")
                Slider(value = quiz, onValueChange = { quiz = it }, valueRange = 0f..100f)
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(if (isArabic) "ملاحظة تواصل البيت والمدرسة" else "Home-School Note") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.trim().length >= 2) {
                        onConfirm(name, course, progress.toLong(), quiz.toLong(), 95L, note)
                    }
                },
                enabled = name.trim().length >= 2
            ) {
                Text(if (isArabic) "حفظ" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(if (isArabic) "إلغاء" else "Cancel") }
        }
    )
}

@Composable
private fun SendAlertDialog(
    isArabic: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String, String, NotificationCategory, Boolean) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(NotificationCategory.PARENT_SCHOOL) }
    var highPriority by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isArabic) "إرسال تنبيه ذكي" else "Send Smart Notification") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(if (isArabic) "عنوان التنبيه" else "Alert Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = body,
                    onValueChange = { body = it },
                    label = { Text(if (isArabic) "نص الرسالة" else "Message") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    NotificationCategory.entries.take(2).forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(if (isArabic) cat.labelAr else cat.labelEn) }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    NotificationCategory.entries.drop(2).forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(if (isArabic) cat.labelAr else cat.labelEn) }
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(if (isArabic) "أولوية عالية" else "High Priority")
                    Switch(checked = highPriority, onCheckedChange = { highPriority = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.trim().length >= 2 && body.trim().isNotEmpty()) {
                        onConfirm(title, body, selectedCategory, highPriority)
                    }
                },
                enabled = title.trim().length >= 2 && body.trim().isNotEmpty()
            ) {
                Text(if (isArabic) "إرسال" else "Send")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(if (isArabic) "إلغاء" else "Cancel") }
        }
    )
}
