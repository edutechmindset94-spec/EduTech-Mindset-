package com.example.data.repository

import android.graphics.Bitmap
import android.util.Log
import com.example.data.model.AiTutorPersona
import com.example.data.model.GeneratedFlashcard
import com.example.data.model.GeneratedQuizItem
import com.example.data.model.SupportChatEntry
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.ImagePart
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

data class GeneratedLessonPlanDraft(
    val title: String,
    val subject: String,
    val gradeLevel: String,
    val objectives: String,
    val activities: String,
    val assessmentMethod: String,
)

data class GeneratedFlashcardsBundle(
    val summary: String,
    val cards: List<GeneratedFlashcard>,
)

class AiEducationRepository {
    private val supportModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            systemInstruction = content {
                text(
                    "أنت المساعد الذكي للدعم الفني والأكاديمي على مدار الساعة في منصة EduTech Mindset " +
                        "(عقلية التعليم والتكنولوجيا) بإدارة الأستاذ باسل الفقعاوي. " +
                        "أجب بدقة ووضوح وبأسلوب تربوي محترف باللغة التي يكتب بها المستخدم (العربية أو الإنجليزية). " +
                        "ساعد الطلاب والمعلمين في الدورات، التخطيط الدراسي، التقييمات، وحلول المشكلات التقنية."
                )
            }
        )
    }

    private val lessonPlanSchema = Schema.obj(
        properties = mapOf(
            "title" to Schema.string(description = "Concise lesson plan title"),
            "subject" to Schema.string(description = "Academic subject or tech track"),
            "gradeLevel" to Schema.string(description = "Target grade or skill level"),
            "objectives" to Schema.string(description = "Clear measurable learning objectives"),
            "activities" to Schema.string(description = "Interactive classroom or digital activities"),
            "assessmentMethod" to Schema.string(description = "Formative or summative assessment method")
        )
    )

    private val lessonPlanModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = lessonPlanSchema
            }
        )
    }

    private val analyticsModel by lazy {
        Firebase.ai.generativeModel(modelName = "gemini-3.8-flash")
    }

    private val visionModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            systemInstruction = content {
                text(
                    "أنت محلل واجبات ومناهج تعليمية ذكي في EduTech Mindset. " +
                        "حلل الصورة التعليمية أو المسألة أو الصفحة الدراسية واشرح الحل خطوة بخطوة بوضوح، " +
                        "مع تلخيص المفهوم الأساسي ونصيحة للمذاكرة. أجب بنفس لغة سؤال المستخدم."
                )
            }
        )
    }

    private val quizSchema = Schema.obj(
        properties = mapOf(
            "questions" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "question" to Schema.string(description = "Clear educational quiz question"),
                        "options" to Schema.array(
                            items = Schema.string(),
                            description = "Exactly 4 answer options"
                        ),
                        "correctIndex" to Schema.integer(description = "0-based index of the correct option (0..3)"),
                        "explanation" to Schema.string(description = "Brief educational explanation of the correct answer")
                    )
                ),
                description = "List of 4 multiple-choice questions"
            )
        )
    )

    private val quizModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = quizSchema
            }
        )
    }

    private val flashcardsSchema = Schema.obj(
        properties = mapOf(
            "summary" to Schema.string(description = "Concise structured summary of the lesson material"),
            "cards" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "front" to Schema.string(description = "Question or key concept on card front"),
                        "back" to Schema.string(description = "Clear concise answer or definition on card back"),
                        "memoryTip" to Schema.string(description = "Short mnemonic or real-world analogy")
                    )
                ),
                description = "4 to 5 study flashcards for active recall"
            )
        )
    )

    private val flashcardsModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.5-flash-lite",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = flashcardsSchema
            }
        )
    }

    private val educationalImageModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-nano-banana-2.1",
            generationConfig = generationConfig {
                responseModalities = listOf(ResponseModality.IMAGE)
            }
        )
    }

    fun streamSupportResponse(userPrompt: String, contextSummary: String): Flow<String> = flow {
        val fullPrompt = "سياق الحساب: $contextSummary\n\nسؤال المستخدم: $userPrompt"
        val accumulated = StringBuilder()
        withTimeout(60_000L) {
            supportModel.generateContentStream(fullPrompt).collect { chunk ->
                val text = chunk.text.orEmpty()
                if (text.isNotEmpty()) {
                    accumulated.append(text)
                    emit(accumulated.toString())
                }
            }
        }
        if (accumulated.isBlank()) {
            throw IllegalStateException("Empty response from AI support model.")
        }
    }

    fun streamTutorReply(
        persona: AiTutorPersona,
        historyEntries: List<SupportChatEntry>,
        userQuestion: String,
        courseContext: String,
    ): Flow<String> = flow {
        val tutorModel = Firebase.ai.generativeModel(
            modelName = persona.modelName,
            systemInstruction = content {
                text(
                    "${persona.roleInstruction}\n" +
                        if (courseContext.isNotBlank()) "سياق الدورة الحالية: $courseContext" else ""
                )
            }
        )
        val chatHistory = historyEntries.takeLast(10).map { entry ->
            content(if (entry.isUser) "user" else "model") {
                text(entry.text)
            }
        }
        val chat = tutorModel.startChat(history = chatHistory)
        val accumulated = StringBuilder()
        withTimeout(60_000L) {
            chat.sendMessageStream(userQuestion).collect { chunk ->
                val text = chunk.text.orEmpty()
                if (text.isNotEmpty()) {
                    accumulated.append(text)
                    emit(accumulated.toString())
                }
            }
        }
        if (accumulated.isBlank()) {
            throw IllegalStateException("Empty response from AI tutor.")
        }
    }

    fun streamVisionHomeworkHelp(
        bitmap: Bitmap,
        question: String,
        isArabic: Boolean,
    ): Flow<String> = flow {
        val prompt = question.trim().ifBlank {
            if (isArabic) {
                "حلل هذه الصورة التعليمية أو ورقة العمل واشرح المفاهيم والحل خطوة بخطوة."
            } else {
                "Analyze this educational image or worksheet and explain the concepts and solution step by step."
            }
        }
        val accumulated = StringBuilder()
        withTimeout(60_000L) {
            visionModel.generateContentStream(
                content {
                    image(bitmap)
                    text(prompt)
                }
            ).collect { chunk ->
                val text = chunk.text.orEmpty()
                if (text.isNotEmpty()) {
                    accumulated.append(text)
                    emit(accumulated.toString())
                }
            }
        }
        if (accumulated.isBlank()) {
            throw IllegalStateException("Vision model returned empty analysis.")
        }
    }

    suspend fun generateInteractiveQuiz(
        topic: String,
        gradeLevel: String,
        difficulty: String,
        isArabic: Boolean,
    ): Result<List<GeneratedQuizItem>> {
        return try {
            val langInstruction = if (isArabic) "باللغة العربية الفصحى" else "in clear English"
            val prompt = "أنشئ اختباراً تفاعلياً من 4 أسئلة اختيار من متعدد $langInstruction " +
                "حول الموضوع: '$topic'، للمرحلة/المستوى: '$gradeLevel'، بدرجة صعوبة: '$difficulty'. " +
                "تأكد أن لكل سؤال 4 خيارات بالضبط ومؤشر الإجابة الصحيحة من 0 إلى 3 مع شرح تعليمي مختصر."
            val response = withTimeout(60_000L) {
                quizModel.generateContent(prompt)
            }
            val rawJson = response.text ?: throw IllegalStateException("Model returned empty quiz.")
            val root = JSONObject(rawJson)
            val arr = root.getJSONArray("questions")
            val items = (0 until arr.length()).map { i ->
                val qObj = arr.getJSONObject(i)
                val optsArr = qObj.getJSONArray("options")
                val opts = (0 until optsArr.length()).map { j -> optsArr.getString(j) }
                val safeOpts = if (opts.isEmpty()) listOf("A", "B", "C", "D") else opts
                GeneratedQuizItem(
                    question = qObj.getString("question"),
                    options = safeOpts,
                    correctIndex = qObj.optInt("correctIndex", 0).coerceIn(0, safeOpts.lastIndex),
                    explanation = qObj.optString("explanation", "")
                )
            }
            if (items.isEmpty()) {
                Result.failure(IllegalStateException("No quiz questions generated."))
            } else {
                Result.success(items)
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Quiz generation timed out", e)
            Result.failure(e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Quiz generation failed", e)
            Result.failure(e)
        }
    }

    suspend fun generateSummaryAndFlashcards(
        sourceText: String,
        isArabic: Boolean,
    ): Result<GeneratedFlashcardsBundle> {
        return try {
            val langInstruction = if (isArabic) "باللغة العربية الواضحة" else "in clear English"
            val prompt = "لخّص المحتوى التعليمي التالي وأنشئ 4 بطاقات استذكار تفاعلية $langInstruction:\n\n$sourceText"
            val response = withTimeout(60_000L) {
                flashcardsModel.generateContent(prompt)
            }
            val rawJson = response.text ?: throw IllegalStateException("Model returned empty flashcards.")
            val root = JSONObject(rawJson)
            val summary = root.optString("summary", "")
            val cardsArr = root.getJSONArray("cards")
            val cards = (0 until cardsArr.length()).map { i ->
                val obj = cardsArr.getJSONObject(i)
                GeneratedFlashcard(
                    front = obj.getString("front"),
                    back = obj.getString("back"),
                    memoryTip = obj.optString("memoryTip", "")
                )
            }
            Result.success(GeneratedFlashcardsBundle(summary = summary, cards = cards))
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Flashcards generation timed out", e)
            Result.failure(e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Flashcards generation failed", e)
            Result.failure(e)
        }
    }

    suspend fun generateEducationalVisual(
        promptText: String,
        referenceBitmap: Bitmap?,
    ): Result<Bitmap> {
        return try {
            val fullPrompt = "Create a clear, clean, high-contrast educational illustration or diagram for students: $promptText"
            val response = withTimeout(60_000L) {
                if (referenceBitmap != null) {
                    educationalImageModel.generateContent(
                        content {
                            image(referenceBitmap)
                            text(fullPrompt)
                        }
                    )
                } else {
                    educationalImageModel.generateContent(fullPrompt)
                }
            }
            val imagePart = response.candidates.firstOrNull()
                ?.content?.parts?.filterIsInstance<ImagePart>()
                ?.firstOrNull()
            val bitmap = imagePart?.image
            if (bitmap != null) {
                Result.success(bitmap)
            } else {
                Result.failure(IllegalStateException("No educational image was returned by the model."))
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Educational image generation timed out", e)
            Result.failure(e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Educational image generation failed", e)
            Result.failure(e)
        }
    }

    suspend fun generateStructuredLessonPlan(
        topic: String,
        gradeLevel: String,
        isArabic: Boolean
    ): Result<GeneratedLessonPlanDraft> {
        return try {
            val langInstruction = if (isArabic) "باللغة العربية الفصحى" else "in clear English"
            val prompt = "أنشئ مخطط درس احترافي لـ EduTech Mindset $langInstruction حول الموضوع: '$topic' للمرحلة/المستوى: '$gradeLevel'."
            val response = withTimeout(60_000L) {
                lessonPlanModel.generateContent(prompt)
            }
            val rawJson = response.text ?: throw IllegalStateException("Model returned empty text.")
            val json = JSONObject(rawJson)
            Result.success(
                GeneratedLessonPlanDraft(
                    title = json.optString("title", topic).take(120),
                    subject = json.optString("subject", "تقنيات التعليم").take(80),
                    gradeLevel = json.optString("gradeLevel", gradeLevel).take(60),
                    objectives = json.optString("objectives", "").take(1200),
                    activities = json.optString("activities", "").take(1500),
                    assessmentMethod = json.optString("assessmentMethod", "").take(600),
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "AI lesson plan timed out", e)
            Result.failure(e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AI lesson plan generation failed", e)
            Result.failure(e)
        }
    }

    suspend fun generateAcademicReport(
        summaryContext: String,
        isArabic: Boolean
    ): Result<String> {
        return try {
            val prompt = if (isArabic) {
                "بصفتك مستشار التحليل الأكاديمي في منصة EduTech Mindset بإدارة الأستاذ باسل الفقعاوي، " +
                    "قدّم تقريراً تحليلياً موجزاً وعملياً (3 إلى 4 نقاط واضحة) بناءً على المؤشرات التالية:\n$summaryContext"
            } else {
                "As the academic analytics advisor for EduTech Mindset (Mr. Basel Al-Faqawi), " +
                    "provide a concise, actionable analytical report (3-4 bullet points) based on these metrics:\n$summaryContext"
            }
            val response = withTimeout(60_000L) {
                analyticsModel.generateContent(prompt)
            }
            val text = response.text
            if (text.isNullOrBlank()) {
                Result.failure(IllegalStateException("Model returned empty report."))
            } else {
                Result.success(text)
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "AI report timed out", e)
            Result.failure(e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AI academic report failed", e)
            Result.failure(e)
        }
    }

    private companion object {
        const val TAG = "AiEducationRepo"
    }
}
