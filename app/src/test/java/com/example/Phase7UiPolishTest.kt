package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import com.example.data.catalog.FitnessCatalog
import com.example.data.model.UserChallenge
import com.example.data.model.UserProfile
import com.example.ui.screens.BodyProgressScreen
import com.example.ui.screens.CalculatorsScreen
import com.example.ui.screens.ChallengesScreen
import com.example.ui.screens.SettingsScreen
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
class Phase7UiPolishTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun settingsScreen_reducedMotionSwitchInvokesCallback() {
        var latestToggle: Boolean? = null
        composeRule.setContent {
            FitTrackTheme(themePreference = "dark") {
                SettingsScreen(
                    profile = UserProfile(
                        displayName = "Alex",
                        themePreference = "dark",
                        unitSystem = "metric",
                        reducedMotionEnabled = false
                    ),
                    onChangeTheme = {},
                    onChangeUnits = {},
                    onToggleReducedMotion = { latestToggle = it },
                    onUpdateConsent = { _, _ -> },
                    onExportJson = { "{}" },
                    onSignOut = {},
                    onDeleteAccountData = {},
                    onBack = {}
                )
            }
        }

        composeRule.onNodeWithTag("reduced_motion_switch").assertIsDisplayed().assertIsOff()
        composeRule.onNodeWithTag("reduced_motion_switch").performClick()
        assertEquals(true, latestToggle)
    }

    @Test
    fun bodyProgressScreen_convertsImperialWeightInputBackToKilograms() {
        var loggedKg: Double? = null
        composeRule.setContent {
            FitTrackTheme(themePreference = "dark") {
                BodyProgressScreen(
                    profile = UserProfile(
                        weightKg = 80.0,
                        targetWeightKg = 75.0,
                        heightCm = 180.0,
                        unitSystem = "imperial"
                    ),
                    weightRecords = emptyList(),
                    measurements = emptyList(),
                    stepRecords = emptyList(),
                    meals = emptyList(),
                    gpsActivities = emptyList(),
                    workoutSessions = emptyList(),
                    progressPhotos = emptyList(),
                    onLogWeight = { kg, _ -> loggedKg = kg },
                    onLogMeasurements = { _, _, _, _, _ -> },
                    onAddProgressPhoto = { _, _ -> },
                    onDeleteProgressPhoto = {},
                    onBack = {}
                )
            }
        }

        composeRule.onNodeWithText("Weight (lb)").assertIsDisplayed()
        composeRule.onNodeWithTag("progress_weight_input").performTextReplacement("220.5")
        composeRule.onNodeWithTag("log_weight_button").performClick()

        val recorded = loggedKg ?: error("Expected weight callback")
        assertTrue("Expected ~100.0 kg from 220.5 lb, got $recorded", kotlin.math.abs(recorded - 100.0) < 0.25)
    }

    @Test
    fun calculatorsScreen_honorsImperialLabelsAndMinorSafetyMode() {
        composeRule.setContent {
            FitTrackTheme(themePreference = "dark") {
                CalculatorsScreen(
                    profile = UserProfile(
                        weightKg = 65.0,
                        heightCm = 170.0,
                        ageYears = 16L,
                        targetWeightKg = 60.0,
                        unitSystem = "imperial",
                        goal = "weight_loss"
                    ),
                    onBack = {}
                )
            }
        }

        composeRule.onNodeWithText("Weight lb").assertIsDisplayed()
        composeRule.onNodeWithText("Height in").assertIsDisplayed()
        composeRule.onNodeWithText("Goal lb").assertIsDisplayed()
        composeRule.onNodeWithText("Run mi").assertIsDisplayed()
        composeRule.onNodeWithText("Under-18 Safety Mode: Weight-loss calorie deficits and timeline projections are disabled.").assertIsDisplayed()
    }

    @Test
    fun challengesScreen_waterCategoryIncrementsHydrationAndRespectsUnits() {
        var updatedChallenge: UserChallenge? = null
        var updatedValue: Double? = null
        var updatedJoined: Boolean? = null

        val waterChallenge = FitnessCatalog.defaultChallenges("uid_1").first { it.id == "ch_hydration_week" }

        composeRule.setContent {
            FitTrackTheme(themePreference = "dark") {
                ChallengesScreen(
                    uid = "uid_1",
                    challenges = listOf(waterChallenge),
                    onUpdateChallenge = { ch, value, joined ->
                        updatedChallenge = ch
                        updatedValue = value
                        updatedJoined = joined
                    },
                    onBack = {},
                    unitSystem = "metric"
                )
            }
        }

        composeRule.onNodeWithText("0 / 2500 ml").assertIsDisplayed()
        composeRule.onNodeWithTag("challenge_log_ch_hydration_week").performClick()

        assertEquals("ch_hydration_week", updatedChallenge?.id)
        assertEquals(250.0, updatedValue ?: 0.0, 0.01)
        assertEquals(true, updatedJoined)
    }
}
