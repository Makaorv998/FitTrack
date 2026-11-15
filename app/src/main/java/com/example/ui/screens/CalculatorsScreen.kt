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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.UserProfile
import com.example.domain.FitnessCalculator
import com.example.ui.components.AuroraAmbientBackground
import com.example.ui.components.DisclaimerBanner
import com.example.ui.components.FitTrackCard
import com.example.ui.components.GlassCard
import com.example.ui.components.StatusBadge
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CalculatorsScreen(
    profile: UserProfile?,
    onBack: () -> Unit
) {
    var weightText by remember(profile?.weightKg) { mutableStateOf((profile?.weightKg ?: 72.0).toString()) }
    var heightText by remember(profile?.heightCm) { mutableStateOf((profile?.heightCm ?: 175.0).toString()) }
    var ageText by remember(profile?.ageYears) { mutableStateOf((profile?.ageYears ?: 26L).toString()) }
    var targetWeightText by remember(profile?.targetWeightKg) { mutableStateOf((profile?.targetWeightKg ?: 70.0).toString()) }
    var sex by remember(profile?.sex) { mutableStateOf(profile?.sex ?: "male") }
    var activityLevel by remember(profile?.activityLevel) { mutableStateOf(profile?.activityLevel ?: "moderate") }
    var goal by remember(profile?.goal) { mutableStateOf(profile?.goal ?: "general_fitness") }

    var stepsText by remember { mutableStateOf("8500") }
    var runDistText by remember { mutableStateOf("5.0") }
    var runMinText by remember { mutableStateOf("27.5") }
    var metText by remember { mutableStateOf("8.0") }
    var metMinText by remember { mutableStateOf("40") }

    val w = (weightText.toDoubleOrNull() ?: 72.0).coerceIn(25.0, 400.0)
    val h = (heightText.toDoubleOrNull() ?: 175.0).coerceIn(80.0, 260.0)
    val age = (ageText.toIntOrNull() ?: 26).coerceIn(13, 120)
    val tw = (targetWeightText.toDoubleOrNull() ?: 70.0).coerceIn(25.0, 400.0)
    val steps = (stepsText.toLongOrNull() ?: 8500L).coerceAtLeast(0L)
    val runDist = (runDistText.toDoubleOrNull() ?: 5.0).coerceAtLeast(0.1)
    val runMin = (runMinText.toDoubleOrNull() ?: 27.5).coerceAtLeast(1.0)
    val met = (metText.toDoubleOrNull() ?: 8.0).coerceIn(1.0, 20.0)
    val metMin = (metMinText.toDoubleOrNull() ?: 40.0).coerceAtLeast(1.0)

    val bmi = FitnessCalculator.calculateBmi(w, h)
    val bmrResult = FitnessCalculator.calculateBmrResult(w, h, age, sex)
    val tdeeResult = FitnessCalculator.calculateTdeeResult(w, h, age, sex, activityLevel)
    val (calorieTarget, calNote) = FitnessCalculator.calculateDailyCalorieTarget(w, h, age, sex, activityLevel, goal)
    val macros = FitnessCalculator.calculateMacros(calorieTarget, w, goal, age)
    val metCalories = FitnessCalculator.calculateMetCalories(met, w, metMin)
    val walkKm = FitnessCalculator.calculateWalkingDistanceKm(steps, h)
    val runPace = FitnessCalculator.calculateRunningPaceMinPerKm(runMin, runDist)
    val waterMl = FitnessCalculator.calculateWaterTargetMl(w, metMin.roundToInt())
    val tdeeVal = FitnessCalculator.calculateTdee(FitnessCalculator.calculateBmrMifflinStJeor(w, h, age, sex), activityLevel)
    val weightProj = FitnessCalculator.calculateWeightProgressResult(w, tw, (calorieTarget - tdeeVal).roundToInt(), age)

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
                        OutlinedTextField(value = weightText, onValueChange = { weightText = it }, label = { Text("Weight kg") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
                        OutlinedTextField(value = heightText, onValueChange = { heightText = it }, label = { Text("Height cm") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
                        OutlinedTextField(value = ageText, onValueChange = { ageText = it.filter(Char::isDigit) }, label = { Text("Age") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(0.8f))
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
                        listOf("weight_loss" to "Loss", "general_fitness" to "Fitness", "muscle_gain" to "Muscle").forEach { (k, l) ->
                            FilterChip(selected = goal == k, onClick = { goal = k }, label = { Text(l) })
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = stepsText, onValueChange = { stepsText = it.filter(Char::isDigit) }, label = { Text("Steps") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                        OutlinedTextField(value = targetWeightText, onValueChange = { targetWeightText = it }, label = { Text("Goal kg") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = runDistText, onValueChange = { runDistText = it }, label = { Text("Run km") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
                        OutlinedTextField(value = runMinText, onValueChange = { runMinText = it }, label = { Text("Run min") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
                        OutlinedTextField(value = metText, onValueChange = { metText = it }, label = { Text("MET") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(0.8f))
                        OutlinedTextField(value = metMinText, onValueChange = { metMinText = it }, label = { Text("Min") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(0.8f))
                    }
                }

                CalculatorResultCard(bmi.title, bmi.primaryValue, bmi.categoryOrBadge, bmi.formulaUsed, bmi.assumptions, bmi.safetyNote)
                CalculatorResultCard(bmrResult.title, bmrResult.primaryValue, bmrResult.categoryOrBadge, bmrResult.formulaUsed, bmrResult.assumptions, bmrResult.safetyNote)
                CalculatorResultCard(tdeeResult.title, tdeeResult.primaryValue, tdeeResult.categoryOrBadge, tdeeResult.formulaUsed, tdeeResult.assumptions, tdeeResult.safetyNote)
                CalculatorResultCard(
                    "4. Daily Calorie Target",
                    "$calorieTarget kcal/day",
                    goal.replace("_", " ").uppercase(),
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
                    String.format(Locale.US, "%.2f km", walkKm),
                    "$steps steps",
                    "steps * (heightCm * 0.415 / 100) / 1000",
                    "Height-proportional stride length estimation."
                )
                CalculatorResultCard(
                    "8. Running Pace",
                    FitnessCalculator.formatPace(runPace),
                    String.format(Locale.US, "%.1f km/h avg", (runDist / (runMin / 60.0))),
                    "paceMinPerKm = durationMinutes / distanceKm",
                    "Average split pace across flat distance."
                )
                CalculatorResultCard(
                    "9. Daily Hydration & Weight Projection",
                    "$waterMl ml/day • ${weightProj.primaryValue}",
                    weightProj.categoryOrBadge,
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
