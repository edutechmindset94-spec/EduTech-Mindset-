package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

sealed interface AiUiState {
    data object Idle : AiUiState
    data object Loading : AiUiState
    data class Streaming(val text: String) : AiUiState
    data class Success(val text: String) : AiUiState
    data class Error(val message: String) : AiUiState
}

enum class UserRole {
    STUDENT,
    TEACHER;

    companion object {
        fun fromRaw(raw: String?): UserRole =
            entries.firstOrNull { it.name == raw } ?: STUDENT
    }
}

enum class PaymentStatus {
    FREE,
    UNPAID,
    PAID;

    companion object {
        fun fromRaw(raw: String?): PaymentStatus =
            entries.firstOrNull { it.name == raw } ?: FREE
    }
}

enum class PaymentGateway(val labelAr: String, val labelEn: String) {
    MADA("بطاقة مدى", "Mada Card"),
    VISA_MASTERCARD("فيزا / ماستركارد", "Visa / Mastercard"),
    APPLE_GOOGLE_PAY("Apple / Google Pay", "Apple / Google Pay"),
    WALLET("المحفظة التعليمية", "Edu Wallet");

    companion object {
        fun fromRaw(raw: String?): PaymentGateway =
            entries.firstOrNull { it.name == raw } ?: MADA
    }
}

enum class StudentStatus(val labelAr: String, val labelEn: String) {
    EXCELLENT("متفوق", "Excellent"),
    ON_TRACK("على المسار", "On Track"),
    NEEDS_SUPPORT("يحتاج دعم", "Needs Support");

    companion object {
        fun fromRaw(raw: String?): StudentStatus =
            entries.firstOrNull { it.name == raw } ?: ON_TRACK
    }
}

enum class NotificationCategory(val labelAr: String, val labelEn: String) {
    ASSIGNMENT("مهمة مطلوبة", "Assignment"),
    COURSE_UPDATE("تحديث دورة", "Course Update"),
    PARENT_SCHOOL("البيت والمدرسة", "Home & School"),
    SMART_ALERT("تنبيه ذكي", "Smart Alert");

    companion object {
        fun fromRaw(raw: String?): NotificationCategory =
            entries.firstOrNull { it.name == raw } ?: SMART_ALERT
    }
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val role: String = UserRole.STUDENT.name,
    val track: String = "",
    val preferredLanguage: String = "AR",
    val darkMode: Boolean = false,
    val autoBackup: Boolean = true,
    val studyGoalMinutes: Long = 60L,
    val overallScore: Double = 92.0,
    val completedLessonsCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class Course(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val instructor: String = "",
    val category: String = "",
    val description: String = "",
    val priceSar: Long = 0L,
    val enrolled: Boolean = false,
    val progressPercent: Long = 0L,
    val completedLessons: Long = 0L,
    val totalLessons: Long = 10L,
    val rating: Double = 4.8,
    val evaluationNote: String = "",
    val paymentStatus: String = PaymentStatus.FREE.name,
    val paymentMethod: String = "",
    val imageKey: String = "ai",
    val externalPlatformUrl: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class StudentRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val studentName: String = "",
    val courseTitle: String = "",
    val progressPercent: Long = 0L,
    val quizScore: Long = 0L,
    val attendanceRate: Long = 100L,
    val status: String = StudentStatus.ON_TRACK.name,
    val parentContactNote: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class LessonPlan(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val subject: String = "",
    val gradeLevel: String = "",
    val objectives: String = "",
    val activities: String = "",
    val assessmentMethod: String = "",
    val aiGenerated: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class SmartNotification(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val body: String = "",
    val category: String = NotificationCategory.SMART_ALERT.name,
    val priority: String = "NORMAL",
    val read: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class PaymentTransaction(
    @DocumentId val id: String = "",
    val userId: String = "",
    val courseId: String = "",
    val courseTitle: String = "",
    val amountSar: Long = 0L,
    val gateway: String = PaymentGateway.MADA.name,
    val maskedCard: String = "",
    val status: String = "COMPLETED",
    val transactionRef: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class CloudBackupSnapshot(
    @DocumentId val id: String = "",
    val userId: String = "",
    val summary: String = "",
    val coursesCount: Long = 0L,
    val recordsCount: Long = 0L,
    val plansCount: Long = 0L,
    val encryptedDigest: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class AcademyMessage(
    @DocumentId val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderRole: String = UserRole.STUDENT.name,
    val channelId: String = "general",
    val channelName: String = "المجتمع التعليمي العام",
    val body: String = "",
    val encryptedPayload: String = "",
    val encrypted: Boolean = false,
    val publicRoom: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
)

data class SupportChatEntry(
    val id: String,
    val isUser: Boolean,
    val text: String,
)

data class ExternalPlatform(
    val id: String,
    val nameAr: String,
    val nameEn: String,
    val descAr: String,
    val descEn: String,
    val url: String,
)

data class QuizQuestion(
    val questionAr: String,
    val questionEn: String,
    val optionsAr: List<String>,
    val optionsEn: List<String>,
    val correctIndex: Int,
)

enum class AiTutorPersona(
    val labelAr: String,
    val labelEn: String,
    val modelName: String,
    val roleInstruction: String,
) {
    SMART_TUTOR(
        labelAr = "المعلم الذكي",
        labelEn = "Smart Tutor",
        modelName = "gemini-3.8-flash",
        roleInstruction = "أنت المعلم الذكي في منصة EduTech Mindset (إدارة الأستاذ باسل الفقعاوي). اشرح المفاهيم بوضوح وتدرج تربوي مع أمثلة تطبيقية وتشجيع مستمر للطالب."
    ),
    SOCRATIC_COACH(
        labelAr = "حوار سقراطي",
        labelEn = "Socratic Coach",
        modelName = "gemini-3.8-flash",
        roleInstruction = "أنت موجه سقراطي تربوي في منصة EduTech Mindset. لا تعطِ الإجابة النهائية فوراً؛ وجّه المتعلم بأسئلة استكشافية ذكية وتلميحات خطوة بخطوة ليصل للحل بنفسه."
    ),
    STEM_SOLVER(
        labelAr = "حل مسائل STEM",
        labelEn = "STEM Solver",
        modelName = "gemini-3.8-flash",
        roleInstruction = "أنت خبير الرياضيات والعلوم والبرمجة في EduTech Mindset. حلّ المسائل بخطوات رياضية ومنطقية مرقمة واضحة وتحقق من النتيجة النهائية."
    ),
    QUICK_EXPLAINER(
        labelAr = "شرح سريع",
        labelEn = "Quick Explainer",
        modelName = "gemini-3.5-flash-lite",
        roleInstruction = "أنت مساعد مراجعة سريعة في EduTech Mindset. قدّم إجابات مركزة ومختصرة جداً على شكل نقاط واضحة مناسبة للمراجعة قبل الاختبار."
    ),
}

enum class AiToolTab {
    TUTOR,
    VISION,
    QUIZ,
    FLASHCARDS,
    VISUALS,
}

data class GeneratedQuizItem(
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String,
)

data class GeneratedFlashcard(
    val front: String,
    val back: String,
    val memoryTip: String,
)

sealed interface AiQuizUiState {
    data object Idle : AiQuizUiState
    data object Loading : AiQuizUiState
    data class Success(val topic: String, val questions: List<GeneratedQuizItem>) : AiQuizUiState
    data class Error(val message: String) : AiQuizUiState
}

sealed interface AiFlashcardsUiState {
    data object Idle : AiFlashcardsUiState
    data object Loading : AiFlashcardsUiState
    data class Success(val summary: String, val cards: List<GeneratedFlashcard>) : AiFlashcardsUiState
    data class Error(val message: String) : AiFlashcardsUiState
}

sealed interface AiImageUiState {
    data object Idle : AiImageUiState
    data object Loading : AiImageUiState
    data class Success(val bitmap: android.graphics.Bitmap, val prompt: String) : AiImageUiState
    data class Error(val message: String) : AiImageUiState
}

