package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.ConnectivityObserver
import com.example.core.LocalAppPreferences
import com.example.core.PreferencesStore
import com.example.data.model.AcademyMessage
import com.example.data.model.AiUiState
import com.example.data.model.CloudBackupSnapshot
import com.example.data.model.Course
import com.example.data.model.LessonPlan
import com.example.data.model.NotificationCategory
import com.example.data.model.PaymentGateway
import com.example.data.model.PaymentTransaction
import com.example.data.model.SmartNotification
import com.example.data.model.StudentRecord
import com.example.data.model.SupportChatEntry
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.data.model.UserRole
import com.example.data.repository.AiEducationRepository
import com.example.data.repository.EduTechRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class MainViewModel(
    private val repository: EduTechRepository,
    private val aiRepository: AiEducationRepository,
    private val preferencesStore: PreferencesStore,
    connectivityObserver: ConnectivityObserver,
    private val currentUserId: String,
    initialDisplayName: String,
) : ViewModel() {

    private fun <T> Flow<T>.toUiStateFlow(label: String): StateFlow<UiState<T>> =
        this.map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing $label", rootError)
                emit(UiState.Error(rootError.localizedMessage ?: "Failed to load $label"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val isOnline: StateFlow<Boolean> = connectivityObserver.observeIsOnline()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), true)

    val localPreferences: StateFlow<LocalAppPreferences> = preferencesStore.preferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), LocalAppPreferences())

    val profileState: StateFlow<UiState<UserProfile?>> =
        repository.observeProfile(currentUserId).toUiStateFlow("profile")

    val coursesState: StateFlow<UiState<List<Course>>> =
        repository.observeCourses(currentUserId).toUiStateFlow("courses")

    val studentRecordsState: StateFlow<UiState<List<StudentRecord>>> =
        repository.observeStudentRecords(currentUserId).toUiStateFlow("student_records")

    val lessonPlansState: StateFlow<UiState<List<LessonPlan>>> =
        repository.observeLessonPlans(currentUserId).toUiStateFlow("lesson_plans")

    val notificationsState: StateFlow<UiState<List<SmartNotification>>> =
        repository.observeNotifications(currentUserId).toUiStateFlow("notifications")

    val paymentsState: StateFlow<UiState<List<PaymentTransaction>>> =
        repository.observePayments(currentUserId).toUiStateFlow("payments")

    val backupsState: StateFlow<UiState<List<CloudBackupSnapshot>>> =
        repository.observeBackups(currentUserId).toUiStateFlow("backups")

    val academyMessagesState: StateFlow<UiState<List<AcademyMessage>>> =
        repository.observeAcademyMessages(currentUserId).toUiStateFlow("academy_messages")

    private val _supportHistory = MutableStateFlow(
        listOf(
            SupportChatEntry(
                id = "welcome_ai",
                isUser = false,
                text = "مرحباً بك في الدعم الفني الذكي على مدار الساعة لمنصة EduTech Mindset (إدارة الأستاذ باسل الفقعاوي). كيف يمكنني مساعدتك اليوم؟"
            )
        )
    )
    val supportHistory: StateFlow<List<SupportChatEntry>> = _supportHistory.asStateFlow()

    private val _supportAiState = MutableStateFlow<AiUiState>(AiUiState.Idle)
    val supportAiState: StateFlow<AiUiState> = _supportAiState.asStateFlow()

    private val _lessonPlanAiState = MutableStateFlow<AiUiState>(AiUiState.Idle)
    val lessonPlanAiState: StateFlow<AiUiState> = _lessonPlanAiState.asStateFlow()

    private val _analyticsReportState = MutableStateFlow<AiUiState>(AiUiState.Idle)
    val analyticsReportState: StateFlow<AiUiState> = _analyticsReportState.asStateFlow()

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val events: SharedFlow<String> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            val prefs = localPreferences.value
            val result = repository.ensureInitialWorkspace(
                userId = currentUserId,
                fallbackName = initialDisplayName,
                preferredLanguage = prefs.language,
                darkMode = prefs.darkMode
            )
            result.onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "تعذر تهيئة البيانات")
            }
        }
    }

    fun toggleDarkMode() {
        viewModelScope.launch {
            val next = !localPreferences.value.darkMode
            preferencesStore.setDarkMode(next)
            val profile = (profileState.value as? UiState.Success)?.data
            if (profile != null) {
                repository.updateProfile(
                    userId = currentUserId,
                    displayName = profile.displayName,
                    role = UserRole.fromRaw(profile.role),
                    track = profile.track,
                    preferredLanguage = profile.preferredLanguage,
                    darkMode = next,
                    autoBackup = profile.autoBackup,
                    studyGoalMinutes = profile.studyGoalMinutes
                )
            }
        }
    }

    fun toggleLanguage() {
        viewModelScope.launch {
            val next = if (localPreferences.value.language == "AR") "EN" else "AR"
            preferencesStore.setLanguage(next)
            val profile = (profileState.value as? UiState.Success)?.data
            if (profile != null) {
                repository.updateProfile(
                    userId = currentUserId,
                    displayName = profile.displayName,
                    role = UserRole.fromRaw(profile.role),
                    track = profile.track,
                    preferredLanguage = next,
                    darkMode = profile.darkMode,
                    autoBackup = profile.autoBackup,
                    studyGoalMinutes = profile.studyGoalMinutes
                )
            }
        }
    }

    fun switchRole(role: UserRole) {
        viewModelScope.launch {
            val profile = (profileState.value as? UiState.Success)?.data ?: return@launch
            repository.updateProfile(
                userId = currentUserId,
                displayName = profile.displayName,
                role = role,
                track = profile.track,
                preferredLanguage = profile.preferredLanguage,
                darkMode = profile.darkMode,
                autoBackup = profile.autoBackup,
                studyGoalMinutes = profile.studyGoalMinutes
            ).onFailure { _events.tryEmit(it.localizedMessage ?: "Update failed") }
        }
    }

    fun saveProfile(
        displayName: String,
        role: UserRole,
        track: String,
        preferredLanguage: String,
        darkMode: Boolean,
        autoBackup: Boolean,
        studyGoalMinutes: Long
    ) {
        viewModelScope.launch {
            preferencesStore.setDarkMode(darkMode)
            preferencesStore.setLanguage(preferredLanguage)
            preferencesStore.setAutoBackup(autoBackup)
            val res = repository.updateProfile(
                userId = currentUserId,
                displayName = displayName,
                role = role,
                track = track,
                preferredLanguage = preferredLanguage,
                darkMode = darkMode,
                autoBackup = autoBackup,
                studyGoalMinutes = studyGoalMinutes
            )
            res.onSuccess {
                _events.tryEmit(if (preferredLanguage == "AR") "تم حفظ الملف الشخصي بنجاح" else "Profile saved")
                if (autoBackup) {
                    triggerCloudBackupInternal("تحديث تلقائي بعد تعديل الملف الشخصي")
                }
            }.onFailure {
                _events.tryEmit(it.localizedMessage ?: "Failed to save profile")
            }
        }
    }

    fun createCourse(
        title: String,
        instructor: String,
        category: String,
        description: String,
        priceSar: Long,
        totalLessons: Long
    ) {
        viewModelScope.launch {
            repository.createCourse(currentUserId, title, instructor, category, description, priceSar, totalLessons)
                .onSuccess {
                    _events.tryEmit("تمت إضافة الدورة التدريبية بنجاح")
                    if (localPreferences.value.autoBackup) {
                        triggerCloudBackupInternal("نسخة تلقائية: إضافة دورة $title")
                    }
                }
                .onFailure { _events.tryEmit(it.localizedMessage ?: "Error creating course") }
        }
    }

    fun advanceCourseLesson(course: Course) {
        viewModelScope.launch {
            repository.advanceCourseLesson(currentUserId, course)
                .onSuccess { _events.tryEmit("تم تسجيل إتمام الدرس والمزامنة") }
                .onFailure { _events.tryEmit(it.localizedMessage ?: "Failed to update progress") }
        }
    }

    fun submitCourseEvaluation(courseId: String, rating: Double, note: String, quizScorePercent: Double) {
        viewModelScope.launch {
            repository.submitCourseEvaluation(currentUserId, courseId, rating, note, quizScorePercent)
                .onSuccess { _events.tryEmit("تم حفظ تقييم المنهج ودرجة الاختبار") }
                .onFailure { _events.tryEmit(it.localizedMessage ?: "Failed to submit evaluation") }
        }
    }

    fun completePaymentAndEnroll(course: Course, gateway: PaymentGateway, maskedCard: String) {
        viewModelScope.launch {
            repository.completePaymentAndEnroll(currentUserId, course, gateway, maskedCard)
                .onSuccess { txRef -> _events.tryEmit("تم الدفع والاشتراك بنجاح (مرجع: $txRef)") }
                .onFailure { _events.tryEmit(it.localizedMessage ?: "Payment failed") }
        }
    }

    fun addStudentRecord(
        studentName: String,
        courseTitle: String,
        progressPercent: Long,
        quizScore: Long,
        attendanceRate: Long,
        parentNote: String
    ) {
        viewModelScope.launch {
            repository.addStudentRecord(
                currentUserId, studentName, courseTitle, progressPercent, quizScore, attendanceRate, parentNote
            ).onSuccess {
                _events.tryEmit("تمت إضافة سجل الطالب بنجاح")
            }.onFailure {
                _events.tryEmit(it.localizedMessage ?: "Failed to add student record")
            }
        }
    }

    fun incrementStudentProgress(record: StudentRecord) {
        viewModelScope.launch {
            repository.incrementStudentProgress(currentUserId, record)
                .onFailure { _events.tryEmit(it.localizedMessage ?: "Failed to update student") }
        }
    }

    fun sendNotification(title: String, body: String, category: NotificationCategory, highPriority: Boolean) {
        viewModelScope.launch {
            repository.addNotification(
                currentUserId,
                title,
                body,
                category,
                if (highPriority) "HIGH" else "NORMAL"
            ).onSuccess {
                _events.tryEmit("تم إرسال التنبيه الذكي")
            }.onFailure {
                _events.tryEmit(it.localizedMessage ?: "Failed to send alert")
            }
        }
    }

    fun markNotificationRead(id: String) {
        viewModelScope.launch {
            repository.markNotificationRead(currentUserId, id)
        }
    }

    fun saveManualLessonPlan(
        title: String,
        subject: String,
        gradeLevel: String,
        objectives: String,
        activities: String,
        assessmentMethod: String
    ) {
        viewModelScope.launch {
            repository.saveLessonPlan(
                currentUserId, title, subject, gradeLevel, objectives, activities, assessmentMethod, aiGenerated = false
            ).onSuccess {
                _events.tryEmit("تم حفظ المخطط الدراسي")
            }.onFailure {
                _events.tryEmit(it.localizedMessage ?: "Failed to save plan")
            }
        }
    }

    fun generateAiLessonPlan(topic: String, gradeLevel: String) {
        if (topic.isBlank()) return
        viewModelScope.launch {
            _lessonPlanAiState.value = AiUiState.Loading
            val isAr = localPreferences.value.language == "AR"
            val result = aiRepository.generateStructuredLessonPlan(topic, gradeLevel.ifBlank { "المرحلة الثانوية" }, isAr)
            result.onSuccess { draft ->
                val saveRes = repository.saveLessonPlan(
                    userId = currentUserId,
                    title = draft.title,
                    subject = draft.subject,
                    gradeLevel = draft.gradeLevel,
                    objectives = draft.objectives,
                    activities = draft.activities,
                    assessmentMethod = draft.assessmentMethod,
                    aiGenerated = true
                )
                saveRes.onSuccess {
                    _lessonPlanAiState.value = AiUiState.Success(draft.title)
                    _events.tryEmit("تم توليد وحفظ مخطط الدرس بالذكاء الاصطناعي")
                }.onFailure { err ->
                    _lessonPlanAiState.value = AiUiState.Error(err.localizedMessage ?: "Save failed")
                }
            }.onFailure { err ->
                val msg = if (err is TimeoutCancellationException) {
                    "انتهت مهلة الذكاء الاصطناعي (60 ثانية). حاول مرة أخرى."
                } else {
                    err.localizedMessage ?: "فشل توليد المخطط الدراسي"
                }
                _lessonPlanAiState.value = AiUiState.Error(msg)
            }
        }
    }

    fun generateAiAcademicReport() {
        viewModelScope.launch {
            _analyticsReportState.value = AiUiState.Loading
            val isAr = localPreferences.value.language == "AR"
            val profile = (profileState.value as? UiState.Success)?.data
            val courses = (coursesState.value as? UiState.Success)?.data.orEmpty()
            val students = (studentRecordsState.value as? UiState.Success)?.data.orEmpty()
            val summary = buildString {
                append("Overall score: ${profile?.overallScore ?: 92.0}%, ")
                append("Completed lessons: ${profile?.completedLessonsCount ?: 0}, ")
                append("Courses count: ${courses.size}, enrolled: ${courses.count { it.enrolled }}, ")
                append("Avg course progress: ${if (courses.isEmpty()) 0 else courses.map { it.progressPercent }.average().toInt()}%, ")
                append("Tracked students: ${students.size}, avg quiz: ${if (students.isEmpty()) 0 else students.map { it.quizScore }.average().toInt()}%.")
            }
            aiRepository.generateAcademicReport(summary, isAr)
                .onSuccess { report -> _analyticsReportState.value = AiUiState.Success(report) }
                .onFailure { err ->
                    val msg = if (err is TimeoutCancellationException) {
                        "انتهت مهلة تحليل الأداء (60 ثانية). يرجى المحاولة لاحقاً."
                    } else {
                        err.localizedMessage ?: "فشل إنشاء التقرير التحليلي"
                    }
                    _analyticsReportState.value = AiUiState.Error(msg)
                }
        }
    }

    fun createCloudBackup(summary: String) {
        viewModelScope.launch {
            triggerCloudBackupInternal(summary)
        }
    }

    private suspend fun triggerCloudBackupInternal(summary: String) {
        val coursesCount = (coursesState.value as? UiState.Success)?.data?.size ?: 0
        val recordsCount = (studentRecordsState.value as? UiState.Success)?.data?.size ?: 0
        val plansCount = (lessonPlansState.value as? UiState.Success)?.data?.size ?: 0
        repository.createCloudBackup(currentUserId, summary, coursesCount, recordsCount, plansCount)
            .onSuccess { digest ->
                _events.tryEmit("تم النسخ الاحتياطي السحابي المشفر ($digest)")
            }
            .onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "Backup failed")
            }
    }

    fun sendAcademyMessage(text: String, encryptWithAes: Boolean) {
        if (text.isBlank()) return
        viewModelScope.launch {
            val profile = (profileState.value as? UiState.Success)?.data
            val name = profile?.displayName ?: "عضو الأكاديمية"
            val role = UserRole.fromRaw(profile?.role)
            repository.sendAcademyMessage(
                senderName = name,
                senderRole = role,
                channelId = "general",
                channelName = "المجتمع التعليمي العام",
                plainText = text,
                encryptWithAes = encryptWithAes
            ).onFailure {
                _events.tryEmit(it.localizedMessage ?: "Failed to send message")
            }
        }
    }

    fun deleteAcademyMessage(messageId: String) {
        viewModelScope.launch {
            repository.deleteAcademyMessage(messageId)
        }
    }

    fun askAiSupport(question: String) {
        val clean = question.trim()
        if (clean.isEmpty()) return
        val userMsgId = UUID.randomUUID().toString()
        _supportHistory.value = _supportHistory.value + SupportChatEntry(userMsgId, isUser = true, text = clean)

        viewModelScope.launch {
            _supportAiState.value = AiUiState.Loading
            val profile = (profileState.value as? UiState.Success)?.data
            val courses = (coursesState.value as? UiState.Success)?.data.orEmpty()
            val contextSummary =
                "الاسم: ${profile?.displayName ?: "مستخدم"}، الدور: ${profile?.role ?: "STUDENT"}، " +
                    "عدد الدورات: ${courses.size}، المسجل بها: ${courses.count { it.enrolled }}"
            try {
                var lastText = ""
                aiRepository.streamSupportResponse(clean, contextSummary).collect { partial ->
                    lastText = partial
                    _supportAiState.value = AiUiState.Streaming(partial)
                }
                _supportHistory.value = _supportHistory.value + SupportChatEntry(
                    id = UUID.randomUUID().toString(),
                    isUser = false,
                    text = lastText
                )
                _supportAiState.value = AiUiState.Success(lastText)
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Support AI timed out", e)
                _supportAiState.value = AiUiState.Error("انتهت مهلة الاستجابة (60 ثانية). حاول مرة أخرى.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Support AI failed", e)
                _supportAiState.value = AiUiState.Error(e.localizedMessage ?: "تعذر الاتصال بالدعم الذكي.")
            }
        }
    }

    private companion object {
        const val TAG = "MainViewModel"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
