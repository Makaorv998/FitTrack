package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.catalog.WorkoutCatalog
import com.example.data.model.GpsActivity
import com.example.data.model.MealLog
import com.example.data.model.UserChallenge
import com.example.data.model.UserProfile
import com.example.data.model.WaterLog
import com.example.data.model.WorkoutPlan
import com.example.data.model.WorkoutScheduleItem
import com.example.data.model.WorkoutSessionLog
import com.example.domain.FitnessCalculator
import com.example.platform.todayDateKey
import com.example.ui.branding.FitTrackGeometricIllustration
import com.example.ui.branding.FitTrackIllustrationType
import com.example.ui.components.AnimatedCounterText
import com.example.ui.components.AnimatedProgressRing
import com.example.ui.components.AuroraAmbientBackground
import com.example.ui.components.AuroraProgressBar
import com.example.ui.components.FitTrackAvatar
import com.example.ui.components.FitTrackCard
import com.example.ui.components.GlassCard
import com.example.ui.components.GradientButton
import com.example.ui.components.SecondaryButton
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusBadge
import com.example.ui.theme.HeroNumberTextStyle
import com.example.ui.theme.LocalFitTrackColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    profile: UserProfile?,
    todaySteps: Long,
    meals: List<MealLog>,
    waterLogs: List<WaterLog>,
    gpsActivities: List<GpsActivity>,
    workoutPlans: List<WorkoutPlan>,
    workoutSchedules: List<WorkoutScheduleItem>,
    workoutSessions: List<WorkoutSessionLog>,
    challenges: List<UserChallenge>,
    onQuickAddWater: (Long) -> Unit,
    onOpenActivity: () -> Unit,
    onOpenNutrition: () -> Unit,
    onOpenFoodScanner: () -> Unit,
    onOpenAiCoach: () -> Unit,
    onOpenWorkouts: () -> Unit,
    onStartWorkout: (WorkoutPlan) -> Unit,
    onOpenChallenges: () -> Unit
) {
    val ext = LocalFitTrackColors.current
    val today = remember { todayDateKey() }
    val formattedDate = remember {
        SimpleDateFormat("EEE, MMM d", Locale.US).format(Date())
    }
    val todayMeals = remember(meals, today) { meals.filter { it.dateKey == today } }
    val todayCaloriesIn = todayMeals.sumOf { it.calories }
    val todayProtein = todayMeals.sumOf { it.proteinG }
    val todayCarbs = todayMeals.sumOf { it.carbsG }
    val todayFat = todayMeals.sumOf { it.fatG }
    val todayWaterMl = remember(waterLogs, today) { waterLogs.filter { it.dateKey == today }.sumOf { it.amountMl } }

    val stepGoal = (profile?.dailyStepGoal ?: 10000L).coerceAtLeast(1000L)
    val calorieGoal = (profile?.dailyCalorieTarget ?: 2200L).coerceAtLeast(1200L)
    val waterGoal = (profile?.dailyWaterTargetMl ?: 2500L).coerceAtLeast(500L)
    val stepProgress = (todaySteps.toFloat() / stepGoal.toFloat()).coerceIn(0f, 1f)
    val stepCompletionPct = ((todaySteps.toDouble() / stepGoal.toDouble()) * 100.0).toInt().coerceAtLeast(0)
    val distanceKm = FitnessCalculator.calculateWalkingDistanceKm(todaySteps, profile?.heightCm ?: 175.0)
    val stepCaloriesBurned = (todaySteps * 0.04) * ((profile?.weightKg ?: 70.0) / 70.0)
    val todayWorkoutCalories = workoutSessions.filter { it.dateKey == today }.sumOf { it.caloriesBurned }
    val totalBurnedToday = stepCaloriesBurned + todayWorkoutCalories
    val todayGpsMinutes = gpsActivities.filter {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(it.startedAtMs)) == today
    }.sumOf { it.durationSeconds } / 60L
    val activeMinutes = ((todaySteps / 100L) + todayGpsMinutes).toInt()
    val remainingCalories = calorieGoal - todayCaloriesIn.toLong()

    val macroTargets = remember(profile, calorieGoal) {
        FitnessCalculator.calculateMacros(
            calorieTarget = calorieGoal.toInt(),
            weightKg = profile?.weightKg ?: 70.0,
            goal = profile?.goal ?: "general_fitness",
            ageYears = (profile?.ageYears ?: 25L).toInt()
        )
    }

    val allPlans = remember(workoutPlans, profile) {
        workoutPlans + WorkoutCatalog.starterTemplatesForUser(profile)
    }
    val todaySchedule = remember(workoutSchedules, today) {
        workoutSchedules.firstOrNull { it.dateKey == today }
    }
    val upcomingPlan = remember(todaySchedule, allPlans, workoutSessions) {
        when {
            todaySchedule != null && todaySchedule.status != "rest" -> {
                allPlans.firstOrNull { it.id == todaySchedule.workoutId || it.name == todaySchedule.workoutName }
                    ?: allPlans.firstOrNull()
            }
            todaySchedule?.status == "rest" -> null
            else -> {
                val lastWorkoutId = workoutSessions.firstOrNull()?.workoutId
                allPlans.firstOrNull { it.id != lastWorkoutId } ?: allPlans.firstOrNull()
            }
        }
    }

    var showNotifications by remember { mutableStateOf(false) }
    val bottomClearance = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 104.dp

    AuroraAmbientBackground {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = bottomClearance),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top Personalized Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        FitTrackAvatar(
                            photoUrl = profile?.photoUrl,
                            displayName = profile?.displayName ?: "Athlete",
                            size = 46.dp
                        )
                        Column {
                            Text(
                                text = formattedDate.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = "Hi, ${profile?.displayName ?: "Athlete"}",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                    IconButton(
                        onClick = { showNotifications = true },
                        modifier = Modifier.testTag("notifications_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Notifications,
                            contentDescription = "Notifications",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }

            // 1. Daily Activity Overview Hero Ring Card
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onOpenActivity,
                    accentBorder = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Daily Activity", style = MaterialTheme.typography.titleLarge)
                        StatusBadge(
                            text = "$stepCompletionPct% of goal",
                            tint = if (stepCompletionPct >= 100) ext.success else MaterialTheme.colorScheme.secondary
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        AnimatedProgressRing(
                            progress = stepProgress,
                            size = 144.dp,
                            strokeWidth = 14.dp,
                            primaryColor = MaterialTheme.colorScheme.primary,
                            secondaryColor = MaterialTheme.colorScheme.secondary
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                AnimatedCounterText(
                                    targetValue = todaySteps.toInt(),
                                    style = HeroNumberTextStyle,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "/ $stepGoal steps",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Column(
                            modifier = Modifier.padding(start = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            HeroSubStat(
                                icon = Icons.Filled.LocalFireDepartment,
                                label = "Burned",
                                value = "${totalBurnedToday.toInt()} kcal",
                                tint = ext.warning
                            )
                            HeroSubStat(
                                icon = Icons.AutoMirrored.Filled.DirectionsWalk,
                                label = "Distance",
                                value = String.format(Locale.US, "%.2f km", distanceKm),
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            HeroSubStat(
                                icon = Icons.Filled.Timer,
                                label = "Active",
                                value = "$activeMinutes min",
                                tint = ext.success
                            )
                        }
                    }
                }
            }

            // 2. Quick Actions Row
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickActionPill(
                        icon = Icons.AutoMirrored.Filled.DirectionsWalk,
                        label = "Start Walk",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.testTag("quick_start_walk"),
                        onClick = onOpenActivity
                    )
                    QuickActionPill(
                        icon = Icons.AutoMirrored.Filled.DirectionsRun,
                        label = "GPS Run",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.testTag("quick_gps_run"),
                        onClick = onOpenActivity
                    )
                    QuickActionPill(
                        icon = Icons.Filled.FitnessCenter,
                        label = "Train Now",
                        tint = ext.warning,
                        modifier = Modifier.testTag("quick_workouts"),
                        onClick = {
                            if (upcomingPlan != null) onStartWorkout(upcomingPlan) else onOpenWorkouts()
                        }
                    )
                    QuickActionPill(
                        icon = Icons.Filled.Restaurant,
                        label = "Add Food",
                        tint = ext.success,
                        modifier = Modifier.testTag("quick_add_food"),
                        onClick = onOpenNutrition
                    )
                    QuickActionPill(
                        icon = Icons.Filled.CameraAlt,
                        label = "Scan Food",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.testTag("quick_scan_food"),
                        onClick = onOpenFoodScanner
                    )
                    QuickActionPill(
                        icon = Icons.Filled.WaterDrop,
                        label = "+250ml",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.testTag("quick_add_water"),
                        onClick = { onQuickAddWater(250L) }
                    )
                }
            }

            // 3. Upcoming Workout Card
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    accentBorder = ext.warning.copy(alpha = 0.45f)
                ) {
                    SectionHeader(
                        title = "Upcoming Workout",
                        actionLabel = "All Workouts",
                        onAction = onOpenWorkouts
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    if (todaySchedule?.status == "rest") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(ext.success.copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.SelfImprovement, contentDescription = "Rest Day", tint = ext.success)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                StatusBadge(text = "REST & RECOVERY DAY", tint = ext.success)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Scheduled active recovery today",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Prioritize sleep, hydration, and light mobility.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        SecondaryButton(
                            text = "Browse Workouts Anyway",
                            onClick = onOpenWorkouts,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("home_browse_workouts_button")
                        )
                    } else if (upcomingPlan != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                StatusBadge(
                                    text = if (todaySchedule?.status == "completed") "COMPLETED TODAY" else if (todaySchedule != null) "SCHEDULED TODAY" else "NEXT SESSION",
                                    tint = if (todaySchedule?.status == "completed") ext.success else MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = upcomingPlan.name,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${upcomingPlan.exercises.size} exercises • ~${upcomingPlan.estimatedMinutes} min • ${upcomingPlan.level.replaceFirstChar { it.uppercase() }}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        GradientButton(
                            text = if (todaySchedule?.status == "completed") "Train Again" else "Start Workout",
                            icon = Icons.Filled.PlayArrow,
                            onClick = { onStartWorkout(upcomingPlan) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("home_start_upcoming_workout_button")
                        )
                    } else {
                        GradientButton(
                            text = "Choose a Workout Plan",
                            icon = Icons.Filled.FitnessCenter,
                            onClick = onOpenWorkouts,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("home_choose_workout_button")
                        )
                    }
                }
            }

            // 4. Nutrition Summary Card
            item {
                FitTrackCard(modifier = Modifier.fillMaxWidth(), onClick = onOpenNutrition) {
                    SectionHeader(
                        title = "Nutrition Summary",
                        actionLabel = "Log Meal",
                        onAction = onOpenNutrition
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text("Consumed", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${todayCaloriesIn.toInt()} kcal", style = MaterialTheme.typography.headlineMedium)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Hydration", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$todayWaterMl / $waterGoal ml", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Remaining", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = if (remainingCalories >= 0) "$remainingCalories kcal" else "+${-remainingCalories} over",
                                style = MaterialTheme.typography.titleLarge,
                                color = if (remainingCalories >= 0) ext.success else ext.warning
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    AuroraProgressBar(
                        label = "Protein",
                        valueText = "${todayProtein.toInt()} / ${macroTargets.proteinGrams} g",
                        progress = todayProtein.toFloat() / macroTargets.proteinGrams.coerceAtLeast(1),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    AuroraProgressBar(
                        label = "Carbohydrates",
                        valueText = "${todayCarbs.toInt()} / ${macroTargets.carbsGrams} g",
                        progress = todayCarbs.toFloat() / macroTargets.carbsGrams.coerceAtLeast(1),
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    AuroraProgressBar(
                        label = "Fat",
                        valueText = "${todayFat.toInt()} / ${macroTargets.fatGrams} g",
                        progress = todayFat.toFloat() / macroTargets.fatGrams.coerceAtLeast(1),
                        color = ext.warning
                    )
                }
            }

            // 5. Fitness Goals Snapshot
            item {
                val goalLabel = (profile?.goal ?: "general_fitness").replace("_", " ").replaceFirstChar { it.uppercase() }
                FitTrackCard(modifier = Modifier.fillMaxWidth()) {
                    SectionHeader(
                        title = "Fitness Goals",
                        actionLabel = "Challenges",
                        onAction = onOpenChallenges
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    StatusBadge(text = "PRIMARY GOAL: ${goalLabel.uppercase()}", tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(12.dp))
                    AuroraProgressBar(
                        label = "Daily Steps Target",
                        valueText = "$todaySteps / $stepGoal",
                        progress = stepProgress,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    AuroraProgressBar(
                        label = "Hydration Target",
                        valueText = "$todayWaterMl / $waterGoal ml",
                        progress = (todayWaterMl.toFloat() / waterGoal.toFloat()).coerceIn(0f, 1f),
                        color = ext.success
                    )
                    if (challenges.isNotEmpty()) {
                        val first = challenges.first()
                        Spacer(modifier = Modifier.height(10.dp))
                        AuroraProgressBar(
                            label = first.title,
                            valueText = "${first.currentValue.toInt()} / ${first.targetValue.toInt()} ${first.unit}",
                            progress = (first.currentValue / first.targetValue.coerceAtLeast(1.0)).toFloat().coerceIn(0f, 1f),
                            color = ext.warning
                        )
                    }
                }
            }

            // 6. AI Insights Card
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onOpenAiCoach,
                    accentBorder = MaterialTheme.colorScheme.secondary.copy(alpha = 0.55f)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        FitTrackGeometricIllustration(type = FitTrackIllustrationType.AI_COACH, size = 68.dp)
                        Column(modifier = Modifier.weight(1f)) {
                            StatusBadge(text = "AI INSIGHTS", tint = MaterialTheme.colorScheme.secondary)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Ready for today's briefing?",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = "Review workout readiness, macro gaps, or ask your AI Coach.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    GradientButton(
                        text = "Open AI Fitness Coach",
                        icon = Icons.Filled.AutoAwesome,
                        onClick = onOpenAiCoach,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("home_ai_coach_chip")
                    )
                }
            }
        }
    }

    if (showNotifications) {
        AlertDialog(
            onDismissRequest = { showNotifications = false },
            title = { Text("Smart Reminders & Telemetry") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("• Steps: $todaySteps / $stepGoal today ($stepCompletionPct%)")
                    Text("• Hydration: $todayWaterMl / $waterGoal ml today")
                    Text("• Meals logged today: ${todayMeals.size}")
                    Text("• Workouts completed: ${workoutSessions.size}")
                }
            },
            confirmButton = {
                Button(onClick = { showNotifications = false }) { Text("Got it") }
            }
        )
    }
}

@Composable
private fun HeroSubStat(
    icon: ImageVector,
    label: String,
    value: String,
    tint: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(16.dp))
        }
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun QuickActionPill(
    icon: ImageVector,
    label: String,
    tint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val ext = LocalFitTrackColors.current
    Surface(
        modifier = modifier
            .width(88.dp)
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, ext.border)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(tint.copy(alpha = 0.25f), tint.copy(alpha = 0.10f)))),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(19.dp))
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}
