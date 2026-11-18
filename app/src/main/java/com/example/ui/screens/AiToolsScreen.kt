package com.example.ui.screens

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.LocalAppStrings
import com.example.data.model.AiFlashcardsUiState
import com.example.data.model.AiImageUiState
import com.example.data.model.AiQuizUiState
import com.example.data.model.AiToolTab
import com.example.data.model.AiTutorPersona
import com.example.data.model.AiUiState
import com.example.data.model.Course
import com.example.data.model.SupportChatEntry
import com.example.ui.theme.AcademyTeal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

@Composable
fun AiToolsScreen(
    initialTab: AiToolTab,
    courses: List<Course>,
    selectedPersona: AiTutorPersona,
    tutorHistory: List<SupportChatEntry>,
    tutorAiState: AiUiState,
    visionAiState: AiUiState,
    quizAiState: AiQuizUiState,
    flashcardsAiState: AiFlashcardsUiState,
    visualAidState: AiImageUiState,
    onSelectPersona: (AiTutorPersona) -> Unit,
    onClearTutorChat: () -> Unit,
    onAskTutor: (String, String) -> Unit,
    onAnalyzeImage: (Bitmap, String) -> Unit,
    onGenerateQuiz: (String, String, String) -> Unit,
    onSaveQuizResult: (String, Int) -> Unit,
    onGenerateFlashcards: (String) -> Unit,
    onGenerateVisual: (String, Bitmap?) -> Unit,
    onBack: () -> Unit,
) {
    val strings = LocalAppStrings.current
    var activeTool by remember(initialTab) { mutableStateOf(initialTab) }
    var selectedCourseId by remember { mutableStateOf<String?>(courses.firstOrNull { it.enrolled }?.id) }
    val selectedCourse = remember(courses, selectedCourseId) { courses.firstOrNull { it.id == selectedCourseId } }

    BackHandler(onBack = onBack)

    Column(modifier = Modifier.fillMaxSize()) {
        // Studio Header
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("ai_studio_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = strings.backLabel
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = strings.aiStudioTitle,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = strings.aiStudioSubtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Tool Switcher Row
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(AiToolTab.entries) { tool ->
                        val (label, icon) = toolMeta(tool, strings.isArabic, strings)
                        FilterChip(
                            selected = activeTool == tool,
                            onClick = { activeTool = tool },
                            label = { Text(label) },
                            leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            modifier = Modifier.testTag("ai_tool_tab_${tool.name.lowercase()}")
                        )
                    }
                }

                // Course Context Grounding Chips
                if (courses.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item {
                            FilterChip(
                                selected = selectedCourseId == null,
                                onClick = { selectedCourseId = null },
                                label = { Text(if (strings.isArabic) "موضوع عام" else "General Topic") }
                            )
                        }
                        items(courses, key = { "ctx_${it.id}" }) { course ->
                            FilterChip(
                                selected = selectedCourseId == course.id,
                                onClick = { selectedCourseId = course.id },
                                label = { Text(course.title, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                            )
                        }
                    }
                }
            }
        }

        // Tool Body
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (activeTool) {
                AiToolTab.TUTOR -> AiTutorToolPane(
                    selectedCourse = selectedCourse,
                    selectedPersona = selectedPersona,
                    history = tutorHistory,
                    aiState = tutorAiState,
                    onSelectPersona = onSelectPersona,
                    onClearChat = onClearTutorChat,
                    onSendQuestion = { q ->
                        val ctx = selectedCourse?.let { "${it.title} (${it.category}) - ${it.description}" }.orEmpty()
                        onAskTutor(q, ctx)
                    }
                )

                AiToolTab.VISION -> AiVisionHomeworkPane(
                    visionState = visionAiState,
                    onAnalyzeImage = onAnalyzeImage
                )

                AiToolTab.QUIZ -> AiQuizMakerPane(
                    selectedCourse = selectedCourse,
                    quizState = quizAiState,
                    onGenerateQuiz = onGenerateQuiz,
                    onSaveQuizResult = onSaveQuizResult
                )

                AiToolTab.FLASHCARDS -> AiFlashcardsPane(
                    selectedCourse = selectedCourse,
                    flashcardsState = flashcardsAiState,
                    onGenerateFlashcards = onGenerateFlashcards
                )

                AiToolTab.VISUALS -> AiVisualAidStudioPane(
                    selectedCourse = selectedCourse,
                    visualState = visualAidState,
                    onGenerateVisual = onGenerateVisual
                )
            }
        }
    }
}

private fun toolMeta(
    tool: AiToolTab,
    isArabic: Boolean,
    strings: com.example.core.AppStrings,
): Pair<String, ImageVector> = when (tool) {
    AiToolTab.TUTOR -> strings.aiToolTutor to Icons.Default.Psychology
    AiToolTab.VISION -> strings.aiToolVision to Icons.Default.CameraAlt
    AiToolTab.QUIZ -> strings.aiToolQuiz to Icons.Default.Quiz
    AiToolTab.FLASHCARDS -> strings.aiToolFlashcards to Icons.Default.Style
    AiToolTab.VISUALS -> strings.aiToolVisuals to Icons.Default.Image
}

@Composable
private fun AiTutorToolPane(
    selectedCourse: Course?,
    selectedPersona: AiTutorPersona,
    history: List<SupportChatEntry>,
    aiState: AiUiState,
    onSelectPersona: (AiTutorPersona) -> Unit,
    onClearChat: () -> Unit,
    onSendQuestion: (String) -> Unit,
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    var draft by remember { mutableStateOf("") }
    var voiceNotice by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()

    var ttsReady by remember { mutableStateOf(false) }
    val tts = remember(context) {
        var engine: TextToSpeech? = null
        engine = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                engine?.language = if (strings.isArabic) Locale("ar") else Locale.US
                ttsReady = true
            }
        }
        engine
    }
    DisposableEffect(tts) {
        onDispose {
            tts.stop()
            tts.shutdown()
        }
    }

    val voiceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
                .orEmpty()
            if (spoken.isNotBlank()) {
                draft = spoken
                voiceNotice = null
            }
        }
    }

    LaunchedEffect(history.size, aiState) {
        if (history.isNotEmpty()) {
            listState.animateScrollToItem(history.lastIndex)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            LazyRow(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(AiTutorPersona.entries) { persona ->
                    FilterChip(
                        selected = selectedPersona == persona,
                        onClick = { onSelectPersona(persona) },
                        label = { Text(if (strings.isArabic) persona.labelAr else persona.labelEn) }
                    )
                }
            }
            IconButton(onClick = onClearChat) {
                Icon(Icons.Default.Refresh, contentDescription = strings.clearChatBtn)
            }
        }

        selectedCourse?.let { course ->
            Text(
                text = if (strings.isArabic) "السياق المرتبط: ${course.title}" else "Course context: ${course.title}",
                style = MaterialTheme.typography.labelSmall,
                color = AcademyTeal,
                modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth().padding(bottom = 4.dp)
            )
        }

        if (voiceNotice != null) {
            Text(
                text = voiceNotice!!,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth().padding(bottom = 4.dp)
            )
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .widthIn(max = 680.dp)
                .fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(history, key = { it.id }) { entry ->
                TutorBubble(
                    isUser = entry.isUser,
                    text = entry.text,
                    canSpeak = !entry.isUser && ttsReady,
                    speakLabel = strings.readAloudBtn,
                    onSpeak = {
                        tts.language = if (strings.isArabic) Locale("ar") else Locale.US
                        tts.speak(entry.text, TextToSpeech.QUEUE_FLUSH, null, entry.id)
                    }
                )
            }
            if (aiState is AiUiState.Loading) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(8.dp)
                    ) {
                        CircularProgressIndicator(Modifier.size(18.dp))
                        Text(if (strings.isArabic) "يفكر المعلم الذكي..." else "AI Tutor is thinking...")
                    }
                }
            } else if (aiState is AiUiState.Streaming) {
                item {
                    TutorBubble(
                        isUser = false,
                        text = aiState.text,
                        canSpeak = false,
                        speakLabel = strings.readAloudBtn,
                        onSpeak = {}
                    )
                }
            } else if (aiState is AiUiState.Error) {
                item { ErrorCard(aiState.message) }
            }
        }

        Row(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            IconButton(
                onClick = {
                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, if (strings.isArabic) "ar-SA" else "en-US")
                        putExtra(RecognizerIntent.EXTRA_PROMPT, strings.voiceInputBtn)
                    }
                    try {
                        voiceLauncher.launch(intent)
                    } catch (_: ActivityNotFoundException) {
                        voiceNotice = if (strings.isArabic) {
                            "التعرف الصوتي غير متوفر على هذا المحاكي؛ اكتب سؤالك مباشرة."
                        } else {
                            "Speech input unavailable on this device; please type your question."
                        }
                    }
                },
                modifier = Modifier.testTag("ai_tutor_voice_btn")
            ) {
                Icon(Icons.Default.Mic, contentDescription = strings.voiceInputBtn, tint = AcademyTeal)
            }
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                placeholder = {
                    Text(if (strings.isArabic) "اسأل المعلم الذكي..." else "Ask the AI Tutor...")
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("ai_tutor_input"),
                shape = RoundedCornerShape(16.dp),
                maxLines = 3
            )
            FilledIconButton(
                onClick = {
                    if (draft.isNotBlank() && aiState !is AiUiState.Loading && aiState !is AiUiState.Streaming) {
                        onSendQuestion(draft)
                        draft = ""
                    }
                },
                modifier = Modifier
                    .size(50.dp)
                    .testTag("ai_tutor_send_btn")
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = strings.sendBtn)
            }
        }
    }
}

@Composable
private fun TutorBubble(
    isUser: Boolean,
    text: String,
    canSpeak: Boolean,
    speakLabel: String,
    onSpeak: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 330.dp),
            shape = RoundedCornerShape(16.dp),
            color = if (isUser) AcademyTeal else MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface
                )
                if (canSpeak) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = onSpeak, contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)) {
                            Icon(Icons.Default.VolumeUp, contentDescription = speakLabel, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.size(4.dp))
                            Text(speakLabel, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AiVisionHomeworkPane(
    visionState: AiUiState,
    onAnalyzeImage: (Bitmap, String) -> Unit,
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pickedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var question by remember { mutableStateOf("") }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                pickedBitmap = withContext(Dispatchers.IO) {
                    decodeSampledBitmapFromUri(context, uri, maxDimension = 1280)
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
                        Icon(Icons.Default.CameraAlt, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(strings.aiToolVision, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = if (strings.isArabic) {
                            "اختر صورة مسألة رياضية، رسم بياني، أو صفحة من الكتاب ليقوم الذكاء الاصطناعي بشرحها خطوة بخطوة."
                        } else {
                            "Pick a photo of a math problem, diagram, or textbook page for step-by-step AI explanation."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = {
                            photoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("pick_homework_image_btn"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(8.dp))
                        Text(strings.pickImageBtn)
                    }

                    pickedBitmap?.let { bmp ->
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = strings.aiToolVision,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(14.dp)),
                            contentScale = ContentScale.Fit
                        )
                    }

                    OutlinedTextField(
                        value = question,
                        onValueChange = { question = it },
                        label = { Text(if (strings.isArabic) "سؤالك حول الصورة (اختياري)" else "Your question about the image (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )

                    Button(
                        onClick = { pickedBitmap?.let { onAnalyzeImage(it, question) } },
                        enabled = pickedBitmap != null && visionState !is AiUiState.Loading && visionState !is AiUiState.Streaming,
                        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("analyze_homework_image_btn"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(8.dp))
                        Text(strings.analyzeImageBtn)
                    }
                }
            }
        }

        item {
            Box(Modifier.widthIn(max = 680.dp).fillMaxWidth()) {
                when (visionState) {
                    is AiUiState.Idle -> EmptyStateCard(
                        if (strings.isArabic) "ارفع صورة تعليمية ثم اضغط تحليل بالذكاء الاصطناعي." else "Select an educational image and tap Analyze."
                    )
                    is AiUiState.Loading -> Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(12.dp)
                    ) {
                        CircularProgressIndicator(Modifier.size(22.dp))
                        Text(if (strings.isArabic) "جاري قراءة الصورة وتحليلها..." else "Analyzing image...")
                    }
                    is AiUiState.Streaming -> Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(18.dp),
                        tonalElevation = 2.dp
                    ) {
                        Text(visionState.text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(16.dp))
                    }
                    is AiUiState.Success -> Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Text(visionState.text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(16.dp))
                    }
                    is AiUiState.Error -> ErrorCard(visionState.message)
                }
            }
        }
    }
}

internal fun decodeSampledBitmapFromUri(context: Context, uri: Uri, maxDimension: Int): Bitmap? {
    return try {
        val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, boundsOpts)
        }
        var sampleSize = 1
        val maxSide = maxOf(boundsOpts.outWidth, boundsOpts.outHeight)
        while (maxSide / sampleSize > maxDimension) {
            sampleSize *= 2
        }
        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOpts)
        }
    } catch (_: Exception) {
        null
    }
}
