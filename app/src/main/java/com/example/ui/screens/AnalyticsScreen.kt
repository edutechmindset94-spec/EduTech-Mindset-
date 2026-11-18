package com.example.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.LocalAppStrings
import com.example.data.model.AiUiState
import com.example.data.model.CloudBackupSnapshot
import com.example.data.model.Course
import com.example.data.model.StudentRecord
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.ui.theme.AcademySky
import com.example.ui.theme.AcademyTeal
import com.example.ui.theme.AcademyTealBright
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.ScholarGold

@Composable
fun AnalyticsScreen(
    profileState: UiState<UserProfile?>,
    coursesState: UiState<List<Course>>,
    studentsState: UiState<List<StudentRecord>>,
    backupsState: UiState<List<CloudBackupSnapshot>>,
    analyticsReportState: AiUiState,
    onGenerateAiReport: () -> Unit,
    onCreateBackupNow: (String) -> Unit,
) {
    val strings = LocalAppStrings.current
    val profile = (profileState as? UiState.Success)?.data
    val courses = (coursesState as? UiState.Success)?.data.orEmpty()
    val students = (studentsState as? UiState.Success)?.data.orEmpty()
    val backups = (backupsState as? UiState.Success)?.data.orEmpty()

    val avgCourseProgress = if (courses.isEmpty()) 0f else courses.map { it.progressPercent }.average().toFloat()
    val avgQuizScore = if (students.isEmpty()) (profile?.overallScore?.toFloat() ?: 92f) else students.map { it.quizScore }.average().toFloat()
    val avgAttendance = if (students.isEmpty()) 95f else students.map { it.attendanceRate }.average().toFloat()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Text(
                text = strings.analyticsTitle,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier
                    .widthIn(max = 680.dp)
                    .fillMaxWidth()
            )
        }

        // 1. Completion & Attendance Donut Ring + Summary
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
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    val primaryRingColor = AcademyTeal
                    val secondaryRingColor = ScholarGold
                    val trackColor = MaterialTheme.colorScheme.surfaceVariant

                    Box(
                        modifier = Modifier.size(116.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val strokeWidth = 12.dp.toPx()
                            drawArc(
                                color = trackColor,
                                startAngle = 0f,
                                sweepAngle = 360f,
                                useCenter = false,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )
                            drawArc(
                                color = primaryRingColor,
                                startAngle = -90f,
                                sweepAngle = (avgQuizScore / 100f) * 360f,
                                useCenter = false,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )
                            val innerInset = strokeWidth + 6.dp.toPx()
                            drawArc(
                                color = secondaryRingColor,
                                startAngle = -90f,
                                sweepAngle = (avgAttendance / 100f) * 360f,
                                useCenter = false,
                                topLeft = Offset(innerInset, innerInset),
                                size = Size(size.width - innerInset * 2, size.height - innerInset * 2),
                                style = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${avgQuizScore.toInt()}%",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (strings.isArabic) "التحصيل" else "Score",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        MetricRow(
                            label = if (strings.isArabic) "متوسط التحصيل الأكاديمي" else "Academic Average",
                            value = "${avgQuizScore.toInt()}%",
                            color = AcademyTeal
                        )
                        MetricRow(
                            label = if (strings.isArabic) "معدل الحضور والمشاركة" else "Attendance Rate",
                            value = "${avgAttendance.toInt()}%",
                            color = ScholarGold
                        )
                        MetricRow(
                            label = if (strings.isArabic) "تقدم المناهج النشطة" else "Course Completion",
                            value = "${avgCourseProgress.toInt()}%",
                            color = AcademySky
                        )
                    }
                }
            }
        }

        // 2. Academic Achievement Trend Chart (Custom Compose Canvas)
        item {
            Card(
                modifier = Modifier
                    .widthIn(max = 680.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(strings.achievementTrendTitle, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(12.dp))
                    val points = listOf(74f, 79f, 83f, 86f, 90f, avgQuizScore.coerceIn(60f, 100f))
                    val lineColor = AcademyTealBright
                    val fillColor = AcademyTeal
                    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)

                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(155.dp)
                    ) {
                        val w = size.width
                        val h = size.height
                        val stepX = w / (points.size - 1).coerceAtLeast(1)

                        for (i in 0..3) {
                            val y = h * (i / 3f)
                            drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 1.dp.toPx())
                        }

                        val coords = points.mapIndexed { idx, value ->
                            val x = idx * stepX
                            val normalized = ((value - 50f) / 50f).coerceIn(0f, 1f)
                            val y = h - (normalized * (h * 0.85f) + h * 0.08f)
                            Offset(x, y)
                        }

                        val strokePath = Path().apply {
                            moveTo(coords.first().x, coords.first().y)
                            for (i in 1 until coords.size) {
                                val prev = coords[i - 1]
                                val curr = coords[i]
                                val midX = (prev.x + curr.x) / 2f
                                cubicTo(midX, prev.y, midX, curr.y, curr.x, curr.y)
                            }
                        }

                        val fillPath = Path().apply {
                            addPath(strokePath)
                            lineTo(coords.last().x, h)
                            lineTo(coords.first().x, h)
                            close()
                        }

                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                listOf(fillColor.copy(alpha = 0.35f), Color.Transparent)
                            )
                        )
                        drawPath(
                            path = strokePath,
                            color = lineColor,
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                        )
                        coords.forEach { pt ->
                            drawCircle(color = Color.White, radius = 5.dp.toPx(), center = pt)
                            drawCircle(color = fillColor, radius = 3.dp.toPx(), center = pt)
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val labels = if (strings.isArabic) {
                            listOf("الأسبوع 1", "الأسبوع 2", "الأسبوع 3", "الأسبوع 4", "الأسبوع 5", "الآن")
                        } else {
                            listOf("W1", "W2", "W3", "W4", "W5", "Now")
                        }
                        labels.forEach { lbl ->
                            Text(
                                text = lbl,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // 3. Mastery by Track Bar Chart
        item {
            Card(
                modifier = Modifier
                    .widthIn(max = 680.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(strings.masteryBySubjectTitle, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(12.dp))
                    val trackBars = if (courses.isEmpty()) {
                        listOf("AI" to 80f, "Code" to 65f, "Analytics" to 75f)
                    } else {
                        courses.take(4).map { it.category.take(14) to it.progressPercent.toFloat().coerceAtLeast(15f) }
                    }
                    val trackBg = MaterialTheme.colorScheme.surfaceVariant
                    trackBars.forEachIndexed { idx, (label, pct) ->
                        val barColor = when (idx % 3) {
                            0 -> AcademyTeal
                            1 -> AcademySky
                            else -> ScholarGold
                        }
                        Column(modifier = Modifier.padding(vertical = 5.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(label, style = MaterialTheme.typography.bodySmall)
                                Text("${pct.toInt()}%", style = MaterialTheme.typography.labelMedium, color = barColor)
                            }
                            Spacer(Modifier.height(4.dp))
                            Canvas(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                            ) {
                                drawRoundRect(
                                    color = trackBg,
                                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                                )
                                drawRoundRect(
                                    color = barColor,
                                    size = Size(size.width * (pct / 100f).coerceIn(0f, 1f), size.height),
                                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. AI Periodic Analytical Report
        item {
            Card(
                modifier = Modifier
                    .widthIn(max = 680.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onGenerateAiReport,
                        enabled = analyticsReportState !is AiUiState.Loading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("generate_ai_report_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text(strings.generateAiReportBtn)
                    }

                    when (analyticsReportState) {
                        is AiUiState.Idle -> Text(
                            text = if (strings.isArabic) {
                                "اضغط لتوليد تقرير تحليلي ذكي لأداء الطلاب ومستوى التحصيل العلمي."
                            } else {
                                "Tap to generate an AI-powered periodic performance report."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        is AiUiState.Loading -> Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator(Modifier.size(20.dp))
                            Text(if (strings.isArabic) "جاري تحليل البيانات الأكاديمية..." else "Analyzing academic metrics...")
                        }
                        is AiUiState.Streaming -> Text(analyticsReportState.text, style = MaterialTheme.typography.bodyMedium)
                        is AiUiState.Success -> Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(
                                text = analyticsReportState.text,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                        is AiUiState.Error -> ErrorCard(analyticsReportState.message)
                    }
                }
            }
        }

        // 5. Encrypted Cloud Backup Card
        item {
            Card(
                modifier = Modifier
                    .widthIn(max = 680.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = EmeraldSuccess)
                        Text(strings.cloudBackupTitle, style = MaterialTheme.typography.titleMedium)
                    }
                    Text(
                        text = if (strings.isArabic) {
                            "عدد النسخ المحفوظة: ${backups.size} • أحدث توقيع: ${backups.firstOrNull()?.encryptedDigest ?: "جاهز للإنشاء"}"
                        } else {
                            "Saved snapshots: ${backups.size} • Latest digest: ${backups.firstOrNull()?.encryptedDigest ?: "Ready"}"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = {
                            onCreateBackupNow(
                                if (strings.isArabic) "نسخة احتياطية يدوية مشفرة" else "Encrypted manual cloud backup"
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("create_cloud_backup_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(8.dp))
                        Text(strings.createBackupNowBtn)
                    }
                }
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun MetricRow(label: String, value: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleSmall, color = color, fontWeight = FontWeight.Bold)
    }
}
