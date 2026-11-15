package com.example.data.firebase

import android.content.Context
import com.example.R
import com.example.data.model.AiChatMessage
import com.example.data.model.BodyMeasurement
import com.example.data.model.FoodItem
import com.example.data.model.GpsActivity
import com.example.data.model.MealLog
import com.example.data.model.PersonalRecord
import com.example.data.model.ProgressPhotoNote
import com.example.data.model.StepRecord
import com.example.data.model.TrainingNote
import com.example.data.model.UserChallenge
import com.example.data.model.UserProfile
import com.example.data.model.WaterLog
import com.example.data.model.WeightRecord
import com.example.data.model.WorkoutExerciseEntry
import com.example.data.model.WorkoutPlan
import com.example.data.model.WorkoutProgram
import com.example.data.model.WorkoutScheduleItem
import com.example.data.model.WorkoutSessionLog
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject

class FitTrackRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun userDoc(uid: String) = db.collection("users").document(uid)

    private fun <T> observeCollection(
        uid: String,
        subcollection: String,
        clazz: Class<T>,
        orderByField: String? = null,
        descending: Boolean = true
    ): Flow<List<T>> = flow {
        val path = "users/$uid/$subcollection"
        val baseQuery: Query = if (orderByField != null) {
            userDoc(uid).collection(subcollection)
                .orderBy(
                    orderByField,
                    if (descending) Query.Direction.DESCENDING else Query.Direction.ASCENDING
                )
        } else {
            userDoc(uid).collection(subcollection)
        }
        emitAll(
            baseQuery.snapshots()
                .map { snap ->
                    snap.toObjects(
                        clazz,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeUserProfile(uid: String): Flow<UserProfile?> = flow {
        val path = "users/$uid"
        emitAll(
            userDoc(uid).snapshots()
                .map { snap ->
                    if (snap.exists()) {
                        snap.toObject(
                            UserProfile::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else {
                        null
                    }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    suspend fun getOrCreateProfile(profile: UserProfile): UserProfile {
        val uid = requireUserId()
        val ref = userDoc(uid)
        return try {
            val snap = ref.get().await()
            if (snap.exists()) {
                snap.toObject(
                    UserProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ) ?: profile.copy(id = uid, userId = uid)
            } else {
                val payload = mapOf(
                    "userId" to uid,
                    "displayName" to profile.displayName.take(80).ifBlank { "Athlete" },
                    "email" to profile.email.take(150),
                    "photoUrl" to profile.photoUrl.take(500),
                    "ageYears" to profile.ageYears.coerceIn(13L, 120L),
                    "sex" to profile.sex,
                    "heightCm" to profile.heightCm.coerceIn(80.0, 260.0),
                    "weightKg" to profile.weightKg.coerceIn(20.0, 400.0),
                    "targetWeightKg" to profile.targetWeightKg.coerceIn(20.0, 400.0),
                    "activityLevel" to profile.activityLevel,
                    "goal" to profile.goal,
                    "dailyStepGoal" to profile.dailyStepGoal.coerceIn(1000L, 100000L),
                    "dailyWaterTargetMl" to profile.dailyWaterTargetMl.coerceIn(500L, 10000L),
                    "dailyCalorieTarget" to profile.dailyCalorieTarget.coerceIn(1000L, 10000L),
                    "onboardingCompleted" to profile.onboardingCompleted,
                    "unitSystem" to profile.unitSystem,
                    "themePreference" to profile.themePreference,
                    "sensorConsentGranted" to profile.sensorConsentGranted,
                    "locationConsentGranted" to profile.locationConsentGranted,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                ref.set(payload).await()
                ref.get().await().toObject(
                    UserProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ) ?: profile.copy(id = uid, userId = uid)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, ref.path)
            throw e
        }
    }

    suspend fun updateProfile(profile: UserProfile) {
        val uid = requireUserId()
        val ref = userDoc(uid)
        val payload = mapOf(
            "displayName" to profile.displayName.take(80).ifBlank { "Athlete" },
            "email" to profile.email.take(150),
            "photoUrl" to profile.photoUrl.take(500),
            "ageYears" to profile.ageYears.coerceIn(13L, 120L),
            "sex" to profile.sex,
            "heightCm" to profile.heightCm.coerceIn(80.0, 260.0),
            "weightKg" to profile.weightKg.coerceIn(20.0, 400.0),
            "targetWeightKg" to profile.targetWeightKg.coerceIn(20.0, 400.0),
            "activityLevel" to profile.activityLevel,
            "goal" to profile.goal,
            "dailyStepGoal" to profile.dailyStepGoal.coerceIn(1000L, 100000L),
            "dailyWaterTargetMl" to profile.dailyWaterTargetMl.coerceIn(500L, 10000L),
            "dailyCalorieTarget" to profile.dailyCalorieTarget.coerceIn(1000L, 10000L),
            "onboardingCompleted" to profile.onboardingCompleted,
            "unitSystem" to profile.unitSystem,
            "themePreference" to profile.themePreference,
            "sensorConsentGranted" to profile.sensorConsentGranted,
            "locationConsentGranted" to profile.locationConsentGranted,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            ref.update(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, ref.path)
            throw e
        }
    }

    fun observeStepRecords(uid: String): Flow<List<StepRecord>> =
        observeCollection(uid, "step_records", StepRecord::class.java, "dateKey", true)

    suspend fun upsertStepRecord(record: StepRecord) {
        val uid = requireUserId()
        require(record.dateKey.isNotBlank()) { "dateKey must not be blank" }
        val ref = userDoc(uid).collection("step_records").document(record.dateKey)
        try {
            val snap = ref.get().await()
            if (snap.exists()) {
                ref.update(
                    mapOf(
                        "steps" to record.steps.coerceIn(0L, 500000L),
                        "manualSteps" to record.manualSteps.coerceIn(0L, 500000L),
                        "sensorBaseline" to record.sensorBaseline.coerceIn(0L, 100000000L),
                        "distanceKm" to record.distanceKm.coerceIn(0.0, 500.0),
                        "caloriesBurned" to record.caloriesBurned.coerceIn(0.0, 50000.0),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                ref.set(
                    mapOf(
                        "userId" to uid,
                        "dateKey" to record.dateKey,
                        "steps" to record.steps.coerceIn(0L, 500000L),
                        "manualSteps" to record.manualSteps.coerceIn(0L, 500000L),
                        "sensorBaseline" to record.sensorBaseline.coerceIn(0L, 100000000L),
                        "distanceKm" to record.distanceKm.coerceIn(0.0, 500.0),
                        "caloriesBurned" to record.caloriesBurned.coerceIn(0.0, 50000.0),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, ref.path)
            throw e
        }
    }

    fun observeGpsActivities(uid: String): Flow<List<GpsActivity>> =
        observeCollection(uid, "gps_activities", GpsActivity::class.java, "createdAt", true)

    suspend fun saveGpsActivity(activity: GpsActivity): String {
        val uid = requireUserId()
        val ref = userDoc(uid).collection("gps_activities").document()
        val sampledPoints = activity.routePoints.take(250).map {
            mapOf("lat" to it.lat, "lng" to it.lng, "timestampMs" to it.timestampMs)
        }
        val payload = mapOf(
            "userId" to uid,
            "activityType" to activity.activityType,
            "durationSeconds" to activity.durationSeconds.coerceIn(0L, 86400L),
            "distanceKm" to activity.distanceKm.coerceIn(0.0, 1000.0),
            "avgSpeedKmh" to activity.avgSpeedKmh.coerceIn(0.0, 200.0),
            "avgPaceMinPerKm" to activity.avgPaceMinPerKm.coerceIn(0.0, 120.0),
            "caloriesBurned" to activity.caloriesBurned.coerceIn(0.0, 20000.0),
            "routePoints" to sampledPoints,
            "notes" to activity.notes.take(300),
            "startedAtMs" to activity.startedAtMs.coerceAtLeast(0L),
            "endedAtMs" to activity.endedAtMs.coerceAtLeast(activity.startedAtMs),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            ref.set(payload).await()
            ref.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, ref.path)
            throw e
        }
    }

    suspend fun deleteGpsActivity(activityId: String) {
        val uid = requireUserId()
        val ref = userDoc(uid).collection("gps_activities").document(activityId)
        try {
            ref.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, ref.path)
            throw e
        }
    }

    fun observeCustomFoods(uid: String): Flow<List<FoodItem>> =
        observeCollection(uid, "custom_foods", FoodItem::class.java, "createdAt", true)

    suspend fun saveCustomFood(food: FoodItem): String {
        val uid = requireUserId()
        val ref = if (food.id.isNotBlank()) {
            userDoc(uid).collection("custom_foods").document(food.id)
        } else {
            userDoc(uid).collection("custom_foods").document()
        }
        val payload = mapOf(
            "userId" to uid,
            "name" to food.name.take(100).ifBlank { "Custom Food" },
            "brand" to food.brand.take(100),
            "servingSizeGrams" to food.servingSizeGrams.coerceIn(1.0, 5000.0),
            "servingLabel" to food.servingLabel.take(50).ifBlank { "100g" },
            "caloriesPer100g" to food.caloriesPer100g.coerceIn(0.0, 1500.0),
            "proteinPer100g" to food.proteinPer100g.coerceIn(0.0, 100.0),
            "carbsPer100g" to food.carbsPer100g.coerceIn(0.0, 100.0),
            "fatPer100g" to food.fatPer100g.coerceIn(0.0, 100.0),
            "fiberPer100g" to food.fiberPer100g.coerceIn(0.0, 100.0),
            "sugarPer100g" to food.sugarPer100g.coerceIn(0.0, 100.0),
            "isFavorite" to food.isFavorite,
            "isCustom" to food.isCustom,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            ref.set(payload).await()
            ref.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, ref.path)
            throw e
        }
    }

    suspend fun toggleFoodFavorite(food: FoodItem) {
        val uid = requireUserId()
        require(food.id.isNotBlank()) { "Food id required" }
        val ref = userDoc(uid).collection("custom_foods").document(food.id)
        try {
            ref.update(
                mapOf(
                    "isFavorite" to !food.isFavorite,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, ref.path)
            throw e
        }
    }

    fun observeMeals(uid: String): Flow<List<MealLog>> =
        observeCollection(uid, "meals", MealLog::class.java, "createdAt", true)

    suspend fun addMeal(meal: MealLog): String {
        val uid = requireUserId()
        val ref = userDoc(uid).collection("meals").document()
        val payload = mapOf(
            "userId" to uid,
            "dateKey" to meal.dateKey,
            "mealType" to meal.mealType,
            "foodName" to meal.foodName.take(120).ifBlank { "Food" },
            "grams" to meal.grams.coerceIn(1.0, 5000.0),
            "calories" to meal.calories.coerceIn(0.0, 10000.0),
            "proteinG" to meal.proteinG.coerceIn(0.0, 1000.0),
            "carbsG" to meal.carbsG.coerceIn(0.0, 1000.0),
            "fatG" to meal.fatG.coerceIn(0.0, 1000.0),
            "fiberG" to meal.fiberG.coerceIn(0.0, 500.0),
            "sugarG" to meal.sugarG.coerceIn(0.0, 500.0),
            "source" to meal.source,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            ref.set(payload).await()
            ref.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, ref.path)
            throw e
        }
    }

    suspend fun updateMeal(meal: MealLog) {
        val uid = requireUserId()
        require(meal.id.isNotBlank()) { "Meal ID required" }
        val ref = userDoc(uid).collection("meals").document(meal.id)
        try {
            ref.update(
                mapOf(
                    "mealType" to meal.mealType,
                    "grams" to meal.grams.coerceIn(1.0, 5000.0),
                    "calories" to meal.calories.coerceIn(0.0, 10000.0),
                    "proteinG" to meal.proteinG.coerceIn(0.0, 1000.0),
                    "carbsG" to meal.carbsG.coerceIn(0.0, 1000.0),
                    "fatG" to meal.fatG.coerceIn(0.0, 1000.0),
                    "fiberG" to meal.fiberG.coerceIn(0.0, 500.0),
                    "sugarG" to meal.sugarG.coerceIn(0.0, 500.0),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, ref.path)
            throw e
        }
    }

    suspend fun deleteMeal(mealId: String) {
        val uid = requireUserId()
        val ref = userDoc(uid).collection("meals").document(mealId)
        try {
            ref.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, ref.path)
            throw e
        }
    }

    fun observeWaterLogs(uid: String): Flow<List<WaterLog>> =
        observeCollection(uid, "water_logs", WaterLog::class.java, "createdAt", true)

    suspend fun addWaterLog(dateKey: String, amountMl: Long): String {
        val uid = requireUserId()
        val ref = userDoc(uid).collection("water_logs").document()
        val payload = mapOf(
            "userId" to uid,
            "dateKey" to dateKey,
            "amountMl" to amountMl.coerceIn(10L, 5000L),
            "createdAt" to FieldValue.serverTimestamp()
        )
        return try {
            ref.set(payload).await()
            ref.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, ref.path)
            throw e
        }
    }

    suspend fun deleteWaterLog(logId: String) {
        val uid = requireUserId()
        val ref = userDoc(uid).collection("water_logs").document(logId)
        try {
            ref.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, ref.path)
            throw e
        }
    }

    private fun serializeExerciseEntries(entries: List<WorkoutExerciseEntry>): List<Map<String, Any>> =
        entries.take(30).map {
            mapOf(
                "exerciseId" to it.exerciseId.take(64),
                "exerciseName" to it.exerciseName.take(100),
                "muscleGroup" to it.muscleGroup.take(40),
                "secondaryMuscles" to it.secondaryMuscles.take(8),
                "equipment" to it.equipment.take(40),
                "targetSets" to it.targetSets.coerceIn(1L, 20L),
                "targetReps" to it.targetReps.coerceIn(1L, 200L),
                "repRangeMin" to it.repRangeMin.coerceIn(1L, 200L),
                "repRangeMax" to it.repRangeMax.coerceIn(1L, 200L),
                "targetWeightKg" to it.targetWeightKg.coerceIn(0.0, 1000.0),
                "restSeconds" to it.restSeconds.coerceIn(0L, 600L),
                "setType" to it.setType.take(20),
                "supersetGroup" to it.supersetGroup.take(20),
                "notes" to it.notes.take(200)
            )
        }

    fun observeWorkouts(uid: String): Flow<List<WorkoutPlan>> =
        observeCollection(uid, "workouts", WorkoutPlan::class.java, "createdAt", true)

    suspend fun saveWorkoutPlan(plan: WorkoutPlan): String {
        val uid = requireUserId()
        val ref = if (plan.id.isNotBlank()) {
            userDoc(uid).collection("workouts").document(plan.id)
        } else {
            userDoc(uid).collection("workouts").document()
        }
        val exercisesPayload = serializeExerciseEntries(plan.exercises)
        return try {
            if (plan.id.isNotBlank() && ref.get().await().exists()) {
                ref.update(
                    mapOf(
                        "name" to plan.name.take(100).ifBlank { "Workout" },
                        "level" to plan.level,
                        "targetGoal" to plan.targetGoal.take(60),
                        "programId" to plan.programId.take(64),
                        "programName" to plan.programName.take(100),
                        "dayLabel" to plan.dayLabel.take(40),
                        "progressionNote" to plan.progressionNote.take(300),
                        "exercises" to exercisesPayload,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                ref.set(
                    mapOf(
                        "userId" to uid,
                        "name" to plan.name.take(100).ifBlank { "Workout" },
                        "level" to plan.level,
                        "targetGoal" to plan.targetGoal.take(60),
                        "programId" to plan.programId.take(64),
                        "programName" to plan.programName.take(100),
                        "dayLabel" to plan.dayLabel.take(40),
                        "progressionNote" to plan.progressionNote.take(300),
                        "exercises" to exercisesPayload,
                        "isAiGenerated" to plan.isAiGenerated,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            ref.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, ref.path)
            throw e
        }
    }

    suspend fun deleteWorkoutPlan(workoutId: String) {
        val uid = requireUserId()
        val ref = userDoc(uid).collection("workouts").document(workoutId)
        try {
            ref.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, ref.path)
            throw e
        }
    }

    fun observeWorkoutSessions(uid: String): Flow<List<WorkoutSessionLog>> =
        observeCollection(uid, "workout_sessions", WorkoutSessionLog::class.java, "createdAt", true)

    suspend fun saveWorkoutSession(session: WorkoutSessionLog): String {
        val uid = requireUserId()
        val ref = userDoc(uid).collection("workout_sessions").document()
        val exerciseRecordsPayload = session.exerciseRecords.take(30).map { rec ->
            mapOf(
                "exerciseId" to rec.exerciseId.take(64),
                "exerciseName" to rec.exerciseName.take(100),
                "primaryMuscle" to rec.primaryMuscle.take(40),
                "secondaryMuscles" to rec.secondaryMuscles.take(8),
                "equipment" to rec.equipment.take(40),
                "skipped" to rec.skipped,
                "replacedFromExerciseName" to rec.replacedFromExerciseName.take(100),
                "notes" to rec.notes.take(300),
                "sets" to rec.sets.take(30).map { s ->
                    mapOf(
                        "setNumber" to s.setNumber,
                        "setType" to s.setType.take(20),
                        "reps" to s.reps,
                        "weightKg" to s.weightKg,
                        "completedAtMs" to s.completedAtMs
                    )
                }
            )
        }
        val payload = mapOf(
            "userId" to uid,
            "workoutId" to session.workoutId.take(128),
            "workoutName" to session.workoutName.take(100).ifBlank { "Workout" },
            "programId" to session.programId.take(64),
            "dateKey" to session.dateKey.take(16),
            "notes" to session.notes.take(400),
            "durationSeconds" to session.durationSeconds.coerceIn(0L, 86400L),
            "totalVolumeKg" to session.totalVolumeKg.coerceIn(0.0, 500000.0),
            "warmupVolumeKg" to session.warmupVolumeKg.coerceIn(0.0, 500000.0),
            "workingVolumeKg" to session.workingVolumeKg.coerceIn(0.0, 500000.0),
            "caloriesBurned" to session.caloriesBurned.coerceIn(0.0, 10000.0),
            "setsCompleted" to session.setsCompleted.coerceIn(0L, 500L),
            "personalRecordsHit" to session.personalRecordsHit.coerceIn(0L, 100L),
            "completedSetsSummary" to session.completedSetsSummary.take(100),
            "exerciseRecords" to exerciseRecordsPayload,
            "createdAt" to FieldValue.serverTimestamp()
        )
        return try {
            ref.set(payload).await()
            ref.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, ref.path)
            throw e
        }
    }

    fun observeWorkoutPrograms(uid: String): Flow<List<WorkoutProgram>> =
        observeCollection(uid, "workout_programs", WorkoutProgram::class.java, "createdAt", true)

    suspend fun saveWorkoutProgram(program: WorkoutProgram): String {
        val uid = requireUserId()
        val ref = if (program.id.isNotBlank()) {
            userDoc(uid).collection("workout_programs").document(program.id)
        } else {
            userDoc(uid).collection("workout_programs").document()
        }
        val workoutsPayload = program.workouts.take(14).map { wp ->
            mapOf(
                "id" to wp.id.take(64),
                "name" to wp.name.take(100),
                "level" to wp.level,
                "targetGoal" to wp.targetGoal.take(60),
                "dayLabel" to wp.dayLabel.take(40),
                "progressionNote" to wp.progressionNote.take(300),
                "exercises" to serializeExerciseEntries(wp.exercises)
            )
        }
        return try {
            if (program.id.isNotBlank() && ref.get().await().exists()) {
                ref.update(
                    mapOf(
                        "name" to program.name.take(100).ifBlank { "Custom Program" },
                        "category" to program.category.take(40),
                        "goal" to program.goal.take(60),
                        "level" to program.level,
                        "daysPerWeek" to program.daysPerWeek.coerceIn(1L, 7L),
                        "description" to program.description.take(400),
                        "weeklySplit" to program.weeklySplit.take(14),
                        "progressionGuidance" to program.progressionGuidance.take(400),
                        "workouts" to workoutsPayload,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                ref.set(
                    mapOf(
                        "userId" to uid,
                        "name" to program.name.take(100).ifBlank { "Custom Program" },
                        "category" to program.category.take(40),
                        "goal" to program.goal.take(60),
                        "level" to program.level,
                        "daysPerWeek" to program.daysPerWeek.coerceIn(1L, 7L),
                        "description" to program.description.take(400),
                        "weeklySplit" to program.weeklySplit.take(14),
                        "progressionGuidance" to program.progressionGuidance.take(400),
                        "workouts" to workoutsPayload,
                        "isCustom" to program.isCustom,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            ref.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, ref.path)
            throw e
        }
    }

    suspend fun deleteWorkoutProgram(programId: String) {
        val uid = requireUserId()
        val ref = userDoc(uid).collection("workout_programs").document(programId)
        try {
            ref.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, ref.path)
            throw e
        }
    }

    fun observeWorkoutSchedules(uid: String): Flow<List<WorkoutScheduleItem>> =
        observeCollection(uid, "workout_schedules", WorkoutScheduleItem::class.java, "dateKey", false)

    suspend fun upsertWorkoutSchedule(item: WorkoutScheduleItem): String {
        val uid = requireUserId()
        require(item.dateKey.isNotBlank()) { "dateKey must not be blank" }
        val docId = item.id.ifBlank { item.dateKey }
        val ref = userDoc(uid).collection("workout_schedules").document(docId)
        return try {
            val snap = ref.get().await()
            if (snap.exists()) {
                ref.update(
                    mapOf(
                        "dateKey" to item.dateKey.take(16),
                        "workoutId" to item.workoutId.take(128),
                        "workoutName" to item.workoutName.take(100),
                        "programId" to item.programId.take(64),
                        "status" to item.status,
                        "notes" to item.notes.take(300),
                        "completedSessionId" to item.completedSessionId.take(128),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                ref.set(
                    mapOf(
                        "userId" to uid,
                        "dateKey" to item.dateKey.take(16),
                        "workoutId" to item.workoutId.take(128),
                        "workoutName" to item.workoutName.take(100),
                        "programId" to item.programId.take(64),
                        "status" to item.status,
                        "notes" to item.notes.take(300),
                        "completedSessionId" to item.completedSessionId.take(128),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            ref.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, ref.path)
            throw e
        }
    }

    suspend fun deleteWorkoutSchedule(scheduleId: String) {
        val uid = requireUserId()
        val ref = userDoc(uid).collection("workout_schedules").document(scheduleId)
        try {
            ref.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, ref.path)
            throw e
        }
    }

    fun observePersonalRecords(uid: String): Flow<List<PersonalRecord>> =
        observeCollection(uid, "personal_records", PersonalRecord::class.java, "estimatedOneRmKg", true)

    suspend fun upsertPersonalRecord(pr: PersonalRecord): String {
        val uid = requireUserId()
        val docId = pr.id.ifBlank {
            (pr.exerciseId.ifBlank { pr.exerciseName.lowercase().replace(Regex("[^a-z0-9]+"), "_") }).take(64)
        }
        val ref = userDoc(uid).collection("personal_records").document(docId)
        val payload = mapOf(
            "userId" to uid,
            "exerciseId" to pr.exerciseId.take(64),
            "exerciseName" to pr.exerciseName.take(100),
            "primaryMuscle" to pr.primaryMuscle.take(40),
            "weightKg" to pr.weightKg.coerceIn(0.0, 1000.0),
            "reps" to pr.reps.coerceIn(1L, 500L),
            "estimatedOneRmKg" to pr.estimatedOneRmKg.coerceIn(0.0, 1500.0),
            "setVolumeKg" to pr.setVolumeKg.coerceIn(0.0, 50000.0),
            "dateKey" to pr.dateKey.take(16),
            "sessionId" to pr.sessionId.take(128),
            "createdAt" to FieldValue.serverTimestamp()
        )
        return try {
            ref.set(payload).await()
            ref.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, ref.path)
            throw e
        }
    }

    fun observeTrainingNotes(uid: String): Flow<List<TrainingNote>> =
        observeCollection(uid, "training_notes", TrainingNote::class.java, "createdAt", true)

    suspend fun saveTrainingNote(note: TrainingNote): String {
        val uid = requireUserId()
        val ref = if (note.id.isNotBlank()) {
            userDoc(uid).collection("training_notes").document(note.id)
        } else {
            userDoc(uid).collection("training_notes").document()
        }
        return try {
            if (note.id.isNotBlank() && ref.get().await().exists()) {
                ref.update(
                    mapOf(
                        "title" to note.title.take(100).ifBlank { "Training Note" },
                        "body" to note.body.take(2000),
                        "dateKey" to note.dateKey.take(16),
                        "exerciseName" to note.exerciseName.take(100),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                ref.set(
                    mapOf(
                        "userId" to uid,
                        "title" to note.title.take(100).ifBlank { "Training Note" },
                        "body" to note.body.take(2000),
                        "dateKey" to note.dateKey.take(16),
                        "exerciseName" to note.exerciseName.take(100),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            ref.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, ref.path)
            throw e
        }
    }

    suspend fun deleteTrainingNote(noteId: String) {
        val uid = requireUserId()
        val ref = userDoc(uid).collection("training_notes").document(noteId)
        try {
            ref.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, ref.path)
            throw e
        }
    }

    fun observeWeightRecords(uid: String): Flow<List<WeightRecord>> =
        observeCollection(uid, "weight_records", WeightRecord::class.java, "createdAt", true)

    suspend fun addWeightRecord(dateKey: String, weightKg: Double, note: String = ""): String {
        val uid = requireUserId()
        val ref = userDoc(uid).collection("weight_records").document()
        val payload = mapOf(
            "userId" to uid,
            "dateKey" to dateKey,
            "weightKg" to weightKg.coerceIn(20.0, 400.0),
            "note" to note.take(200),
            "createdAt" to FieldValue.serverTimestamp()
        )
        return try {
            ref.set(payload).await()
            ref.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, ref.path)
            throw e
        }
    }

    fun observeBodyMeasurements(uid: String): Flow<List<BodyMeasurement>> =
        observeCollection(uid, "body_measurements", BodyMeasurement::class.java, "createdAt", true)

    suspend fun addBodyMeasurement(measurement: BodyMeasurement): String {
        val uid = requireUserId()
        val ref = userDoc(uid).collection("body_measurements").document()
        val payload = mapOf(
            "userId" to uid,
            "dateKey" to measurement.dateKey,
            "waistCm" to measurement.waistCm.coerceIn(0.0, 250.0),
            "chestCm" to measurement.chestCm.coerceIn(0.0, 250.0),
            "armsCm" to measurement.armsCm.coerceIn(0.0, 100.0),
            "legsCm" to measurement.legsCm.coerceIn(0.0, 150.0),
            "hipsCm" to measurement.hipsCm.coerceIn(0.0, 250.0),
            "createdAt" to FieldValue.serverTimestamp()
        )
        return try {
            ref.set(payload).await()
            ref.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, ref.path)
            throw e
        }
    }

    fun observeChallenges(uid: String): Flow<List<UserChallenge>> =
        observeCollection(uid, "challenges", UserChallenge::class.java, "createdAt", true)

    suspend fun upsertChallenge(challenge: UserChallenge) {
        val uid = requireUserId()
        val docId = challenge.id.ifBlank { challenge.title.lowercase().replace(Regex("[^a-z0-9]+"), "_").take(64) }
        val ref = userDoc(uid).collection("challenges").document(docId)
        try {
            val snap = ref.get().await()
            if (snap.exists()) {
                ref.update(
                    mapOf(
                        "currentValue" to challenge.currentValue.coerceIn(0.0, 1000000.0),
                        "joined" to challenge.joined,
                        "completed" to challenge.completed,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                ref.set(
                    mapOf(
                        "userId" to uid,
                        "title" to challenge.title.take(100),
                        "description" to challenge.description.take(300),
                        "category" to challenge.category.take(40),
                        "targetValue" to challenge.targetValue.coerceIn(1.0, 1000000.0),
                        "currentValue" to challenge.currentValue.coerceIn(0.0, 1000000.0),
                        "unit" to challenge.unit.take(30),
                        "joined" to challenge.joined,
                        "completed" to challenge.completed,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, ref.path)
            throw e
        }
    }

    fun observeAiMessages(uid: String): Flow<List<AiChatMessage>> =
        observeCollection(uid, "ai_messages", AiChatMessage::class.java, "createdAt", false)

    suspend fun addAiMessage(role: String, text: String) {
        val uid = requireUserId()
        val ref = userDoc(uid).collection("ai_messages").document()
        val payload = mapOf(
            "userId" to uid,
            "role" to role,
            "text" to text.take(8000).ifBlank { "..." },
            "createdAt" to FieldValue.serverTimestamp()
        )
        try {
            ref.set(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, ref.path)
            throw e
        }
    }

    fun observeProgressPhotos(uid: String): Flow<List<ProgressPhotoNote>> =
        observeCollection(uid, "progress_photos", ProgressPhotoNote::class.java, "createdAt", true)

    suspend fun addProgressPhoto(localUri: String, weightKg: Double, caption: String, dateKey: String) {
        val uid = requireUserId()
        val ref = userDoc(uid).collection("progress_photos").document()
        val payload = mapOf(
            "userId" to uid,
            "localUri" to localUri.take(1000),
            "weightKg" to weightKg.coerceIn(0.0, 400.0),
            "caption" to caption.take(200),
            "dateKey" to dateKey,
            "createdAt" to FieldValue.serverTimestamp()
        )
        try {
            ref.set(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, ref.path)
            throw e
        }
    }

    suspend fun deleteProgressPhoto(photoId: String) {
        val uid = requireUserId()
        val ref = userDoc(uid).collection("progress_photos").document(photoId)
        try {
            ref.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, ref.path)
            throw e
        }
    }

    suspend fun exportAllUserDataJson(): String {
        val uid = requireUserId()
        val root = JSONObject()
        try {
            val profileSnap = userDoc(uid).get().await()
            root.put("userProfile", JSONObject(profileSnap.data ?: emptyMap<String, Any>()))
            val subcollections = listOf(
                "step_records", "gps_activities", "custom_foods", "meals",
                "water_logs", "workouts", "workout_sessions", "workout_programs",
                "workout_schedules", "personal_records", "training_notes", "weight_records",
                "body_measurements", "challenges", "ai_messages", "progress_photos"
            )
            for (sub in subcollections) {
                val snaps = userDoc(uid).collection(sub).get().await()
                val arr = JSONArray()
                for (doc in snaps.documents) {
                    arr.put(JSONObject(doc.data ?: emptyMap<String, Any>()))
                }
                root.put(sub, arr)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, "users/$uid")
            throw e
        }
        return root.toString(2)
    }

    suspend fun deleteAllUserData() {
        val uid = requireUserId()
        val subcollections = listOf(
            "step_records", "gps_activities", "custom_foods", "meals",
            "water_logs", "workouts", "workout_sessions", "workout_programs",
            "workout_schedules", "personal_records", "training_notes", "weight_records",
            "body_measurements", "challenges", "ai_messages", "progress_photos"
        )
        try {
            for (sub in subcollections) {
                val snaps = userDoc(uid).collection(sub).get().await()
                for (doc in snaps.documents) {
                    doc.reference.delete().await()
                }
            }
            userDoc(uid).delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "users/$uid")
            throw e
        }
    }
}
