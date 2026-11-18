const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_basel_01";
const BOB_UID = "bob_student_02";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated users are rejected on all collections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("courses").get());
  await assertFails(
    unauthDb.collection("academy_messages").where("publicRoom", "==", true).get()
  );
});

test("UserProfile: owner create, partial update, cross-user isolation, and ghost field rejection", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 2000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "أ. باسل الفقعاوي",
      role: "TEACHER",
      track: "الذكاء الاصطناعي وتقنيات التعليم",
      preferredLanguage: "AR",
      darkMode: true,
      autoBackup: true,
      studyGoalMinutes: 60,
      overallScore: 95.5,
      completedLessonsCount: 18,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      darkMode: false,
      studyGoalMinutes: 90,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject cross-user read or write
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({ displayName: "Hacked" })
  );

  // Reject ghost field / shadow update
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 400),
    })
  );

  // Reject createdAt tampering
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      createdAt: new Date(Date.now() - 100),
      updatedAt: new Date(Date.now() - 100),
    })
  );
});

test("Courses, StudentRecords, LessonPlans, Notifications, Payments, Backups: Kotlin-mirrored payloads", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 2000);

  // Course create (no `id` field since Kotlin uses @DocumentId)
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("courses").doc("course_ai_1").set({
      userId: ALICE_UID,
      title: "الذكاء الاصطناعي في التعليم الحديث",
      instructor: "أ. باسل الفقعاوي",
      category: "الذكاء الاصطناعي",
      description: "دورة تطبيقية شاملة لتوظيف أدوات الذكاء الاصطناعي.",
      priceSar: 299,
      enrolled: true,
      progressPercent: 45,
      completedLessons: 5,
      totalLessons: 11,
      rating: 4.9,
      evaluationNote: "منهج متكامل وعصري",
      paymentStatus: "PAID",
      paymentMethod: "MADA",
      imageKey: "ai",
      externalPlatformUrl: "https://classroom.google.com",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Partial update on course progress & rating
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("courses").doc("course_ai_1").update({
      progressPercent: 72,
      completedLessons: 8,
      rating: 5.0,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject `@DocumentId` `id` field inside payload
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("courses").doc("course_bad").set({
      id: "course_bad",
      userId: ALICE_UID,
      title: "دورة تجريبية",
      instructor: "أ. باسل الفقعاوي",
      category: "عام",
      priceSar: 0,
      enrolled: false,
      progressPercent: 0,
      completedLessons: 0,
      totalLessons: 5,
      rating: 0,
      paymentStatus: "FREE",
      createdAt: now,
      updatedAt: now,
    })
  );

  // StudentRecord
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("student_records").doc("rec_1").set({
      userId: ALICE_UID,
      studentName: "سارة المنصور",
      courseTitle: "الذكاء الاصطناعي في التعليم الحديث",
      progressPercent: 88,
      quizScore: 94,
      attendanceRate: 96,
      status: "EXCELLENT",
      parentContactNote: "تم إرسال تقرير التميز لولي الأمر",
      createdAt: now,
      updatedAt: now,
    })
  );

  // LessonPlan
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("lesson_plans").doc("plan_1").set({
      userId: ALICE_UID,
      title: "مقدمة في التعلم التكيفي",
      subject: "تقنيات التعليم",
      gradeLevel: "المرحلة الثانوية",
      objectives: "فهم أسس التخصيص الذكي للمحتوى",
      activities: "نشاط مجموعات وتصميم خريطة ذهنية",
      assessmentMethod: "اختبار قصير تفاعلي",
      aiGenerated: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  // SmartNotification
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("notifications").doc("notif_1").set({
      userId: ALICE_UID,
      title: "تحديث المنهج",
      body: "تمت إضافة وحدة جديدة في دورة البرمجة",
      category: "COURSE_UPDATE",
      priority: "HIGH",
      read: false,
      createdAt: now,
      updatedAt: now,
    })
  );

  // PaymentTransaction
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("payments").doc("pay_1").set({
      userId: ALICE_UID,
      courseId: "course_ai_1",
      courseTitle: "الذكاء الاصطناعي في التعليم الحديث",
      amountSar: 299,
      gateway: "MADA",
      maskedCard: "MADA •••• 4821",
      status: "COMPLETED",
      transactionRef: "ETM-2026-9941",
      createdAt: now,
    })
  );

  // CloudBackupSnapshot
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("backups").doc("bkp_1").set({
      userId: ALICE_UID,
      summary: "نسخة احتياطية تلقائية مشفرة",
      coursesCount: 3,
      recordsCount: 4,
      plansCount: 2,
      encryptedDigest: "AES256-GCM:9f84b2c1a77e",
      createdAt: now,
    })
  );

  // Bob cannot list Alice's courses
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("courses").get());
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("courses").get());
});

test("AcademyMessages: shared query alignment, AES encryption payload, and sender ownership", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 1500);

  await assertSucceeds(
    aliceDb.collection("academy_messages").doc("msg_1").set({
      senderId: ALICE_UID,
      senderName: "أ. باسل الفقعاوي",
      senderRole: "TEACHER",
      channelId: "general",
      channelName: "المجتمع التعليمي العام",
      body: "مرحباً بكم في منصة EduTech Mindset",
      encryptedPayload: "ENC_AES256_BASE64_SAMPLE",
      encrypted: true,
      publicRoom: true,
      createdAt: now,
    })
  );

  // Filtered query matching rule succeeds
  await assertSucceeds(
    bobDb.collection("academy_messages").where("publicRoom", "==", true).get()
  );

  // Unfiltered list query fails
  await assertFails(bobDb.collection("academy_messages").get());

  // Bob cannot delete or edit Alice's message
  await assertFails(bobDb.collection("academy_messages").doc("msg_1").delete());
  await assertFails(
    bobDb.collection("academy_messages").doc("msg_1").update({ body: "Tampered" })
  );

  // Alice can delete her own message
  await assertSucceeds(aliceDb.collection("academy_messages").doc("msg_1").delete());

  // Future timestamp rejected
  await assertFails(
    aliceDb.collection("academy_messages").doc("msg_future").set({
      senderId: ALICE_UID,
      senderName: "أ. باسل",
      senderRole: "TEACHER",
      channelId: "general",
      channelName: "عام",
      body: "رسالة",
      encrypted: false,
      publicRoom: true,
      createdAt: new Date(Date.now() + 60000),
    })
  );
});
