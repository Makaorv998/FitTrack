package com.example.domain

import com.example.data.model.CompletedSetRecord
import com.example.data.model.ExerciseSessionRecord
import com.example.data.model.PersonalRecord
import com.example.data.model.WorkoutExerciseEntry
import com.example.data.model.WorkoutScheduleItem
import com.example.data.model.WorkoutSessionLog
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

data class SessionVolumeBreakdown(
    val workingVolumeKg: Double,
    val warmupVolumeKg: Double,
    val totalVolumeKg: Double,
    val workingSetsCount: Int,
    val warmupSetsCount: Int
)

data class ProgressionRecommendation(
    val exerciseName: String,
    val previousBestWeightKg: Double?,
    val previousBestReps: Long?,
    val previousSessionSummary: String,
    val suggestedWeightKg: Double,
    val suggestedReps: Long,
    val rationale: String,
    val shouldDeload: Boolean,
    val hasActionableIncrease: Boolean
)

data class ExerciseProgressPoint(
    val dateKey: String,
    val maxWeightKg: Double,
    val estimatedOneRmKg: Double,
    val workingVolumeKg: Double
)

data class WorkoutDashboardMetrics(
    val totalWorkouts: Int,
    val totalMinutes: Long,
    val totalWorkingVolumeKg: Double,
    val averageDurationMinutes: Int,
    val currentStreakDays: Int,
    val weeklyFrequency: Double,
    val consistencyPercent: Int,
    val weeklyVolumeBars: List<Pair<String, Float>>,
    val monthlyVolumeBars: List<Pair<String, Float>>,
    val muscleDistribution: List<Pair<String, Int>>,
    val recoveryGuidance: String
)

object WorkoutAnalyticsEngine {

    fun calculateSetVolume(weightKg: Double, reps: Long): Double =
        (weightKg.coerceAtLeast(0.0) * reps.coerceAtLeast(0L).toDouble())

    fun estimateOneRmKg(weightKg: Double, reps: Long): Double {
        if (weightKg <= 0.0 || reps <= 0L) return 0.0
        if (reps == 1L) return weightKg
        return weightKg * (1.0 + (reps.toDouble() / 30.0))
    }

    fun computeSessionVolumes(records: List<ExerciseSessionRecord>): SessionVolumeBreakdown {
        var workingVol = 0.0
        var warmupVol = 0.0
        var workingSets = 0
        var warmupSets = 0

        for (exercise in records) {
            if (exercise.skipped) continue
            for (set in exercise.sets) {
                val vol = calculateSetVolume(set.weightKg, set.reps)
                if (set.setType.equals("warmup", ignoreCase = true)) {
                    warmupVol += vol
                    warmupSets++
                } else {
                    workingVol += vol
                    workingSets++
                }
            }
        }
        return SessionVolumeBreakdown(
            workingVolumeKg = workingVol,
            warmupVolumeKg = warmupVol,
            totalVolumeKg = workingVol + warmupVol,
            workingSetsCount = workingSets,
            warmupSetsCount = warmupSets
        )
    }

    /**
     * Evaluates previous logged sets for an exercise and proposes a conservative overload
     * increment (+2.5 kg or +1 rep) or deload guidance. Suggestions are advisory only and
     * require explicit user confirmation before updating workout weights.
     */
    fun analyzeProgression(
        entry: WorkoutExerciseEntry,
        sessions: List<WorkoutSessionLog>,
        personalRecord: PersonalRecord?
    ): ProgressionRecommendation {
        val pastExerciseLogs = sessions.mapNotNull { session ->
            val rec = session.exerciseRecords.firstOrNull {
                it.exerciseId == entry.exerciseId ||
                    it.exerciseName.equals(entry.exerciseName, ignoreCase = true)
            }
            if (rec != null && !rec.skipped) {
                val workingSets = rec.sets.filter { !it.setType.equals("warmup", ignoreCase = true) }
                if (workingSets.isNotEmpty()) session to workingSets else null
            } else null
        }

        if (pastExerciseLogs.isEmpty()) {
            return ProgressionRecommendation(
                exerciseName = entry.exerciseName,
                previousBestWeightKg = personalRecord?.weightKg,
                previousBestReps = personalRecord?.reps,
                previousSessionSummary = if (personalRecord != null) {
                    "Best PR: ${formatWeight(personalRecord.weightKg)} × ${personalRecord.reps}"
                } else {
                    "No previous session logged yet"
                },
                suggestedWeightKg = entry.targetWeightKg,
                suggestedReps = entry.targetReps,
                rationale = "Baseline session: focus on clean technique within ${entry.repRangeMin}–${entry.repRangeMax} reps.",
                shouldDeload = false,
                hasActionableIncrease = false
            )
        }

        val (_, lastSets) = pastExerciseLogs.first()
        val lastTopSet = lastSets.maxByOrNull { it.weightKg * 100 + it.reps } ?: lastSets.first()
        val allHitTopRange = lastSets.size >= entry.targetSets.toInt().coerceAtLeast(1) &&
            lastSets.all { it.reps >= entry.repRangeMax }

        val recentTopWeights = pastExerciseLogs.take(3).map { (_, sets) ->
            sets.maxOfOrNull { it.weightKg } ?: 0.0
        }
        val recentTopReps = pastExerciseLogs.take(3).map { (_, sets) ->
            sets.maxOfOrNull { it.reps } ?: 0L
        }
        val stagnantOrFatigued = recentTopWeights.size >= 3 &&
            recentTopWeights[0] <= recentTopWeights[1] &&
            recentTopReps[0] < entry.repRangeMin &&
            recentTopReps[1] < entry.repRangeMin

        val prevSummary = lastSets.joinToString(", ") {
            "${formatWeight(it.weightKg)}×${it.reps}"
        }

        return when {
            stagnantOrFatigued && lastTopSet.weightKg <= 0.0 -> {
                val recoveryReps = (entry.repRangeMin - 1L).coerceAtLeast(1L)
                ProgressionRecommendation(
                    exerciseName = entry.exerciseName,
                    previousBestWeightKg = 0.0,
                    previousBestReps = lastTopSet.reps,
                    previousSessionSummary = "Last: $prevSummary",
                    suggestedWeightKg = 0.0,
                    suggestedReps = recoveryReps,
                    rationale = "Bodyweight reps dropped below target range across consecutive sessions. Focus on recovery and stop 2 reps shy of failure today.",
                    shouldDeload = true,
                    hasActionableIncrease = false
                )
            }
            stagnantOrFatigued -> {
                val deloadWeight = (lastTopSet.weightKg * 0.85).let { roundToHalfKg(it) }
                ProgressionRecommendation(
                    exerciseName = entry.exerciseName,
                    previousBestWeightKg = lastTopSet.weightKg,
                    previousBestReps = lastTopSet.reps,
                    previousSessionSummary = "Last: $prevSummary",
                    suggestedWeightKg = deloadWeight,
                    suggestedReps = entry.repRangeMin,
                    rationale = "Reps dropped below target range across consecutive sessions. Consider a 15% recovery deload today.",
                    shouldDeload = true,
                    hasActionableIncrease = deloadWeight != entry.targetWeightKg
                )
            }
            allHitTopRange && lastTopSet.weightKg > 0.0 -> {
                val nextWeight = roundToHalfKg(lastTopSet.weightKg + 2.5)
                ProgressionRecommendation(
                    exerciseName = entry.exerciseName,
                    previousBestWeightKg = lastTopSet.weightKg,
                    previousBestReps = lastTopSet.reps,
                    previousSessionSummary = "Last: $prevSummary",
                    suggestedWeightKg = nextWeight,
                    suggestedReps = entry.repRangeMin,
                    rationale = "You completed all working sets at ${entry.repRangeMax}+ reps. Ready to confirm +2.5 kg.",
                    shouldDeload = false,
                    hasActionableIncrease = nextWeight > entry.targetWeightKg
                )
            }
            else -> {
                val nextReps = (lastTopSet.reps + 1L).coerceIn(entry.repRangeMin, entry.repRangeMax + 2)
                val rationaleText = if (lastTopSet.weightKg <= 0.0) {
                    "Bodyweight progression: aim for +1 rep per set (or slow down eccentric tempo) toward ${entry.repRangeMax + 2} reps."
                } else {
                    "Keep load at ${formatWeight(lastTopSet.weightKg)} and aim for +1 rep per set before adding weight."
                }
                ProgressionRecommendation(
                    exerciseName = entry.exerciseName,
                    previousBestWeightKg = lastTopSet.weightKg,
                    previousBestReps = lastTopSet.reps,
                    previousSessionSummary = "Last: $prevSummary",
                    suggestedWeightKg = lastTopSet.weightKg,
                    suggestedReps = nextReps,
                    rationale = rationaleText,
                    shouldDeload = false,
                    hasActionableIncrease = nextReps > entry.targetReps || lastTopSet.weightKg > entry.targetWeightKg
                )
            }
        }
    }

    fun exerciseHistorySeries(
        exerciseId: String,
        exerciseName: String,
        sessions: List<WorkoutSessionLog>
    ): List<ExerciseProgressPoint> {
        val points = sessions.mapNotNull { s ->
            val rec = s.exerciseRecords.firstOrNull {
                it.exerciseId == exerciseId || it.exerciseName.equals(exerciseName, ignoreCase = true)
            } ?: return@mapNotNull null
            if (rec.skipped) return@mapNotNull null
            val working = rec.sets.filter { !it.setType.equals("warmup", ignoreCase = true) }
            if (working.isEmpty()) return@mapNotNull null
            val maxW = working.maxOf { it.weightKg }
            val best1Rm = working.maxOf { estimateOneRmKg(it.weightKg, it.reps) }
            val workVol = working.sumOf { calculateSetVolume(it.weightKg, it.reps) }
            val key = s.dateKey.ifBlank { dateKeyFromEpoch(s.createdAt?.toDate()?.time ?: System.currentTimeMillis()) }
            ExerciseProgressPoint(key, maxW, best1Rm, workVol)
        }
        return points.sortedBy { it.dateKey }
    }

    fun detectNewPersonalRecords(
        sessionId: String,
        dateKey: String,
        records: List<ExerciseSessionRecord>,
        existingPrs: List<PersonalRecord>
    ): List<PersonalRecord> {
        val newlyBroken = mutableListOf<PersonalRecord>()
        for (ex in records) {
            if (ex.skipped) continue
            val working = ex.sets.filter { !it.setType.equals("warmup", ignoreCase = true) && it.reps > 0L }
            if (working.isEmpty()) continue
            val bestSet = working.maxByOrNull { estimateOneRmKg(it.weightKg, it.reps) * 1000.0 + it.reps } ?: continue
            val bestOneRm = estimateOneRmKg(bestSet.weightKg, bestSet.reps)
            val bestSetVol = calculateSetVolume(bestSet.weightKg, bestSet.reps)

            val prevBest = existingPrs
                .filter { it.exerciseId == ex.exerciseId || it.exerciseName.equals(ex.exerciseName, ignoreCase = true) }
                .maxByOrNull { it.estimatedOneRmKg * 1000.0 + it.reps }

            val beatOneRm = prevBest == null && (bestSet.weightKg > 0.0 || bestSet.reps >= 1L)
            val beatExisting = prevBest != null && (
                bestOneRm > prevBest.estimatedOneRmKg + 0.1 ||
                    bestSet.weightKg > prevBest.weightKg + 0.1 ||
                    (kotlin.math.abs(bestSet.weightKg - prevBest.weightKg) <= 0.01 && bestSet.reps > prevBest.reps)
                )

            if (beatOneRm || beatExisting) {
                val docId = (ex.exerciseId.ifBlank {
                    ex.exerciseName.lowercase().replace(Regex("[^a-z0-9]+"), "_")
                }).take(64)
                newlyBroken.add(
                    PersonalRecord(
                        id = docId,
                        exerciseId = ex.exerciseId,
                        exerciseName = ex.exerciseName,
                        primaryMuscle = ex.primaryMuscle,
                        weightKg = bestSet.weightKg,
                        reps = bestSet.reps,
                        estimatedOneRmKg = bestOneRm,
                        setVolumeKg = bestSetVol,
                        dateKey = dateKey,
                        sessionId = sessionId
                    )
                )
            }
        }
        return newlyBroken
    }

    fun computeDashboardMetrics(
        sessions: List<WorkoutSessionLog>,
        schedules: List<WorkoutScheduleItem>,
        todayKey: String = dateKeyFromEpoch(System.currentTimeMillis())
    ): WorkoutDashboardMetrics {
        val totalWorkouts = sessions.size
        val totalMinutes = sessions.sumOf { it.durationSeconds } / 60L
        val totalWorkingVol = sessions.sumOf { s ->
            if (s.workingVolumeKg > 0.0) s.workingVolumeKg else s.totalVolumeKg
        }
        val avgDuration = if (totalWorkouts > 0) (totalMinutes / totalWorkouts).toInt().coerceAtLeast(1) else 0

        val sessionDateKeys = sessions.mapNotNull { s ->
            s.dateKey.ifBlank { s.createdAt?.toDate()?.time?.let(::dateKeyFromEpoch) }
        }.toSet()

        val streak = computeTrainingStreak(sessionDateKeys, schedules, todayKey)

        // Weekly frequency across active span (last 28 days)
        val cutoff28 = shiftDateKey(todayKey, -28)
        val recentSessions28 = sessions.count { s ->
            val dk = s.dateKey.ifBlank { dateKeyFromEpoch(s.createdAt?.toDate()?.time ?: 0L) }
            dk >= cutoff28
        }
        val weeklyFreq = (recentSessions28 / 4.0)

        // Consistency against scheduled non-rest days
        val plannedDays = schedules.filter { it.status != "rest" && it.dateKey <= todayKey }
        val consistency = if (plannedDays.isNotEmpty()) {
            val donePlanned = plannedDays.count { it.status == "completed" || it.dateKey in sessionDateKeys }
            ((donePlanned.toDouble() / plannedDays.size.toDouble()) * 100.0).roundToInt().coerceIn(0, 100)
        } else if (recentSessions28 > 0) {
            ((recentSessions28.toDouble() / 12.0) * 100.0).roundToInt().coerceIn(0, 100)
        } else 0

        // Weekly volume bars (real sessions grouped by week label)
        val weekFmt = SimpleDateFormat("'W'w", Locale.US)
        val monthFmt = SimpleDateFormat("MMM", Locale.US)
        val dateParser = SimpleDateFormat("yyyy-MM-dd", Locale.US)

        val weeklyBuckets = linkedMapOf<String, Float>()
        val monthlyBuckets = linkedMapOf<String, Float>()
        for (s in sessions.sortedBy { it.dateKey.ifBlank { "9999-99-99" } }) {
            val dk = s.dateKey.ifBlank { dateKeyFromEpoch(s.createdAt?.toDate()?.time ?: System.currentTimeMillis()) }
            val dt = runCatching { dateParser.parse(dk) }.getOrNull() ?: Date()
            val wk = weekFmt.format(dt)
            val mo = monthFmt.format(dt)
            val vol = (if (s.workingVolumeKg > 0.0) s.workingVolumeKg else s.totalVolumeKg).toFloat()
            weeklyBuckets[wk] = (weeklyBuckets[wk] ?: 0f) + vol
            monthlyBuckets[mo] = (monthlyBuckets[mo] ?: 0f) + vol
        }

        // Muscle group working set distribution
        val muscleSetCounts = linkedMapOf<String, Int>()
        for (s in sessions) {
            for (ex in s.exerciseRecords) {
                if (ex.skipped || ex.primaryMuscle.isBlank()) continue
                val workingCount = ex.sets.count { !it.setType.equals("warmup", ignoreCase = true) }
                if (workingCount > 0) {
                    muscleSetCounts[ex.primaryMuscle] = (muscleSetCounts[ex.primaryMuscle] ?: 0) + workingCount
                }
            }
        }
        val sortedMuscles = muscleSetCounts.entries
            .sortedByDescending { it.value }
            .map { it.key to it.value }

        val recent3Days = (0..2).count { shiftDateKey(todayKey, -it) in sessionDateKeys }
        val recoveryGuidance = when {
            totalWorkouts == 0 -> "Log your first workout to unlock recovery pacing and progression trends."
            recent3Days >= 3 -> "3 consecutive training days logged — schedule a recovery or mobility day next."
            streak >= 5 -> "Strong training streak! Watch sleep and hydration to sustain progressive overload."
            else -> "Balanced workload. Aim for 48 hours of recovery before repeating the same primary muscle group."
        }

        return WorkoutDashboardMetrics(
            totalWorkouts = totalWorkouts,
            totalMinutes = totalMinutes,
            totalWorkingVolumeKg = totalWorkingVol,
            averageDurationMinutes = avgDuration,
            currentStreakDays = streak,
            weeklyFrequency = weeklyFreq,
            consistencyPercent = consistency,
            weeklyVolumeBars = weeklyBuckets.entries.toList().takeLast(6).map { it.key to it.value },
            monthlyVolumeBars = monthlyBuckets.entries.toList().takeLast(6).map { it.key to it.value },
            muscleDistribution = sortedMuscles,
            recoveryGuidance = recoveryGuidance
        )
    }

    private fun computeTrainingStreak(
        sessionDateKeys: Set<String>,
        schedules: List<WorkoutScheduleItem>,
        todayKey: String
    ): Int {
        if (sessionDateKeys.isEmpty()) return 0
        val restDays = schedules.filter { it.status == "rest" }.map { it.dateKey }.toSet()
        var streak = 0
        var offset = 0
        // Allow today to still be pending if yesterday was active
        if (todayKey !in sessionDateKeys) {
            offset = 1
        }
        while (offset < 365) {
            val dk = shiftDateKey(todayKey, -offset)
            if (dk in sessionDateKeys) {
                streak++
                offset++
            } else if (dk in restDays) {
                offset++
            } else {
                break
            }
        }
        return streak
    }

    fun dateKeyFromEpoch(epochMs: Long): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(epochMs))

    fun shiftDateKey(baseDateKey: String, deltaDays: Int): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val cal = Calendar.getInstance()
        cal.time = runCatching { fmt.parse(baseDateKey) }.getOrNull() ?: Date()
        cal.add(Calendar.DAY_OF_YEAR, deltaDays)
        return fmt.format(cal.time)
    }

    private fun roundToHalfKg(value: Double): Double =
        (value * 2.0).roundToInt() / 2.0

    fun formatWeight(kg: Double): String =
        if (kg <= 0.0) "BW" else if (kg % 1.0 == 0.0) "${kg.toInt()} kg" else String.format(Locale.US, "%.1f kg", kg)

    fun formatWeight(kg: Double, unitSystem: String): String =
        if (kg <= 0.0) "BW" else UnitFormatter.formatWeight(kg, unitSystem)

    fun formatVolume(kg: Double, unitSystem: String): String {
        val displayVal = UnitFormatter.kgToDisplayWeight(kg.coerceAtLeast(0.0), unitSystem).roundToInt()
        return "$displayVal ${UnitFormatter.weightUnitLabel(unitSystem)}"
    }
}
