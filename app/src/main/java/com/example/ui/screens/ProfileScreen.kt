package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.LocalAppStrings
import com.example.data.model.CloudBackupSnapshot
import com.example.data.model.PaymentTransaction
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.data.model.UserRole
import com.example.ui.components.AcademyAsyncImage
import com.example.ui.theme.CoralAlert
import com.example.ui.theme.EmeraldSuccess

@Composable
fun ProfileScreen(
    profileState: UiState<UserProfile?>,
    paymentsState: UiState<List<PaymentTransaction>>,
    backupsState: UiState<List<CloudBackupSnapshot>>,
    darkMode: Boolean,
    language: String,
    autoBackup: Boolean,
    onSaveProfile: (String, UserRole, String, String, Boolean, Boolean, Long) -> Unit,
    onSignOut: () -> Unit,
) {
    val strings = LocalAppStrings.current
    val profile = (profileState as? UiState.Success)?.data
    val payments = (paymentsState as? UiState.Success)?.data.orEmpty()
    val backups = (backupsState as? UiState.Success)?.data.orEmpty()

    var displayName by remember { mutableStateOf("") }
    var track by remember { mutableStateOf("") }
    var role by remember { mutableStateOf(UserRole.TEACHER) }
    var studyGoal by remember { mutableFloatStateOf(60f) }
    var localDark by remember(darkMode) { mutableStateOf(darkMode) }
    var localLang by remember(language) { mutableStateOf(language) }
    var localAutoBackup by remember(autoBackup) { mutableStateOf(autoBackup) }

    LaunchedEffect(profile) {
        if (profile != null) {
            displayName = profile.displayName
            track = profile.track
            role = UserRole.fromRaw(profile.role)
            studyGoal = profile.studyGoalMinutes.toFloat().coerceIn(15f, 300f)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Academy & User Header
        item {
            Card(
                modifier = Modifier
                    .widthIn(max = 680.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    AcademyAsyncImage(
                        resId = R.drawable.img_educator_emblem,
                        contentDescription = strings.brandTitle,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Column {
                        Text(
                            text = displayName.ifBlank { strings.brandTitle },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${strings.brandTitle} • ${strings.directorBadge}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // 2. Personalization & Preferences Form
        item {
            Card(
                modifier = Modifier
                    .widthIn(max = 680.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(strings.profileTitle, style = MaterialTheme.typography.titleMedium)

                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        label = { Text(if (strings.isArabic) "الاسم الكامل" else "Display Name") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_name_input")
                    )

                    OutlinedTextField(
                        value = track,
                        onValueChange = { track = it },
                        label = { Text(if (strings.isArabic) "التخصص / المسار التعليمي" else "Learning Track") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = role == UserRole.TEACHER,
                            onClick = { role = UserRole.TEACHER },
                            label = { Text(strings.roleTeacher) }
                        )
                        FilterChip(
                            selected = role == UserRole.STUDENT,
                            onClick = { role = UserRole.STUDENT },
                            label = { Text(strings.roleStudent) }
                        )
                    }

                    Text(
                        text = if (strings.isArabic) {
                            "الهدف اليومي للدراسة: ${studyGoal.toInt()} دقيقة"
                        } else {
                            "Daily Study Goal: ${studyGoal.toInt()} min"
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
                    Slider(
                        value = studyGoal,
                        onValueChange = { studyGoal = it },
                        valueRange = 15f..300f
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(strings.nightModeLabel, style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = localDark,
                            onCheckedChange = { localDark = it },
                            modifier = Modifier.testTag("profile_dark_mode_switch")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(strings.autoBackupToggle, style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = localAutoBackup,
                            onCheckedChange = { localAutoBackup = it },
                            modifier = Modifier.testTag("profile_auto_backup_switch")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(strings.languageLabel, style = MaterialTheme.typography.bodyMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = localLang == "AR",
                                onClick = { localLang = "AR" },
                                label = { Text("العربية") }
                            )
                            FilterChip(
                                selected = localLang == "EN",
                                onClick = { localLang = "EN" },
                                label = { Text("English") }
                            )
                        }
                    }

                    Button(
                        onClick = {
                            if (displayName.isNotBlank()) {
                                onSaveProfile(
                                    displayName,
                                    role,
                                    track,
                                    localLang,
                                    localDark,
                                    localAutoBackup,
                                    studyGoal.toLong()
                                )
                            }
                        },
                        enabled = displayName.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("save_profile_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(strings.saveProfileBtn)
                    }
                }
            }
        }

        // 3. Payment Receipts History
        item {
            Card(
                modifier = Modifier
                    .widthIn(max = 680.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(strings.paymentHistoryTitle, style = MaterialTheme.typography.titleMedium)
                    }
                    if (payments.isEmpty()) {
                        Text(
                            text = if (strings.isArabic) "لا توجد عمليات دفع مسجلة حتى الآن." else "No payment receipts yet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        payments.take(5).forEach { tx ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(tx.courseTitle, style = MaterialTheme.typography.bodySmall)
                                    Text(
                                        "${tx.maskedCard} • ${tx.transactionRef}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    "${tx.amountSar} SAR",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = EmeraldSuccess
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. Backup & Security Card
        item {
            Card(
                modifier = Modifier
                    .widthIn(max = 680.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = EmeraldSuccess)
                        Text(strings.backupHistoryTitle, style = MaterialTheme.typography.titleMedium)
                    }
                    Text(
                        text = strings.securityNotice,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    backups.take(3).forEach { bkp ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.CloudDone, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(16.dp))
                            Text(
                                "${bkp.summary} (${bkp.encryptedDigest})",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
        }

        // 5. Sign Out
        item {
            OutlinedButton(
                onClick = onSignOut,
                modifier = Modifier
                    .widthIn(max = 680.dp)
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("sign_out_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = CoralAlert)
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text(strings.signOutBtn)
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}
