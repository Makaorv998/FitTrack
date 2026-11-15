package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "Athlete",
    val email: String = "",
    val photoUrl: String = "",
    val ageYears: Long = 28L,
    val dateOfBirthIso: String = "",
    val sex: String = "male",
    val heightCm: Double = 175.0,
    val weightKg: Double = 72.0,
    val targetWeightKg: Double = 70.0,
    val activityLevel: String = "moderate",
    val goal: String = "general_fitness",
    val dailyStepGoal: Long = 10000L,
    val dailyWaterTargetMl: Long = 2500L,
    val dailyCalorieTarget: Long = 2200L,
    val onboardingCompleted: Boolean = false,
    val unitSystem: String = "metric",
    val themePreference: String = "dark",
    val reducedMotionEnabled: Boolean = false,
    val sensorConsentGranted: Boolean = false,
    val locationConsentGranted: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class StepRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val dateKey: String = "",
    val steps: Long = 0L,
    val manualSteps: Long = 0L,
    val sensorBaseline: Long = 0L,
    val distanceKm: Double = 0.0,
    val caloriesBurned: Double = 0.0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class LatLngPoint(
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val timestampMs: Long = 0L
)

@IgnoreExtraProperties
data class GpsActivity(
    @DocumentId val id: String = "",
    val userId: String = "",
    val activityType: String = "running",
    val durationSeconds: Long = 0L,
    val distanceKm: Double = 0.0,
    val avgSpeedKmh: Double = 0.0,
    val avgPaceMinPerKm: Double = 0.0,
    val caloriesBurned: Double = 0.0,
    val routePoints: List<LatLngPoint> = emptyList(),
    val notes: String = "",
    val startedAtMs: Long = 0L,
    val endedAtMs: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FoodItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val brand: String = "",
    val servingSizeGrams: Double = 100.0,
    val servingLabel: String = "100g",
    val measurementUnit: String = "g",
    val caloriesPer100g: Double = 0.0,
    val proteinPer100g: Double = 0.0,
    val carbsPer100g: Double = 0.0,
    val fatPer100g: Double = 0.0,
    val fiberPer100g: Double = 0.0,
    val sugarPer100g: Double = 0.0,
    val isFavorite: Boolean = false,
    val isCustom: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class MealLog(
    @DocumentId val id: String = "",
    val userId: String = "",
    val dateKey: String = "",
    val mealType: String = "breakfast",
    val foodName: String = "",
    val grams: Double = 100.0,
    val measurementUnit: String = "g",
    val calories: Double = 0.0,
    val proteinG: Double = 0.0,
    val carbsG: Double = 0.0,
    val fatG: Double = 0.0,
    val fiberG: Double = 0.0,
    val sugarG: Double = 0.0,
    val source: String = "manual",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class WaterLog(
    @DocumentId val id: String = "",
    val userId: String = "",
    val dateKey: String = "",
    val amountMl: Long = 250L,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class WorkoutExerciseEntry(
    val exerciseId: String = "",
    val exerciseName: String = "",
    val muscleGroup: String = "",
    val equipment: String = "",
    val targetSets: Long = 3L,
    val targetReps: Long = 10L,
    val targetWeightKg: Double = 0.0,
    val restSeconds: Long = 90L,
    val secondaryMuscles: List<String> = emptyList(),
    val repRangeMin: Long = 8L,
    val repRangeMax: Long = 12L,
    val setType: String = "working",
    val supersetGroup: String = "",
    val notes: String = ""
)

@IgnoreExtraProperties
data class WorkoutPlan(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val level: String = "intermediate",
    val targetGoal: String = "general_fitness",
    val exercises: List<WorkoutExerciseEntry> = emptyList(),
    val isAiGenerated: Boolean = false,
    val programId: String = "",
    val programName: String = "",
    val dayLabel: String = "",
    val progressionNote: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

fun WorkoutPlan.estimatedDurationMinutes(): Int {
    if (exercises.isEmpty()) return 0
    val totalSeconds = exercises.sumOf { ex ->
        val setCount = ex.targetSets.coerceAtLeast(1L)
        val workSec = 40L * setCount
        val restSec = ex.restSeconds.coerceAtLeast(15L) * (setCount - 1L).coerceAtLeast(1L)
        workSec + restSec
    }
    return ((totalSeconds + 59L) / 60L).toInt().coerceAtLeast(10)
}

@IgnoreExtraProperties
data class CompletedSetRecord(
    val setNumber: Long = 1L,
    val setType: String = "working",
    val reps: Long = 10L,
    val weightKg: Double = 0.0,
    val completedAtMs: Long = 0L
)

@IgnoreExtraProperties
data class ExerciseSessionRecord(
    val exerciseId: String = "",
    val exerciseName: String = "",
    val primaryMuscle: String = "",
    val secondaryMuscles: List<String> = emptyList(),
    val equipment: String = "",
    val skipped: Boolean = false,
    val replacedFromExerciseName: String = "",
    val notes: String = "",
    val sets: List<CompletedSetRecord> = emptyList()
)

@IgnoreExtraProperties
data class WorkoutSessionLog(
    @DocumentId val id: String = "",
    val userId: String = "",
    val workoutId: String = "",
    val workoutName: String = "",
    val durationSeconds: Long = 0L,
    val totalVolumeKg: Double = 0.0,
    val caloriesBurned: Double = 0.0,
    val setsCompleted: Long = 0L,
    val personalRecordsHit: Long = 0L,
    val completedSetsSummary: List<String> = emptyList(),
    val programId: String = "",
    val dateKey: String = "",
    val notes: String = "",
    val warmupVolumeKg: Double = 0.0,
    val workingVolumeKg: Double = 0.0,
    val exerciseRecords: List<ExerciseSessionRecord> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class WorkoutProgram(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val category: String = "custom",
    val goal: String = "general_fitness",
    val level: String = "intermediate",
    val daysPerWeek: Long = 3L,
    val description: String = "",
    val weeklySplit: List<String> = emptyList(),
    val progressionGuidance: String = "",
    val workouts: List<WorkoutPlan> = emptyList(),
    val isCustom: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class WorkoutScheduleItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val dateKey: String = "",
    val workoutId: String = "",
    val workoutName: String = "",
    val programId: String = "",
    val status: String = "scheduled",
    val notes: String = "",
    val completedSessionId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class PersonalRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val exerciseId: String = "",
    val exerciseName: String = "",
    val primaryMuscle: String = "",
    val weightKg: Double = 0.0,
    val reps: Long = 1L,
    val estimatedOneRmKg: Double = 0.0,
    val setVolumeKg: Double = 0.0,
    val dateKey: String = "",
    val sessionId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class TrainingNote(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val body: String = "",
    val dateKey: String = "",
    val exerciseName: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class WeightRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val dateKey: String = "",
    val weightKg: Double = 70.0,
    val note: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class BodyMeasurement(
    @DocumentId val id: String = "",
    val userId: String = "",
    val dateKey: String = "",
    val waistCm: Double = 0.0,
    val chestCm: Double = 0.0,
    val armsCm: Double = 0.0,
    val legsCm: Double = 0.0,
    val hipsCm: Double = 0.0,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class UserChallenge(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = "steps",
    val targetValue: Double = 10000.0,
    val currentValue: Double = 0.0,
    val unit: String = "steps",
    val joined: Boolean = false,
    val completed: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class AiChatMessage(
    @DocumentId val id: String = "",
    val userId: String = "",
    val role: String = "user",
    val text: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ProgressPhotoNote(
    @DocumentId val id: String = "",
    val userId: String = "",
    val localUri: String = "",
    val weightKg: Double = 0.0,
    val caption: String = "",
    val dateKey: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class GoalHistoryEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val goalType: String = "general_fitness",
    val title: String = "",
    val status: String = "active",
    val targetValue: Double = 0.0,
    val currentValue: Double = 0.0,
    val unit: String = "kg",
    val targetDateKey: String = "",
    val completedDateKey: String = "",
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class ExerciseDefinition(
    val id: String,
    val name: String,
    val muscleGroup: String,
    val equipment: String,
    val level: String,
    val metValue: Double,
    val instructions: String,
    val defaultSets: Int = 3,
    val defaultReps: Int = 12,
    val secondaryMuscles: List<String> = emptyList(),
    val steps: List<String> = emptyList(),
    val techniqueGuidance: String = "",
    val commonMistakes: List<String> = emptyList(),
    val alternativeIds: List<String> = emptyList(),
    val defaultRepMin: Int = 8,
    val defaultRepMax: Int = 12,
    val defaultRestSeconds: Int = 90,
    val illustrationPose: String = "press"
)
