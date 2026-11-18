package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.AppStrings
import com.example.core.LocalAppStrings
import com.example.data.model.Course
import com.example.data.model.UiState
import com.example.ui.components.AcademyAsyncImage
import com.example.ui.theme.AcademyNavy
import com.example.ui.theme.AcademyNavyDeep
import com.example.ui.theme.AcademySky
import com.example.ui.theme.AcademyTeal
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.ScholarGold
import java.util.Locale

fun LazyListScope.studentDashboardItems(
    coursesState: UiState<List<Course>>,
    strings: AppStrings,
    onAdvanceLesson: (Course) -> Unit,
    onNavigateToCourses: () -> Unit,
) {
    when (coursesState) {
        is UiState.Loading -> {
            item(key = "student_dash_loading") {
                Box(
                    modifier = Modifier
                        .widthIn(max = 680.dp)
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }

        is UiState.Error -> {
            item(key = "student_dash_error") {
                Box(
                    modifier = Modifier
                        .widthIn(max = 680.dp)
                        .fillMaxWidth()
                ) {
                    ErrorCard(coursesState.message)
                }
            }
        }

        is UiState.Success -> {
            val allCourses = coursesState.data
            val ongoingCourses = allCourses.filter { it.enrolled && it.progressPercent < 100L }
                .ifEmpty { allCourses.filter { it.enrolled } }
            val activeEnrolled = allCourses.filter { it.enrolled }

            // 1. Quick Access Section for Ongoing Modules
            item(key = "quick_access_ongoing") {
                Box(
                    modifier = Modifier
                        .widthIn(max = 680.dp)
                        .fillMaxWidth()
                ) {
                    QuickAccessOngoingModulesSection(
                        ongoingCourses = ongoingCourses,
                        onResumeModule = onAdvanceLesson,
                        onViewAll = onNavigateToCourses
                    )
                }
            }

            // 2. Progress Bars for Active Learning
            item(key = "active_learning_progress") {
                Box(
                    modifier = Modifier
                        .widthIn(max = 680.dp)
                        .fillMaxWidth()
                ) {
                    ActiveLearningProgressSection(
                        activeCourses = activeEnrolled,
                        onAdvanceLesson = onAdvanceLesson,
                        onExploreCourses = onNavigateToCourses
                    )
                }
            }

            // 3. Available Courses Header
            item(key = "available_courses_header") {
                Box(
                    modifier = Modifier
                        .widthIn(max = 680.dp)
                        .fillMaxWidth()
                ) {
                    AvailableCoursesHeader(
                        strings = strings,
                        onOpenCourses = onNavigateToCourses
                    )
                }
            }

            if (allCourses.isEmpty()) {
                item(key = "available_courses_empty") {
                    Box(
                        modifier = Modifier
                            .widthIn(max = 680.dp)
                            .fillMaxWidth()
                    ) {
                        EmptyStateCard(strings.emptyCoursesMsg)
                    }
                }
            } else {
                items(allCourses, key = { "dash_course_${it.id}" }) { course ->
                    Box(
                        modifier = Modifier
                            .widthIn(max = 680.dp)
                            .fillMaxWidth()
                    ) {
                        AvailableCourseRowCard(
                            course = course,
                            strings = strings,
                            onOpenCourses = onNavigateToCourses
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickAccessOngoingModulesSection(
    ongoingCourses: List<Course>,
    onResumeModule: (Course) -> Unit,
    onViewAll: () -> Unit,
) {
    val strings = LocalAppStrings.current
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = null,
                    tint = ScholarGold,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = strings.ongoingModulesTitle,
                    style = MaterialTheme.typography.titleLarge
                )
            }
            TextButton(onClick = onViewAll) {
                Text(strings.viewAllBtn)
            }
        }

        if (ongoingCourses.isEmpty()) {
            EmptyStateCard(strings.noOngoingModulesMsg)
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(end = 4.dp)
            ) {
                items(ongoingCourses, key = { "ongoing_${it.id}" }) { course ->
                    val nextLesson = (course.completedLessons + 1L).coerceAtMost(course.totalLessons.coerceAtLeast(1L))
                    val thumbRes = if (course.imageKey == "code") R.drawable.img_course_code else R.drawable.img_course_ai

                    Card(
                        modifier = Modifier
                            .width(268.dp)
                            .testTag("ongoing_module_card_${course.id}"),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                    ) {
                        Column {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(92.dp)
                            ) {
                                AcademyAsyncImage(
                                    resId = thumbRes,
                                    contentDescription = course.title,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(Color.Transparent, AcademyNavyDeep.copy(alpha = 0.85f))
                                            )
                                        )
                                )
                                Surface(
                                    color = AcademyTeal,
                                    shape = RoundedCornerShape(999.dp),
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(10.dp)
                                ) {
                                    Text(
                                        text = "${strings.nextLessonLabel} #$nextLesson",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                                Text(
                                    text = "${course.progressPercent}%",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(10.dp)
                                )
                            }

                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = course.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                LinearProgressIndicator(
                                    progress = { (course.progressPercent / 100f).coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(7.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = AcademyTeal
                                )
                                Button(
                                    onClick = { onResumeModule(course) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("resume_module_btn_${course.id}"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = AcademyNavy)
                                ) {
                                    Icon(
                                        Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = Color.White
                                    )
                                    Spacer(Modifier.size(6.dp))
                                    Text(strings.resumeModuleBtn, color = Color.White, maxLines = 1)
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
private fun ActiveLearningProgressSection(
    activeCourses: List<Course>,
    onAdvanceLesson: (Course) -> Unit,
    onExploreCourses: () -> Unit,
) {
    val strings = LocalAppStrings.current
    val overallProgress = if (activeCourses.isEmpty()) 0 else activeCourses.map { it.progressPercent }.average().toInt()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("active_learning_progress_card"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = strings.activeLearningTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${strings.overallActiveProgressLabel}: $overallProgress%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Surface(
                    color = AcademyTeal.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Text(
                        text = "$overallProgress%",
                        color = AcademyTeal,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            if (activeCourses.isEmpty()) {
                OutlinedButton(
                    onClick = onExploreCourses,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(strings.availableCoursesTitle)
                }
            } else {
                activeCourses.forEachIndexed { idx, course ->
                    val barColor = when (idx % 3) {
                        0 -> AcademyTeal
                        1 -> AcademySky
                        else -> EmeraldSuccess
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onAdvanceLesson(course) }
                            .padding(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = course.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.size(8.dp))
                            Text(
                                text = "${course.completedLessons}/${course.totalLessons} (${course.progressPercent}%)",
                                style = MaterialTheme.typography.labelMedium,
                                color = barColor
                            )
                        }
                        LinearProgressIndicator(
                            progress = { (course.progressPercent / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(9.dp)
                                .clip(RoundedCornerShape(5.dp)),
                            color = barColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AvailableCoursesHeader(
    strings: AppStrings,
    onOpenCourses: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.School,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = strings.availableCoursesTitle,
                style = MaterialTheme.typography.titleLarge
            )
        }
        TextButton(
            onClick = onOpenCourses,
            modifier = Modifier.testTag("available_courses_view_all")
        ) {
            Text(strings.viewAllBtn)
            Spacer(Modifier.size(4.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun AvailableCourseRowCard(
    course: Course,
    strings: AppStrings,
    onOpenCourses: () -> Unit,
) {
    val thumbRes = if (course.imageKey == "code") R.drawable.img_course_code else R.drawable.img_course_ai
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenCourses)
            .testTag("dashboard_available_course_${course.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AcademyAsyncImage(
                resId = thumbRes,
                contentDescription = course.title,
                modifier = Modifier
                    .size(78.dp)
                    .clip(RoundedCornerShape(14.dp)),
                contentScale = ContentScale.Crop
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = course.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = ScholarGold,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = String.format(Locale.US, "%.1f", course.rating),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
                Text(
                    text = course.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = course.instructor,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Surface(
                        color = if (course.enrolled) {
                            EmeraldSuccess.copy(alpha = 0.14f)
                        } else {
                            MaterialTheme.colorScheme.secondaryContainer
                        },
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Text(
                            text = when {
                                course.enrolled -> strings.enrolledBadge
                                course.priceSar == 0L -> strings.freeBadge
                                else -> "${course.priceSar} SAR"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (course.enrolled) EmeraldSuccess else MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }
    }
}
