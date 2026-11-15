package com.example.ui

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.DefaultDispatcherProvider
import com.example.core.DispatcherProvider
import com.example.data.ai.AiMealPlanResult
import com.example.data.ai.FitTrackAiService
import com.example.data.ai.FoodScanResult
import com.example.data.catalog.FitnessCatalog
import com.example.data.firebase.FitTrackRepository
import com.example.data.model.AiChatMessage
import com.example.data.model.BodyMeasurement
import com.example.data.model.CompletedSetRecord
import com.example.data.model.ExerciseDefinition
import com.example.data.model.ExerciseSessionRecord
import com.example.data.model.FoodItem
import com.example.data.model.GoalHistoryEntry
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
import com.example.data.model.WorkoutPlan
import com.example.data.model.WorkoutProgram
import com.example.data.model.WorkoutScheduleItem
import com.example.data.model.WorkoutSessionLog
import com.example.domain.FitnessCalculator
import com.example.domain.UnitFormatter
import com.example.domain.WorkoutAnalyticsEngine
import com.example.platform.ConnectivityMonitor
import com.example.platform.GpsTrackerStore
import com.example.platform.LocationTrackingService
import com.example.platform.NotificationPreferences
import com.example.platform.NutritionPreferences
import com.example.platform.PersistedActiveWorkout
import com.example.platform.PlannedMealEntry
import com.example.platform.RecentFoodEntry
import com.example.platform.RestTimerAndSessionStore
import com.example.platform.RestTimerSettings
import com.example.platform.SavedRecipe
import com.example.platform.StepCounterManager
import com.example.platform.StepSensorState
import com.example.platform.TrainingPreferences
import com.example.platform.UserPreferencesStore
import com.example.platform.toModel
import com.example.platform.toPersisted
import com.example.platform.toWorkoutPlan
import com.example.platform.todayDateKey
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

sealed interface AsyncState<out T> {
    data object Idle : AsyncState<Nothing>
    data object Loading : AsyncState<Nothing>
    data class Streaming(val text: String) : AsyncState<Nothing>
    data class Success<T>(val data: T) : AsyncState<T>
    data class Error(val message: String) : AsyncState<Nothing>
}

data class ActiveWorkoutState(
    val plan: WorkoutPlan,
    val records: List<ExerciseSessionRecord> = emptyList(),
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
    val startedAtMs: Long = System.currentTimeMillis(),
    val restSettings: RestTimerSettings = RestTimerSettings(),
    val savedOfflineAtMs: Long = 0L
) {
    val volumeBreakdown get() = WorkoutAnalyticsEngine.computeSessionVolumes(records)
    val totalVolumeKg: Double get() = volumeBreakdown.totalVolumeKg
    val workingVolumeKg: Double get() = volumeBreakdown.workingVolumeKg
    val warmupVolumeKg: Double get() = volumeBreakdown.warmupVolumeKg
    val completedSetsCount: Int get() = records.sumOf { ex -> if (ex.skipped) 0 else ex.sets.size }
}

class FitTrackViewModel(
    private val repository: FitTrackRepository,
    private val aiService: FitTrackAiService,
    private val stepManager: StepCounterManager,
    private val restSessionStore: RestTimerAndSessionStore,
    private val userPreferencesStore: UserPreferencesStore,
    private val connectivityMonitor: ConnectivityMonitor? = null,
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider,
    private val currentUserId: String,
    private val userEmail: String,
    private val userDisplayName: String,
    private val userPhotoUrl: String
) : ViewModel() {

    private fun <T> Flow<T>.toSafeStateFlow(initial: T, tag: String): StateFlow<T> =
        this.catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val root = firestoreError ?: error
            Log.w(TAG, "Stream error ($tag): ${root.message}", root)
            _bannerError.value = root.localizedMessage ?: "Failed to sync $tag"
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = initial
        )

    private val _bannerError = MutableStateFlow<String?>(null)
    val bannerError: StateFlow<String?> = _bannerError.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _profileLoaded = MutableStateFlow(false)
    val profileLoaded: StateFlow<Boolean> = _profileLoaded.asStateFlow()

    val userProfile: StateFlow<UserProfile?> = repository.observeUserProfile(currentUserId)
        .map { p ->
            _profileLoaded.value = true
            p
        }
        .toSafeStateFlow(null, "profile")

    val stepRecords: StateFlow<List<StepRecord>> = repository.observeStepRecords(currentUserId)
        .toSafeStateFlow(emptyList(), "steps")

    val gpsActivities: StateFlow<List<GpsActivity>> = repository.observeGpsActivities(currentUserId)
        .toSafeStateFlow(emptyList(), "gps_activities")

    val customFoods: StateFlow<List<FoodItem>> = repository.observeCustomFoods(currentUserId)
        .toSafeStateFlow(emptyList(), "custom_foods")

    val meals: StateFlow<List<MealLog>> = repository.observeMeals(currentUserId)
        .toSafeStateFlow(emptyList(), "meals")

    val waterLogs: StateFlow<List<WaterLog>> = repository.observeWaterLogs(currentUserId)
        .toSafeStateFlow(emptyList(), "water_logs")

    val workoutPlans: StateFlow<List<WorkoutPlan>> = repository.observeWorkouts(currentUserId)
        .toSafeStateFlow(emptyList(), "workouts")

    val workoutSessions: StateFlow<List<WorkoutSessionLog>> = repository.observeWorkoutSessions(currentUserId)
        .toSafeStateFlow(emptyList(), "workout_sessions")

    val workoutPrograms: StateFlow<List<WorkoutProgram>> = repository.observeWorkoutPrograms(currentUserId)
        .toSafeStateFlow(emptyList(), "workout_programs")

    val workoutSchedules: StateFlow<List<WorkoutScheduleItem>> = repository.observeWorkoutSchedules(currentUserId)
        .toSafeStateFlow(emptyList(), "workout_schedules")

    val personalRecords: StateFlow<List<PersonalRecord>> = repository.observePersonalRecords(currentUserId)
        .toSafeStateFlow(emptyList(), "personal_records")

    val trainingNotes: StateFlow<List<TrainingNote>> = repository.observeTrainingNotes(currentUserId)
        .toSafeStateFlow(emptyList(), "training_notes")

    val weightRecords: StateFlow<List<WeightRecord>> = repository.observeWeightRecords(currentUserId)
        .toSafeStateFlow(emptyList(), "weight_records")

    val bodyMeasurements: StateFlow<List<BodyMeasurement>> = repository.observeBodyMeasurements(currentUserId)
        .toSafeStateFlow(emptyList(), "body_measurements")

    val challenges: StateFlow<List<UserChallenge>> = repository.observeChallenges(currentUserId)
        .toSafeStateFlow(emptyList(), "challenges")

    val aiMessages: StateFlow<List<AiChatMessage>> = repository.observeAiMessages(currentUserId)
        .toSafeStateFlow(emptyList(), "ai_messages")

    val progressPhotos: StateFlow<List<ProgressPhotoNote>> = repository.observeProgressPhotos(currentUserId)
        .toSafeStateFlow(emptyList(), "progress_photos")

    val isOnline: StateFlow<Boolean> = connectivityMonitor?.isOnline
        ?: MutableStateFlow(true).asStateFlow()

    val stepSensorState: StateFlow<StepSensorState> = stepManager.state
    val gpsTrackerState = GpsTrackerStore.state

    val goalHistory: StateFlow<List<GoalHistoryEntry>> = repository.observeGoalHistory(currentUserId)
        .toSafeStateFlow(emptyList(), "goal_history")

    val trainingPreferences: StateFlow<TrainingPreferences> = combine(
        userPreferencesStore.observeTrainingPreferences(currentUserId),
        repository.observeSyncPreference(currentUserId, "training").catch { emit(null) }
    ) { local, remoteJson ->
        remoteJson?.let { userPreferencesStore.decodeTraining(it) } ?: local
    }.toSafeStateFlow(TrainingPreferences(), "training_preferences")

    val nutritionPreferences: StateFlow<NutritionPreferences> = combine(
        userPreferencesStore.observeNutritionPreferences(currentUserId),
        repository.observeSyncPreference(currentUserId, "nutrition").catch { emit(null) }
    ) { local, remoteJson ->
        remoteJson?.let { userPreferencesStore.decodeNutrition(it) } ?: local
    }.toSafeStateFlow(NutritionPreferences(), "nutrition_preferences")

    val notificationPreferences: StateFlow<NotificationPreferences> = combine(
        userPreferencesStore.observeNotificationPreferences(currentUserId),
        repository.observeSyncPreference(currentUserId, "notifications").catch { emit(null) }
    ) { local, remoteJson ->
        remoteJson?.let { userPreferencesStore.decodeNotifications(it) } ?: local
    }.toSafeStateFlow(NotificationPreferences(), "notification_preferences")

    val savedRecipes: StateFlow<List<SavedRecipe>> = combine(
        userPreferencesStore.observeRecipes(currentUserId),
        repository.observeRecipes(currentUserId)
            .onEach { remote ->
                if (remote.isNotEmpty()) {
                    userPreferencesStore.replaceRecipes(currentUserId, remote)
                }
            }
            .catch { emit(emptyList()) }
    ) { local, remote ->
        if (remote.isNotEmpty()) remote else local
    }.toSafeStateFlow(emptyList(), "saved_recipes")

    val plannedMeals: StateFlow<List<PlannedMealEntry>> = combine(
        userPreferencesStore.observePlannedMeals(currentUserId),
        repository.observePlannedMeals(currentUserId)
            .onEach { remote ->
                if (remote.isNotEmpty()) {
                    userPreferencesStore.replacePlannedMeals(currentUserId, remote)
                }
            }
            .catch { emit(emptyList()) }
    ) { local, remote ->
        if (remote.isNotEmpty()) remote else local
    }.toSafeStateFlow(emptyList(), "planned_meals")

    val recentFoods: StateFlow<List<RecentFoodEntry>> = userPreferencesStore
        .observeRecentFoods(currentUserId)
        .toSafeStateFlow(emptyList(), "recent_foods")

    // AI Feature States
    private val _coachState = MutableStateFlow<AsyncState<String>>(AsyncState.Idle)
    val coachState: StateFlow<AsyncState<String>> = _coachState.asStateFlow()

    private val _foodScanState = MutableStateFlow<AsyncState<FoodScanResult>>(AsyncState.Idle)
    val foodScanState: StateFlow<AsyncState<FoodScanResult>> = _foodScanState.asStateFlow()

    private val _aiWorkoutState = MutableStateFlow<AsyncState<Pair<WorkoutPlan, String>>>(AsyncState.Idle)
    val aiWorkoutState: StateFlow<AsyncState<Pair<WorkoutPlan, String>>> = _aiWorkoutState.asStateFlow()

    private val _aiMealPlanState = MutableStateFlow<AsyncState<AiMealPlanResult>>(AsyncState.Idle)
    val aiMealPlanState: StateFlow<AsyncState<AiMealPlanResult>> = _aiMealPlanState.asStateFlow()

    private val _aiSubstitutionsState = MutableStateFlow<AsyncState<String>>(AsyncState.Idle)
    val aiSubstitutionsState: StateFlow<AsyncState<String>> = _aiSubstitutionsState.asStateFlow()

    private val _aiWorkoutAnalysisState = MutableStateFlow<AsyncState<String>>(AsyncState.Idle)
    val aiWorkoutAnalysisState: StateFlow<AsyncState<String>> = _aiWorkoutAnalysisState.asStateFlow()

    private val _restSettings = MutableStateFlow(RestTimerSettings())
    val restSettings: StateFlow<RestTimerSettings> = _restSettings.asStateFlow()

    private val _activeWorkout = MutableStateFlow<ActiveWorkoutState?>(null)
    val activeWorkout: StateFlow<ActiveWorkoutState?> = _activeWorkout.asStateFlow()

    private var workoutTimerJob: Job? = null
    private var lastLoggedSetSignature: String = ""
    private var lastLoggedSetEpochMs: Long = 0L

    val todaySteps: StateFlow<Long> = combine(stepSensorState, stepRecords) { sensor, records ->
        val today = todayDateKey()
        val remoteSteps = records.firstOrNull { it.dateKey == today }?.steps ?: 0L
        maxOf(sensor.totalSteps, remoteSteps)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), 0L)

    init {
        viewModelScope.launch {
            try {
                repository.getOrCreateProfile(
                    UserProfile(
                        userId = currentUserId,
                        displayName = userDisplayName.ifBlank { "Athlete" },
                        email = userEmail,
                        photoUrl = userPhotoUrl,
                        onboardingCompleted = false
                    )
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Profile bootstrap failed", e)
                _bannerError.value = e.localizedMessage ?: "Failed to initialize profile"
            } finally {
                _profileLoaded.value = true
            }
        }

        viewModelScope.launch {
            runCatching {
                val loadedSettings = restSessionStore.loadRestSettings(currentUserId)
                _restSettings.value = loadedSettings

                val recovered = restSessionStore.loadActiveWorkout(currentUserId)
                if (recovered != null && recovered.exercises.isNotEmpty()) {
                    val now = System.currentTimeMillis()
                    val remainingRest = if (recovered.isResting && !recovered.restPaused && recovered.restEndEpochMs > now) {
                        ((recovered.restEndEpochMs - now) / 1000L).toInt().coerceAtLeast(1)
                    } else if (recovered.isResting && recovered.restPaused) {
                        recovered.restSecondsRemaining
                    } else {
                        0
                    }
                    val restored = ActiveWorkoutState(
                        plan = recovered.toWorkoutPlan(),
                        records = recovered.records.map { it.toModel() },
                        currentExerciseIndex = recovered.currentExerciseIndex.coerceIn(0, recovered.exercises.lastIndex),
                        elapsedSeconds = recovered.elapsedSeconds,
                        isPaused = recovered.isPaused,
                        isResting = remainingRest > 0,
                        restTotalSeconds = recovered.restTotalSeconds.coerceAtLeast(1),
                        restSecondsRemaining = remainingRest,
                        restPaused = recovered.restPaused && remainingRest > 0,
                        restEndEpochMs = if (remainingRest > 0 && !recovered.restPaused) recovered.restEndEpochMs else 0L,
                        restExerciseName = recovered.restExerciseName,
                        sessionNotes = recovered.sessionNotes,
                        startedAtMs = if (recovered.startedAtMs > 0L) recovered.startedAtMs else now,
                        restSettings = loadedSettings,
                        savedOfflineAtMs = now
                    )
                    _activeWorkout.value = restored
                    startWorkoutTicker()
                    _statusMessage.value = "Recovered active workout: ${restored.plan.name}"
                }

                flushPendingOfflineSessions()
            }.onFailure {
                if (it is CancellationException) throw it
                Log.w(TAG, "Offline recovery check warning: ${it.message}")
            }
        }

        stepManager.startListening()
    }

    private suspend fun flushPendingOfflineSessions() {
        val pending = restSessionStore.loadPendingSessions(currentUserId)
        if (pending.isEmpty()) return
        var syncedCount = 0
        for (payload in pending) {
            val session = payload.toModel().copy(userId = currentUserId)
            val savedId = runCatching { repository.saveWorkoutSession(session) }.getOrNull() ?: return
            val broken = WorkoutAnalyticsEngine.detectNewPersonalRecords(
                sessionId = savedId,
                dateKey = session.dateKey.ifBlank { todayDateKey() },
                records = session.exerciseRecords,
                existingPrs = personalRecords.value
            )
            broken.forEach { pr -> runCatching { repository.upsertPersonalRecord(pr) } }
            syncedCount++
        }
        if (syncedCount == pending.size) {
            restSessionStore.clearPendingSessions(currentUserId)
            _statusMessage.value = "Synced $syncedCount offline workout(s) to cloud"
        }
    }

    fun clearBannerError() {
        _bannerError.value = null
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun completeOnboarding(
        displayName: String,
        ageYears: Long,
        sex: String,
        heightCm: Double,
        weightKg: Double,
        targetWeightKg: Double,
        activityLevel: String,
        goal: String,
        sensorConsent: Boolean,
        locationConsent: Boolean,
        dobIso: String = ""
    ) {
        viewModelScope.launch {
            try {
                val effectiveAge = FitnessCalculator.resolveAgeYears(ageYears, dobIso).coerceIn(13, 120)
                val safeGoal = if (FitnessCalculator.isMinor(effectiveAge) && goal == "weight_loss") {
                    "weight_maintenance"
                } else {
                    goal
                }
                val (calTarget, _) = FitnessCalculator.calculateDailyCalorieTarget(
                    weightKg = weightKg,
                    heightCm = heightCm,
                    ageYears = effectiveAge,
                    sex = sex,
                    activityLevel = activityLevel,
                    goal = safeGoal
                )
                val waterTarget = FitnessCalculator.calculateWaterTargetMl(weightKg).toLong()
                val existing = userProfile.value ?: UserProfile(userId = currentUserId)
                val updated = existing.copy(
                    displayName = displayName.ifBlank { existing.displayName },
                    email = userEmail,
                    photoUrl = userPhotoUrl,
                    ageYears = effectiveAge.toLong(),
                    dateOfBirthIso = dobIso.trim(),
                    sex = sex,
                    heightCm = heightCm,
                    weightKg = weightKg,
                    targetWeightKg = targetWeightKg,
                    activityLevel = activityLevel,
                    goal = safeGoal,
                    dailyCalorieTarget = calTarget.toLong(),
                    dailyWaterTargetMl = waterTarget,
                    onboardingCompleted = true,
                    sensorConsentGranted = sensorConsent,
                    locationConsentGranted = locationConsent
                )
                repository.updateProfile(updated)
                repository.addWeightRecord(todayDateKey(), weightKg, "Initial weight")
                repository.saveGoalHistoryEntry(
                    GoalHistoryEntry(
                        userId = currentUserId,
                        goalType = safeGoal,
                        title = "Primary Goal: ${safeGoal.replace("_", " ").replaceFirstChar { it.uppercase() }}",
                        status = "active",
                        targetValue = targetWeightKg,
                        currentValue = weightKg,
                        unit = "kg",
                        targetDateKey = "",
                        completedDateKey = "",
                        notes = "Initial onboarding target"
                    )
                )
                FitnessCatalog.defaultChallenges(currentUserId).forEach { ch ->
                    repository.upsertChallenge(ch)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _bannerError.value = e.localizedMessage ?: "Failed to save onboarding"
            }
        }
    }

    fun updateThemePreference(theme: String) {
        val current = userProfile.value ?: return
        viewModelScope.launch {
            runCatching { repository.updateProfile(current.copy(themePreference = theme)) }
                .onFailure { _bannerError.value = it.localizedMessage }
        }
    }

    fun updateUnitSystem(units: String) {
        val current = userProfile.value ?: return
        viewModelScope.launch {
            runCatching { repository.updateProfile(current.copy(unitSystem = units)) }
                .onFailure { _bannerError.value = it.localizedMessage }
        }
    }

    fun updateReducedMotion(enabled: Boolean) {
        val current = userProfile.value ?: return
        viewModelScope.launch {
            runCatching { repository.updateProfile(current.copy(reducedMotionEnabled = enabled)) }
                .onFailure { _bannerError.value = it.localizedMessage }
        }
    }

    fun updateProfileGoals(
        displayName: String,
        stepGoal: Long,
        waterGoalMl: Long,
        calorieGoal: Long,
        goal: String,
        sensorConsent: Boolean,
        locationConsent: Boolean
    ) {
        val current = userProfile.value ?: return
        viewModelScope.launch {
            try {
                val finalCalories = if (FitnessCalculator.isMinor(current.ageYears.toInt())) {
                    val (safeTarget, _) = FitnessCalculator.calculateDailyCalorieTarget(
                        current.weightKg, current.heightCm, current.ageYears.toInt(), current.sex, current.activityLevel, goal
                    )
                    safeTarget.toLong()
                } else {
                    calorieGoal.coerceIn(1200L, 10000L)
                }
                repository.updateProfile(
                    current.copy(
                        displayName = displayName.ifBlank { current.displayName },
                        dailyStepGoal = stepGoal.coerceIn(1000L, 100000L),
                        dailyWaterTargetMl = waterGoalMl.coerceIn(500L, 10000L),
                        dailyCalorieTarget = finalCalories,
                        goal = goal,
                        sensorConsentGranted = sensorConsent,
                        locationConsentGranted = locationConsent
                    )
                )
                _statusMessage.value = "Profile & targets updated"
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _bannerError.value = e.localizedMessage ?: "Failed to update profile"
            }
        }
    }

    fun updatePersonalInfoAndGoals(
        displayName: String,
        ageYears: Long,
        sex: String,
        heightCm: Double,
        weightKg: Double,
        targetWeightKg: Double,
        activityLevel: String,
        stepGoal: Long,
        waterGoalMl: Long,
        calorieGoal: Long,
        goal: String,
        dobIso: String? = null
    ) {
        val current = userProfile.value ?: return
        viewModelScope.launch {
            try {
                val effectiveDob = (dobIso ?: current.dateOfBirthIso).trim()
                val effectiveAge = FitnessCalculator.resolveAgeYears(ageYears, effectiveDob).coerceIn(13, 120)
                val safeAge = effectiveAge.toLong()
                val safeHeight = heightCm.coerceIn(80.0, 260.0)
                val safeWeight = weightKg.coerceIn(25.0, 400.0)
                val safeTargetWeight = targetWeightKg.coerceIn(25.0, 400.0)
                val safeGoal = if (FitnessCalculator.isMinor(effectiveAge) && goal == "weight_loss") {
                    "weight_maintenance"
                } else {
                    goal
                }
                val finalCalories = if (FitnessCalculator.isMinor(effectiveAge)) {
                    val (safeTarget, _) = FitnessCalculator.calculateDailyCalorieTarget(
                        safeWeight, safeHeight, effectiveAge, sex, activityLevel, safeGoal
                    )
                    safeTarget.toLong()
                } else {
                    calorieGoal.coerceIn(1200L, 10000L)
                }
                repository.updateProfile(
                    current.copy(
                        displayName = displayName.ifBlank { current.displayName },
                        ageYears = safeAge,
                        dateOfBirthIso = effectiveDob,
                        sex = sex,
                        heightCm = safeHeight,
                        weightKg = safeWeight,
                        targetWeightKg = safeTargetWeight,
                        activityLevel = activityLevel,
                        dailyStepGoal = stepGoal.coerceIn(1000L, 100000L),
                        dailyWaterTargetMl = waterGoalMl.coerceIn(500L, 10000L),
                        dailyCalorieTarget = finalCalories,
                        goal = safeGoal
                    )
                )
                _statusMessage.value = "Personal info & goals saved"
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _bannerError.value = e.localizedMessage ?: "Failed to update profile"
            }
        }
    }

    fun saveGoalHistoryEntry(entry: GoalHistoryEntry) {
        val currentAge = (userProfile.value?.ageYears ?: 28L).toInt()
        val safeType = if (FitnessCalculator.isMinor(currentAge) && entry.goalType == "weight_loss") {
            "weight_maintenance"
        } else {
            entry.goalType
        }
        viewModelScope.launch {
            runCatching {
                repository.saveGoalHistoryEntry(entry.copy(userId = currentUserId, goalType = safeType))
            }.onSuccess { _statusMessage.value = "Goal saved" }
                .onFailure { _bannerError.value = it.localizedMessage }
        }
    }

    fun updateGoalStatus(entry: GoalHistoryEntry, newStatus: String) {
        val completedDate = if (newStatus == "completed" && entry.completedDateKey.isBlank()) {
            todayDateKey()
        } else {
            entry.completedDateKey
        }
        saveGoalHistoryEntry(entry.copy(status = newStatus, completedDateKey = completedDate))
    }

    fun deleteGoalHistoryEntry(goalId: String) {
        viewModelScope.launch {
            runCatching { repository.deleteGoalHistoryEntry(goalId) }
                .onSuccess { _statusMessage.value = "Goal removed" }
                .onFailure { _bannerError.value = it.localizedMessage }
        }
    }

    fun updateTrainingPreferences(prefs: TrainingPreferences) {
        viewModelScope.launch {
            runCatching {
                userPreferencesStore.saveTrainingPreferences(currentUserId, prefs)
                runCatching { repository.saveSyncPreference("training", userPreferencesStore.encodeTraining(prefs)) }
            }.onSuccess { _statusMessage.value = "Training preferences updated" }
                .onFailure { _bannerError.value = it.localizedMessage }
        }
    }

    fun updateNutritionPreferences(prefs: NutritionPreferences) {
        viewModelScope.launch {
            runCatching {
                userPreferencesStore.saveNutritionPreferences(currentUserId, prefs)
                runCatching { repository.saveSyncPreference("nutrition", userPreferencesStore.encodeNutrition(prefs)) }
            }.onSuccess { _statusMessage.value = "Nutrition preferences updated" }
                .onFailure { _bannerError.value = it.localizedMessage }
        }
    }

    fun updateNotificationPreferences(prefs: NotificationPreferences) {
        viewModelScope.launch {
            runCatching {
                userPreferencesStore.saveNotificationPreferences(currentUserId, prefs)
                runCatching { repository.saveSyncPreference("notifications", userPreferencesStore.encodeNotifications(prefs)) }
            }.onSuccess { _statusMessage.value = "Reminder preferences updated" }
                .onFailure { _bannerError.value = it.localizedMessage }
        }
    }

    fun addManualSteps(delta: Long) {
        stepManager.addManualSteps(delta)
        syncTodaySteps()
    }

    fun syncTodaySteps() {
        val sensor = stepManager.state.value
        val profile = userProfile.value
        val weight = profile?.weightKg ?: 70.0
        val height = profile?.heightCm ?: 172.0
        val total = sensor.totalSteps
        val distKm = FitnessCalculator.calculateWalkingDistanceKm(total, height)
        val cals = FitnessCalculator.calculateStepCalories(total, weight)
        viewModelScope.launch {
            runCatching {
                repository.upsertStepRecord(
                    StepRecord(
                        userId = currentUserId,
                        dateKey = sensor.dateKey,
                        steps = total,
                        manualSteps = sensor.manualSteps,
                        sensorBaseline = sensor.sensorBaseline,
                        distanceKm = distKm,
                        caloriesBurned = cals
                    )
                )
            }.onFailure {
                if (it is CancellationException) throw it
                _bannerError.value = it.localizedMessage
            }
        }
    }

    fun startGpsTracking(context: Context, activityType: String) {
        val weight = userProfile.value?.weightKg ?: 70.0
        LocationTrackingService.sendCommand(
            context = context,
            action = LocationTrackingService.ACTION_START,
            activityType = activityType,
            weightKg = weight
        )
    }

    fun pauseGpsTracking(context: Context) {
        LocationTrackingService.sendCommand(context, LocationTrackingService.ACTION_PAUSE)
    }

    fun resumeGpsTracking(context: Context) {
        LocationTrackingService.sendCommand(context, LocationTrackingService.ACTION_RESUME)
    }

    fun discardGpsTracking(context: Context) {
        LocationTrackingService.discardSession(context)
    }

    fun finishAndSaveGpsTracking(context: Context, notes: String = "") {
        val st = GpsTrackerStore.state.value
        LocationTrackingService.sendCommand(context, LocationTrackingService.ACTION_STOP)
        viewModelScope.launch {
            try {
                val now = System.currentTimeMillis()
                repository.saveGpsActivity(
                    GpsActivity(
                        userId = currentUserId,
                        activityType = st.activityType,
                        durationSeconds = st.elapsedSeconds,
                        distanceKm = st.distanceKm,
                        avgSpeedKmh = if (st.elapsedSeconds > 0) (st.distanceKm / (st.elapsedSeconds / 3600.0)) else 0.0,
                        avgPaceMinPerKm = st.avgPaceMinPerKm,
                        caloriesBurned = st.estimatedCalories,
                        routePoints = st.routePoints,
                        notes = notes,
                        startedAtMs = if (st.startedAtMs > 0L) st.startedAtMs else now,
                        endedAtMs = now
                    )
                )
                GpsTrackerStore.reset(context)
                _statusMessage.value = "GPS activity saved"
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _bannerError.value = e.localizedMessage ?: "Failed to save GPS session"
            }
        }
    }

    fun deleteGpsActivity(id: String) {
        viewModelScope.launch {
            runCatching { repository.deleteGpsActivity(id) }
                .onFailure { _bannerError.value = it.localizedMessage }
        }
    }

    fun logFoodToMeal(
        food: FoodItem,
        grams: Double,
        mealType: String,
        source: String = "manual",
        dateKey: String = todayDateKey()
    ) {
        val factor = grams / 100.0
        viewModelScope.launch {
            try {
                repository.addMeal(
                    MealLog(
                        userId = currentUserId,
                        dateKey = dateKey,
                        mealType = mealType,
                        foodName = food.name,
                        grams = grams,
                        measurementUnit = food.measurementUnit.takeIf { it == "g" || it == "ml" } ?: "g",
                        calories = food.caloriesPer100g * factor,
                        proteinG = food.proteinPer100g * factor,
                        carbsG = food.carbsPer100g * factor,
                        fatG = food.fatPer100g * factor,
                        fiberG = food.fiberPer100g * factor,
                        sugarG = food.sugarPer100g * factor,
                        source = source
                    )
                )
                userPreferencesStore.recordRecentFood(
                    currentUserId,
                    RecentFoodEntry(
                        foodName = food.name,
                        brand = food.brand,
                        caloriesPer100g = food.caloriesPer100g,
                        proteinPer100g = food.proteinPer100g,
                        carbsPer100g = food.carbsPer100g,
                        fatPer100g = food.fatPer100g,
                        fiberPer100g = food.fiberPer100g,
                        sugarPer100g = food.sugarPer100g,
                        lastGrams = grams,
                        measurementUnit = food.measurementUnit.takeIf { it == "g" || it == "ml" } ?: "g"
                    )
                )
                _statusMessage.value = "Added ${food.name} to $mealType"
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _bannerError.value = e.localizedMessage ?: "Failed to log meal"
            }
        }
    }

    fun logDirectMeal(meal: MealLog, dateKey: String = todayDateKey()) {
        viewModelScope.launch {
            runCatching { repository.addMeal(meal.copy(userId = currentUserId, dateKey = meal.dateKey.ifBlank { dateKey })) }
                .onSuccess { _statusMessage.value = "Logged ${meal.foodName}" }
                .onFailure { _bannerError.value = it.localizedMessage }
        }
    }

    fun saveRecipe(recipe: SavedRecipe) {
        viewModelScope.launch {
            runCatching {
                val remoteId = runCatching { repository.saveRecipe(recipe) }.getOrNull()
                val localRecipe = if (!remoteId.isNullOrBlank()) recipe.copy(id = remoteId) else recipe
                userPreferencesStore.saveRecipe(currentUserId, localRecipe)
            }.onSuccess { _statusMessage.value = "Saved recipe '${recipe.name}'" }
                .onFailure { _bannerError.value = it.localizedMessage }
        }
    }

    fun deleteRecipe(recipeId: String) {
        viewModelScope.launch {
            runCatching {
                userPreferencesStore.deleteRecipe(currentUserId, recipeId)
                runCatching { repository.deleteRecipe(recipeId) }
            }.onSuccess { _statusMessage.value = "Recipe removed" }
                .onFailure { _bannerError.value = it.localizedMessage }
        }
    }

    fun logRecipeToMeal(recipe: SavedRecipe, mealType: String, servingsCount: Double = 1.0, dateKey: String = todayDateKey()) {
        val factor = servingsCount.coerceAtLeast(0.25)
        logDirectMeal(
            MealLog(
                userId = currentUserId,
                dateKey = dateKey,
                mealType = mealType,
                foodName = "${recipe.name} (${String.format(java.util.Locale.US, "%.1f", factor)} srv)",
                grams = recipe.gramsPerServing * factor,
                measurementUnit = recipe.measurementUnit,
                calories = recipe.caloriesPerServing * factor,
                proteinG = recipe.proteinPerServingG * factor,
                carbsG = recipe.carbsPerServingG * factor,
                fatG = recipe.fatPerServingG * factor,
                fiberG = recipe.fiberPerServingG * factor,
                sugarG = recipe.sugarPerServingG * factor,
                source = "recipe"
            ),
            dateKey = dateKey
        )
    }

    fun savePlannedMeal(entry: PlannedMealEntry) {
        viewModelScope.launch {
            runCatching {
                val remoteId = runCatching { repository.savePlannedMeal(entry) }.getOrNull()
                val savedEntry = if (!remoteId.isNullOrBlank()) entry.copy(id = remoteId) else entry
                userPreferencesStore.savePlannedMeal(currentUserId, savedEntry)
            }.onSuccess { _statusMessage.value = "Added '${entry.title}' to ${entry.dayOfWeek} plan" }
                .onFailure { _bannerError.value = it.localizedMessage }
        }
    }

    fun deletePlannedMeal(entryId: String) {
        viewModelScope.launch {
            runCatching {
                userPreferencesStore.deletePlannedMeal(currentUserId, entryId)
                runCatching { repository.deletePlannedMeal(entryId) }
            }.onFailure { _bannerError.value = it.localizedMessage }
        }
    }

    fun updateMealGrams(meal: MealLog, newGrams: Double) {
        if (meal.grams <= 0.0 || newGrams <= 0.0) return
        val ratio = newGrams / meal.grams
        viewModelScope.launch {
            runCatching {
                repository.updateMeal(
                    meal.copy(
                        grams = newGrams,
                        calories = meal.calories * ratio,
                        proteinG = meal.proteinG * ratio,
                        carbsG = meal.carbsG * ratio,
                        fatG = meal.fatG * ratio,
                        fiberG = meal.fiberG * ratio,
                        sugarG = meal.sugarG * ratio
                    )
                )
            }.onFailure { _bannerError.value = it.localizedMessage }
        }
    }

    fun deleteMeal(mealId: String) {
        viewModelScope.launch {
            runCatching { repository.deleteMeal(mealId) }
                .onFailure { _bannerError.value = it.localizedMessage }
        }
    }

    fun saveCustomFood(food: FoodItem) {
        viewModelScope.launch {
            runCatching { repository.saveCustomFood(food.copy(userId = currentUserId, isCustom = true)) }
                .onSuccess { _statusMessage.value = "Saved ${food.name}" }
                .onFailure { _bannerError.value = it.localizedMessage }
        }
    }

    fun toggleFavoriteFood(food: FoodItem) {
        viewModelScope.launch {
            try {
                if (food.userId.isBlank()) {
                    repository.saveCustomFood(food.copy(id = "", userId = currentUserId, isFavorite = true, isCustom = false))
                } else {
                    repository.toggleFoodFavorite(food)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _bannerError.value = e.localizedMessage
            }
        }
    }

    fun addWater(amountMl: Long) {
        val unitSystem = userProfile.value?.unitSystem ?: "metric"
        viewModelScope.launch {
            runCatching { repository.addWaterLog(todayDateKey(), amountMl) }
                .onSuccess { _statusMessage.value = "+${UnitFormatter.formatWater(amountMl, unitSystem)} logged" }
                .onFailure { _bannerError.value = it.localizedMessage }
        }
    }

    fun deleteWaterLog(id: String) {
        viewModelScope.launch {
            runCatching { repository.deleteWaterLog(id) }
                .onFailure { _bannerError.value = it.localizedMessage }
        }
    }

    // ==================== WORKOUT PLANS, PROGRAMS, SCHEDULES & NOTES ====================

    fun saveWorkoutPlan(plan: WorkoutPlan) {
        viewModelScope.launch {
            runCatching { repository.saveWorkoutPlan(plan.copy(userId = currentUserId)) }
                .onSuccess { _statusMessage.value = "Saved workout '${plan.name}'" }
                .onFailure { _bannerError.value = it.localizedMessage }
        }
    }

    fun duplicateWorkoutPlan(plan: WorkoutPlan) {
        val copy = plan.copy(
            id = "",
            userId = currentUserId,
            name = "${plan.name} (Copy)"
        )
        saveWorkoutPlan(copy)
    }

    fun deleteWorkoutPlan(id: String) {
        viewModelScope.launch {
            runCatching { repository.deleteWorkoutPlan(id) }
                .onSuccess { _statusMessage.value = "Workout plan removed" }
                .onFailure { _bannerError.value = it.localizedMessage }
        }
    }

    fun saveWorkoutProgram(program: WorkoutProgram) {
        viewModelScope.launch {
            runCatching { repository.saveWorkoutProgram(program.copy(userId = currentUserId, isCustom = true)) }
                .onSuccess { _statusMessage.value = "Saved program '${program.name}'" }
                .onFailure { _bannerError.value = it.localizedMessage }
        }
    }

    fun deleteWorkoutProgram(programId: String) {
        viewModelScope.launch {
            runCatching { repository.deleteWorkoutProgram(programId) }
                .onSuccess { _statusMessage.value = "Program deleted" }
                .onFailure { _bannerError.value = it.localizedMessage }
        }
    }

    fun upsertWorkoutSchedule(item: WorkoutScheduleItem) {
        viewModelScope.launch {
            runCatching { repository.upsertWorkoutSchedule(item.copy(userId = currentUserId)) }
                .onSuccess { _statusMessage.value = "Updated schedule for ${item.dateKey}" }
                .onFailure { _bannerError.value = it.localizedMessage }
        }
    }

    fun deleteWorkoutSchedule(scheduleId: String) {
        viewModelScope.launch {
            runCatching { repository.deleteWorkoutSchedule(scheduleId) }
                .onSuccess { _statusMessage.value = "Schedule cleared" }
                .onFailure { _bannerError.value = it.localizedMessage }
        }
    }

    fun saveTrainingNote(note: TrainingNote) {
        viewModelScope.launch {
            runCatching { repository.saveTrainingNote(note.copy(userId = currentUserId, dateKey = note.dateKey.ifBlank { todayDateKey() })) }
                .onSuccess { _statusMessage.value = "Training note saved" }
                .onFailure { _bannerError.value = it.localizedMessage }
        }
    }

    fun deleteTrainingNote(noteId: String) {
        viewModelScope.launch {
            runCatching { repository.deleteTrainingNote(noteId) }
                .onFailure { _bannerError.value = it.localizedMessage }
        }
    }

    // ==================== ACTIVE WORKOUT ENGINE & REST TIMER ====================

    fun startActiveWorkout(plan: WorkoutPlan) {
        val now = System.currentTimeMillis()
        val initialRecords = plan.exercises.map { ex ->
            ExerciseSessionRecord(
                exerciseId = ex.exerciseId,
                exerciseName = ex.exerciseName,
                primaryMuscle = ex.muscleGroup,
                secondaryMuscles = ex.secondaryMuscles,
                equipment = ex.equipment,
                notes = ex.notes
            )
        }
        val state = ActiveWorkoutState(
            plan = plan,
            records = initialRecords,
            currentExerciseIndex = 0,
            elapsedSeconds = 0L,
            isPaused = false,
            startedAtMs = now,
            restSettings = _restSettings.value,
            savedOfflineAtMs = now
        )
        _activeWorkout.value = state
        persistActiveStateAsync(state)
        startWorkoutTicker()
    }

    private fun startWorkoutTicker() {
        workoutTimerJob?.cancel()
        workoutTimerJob = viewModelScope.launch {
            var ticksSincePersist = 0
            while (isActive) {
                delay(1000L)
                val current = _activeWorkout.value ?: break
                val now = System.currentTimeMillis()
                var alarmFired = false
                val nextRestRemaining = when {
                    !current.isResting -> 0
                    current.restPaused -> current.restSecondsRemaining
                    current.restEndEpochMs > now -> ((current.restEndEpochMs - now + 999L) / 1000L).toInt()
                    else -> {
                        alarmFired = true
                        0
                    }
                }
                if (alarmFired) {
                    restSessionStore.cancelRestAlarm()
                    restSessionStore.fireImmediateAlert(
                        exerciseName = current.restExerciseName,
                        soundEnabled = current.restSettings.soundEnabled,
                        vibrationEnabled = current.restSettings.vibrationEnabled
                    )
                }
                val updated = current.copy(
                    elapsedSeconds = if (current.isPaused) current.elapsedSeconds else current.elapsedSeconds + 1L,
                    restSecondsRemaining = nextRestRemaining,
                    isResting = nextRestRemaining > 0,
                    restEndEpochMs = if (nextRestRemaining > 0) current.restEndEpochMs else 0L
                )
                _activeWorkout.value = updated
                ticksSincePersist++
                if (alarmFired || ticksSincePersist >= 10) {
                    ticksSincePersist = 0
                    persistActiveStateAsync(updated)
                }
            }
        }
    }

    fun toggleWorkoutPause() {
        val current = _activeWorkout.value ?: return
        val updated = current.copy(isPaused = !current.isPaused, savedOfflineAtMs = System.currentTimeMillis())
        _activeWorkout.value = updated
        persistActiveStateAsync(updated)
    }

    fun selectExerciseIndex(index: Int) {
        val current = _activeWorkout.value ?: return
        if (current.records.isEmpty()) return
        val clamped = index.coerceIn(0, current.records.lastIndex)
        val updated = current.copy(currentExerciseIndex = clamped, savedOfflineAtMs = System.currentTimeMillis())
        _activeWorkout.value = updated
        persistActiveStateAsync(updated)
    }

    fun logActiveSet(
        exerciseIndex: Int,
        setType: String,
        reps: Int,
        weightKg: Double,
        overrideRestSeconds: Int? = null
    ) {
        val current = _activeWorkout.value ?: return
        if (exerciseIndex !in current.records.indices) return

        val now = System.currentTimeMillis()
        val updatedRecords = current.records.toMutableList()
        val existing = updatedRecords[exerciseIndex]
        val nextSetNumber = (existing.sets.size + 1).toLong()
        val signature = "${current.plan.id}|$exerciseIndex|$nextSetNumber|$setType|$reps|$weightKg"
        if (signature == lastLoggedSetSignature && (now - lastLoggedSetEpochMs) < 1500L) {
            return
        }
        lastLoggedSetSignature = signature
        lastLoggedSetEpochMs = now

        val newSet = CompletedSetRecord(
            setNumber = nextSetNumber,
            setType = setType,
            reps = reps.coerceAtLeast(1).toLong(),
            weightKg = weightKg.coerceAtLeast(0.0),
            completedAtMs = now
        )
        updatedRecords[exerciseIndex] = existing.copy(
            skipped = false,
            sets = existing.sets + newSet
        )

        val settings = current.restSettings
        val shouldStartRest = settings.autoStartRest
        val plannedRest = overrideRestSeconds ?: if (setType.equals("warmup", ignoreCase = true)) {
            settings.warmupRestSeconds
        } else {
            current.plan.exercises.getOrNull(exerciseIndex)?.restSeconds?.toInt() ?: settings.defaultRestSeconds
        }
        val activeRest = if (shouldStartRest && plannedRest > 0) plannedRest else 0
        val endEpoch = if (activeRest > 0) now + activeRest * 1000L else 0L

        if (activeRest > 0) {
            restSessionStore.scheduleRestAlarm(
                triggerAtEpochMs = endEpoch,
                exerciseName = existing.exerciseName,
                soundEnabled = settings.soundEnabled,
                vibrationEnabled = settings.vibrationEnabled
            )
        }

        val updated = current.copy(
            records = updatedRecords,
            isResting = activeRest > 0,
            restTotalSeconds = if (activeRest > 0) activeRest else current.restTotalSeconds,
            restSecondsRemaining = activeRest,
            restPaused = false,
            restEndEpochMs = endEpoch,
            restExerciseName = existing.exerciseName,
            savedOfflineAtMs = now
        )
        _activeWorkout.value = updated
        persistActiveStateAsync(updated)
    }

    fun updateLoggedSet(
        exerciseIndex: Int,
        setIndex: Int,
        newSetType: String,
        newReps: Int,
        newWeightKg: Double
    ) {
        val current = _activeWorkout.value ?: return
        if (exerciseIndex !in current.records.indices) return
        val ex = current.records[exerciseIndex]
        if (setIndex !in ex.sets.indices) return

        val newSets = ex.sets.toMutableList()
        newSets[setIndex] = newSets[setIndex].copy(
            setType = newSetType,
            reps = newReps.coerceAtLeast(1).toLong(),
            weightKg = newWeightKg.coerceAtLeast(0.0)
        )
        val newRecords = current.records.toMutableList()
        newRecords[exerciseIndex] = ex.copy(sets = newSets)
        val updated = current.copy(records = newRecords, savedOfflineAtMs = System.currentTimeMillis())
        _activeWorkout.value = updated
        persistActiveStateAsync(updated)
    }

    fun deleteLoggedSet(exerciseIndex: Int, setIndex: Int) {
        val current = _activeWorkout.value ?: return
        if (exerciseIndex !in current.records.indices) return
        val ex = current.records[exerciseIndex]
        if (setIndex !in ex.sets.indices) return

        val newSets = ex.sets.filterIndexed { i, _ -> i != setIndex }
            .mapIndexed { i, s -> s.copy(setNumber = (i + 1).toLong()) }
        val newRecords = current.records.toMutableList()
        newRecords[exerciseIndex] = ex.copy(sets = newSets)
        val updated = current.copy(records = newRecords, savedOfflineAtMs = System.currentTimeMillis())
        _activeWorkout.value = updated
        persistActiveStateAsync(updated)
    }

    fun skipExercise(exerciseIndex: Int) {
        val current = _activeWorkout.value ?: return
        if (exerciseIndex !in current.records.indices) return
        val newRecords = current.records.toMutableList()
        val ex = newRecords[exerciseIndex]
        newRecords[exerciseIndex] = ex.copy(skipped = !ex.skipped)
        val nextIndex = if (!ex.skipped && exerciseIndex < newRecords.lastIndex) {
            exerciseIndex + 1
        } else {
            exerciseIndex
        }
        val updated = current.copy(
            records = newRecords,
            currentExerciseIndex = nextIndex,
            savedOfflineAtMs = System.currentTimeMillis()
        )
        _activeWorkout.value = updated
        persistActiveStateAsync(updated)
    }

    fun replaceExerciseInActiveWorkout(exerciseIndex: Int, replacement: ExerciseDefinition) {
        val current = _activeWorkout.value ?: return
        if (exerciseIndex !in current.records.indices || exerciseIndex !in current.plan.exercises.indices) return

        val oldEntry = current.plan.exercises[exerciseIndex]
        val newEntry = oldEntry.copy(
            exerciseId = replacement.id,
            exerciseName = replacement.name,
            muscleGroup = replacement.muscleGroup,
            secondaryMuscles = replacement.secondaryMuscles,
            equipment = replacement.equipment,
            repRangeMin = replacement.defaultRepMin.toLong(),
            repRangeMax = replacement.defaultRepMax.toLong(),
            restSeconds = replacement.defaultRestSeconds.toLong()
        )
        val newEntries = current.plan.exercises.toMutableList().apply { set(exerciseIndex, newEntry) }
        val oldRec = current.records[exerciseIndex]
        val newRecords = current.records.toMutableList().apply {
            set(
                exerciseIndex,
                oldRec.copy(
                    exerciseId = replacement.id,
                    exerciseName = replacement.name,
                    primaryMuscle = replacement.muscleGroup,
                    secondaryMuscles = replacement.secondaryMuscles,
                    equipment = replacement.equipment,
                    skipped = false,
                    replacedFromExerciseName = oldRec.replacedFromExerciseName.ifBlank { oldRec.exerciseName }
                )
            )
        }
        val updated = current.copy(
            plan = current.plan.copy(exercises = newEntries),
            records = newRecords,
            savedOfflineAtMs = System.currentTimeMillis()
        )
        _activeWorkout.value = updated
        persistActiveStateAsync(updated)
        _statusMessage.value = "Swapped to ${replacement.name}"
    }

    fun updateExerciseNote(exerciseIndex: Int, note: String) {
        val current = _activeWorkout.value ?: return
        if (exerciseIndex !in current.records.indices) return
        val newRecords = current.records.toMutableList()
        newRecords[exerciseIndex] = newRecords[exerciseIndex].copy(notes = note.take(300))
        val updated = current.copy(records = newRecords, savedOfflineAtMs = System.currentTimeMillis())
        _activeWorkout.value = updated
        persistActiveStateAsync(updated)
    }

    fun updateSessionNotes(notes: String) {
        val current = _activeWorkout.value ?: return
        val updated = current.copy(sessionNotes = notes.take(400), savedOfflineAtMs = System.currentTimeMillis())
        _activeWorkout.value = updated
        persistActiveStateAsync(updated)
    }

    fun applyProgressionConfirmation(exerciseIndex: Int, confirmedWeightKg: Double, confirmedReps: Long) {
        val current = _activeWorkout.value ?: return
        if (exerciseIndex !in current.plan.exercises.indices) return
        val newEntries = current.plan.exercises.toMutableList()
        val entry = newEntries[exerciseIndex]
        newEntries[exerciseIndex] = entry.copy(
            targetWeightKg = confirmedWeightKg.coerceIn(0.0, 1000.0),
            targetReps = confirmedReps.coerceIn(1L, 100L)
        )
        val updated = current.copy(
            plan = current.plan.copy(exercises = newEntries),
            savedOfflineAtMs = System.currentTimeMillis()
        )
        _activeWorkout.value = updated
        persistActiveStateAsync(updated)
        _statusMessage.value = "Updated ${entry.exerciseName} target to ${WorkoutAnalyticsEngine.formatWeight(confirmedWeightKg)} × $confirmedReps"
    }

    fun startRestTimer(seconds: Int, exerciseName: String) {
        val current = _activeWorkout.value ?: return
        val clamped = seconds.coerceIn(5, 600)
        val endEpoch = System.currentTimeMillis() + clamped * 1000L
        restSessionStore.scheduleRestAlarm(
            triggerAtEpochMs = endEpoch,
            exerciseName = exerciseName,
            soundEnabled = current.restSettings.soundEnabled,
            vibrationEnabled = current.restSettings.vibrationEnabled
        )
        val updated = current.copy(
            isResting = true,
            restTotalSeconds = clamped,
            restSecondsRemaining = clamped,
            restPaused = false,
            restEndEpochMs = endEpoch,
            restExerciseName = exerciseName,
            savedOfflineAtMs = System.currentTimeMillis()
        )
        _activeWorkout.value = updated
        persistActiveStateAsync(updated)
    }

    fun adjustRestTimer(deltaSeconds: Int) {
        val current = _activeWorkout.value ?: return
        if (!current.isResting) return
        val newRemaining = (current.restSecondsRemaining + deltaSeconds).coerceIn(0, 600)
        if (newRemaining == 0) {
            skipRestTimer()
            return
        }
        val newTotal = maxOf(current.restTotalSeconds, newRemaining)
        val newEnd = if (current.restPaused) 0L else System.currentTimeMillis() + newRemaining * 1000L
        if (!current.restPaused) {
            restSessionStore.scheduleRestAlarm(
                triggerAtEpochMs = newEnd,
                exerciseName = current.restExerciseName,
                soundEnabled = current.restSettings.soundEnabled,
                vibrationEnabled = current.restSettings.vibrationEnabled
            )
        }
        val updated = current.copy(
            restTotalSeconds = newTotal,
            restSecondsRemaining = newRemaining,
            restEndEpochMs = newEnd,
            savedOfflineAtMs = System.currentTimeMillis()
        )
        _activeWorkout.value = updated
        persistActiveStateAsync(updated)
    }

    fun toggleRestPause() {
        val current = _activeWorkout.value ?: return
        if (!current.isResting) return
        val nextPaused = !current.restPaused
        val newEnd = if (nextPaused) {
            restSessionStore.cancelRestAlarm()
            0L
        } else {
            val end = System.currentTimeMillis() + current.restSecondsRemaining * 1000L
            restSessionStore.scheduleRestAlarm(
                triggerAtEpochMs = end,
                exerciseName = current.restExerciseName,
                soundEnabled = current.restSettings.soundEnabled,
                vibrationEnabled = current.restSettings.vibrationEnabled
            )
            end
        }
        val updated = current.copy(
            restPaused = nextPaused,
            restEndEpochMs = newEnd,
            savedOfflineAtMs = System.currentTimeMillis()
        )
        _activeWorkout.value = updated
        persistActiveStateAsync(updated)
    }

    fun skipRestTimer() {
        val current = _activeWorkout.value ?: return
        restSessionStore.cancelRestAlarm()
        val updated = current.copy(
            isResting = false,
            restSecondsRemaining = 0,
            restPaused = false,
            restEndEpochMs = 0L,
            savedOfflineAtMs = System.currentTimeMillis()
        )
        _activeWorkout.value = updated
        persistActiveStateAsync(updated)
    }

    fun updateRestSettings(settings: RestTimerSettings) {
        _restSettings.value = settings
        _activeWorkout.value = _activeWorkout.value?.copy(restSettings = settings)
        viewModelScope.launch {
            runCatching { restSessionStore.saveRestSettings(currentUserId, settings) }
        }
    }

    fun finishActiveWorkout() {
        val active = _activeWorkout.value ?: return
        workoutTimerJob?.cancel()
        _activeWorkout.value = null
        restSessionStore.cancelRestAlarm()

        val today = todayDateKey()
        val weightKg = userProfile.value?.weightKg ?: 70.0
        val durationMin = (active.elapsedSeconds / 60.0).coerceAtLeast(1.0)
        val cals = FitnessCalculator.calculateMetCalories(5.5, weightKg, durationMin)
        val breakdown = active.volumeBreakdown
        val totalSetsCount = (breakdown.workingSetsCount + breakdown.warmupSetsCount).toLong()
        val brokenPrs = WorkoutAnalyticsEngine.detectNewPersonalRecords(
            sessionId = "pending_${System.currentTimeMillis()}",
            dateKey = today,
            records = active.records,
            existingPrs = personalRecords.value
        )
        val summary = active.records.filter { !it.skipped && it.sets.isNotEmpty() }.map { rec ->
            val topWeight = rec.sets.maxOfOrNull { it.weightKg } ?: 0.0
            "${rec.exerciseName}: ${rec.sets.size} sets (top ${WorkoutAnalyticsEngine.formatWeight(topWeight)})"
        }

        val sessionPayload = WorkoutSessionLog(
            userId = currentUserId,
            workoutId = active.plan.id,
            workoutName = active.plan.name,
            programId = active.plan.programId,
            dateKey = today,
            notes = active.sessionNotes,
            durationSeconds = active.elapsedSeconds,
            totalVolumeKg = breakdown.totalVolumeKg,
            warmupVolumeKg = breakdown.warmupVolumeKg,
            workingVolumeKg = breakdown.workingVolumeKg,
            caloriesBurned = cals,
            setsCompleted = totalSetsCount,
            personalRecordsHit = brokenPrs.size.toLong(),
            completedSetsSummary = summary,
            exerciseRecords = active.records
        )

        viewModelScope.launch {
            restSessionStore.clearActiveWorkout(currentUserId)
            try {
                val savedSessionId = repository.saveWorkoutSession(sessionPayload)
                brokenPrs.forEach { pr ->
                    runCatching { repository.upsertPersonalRecord(pr.copy(sessionId = savedSessionId)) }
                }
                val todaySchedule = workoutSchedules.value.firstOrNull { it.dateKey == today }
                val scheduleItem = (todaySchedule ?: WorkoutScheduleItem(
                    id = today,
                    userId = currentUserId,
                    dateKey = today,
                    workoutId = active.plan.id,
                    workoutName = active.plan.name,
                    programId = active.plan.programId
                )).copy(
                    workoutId = active.plan.id,
                    workoutName = active.plan.name,
                    status = "completed",
                    completedSessionId = savedSessionId
                )
                runCatching { repository.upsertWorkoutSchedule(scheduleItem) }

                _statusMessage.value = if (brokenPrs.isNotEmpty()) {
                    "Workout saved! ${brokenPrs.size} new Personal Record(s)!"
                } else {
                    "Workout saved!"
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                restSessionStore.enqueuePendingSession(currentUserId, sessionPayload)
                _statusMessage.value = "Saved offline — will sync automatically when connected."
                Log.w(TAG, "Queued offline workout session: ${e.message}")
            }
        }
    }

    fun cancelActiveWorkout() {
        workoutTimerJob?.cancel()
        _activeWorkout.value = null
        viewModelScope.launch {
            restSessionStore.clearActiveWorkout(currentUserId)
        }
    }

    private fun persistActiveStateAsync(state: ActiveWorkoutState) {
        viewModelScope.launch {
            runCatching {
                val persisted = PersistedActiveWorkout(
                    userId = currentUserId,
                    planId = state.plan.id,
                    planName = state.plan.name,
                    planLevel = state.plan.level,
                    planGoal = state.plan.targetGoal,
                    programId = state.plan.programId,
                    exercises = state.plan.exercises.map { it.toPersisted() },
                    records = state.records.map { it.toPersisted() },
                    currentExerciseIndex = state.currentExerciseIndex,
                    elapsedSeconds = state.elapsedSeconds,
                    isPaused = state.isPaused,
                    isResting = state.isResting,
                    restTotalSeconds = state.restTotalSeconds,
                    restSecondsRemaining = state.restSecondsRemaining,
                    restPaused = state.restPaused,
                    restEndEpochMs = state.restEndEpochMs,
                    restExerciseName = state.restExerciseName,
                    sessionNotes = state.sessionNotes,
                    startedAtMs = state.startedAtMs
                )
                restSessionStore.saveActiveWorkout(currentUserId, persisted)
            }
        }
    }

    // ==================== BODY, CHALLENGES, AI & EXPORT ====================

    fun logWeight(weightKg: Double, note: String = "") {
        viewModelScope.launch {
            try {
                repository.addWeightRecord(todayDateKey(), weightKg, note)
                userProfile.value?.let { profile ->
                    repository.updateProfile(profile.copy(weightKg = weightKg))
                }
                _statusMessage.value = "Weight updated to ${String.format("%.1f", weightKg)} kg"
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _bannerError.value = e.localizedMessage
            }
        }
    }

    fun logMeasurements(waist: Double, chest: Double, arms: Double, legs: Double, hips: Double) {
        viewModelScope.launch {
            runCatching {
                repository.addBodyMeasurement(
                    BodyMeasurement(
                        userId = currentUserId,
                        dateKey = todayDateKey(),
                        waistCm = waist,
                        chestCm = chest,
                        armsCm = arms,
                        legsCm = legs,
                        hipsCm = hips
                    )
                )
            }.onSuccess { _statusMessage.value = "Measurements saved" }
                .onFailure { _bannerError.value = it.localizedMessage }
        }
    }

    fun updateChallengeProgress(challenge: UserChallenge, newValue: Double, joined: Boolean) {
        viewModelScope.launch {
            runCatching {
                repository.upsertChallenge(
                    challenge.copy(
                        userId = currentUserId,
                        currentValue = newValue,
                        joined = joined,
                        completed = newValue >= challenge.targetValue
                    )
                )
            }.onFailure { _bannerError.value = it.localizedMessage }
        }
    }

    fun addProgressPhoto(localUri: String, caption: String) {
        val weight = userProfile.value?.weightKg ?: 70.0
        viewModelScope.launch {
            runCatching {
                repository.addProgressPhoto(localUri, weight, caption, todayDateKey())
            }.onSuccess { _statusMessage.value = "Private progress photo added" }
                .onFailure { _bannerError.value = it.localizedMessage }
        }
    }

    fun deleteProgressPhoto(id: String) {
        viewModelScope.launch {
            runCatching { repository.deleteProgressPhoto(id) }
                .onFailure { _bannerError.value = it.localizedMessage }
        }
    }

    fun sendCoachMessage(prompt: String) {
        val clean = prompt.trim()
        if (clean.isBlank()) return
        val today = todayDateKey()
        val todayCals = meals.value.filter { it.dateKey == today }.sumOf { it.calories }
        val todayWater = waterLogs.value.filter { it.dateKey == today }.sumOf { it.amountMl }
        val recentSessions = workoutSessions.value.take(3).joinToString("; ") {
            "${it.workoutName} (${it.workingVolumeKg.toInt()} kg)"
        }
        val prSummary = personalRecords.value.take(4).joinToString("; ") {
            "${it.exerciseName} ${it.weightKg}kg×${it.reps}"
        }
        val trainingContext = buildString {
            if (recentSessions.isNotBlank()) append("Recent workouts: $recentSessions. ")
            if (prSummary.isNotBlank()) append("Top PRs: $prSummary.")
        }

        viewModelScope.launch {
            _coachState.value = AsyncState.Loading
            try {
                repository.addAiMessage("user", clean)
                var finalAnswer = ""
                aiService.streamCoachResponse(
                    userMessage = clean,
                    profile = userProfile.value,
                    todaySteps = todaySteps.value,
                    todayCaloriesIn = todayCals,
                    todayWaterMl = todayWater,
                    workoutContextSummary = trainingContext
                ).collect { partial ->
                    finalAnswer = partial
                    _coachState.value = AsyncState.Streaming(partial)
                }
                repository.addAiMessage("model", finalAnswer)
                _coachState.value = AsyncState.Success(finalAnswer)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _coachState.value = AsyncState.Error(e.localizedMessage ?: "AI Coach failed.")
            }
        }
    }

    fun requestAiSubstitutions(
        exerciseName: String,
        muscleGroup: String,
        availableEquipment: String,
        reason: String
    ) {
        viewModelScope.launch {
            _aiSubstitutionsState.value = AsyncState.Loading
            try {
                val res = aiService.suggestExerciseSubstitutions(
                    exerciseName = exerciseName,
                    muscleGroup = muscleGroup,
                    availableEquipment = availableEquipment,
                    reason = reason
                )
                _aiSubstitutionsState.value = AsyncState.Success(res)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _aiSubstitutionsState.value = AsyncState.Error(e.localizedMessage ?: "AI substitution failed.")
            }
        }
    }

    fun resetAiSubstitutions() {
        _aiSubstitutionsState.value = AsyncState.Idle
    }

    fun requestAiWorkoutAnalysis() {
        val recentSummary = workoutSessions.value.take(5).joinToString("; ") {
            "${it.dateKey}: ${it.workoutName} (${it.setsCompleted} sets, ${it.workingVolumeKg.toInt()} kg working vol)"
        }.ifBlank { "No completed sessions yet." }
        val prSummary = personalRecords.value.take(6).joinToString("; ") {
            "${it.exerciseName} ${it.weightKg}kg×${it.reps} (1RM ~${it.estimatedOneRmKg.toInt()}kg)"
        }.ifBlank { "No PRs recorded yet." }

        viewModelScope.launch {
            _aiWorkoutAnalysisState.value = AsyncState.Loading
            try {
                val res = aiService.analyzeWorkoutProgressAndRecovery(
                    profile = userProfile.value,
                    recentSessionsSummary = recentSummary,
                    personalRecordsSummary = prSummary
                )
                _aiWorkoutAnalysisState.value = AsyncState.Success(res)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _aiWorkoutAnalysisState.value = AsyncState.Error(e.localizedMessage ?: "AI progress analysis failed.")
            }
        }
    }

    fun scanFoodBitmap(bitmap: Bitmap) {
        viewModelScope.launch {
            _foodScanState.value = AsyncState.Loading
            try {
                val res = aiService.analyzeFoodImage(bitmap)
                _foodScanState.value = AsyncState.Success(res)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _foodScanState.value = AsyncState.Error(e.localizedMessage ?: "Food scan failed.")
            }
        }
    }

    fun resetFoodScan() {
        _foodScanState.value = AsyncState.Idle
    }

    fun generateAiWorkoutPlan(experience: String, equipment: String, goal: String, days: Int) {
        viewModelScope.launch {
            _aiWorkoutState.value = AsyncState.Loading
            try {
                val res = aiService.generateWorkoutPlan(experience, equipment, goal, days)
                _aiWorkoutState.value = AsyncState.Success(res)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _aiWorkoutState.value = AsyncState.Error(e.localizedMessage ?: "AI workout planner failed.")
            }
        }
    }

    fun generateAiMealPlan(preferences: String, allergies: String) {
        val profile = userProfile.value
        val effectiveAge = FitnessCalculator.resolveAgeYears(profile?.ageYears ?: 25L, profile?.dateOfBirthIso.orEmpty())
        val isMinor = FitnessCalculator.isMinor(effectiveAge)
        val rawCals = (profile?.dailyCalorieTarget ?: 2100L).toInt()
        val cals = FitTrackAiService.safeMealPlanCalorieFloor(rawCals, isMinor)
        val safeGoal = if (isMinor && profile?.goal == "weight_loss") "weight_maintenance" else (profile?.goal ?: "general_fitness")
        val macros = FitnessCalculator.calculateMacros(
            calorieTarget = cals,
            weightKg = profile?.weightKg ?: 70.0,
            goal = safeGoal,
            ageYears = effectiveAge
        )
        viewModelScope.launch {
            _aiMealPlanState.value = AsyncState.Loading
            try {
                val res = aiService.generateMealPlan(
                    calorieTarget = cals,
                    proteinG = macros.proteinGrams,
                    carbsG = macros.carbsGrams,
                    fatG = macros.fatGrams,
                    preferences = preferences,
                    allergies = allergies,
                    isMinor = isMinor
                )
                _aiMealPlanState.value = AsyncState.Success(res)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _aiMealPlanState.value = AsyncState.Error(e.localizedMessage ?: "AI meal planner failed.")
            }
        }
    }

    fun clearAiChatHistory() {
        viewModelScope.launch {
            runCatching {
                repository.clearAiMessages()
                _coachState.value = AsyncState.Idle
            }.onSuccess { _statusMessage.value = "AI Coach chat history cleared" }
                .onFailure { _bannerError.value = it.localizedMessage ?: "Failed to clear chat history" }
        }
    }

    suspend fun exportUserDataJson(): String {
        return repository.exportAllUserDataJson()
    }

    fun deleteAccountData(onComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                workoutTimerJob?.cancel()
                _activeWorkout.value = null
                repository.deleteAllUserData()
                userPreferencesStore.clearAllForUser(currentUserId)
                restSessionStore.clearAllForUser(currentUserId)
                stepManager.resetForAccountDeletion()
                GpsTrackerStore.reset()
                onComplete()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _bannerError.value = e.localizedMessage ?: "Failed to delete user data"
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stepManager.stopListening()
        workoutTimerJob?.cancel()
    }

    private companion object {
        const val TAG = "FitTrackViewModel"
    }
}
