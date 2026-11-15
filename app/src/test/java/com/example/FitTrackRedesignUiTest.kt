package com.example

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.outlined.DirectionsRun
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import com.example.data.ai.FoodScanResult
import com.example.data.ai.ScannedFoodItem
import com.example.data.model.ExerciseSessionRecord
import com.example.data.model.GpsActivity
import com.example.data.model.LatLngPoint
import com.example.data.model.MealLog
import com.example.data.model.UserProfile
import com.example.data.model.WorkoutExerciseEntry
import com.example.data.model.WorkoutPlan
import com.example.platform.NutritionPreferences
import com.example.platform.RecipeIngredient
import com.example.platform.RestTimerSettings
import com.example.platform.SavedRecipe
import com.example.platform.TrainingPreferences
import com.example.platform.todayDateKey
import com.example.ui.ActiveWorkoutState
import com.example.ui.AsyncState
import com.example.ui.components.BottomNavTabItem
import com.example.ui.components.FloatingBottomNavigationBar
import com.example.ui.navigation.FitTrackRoute
import com.example.ui.screens.ActivityMapScreen
import com.example.ui.screens.AiFoodScannerScreen
import com.example.ui.screens.NutritionScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.workouts.ActiveWorkoutScreen
import com.example.ui.screens.workouts.WorkoutProgramsView
import com.example.ui.theme.FitTrackTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class FitTrackRedesignUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun primaryBottomNav_rendersFiveDestinationsAndSupportsLightDarkThemes() {
        val tabs = listOf(
            BottomNavTabItem(FitTrackRoute.Home, "Home", Icons.Filled.Home, Icons.Outlined.Home),
            BottomNavTabItem(FitTrackRoute.Activity, "Activity", Icons.AutoMirrored.Filled.DirectionsRun, Icons.AutoMirrored.Outlined.DirectionsRun),
            BottomNavTabItem(FitTrackRoute.Workouts, "Workout", Icons.Filled.FitnessCenter, Icons.Outlined.FitnessCenter),
            BottomNavTabItem(FitTrackRoute.Nutrition, "Nutrition", Icons.Filled.Restaurant, Icons.Outlined.Restaurant),
            BottomNavTabItem(FitTrackRoute.Profile, "Profile", Icons.Filled.Person, Icons.Outlined.Person)
        )
        var selectedRoute by mutableStateOf<FitTrackRoute>(FitTrackRoute.Home)
        var themePref by mutableStateOf("dark")

        composeRule.setContent {
            FitTrackTheme(themePreference = themePref) {
                FloatingBottomNavigationBar(
                    tabs = tabs,
                    isSelected = { it == selectedRoute },
                    onSelect = { selectedRoute = it }
                )
            }
        }

        listOf("Home", "Activity", "Workout", "Nutrition", "Profile").forEach { label ->
            composeRule.onNodeWithText(label).assertIsDisplayed()
        }

        composeRule.onNodeWithTag("nav_workout").performClick()
        assertEquals(FitTrackRoute.Workouts, selectedRoute)

        themePref = "light"
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("nav_nutrition").performClick()
        assertEquals(FitTrackRoute.Nutrition, selectedRoute)
    }

    @Test
    fun readOnlyMapScreen_showsEmptyStateAndNeverExposesTrackingButtons() {
        composeRule.setContent {
            FitTrackTheme(themePreference = "dark") {
                ActivityMapScreen(
                    gpsHistory = emptyList(),
                    onBack = {}
                )
            }
        }

        composeRule.onNodeWithText("Route Map & History").assertIsDisplayed()
        composeRule.onNodeWithTag("read_only_map_screen")
            .performScrollToNode(hasText("No Recorded GPS Route Yet"))
        composeRule.onNodeWithText("No Recorded GPS Route Yet").assertIsDisplayed()
        composeRule.onNodeWithTag("read_only_map_screen")
            .performScrollToNode(hasText("No saved routes"))
        composeRule.onNodeWithText("No saved routes").assertIsDisplayed()

        // Strictly read-only invariant
        composeRule.onNodeWithText("Start Activity").assertDoesNotExist()
        composeRule.onNodeWithText("Stop Activity").assertDoesNotExist()
        composeRule.onNodeWithText("Start Run").assertDoesNotExist()
    }

    @Test
    fun readOnlyMapScreen_rendersSavedRouteStartFinishMarkersAndTelemetry() {
        val sampleRoute = GpsActivity(
            id = "route_1",
            activityType = "running",
            durationSeconds = 1530L,
            distanceKm = 4.25,
            avgSpeedKmh = 10.0,
            avgPaceMinPerKm = 6.0,
            caloriesBurned = 320.0,
            routePoints = listOf(
                LatLngPoint(37.7749, -122.4194, 1700000000000L),
                LatLngPoint(37.7765, -122.4170, 1700000600000L),
                LatLngPoint(37.7780, -122.4150, 1700001530000L)
            ),
            startedAtMs = 1700000000000L,
            endedAtMs = 1700001530000L
        )

        composeRule.setContent {
            FitTrackTheme(themePreference = "light") {
                ActivityMapScreen(
                    gpsHistory = listOf(sampleRoute),
                    initialActivityId = "route_1",
                    onBack = {}
                )
            }
        }

        composeRule.onNodeWithTag("read_only_map_screen")
            .performScrollToNode(hasText("Start Marker"))
        composeRule.onNodeWithText("Start Marker").assertExists()
        composeRule.onNodeWithText("Finish Marker").assertExists()
        composeRule.onNodeWithText("4.25 km").assertExists()
        composeRule.onNodeWithTag("read_only_map_screen")
            .performScrollToNode(hasText("Saved Route History (1)"))
        composeRule.onNodeWithText("Saved Route History (1)").assertIsDisplayed()
        composeRule.onNodeWithText("Start Activity").assertDoesNotExist()
        composeRule.onNodeWithText("Stop Activity").assertDoesNotExist()
    }

    @Test
    fun workoutProgramsView_exposesAllNineCategoryFiltersAndFiltersCustomEmptyState() {
        composeRule.setContent {
            FitTrackTheme(themePreference = "dark") {
                WorkoutProgramsView(
                    profile = UserProfile(displayName = "Test Athlete"),
                    trainingPreferences = TrainingPreferences(),
                    customPlans = emptyList(),
                    customPrograms = emptyList(),
                    onStartWorkout = {},
                    onScheduleWorkoutForToday = {},
                    onSelectActiveProgram = {},
                    onSavePlan = {},
                    onDuplicatePlan = {},
                    onDeletePlan = {},
                    onSaveProgram = {},
                    onDeleteProgram = {},
                    onOpenAiPlanner = {}
                )
            }
        }

        val expectedCategoryKeys = listOf(
            "all", "full_body", "upper_lower", "ppl", "bro_split",
            "strength", "hypertrophy", "beginner", "home", "custom"
        )
        expectedCategoryKeys.forEach { key ->
            composeRule.onNodeWithTag("program_category_chip_$key").assertExists()
        }

        composeRule.onNodeWithTag("program_category_chip_custom")
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithText("No custom programs yet").assertIsDisplayed()
    }

    @Test
    fun activeWorkoutScreen_rendersFiveColumnSetTableTogglesInstructionsAndLogsSet() {
        var loggedCallCount = 0
        val benchEntry = WorkoutExerciseEntry(
            exerciseId = "ex_bench_press",
            exerciseName = "Barbell Bench Press",
            muscleGroup = "Chest",
            equipment = "Barbell",
            targetSets = 3L,
            targetReps = 8L,
            repRangeMin = 6L,
            repRangeMax = 8L,
            targetWeightKg = 60.0,
            restSeconds = 90L
        )
        val state = ActiveWorkoutState(
            plan = WorkoutPlan(
                id = "plan_push",
                name = "Push Strength",
                exercises = listOf(benchEntry)
            ),
            records = listOf(
                ExerciseSessionRecord(
                    exerciseId = "ex_bench_press",
                    exerciseName = "Barbell Bench Press",
                    primaryMuscle = "Chest",
                    equipment = "Barbell",
                    sets = emptyList()
                )
            ),
            currentExerciseIndex = 0,
            elapsedSeconds = 125L,
            restSettings = RestTimerSettings()
        )

        composeRule.setContent {
            FitTrackTheme(themePreference = "dark") {
                ActiveWorkoutScreen(
                    active = state,
                    sessions = emptyList(),
                    personalRecords = emptyList(),
                    aiSubstitutionsState = AsyncState.Idle,
                    onToggleWorkoutPause = {},
                    onSelectExerciseIndex = {},
                    onLogSet = { _, _, _, _, _ -> loggedCallCount++ },
                    onUpdateLoggedSet = { _, _, _, _, _ -> },
                    onDeleteLoggedSet = { _, _ -> },
                    onSkipExercise = {},
                    onReplaceExercise = { _, _ -> },
                    onUpdateExerciseNote = { _, _ -> },
                    onUpdateSessionNotes = {},
                    onApplyProgressionConfirmation = { _, _, _ -> },
                    onAdjustRest = {},
                    onToggleRestPause = {},
                    onSkipRest = {},
                    onStartCustomRest = {},
                    onUpdateRestSettings = {},
                    onRequestAiSubstitutions = { _, _, _, _ -> },
                    onResetAiSubstitutions = {},
                    onFinishWorkout = {},
                    onDiscardWorkout = {}
                )
            }
        }

        // Set table headers inside active_set_table
        listOf("Set", "Previous", "Target", "Actual", "Status").forEach { header ->
            composeRule.onNode(
                hasText(header) and hasAnyAncestor(hasTestTag("active_set_table"))
            ).performScrollTo().assertIsDisplayed()
        }
        composeRule.onNode(
            hasText("Next") and hasAnyAncestor(hasTestTag("active_set_table"))
        ).performScrollTo().assertIsDisplayed()

        // Collapsible exercise instructions
        composeRule.onNodeWithTag("toggle_instructions_button").performScrollTo().performClick()
        composeRule.onNodeWithText("Exercise Instructions & Cues").assertIsDisplayed()

        // Complete set button
        composeRule.onNodeWithTag("log_set_button").performScrollTo().performClick()
        assertEquals(1, loggedCallCount)
    }

    @Test
    fun savedRecipe_calculatesPerServingNutritionAccurately() {
        val recipe = SavedRecipe(
            id = "rec_1",
            name = "Overnight Protein Oats",
            servings = 2,
            ingredients = listOf(
                RecipeIngredient("Rolled Oats", grams = 100.0, calories = 380.0, proteinG = 13.0, carbsG = 66.0, fatG = 7.0),
                RecipeIngredient("Whey Isolate", grams = 60.0, calories = 220.0, proteinG = 48.0, carbsG = 4.0, fatG = 1.0)
            )
        )

        assertEquals(600.0, recipe.totalCalories, 0.01)
        assertEquals(300.0, recipe.caloriesPerServing, 0.01)
        assertEquals(30.5, recipe.proteinPerServingG, 0.01)
        assertEquals(35.0, recipe.carbsPerServingG, 0.01)
        assertEquals(4.0, recipe.fatPerServingG, 0.01)
    }

    @Test
    fun nutritionScreen_rendersGroupedMealsAndSwitchesToHistoryAndRecipesTabs() {
        val today = todayDateKey()
        val meals = listOf(
            MealLog(
                id = "m_b",
                dateKey = today,
                mealType = "breakfast",
                foodName = "Greek Yogurt Parfait",
                grams = 250.0,
                calories = 280.0,
                proteinG = 24.0,
                carbsG = 30.0,
                fatG = 6.0
            ),
            MealLog(
                id = "m_l",
                dateKey = today,
                mealType = "lunch",
                foodName = "Chicken Rice Bowl",
                grams = 400.0,
                calories = 540.0,
                proteinG = 46.0,
                carbsG = 58.0,
                fatG = 12.0
            )
        )

        composeRule.setContent {
            FitTrackTheme(themePreference = "light") {
                NutritionScreen(
                    profile = UserProfile(dailyCalorieTarget = 2200L, dailyWaterTargetMl = 2500L),
                    meals = meals,
                    waterLogs = emptyList(),
                    customFoods = emptyList(),
                    recentFoods = emptyList(),
                    savedRecipes = emptyList(),
                    plannedMeals = emptyList(),
                    nutritionPreferences = NutritionPreferences(),
                    onLogFood = { _, _, _ -> },
                    onUpdateMealGrams = { _, _ -> },
                    onDeleteMeal = {},
                    onSaveCustomFood = {},
                    onToggleFavorite = {},
                    onAddWater = {},
                    onOpenAiScanner = {},
                    onOpenAiMealPlanner = {}
                )
            }
        }

        // Verify Breakfast and Lunch meal group summaries appear in Today view
        composeRule.onNodeWithTag("nutrition_today_list")
            .performScrollToNode(hasTestTag("meal_group_breakfast"))
        composeRule.onNodeWithText("Greek Yogurt Parfait").assertIsDisplayed()
        composeRule.onNodeWithText("280 kcal • P 24g / C 30g / F 6g").assertIsDisplayed()

        // Switch to History & Reports sub-tab
        composeRule.onNodeWithText("History & Reports").performClick()
        composeRule.onNodeWithText("7-Day Nutrition Report").assertIsDisplayed()

        // Switch to Recipes & Plan sub-tab
        composeRule.onNodeWithText("Recipes & Plan").performClick()
        composeRule.onNodeWithText("Dietary Preferences").assertIsDisplayed()
        composeRule.onNodeWithText("My Recipes (0)").assertIsDisplayed()
    }

    @Test
    fun aiFoodScannerScreen_allowsEditingScannedFoodValuesBeforeConfirmingLog() {
        var confirmedMeal: MealLog? = null
        val scanResult = FoodScanResult(
            mealSummary = "Salmon Plate",
            items = listOf(
                ScannedFoodItem(
                    name = "Grilled Salmon",
                    estimatedGrams = 180.0,
                    calories = 360.0,
                    proteinG = 38.0,
                    carbsG = 0.0,
                    fatG = 22.0,
                    fiberG = 0.0,
                    sugarG = 0.0,
                    confidenceNote = "High confidence visual estimate"
                )
            ),
            uncertaintyNotice = "Portion weights are estimates."
        )

        composeRule.setContent {
            FitTrackTheme(themePreference = "dark") {
                AiFoodScannerScreen(
                    scanState = AsyncState.Success(scanResult),
                    onScanBitmap = {},
                    onConfirmLogMeal = { confirmedMeal = it },
                    onReset = {},
                    onBack = {}
                )
            }
        }

        composeRule.onNodeWithText("Grilled Salmon").performTextReplacement("Wild Sockeye Salmon")
        composeRule.onNodeWithText("360").performTextReplacement("410")
        composeRule.onNodeWithText("Confirm & Log Item").performScrollTo().performClick()

        assertTrue(confirmedMeal != null)
        assertEquals("Wild Sockeye Salmon", confirmedMeal?.foodName)
        assertEquals(410.0, confirmedMeal?.calories ?: 0.0, 0.01)
    }

    @Test
    fun profileScreen_enforcesMinorWeightLossSafeguardWhenSavingUnder18() {
        var savedGoal = ""
        var savedName = ""
        val minorProfile = UserProfile(
            displayName = "Alex Teen",
            ageYears = 16L,
            sex = "male",
            heightCm = 170.0,
            weightKg = 64.0,
            targetWeightKg = 64.0,
            goal = "weight_loss"
        )

        composeRule.setContent {
            FitTrackTheme(themePreference = "dark") {
                ProfileScreen(
                    profile = minorProfile,
                    onSavePersonalInfoAndGoals = { name, _, _, _, _, _, _, _, _, _, goal ->
                        savedName = name
                        savedGoal = goal
                    },
                    onOpenBodyProgress = {},
                    onOpenCalculators = {},
                    onOpenChallenges = {},
                    onOpenAiCoach = {},
                    onOpenSettings = {}
                )
            }
        }

        composeRule.onNodeWithTag("profile_list")
            .performScrollToNode(hasTestTag("save_profile_goals_button"))
        composeRule.onNodeWithTag("save_profile_goals_button").performScrollTo().performClick()
        assertEquals("Alex Teen", savedName)
        assertEquals("weight_maintenance", savedGoal)
    }
}
