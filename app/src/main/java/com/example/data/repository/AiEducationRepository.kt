package com.example.data.repository

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
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
