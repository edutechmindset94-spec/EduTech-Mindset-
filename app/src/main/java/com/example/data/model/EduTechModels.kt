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
