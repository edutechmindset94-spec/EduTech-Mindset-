package com.example.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.data.model.AiUiState
import com.example.data.model.Course
import com.example.data.model.LessonPlan
import com.example.data.model.PaymentGateway
import com.example.data.model.QuizQuestion
import com.example.data.model.UiState
import com.example.ui.components.AcademyAsyncImage
import com.example.ui.theme.AcademyTeal
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.ScholarGold
import java.util.Locale

private val CurriculumQuizQuestions = listOf(
    QuizQuestion(
        questionAr = "ما الهدف الرئيسي من توظيف التحليلات التربوية في التعليم الذكي؟",
        questionEn = "What is the main goal of learning analytics in smart education?",
        optionsAr = listOf("تخصيص المسار التعليمي ودعم المتعلم مبكراً", "تقليل التواصل مع المدرسة", "استبدال المعلم بالكامل"),
        optionsEn = listOf("Personalize learning & support early", "Reduce school communication", "Replace teachers completely"),
        correctIndex = 0
    ),
    QuizQuestion(
        questionAr = "لماذا نعتمد على التخزين المحلي المؤقت في المنصات التعليمية؟",
        questionEn = "Why do educational platforms use offline persistent caching?",
        optionsAr = listOf("استمرار التعلم دون انقطاع والمزامنة لاحقاً", "حذف البيانات القديمة", "إيقاف التحديثات"),
        optionsEn = listOf("Continue learning offline & sync later", "Delete old data", "Stop updates"),
        correctIndex = 0
    ),
    QuizQuestion(
        questionAr = "أي معيار يضمن حماية سرية المحادثات والملفات التعليمية؟",
        questionEn = "Which standard protects confidentiality of educational chats & files?",
        optionsAr = listOf("تشفير AES-256-GCM وقواعد العزل", "النص المفتوح", "تجاهل المصادقة"),
        optionsEn = listOf("AES-256-GCM & isolation rules", "Plaintext", "Skip authentication"),
        correctIndex = 0
    )
)

@Composable
fun CoursesScreen(
    coursesState: UiState<List<Course>>,
    lessonPlansState: UiState<List<LessonPlan>>,
    lessonPlanAiState: AiUiState,
    onCreateCourse: (String, String, String, String, Long, Long) -> Unit,
    onAdvanceLesson: (Course) -> Unit,
    onCompletePayment: (Course, PaymentGateway, String) -> Unit,
    onSubmitEvaluation: (String, Double, String, Double) -> Unit,
    onGenerateAiPlan: (String, String) -> Unit,
    onSaveManualPlan: (String, String, String, String, String, String) -> Unit,
) {
    val strings = LocalAppStrings.current
    var selectedSection by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }

    var showCreateCourseDialog by remember { mutableStateOf(false) }
    var paymentTargetCourse by remember { mutableStateOf<Course?>(null) }
    var evaluationTargetCourse by remember { mutableStateOf<Course?>(null) }
    var showAiPlanDialog by remember { mutableStateOf(false) }
    var showManualPlanDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(12.dp))
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxWidth()
        ) {
            SegmentedButton(
                selected = selectedSection == 0,
                onClick = { selectedSection = 0 },
                shape = SegmentedButtonDefaults.itemShape(0, 2),
                modifier = Modifier.testTag("tab_courses_catalog")
            ) {
                Text(strings.coursesCatalogTitle)
            }
            SegmentedButton(
                selected = selectedSection == 1,
                onClick = { selectedSection = 1 },
                shape = SegmentedButtonDefaults.itemShape(1, 2),
                modifier = Modifier.testTag("tab_lesson_planner")
            ) {
                Text(strings.lessonPlannerTitle)
            }
        }
        Spacer(Modifier.height(12.dp))

        if (selectedSection == 0) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    Row(
                        modifier = Modifier
                            .widthIn(max = 680.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            label = { Text(if (strings.isArabic) "ابحث عن دورة..." else "Search courses...") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("search_courses_input"),
                            shape = RoundedCornerShape(14.dp)
                        )
                        Button(
                            onClick = { showCreateCourseDialog = true },
                            modifier = Modifier
                                .height(54.dp)
                                .testTag("create_course_button"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.size(4.dp))
                            Text(strings.createCourseBtn)
                        }
                    }
                }

                when (coursesState) {
                    is UiState.Loading -> item { CircularProgressIndicator(Modifier.padding(24.dp)) }
                    is UiState.Error -> item {
                        Box(Modifier.widthIn(max = 680.dp)) { ErrorCard(coursesState.message) }
                    }
                    is UiState.Success -> {
                        val filtered = coursesState.data.filter {
                            searchQuery.isBlank() ||
                                it.title.contains(searchQuery, ignoreCase = true) ||
                                it.category.contains(searchQuery, ignoreCase = true)
                        }
                        if (filtered.isEmpty()) {
                            item {
                                Box(Modifier.widthIn(max = 680.dp)) { EmptyStateCard(strings.emptyCoursesMsg) }
                            }
                        } else {
                            items(filtered, key = { it.id }) { course ->
                                CourseCard(
                                    course = course,
                                    isArabic = strings.isArabic,
                                    enrollPayLabel = strings.enrollPayBtn,
                                    enrolledBadge = strings.enrolledBadge,
                                    freeBadge = strings.freeBadge,
                                    continueLabel = strings.continueLessonBtn,
                                    evaluateLabel = strings.evaluateQuizBtn,
                                    onEnrollClick = { paymentTargetCourse = course },
                                    onAdvanceLesson = { onAdvanceLesson(course) },
                                    onEvaluateClick = { evaluationTargetCourse = course }
                                )
                            }
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    Row(
                        modifier = Modifier
                            .widthIn(max = 680.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showAiPlanDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("ai_lesson_plan_button"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.size(6.dp))
                            Text(strings.generatePlanAiBtn)
                        }
                        OutlinedButton(
                            onClick = { showManualPlanDialog = true },
                            modifier = Modifier
                                .height(50.dp)
                                .testTag("manual_lesson_plan_button"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.size(6.dp))
                            Text(strings.addManualPlanBtn)
                        }
                    }
                }

                if (lessonPlanAiState is AiUiState.Loading) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(8.dp)
                        ) {
                            CircularProgressIndicator(Modifier.size(22.dp))
                            Text(if (strings.isArabic) "جاري إعداد المخطط الدراسي الذكي..." else "Generating AI lesson plan...")
                        }
                    }
                } else if (lessonPlanAiState is AiUiState.Error) {
                    item {
                        Box(Modifier.widthIn(max = 680.dp)) { ErrorCard(lessonPlanAiState.message) }
                    }
                }

                when (lessonPlansState) {
                    is UiState.Loading -> item { CircularProgressIndicator(Modifier.padding(24.dp)) }
                    is UiState.Error -> item {
                        Box(Modifier.widthIn(max = 680.dp)) { ErrorCard(lessonPlansState.message) }
                    }
                    is UiState.Success -> {
                        if (lessonPlansState.data.isEmpty()) {
                            item {
                                Box(Modifier.widthIn(max = 680.dp)) { EmptyStateCard(strings.emptyPlansMsg) }
                            }
                        } else {
                            items(lessonPlansState.data, key = { it.id }) { plan ->
                                LessonPlanCard(plan = plan, isArabic = strings.isArabic)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateCourseDialog) {
        CreateCourseDialog(
            isArabic = strings.isArabic,
            onDismiss = { showCreateCourseDialog = false },
            onConfirm = { title, inst, cat, desc, price, lessons ->
                onCreateCourse(title, inst, cat, desc, price, lessons)
                showCreateCourseDialog = false
            }
        )
    }

    paymentTargetCourse?.let { target ->
        PaymentCheckoutDialog(
            course = target,
            isArabic = strings.isArabic,
            title = strings.paymentGatewayTitle,
            confirmLabel = strings.confirmPaymentBtn,
            onDismiss = { paymentTargetCourse = null },
            onConfirm = { gateway, maskedCard ->
                onCompletePayment(target, gateway, maskedCard)
                paymentTargetCourse = null
            }
        )
    }

    evaluationTargetCourse?.let { target ->
        CourseEvaluationQuizDialog(
            course = target,
            isArabic = strings.isArabic,
            onDismiss = { evaluationTargetCourse = null },
            onSubmit = { rating, note, quizScore ->
                onSubmitEvaluation(target.id, rating, note, quizScore)
                evaluationTargetCourse = null
            }
        )
    }

    if (showAiPlanDialog) {
        AiLessonPlanDialog(
            isArabic = strings.isArabic,
            onDismiss = { showAiPlanDialog = false },
            onGenerate = { topic, grade ->
                onGenerateAiPlan(topic, grade)
                showAiPlanDialog = false
            }
        )
    }

    if (showManualPlanDialog) {
        ManualLessonPlanDialog(
            isArabic = strings.isArabic,
            onDismiss = { showManualPlanDialog = false },
            onSave = { title, subject, grade, obj, act, assess ->
                onSaveManualPlan(title, subject, grade, obj, act, assess)
                showManualPlanDialog = false
            }
        )
    }
}

@Composable
private fun CourseCard(
    course: Course,
    isArabic: Boolean,
    enrollPayLabel: String,
    enrolledBadge: String,
    freeBadge: String,
    continueLabel: String,
    evaluateLabel: String,
    onEnrollClick: () -> Unit,
    onAdvanceLesson: () -> Unit,
    onEvaluateClick: () -> Unit
) {
    val thumbRes = if (course.imageKey == "code") R.drawable.img_course_code else R.drawable.img_course_ai

    Card(
        modifier = Modifier
            .widthIn(max = 680.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column {
            AcademyAsyncImage(
                resId = thumbRes,
                contentDescription = course.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(145.dp),
                contentScale = ContentScale.Crop
            )
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = course.category,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = ScholarGold, modifier = Modifier.size(16.dp))
                        Text(text = String.format(Locale.US, "%.1f", course.rating), style = MaterialTheme.typography.labelMedium)
                    }
                }

                Spacer(Modifier.height(8.dp))
                Text(course.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(course.instructor, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                if (course.description.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        course.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(10.dp))

                if (course.enrolled) {
                    LinearProgressIndicator(
                        progress = { (course.progressPercent / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = AcademyTeal
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (isArabic) {
                                "${course.completedLessons} / ${course.totalLessons} دروس (${course.progressPercent}%)"
                            } else {
                                "${course.completedLessons} / ${course.totalLessons} lessons (${course.progressPercent}%)"
                            },
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = enrolledBadge,
                            color = EmeraldSuccess,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onAdvanceLesson,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(continueLabel, maxLines = 1)
                        }
                        OutlinedButton(
                            onClick = onEvaluateClick,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Quiz, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.size(4.dp))
                            Text(evaluateLabel, maxLines = 1)
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (course.priceSar == 0L) freeBadge else "${course.priceSar} SAR",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Button(
                            onClick = onEnrollClick,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("enroll_pay_button_${course.id}")
                        ) {
                            Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.size(6.dp))
                            Text(enrollPayLabel)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LessonPlanCard(plan: LessonPlan, isArabic: Boolean) {
    Card(
        modifier = Modifier
            .widthIn(max = 680.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(plan.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if (plan.aiGenerated) {
                    AssistChip(
                        onClick = {},
                        label = { Text("Gemini AI") },
                        leadingIcon = { Icon(Icons.Default.AutoAwesome, null, Modifier.size(14.dp)) }
                    )
                }
            }
            Text(
                text = "${plan.subject} • ${plan.gradeLevel}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = (if (isArabic) "الأهداف: " else "Objectives: ") + plan.objectives,
                style = MaterialTheme.typography.bodySmall
            )
            if (plan.activities.isNotBlank()) {
                Text(
                    text = (if (isArabic) "الأنشطة: " else "Activities: ") + plan.activities,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (plan.assessmentMethod.isNotBlank()) {
                Text(
                    text = (if (isArabic) "التقييم: " else "Assessment: ") + plan.assessmentMethod,
                    style = MaterialTheme.typography.labelSmall,
                    color = AcademyTeal
                )
            }
        }
    }
}

@Composable
private fun CreateCourseDialog(
    isArabic: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String, Long, Long) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var instructor by remember { mutableStateOf("أ. باسل الفقعاوي") }
    var category by remember { mutableStateOf("تقنيات التعليم") }
    var description by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("199") }
    var lessons by remember { mutableStateOf("10") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isArabic) "إضافة دورة تدريبية جديدة" else "Create Training Course") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(if (isArabic) "عنوان الدورة" else "Course Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = instructor,
                    onValueChange = { instructor = it },
                    label = { Text(if (isArabic) "المدرب / المشرف" else "Instructor") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text(if (isArabic) "التصنيف" else "Category") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(if (isArabic) "الوصف" else "Description") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = price,
                        onValueChange = { price = it.filter { ch -> ch.isDigit() } },
                        label = { Text(if (isArabic) "السعر (SAR)" else "Price (SAR)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = lessons,
                        onValueChange = { lessons = it.filter { ch -> ch.isDigit() } },
                        label = { Text(if (isArabic) "الدروس" else "Lessons") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        title,
                        instructor,
                        category,
                        description,
                        price.toLongOrNull() ?: 0L,
                        (lessons.toLongOrNull() ?: 8L).coerceAtLeast(1L)
                    )
                },
                enabled = title.trim().length >= 2
            ) {
                Text(if (isArabic) "إنشاء" else "Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(if (isArabic) "إلغاء" else "Cancel") }
        }
    )
}

@Composable
private fun PaymentCheckoutDialog(
    course: Course,
    isArabic: Boolean,
    title: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (PaymentGateway, String) -> Unit
) {
    var selectedGateway by remember { mutableStateOf(PaymentGateway.MADA) }
    var cardLastDigits by remember { mutableStateOf("4821") }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.CreditCard, contentDescription = null, tint = AcademyTeal) },
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(course.title, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = if (isArabic) "المبلغ المطلوب: ${course.priceSar} ر.س" else "Total: ${course.priceSar} SAR",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                PaymentGateway.entries.forEach { gateway ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedGateway == gateway,
                            onClick = { selectedGateway = gateway }
                        )
                        Text(if (isArabic) gateway.labelAr else gateway.labelEn)
                    }
                }
                OutlinedTextField(
                    value = cardLastDigits,
                    onValueChange = { cardLastDigits = it.filter { c -> c.isDigit() }.take(4) },
                    label = { Text(if (isArabic) "آخر 4 أرقام للبطاقة / المحفظة" else "Last 4 Digits") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val masked = "${selectedGateway.name} •••• ${cardLastDigits.ifBlank { "0000" }}"
                    onConfirm(selectedGateway, masked)
                },
                modifier = Modifier.testTag("confirm_payment_button")
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(if (isArabic) "إلغاء" else "Cancel") }
        }
    )
}

@Composable
private fun CourseEvaluationQuizDialog(
    course: Course,
    isArabic: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (Double, String, Double) -> Unit
) {
    var rating by remember { mutableFloatStateOf(course.rating.toFloat().coerceIn(1f, 5f)) }
    var note by remember { mutableStateOf(course.evaluationNote) }
    var selectedAnswers by remember { mutableStateOf(List(CurriculumQuizQuestions.size) { 0 }) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isArabic) "تقييم المنهج والاختبار التفاعلي" else "Curriculum Rating & Quiz") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    Text(
                        text = if (isArabic) "تقييم جودة المنهج: ${String.format(Locale.US, "%.1f", rating)} / 5" else "Rating: ${String.format(Locale.US, "%.1f", rating)} / 5",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Slider(value = rating, onValueChange = { rating = it }, valueRange = 1f..5f)
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text(if (isArabic) "ملاحظاتك على المنهج" else "Curriculum Feedback") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                items(CurriculumQuizQuestions.indices.toList()) { qIdx ->
                    val q = CurriculumQuizQuestions[qIdx]
                    Column {
                        Text(
                            text = "${qIdx + 1}. ${if (isArabic) q.questionAr else q.questionEn}",
                            style = MaterialTheme.typography.labelLarge
                        )
                        val opts = if (isArabic) q.optionsAr else q.optionsEn
                        opts.forEachIndexed { oIdx, opt ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = selectedAnswers[qIdx] == oIdx,
                                    onClick = {
                                        selectedAnswers = selectedAnswers.toMutableList().also { it[qIdx] = oIdx }
                                    }
                                )
                                Text(opt, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val correctCount = CurriculumQuizQuestions.indices.count {
                        selectedAnswers[it] == CurriculumQuizQuestions[it].correctIndex
                    }
                    val quizPercent = (correctCount * 100.0) / CurriculumQuizQuestions.size
                    onSubmit(rating.toDouble(), note, quizPercent.coerceAtLeast(60.0))
                }
            ) {
                Text(if (isArabic) "اعتماد التقييم" else "Submit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(if (isArabic) "إلغاء" else "Cancel") }
        }
    )
}

@Composable
private fun AiLessonPlanDialog(
    isArabic: Boolean,
    onDismiss: () -> Unit,
    onGenerate: (String, String) -> Unit
) {
    var topic by remember { mutableStateOf("") }
    var gradeLevel by remember { mutableStateOf(if (isArabic) "المرحلة الثانوية" else "High School") }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = AcademyTeal) },
        title = { Text(if (isArabic) "تحضير مخطط درس بالذكاء الاصطناعي" else "AI Lesson Plan Generator") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = topic,
                    onValueChange = { topic = it },
                    label = { Text(if (isArabic) "موضوع الدرس" else "Lesson Topic") },
                    modifier = Modifier.fillMaxWidth().testTag("ai_plan_topic_input")
                )
                OutlinedTextField(
                    value = gradeLevel,
                    onValueChange = { gradeLevel = it },
                    label = { Text(if (isArabic) "المرحلة الدراسية" else "Grade Level") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (topic.trim().length >= 2) onGenerate(topic.trim(), gradeLevel.trim()) },
                enabled = topic.trim().length >= 2
            ) {
                Text(if (isArabic) "توليد وحفظ" else "Generate & Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(if (isArabic) "إلغاء" else "Cancel") }
        }
    )
}

@Composable
private fun ManualLessonPlanDialog(
    isArabic: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("تقنيات التعليم") }
    var gradeLevel by remember { mutableStateOf("المستوى المتوسط") }
    var objectives by remember { mutableStateOf("") }
    var activities by remember { mutableStateOf("") }
    var assessment by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isArabic) "إنشاء مخطط دراسي يدوي" else "New Manual Lesson Plan") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(if (isArabic) "عنوان الدرس" else "Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text(if (isArabic) "المادة" else "Subject") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = gradeLevel,
                    onValueChange = { gradeLevel = it },
                    label = { Text(if (isArabic) "المرحلة" else "Grade") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = objectives,
                    onValueChange = { objectives = it },
                    label = { Text(if (isArabic) "الأهداف التعليمية" else "Objectives") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = activities,
                    onValueChange = { activities = it },
                    label = { Text(if (isArabic) "الأنشطة" else "Activities") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = assessment,
                    onValueChange = { assessment = it },
                    label = { Text(if (isArabic) "أسلوب التقييم" else "Assessment") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(title, subject, gradeLevel, objectives, activities, assessment) },
                enabled = title.trim().length >= 2 && objectives.trim().isNotEmpty()
            ) {
                Text(if (isArabic) "حفظ" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(if (isArabic) "إلغاء" else "Cancel") }
        }
    )
}
