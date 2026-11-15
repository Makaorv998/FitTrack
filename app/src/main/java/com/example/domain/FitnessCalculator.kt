package com.example.domain

import kotlin.math.abs
import kotlin.math.roundToInt

data class CalculationResult(
    val title: String,
    val primaryValue: String,
    val categoryOrBadge: String,
    val formulaUsed: String,
    val assumptions: String,
    val safetyNote: String? = null
)

data class MacroBreakdown(
    val calories: Int,
    val proteinGrams: Int,
    val carbsGrams: Int,
    val fatGrams: Int,
    val fiberGrams: Int,
    val safetyNote: String?
)

object FitnessCalculator {

    fun isMinor(ageYears: Int): Boolean = ageYears < 18

    fun calculateBmi(weightKg: Double, heightCm: Double): CalculationResult {
        val heightM = (heightCm / 100.0).coerceAtLeast(0.5)
        val bmi = weightKg / (heightM * heightM)
        val category = when {
            bmi < 18.5 -> "Underweight"
            bmi < 25.0 -> "Normal range"
            bmi < 30.0 -> "Overweight"
            else -> "Obese range"
        }
        return CalculationResult(
            title = "Body Mass Index (BMI)",
            primaryValue = String.format("%.1f kg/m²", bmi),
            categoryOrBadge = category,
            formulaUsed = "BMI = weightKg / (heightM * heightM)",
            assumptions = "Estimate based on height and total body weight; does not distinguish lean muscle mass from body fat."
        )
    }

    fun calculateBmrMifflinStJeor(
        weightKg: Double,
        heightCm: Double,
        ageYears: Int,
        sex: String
    ): Double {
        val base = 10.0 * weightKg + 6.25 * heightCm - 5.0 * ageYears
        return when (sex.lowercase()) {
            "male" -> base + 5.0
            "female" -> base - 161.0
            else -> base - 78.0
        }
    }

    fun calculateBmrResult(
        weightKg: Double,
        heightCm: Double,
        ageYears: Int,
        sex: String
    ): CalculationResult {
        val bmr = calculateBmrMifflinStJeor(weightKg, heightCm, ageYears, sex)
        val sexLabel = when (sex.lowercase()) {
            "male" -> "Male (+5)"
            "female" -> "Female (-161)"
            else -> "Neutral midpoint (-78)"
        }
        return CalculationResult(
            title = "Basal Metabolic Rate (BMR)",
            primaryValue = "${bmr.roundToInt()} kcal/day",
            categoryOrBadge = sexLabel,
            formulaUsed = "10*weightKg + 6.25*heightCm - 5*age + sexOffset",
            assumptions = "Mifflin-St Jeor resting energy expenditure estimate assuming average body composition."
        )
    }

    fun activityFactor(activityLevel: String): Double = when (activityLevel.lowercase()) {
        "sedentary" -> 1.2
        "light" -> 1.375
        "moderate" -> 1.55
        "active" -> 1.725
        "very_active" -> 1.9
        else -> 1.55
    }

    fun calculateTdee(bmr: Double, activityLevel: String): Double {
        return bmr * activityFactor(activityLevel)
    }

    fun calculateTdeeResult(
        weightKg: Double,
        heightCm: Double,
        ageYears: Int,
        sex: String,
        activityLevel: String
    ): CalculationResult {
        val bmr = calculateBmrMifflinStJeor(weightKg, heightCm, ageYears, sex)
        val factor = activityFactor(activityLevel)
        val tdee = bmr * factor
        return CalculationResult(
            title = "Total Daily Energy Expenditure (TDEE)",
            primaryValue = "${tdee.roundToInt()} kcal/day",
            categoryOrBadge = "Factor ${factor}x",
            formulaUsed = "TDEE = BMR * activityFactor",
            assumptions = "Activity factor ($activityLevel = $factor) estimates daily movement and exercise energy expenditure."
        )
    }

    fun calculateDailyCalorieTarget(
        weightKg: Double,
        heightCm: Double,
        ageYears: Int,
        sex: String,
        activityLevel: String,
        goal: String
    ): Pair<Int, String?> {
        val bmr = calculateBmrMifflinStJeor(weightKg, heightCm, ageYears, sex)
        val tdee = calculateTdee(bmr, activityLevel)
        if (isMinor(ageYears)) {
            val safeTarget = tdee.roundToInt().coerceAtLeast(1600)
            return safeTarget to "Minor Safeguard: Automated weight-loss calorie deficits are disabled under age 18. Target reflects estimated maintenance energy."
        }
        val adjusted = when (goal.lowercase()) {
            "weight_loss" -> tdee - 450.0
            "muscle_gain" -> tdee + 300.0
            "weight_maintenance", "general_fitness" -> tdee
            else -> tdee
        }
        val floor = if (sex.lowercase() == "female") 1200.0 else 1500.0
        val finalCalories = adjusted.coerceAtLeast(floor).roundToInt()
        val note = if (adjusted < floor) {
            "Safety floor applied (${floor.roundToInt()} kcal minimum) to avoid overly aggressive restriction."
        } else {
            null
        }
        return finalCalories to note
    }

    fun calculateMacros(
        calorieTarget: Int,
        weightKg: Double,
        goal: String,
        ageYears: Int = 25
    ): MacroBreakdown {
        val safeGoal = if (isMinor(ageYears) && goal == "weight_loss") "general_fitness" else goal
        val proteinPerKg = when (safeGoal.lowercase()) {
            "muscle_gain" -> 1.8
            "weight_loss" -> 1.8
            else -> 1.5
        }
        val proteinG = (weightKg * proteinPerKg).roundToInt().coerceIn(50, 260)
        val fatRatio = if (safeGoal.lowercase() == "muscle_gain") 0.25 else 0.28
        val fatG = ((calorieTarget * fatRatio) / 9.0).roundToInt().coerceIn(35, 140)
        val remainingCals = (calorieTarget - (proteinG * 4) - (fatG * 9)).coerceAtLeast(200)
        val carbsG = (remainingCals / 4.0).roundToInt()
        val fiberG = ((calorieTarget / 1000.0) * 14.0).roundToInt().coerceIn(20, 45)
        val note = if (isMinor(ageYears)) {
            "Balanced growth-supportive macro split; consult a pediatrician or registered dietitian for individualized youth nutrition."
        } else {
            null
        }
        return MacroBreakdown(
            calories = calorieTarget,
            proteinGrams = proteinG,
            carbsGrams = carbsG,
            fatGrams = fatG,
            fiberGrams = fiberG,
            safetyNote = note
        )
    }

    fun calculateMetCalories(met: Double, weightKg: Double, durationMinutes: Double): Double {
        return ((met * 3.5 * weightKg) / 200.0) * durationMinutes.coerceAtLeast(0.0)
    }

    fun calculateWalkingDistanceKm(steps: Long, heightCm: Double): Double {
        val strideLengthMeters = (heightCm * 0.415) / 100.0
        return (steps.coerceAtLeast(0L) * strideLengthMeters) / 1000.0
    }

    fun calculateRunningPaceMinPerKm(durationMinutes: Double, distanceKm: Double): Double {
        if (distanceKm <= 0.001) return 0.0
        return durationMinutes / distanceKm
    }

    fun calculateWaterTargetMl(weightKg: Double, activityMinutes: Int = 30): Int {
        val baseMl = weightKg * 35.0
        val activityBonus = (activityMinutes / 30.0) * 350.0
        return (baseMl + activityBonus).roundToInt().coerceIn(1500, 6000)
    }

    fun calculateWeightProgressResult(
        currentKg: Double,
        targetKg: Double,
        dailyCalorieDelta: Int,
        ageYears: Int
    ): CalculationResult {
        if (isMinor(ageYears) && targetKg < currentKg) {
            return CalculationResult(
                title = "Weight Progress Estimate",
                primaryValue = "Disabled under 18",
                categoryOrBadge = "Safety Guardrail",
                formulaUsed = "7,700 kcal ≈ 1 kg body weight change",
                assumptions = "Growth and development vary widely under age 18.",
                safetyNote = "Automated weight-loss projections are disabled for users under 18."
            )
        }
        val diffKg = abs(currentKg - targetKg)
        val absDelta = abs(dailyCalorieDelta).coerceAtLeast(100)
        val weeklyChangeKg = (absDelta * 7.0) / 7700.0
        val weeks = if (diffKg < 0.1) 0.0 else diffKg / weeklyChangeKg
        val direction = when {
            targetKg < currentKg -> "loss"
            targetKg > currentKg -> "gain"
            else -> "maintenance"
        }
        return CalculationResult(
            title = "Weight Progress Estimate",
            primaryValue = if (direction == "maintenance") "At Target" else String.format("~%.1f weeks", weeks),
            categoryOrBadge = String.format("%.2f kg/week %s", weeklyChangeKg, direction),
            formulaUsed = "weeks = |currentKg - targetKg| / ((|dailyDelta| * 7) / 7700)",
            assumptions = "Assumes 1 kg body tissue ≈ 7,700 kcal and steady adherence; real metabolic adaptation varies."
        )
    }

    fun formatPace(paceMinPerKm: Double): String {
        if (paceMinPerKm <= 0.0 || paceMinPerKm > 99.0) return "--:-- /km"
        val mins = paceMinPerKm.toInt()
        val secs = ((paceMinPerKm - mins) * 60.0).roundToInt().coerceIn(0, 59)
        return String.format("%d:%02d /km", mins, secs)
    }

    fun formatDuration(seconds: Long): String {
        val hrs = seconds / 3600
        val mins = (seconds % 3600) / 60
        val secs = seconds % 60
        return if (hrs > 0) {
            String.format("%d:%02d:%02d", hrs, mins, secs)
        } else {
            String.format("%02d:%02d", mins, secs)
        }
    }
}
