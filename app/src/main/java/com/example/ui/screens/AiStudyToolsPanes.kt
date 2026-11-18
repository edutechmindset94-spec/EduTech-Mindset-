package com.example.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.LocalAppStrings
import com.example.data.model.AiFlashcardsUiState
import com.example.data.model.AiImageUiState
import com.example.data.model.AiQuizUiState
import com.example.data.model.Course
import com.example.ui.theme.AcademyTeal
import com.example.ui.theme.CoralAlert
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.ScholarGold
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiQuizMakerPane(
    selectedCourse: Course?,
    quizState: AiQuizUiState,
    onGenerateQuiz: (String, String, String) -> Unit,
    onSaveQuizResult: (String, Int) -> Unit,
) {
    val strings = LocalAppStrings.current
    var topic by remember(selectedCourse?.id) {
        mutableStateOf(selectedCourse?.title ?: "")
    }
    var gradeLevel by remember { mutableStateOf(if (strings.isArabic) "المرحلة الثانوية" else "High School") }
    var difficulty by remember { mutableStateOf("Medium") }
    val selectedAnswers = remember(quizState) { mutableStateMapOf<Int, Int>() }
    var submitted by remember(quizState) { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Card(
                modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Quiz, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(strings.aiToolQuiz, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    OutlinedTextField(
                        value = topic,
                        onValueChange = { topic = it },
                        label = { Text(if (strings.isArabic) "موضوع الاختبار أو الدرس" else "Quiz Topic") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("ai_quiz_topic_input")
                    )
                    OutlinedTextField(
                        value = gradeLevel,
                        onValueChange = { gradeLevel = it },
                        label = { Text(if (strings.isArabic) "المستوى الدراسي" else "Grade Level") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            "Easy" to (if (strings.isArabic) "مبتدئ" else "Easy"),
                            "Medium" to (if (strings.isArabic) "متوسط" else "Medium"),
                            "Advanced" to (if (strings.isArabic) "متقدم" else "Advanced")
                        ).forEach { (key, label) ->
                            FilterChip(
                                selected = difficulty == key,
                                onClick = { difficulty = key },
                                label = { Text(label) }
                            )
                        }
                    }
                    Button(
                        onClick = { onGenerateQuiz(topic, gradeLevel, difficulty) },
                        enabled = topic.trim().length >= 2 && quizState !is AiQuizUiState.Loading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("generate_ai_quiz_btn"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(8.dp))
                        Text(strings.generateQuizBtn)
                    }
                }
            }
        }

        when (quizState) {
            is AiQuizUiState.Idle -> Unit
            is AiQuizUiState.Loading -> item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    CircularProgressIndicator(Modifier.size(22.dp))
                    Text(if (strings.isArabic) "جاري إعداد أسئلة الاختبار الذكي..." else "Generating interactive quiz...")
                }
            }
            is AiQuizUiState.Error -> item {
                Box(Modifier.widthIn(max = 680.dp).fillMaxWidth()) { ErrorCard(quizState.message) }
            }
            is AiQuizUiState.Success -> {
                itemsIndexed(quizState.questions, key = { idx, _ -> "quiz_q_$idx" }) { qIdx, q ->
                    Card(
                        modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "${qIdx + 1}. ${q.question}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            q.options.forEachIndexed { oIdx, optionText ->
                                val isChosen = selectedAnswers[qIdx] == oIdx
                                val isCorrect = oIdx == q.correctIndex
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable(enabled = !submitted) { selectedAnswers[qIdx] = oIdx }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isChosen,
                                        onClick = { if (!submitted) selectedAnswers[qIdx] = oIdx }
                                    )
                                    Text(
                                        text = optionText,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = when {
                                            submitted && isCorrect -> EmeraldSuccess
                                            submitted && isChosen && !isCorrect -> CoralAlert
                                            else -> MaterialTheme.colorScheme.onSurface
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                            if (submitted && q.explanation.isNotBlank()) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        text = q.explanation,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    val correctCount = quizState.questions.indices.count { selectedAnswers[it] == quizState.questions[it].correctIndex }
                    val scorePct = if (quizState.questions.isEmpty()) 0 else (correctCount * 100) / quizState.questions.size
                    Card(
                        modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (!submitted) {
                                Button(
                                    onClick = { submitted = true },
                                    enabled = selectedAnswers.size == quizState.questions.size,
                                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("submit_ai_quiz_btn"),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.size(8.dp))
                                    Text(if (strings.isArabic) "تصحيح الاختبار وعرض الشرح" else "Check Answers & Explanations")
                                }
                            } else {
                                Text(
                                    text = if (strings.isArabic) {
                                        "النتيجة النهائية: $correctCount / ${quizState.questions.size} ($scorePct%)"
                                    } else {
                                        "Final Score: $correctCount / ${quizState.questions.size} ($scorePct%)"
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (scorePct >= 70) EmeraldSuccess else ScholarGold,
                                    fontWeight = FontWeight.Bold
                                )
                                OutlinedButton(
                                    onClick = { onSaveQuizResult(quizState.topic, scorePct) },
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.BookmarkAdded, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.size(8.dp))
                                    Text(if (strings.isArabic) "حفظ النتيجة في سجل التنبيهات" else "Save Score to Alerts")
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
fun AiFlashcardsPane(
    selectedCourse: Course?,
    flashcardsState: AiFlashcardsUiState,
    onGenerateFlashcards: (String) -> Unit,
) {
    val strings = LocalAppStrings.current
    var sourceText by remember(selectedCourse?.id) {
        mutableStateOf(
            if (selectedCourse != null) {
                "${selectedCourse.title}: ${selectedCourse.description}"
            } else {
                ""
            }
        )
    }
    val flippedCards = remember(flashcardsState) { mutableStateMapOf<Int, Boolean>() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Card(
                modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Style, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(strings.aiToolFlashcards, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    OutlinedTextField(
                        value = sourceText,
                        onValueChange = { sourceText = it },
                        label = {
                            Text(
                                if (strings.isArabic) "الصق نص الدرس أو اكتب الموضوع المراد تلخيصه" else "Paste lesson notes or topic to summarize"
                            )
                        },
                        minLines = 3,
                        maxLines = 6,
                        modifier = Modifier.fillMaxWidth().testTag("ai_flashcards_source_input")
                    )
                    Button(
                        onClick = { onGenerateFlashcards(sourceText) },
                        enabled = sourceText.trim().length >= 5 && flashcardsState !is AiFlashcardsUiState.Loading,
                        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("generate_flashcards_btn"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(8.dp))
                        Text(strings.generateFlashcardsBtn)
                    }
                }
            }
        }

        when (flashcardsState) {
            is AiFlashcardsUiState.Idle -> Unit
            is AiFlashcardsUiState.Loading -> item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    CircularProgressIndicator(Modifier.size(22.dp))
                    Text(if (strings.isArabic) "جاري التلخيص وبناء البطاقات..." else "Summarizing & building flashcards...")
                }
            }
            is AiFlashcardsUiState.Error -> item {
                Box(Modifier.widthIn(max = 680.dp).fillMaxWidth()) { ErrorCard(flashcardsState.message) }
            }
            is AiFlashcardsUiState.Success -> {
                if (flashcardsState.summary.isNotBlank()) {
                    item {
                        Card(
                            modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = if (strings.isArabic) "الملخص المركز" else "Executive Summary",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(flashcardsState.summary, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }

                itemsIndexed(flashcardsState.cards, key = { idx, _ -> "flashcard_$idx" }) { idx, card ->
                    val isFlipped = flippedCards[idx] == true
                    Card(
                        modifier = Modifier
                            .widthIn(max = 680.dp)
                            .fillMaxWidth()
                            .clickable { flippedCards[idx] = !isFlipped }
                            .testTag("flashcard_item_$idx"),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isFlipped) {
                                MaterialTheme.colorScheme.secondaryContainer
                            } else {
                                MaterialTheme.colorScheme.surface
                            }
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isFlipped) {
                                        if (strings.isArabic) "الإجابة والشرح" else "Answer"
                                    } else {
                                        if (strings.isArabic) "السؤال / المفهوم #${idx + 1}" else "Concept #${idx + 1}"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = AcademyTeal,
                                    fontWeight = FontWeight.Bold
                                )
                                Icon(
                                    Icons.Default.Flip,
                                    contentDescription = if (strings.isArabic) "قلب البطاقة" else "Flip card",
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = if (isFlipped) card.back else card.front,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (isFlipped && card.memoryTip.isNotBlank()) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Default.Lightbulb, contentDescription = null, tint = ScholarGold, modifier = Modifier.size(16.dp))
                                    Text(
                                        text = card.memoryTip,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiVisualAidStudioPane(
    selectedCourse: Course?,
    visualState: AiImageUiState,
    onGenerateVisual: (String, Bitmap?) -> Unit,
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var prompt by remember(selectedCourse?.id) {
        mutableStateOf(
            if (selectedCourse != null) {
                if (strings.isArabic) "مخطط تعليمي يوضح مفاهيم ${selectedCourse.title}" else "Educational diagram illustrating ${selectedCourse.title}"
            } else {
                ""
            }
        )
    }
    var referenceBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                referenceBitmap = withContext(Dispatchers.IO) {
                    decodeSampledBitmapFromUri(context, uri, maxDimension = 1024)
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Card(
                modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Image, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(strings.aiToolVisuals, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = if (strings.isArabic) {
                            "أنشئ رسوماً توضيحية ومخططات تعليمية بالذكاء الاصطناعي، أو اختر صورة لتعديلها تربوياً."
                        } else {
                            "Create educational diagrams & visual aids with AI, or pick a photo to enhance."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val presets = if (strings.isArabic) {
                            listOf("دورة الماء في الطبيعة برسم تعليمي", "تركيب الخلية النباتية مع تسميات واضحة", "خريطة ذهنية لمفاهيم الذكاء الاصطناعي")
                        } else {
                            listOf("Water cycle educational infographic", "Plant cell structure clean diagram", "AI neural network visual diagram")
                        }
                        presets.forEach { preset ->
                            AssistChip(onClick = { prompt = preset }, label = { Text(preset) })
                        }
                    }
                    OutlinedTextField(
                        value = prompt,
                        onValueChange = { prompt = it },
                        label = { Text(if (strings.isArabic) "صف الرسم أو الوسيلة التعليمية" else "Describe educational visual") },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth().testTag("ai_visual_prompt_input")
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(
                            onClick = {
                                pickImageLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier.height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(if (strings.isArabic) "صورة مرجعية (اختياري)" else "Reference Photo")
                        }
                        if (referenceBitmap != null) {
                            TextButton(onClick = { referenceBitmap = null }) {
                                Text(if (strings.isArabic) "إزالة" else "Clear")
                            }
                        }
                    }
                    referenceBitmap?.let { bmp ->
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Button(
                        onClick = { onGenerateVisual(prompt, referenceBitmap) },
                        enabled = prompt.trim().length >= 3 && visualState !is AiImageUiState.Loading,
                        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("generate_visual_aid_btn"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(8.dp))
                        Text(strings.generateVisualBtn)
                    }
                }
            }
        }

        item {
            Box(modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth()) {
                when (visualState) {
                    is AiImageUiState.Idle -> EmptyStateCard(
                        if (strings.isArabic) "أدخل وصف الوسيلة البصرية لإنشائها بالذكاء الاصطناعي." else "Enter a prompt to generate an educational diagram."
                    )
                    is AiImageUiState.Loading -> Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator()
                            Text(
                                text = if (strings.isArabic) "جاري رسم الوسيلة التعليمية (قد يستغرق 30-60 ثانية)..." else "Synthesizing educational visual (30-60s)...",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    is AiImageUiState.Success -> Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Image(
                                bitmap = visualState.bitmap.asImageBitmap(),
                                contentDescription = visualState.prompt,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp)),
                                contentScale = ContentScale.Fit
                            )
                            Text(
                                text = visualState.prompt,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    is AiImageUiState.Error -> ErrorCard(visualState.message)
                }
            }
        }
    }
}
