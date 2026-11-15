package com.example

import com.example.data.catalog.WorkoutCatalog
import com.example.data.model.CompletedSetRecord
import com.example.data.model.ExerciseSessionRecord
import com.example.data.model.PersonalRecord
import com.example.data.model.UserProfile
import com.example.data.model.WorkoutExerciseEntry
import com.example.data.model.WorkoutScheduleItem
import com.example.data.model.WorkoutSessionLog
import com.example.domain.WorkoutAnalyticsEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutAnalyticsEngineTest {

    @Test
    fun calculateSetVolume_computesWeightTimesReps() {
        assertEquals(600.0, WorkoutAnalyticsEngine.calculateSetVolume(60.0, 10L), 0.001)
        assertEquals(0.0, WorkoutAnalyticsEngine.calculateSetVolume(-10.0, 10L), 0.001)
        assertEquals(0.0, WorkoutAnalyticsEngine.calculateSetVolume(50.0, -2L), 0.001)
    }

    @Test
    fun computeSessionVolumes_excludesWarmupFromWorkingVolumeAndSkipsSkippedExercises() {
        val records = listOf(
            ExerciseSessionRecord(
                exerciseId = "ex_bench_press",
                exerciseName = "Bench Press",
                primaryMuscle = "Chest",
                sets = listOf(
                    CompletedSetRecord(setNumber = 1, setType = "warmup", reps = 10, weightKg = 20.0), // 200
                    CompletedSetRecord(setNumber = 2, setType = "working", reps = 8, weightKg = 60.0), // 480
                    CompletedSetRecord(setNumber = 3, setType = "superset", reps = 10, weightKg = 40.0), // 400
                    CompletedSetRecord(setNumber = 4, setType = "dropset", reps = 12, weightKg = 30.0) // 360
                )
            ),
            ExerciseSessionRecord(
                exerciseId = "ex_squat",
                exerciseName = "Squat",
                primaryMuscle = "Quadriceps",
                skipped = true,
                sets = listOf(
                    CompletedSetRecord(setNumber = 1, setType = "working", reps = 5, weightKg = 100.0)
                )
            )
        )

        val breakdown = WorkoutAnalyticsEngine.computeSessionVolumes(records)
        assertEquals(1240.0, breakdown.workingVolumeKg, 0.001)
        assertEquals(200.0, breakdown.warmupVolumeKg, 0.001)
        assertEquals(1440.0, breakdown.totalVolumeKg, 0.001)
        assertEquals(3, breakdown.workingSetsCount)
        assertEquals(1, breakdown.warmupSetsCount)
    }

    @Test
    fun estimateOneRmKg_handlesSingleRepAndEpleyFormula() {
        assertEquals(100.0, WorkoutAnalyticsEngine.estimateOneRmKg(100.0, 1L), 0.001)
        // Epley: 100 * (1 + 10/30) = 133.333
        assertEquals(133.333, WorkoutAnalyticsEngine.estimateOneRmKg(100.0, 10L), 0.01)
        assertEquals(0.0, WorkoutAnalyticsEngine.estimateOneRmKg(0.0, 10L), 0.001)
    }

    @Test
    fun analyzeProgression_suggestsPlusTwoPointFiveKgAfterHittingTopRepRangeWithoutAutoMutating() {
        val entry = WorkoutExerciseEntry(
            exerciseId = "ex_bench_press",
            exerciseName = "Barbell Bench Press",
            muscleGroup = "Chest",
            targetSets = 3L,
            targetReps = 8L,
            repRangeMin = 6L,
            repRangeMax = 8L,
            targetWeightKg = 60.0
        )
        val priorSession = WorkoutSessionLog(
            id = "s1",
            dateKey = "2026-11-10",
            exerciseRecords = listOf(
                ExerciseSessionRecord(
                    exerciseId = "ex_bench_press",
                    exerciseName = "Barbell Bench Press",
                    primaryMuscle = "Chest",
                    sets = listOf(
                        CompletedSetRecord(1, "warmup", 10, 30.0),
                        CompletedSetRecord(2, "working", 8, 60.0),
                        CompletedSetRecord(3, "working", 8, 60.0),
                        CompletedSetRecord(4, "working", 8, 60.0)
                    )
                )
            )
        )

        val rec = WorkoutAnalyticsEngine.analyzeProgression(entry, listOf(priorSession), null)
        assertFalse(rec.shouldDeload)
        assertTrue(rec.hasActionableIncrease)
        assertEquals(62.5, rec.suggestedWeightKg, 0.001)
        assertEquals(6L, rec.suggestedReps)
        // Explicit confirmation rule: entry must never be auto-mutated by analysis
        assertEquals(60.0, entry.targetWeightKg, 0.001)
    }

    @Test
    fun analyzeProgression_suggestsDeloadWhenConsecutiveSessionsFallBelowMinReps() {
        val entry = WorkoutExerciseEntry(
            exerciseId = "ex_squat",
            exerciseName = "Barbell Back Squat",
            muscleGroup = "Quadriceps",
            targetSets = 3L,
            targetReps = 8L,
            repRangeMin = 6L,
            repRangeMax = 8L,
            targetWeightKg = 100.0
        )
        val sessions = listOf("2026-11-12", "2026-11-09", "2026-11-06").mapIndexed { idx, dk ->
            WorkoutSessionLog(
                id = "s$idx",
                dateKey = dk,
                exerciseRecords = listOf(
                    ExerciseSessionRecord(
                        exerciseId = "ex_squat",
                        exerciseName = "Barbell Back Squat",
                        primaryMuscle = "Quadriceps",
                        sets = listOf(
                            CompletedSetRecord(1, "working", 4, 100.0),
                            CompletedSetRecord(2, "working", 4, 100.0)
                        )
                    )
                )
            )
        }

        val rec = WorkoutAnalyticsEngine.analyzeProgression(entry, sessions, null)
        assertTrue(rec.shouldDeload)
        assertEquals(85.0, rec.suggestedWeightKg, 0.001)
        assertEquals(6L, rec.suggestedReps)
    }

    @Test
    fun detectNewPersonalRecords_ignoresWarmupSetsAndDetectsWorkingPr() {
        val existingPr = PersonalRecord(
            id = "ex_bench_press",
            exerciseId = "ex_bench_press",
            exerciseName = "Bench Press",
            primaryMuscle = "Chest",
            weightKg = 60.0,
            reps = 8L,
            estimatedOneRmKg = WorkoutAnalyticsEngine.estimateOneRmKg(60.0, 8L)
        )
        val records = listOf(
            ExerciseSessionRecord(
                exerciseId = "ex_bench_press",
                exerciseName = "Bench Press",
                primaryMuscle = "Chest",
                sets = listOf(
                    CompletedSetRecord(1, "warmup", 12, 90.0), // deliberately heavy warmup should NOT trigger PR
                    CompletedSetRecord(2, "working", 10, 62.5)
                )
            )
        )

        val prs = WorkoutAnalyticsEngine.detectNewPersonalRecords("sess_1", "2026-11-15", records, listOf(existingPr))
        assertEquals(1, prs.size)
        assertEquals(62.5, prs.first().weightKg, 0.001)
        assertEquals(10L, prs.first().reps)
    }

    @Test
    fun personalizedProgramsForUser_scalesSetsAndRestForProfileRecoveryAndGoals() {
        val minorBeginner = UserProfile(ageYears = 16L, activityLevel = "light", goal = "general_fitness")
        val intenseAthlete = UserProfile(ageYears = 27L, activityLevel = "very_active", goal = "muscle_gain")

        val minorSets = WorkoutCatalog.personalizedProgramsForUser(minorBeginner)
            .first { it.id == "prog_upper_lower" }.workouts.first().exercises.first().targetSets
        val athleteSets = WorkoutCatalog.personalizedProgramsForUser(intenseAthlete)
            .first { it.id == "prog_upper_lower" }.workouts.first().exercises.first().targetSets

        assertTrue("Expected athlete sets ($athleteSets) > minor beginner sets ($minorSets)", athleteSets > minorSets)
        assertTrue(WorkoutCatalog.exercises.size >= 30)
        assertTrue(WorkoutCatalog.exercises.map { it.muscleGroup }.distinct().size >= 11)
    }

    @Test
    fun computeDashboardMetrics_preservesStreakAcrossScheduledRestDay() {
        val sessions = listOf(
            WorkoutSessionLog(
                id = "s1",
                dateKey = "2026-11-15",
                durationSeconds = 2400L,
                workingVolumeKg = 3000.0,
                exerciseRecords = listOf(
                    ExerciseSessionRecord(
                        exerciseId = "ex_bench_press",
                        exerciseName = "Bench Press",
                        primaryMuscle = "Chest",
                        sets = listOf(CompletedSetRecord(1, "working", 8, 60.0))
                    )
                )
            ),
            WorkoutSessionLog(
                id = "s2",
                dateKey = "2026-11-13",
                durationSeconds = 1800L,
                workingVolumeKg = 2200.0,
                exerciseRecords = listOf(
                    ExerciseSessionRecord(
                        exerciseId = "ex_squat",
                        exerciseName = "Squat",
                        primaryMuscle = "Quadriceps",
                        sets = listOf(CompletedSetRecord(1, "working", 8, 70.0))
                    )
                )
            )
        )
        val schedules = listOf(
            WorkoutScheduleItem(id = "2026-11-15", dateKey = "2026-11-15", status = "completed"),
            WorkoutScheduleItem(id = "2026-11-14", dateKey = "2026-11-14", status = "rest"),
            WorkoutScheduleItem(id = "2026-11-13", dateKey = "2026-11-13", status = "completed")
        )

        val metrics = WorkoutAnalyticsEngine.computeDashboardMetrics(sessions, schedules, todayKey = "2026-11-15")
        assertEquals(2, metrics.totalWorkouts)
        assertEquals(2, metrics.currentStreakDays)
        assertEquals(100, metrics.consistencyPercent)
        assertEquals(5200.0, metrics.totalWorkingVolumeKg, 0.001)
    }

    @Test
    fun detectNewPersonalRecords_handlesBodyweightMaxRepTieBreaker() {
        val existingBwPr = PersonalRecord(
            id = "ex_pull_ups",
            exerciseId = "ex_pull_ups",
            exerciseName = "Pull-Ups",
            primaryMuscle = "Back",
            weightKg = 0.0,
            reps = 10L,
            estimatedOneRmKg = 0.0
        )
        val records = listOf(
            ExerciseSessionRecord(
                exerciseId = "ex_pull_ups",
                exerciseName = "Pull-Ups",
                primaryMuscle = "Back",
                sets = listOf(
                    CompletedSetRecord(1, "working", 8L, 0.0),
                    CompletedSetRecord(2, "working", 15L, 0.0),
                    CompletedSetRecord(3, "working", 11L, 0.0)
                )
            )
        )

        val prs = WorkoutAnalyticsEngine.detectNewPersonalRecords("sess_bw", "2026-11-15", records, listOf(existingBwPr))
        assertEquals(1, prs.size)
        assertEquals(0.0, prs.first().weightKg, 0.001)
        assertEquals(15L, prs.first().reps)
    }

    @Test
    fun analyzeProgression_andFormatting_handleBodyweightAndImperialUnits() {
        val entry = WorkoutExerciseEntry(
            exerciseId = "ex_push_ups",
            exerciseName = "Push-Ups",
            muscleGroup = "Chest",
            targetSets = 3L,
            targetReps = 12L,
            repRangeMin = 8L,
            repRangeMax = 15L,
            targetWeightKg = 0.0
        )
        val prior = WorkoutSessionLog(
            id = "s_bw",
            dateKey = "2026-11-14",
            exerciseRecords = listOf(
                ExerciseSessionRecord(
                    exerciseId = "ex_push_ups",
                    exerciseName = "Push-Ups",
                    primaryMuscle = "Chest",
                    sets = listOf(CompletedSetRecord(1, "working", 15L, 0.0))
                )
            )
        )
        val rec = WorkoutAnalyticsEngine.analyzeProgression(entry, listOf(prior), null)
        assertFalse(rec.shouldDeload)
        assertEquals(0.0, rec.suggestedWeightKg, 0.001)
        assertEquals(16L, rec.suggestedReps)
        assertTrue(rec.rationale.contains("Bodyweight progression"))

        assertEquals("BW", WorkoutAnalyticsEngine.formatWeight(0.0, "imperial"))
        assertTrue(WorkoutAnalyticsEngine.formatWeight(100.0, "imperial").endsWith("lb"))
        assertTrue(WorkoutAnalyticsEngine.formatVolume(100.0, "imperial").endsWith("lb"))
    }
}
