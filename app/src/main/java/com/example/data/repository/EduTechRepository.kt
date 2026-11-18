package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.core.CryptoManager
import com.example.core.OperationType
import com.example.core.handleFirestoreError
import com.example.data.model.AcademyMessage
import com.example.data.model.CloudBackupSnapshot
import com.example.data.model.Course
import com.example.data.model.LessonPlan
import com.example.data.model.NotificationCategory
import com.example.data.model.PaymentGateway
import com.example.data.model.PaymentStatus
import com.example.data.model.PaymentTransaction
import com.example.data.model.SmartNotification
import com.example.data.model.StudentRecord
import com.example.data.model.StudentStatus
import com.example.data.model.UserProfile
import com.example.data.model.UserRole
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.firestore.firestoreSettings
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class EduTechRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        createConfiguredFirestore(context.applicationContext)
    )

    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")

    private fun <T> Flow<T>.wrapFirestoreFlow(uid: String, path: String, op: OperationType): Flow<T> =
        this.catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val toLog = firestoreError ?: (error as? Exception)
            if (toLog != null && auth.currentUser?.uid == uid) {
                handleFirestoreError(toLog, op, path)
            }
            throw error
        }

    fun observeProfile(userId: String): Flow<UserProfile?> = flow {
        val path = "users/$userId"
        emitAll(
            db.collection("users").document(userId)
                .snapshots()
                .map { it.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .wrapFirestoreFlow(userId, path, OperationType.GET)
        )
    }

    fun observeCourses(userId: String): Flow<List<Course>> = flow {
        val path = "users/$userId/courses"
        emitAll(
            db.collection("users").document(userId).collection("courses")
                .snapshots()
                .map { snap ->
                    snap.toObjects(Course::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .wrapFirestoreFlow(userId, path, OperationType.LIST)
        )
    }

    fun observeStudentRecords(userId: String): Flow<List<StudentRecord>> = flow {
        val path = "users/$userId/student_records"
        emitAll(
            db.collection("users").document(userId).collection("student_records")
                .snapshots()
                .map { snap ->
                    snap.toObjects(StudentRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
                }
                .wrapFirestoreFlow(userId, path, OperationType.LIST)
        )
    }

    fun observeLessonPlans(userId: String): Flow<List<LessonPlan>> = flow {
        val path = "users/$userId/lesson_plans"
        emitAll(
            db.collection("users").document(userId).collection("lesson_plans")
                .snapshots()
                .map { snap ->
                    snap.toObjects(LessonPlan::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .wrapFirestoreFlow(userId, path, OperationType.LIST)
        )
    }

    fun observeNotifications(userId: String): Flow<List<SmartNotification>> = flow {
        val path = "users/$userId/notifications"
        emitAll(
            db.collection("users").document(userId).collection("notifications")
                .snapshots()
                .map { snap ->
                    snap.toObjects(SmartNotification::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .wrapFirestoreFlow(userId, path, OperationType.LIST)
        )
    }

    fun observePayments(userId: String): Flow<List<PaymentTransaction>> = flow {
        val path = "users/$userId/payments"
        emitAll(
            db.collection("users").document(userId).collection("payments")
                .snapshots()
                .map { snap ->
                    snap.toObjects(PaymentTransaction::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .wrapFirestoreFlow(userId, path, OperationType.LIST)
        )
    }

    fun observeBackups(userId: String): Flow<List<CloudBackupSnapshot>> = flow {
        val path = "users/$userId/backups"
        emitAll(
            db.collection("users").document(userId).collection("backups")
                .snapshots()
                .map { snap ->
                    snap.toObjects(CloudBackupSnapshot::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .wrapFirestoreFlow(userId, path, OperationType.LIST)
        )
    }

    fun observeAcademyMessages(userId: String): Flow<List<AcademyMessage>> = flow {
        val path = "academy_messages"
        emitAll(
            db.collection("academy_messages")
                .whereEqualTo("publicRoom", true)
                .snapshots()
                .map { snap ->
                    snap.toObjects(AcademyMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.createdAt ?: Timestamp(0, 0) }
                }
                .wrapFirestoreFlow(userId, path, OperationType.LIST)
        )
    }

    /**
     * Seeds the official EduTech Mindset academy catalog and initial workspace once for a newly signed-in user.
     */
    suspend fun ensureInitialWorkspace(
        userId: String,
        fallbackName: String,
        preferredLanguage: String,
        darkMode: Boolean
    ): Result<Unit> {
        val userRef = db.collection("users").document(userId)
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch" }
            val existingProfile = userRef.get().await()
            if (!existingProfile.exists()) {
                val safeName = fallbackName.trim().ifBlank { "متعلم EduTech" }.take(80)
                val profilePayload = mapOf(
                    "userId" to uid,
                    "displayName" to safeName,
                    "role" to UserRole.TEACHER.name,
                    "track" to "الذكاء الاصطناعي وتقنيات التعليم",
                    "preferredLanguage" to (if (preferredLanguage == "EN") "EN" else "AR"),
                    "darkMode" to darkMode,
                    "autoBackup" to true,
                    "studyGoalMinutes" to 60L,
                    "overallScore" to 92.5,
                    "completedLessonsCount" to 9L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
                userRef.set(profilePayload).await()

                val batch = db.batch()
                val starterCourses = listOf(
                    mapOf(
                        "userId" to uid,
                        "title" to "هندسة التعليم الذكي والذكاء الاصطناعي",
                        "instructor" to "أ. باسل الفقعاوي",
                        "category" to "الذكاء الاصطناعي",
                        "description" to "توظيف نماذج الذكاء الاصطناعي في بناء مناهج تفاعلية وتقييم تقدم الطلبة لحظياً.",
                        "priceSar" to 0L,
                        "enrolled" to true,
                        "progressPercent" to 60L,
                        "completedLessons" to 6L,
                        "totalLessons" to 10L,
                        "rating" to 4.9,
                        "evaluationNote" to "منهج عملي متميز ومحدث",
                        "paymentStatus" to PaymentStatus.FREE.name,
                        "paymentMethod" to "",
                        "imageKey" to "ai",
                        "externalPlatformUrl" to "https://classroom.google.com",
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    ),
                    mapOf(
                        "userId" to uid,
                        "title" to "تطوير المنصات التعليمية التفاعلية",
                        "instructor" to "أ. باسل الفقعاوي",
                        "category" to "البرمجة والتقنية",
                        "description" to "تصميم وتطوير أنظمة إدارة التعلم LMS والتكامل مع أدوات التقييم الرقمية.",
                        "priceSar" to 249L,
                        "enrolled" to false,
                        "progressPercent" to 0L,
                        "completedLessons" to 0L,
                        "totalLessons" to 12L,
                        "rating" to 4.8,
                        "evaluationNote" to "",
                        "paymentStatus" to PaymentStatus.UNPAID.name,
                        "paymentMethod" to "",
                        "imageKey" to "code",
                        "externalPlatformUrl" to "https://moodle.org",
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    ),
                    mapOf(
                        "userId" to uid,
                        "title" to "تحليلات التعلم وبناء الاختبارات الذكية",
                        "instructor" to "أ. باسل الفقعاوي",
                        "category" to "تحليل البيانات",
                        "description" to "قياس مخرجات التعلم وتحليل الفجوات المعرفية والتواصل الفعال مع الأسرة.",
                        "priceSar" to 179L,
                        "enrolled" to true,
                        "progressPercent" to 38L,
                        "completedLessons" to 3L,
                        "totalLessons" to 8L,
                        "rating" to 4.7,
                        "evaluationNote" to "تقارير دقيقة وسهلة الفهم",
                        "paymentStatus" to PaymentStatus.PAID.name,
                        "paymentMethod" to PaymentGateway.MADA.name,
                        "imageKey" to "ai",
                        "externalPlatformUrl" to "https://www.khanacademy.org",
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                )
                starterCourses.forEachIndexed { idx, data ->
                    batch.set(userRef.collection("courses").document("starter_course_${idx + 1}"), data)
                }

                val starterStudents = listOf(
                    mapOf(
                        "userId" to uid,
                        "studentName" to "سارة أحمد المنصور",
                        "courseTitle" to "هندسة التعليم الذكي والذكاء الاصطناعي",
                        "progressPercent" to 88L,
                        "quizScore" to 95L,
                        "attendanceRate" to 98L,
                        "status" to StudentStatus.EXCELLENT.name,
                        "parentContactNote" to "إشعار تفوق أُرسل لولي الأمر",
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    ),
                    mapOf(
                        "userId" to uid,
                        "studentName" to "خالد محمد العتيبي",
                        "courseTitle" to "تحليلات التعلم وبناء الاختبارات الذكية",
                        "progressPercent" to 64L,
                        "quizScore" to 82L,
                        "attendanceRate" to 90L,
                        "status" to StudentStatus.ON_TRACK.name,
                        "parentContactNote" to "تقدم منتظم في الوحدة الثالثة",
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    ),
                    mapOf(
                        "userId" to uid,
                        "studentName" to "يوسف طارق الحسن",
                        "courseTitle" to "تطوير المنصات التعليمية التفاعلية",
                        "progressPercent" to 35L,
                        "quizScore" to 68L,
                        "attendanceRate" to 78L,
                        "status" to StudentStatus.NEEDS_SUPPORT.name,
                        "parentContactNote" to "تم جدولة جلسة مراجعة مسائية",
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                )
                starterStudents.forEachIndexed { idx, data ->
                    batch.set(userRef.collection("student_records").document("starter_student_${idx + 1}"), data)
                }

                val starterNotifs = listOf(
                    mapOf(
                        "userId" to uid,
                        "title" to "أهلاً بك في EduTech Mindset",
                        "body" to "أكاديمية الأستاذ باسل الفقعاوي جاهزة. يمكنك المتابعة حتى دون اتصال بالإنترنت.",
                        "category" to NotificationCategory.SMART_ALERT.name,
                        "priority" to "HIGH",
                        "read" to false,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    ),
                    mapOf(
                        "userId" to uid,
                        "title" to "تقرير تواصل البيت والمدرسة",
                        "body" to "ارتفع متوسط تحصيل الطلاب هذا الأسبوع بنسبة +6% في مسار الذكاء الاصطناعي.",
                        "category" to NotificationCategory.PARENT_SCHOOL.name,
                        "priority" to "NORMAL",
                        "read" to false,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                )
                starterNotifs.forEachIndexed { idx, data ->
                    batch.set(userRef.collection("notifications").document("starter_notif_${idx + 1}"), data)
                }

                batch.set(
                    userRef.collection("lesson_plans").document("starter_plan_1"),
                    mapOf(
                        "userId" to uid,
                        "title" to "مخطط درس: بناء الفصول الذكية التفاعلية",
                        "subject" to "تقنيات التعليم",
                        "gradeLevel" to "المستوى المتقدم",
                        "objectives" to "تمكين المتعلم من تصميم أنشطة صفية تفاعلية وربطها بمؤشرات الأداء.",
                        "activities" to "ورشة عمل تطبيقية + مناقشة جماعية عبر المنصة.",
                        "assessmentMethod" to "تقييم تفاعلي فوري + مشروع مصغر",
                        "aiGenerated" to false,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                )

                batch.commit().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, userRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateProfile(
        userId: String,
        displayName: String,
        role: UserRole,
        track: String,
        preferredLanguage: String,
        darkMode: Boolean,
        autoBackup: Boolean,
        studyGoalMinutes: Long
    ): Result<Unit> {
        val docRef = db.collection("users").document(userId)
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val safeName = displayName.trim().take(80)
            require(safeName.isNotEmpty()) { "Display name cannot be blank" }
            docRef.update(
                mapOf(
                    "displayName" to safeName,
                    "role" to role.name,
                    "track" to track.trim().take(120),
                    "preferredLanguage" to (if (preferredLanguage == "EN") "EN" else "AR"),
                    "darkMode" to darkMode,
                    "autoBackup" to autoBackup,
                    "studyGoalMinutes" to studyGoalMinutes.coerceIn(0L, 600L),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun createCourse(
        userId: String,
        title: String,
        instructor: String,
        category: String,
        description: String,
        priceSar: Long,
        totalLessons: Long
    ): Result<Unit> {
        val colRef = db.collection("users").document(userId).collection("courses")
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val cleanTitle = title.trim().take(120)
            require(cleanTitle.length >= 2) { "Course title must be at least 2 characters" }
            val cleanInstructor = instructor.trim().ifBlank { "أ. باسل الفقعاوي" }.take(80)
            val cleanCategory = category.trim().ifBlank { "تقنيات التعليم" }.take(60)
            val safePrice = priceSar.coerceIn(0L, 100000L)
            val safeLessons = totalLessons.coerceIn(1L, 500L)
            val isFree = safePrice == 0L
            val docRef = colRef.document()
            docRef.set(
                mapOf(
                    "userId" to uid,
                    "title" to cleanTitle,
                    "instructor" to cleanInstructor,
                    "category" to cleanCategory,
                    "description" to description.trim().take(600),
                    "priceSar" to safePrice,
                    "enrolled" to isFree,
                    "progressPercent" to 0L,
                    "completedLessons" to 0L,
                    "totalLessons" to safeLessons,
                    "rating" to 5.0,
                    "evaluationNote" to "",
                    "paymentStatus" to (if (isFree) PaymentStatus.FREE.name else PaymentStatus.UNPAID.name),
                    "paymentMethod" to "",
                    "imageKey" to (if (cleanCategory.contains("برمج") || cleanCategory.contains("Code", true)) "code" else "ai"),
                    "externalPlatformUrl" to "https://classroom.google.com",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, colRef.path)
            Result.failure(e)
        }
    }

    suspend fun advanceCourseLesson(userId: String, course: Course): Result<Unit> {
        val docRef = db.collection("users").document(userId).collection("courses").document(course.id)
        return try {
            val uid = requireUserId()
            check(uid == userId)
            require(course.id.isNotBlank())
            val nextCompleted = (course.completedLessons + 1L).coerceAtMost(course.totalLessons.coerceAtLeast(1L))
            val nextPercent = ((nextCompleted * 100L) / course.totalLessons.coerceAtLeast(1L)).coerceIn(0L, 100L)
            val batch = db.batch()
            batch.update(
                docRef,
                mapOf(
                    "completedLessons" to nextCompleted,
                    "progressPercent" to nextPercent,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            )
            batch.update(
                db.collection("users").document(uid),
                mapOf(
                    "completedLessonsCount" to FieldValue.increment(1L),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun submitCourseEvaluation(
        userId: String,
        courseId: String,
        rating: Double,
        evaluationNote: String,
        quizScorePercent: Double
    ): Result<Unit> {
        val docRef = db.collection("users").document(userId).collection("courses").document(courseId)
        return try {
            val uid = requireUserId()
            check(uid == userId)
            require(courseId.isNotBlank())
            val safeRating = rating.coerceIn(1.0, 5.0)
            val safeNote = evaluationNote.trim().take(400)
            val batch = db.batch()
            batch.update(
                docRef,
                mapOf(
                    "rating" to safeRating,
                    "evaluationNote" to safeNote,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            )
            batch.update(
                db.collection("users").document(uid),
                mapOf(
                    "overallScore" to quizScorePercent.coerceIn(0.0, 100.0),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun completePaymentAndEnroll(
        userId: String,
        course: Course,
        gateway: PaymentGateway,
        maskedCard: String
    ): Result<String> {
        val userRef = db.collection("users").document(userId)
        val courseRef = userRef.collection("courses").document(course.id)
        val paymentRef = userRef.collection("payments").document()
        val notifRef = userRef.collection("notifications").document()
        return try {
            val uid = requireUserId()
            check(uid == userId)
            require(course.id.isNotBlank())
            val txRef = "ETM-${UUID.randomUUID().toString().take(8).uppercase()}"
            val batch = db.batch()
            batch.update(
                courseRef,
                mapOf(
                    "enrolled" to true,
                    "paymentStatus" to (if (course.priceSar > 0L) PaymentStatus.PAID.name else PaymentStatus.FREE.name),
                    "paymentMethod" to gateway.name,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            )
            batch.set(
                paymentRef,
                mapOf(
                    "userId" to uid,
                    "courseId" to course.id,
                    "courseTitle" to course.title.take(120),
                    "amountSar" to course.priceSar.coerceIn(0L, 100000L),
                    "gateway" to gateway.name,
                    "maskedCard" to maskedCard.trim().ifBlank { "${gateway.name} ••••" }.take(30),
                    "status" to "COMPLETED",
                    "transactionRef" to txRef,
                    "createdAt" to FieldValue.serverTimestamp(),
                )
            )
            batch.set(
                notifRef,
                mapOf(
                    "userId" to uid,
                    "title" to "تأكيد الاشتراك: ${course.title.take(60)}",
                    "body" to "تم تفعيل اشتراكك بنجاح عبر ${gateway.labelAr} (مرجع: $txRef).",
                    "category" to NotificationCategory.COURSE_UPDATE.name,
                    "priority" to "HIGH",
                    "read" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            )
            batch.commit().await()
            Result.success(txRef)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, paymentRef.path)
            Result.failure(e)
        }
    }

    suspend fun addStudentRecord(
        userId: String,
        studentName: String,
        courseTitle: String,
        progressPercent: Long,
        quizScore: Long,
        attendanceRate: Long,
        parentNote: String
    ): Result<Unit> {
        val colRef = db.collection("users").document(userId).collection("student_records")
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val cleanName = studentName.trim().take(80)
            val cleanCourse = courseTitle.trim().take(120)
            require(cleanName.length >= 2 && cleanCourse.length >= 2)
            val safeProgress = progressPercent.coerceIn(0L, 100L)
            val safeQuiz = quizScore.coerceIn(0L, 100L)
            val safeAttendance = attendanceRate.coerceIn(0L, 100L)
            val status = when {
                safeQuiz >= 88L && safeProgress >= 75L -> StudentStatus.EXCELLENT
                safeQuiz >= 70L -> StudentStatus.ON_TRACK
                else -> StudentStatus.NEEDS_SUPPORT
            }
            colRef.document().set(
                mapOf(
                    "userId" to uid,
                    "studentName" to cleanName,
                    "courseTitle" to cleanCourse,
                    "progressPercent" to safeProgress,
                    "quizScore" to safeQuiz,
                    "attendanceRate" to safeAttendance,
                    "status" to status.name,
                    "parentContactNote" to parentNote.trim().take(300),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, colRef.path)
            Result.failure(e)
        }
    }

    suspend fun incrementStudentProgress(userId: String, record: StudentRecord): Result<Unit> {
        val docRef = db.collection("users").document(userId).collection("student_records").document(record.id)
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val nextProgress = (record.progressPercent + 10L).coerceAtMost(100L)
            val nextScore = (record.quizScore + 3L).coerceAtMost(100L)
            val nextStatus = when {
                nextScore >= 88L && nextProgress >= 75L -> StudentStatus.EXCELLENT
                nextScore >= 70L -> StudentStatus.ON_TRACK
                else -> StudentStatus.NEEDS_SUPPORT
            }
            docRef.update(
                mapOf(
                    "progressPercent" to nextProgress,
                    "quizScore" to nextScore,
                    "status" to nextStatus.name,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun saveLessonPlan(
        userId: String,
        title: String,
        subject: String,
        gradeLevel: String,
        objectives: String,
        activities: String,
        assessmentMethod: String,
        aiGenerated: Boolean
    ): Result<Unit> {
        val colRef = db.collection("users").document(userId).collection("lesson_plans")
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val cleanTitle = title.trim().take(120)
            require(cleanTitle.length >= 2)
            colRef.document().set(
                mapOf(
                    "userId" to uid,
                    "title" to cleanTitle,
                    "subject" to subject.trim().ifBlank { "تقنيات التعليم" }.take(80),
                    "gradeLevel" to gradeLevel.trim().ifBlank { "عام" }.take(60),
                    "objectives" to objectives.trim().ifBlank { "تحقيق مخرجات التعلم" }.take(1200),
                    "activities" to activities.trim().take(1500),
                    "assessmentMethod" to assessmentMethod.trim().take(600),
                    "aiGenerated" to aiGenerated,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, colRef.path)
            Result.failure(e)
        }
    }

    suspend fun addNotification(
        userId: String,
        title: String,
        body: String,
        category: NotificationCategory,
        priority: String = "NORMAL"
    ): Result<Unit> {
        val colRef = db.collection("users").document(userId).collection("notifications")
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val cleanTitle = title.trim().take(120)
            val cleanBody = body.trim().take(500)
            require(cleanTitle.length >= 2 && cleanBody.isNotEmpty())
            colRef.document().set(
                mapOf(
                    "userId" to uid,
                    "title" to cleanTitle,
                    "body" to cleanBody,
                    "category" to category.name,
                    "priority" to (if (priority == "HIGH") "HIGH" else "NORMAL"),
                    "read" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, colRef.path)
            Result.failure(e)
        }
    }

    suspend fun markNotificationRead(userId: String, notificationId: String): Result<Unit> {
        val docRef = db.collection("users").document(userId).collection("notifications").document(notificationId)
        return try {
            val uid = requireUserId()
            check(uid == userId)
            require(notificationId.isNotBlank())
            docRef.update(
                mapOf(
                    "read" to true,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun createCloudBackup(
        userId: String,
        summary: String,
        coursesCount: Int,
        recordsCount: Int,
        plansCount: Int
    ): Result<String> {
        val colRef = db.collection("users").document(userId).collection("backups")
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val cleanSummary = summary.trim().ifBlank { "نسخة احتياطية مشفرة" }.take(200)
            val digest = CryptoManager.computeBackupDigest(uid, "$cleanSummary:$coursesCount:$recordsCount:$plansCount")
            colRef.document().set(
                mapOf(
                    "userId" to uid,
                    "summary" to cleanSummary,
                    "coursesCount" to coursesCount.toLong().coerceIn(0L, 10000L),
                    "recordsCount" to recordsCount.toLong().coerceIn(0L, 10000L),
                    "plansCount" to plansCount.toLong().coerceIn(0L, 10000L),
                    "encryptedDigest" to digest,
                    "createdAt" to FieldValue.serverTimestamp(),
                )
            ).await()
            Result.success(digest)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, colRef.path)
            Result.failure(e)
        }
    }

    suspend fun sendAcademyMessage(
        senderName: String,
        senderRole: UserRole,
        channelId: String,
        channelName: String,
        plainText: String,
        encryptWithAes: Boolean
    ): Result<Unit> {
        val colRef = db.collection("academy_messages")
        return try {
            val uid = requireUserId()
            val trimmed = plainText.trim().take(1500)
            require(trimmed.isNotEmpty())
            val safeChannelId = channelId.ifBlank { "general" }.take(64)
            val cipherPayload = if (encryptWithAes) {
                CryptoManager.encryptMessage(trimmed, safeChannelId)
            } else {
                ""
            }
            val visibleBody = if (encryptWithAes && cipherPayload.isNotEmpty()) {
                "[AES-256-GCM]"
            } else {
                trimmed
            }
            val payload = mutableMapOf<String, Any>(
                "senderId" to uid,
                "senderName" to senderName.trim().ifBlank { "عضو الأكاديمية" }.take(80),
                "senderRole" to senderRole.name,
                "channelId" to safeChannelId,
                "channelName" to channelName.trim().ifBlank { "المجتمع التعليمي" }.take(80),
                "body" to visibleBody,
                "encrypted" to (encryptWithAes && cipherPayload.isNotEmpty()),
                "publicRoom" to true,
                "createdAt" to FieldValue.serverTimestamp(),
            )
            if (cipherPayload.isNotEmpty()) {
                payload["encryptedPayload"] = cipherPayload.take(2500)
            }
            colRef.document().set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, colRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteAcademyMessage(messageId: String): Result<Unit> {
        val docRef = db.collection("academy_messages").document(messageId)
        return try {
            requireUserId()
            require(messageId.isNotBlank())
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    companion object {
        fun createConfiguredFirestore(appContext: Context): FirebaseFirestore {
            val databaseId = appContext.getString(R.string.firestore_database_id)
            val instance = FirebaseFirestore.getInstance(databaseId)
            try {
                instance.firestoreSettings = firestoreSettings {
                    setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
                }
            } catch (_: IllegalStateException) {
                // Settings already applied on existing instance.
            }
            return instance
        }
    }
}
