package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.firebase.FitTrackRepository
import com.example.data.model.GpsActivity
import com.example.data.model.LatLngPoint
import com.example.data.model.MealLog
import com.example.data.model.PersonalRecord
import com.example.data.model.StepRecord
import com.example.data.model.TrainingNote
import com.example.data.model.UserProfile
import com.example.data.model.WorkoutPlan
import com.example.data.model.WorkoutProgram
import com.example.data.model.WorkoutScheduleItem
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class FitTrackRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun owner_canCreateProfile_logMeal_andObserveSteps() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repo = FitTrackRepository(firestore)

        val profile = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.getOrCreateProfile(
                UserProfile(
                    userId = uid,
                    displayName = "Alice",
                    email = ALICE_EMAIL
                )
            )
        }
        assertEquals(uid, profile.userId)

        val mealId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.addMeal(
                MealLog(
                    userId = uid,
                    dateKey = "2026-11-15",
                    mealType = "lunch",
                    foodName = "Grilled Salmon Bowl",
                    grams = 300.0,
                    calories = 520.0,
                    proteinG = 38.0,
                    carbsG = 42.0,
                    fatG = 21.0,
                    fiberG = 5.0,
                    sugarG = 3.0,
                    source = "manual"
                )
            )
        }
        assertTrue(mealId.isNotBlank())

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.upsertStepRecord(
                StepRecord(
                    userId = uid,
                    dateKey = "2026-11-15",
                    steps = 8450L,
                    manualSteps = 0L,
                    sensorBaseline = 1000L,
                    distanceKm = 6.1,
                    caloriesBurned = 310.0
                )
            )
        }

        val steps = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeStepRecords(uid).first { list -> list.any { it.dateKey == "2026-11-15" } }
        }
        assertEquals(8450L, steps.first { it.dateKey == "2026-11-15" }.steps)
    }

    @Test
    fun owner_canSaveGpsActivity() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repo = FitTrackRepository(firestore)

        val actId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.saveGpsActivity(
                GpsActivity(
                    userId = uid,
                    activityType = "running",
                    durationSeconds = 1200L,
                    distanceKm = 3.4,
                    avgSpeedKmh = 10.2,
                    avgPaceMinPerKm = 5.88,
                    caloriesBurned = 265.0,
                    routePoints = listOf(LatLngPoint(37.7749, -122.4194, 1700000000000L)),
                    notes = "Park loop",
                    startedAtMs = 1700000000000L,
                    endedAtMs = 1700001200000L
                )
            )
        }
        assertTrue(actId.isNotBlank())
    }

    @Test
    fun crossUserAccess_isDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = FitTrackRepository(firestore)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.getOrCreateProfile(UserProfile(userId = aliceUid, displayName = "Alice_${UUID.randomUUID()}"))
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = FitTrackRepository(firestore)
        var denied = false
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                bobRepo.observeMeals(aliceUid).first()
            }
        } catch (e: Exception) {
            val fsError = e.unwrapFirestoreException()
            if (fsError?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                denied = true
            }
        }
        assertTrue("Cross-user access should fail with PERMISSION_DENIED", denied)
    }

    @Test
    fun unauthenticatedObserveMeals_isDenied() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        auth.signOut()
        val repo = FitTrackRepository(firestore)

        var denied = false
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repo.observeMeals(uid).first()
            }
        } catch (e: Exception) {
            val fsError = e.unwrapFirestoreException()
            if (fsError?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                denied = true
            }
        }
        assertTrue("Unauthenticated query should fail with PERMISSION_DENIED", denied)
    }

    @Test
    fun owner_canManageWorkoutProgramsSchedulesPrsAndNotes() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repo = FitTrackRepository(firestore)

        val progId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.saveWorkoutProgram(
                WorkoutProgram(
                    userId = uid,
                    name = "Athletic PPL",
                    category = "custom",
                    goal = "muscle_gain",
                    level = "intermediate",
                    daysPerWeek = 3L,
                    description = "Custom 3-day split",
                    weeklySplit = listOf("Push", "Pull", "Legs"),
                    progressionGuidance = "Double progression",
                    workouts = listOf(WorkoutPlan(id = "d1", name = "Push")),
                    isCustom = true
                )
            )
        }
        assertTrue(progId.isNotBlank())

        val schedId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.upsertWorkoutSchedule(
                WorkoutScheduleItem(
                    userId = uid,
                    dateKey = "2026-11-16",
                    workoutId = "tpl_ppl_push",
                    workoutName = "Push Day",
                    programId = progId,
                    status = "scheduled"
                )
            )
        }
        assertEquals("2026-11-16", schedId)

        val prId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.upsertPersonalRecord(
                PersonalRecord(
                    userId = uid,
                    exerciseId = "ex_squat",
                    exerciseName = "Barbell Back Squat",
                    primaryMuscle = "Quadriceps",
                    weightKg = 100.0,
                    reps = 5L,
                    estimatedOneRmKg = 116.7,
                    setVolumeKg = 500.0,
                    dateKey = "2026-11-16",
                    sessionId = "s_1"
                )
            )
        }
        assertEquals("ex_squat", prId)

        val noteId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.saveTrainingNote(
                TrainingNote(
                    userId = uid,
                    title = "Squat depth",
                    body = "Sit hips back smoothly",
                    dateKey = "2026-11-16",
                    exerciseName = "Barbell Back Squat"
                )
            )
        }
        assertTrue(noteId.isNotBlank())
    }

    @Test
    fun owner_canManageGoalHistoryRecipesPlannedMealsPreferencesAndClearAiMessages() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repo = FitTrackRepository(firestore)

        // Recipe & meal_plan MealLog sources + ml unit
        val recipeMealId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.addMeal(
                MealLog(
                    userId = uid,
                    dateKey = "2026-11-15",
                    mealType = "snack",
                    foodName = "Whey Shake",
                    grams = 300.0,
                    measurementUnit = "ml",
                    calories = 240.0,
                    proteinG = 30.0,
                    carbsG = 12.0,
                    fatG = 4.0,
                    fiberG = 1.0,
                    sugarG = 3.0,
                    source = "recipe"
                )
            )
        }
        assertTrue(recipeMealId.isNotBlank())

        val goalId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.saveGoalHistoryEntry(
                com.example.data.model.GoalHistoryEntry(
                    userId = uid,
                    goalType = "strength",
                    title = "100 kg Squat",
                    status = "active",
                    targetValue = 100.0,
                    currentValue = 85.0,
                    unit = "kg",
                    targetDateKey = "2026-12-20",
                    notes = "Add 2.5kg biweekly"
                )
            )
        }
        assertTrue(goalId.isNotBlank())

        val recipeId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.saveRecipe(
                com.example.platform.SavedRecipe(
                    name = "Berry Smoothie",
                    servings = 2,
                    notes = "Blend cold",
                    measurementUnit = "ml",
                    ingredients = listOf(
                        com.example.platform.RecipeIngredient(
                            foodName = "Milk",
                            grams = 400.0,
                            measurementUnit = "ml",
                            calories = 200.0,
                            proteinG = 14.0,
                            carbsG = 20.0,
                            fatG = 6.0
                        )
                    )
                )
            )
        }
        assertTrue(recipeId.isNotBlank())

        val plannedId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.savePlannedMeal(
                com.example.platform.PlannedMealEntry(
                    dayOfWeek = "Monday",
                    mealType = "lunch",
                    title = "Chicken Rice Bowl",
                    grams = 420.0,
                    calories = 590.0,
                    proteinG = 46.0,
                    carbsG = 64.0,
                    fatG = 14.0
                )
            )
        }
        assertTrue(plannedId.isNotBlank())

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.saveSyncPreference("notifications", """{"hydrationReminderEnabled":true}""")
            repo.addAiMessage("user", "Quick tip?")
            repo.clearAiMessages()
        }

        val remainingMessages = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeAiMessages(uid).first()
        }
        assertEquals(0, remainingMessages.size)
    }

    private companion object {
        const val ALICE_EMAIL = "alice_fittrack@test.com"
        const val BOB_EMAIL = "bob_fittrack@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
