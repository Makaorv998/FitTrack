const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_fit_123";
const BOB_UID = "bob_fit_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function nowTs() {
  return new Date(Date.now() - 1000);
}

function validProfile(uid) {
  const ts = nowTs();
  return {
    userId: uid,
    displayName: "Alice Runner",
    email: "alice@example.com",
    photoUrl: "",
    ageYears: 28,
    sex: "female",
    heightCm: 168.0,
    weightKg: 62.0,
    targetWeightKg: 58.0,
    activityLevel: "moderate",
    goal: "general_fitness",
    dailyStepGoal: 10000,
    dailyWaterTargetMl: 2500,
    dailyCalorieTarget: 2050,
    onboardingCompleted: true,
    unitSystem: "metric",
    themePreference: "dark",
    sensorConsentGranted: true,
    locationConsentGranted: true,
    createdAt: ts,
    updatedAt: ts,
  };
}

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

test("Unauthenticated user cannot read or write profiles or meals", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("meals").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).set(validProfile(ALICE_UID)));
});

test("Owner can create and update their own profile, but cannot write unknown fields", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ref = aliceDb.collection("users").doc(ALICE_UID);
  await assertSucceeds(ref.set(validProfile(ALICE_UID)));
  await assertSucceeds(ref.update({ dailyStepGoal: 12000, updatedAt: nowTs() }));
  await assertFails(ref.update({ adminRole: true, updatedAt: nowTs() }));
  await assertFails(ref.update({ ghostField: true, updatedAt: nowTs() }));
});

test("Cross-user isolation blocks Bob from reading or writing Alice data", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(validProfile(ALICE_UID)));
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("meals").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("water_logs").doc("w1").set({
      userId: ALICE_UID,
      dateKey: "2026-11-15",
      amountMl: 250,
      createdAt: nowTs(),
    })
  );
});

test("Owner can log and list meals, water, steps, workouts, GPS activities, and weight", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const userDoc = aliceDb.collection("users").doc(ALICE_UID);
  const ts = nowTs();

  await assertSucceeds(
    userDoc.collection("meals").doc("meal_1").set({
      userId: ALICE_UID,
      dateKey: "2026-11-15",
      mealType: "breakfast",
      foodName: "Oatmeal & Berries",
      grams: 250.0,
      calories: 320.0,
      proteinG: 12.0,
      carbsG: 54.0,
      fatG: 6.0,
      fiberG: 7.0,
      sugarG: 9.0,
      source: "manual",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(userDoc.collection("meals").get());

  await assertSucceeds(
    userDoc.collection("step_records").doc("2026-11-15").set({
      userId: ALICE_UID,
      dateKey: "2026-11-15",
      steps: 6420,
      manualSteps: 0,
      sensorBaseline: 12000,
      distanceKm: 4.5,
      caloriesBurned: 260.0,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    userDoc.collection("gps_activities").doc("run_1").set({
      userId: ALICE_UID,
      activityType: "running",
      durationSeconds: 1800,
      distanceKm: 5.0,
      avgSpeedKmh: 10.0,
      avgPaceMinPerKm: 6.0,
      caloriesBurned: 380.0,
      routePoints: [{ lat: 37.7749, lng: -122.4194, timestampMs: 1700000000000 }],
      notes: "Morning tempo",
      startedAtMs: 1700000000000,
      endedAtMs: 1700001800000,
      createdAt: ts,
      updatedAt: ts,
    })
  );
});

test("Owner can manage workout programs, schedules, personal records, and training notes; cross-user denied", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const aliceDoc = aliceDb.collection("users").doc(ALICE_UID);
  const ts = nowTs();

  await assertSucceeds(
    aliceDoc.collection("workout_programs").doc("prog_1").set({
      userId: ALICE_UID,
      name: "Custom Upper Lower",
      category: "custom",
      goal: "muscle_gain",
      level: "intermediate",
      daysPerWeek: 4,
      description: "4-day hypertrophy program",
      weeklySplit: ["Upper A", "Lower A", "Upper B", "Lower B"],
      progressionGuidance: "Add 2.5 kg after top rep target",
      workouts: [],
      isCustom: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDoc.collection("workout_schedules").doc("2026-11-15").set({
      userId: ALICE_UID,
      dateKey: "2026-11-15",
      workoutId: "tpl_upper_strength",
      workoutName: "Upper A",
      programId: "prog_upper_lower",
      status: "scheduled",
      notes: "Evening gym",
      completedSessionId: "",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDoc.collection("personal_records").doc("ex_bench_press").set({
      userId: ALICE_UID,
      exerciseId: "ex_bench_press",
      exerciseName: "Barbell Bench Press",
      primaryMuscle: "Chest",
      weightKg: 80.0,
      reps: 5,
      estimatedOneRmKg: 93.3,
      setVolumeKg: 400.0,
      dateKey: "2026-11-15",
      sessionId: "sess_1",
      createdAt: ts,
    })
  );

  await assertSucceeds(
    aliceDoc.collection("training_notes").doc("note_1").set({
      userId: ALICE_UID,
      title: "Bench cue",
      body: "Keep shoulder blades tucked into bench.",
      dateKey: "2026-11-15",
      exerciseName: "Barbell Bench Press",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("workout_programs").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("workout_schedules").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("personal_records").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("training_notes").get());
});

test("Public can read published app_releases and app_config; non-admins cannot write or read unpublished", async () => {
  const adminDb = testEnv.authenticatedContext("admin_1", {
    email: "makarov.ds998@gmail.com",
    email_verified: true,
  }).firestore();
  const userDb = testEnv.authenticatedContext(ALICE_UID, {
    email: "alice@example.com",
    email_verified: true,
  }).firestore();
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const ts = nowTs();

  const validRelease = {
    versionName: "1.2.0",
    versionCode: 12,
    releaseDate: "2026-11-15",
    title: "Smart Recovery & Update Center",
    summary: "Adds automated update checks and verified release notes.",
    severity: "optional",
    minSupportedVersionCode: 1,
    minSupportedVersionName: "1.0",
    distributionChannel: "both",
    apkDownloadUrl: "https://downloads.fittrack.app/releases/fittrack-1.2.0.apk",
    apkSha256: "a".repeat(64),
    expectedSigningCertSha256: "b".repeat(64),
    playStoreUrl: "https://play.google.com/store/apps/details?id=com.aistudio.fittrack.vrmqtx",
    newFeatures: ["Update Center", "What's New changelog history"],
    improvements: ["Faster workout analytics"],
    bugFixes: ["Fixed rest timer pause sync"],
    securityChanges: ["Enforced APK SHA-256 and signature checks"],
    isPublished: true,
    createdAt: ts,
    updatedAt: ts,
  };

  await assertSucceeds(adminDb.collection("app_releases").doc("v12").set(validRelease));
  await assertSucceeds(
    adminDb.collection("app_releases").doc("v13_draft").set({
      ...validRelease,
      versionName: "1.3.0",
      versionCode: 13,
      isPublished: false,
    })
  );
  await assertSucceeds(
    adminDb.collection("app_config").doc("update_policy").set({
      checkIntervalMinutes: 360,
      minSupportedVersionCode: 1,
      latestVersionCode: 12,
      trustedApkHosts: ["downloads.fittrack.app"],
      defaultDistributionChannel: "both",
      playUpdatePriorityThreshold: 4,
      updatedAt: ts,
    })
  );

  // Public reads for published release and update policy config
  await assertSucceeds(unauthDb.collection("app_releases").doc("v12").get());
  await assertSucceeds(userDb.collection("app_releases").doc("v12").get());
  await assertSucceeds(unauthDb.collection("app_config").doc("update_policy").get());

  // Draft release & config list remain blocked
  await assertFails(unauthDb.collection("app_releases").doc("v13_draft").get());
  await assertFails(userDb.collection("app_releases").doc("v13_draft").get());
  await assertFails(userDb.collection("app_config").get());

  // Non-admin writes and client /admins escalation blocked
  await assertFails(userDb.collection("app_releases").doc("v14").set(validRelease));
  await assertFails(userDb.collection("app_config").doc("update_policy").update({ latestVersionCode: 99, updatedAt: ts }));
  await assertFails(userDb.collection("admins").doc(ALICE_UID).set({ email: "alice@example.com", role: "release_admin" }));

  // Insecure HTTP APK URL rejected even for admin
  await assertFails(
    adminDb.collection("app_releases").doc("v14_bad").set({
      ...validRelease,
      apkDownloadUrl: "http://insecure.example.com/app.apk",
    })
  );
});

test("Owner can manage recipe/meal_plan meals, ml units, goal_history, recipes, planned_meals, and preferences; cross-user denied", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const aliceDoc = aliceDb.collection("users").doc(ALICE_UID);
  const ts = nowTs();

  await assertSucceeds(
    aliceDoc.collection("meals").doc("meal_recipe").set({
      userId: ALICE_UID,
      dateKey: "2026-11-15",
      mealType: "lunch",
      foodName: "Protein Shake (1.0 srv)",
      grams: 350.0,
      measurementUnit: "ml",
      calories: 280.0,
      proteinG: 32.0,
      carbsG: 18.0,
      fatG: 5.0,
      fiberG: 2.0,
      sugarG: 6.0,
      source: "recipe",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDoc.collection("meals").doc("meal_plan_1").set({
      userId: ALICE_UID,
      dateKey: "2026-11-15",
      mealType: "dinner",
      foodName: "Salmon Bowl",
      grams: 400.0,
      calories: 560.0,
      proteinG: 42.0,
      carbsG: 48.0,
      fatG: 22.0,
      fiberG: 6.0,
      sugarG: 4.0,
      source: "meal_plan",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDoc.collection("goal_history").doc("goal_1").set({
      userId: ALICE_UID,
      goalType: "endurance",
      title: "Run 10K under 50 min",
      status: "active",
      targetValue: 10.0,
      currentValue: 6.5,
      unit: "km",
      targetDateKey: "2026-12-31",
      notes: "Build aerobic base",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDoc.collection("recipes").doc("rec_1").set({
      userId: ALICE_UID,
      name: "Green Recovery Smoothie",
      servings: 1,
      notes: "Post-run shake",
      measurementUnit: "ml",
      ingredients: [{ foodName: "Milk", grams: 300, calories: 150 }],
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDoc.collection("planned_meals").doc("pm_1").set({
      userId: ALICE_UID,
      dayOfWeek: "Monday",
      mealType: "breakfast",
      title: "Greek Yogurt Parfait",
      grams: 250.0,
      measurementUnit: "g",
      calories: 310.0,
      proteinG: 24.0,
      carbsG: 34.0,
      fatG: 8.0,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDoc.collection("preferences").doc("notifications").set({
      userId: ALICE_UID,
      prefType: "notifications",
      payloadJson: JSON.stringify({ hydrationRemindersEnabled: true }),
      updatedAt: ts,
    })
  );

  // Unknown prefType rejected
  await assertFails(
    aliceDoc.collection("preferences").doc("unknown_pref").set({
      userId: ALICE_UID,
      prefType: "unknown_pref",
      payloadJson: "{}",
      updatedAt: ts,
    })
  );

  // Cross-user isolation
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("goal_history").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("recipes").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("planned_meals").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("preferences").get());
});



