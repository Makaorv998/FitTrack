package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.example.data.ai.FitTrackAiService
import com.example.data.catalog.FitnessCatalog
import com.example.data.model.AiChatMessage
import com.example.data.model.MealLog
import com.example.data.model.UserProfile
import com.example.data.model.WaterLog
import com.example.platform.RecentFoodEntry
import com.example.platform.todayDateKey
import com.example.ui.AsyncState
import com.example.ui.screens.AiCoachScreen
import com.example.ui.screens.NutritionScreen
import com.example.ui.theme.FitTrackTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class NutritionAndAiSafetyTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun nutritionCatalogAndRecentFood_preserveMilliliterMeasurementUnits() {
        val milk = FitnessCatalog.builtInFoods.first { it.id == "food_milk" }
        assertEquals("ml", milk.measurementUnit)

        val recent = RecentFoodEntry(
            foodName = "Skim Milk",
            lastGrams = 250.0,
            measurementUnit = "ml"
        )
        val food = recent.toFoodItem()
        assertEquals("ml", food.measurementUnit)
        assertEquals("250ml", food.servingLabel)
    }

    @Test
    fun nutritionScreen_displaysImperialHydrationAndMilliliterMeals() {
        val today = todayDateKey()
        val imperialProfile = UserProfile(
            unitSystem = "imperial",
            dailyCalorieTarget = 2200L,
            dailyWaterTargetMl = 2500L
        )
        val meals = listOf(
            MealLog(
                id = "m_milk",
                dateKey = today,
                mealType = "breakfast",
                foodName = "Skim Milk",
                grams = 245.0,
                measurementUnit = "ml",
                calories = 83.0,
                proteinG = 8.0,
                carbsG = 12.0,
                fatG = 0.2
            )
        )
        val waterLogs = listOf(
            WaterLog(id = "w1", dateKey = today, amountMl = 473L)
        )
        var addedWater = 0L

        composeRule.setContent {
            FitTrackTheme(themePreference = "dark") {
                NutritionScreen(
                    profile = imperialProfile,
                    meals = meals,
                    waterLogs = waterLogs,
                    customFoods = emptyList(),
                    onLogFood = { _, _, _ -> },
                    onUpdateMealGrams = { _, _ -> },
                    onDeleteMeal = {},
                    onSaveCustomFood = {},
                    onToggleFavorite = {},
                    onAddWater = { addedWater = it },
                    onOpenAiScanner = {},
                    onOpenAiMealPlanner = {}
                )
            }
        }

        // Imperial hydration buttons and fl oz ratio
        composeRule.onNodeWithTag("nutrition_today_list")
            .performScrollToNode(hasText("16 / 85 fl oz"))
        composeRule.onNodeWithText("16 / 85 fl oz").assertIsDisplayed()
        composeRule.onNodeWithTag("nutrition_today_list")
            .performScrollToNode(hasText("+8 fl oz"))
        composeRule.onNodeWithText("+8 fl oz").performClick()
        assertEquals(237L, addedWater)

        // Milliliter unit preserved on logged breakfast drink
        composeRule.onNodeWithTag("nutrition_today_list")
            .performScrollToNode(hasText("245ml • 83 kcal • P 8g / C 12g / F 0g"))
        composeRule.onNodeWithText("245ml • 83 kcal • P 8g / C 12g / F 0g").assertIsDisplayed()
    }

    @Test
    fun aiCoachSafety_enforcesMinorGuardrailAndCalorieFloor() {
        val minor = UserProfile(
            displayName = "Teen Athlete",
            ageYears = 15L,
            goal = "weight_loss",
            weightKg = 60.0,
            targetWeightKg = 54.0,
            dailyCalorieTarget = 1400L,
            dailyWaterTargetMl = 2200L,
            unitSystem = "imperial"
        )

        val prompt = FitTrackAiService.buildCoachContextPrompt(
            userMessage = "How many calories should I cut?",
            profile = minor,
            todaySteps = 6000L,
            todayCaloriesIn = 1500.0,
            todayWaterMl = 1200L
        )

        assertTrue(prompt.contains("MINOR SAFETY GUARDRAIL"))
        assertTrue(prompt.contains("Goal: weight_maintenance"))
        assertFalse(prompt.contains("Target Weight:"))
        assertTrue(prompt.contains("1800 kcal"))
        assertEquals(1800, FitTrackAiService.safeMealPlanCalorieFloor(1400, isMinor = true))
        assertEquals(1400, FitTrackAiService.safeMealPlanCalorieFloor(1400, isMinor = false))
    }

    @Test
    fun aiCoachScreen_requiresConfirmationBeforeClearingHistory() {
        var cleared = false
        val history = listOf(
            AiChatMessage(id = "1", role = "user", text = "Suggest warm-up stretches"),
            AiChatMessage(id = "2", role = "model", text = "Try 5 minutes of light walking and leg swings.")
        )

        composeRule.setContent {
            FitTrackTheme(themePreference = "dark") {
                AiCoachScreen(
                    messages = history,
                    coachState = AsyncState.Idle,
                    onSendMessage = {},
                    onClearHistory = { cleared = true },
                    onBack = {}
                )
            }
        }

        composeRule.onNodeWithContentDescription("Clear chat history").performClick()
        composeRule.onNodeWithText("Clear AI Coach History?").assertIsDisplayed()
        assertFalse(cleared)

        composeRule.onNodeWithText("Clear History").performClick()
        assertTrue(cleared)
    }
}
