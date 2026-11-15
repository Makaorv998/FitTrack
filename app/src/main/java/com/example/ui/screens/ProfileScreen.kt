package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.UserProfile
import com.example.domain.FitnessCalculator
import com.example.platform.NutritionPreferences
import com.example.platform.TrainingPreferences
import com.example.ui.components.ActivityCard
import com.example.ui.components.AuroraAmbientBackground
import com.example.ui.components.DisclaimerBanner
import com.example.ui.components.FitTrackAvatar
import com.example.ui.components.FitTrackCard
import com.example.ui.components.GlassCard
import com.example.ui.components.GradientButton
import com.example.ui.components.ScreenHeader
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusBadge
import com.example.ui.theme.LocalFitTrackColors

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    profile: UserProfile?,
    trainingPreferences: TrainingPreferences = TrainingPreferences(),
    nutritionPreferences: NutritionPreferences = NutritionPreferences(),
    onSavePersonalInfoAndGoals: (
        displayName: String,
        ageYears: Long,
        sex: String,
        heightCm: Double,
        weightKg: Double,
        targetWeightKg: Double,
        activityLevel: String,
        stepGoal: Long,
        waterGoalMl: Long,
        calorieGoal: Long,
        goal: String
    ) -> Unit,
    onSaveTrainingPreferences: (TrainingPreferences) -> Unit = {},
    onSaveNutritionPreferences: (NutritionPreferences) -> Unit = {},
    onOpenBodyProgress: () -> Unit,
    onOpenCalculators: () -> Unit,
    onOpenChallenges: () -> Unit,
    onOpenAiCoach: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val ext = LocalFitTrackColors.current
    var displayName by remember(profile?.displayName) { mutableStateOf(profile?.displayName ?: "Athlete") }
    var ageText by remember(profile?.ageYears) { mutableStateOf((profile?.ageYears ?: 26L).toString()) }
    var selectedSex by remember(profile?.sex) { mutableStateOf(profile?.sex ?: "male") }
    var heightText by remember(profile?.heightCm) { mutableStateOf((profile?.heightCm ?: 175.0).toString()) }
    var weightText by remember(profile?.weightKg) { mutableStateOf((profile?.weightKg ?: 72.0).toString()) }
    var targetWeightText by remember(profile?.targetWeightKg) { mutableStateOf((profile?.targetWeightKg ?: 70.0).toString()) }
    var selectedActivityLevel by remember(profile?.activityLevel) { mutableStateOf(profile?.activityLevel ?: "moderate") }

    var stepGoalText by remember(profile?.dailyStepGoal) { mutableStateOf((profile?.dailyStepGoal ?: 10000L).toString()) }
    var waterGoalText by remember(profile?.dailyWaterTargetMl) { mutableStateOf((profile?.dailyWaterTargetMl ?: 2500L).toString()) }
    var calorieGoalText by remember(profile?.dailyCalorieTarget) { mutableStateOf((profile?.dailyCalorieTarget ?: 2200L).toString()) }
    var selectedGoal by remember(profile?.goal) { mutableStateOf(profile?.goal ?: "general_fitness") }

    var preferredSplit by remember(trainingPreferences.preferredSplit) { mutableStateOf(trainingPreferences.preferredSplit) }
    var targetDaysText by remember(trainingPreferences.targetDaysPerWeek) { mutableStateOf(trainingPreferences.targetDaysPerWeek.toString()) }
    var trainingExp by remember(trainingPreferences.experienceLevel) { mutableStateOf(trainingPreferences.experienceLevel) }
    var availableEquip by remember(trainingPreferences.availableEquipment) { mutableStateOf(trainingPreferences.availableEquipment) }

    var dietaryStyle by remember(nutritionPreferences.dietaryStyle) { mutableStateOf(nutritionPreferences.dietaryStyle) }
    var allergiesText by remember(nutritionPreferences.allergies) { mutableStateOf(nutritionPreferences.allergies) }
    var mealsPerDayText by remember(nutritionPreferences.mealsPerDay) { mutableStateOf(nutritionPreferences.mealsPerDay.toString()) }

    val ageYears = (ageText.toIntOrNull() ?: (profile?.ageYears ?: 26L).toInt()).coerceIn(13, 120)
    val isMinor = FitnessCalculator.isMinor(ageYears)
    val bmi = FitnessCalculator.calculateBmi(
        weightKg = weightText.toDoubleOrNull() ?: profile?.weightKg ?: 72.0,
        heightCm = heightText.toDoubleOrNull() ?: profile?.heightCm ?: 175.0
    )
    val bottomClearance = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 104.dp

    AuroraAmbientBackground {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("profile_list"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = bottomClearance),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                ScreenHeader(
                    title = "Profile",
                    subtitle = "Athlete details, training preferences & targets"
                )
            }

            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    accentBorder = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        FitTrackAvatar(
                            photoUrl = profile?.photoUrl,
                            displayName = profile?.displayName ?: "Athlete",
                            size = 64.dp
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = profile?.displayName ?: "Athlete",
                                style = MaterialTheme.typography.headlineMedium
                            )
                            if (!profile?.email.isNullOrBlank()) {
                                Text(
                                    text = profile?.email ?: "",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            StatusBadge(
                                text = (profile?.goal ?: "general_fitness").replace("_", " ").uppercase(),
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        BiometricBadge("Weight", "${profile?.weightKg ?: 72.0} kg")
                        BiometricBadge("Height", "${profile?.heightCm ?: 175.0} cm")
                        BiometricBadge("BMI", bmi.primaryValue)
                    }
                }
            }

            // Hub Shortcuts
            item {
                SectionHeader(title = "Fitness Hub & Tools")
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        HubTile(
                            icon = Icons.Filled.MonitorWeight,
                            title = "Body Progress",
                            subtitle = "Weight, tape & photos",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("hub_body_progress"),
                            onClick = onOpenBodyProgress
                        )
                        HubTile(
                            icon = Icons.Filled.Calculate,
                            title = "9 Calculators",
                            subtitle = "BMI, BMR, TDEE, Macros",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("hub_calculators"),
                            onClick = onOpenCalculators
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        HubTile(
                            icon = Icons.Filled.EmojiEvents,
                            title = "Challenges",
                            subtitle = "Active fitness milestones",
                            tint = ext.warning,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("hub_challenges"),
                            onClick = onOpenChallenges
                        )
                        HubTile(
                            icon = Icons.Filled.AutoAwesome,
                            title = "AI Coach",
                            subtitle = "Personalized guidance",
                            tint = ext.success,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("hub_ai_coach"),
                            onClick = onOpenAiCoach
                        )
                    }
                    HubTile(
                        icon = Icons.Filled.Settings,
                        title = "Settings & Data",
                        subtitle = "Theme, units, privacy & export",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("hub_settings"),
                        onClick = onOpenSettings
                    )
                }
            }

            // Personal Info & Daily Targets
            item {
                FitTrackCard(modifier = Modifier.fillMaxWidth()) {
                    SectionHeader(title = "Personal Info & Daily Targets")
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        label = { Text("Display Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = ageText,
                            onValueChange = { ageText = it.filter(Char::isDigit) },
                            label = { Text("Age") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(0.8f)
                        )
                        OutlinedTextField(
                            value = heightText,
                            onValueChange = { heightText = it },
                            label = { Text("Height (cm)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = weightText,
                            onValueChange = { weightText = it },
                            label = { Text("Weight (kg)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = targetWeightText,
                            onValueChange = { targetWeightText = it },
                            label = { Text("Target Weight (kg)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Biological Sex & Activity Level", style = MaterialTheme.typography.labelLarge)
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("male" to "Male", "female" to "Female").forEach { (k, label) ->
                            FilterChip(selected = selectedSex == k, onClick = { selectedSex = k }, label = { Text(label) })
                        }
                        listOf("sedentary" to "Sedentary", "light" to "Light", "moderate" to "Moderate", "active" to "Active").forEach { (k, label) ->
                            FilterChip(selected = selectedActivityLevel == k, onClick = { selectedActivityLevel = k }, label = { Text(label) })
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = stepGoalText,
                            onValueChange = { stepGoalText = it.filter(Char::isDigit) },
                            label = { Text("Step Goal") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = waterGoalText,
                            onValueChange = { waterGoalText = it.filter(Char::isDigit) },
                            label = { Text("Water (ml)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = calorieGoalText,
                        onValueChange = { calorieGoalText = it.filter(Char::isDigit) },
                        label = { Text("Daily Calorie Target (kcal)") },
                        enabled = !isMinor,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (isMinor) {
                        Spacer(modifier = Modifier.height(8.dp))
                        DisclaimerBanner(
                            text = stringResource(R.string.minor_safeguard_notice),
                            isWarning = true
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Primary Fitness Goal", style = MaterialTheme.typography.labelLarge)
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            "general_fitness" to "General Fitness",
                            "muscle_gain" to "Muscle Gain",
                            "weight_maintenance" to "Maintenance",
                            "weight_loss" to "Weight Loss"
                        ).forEach { (key, label) ->
                            val disabled = isMinor && key == "weight_loss"
                            FilterChip(
                                selected = selectedGoal == key,
                                enabled = !disabled,
                                onClick = { selectedGoal = key },
                                label = { Text(label) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    GradientButton(
                        text = "Save Personal Info & Targets",
                        icon = Icons.Filled.CheckCircle,
                        onClick = {
                            val safeGoal = if (isMinor && selectedGoal == "weight_loss") "weight_maintenance" else selectedGoal
                            selectedGoal = safeGoal
                            onSavePersonalInfoAndGoals(
                                displayName,
                                ageYears.toLong(),
                                selectedSex,
                                heightText.toDoubleOrNull() ?: profile?.heightCm ?: 175.0,
                                weightText.toDoubleOrNull() ?: profile?.weightKg ?: 72.0,
                                targetWeightText.toDoubleOrNull() ?: profile?.targetWeightKg ?: 70.0,
                                selectedActivityLevel,
                                stepGoalText.toLongOrNull() ?: 10000L,
                                waterGoalText.toLongOrNull() ?: 2500L,
                                calorieGoalText.toLongOrNull() ?: 2200L,
                                safeGoal
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("save_profile_goals_button")
                    )
                }
            }

            // Training Preferences
            item {
                FitTrackCard(modifier = Modifier.fillMaxWidth()) {
                    SectionHeader(title = "Training Preferences")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Preferred Training Split", style = MaterialTheme.typography.labelLarge)
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Full Body", "Upper / Lower", "Push / Pull / Legs", "Bro Split", "Home Workouts").forEach { split ->
                            FilterChip(
                                selected = preferredSplit == split,
                                onClick = { preferredSplit = split },
                                label = { Text(split) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Experience & Equipment", style = MaterialTheme.typography.labelLarge)
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("beginner" to "Beginner", "intermediate" to "Intermediate", "advanced" to "Advanced").forEach { (k, label) ->
                            FilterChip(selected = trainingExp == k, onClick = { trainingExp = k }, label = { Text(label) })
                        }
                        listOf("Full Gym", "Dumbbells Only", "Home / Bodyweight").forEach { eq ->
                            FilterChip(selected = availableEquip == eq, onClick = { availableEquip = eq }, label = { Text(eq) })
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = targetDaysText,
                        onValueChange = { targetDaysText = it.filter(Char::isDigit) },
                        label = { Text("Target Training Days / Week") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    GradientButton(
                        text = "Save Training Preferences",
                        onClick = {
                            onSaveTrainingPreferences(
                                trainingPreferences.copy(
                                    preferredSplit = preferredSplit,
                                    targetDaysPerWeek = (targetDaysText.toIntOrNull() ?: 4).coerceIn(1, 7),
                                    experienceLevel = trainingExp,
                                    availableEquipment = availableEquip
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth().testTag("save_training_prefs_button")
                    )
                }
            }

            // Nutrition Preferences
            item {
                FitTrackCard(modifier = Modifier.fillMaxWidth()) {
                    SectionHeader(title = "Nutrition Preferences")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Dietary Pattern", style = MaterialTheme.typography.labelLarge)
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Balanced", "High Protein", "Vegetarian", "Vegan", "Mediterranean", "Low Carb").forEach { style ->
                            FilterChip(selected = dietaryStyle == style, onClick = { dietaryStyle = style }, label = { Text(style) })
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = allergiesText,
                            onValueChange = { allergiesText = it },
                            label = { Text("Allergies / Exclusions") },
                            singleLine = true,
                            modifier = Modifier.weight(1.4f)
                        )
                        OutlinedTextField(
                            value = mealsPerDayText,
                            onValueChange = { mealsPerDayText = it.filter(Char::isDigit) },
                            label = { Text("Meals/Day") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(0.7f)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    GradientButton(
                        text = "Save Nutrition Preferences",
                        onClick = {
                            onSaveNutritionPreferences(
                                nutritionPreferences.copy(
                                    dietaryStyle = dietaryStyle,
                                    allergies = allergiesText.trim(),
                                    mealsPerDay = (mealsPerDayText.toIntOrNull() ?: 4).coerceIn(2, 6)
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth().testTag("save_nutrition_prefs_button")
                    )
                }
            }
        }
    }
}

@Composable
private fun BiometricBadge(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)
    }
}

@Composable
private fun HubTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    tint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    ActivityCard(modifier = modifier, onClick = onClick) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(tint.copy(alpha = 0.25f), tint.copy(alpha = 0.10f)))),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = title, tint = tint, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
