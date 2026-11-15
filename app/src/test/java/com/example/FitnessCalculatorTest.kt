package com.example

import com.example.domain.FitnessCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FitnessCalculatorTest {

    @Test
    fun `bmi calculation matches standard formula`() {
        val result = FitnessCalculator.calculateBmi(weightKg = 70.0, heightCm = 175.0)
        // 70 / (1.75 * 1.75) = 22.857 -> 22.9
        assertTrue(result.primaryValue.startsWith("22.9"))
        assertEquals("Normal range", result.categoryOrBadge)
    }

    @Test
    fun `bmr mifflin st jeor differentiates male and female`() {
        val male = FitnessCalculator.calculateBmrMifflinStJeor(70.0, 175.0, 30, "male")
        val female = FitnessCalculator.calculateBmrMifflinStJeor(70.0, 175.0, 30, "female")
        // Male: 700 + 1093.75 - 150 + 5 = 1648.75
        // Female: 700 + 1093.75 - 150 - 161 = 1482.75
        assertEquals(1648.75, male, 0.01)
        assertEquals(1482.75, female, 0.01)
    }

    @Test
    fun `minor safeguard disables weight loss calorie deficit`() {
        val (calories, note) = FitnessCalculator.calculateDailyCalorieTarget(
            weightKg = 62.0,
            heightCm = 168.0,
            ageYears = 15,
            sex = "male",
            activityLevel = "moderate",
            goal = "weight_loss"
        )
        assertNotNull(note)
        assertTrue(note!!.contains("Minor Safeguard"))
        assertTrue(calories >= 1600)
    }

    @Test
    fun `met walking pace and water formulas return expected estimates`() {
        val metCals = FitnessCalculator.calculateMetCalories(met = 8.0, weightKg = 75.0, durationMinutes = 30.0)
        // (8 * 3.5 * 75 / 200) * 30 = 10.5 * 30 = 315.0
        assertEquals(315.0, metCals, 0.01)

        val walkKm = FitnessCalculator.calculateWalkingDistanceKm(steps = 10000L, heightCm = 170.0)
        // 10000 * (170 * 0.415 / 100) / 1000 = 7.055 km
        assertEquals(7.055, walkKm, 0.01)

        val pace = FitnessCalculator.calculateRunningPaceMinPerKm(durationMinutes = 25.0, distanceKm = 5.0)
        assertEquals(5.0, pace, 0.01)
        assertEquals("5:00 /km", FitnessCalculator.formatPace(pace))

        val water = FitnessCalculator.calculateWaterTargetMl(weightKg = 70.0, activityMinutes = 30)
        // 70 * 35 + 350 = 2800
        assertEquals(2800, water)
    }
}
