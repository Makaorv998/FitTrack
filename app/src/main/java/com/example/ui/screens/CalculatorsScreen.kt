package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.UserProfile
import com.example.domain.FitnessCalculator
import com.example.domain.UnitFormatter
import com.example.ui.components.AuroraAmbientBackground
import com.example.ui.components.DisclaimerBanner
import com.example.ui.components.FitTrackCard
import com.example.ui.components.GlassCard
import com.example.ui.components.StatusBadge
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CalculatorsScreen(
    profile: UserProfile?,
    onBack: () -> Unit
) {
    val unitSystem = profile?.unitSystem ?: "metric"
    val isImperial = UnitFormatter.isImperial(unitSystem)
    val initialWeightDisplay = remember(profile?.weightKg, unitSystem) {
        String.format(Locale.US, "%.1f", UnitFormatter.kgToDisplayWeight(profile?.weightKg ?: 72.0, unitSystem))
    }
    val initialHeightDisplay = remember(profile?.heightCm, unitSystem) {
        val converted = UnitFormatter.cmToDisplayHeight(profile?.heightCm ?: 175.0, unitSystem)
        if (isImperial) String.format(Locale.US, "%.1f", converted) else String.format(Locale.US, "%.0f", converted)
    }
    val initialTargetWeightDisplay = remember(profile?.targetWeightKg, unitSystem) {
        String.format(Locale.US, "%.1f", UnitFormatter.kgToDisplayWeight(profile?.targetWeightKg ?: 70.0, unitSystem))
    }
    val effectiveInitialAge = remember(profile?.ageYears, profile?.dateOfBirthIso) {
        FitnessCalculator.resolveAgeYears(profile?.ageYears ?: 26L, profile?.dateOfBirthIso.orEmpty())
    }

    var weightText by remember(initialWeightDisplay) { mutableStateOf(initialWeightDisplay) }
    var heightText by remember(initialHeightDisplay) { mutableStateOf(initialHeightDisplay) }
    var ageText by remember(effectiveInitialAge) { mutableStateOf(effectiveInitialAge.toString()) }
    var targetWeightText by remember(initialTargetWeightDisplay) { mutableStateOf(initialTargetWeightDisplay) }
    var sex by remember(profile?.sex) { mutableStateOf(profile?.sex ?: "male") }
    var activityLevel by remember(profile?.activityLevel) { mutableStateOf(profile?.activityLevel ?: "moderate") }
    var goal by remember(profile?.goal) { mutableStateOf(profile?.goal ?: "general_fitness") }

    var stepsText by remember { mutableStateOf("8500") }
    var runDistText by remember(unitSystem) { mutableStateOf(if (isImperial) "3.1" else "5.0") }
    var runMinText by remember { mutableStateOf("27.5") }
    var metText by remember { mutableStateOf("8.0") }
    var metMinText by remember { mutableStateOf("40") }

    val w = UnitFormatter.inputWeightToKg(
        weightText.toDoubleOrNull() ?: UnitFormatter.kgToDisplayWeight(72.0, unitSystem),
        unitSystem
    ).coerceIn(25.0, 400.0)
    val h = UnitFormatter.inputHeightToCm(
        heightText.toDoubleOrNull() ?: UnitFormatter.cmToDisplayHeight(175.0, unitSystem),
        unitSystem
    ).coerceIn(80.0, 260.0)
    val age = (ageText.toIntOrNull() ?: effectiveInitialAge).coerceIn(13, 120)
    val isMinor = FitnessCalculator.isMinor(age)
    val effectiveGoal = if (isMinor && goal == "weight_loss") "general_fitness" else goal
    val tw = UnitFormatter.inputWeightToKg(
        targetWeightText.toDoubleOrNull() ?: UnitFormatter.kgToDisplayWeight(70.0, unitSystem),
        unitSystem
    ).coerceIn(25.0, 400.0)
    val steps = (stepsText.toLongOrNull() ?: 8500L).coerceAtLeast(0L)
    val runDistKm = UnitFormatter.inputDistanceToKm(
        runDistText.toDoubleOrNull() ?: (if (isImperial) 3.1 else 5.0),
        unitSystem
    ).coerceAtLeast(0.1)
    val runMin = (runMinText.toDoubleOrNull() ?: 27.5).coerceAtLeast(1.0)
    val met = (metText.toDoubleOrNull() ?: 8.0).coerceIn(1.0, 20.0)
    val metMin = (metMinText.toDoubleOrNull() ?: 40.0).coerceAtLeast(1.0)

    val bmi = FitnessCalculator.calculateBmi(w, h)
    val bmrResult = FitnessCalculator.calculateBmrResult(w, h, age, sex)
    val tdeeResult = FitnessCalculator.calculateTdeeResult(w, h, age, sex, activityLevel)
    val (calorieTarget, calNote) = FitnessCalculator.calculateDailyCalorieTarget(w, h, age, sex, activityLevel, effectiveGoal)
    val macros = FitnessCalculator.calculateMacros(calorieTarget, w, effectiveGoal, age)
    val metCalories = FitnessCalculator.calculateMetCalories(met, w, metMin)
    val walkKm = FitnessCalculator.calculateWalkingDistanceKm(steps, h)
    val runPaceMinPerKm = FitnessCalculator.calculateRunningPaceMinPerKm(runMin, runDistKm)
    val waterMl = FitnessCalculator.calculateWaterTargetMl(w, metMin.roundToInt())
    val tdeeVal = FitnessCalculator.calculateTdee(FitnessCalculator.calculateBmrMifflinStJeor(w, h, age, sex), activityLevel)
    val weightProj = FitnessCalculator.calculateWeightProgressResult(w, tw, (calorieTarget - tdeeVal).roundToInt(), age)
    val weightProjBadge = if (isMinor && tw < w) {
        weightProj.categoryOrBadge
    } else {
        val absDelta = abs((calorieTarget - tdeeVal).roundToInt()).coerceAtLeast(100)
        val weeklyChangeKg = (absDelta * 7.0) / 7700.0
        val direction = when {
            tw < w -> "loss"
            tw > w -> "gain"
            else -> "maintenance"
        }
        "${UnitFormatter.formatWeight(weeklyChangeKg, unitSystem, decimals = 2)}/week $direction"
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.85f)),
                title = { Text("9 Fitness Calculators") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        AuroraAmbientBackground {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text("Shared Biometric Inputs", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = weightText,
                            onValueChange = { weightText = it },
                            label = { Text("Weight ${UnitFormatter.weightUnitLabel(unitSystem)}") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("calc_weight_input")
                        )
                        OutlinedTextField(
                            value = heightText,
                            onValueChange = { heightText = it },
                            label = { Text("Height ${UnitFormatter.heightInputLabel(unitSystem)}") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("calc_height_input")
                        )
                        OutlinedTextField(
                            value = ageText,
                            onValueChange = { ageText = it.filter(Char::isDigit) },
                            label = { Text("Age") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .weight(0.8f)
                                .testTag("calc_age_input")
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("male" to "Male", "female" to "Female").forEach { (k, l) ->
                            FilterChip(selected = sex == k, onClick = { sex = k }, label = { Text(l) })
                        }
                        listOf("sedentary" to "Sedentary", "light" to "Light", "moderate" to "Moderate", "active" to "Active").forEach { (k, l) ->
                            FilterChip(selected = activityLevel == k, onClick = { activityLevel = k }, label = { Text(l) })
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val goalOptions = if (isMinor) {
                            listOf("general_fitness" to "Fitness", "muscle_gain" to "Muscle")
                        } else {
                            listOf("weight_loss" to "Loss", "general_fitness" to "Fitness", "muscle_gain" to "Muscle")
                        }
                        goalOptions.forEach { (k, l) ->
                            FilterChip(selected = effectiveGoal == k, onClick = { goal = k }, label = { Text(l) })
                        }
                    }
                    if (isMinor) {
                        Spacer(modifier = Modifier.height(8.dp))
                        DisclaimerBanner(
                            text = "Under-18 Safety Mode: Weight-loss calorie deficits and timeline projections are disabled.",
                            isWarning = true
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = stepsText,
                            onValueChange = { stepsText = it.filter(Char::isDigit) },
                            label = { Text("Steps") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = targetWeightText,
                            onValueChange = { targetWeightText = it },
                            label = { Text("Goal ${UnitFormatter.weightUnitLabel(unitSystem)}") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("calc_target_weight_input")
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = runDistText,
                            onValueChange = { runDistText = it },
                            label = { Text("Run ${UnitFormatter.distanceUnitLabel(unitSystem)}") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("calc_run_dist_input")
                        )
                        OutlinedTextField(
                            value = runMinText,
                            onValueChange = { runMinText = it },
                            label = { Text("Run min") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = metText,
                            onValueChange = { metText = it },
                            label = { Text("MET") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(0.8f)
                        )
                        OutlinedTextField(
                            value = metMinText,
                            onValueChange = { metMinText = it },
                            label = { Text("Min") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(0.8f)
                        )
                    }
                }

                CalculatorResultCard(bmi.title, bmi.primaryValue, bmi.categoryOrBadge, bmi.formulaUsed, bmi.assumptions, bmi.safetyNote)
                CalculatorResultCard(bmrResult.title, bmrResult.primaryValue, bmrResult.categoryOrBadge, bmrResult.formulaUsed, bmrResult.assumptions, bmrResult.safetyNote)
                CalculatorResultCard(tdeeResult.title, tdeeResult.primaryValue, tdeeResult.categoryOrBadge, tdeeResult.formulaUsed, tdeeResult.assumptions, tdeeResult.safetyNote)
                CalculatorResultCard(
                    "4. Daily Calorie Target",
                    "$calorieTarget kcal/day",
                    effectiveGoal.replace("_", " ").uppercase(),
                    "TDEE + goal adjustment (-450 loss / +300 gain)",
                    "Maintains safe minimum energy floor (1200/1500 kcal adults; maintenance for <18).",
                    calNote
                )
                CalculatorResultCard(
                    "5. Macro Split (Protein / Carbs / Fat)",
                    "P ${macros.proteinGrams}g • C ${macros.carbsGrams}g • F ${macros.fatGrams}g",
                    "Fiber ${macros.fiberGrams}g/day",
                    "Protein g/kg + 25-28% Fat + remaining Carbs",
                    "Balanced macro target scaled to body mass and daily calorie goal.",
                    macros.safetyNote
                )
                CalculatorResultCard(
                    "6. MET Calorie Burn",
                    "${metCalories.roundToInt()} kcal",
                    "MET $met for ${metMin.roundToInt()} min",
                    "(MET * 3.5 * weightKg / 200) * durationMinutes",
                    "Compendium of Physical Activities metabolic equivalent formula."
                )
                CalculatorResultCard(
                    "7. Walking Distance from Steps",
                    UnitFormatter.formatDistance(walkKm, unitSystem),
                    "$steps steps",
                    "steps * (heightCm * 0.415 / 100) / 1000",
                    "Height-proportional stride length estimation."
                )
                CalculatorResultCard(
                    "8. Running Pace",
                    UnitFormatter.formatPace(runPaceMinPerKm, unitSystem),
                    "${UnitFormatter.formatSpeed(runDistKm / (runMin / 60.0), unitSystem)} avg",
                    "pace = durationMinutes / distance",
                    "Average split pace across flat distance."
                )
                CalculatorResultCard(
                    "9. Daily Hydration & Weight Projection",
                    "${UnitFormatter.formatWater(waterMl.toLong(), unitSystem)}/day • ${weightProj.primaryValue}",
                    weightProjBadge,
                    "35ml/kg + activity bonus | ${weightProj.formulaUsed}",
                    weightProj.assumptions,
                    weightProj.safetyNote
                )
            }
        }
    }
}

@Composable
private fun CalculatorResultCard(
    title: String,
    primaryValue: String,
    badge: String,
    formula: String,
    assumptions: String,
    safetyNote: String? = null
) {
    FitTrackCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            StatusBadge(text = badge, tint = MaterialTheme.colorScheme.primary)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(primaryValue, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.secondary)
        Spacer(modifier = Modifier.height(6.dp))
        Text("Formula: $formula", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(assumptions, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (!safetyNote.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            DisclaimerBanner(text = safetyNote, isWarning = true)
        }
    }
}
