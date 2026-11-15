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
import com.example.data.model.StepRecord
import com.example.data.model.UpdateCenterUiState
import com.example.data.model.UserChallenge
import com.example.data.model.UserProfile
import com.example.data.model.WaterLog
import com.example.data.model.WorkoutPlan
import com.example.data.model.WorkoutScheduleItem
import com.example.data.model.WorkoutSessionLog
import com.example.data.model.estimatedDurationMinutes
import com.example.domain.FitnessCalculator
import com.example.domain.WorkoutAnalyticsEngine
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
import com.example.ui.components.SimpleBarChart
import com.example.ui.components.StatusBadge
import com.example.ui.components.UpdateAvailabilityBanner
import com.example.ui.theme.HeroNumberTextStyle
import com.example.ui.theme.LocalFitTrackColors
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    profile: UserProfile?,
    todaySteps: Long,
    stepRecords: List<StepRecord> = emptyList(),
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
    onOpenChallenges: () -> Unit,
    updateUiState: UpdateCenterUiState = UpdateCenterUiState(),
    onOpenUpdateCenter: () -> Unit = {},
    onOpenWhatsNew: () -> Unit = {}
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
    val todayGpsCalories = gpsActivities.filter {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(it.startedAtMs)) == today
    }.sumOf { it.caloriesBurned }
    val totalBurnedToday = stepCaloriesBurned + todayWorkoutCalories + todayGpsCalories
    val remainingCalories = calorieGoal - todayCaloriesIn.toLong()

    val weeklyCutoffKey = remember {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -6)
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
    }
    val weeklyStepRecords = remember(stepRecords, weeklyCutoffKey) {
        stepRecords.filter { it.dateKey >= weeklyCutoffKey }.sortedBy { it.dateKey }
    }
    val weeklyWorkoutCount = remember(workoutSessions, weeklyCutoffKey) {
        workoutSessions.count { it.dateKey >= weeklyCutoffKey }
    }
    val weeklyTotalSteps = remember(weeklyStepRecords, todaySteps, today) {
        val fromRecords = weeklyStepRecords.filterNot { it.dateKey == today }.sumOf { it.steps }
        fromRecords + todaySteps
    }

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
    val latestSession = remember(workoutSessions) {
        workoutSessions.maxByOrNull { it.createdAt?.seconds ?: 0L } ?: workoutSessions.firstOrNull()
    }

    val coachInsight = remember(todaySteps, stepGoal, todayCaloriesIn, calorieGoal, todayWaterMl, waterGoal, latestSession, todayProtein, macroTargets) {
        when {
            todaySteps < stepGoal / 2 -> "You're at $stepCompletionPct% of your step target — a brisk 15-min walk adds ~1,500 steps."
            todayWaterMl < waterGoal / 2 -> "Hydration is at $todayWaterMl / $waterGoal ml. Log a glass of water to keep recovery on track."
            todayProtein < macroTargets.proteinGrams * 0.6 && todayMeals.isNotEmpty() ->
                "Protein is at ${todayProtein.toInt()}g of ${macroTargets.proteinGrams}g — prioritize lean protein in your next meal."
            latestSession == null && upcomingPlan != null ->
                "Start '${upcomingPlan.name}' (~${upcomingPlan.estimatedDurationMinutes()} min) to log your first training baseline."
            else ->
                "Strong momentum: ${totalBurnedToday.toInt()} kcal burned, ${todayCaloriesIn.toInt()} kcal consumed, $weeklyWorkoutCount sessions this week."
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
            // Personalized Header
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

            // Update / What's New Banner (shown only when actionable or unread)
            if (updateUiState.isUpdateAvailable ||
                updateUiState.unreadReleaseCount > 0 ||
                updateUiState.playUpdateState is com.example.data.model.PlayUpdateUiState.DownloadedReadyToInstall
            ) {
                item {
                    UpdateAvailabilityBanner(
                        uiState = updateUiState,
                        onOpenUpdateCenter = onOpenUpdateCenter,
                        onOpenWhatsNew = onOpenWhatsNew
                    )
                }
            }

            // 1. Daily Progress & Energy Hero Card
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
                        Text("Daily Progress", style = MaterialTheme.typography.titleLarge)
                        StatusBadge(
                            text = "$stepCompletionPct% step goal",
                            tint = if (stepCompletionPct >= 100) ext.success else MaterialTheme.colorScheme.secondary
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        AnimatedProgressRing(
                            progress = stepProgress,
                            size = 136.dp,
                            strokeWidth = 13.dp,
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
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            HeroSubStat(
                                icon = Icons.Filled.LocalFireDepartment,
                                label = "Burned",
                                value = "${totalBurnedToday.toInt()} kcal",
                                tint = ext.warning
                            )
                            HeroSubStat(
                                icon = Icons.Filled.Restaurant,
                                label = "Consumed",
                                value = "${todayCaloriesIn.toInt()} / $calorieGoal",
                                tint = ext.success
                            )
                            HeroSubStat(
                                icon = Icons.Filled.WaterDrop,
                                label = "Water",
                                value = "$todayWaterMl / $waterGoal ml",
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = String.format(Locale.US, "Step distance: %.2f km today", distanceKm),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
                        label = "Activity",
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

            // 3. Weekly Activity Overview
            item {
                FitTrackCard(modifier = Modifier.fillMaxWidth(), onClick = onOpenActivity) {
                    SectionHeader(
                        title = "Weekly Activity Overview",
                        actionLabel = "Details",
                        onAction = onOpenActivity
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("7-Day Steps", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$weeklyTotalSteps", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Workouts", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$weeklyWorkoutCount sessions", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("GPS Distance", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            val weekGpsKm = gpsActivities.take(10).sumOf { it.distanceKm }
                            Text(String.format(Locale.US, "%.1f km", weekGpsKm), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                    if (weeklyStepRecords.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        SimpleBarChart(
                            values = weeklyStepRecords.map { it.dateKey.takeLast(5) to it.steps.toFloat() }
                        )
                    }
                }
            }

            // 4. Upcoming Workout Card
            item {
                FitTrackCard(
                    modifier = Modifier.fillMaxWidth(),
                    accentBorder = ext.warning.copy(alpha = 0.45f)
                ) {
                    SectionHeader(
                        title = "Upcoming Workout",
                        actionLabel = "All Workouts",
                        onAction = onOpenWorkouts
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (todaySchedule?.status == "rest") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(ext.success.copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.SelfImprovement, contentDescription = "Rest Day", tint = ext.success)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                StatusBadge(text = "REST & RECOVERY DAY", tint = ext.success)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Scheduled recovery today", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("Focus on sleep, hydration, and light mobility.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        SecondaryButton(
                            text = "Browse Workouts Anyway",
                            onClick = onOpenWorkouts,
                            modifier = Modifier.fillMaxWidth().testTag("home_browse_workouts_button")
                        )
                    } else if (upcomingPlan != null) {
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
                            text = "${upcomingPlan.exercises.size} exercises • ~${upcomingPlan.estimatedDurationMinutes()} min • ${upcomingPlan.level.replaceFirstChar { it.uppercase() }}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        GradientButton(
                            text = if (todaySchedule?.status == "completed") "Train Again" else "Start Workout",
                            icon = Icons.Filled.PlayArrow,
                            onClick = { onStartWorkout(upcomingPlan) },
                            modifier = Modifier.fillMaxWidth().testTag("home_start_upcoming_workout_button")
                        )
                    } else {
                        GradientButton(
                            text = "Choose a Workout Plan",
                            icon = Icons.Filled.FitnessCenter,
                            onClick = onOpenWorkouts,
                            modifier = Modifier.fillMaxWidth().testTag("home_choose_workout_button")
                        )
                    }
                }
            }

            // 5. Recent Workout Card
            if (latestSession != null) {
                item {
                    FitTrackCard(modifier = Modifier.fillMaxWidth(), onClick = onOpenWorkouts) {
                        SectionHeader(
                            title = "Recent Workout",
                            actionLabel = "History",
                            onAction = onOpenWorkouts
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = latestSession.workoutName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${latestSession.dateKey} • ${FitnessCalculator.formatDuration(latestSession.durationSeconds)} • ${latestSession.setsCompleted} sets",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = WorkoutAnalyticsEngine.formatWeight(latestSession.totalVolumeKg),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontWeight = FontWeight.Bold
                                )
                                if (latestSession.personalRecordsHit > 0) {
                                    Text(
                                        text = "${latestSession.personalRecordsHit} PR(s)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ext.warning
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 6. Nutrition Summary Card
            item {
                FitTrackCard(modifier = Modifier.fillMaxWidth(), onClick = onOpenNutrition) {
                    SectionHeader(
                        title = "Nutrition Summary",
                        actionLabel = "Diary",
                        onAction = onOpenNutrition
                    )
                    Spacer(modifier = Modifier.height(8.dp))
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
                            Text("Water", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

            // 7. Contextual AI Coach Insights Card
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onOpenAiCoach,
                    accentBorder = MaterialTheme.colorScheme.secondary.copy(alpha = 0.50f)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        FitTrackGeometricIllustration(type = FitTrackIllustrationType.AI_COACH, size = 64.dp)
                        Column(modifier = Modifier.weight(1f)) {
                            StatusBadge(text = "AI COACH INSIGHT", tint = MaterialTheme.colorScheme.secondary)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = coachInsight,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        GradientButton(
                            text = "Ask AI Coach",
                            icon = Icons.Filled.AutoAwesome,
                            onClick = onOpenAiCoach,
                            modifier = Modifier.weight(1f).testTag("home_ai_coach_chip")
                        )
                        if (challenges.isNotEmpty()) {
                            SecondaryButton(
                                text = "Challenges",
                                onClick = onOpenChallenges,
                                modifier = Modifier.weight(0.75f)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showNotifications) {
        AlertDialog(
            onDismissRequest = { showNotifications = false },
            title = { Text("Today's Summary") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("• Steps: $todaySteps / $stepGoal ($stepCompletionPct%)")
                    Text("• Burned: ${totalBurnedToday.toInt()} kcal • Consumed: ${todayCaloriesIn.toInt()} kcal")
                    Text("• Hydration: $todayWaterMl / $waterGoal ml")
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
