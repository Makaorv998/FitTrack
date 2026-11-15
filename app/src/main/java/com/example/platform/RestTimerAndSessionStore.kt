package com.example.platform

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.CompletedSetRecord
import com.example.data.model.ExerciseSessionRecord
import com.example.data.model.WorkoutExerciseEntry
import com.example.data.model.WorkoutPlan
import com.example.data.model.WorkoutSessionLog
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.workoutDataStore: DataStore<Preferences> by preferencesDataStore(name = "fittrack_workout_store")

@Serializable
data class RestTimerSettings(
    val defaultRestSeconds: Int = 90,
    val warmupRestSeconds: Int = 45,
    val autoStartRest: Boolean = true,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val presets: List<Int> = listOf(45, 60, 90, 120, 180)
)

@Serializable
data class PersistedExerciseEntry(
    val exerciseId: String = "",
    val exerciseName: String = "",
    val muscleGroup: String = "",
    val secondaryMuscles: List<String> = emptyList(),
    val equipment: String = "",
    val targetSets: Long = 3L,
    val targetReps: Long = 10L,
    val repRangeMin: Long = 8L,
    val repRangeMax: Long = 12L,
    val targetWeightKg: Double = 0.0,
    val restSeconds: Long = 90L,
    val setType: String = "working",
    val supersetGroup: String = "",
    val notes: String = ""
)

@Serializable
data class PersistedSetRecord(
    val setNumber: Long = 1L,
    val setType: String = "working",
    val reps: Long = 10L,
    val weightKg: Double = 0.0,
    val completedAtMs: Long = 0L
)

@Serializable
data class PersistedExerciseSessionRecord(
    val exerciseId: String = "",
    val exerciseName: String = "",
    val primaryMuscle: String = "",
    val secondaryMuscles: List<String> = emptyList(),
    val equipment: String = "",
    val skipped: Boolean = false,
    val replacedFromExerciseName: String = "",
    val notes: String = "",
    val sets: List<PersistedSetRecord> = emptyList()
)

@Serializable
data class PersistedActiveWorkout(
    val userId: String = "",
    val planId: String = "",
    val planName: String = "",
    val planLevel: String = "intermediate",
    val planGoal: String = "general_fitness",
    val programId: String = "",
    val exercises: List<PersistedExerciseEntry> = emptyList(),
    val records: List<PersistedExerciseSessionRecord> = emptyList(),
    val currentExerciseIndex: Int = 0,
    val elapsedSeconds: Long = 0L,
    val isPaused: Boolean = false,
    val isResting: Boolean = false,
    val restTotalSeconds: Int = 90,
    val restSecondsRemaining: Int = 0,
    val restPaused: Boolean = false,
    val restEndEpochMs: Long = 0L,
    val restExerciseName: String = "",
    val sessionNotes: String = "",
    val startedAtMs: Long = 0L
)

@Serializable
data class PendingWorkoutSessionPayload(
    val userId: String = "",
    val workoutId: String = "",
    val workoutName: String = "",
    val programId: String = "",
    val dateKey: String = "",
    val durationSeconds: Long = 0L,
    val totalVolumeKg: Double = 0.0,
    val warmupVolumeKg: Double = 0.0,
    val workingVolumeKg: Double = 0.0,
    val caloriesBurned: Double = 0.0,
    val setsCompleted: Long = 0L,
    val personalRecordsHit: Long = 0L,
    val notes: String = "",
    val completedSetsSummary: List<String> = emptyList(),
    val exerciseRecords: List<PersistedExerciseSessionRecord> = emptyList()
)

class RestTimerAndSessionStore(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private fun activeKey(uid: String) = stringPreferencesKey("active_workout_$uid")
    private fun settingsKey(uid: String) = stringPreferencesKey("rest_settings_$uid")
    private fun pendingQueueKey(uid: String) = stringPreferencesKey("pending_sessions_$uid")

    suspend fun loadRestSettings(uid: String): RestTimerSettings {
        val raw = context.workoutDataStore.data.first()[settingsKey(uid)] ?: return RestTimerSettings()
        return runCatching { json.decodeFromString<RestTimerSettings>(raw) }.getOrDefault(RestTimerSettings())
    }

    suspend fun saveRestSettings(uid: String, settings: RestTimerSettings) {
        context.workoutDataStore.edit { prefs ->
            prefs[settingsKey(uid)] = json.encodeToString(settings)
        }
    }

    suspend fun loadActiveWorkout(uid: String): PersistedActiveWorkout? {
        val raw = context.workoutDataStore.data.first()[activeKey(uid)] ?: return null
        return runCatching { json.decodeFromString<PersistedActiveWorkout>(raw) }.getOrNull()
    }

    suspend fun saveActiveWorkout(uid: String, state: PersistedActiveWorkout) {
        context.workoutDataStore.edit { prefs ->
            prefs[activeKey(uid)] = json.encodeToString(state)
        }
    }

    suspend fun clearActiveWorkout(uid: String) {
        context.workoutDataStore.edit { prefs ->
            prefs.remove(activeKey(uid))
        }
        cancelRestAlarm()
    }

    suspend fun enqueuePendingSession(uid: String, session: WorkoutSessionLog) {
        val current = loadPendingSessions(uid).toMutableList()
        current.add(session.toPendingPayload())
        context.workoutDataStore.edit { prefs ->
            prefs[pendingQueueKey(uid)] = json.encodeToString(current)
        }
    }

    suspend fun loadPendingSessions(uid: String): List<PendingWorkoutSessionPayload> {
        val raw = context.workoutDataStore.data.first()[pendingQueueKey(uid)] ?: return emptyList()
        return runCatching { json.decodeFromString<List<PendingWorkoutSessionPayload>>(raw) }.getOrDefault(emptyList())
    }

    suspend fun clearPendingSessions(uid: String) {
        context.workoutDataStore.edit { prefs ->
            prefs.remove(pendingQueueKey(uid))
        }
    }

    fun scheduleRestAlarm(
        triggerAtEpochMs: Long,
        exerciseName: String,
        soundEnabled: Boolean,
        vibrationEnabled: Boolean
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, RestTimerAlarmReceiver::class.java).apply {
            putExtra(RestTimerAlarmReceiver.EXTRA_EXERCISE_NAME, exerciseName)
            putExtra(RestTimerAlarmReceiver.EXTRA_SOUND_ENABLED, soundEnabled)
            putExtra(RestTimerAlarmReceiver.EXTRA_VIBRATION_ENABLED, vibrationEnabled)
        }
        val pi = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtEpochMs, pi)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtEpochMs, pi)
            }
        }
    }

    fun cancelRestAlarm() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, RestTimerAlarmReceiver::class.java)
        val pi = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        runCatching { alarmManager.cancel(pi) }
        RestTimerAlarmReceiver.cancelNotification(context)
    }

    fun fireImmediateAlert(exerciseName: String, soundEnabled: Boolean, vibrationEnabled: Boolean) {
        val intent = Intent(context, RestTimerAlarmReceiver::class.java).apply {
            putExtra(RestTimerAlarmReceiver.EXTRA_EXERCISE_NAME, exerciseName)
            putExtra(RestTimerAlarmReceiver.EXTRA_SOUND_ENABLED, soundEnabled)
            putExtra(RestTimerAlarmReceiver.EXTRA_VIBRATION_ENABLED, vibrationEnabled)
        }
        RestTimerAlarmReceiver().onReceive(context, intent)
    }

    companion object {
        private const val ALARM_REQUEST_CODE = 8204
    }
}

fun WorkoutExerciseEntry.toPersisted() = PersistedExerciseEntry(
    exerciseId = exerciseId,
    exerciseName = exerciseName,
    muscleGroup = muscleGroup,
    secondaryMuscles = secondaryMuscles,
    equipment = equipment,
    targetSets = targetSets,
    targetReps = targetReps,
    repRangeMin = repRangeMin,
    repRangeMax = repRangeMax,
    targetWeightKg = targetWeightKg,
    restSeconds = restSeconds,
    setType = setType,
    supersetGroup = supersetGroup,
    notes = notes
)

fun PersistedExerciseEntry.toModel() = WorkoutExerciseEntry(
    exerciseId = exerciseId,
    exerciseName = exerciseName,
    muscleGroup = muscleGroup,
    secondaryMuscles = secondaryMuscles,
    equipment = equipment,
    targetSets = targetSets,
    targetReps = targetReps,
    repRangeMin = repRangeMin,
    repRangeMax = repRangeMax,
    targetWeightKg = targetWeightKg,
    restSeconds = restSeconds,
    setType = setType,
    supersetGroup = supersetGroup,
    notes = notes
)

fun ExerciseSessionRecord.toPersisted() = PersistedExerciseSessionRecord(
    exerciseId = exerciseId,
    exerciseName = exerciseName,
    primaryMuscle = primaryMuscle,
    secondaryMuscles = secondaryMuscles,
    equipment = equipment,
    skipped = skipped,
    replacedFromExerciseName = replacedFromExerciseName,
    notes = notes,
    sets = sets.map {
        PersistedSetRecord(it.setNumber, it.setType, it.reps, it.weightKg, it.completedAtMs)
    }
)

fun PersistedExerciseSessionRecord.toModel() = ExerciseSessionRecord(
    exerciseId = exerciseId,
    exerciseName = exerciseName,
    primaryMuscle = primaryMuscle,
    secondaryMuscles = secondaryMuscles,
    equipment = equipment,
    skipped = skipped,
    replacedFromExerciseName = replacedFromExerciseName,
    notes = notes,
    sets = sets.map {
        CompletedSetRecord(it.setNumber, it.setType, it.reps, it.weightKg, it.completedAtMs)
    }
)

fun WorkoutSessionLog.toPendingPayload() = PendingWorkoutSessionPayload(
    userId = userId,
    workoutId = workoutId,
    workoutName = workoutName,
    programId = programId,
    dateKey = dateKey,
    durationSeconds = durationSeconds,
    totalVolumeKg = totalVolumeKg,
    warmupVolumeKg = warmupVolumeKg,
    workingVolumeKg = workingVolumeKg,
    caloriesBurned = caloriesBurned,
    setsCompleted = setsCompleted,
    personalRecordsHit = personalRecordsHit,
    notes = notes,
    completedSetsSummary = completedSetsSummary,
    exerciseRecords = exerciseRecords.map { it.toPersisted() }
)

fun PendingWorkoutSessionPayload.toModel() = WorkoutSessionLog(
    userId = userId,
    workoutId = workoutId,
    workoutName = workoutName,
    programId = programId,
    dateKey = dateKey,
    durationSeconds = durationSeconds,
    totalVolumeKg = totalVolumeKg,
    warmupVolumeKg = warmupVolumeKg,
    workingVolumeKg = workingVolumeKg,
    caloriesBurned = caloriesBurned,
    setsCompleted = setsCompleted,
    personalRecordsHit = personalRecordsHit,
    notes = notes,
    completedSetsSummary = completedSetsSummary,
    exerciseRecords = exerciseRecords.map { it.toModel() }
)

fun PersistedActiveWorkout.toWorkoutPlan() = WorkoutPlan(
    id = planId,
    userId = userId,
    name = planName,
    level = planLevel,
    targetGoal = planGoal,
    programId = programId,
    exercises = exercises.map { it.toModel() }
)
