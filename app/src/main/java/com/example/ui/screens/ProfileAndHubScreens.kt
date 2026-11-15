package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.catalog.FitnessCatalog
import com.example.data.model.AiChatMessage
import com.example.data.model.BodyMeasurement
import com.example.data.model.GpsActivity
import com.example.data.model.MealLog
import com.example.data.model.ProgressPhotoNote
import com.example.data.model.StepRecord
import com.example.data.model.UserChallenge
import com.example.data.model.UserProfile
import com.example.data.model.WeightRecord
import com.example.data.model.WorkoutSessionLog
import com.example.domain.FitnessCalculator
import com.example.ui.AsyncState
import com.example.ui.branding.FitTrackGeometricIllustration
import com.example.ui.branding.FitTrackIllustrationType
import com.example.ui.components.ActivityCard
import com.example.ui.components.AuroraAmbientBackground
import com.example.ui.components.AuroraProgressBar
import com.example.ui.components.ConfirmationModal
import com.example.ui.components.DisclaimerBanner
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ErrorBannerCard
import com.example.ui.components.FitTrackAvatar
import com.example.ui.components.FitTrackCard
import com.example.ui.components.GlassCard
import com.example.ui.components.GradientButton
import com.example.ui.components.ScreenHeader
import com.example.ui.components.SecondaryButton
import com.example.ui.components.SectionHeader
import com.example.ui.components.SegmentedControl
import com.example.ui.components.SimpleBarChart
import com.example.ui.components.StatCard
import com.example.ui.components.StatusBadge
import com.example.ui.components.TrendLineChart
import com.example.ui.theme.LocalFitTrackColors
import com.example.ui.theme.MetricNumberTextStyle
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

private fun dateKeyDaysAgo(days: Int): String {
    val cal = Calendar.getInstance()
    cal.add(Calendar.DAY_OF_YEAR, -days)
    return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
}

private fun cutoffMillisDaysAgo(days: Int): Long {
    val cal = Calendar.getInstance()
    cal.add(Calendar.DAY_OF_YEAR, -days)
    return cal.timeInMillis
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    profile: UserProfile?,
    onSaveProfileGoals: (
        displayName: String,
        stepGoal: Long,
        waterGoalMl: Long,
        calorieGoal: Long,
        goal: String,
        sensorConsent: Boolean,
        locationConsent: Boolean
    ) -> Unit,
    onOpenBodyProgress: () -> Unit,
    onOpenCalculators: () -> Unit,
    onOpenChallenges: () -> Unit,
    onOpenAiCoach: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val ext = LocalFitTrackColors.current
    var displayName by remember(profile?.displayName) { mutableStateOf(profile?.displayName ?: "Athlete") }
    var stepGoalText by remember(profile?.dailyStepGoal) { mutableStateOf((profile?.dailyStepGoal ?: 10000L).toString()) }
    var waterGoalText by remember(profile?.dailyWaterTargetMl) { mutableStateOf((profile?.dailyWaterTargetMl ?: 2500L).toString()) }
    var calorieGoalText by remember(profile?.dailyCalorieTarget) { mutableStateOf((profile?.dailyCalorieTarget ?: 2200L).toString()) }
    var selectedGoal by remember(profile?.goal) { mutableStateOf(profile?.goal ?: "general_fitness") }
    var sensorConsent by remember(profile?.sensorConsentGranted) { mutableStateOf(profile?.sensorConsentGranted ?: true) }
    var locationConsent by remember(profile?.locationConsentGranted) { mutableStateOf(profile?.locationConsentGranted ?: true) }

    val ageYears = (profile?.ageYears ?: 26L).toInt()
    val isMinor = FitnessCalculator.isMinor(ageYears)
    val bmi = FitnessCalculator.calculateBmi(profile?.weightKg ?: 72.0, profile?.heightCm ?: 175.0)
    val bottomClearance = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 104.dp

    AuroraAmbientBackground {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = bottomClearance),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                ScreenHeader(
                    title = "Profile",
                    subtitle = "Athlete profile, tools & daily targets"
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
                            size = 66.dp
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
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                StatusBadge(
                                    text = (profile?.goal ?: "general_fitness").replace("_", " ").uppercase(),
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                            }
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
                        subtitle = "Theme, privacy & export",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("hub_settings"),
                        onClick = onOpenSettings
                    )
                }
            }

            // Editable Targets
            item {
                FitTrackCard(modifier = Modifier.fillMaxWidth()) {
                    SectionHeader(title = "Personal Goals & Targets")
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
                        text = "Save Profile & Targets",
                        icon = Icons.Filled.CheckCircle,
                        onClick = {
                            onSaveProfileGoals(
                                displayName,
                                stepGoalText.toLongOrNull() ?: 10000L,
                                waterGoalText.toLongOrNull() ?: 2500L,
                                calorieGoalText.toLongOrNull() ?: 2200L,
                                selectedGoal,
                                sensorConsent,
                                locationConsent
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("save_profile_goals_button")
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BodyProgressScreen(
    profile: UserProfile?,
    weightRecords: List<WeightRecord>,
    measurements: List<BodyMeasurement>,
    stepRecords: List<StepRecord>,
    meals: List<MealLog>,
    gpsActivities: List<GpsActivity>,
    workoutSessions: List<WorkoutSessionLog>,
    progressPhotos: List<ProgressPhotoNote>,
    onLogWeight: (Double, String) -> Unit,
    onLogMeasurements: (Double, Double, Double, Double, Double) -> Unit,
    onAddProgressPhoto: (String, String) -> Unit,
    onDeleteProgressPhoto: (String) -> Unit,
    onBack: () -> Unit
) {
    val ext = LocalFitTrackColors.current
    var weightInput by remember(profile?.weightKg) {
        mutableStateOf((profile?.weightKg ?: 72.0).toString())
    }
    var weightNote by remember { mutableStateOf("") }

    var waistText by remember { mutableStateOf("") }
    var chestText by remember { mutableStateOf("") }
    var armsText by remember { mutableStateOf("") }
    var legsText by remember { mutableStateOf("") }
    var hipsText by remember { mutableStateOf("") }

    var rangeIndex by remember { mutableIntStateOf(0) } // 0=7D, 1=30D, 2=90D, 3=1Y
    val daysWindow = when (rangeIndex) {
        0 -> 7
        1 -> 30
        2 -> 90
        else -> 365
    }
    val cutoffKey = remember(daysWindow) { dateKeyDaysAgo(daysWindow) }
    val cutoffMs = remember(daysWindow) { cutoffMillisDaysAgo(daysWindow) }

    val filteredWeights = remember(weightRecords, cutoffKey) {
        weightRecords.filter { it.dateKey >= cutoffKey }.sortedBy { it.dateKey }
    }
    val filteredSteps = remember(stepRecords, cutoffKey) {
        stepRecords.filter { it.dateKey >= cutoffKey }.sortedBy { it.dateKey }
    }
    val filteredGps = remember(gpsActivities, cutoffMs) {
        gpsActivities.filter {
            val ts = if (it.startedAtMs > 0L) it.startedAtMs else (it.createdAt?.toDate()?.time ?: System.currentTimeMillis())
            ts >= cutoffMs
        }
    }
    val filteredWorkouts = remember(workoutSessions, cutoffMs) {
        workoutSessions.filter {
            val ts = it.createdAt?.toDate()?.time ?: System.currentTimeMillis()
            ts >= cutoffMs
        }
    }
    val filteredMeals = remember(meals, cutoffKey) {
        meals.filter { it.dateKey >= cutoffKey }
    }

    var photoCaption by remember { mutableStateOf("") }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onAddProgressPhoto(uri.toString(), photoCaption.ifBlank { "Progress Check-in" })
            photoCaption = ""
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.85f)),
                title = { Text("Body Progress & Analytics") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        AuroraAmbientBackground {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Timeframe Filter
                item {
                    SegmentedControl(
                        options = listOf("7D", "30D", "90D", "1Y"),
                        selectedIndex = rangeIndex,
                        onSelect = { rangeIndex = it }
                    )
                }

                // Weight Tracker & Trend Chart
                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        accentBorder = MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Weight Tracker", style = MaterialTheme.typography.titleLarge)
                            StatusBadge(
                                text = "Goal ${String.format("%.1f", profile?.targetWeightKg ?: 70.0)} kg",
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = String.format("%.1f kg", profile?.weightKg ?: 72.0),
                            style = MetricNumberTextStyle,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = weightInput,
                                onValueChange = { weightInput = it },
                                label = { Text("Weight (kg)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = weightNote,
                                onValueChange = { weightNote = it },
                                label = { Text("Note") },
                                singleLine = true,
                                modifier = Modifier.weight(1.3f)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        GradientButton(
                            text = "Log Weight Entry",
                            icon = Icons.Filled.MonitorWeight,
                            onClick = {
                                weightInput.toDoubleOrNull()?.let { w ->
                                    onLogWeight(w.coerceIn(25.0, 400.0), weightNote)
                                    weightNote = ""
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("log_weight_button")
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        if (filteredWeights.isNotEmpty()) {
                            TrendLineChart(
                                points = filteredWeights.map { it.dateKey.takeLast(5) to it.weightKg.toFloat() }
                            )
                        } else {
                            EmptyStateCard(
                                icon = Icons.Filled.MonitorWeight,
                                title = "No weight logs in last ${daysWindow}d",
                                subtitle = "Log your weight above to visualize your real trend line.",
                                illustration = FitTrackIllustrationType.PROGRESS
                            )
                        }
                    }
                }

                // Real Period Summary + Step Bars (No fabricated values)
                item {
                    FitTrackCard(modifier = Modifier.fillMaxWidth()) {
                        SectionHeader(title = "Period Activity (${daysWindow}D)")
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            StatCard(
                                title = "Steps",
                                value = "${filteredSteps.sumOf { it.steps }}",
                                subtitle = "${filteredSteps.size} days",
                                icon = Icons.Filled.MonitorWeight,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                            StatCard(
                                title = "GPS Distance",
                                value = String.format("%.1f km", filteredGps.sumOf { it.distanceKm }),
                                subtitle = "${filteredGps.size} sessions",
                                icon = Icons.Filled.EmojiEvents,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            StatCard(
                                title = "Workouts",
                                value = "${filteredWorkouts.size}",
                                subtitle = "Completed",
                                icon = Icons.Filled.CheckCircle,
                                tint = ext.success,
                                modifier = Modifier.weight(1f)
                            )
                            StatCard(
                                title = "Meals Logged",
                                value = "${filteredMeals.size}",
                                subtitle = "Nutrition records",
                                icon = Icons.Filled.AutoAwesome,
                                tint = ext.warning,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (filteredSteps.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(14.dp))
                            SimpleBarChart(
                                values = filteredSteps.takeLast(12).map { it.dateKey.takeLast(5) to it.steps.toFloat() }
                            )
                        }
                    }
                }

                // Body Measurements
                item {
                    FitTrackCard(modifier = Modifier.fillMaxWidth()) {
                        SectionHeader(title = "Body Measurements (cm)")
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = waistText,
                                onValueChange = { waistText = it },
                                label = { Text("Waist") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = chestText,
                                onValueChange = { chestText = it },
                                label = { Text("Chest") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = hipsText,
                                onValueChange = { hipsText = it },
                                label = { Text("Hips") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = armsText,
                                onValueChange = { armsText = it },
                                label = { Text("Arms") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = legsText,
                                onValueChange = { legsText = it },
                                label = { Text("Legs") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        GradientButton(
                            text = "Save Measurements",
                            onClick = {
                                onLogMeasurements(
                                    waistText.toDoubleOrNull() ?: 0.0,
                                    chestText.toDoubleOrNull() ?: 0.0,
                                    armsText.toDoubleOrNull() ?: 0.0,
                                    legsText.toDoubleOrNull() ?: 0.0,
                                    hipsText.toDoubleOrNull() ?: 0.0
                                )
                                waistText = ""; chestText = ""; armsText = ""; legsText = ""; hipsText = ""
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("save_measurements_button")
                        )
                        if (measurements.isNotEmpty()) {
                            val latest = measurements.first()
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Latest (${latest.dateKey}): Waist ${latest.waistCm}cm • Chest ${latest.chestCm}cm • Arms ${latest.armsCm}cm • Legs ${latest.legsCm}cm",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }

                // Private Progress Photos (Android Photo Picker)
                item {
                    FitTrackCard(modifier = Modifier.fillMaxWidth()) {
                        SectionHeader(title = "Private Progress Photos")
                        Spacer(modifier = Modifier.height(6.dp))
                        DisclaimerBanner(
                            text = "Selected via Android Photo Picker (no broad media permissions). Image URIs stay private to your device; notes sync to your account."
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = photoCaption,
                            onValueChange = { photoCaption = it },
                            label = { Text("Caption (optional)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        SecondaryButton(
                            text = "Select Progress Photo",
                            icon = Icons.Filled.PhotoLibrary,
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("add_progress_photo_button")
                        )
                    }
                }

                if (progressPhotos.isEmpty()) {
                    item {
                        EmptyStateCard(
                            icon = Icons.Filled.PhotoLibrary,
                            title = "No progress photos yet",
                            subtitle = "Add private visual check-ins alongside your weight log."
                        )
                    }
                } else {
                    items(progressPhotos, key = { it.id }) { photo ->
                        FitTrackCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                AsyncImage(
                                    model = photo.localUri,
                                    contentDescription = photo.caption,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(MaterialTheme.shapes.small)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(photo.caption.ifBlank { "Check-in" }, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        "${photo.dateKey} • ${String.format("%.1f", photo.weightKg)} kg",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = { onDeleteProgressPhoto(photo.id) }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete photo", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }
}

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
                        listOf("sedentary" to "Sedentary", "moderate" to "Moderate", "active" to "Active").forEach { (k, l) ->
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
                    String.format("%.2f km", walkKm),
                    "$steps steps",
                    "steps * (heightCm * 0.415 / 100) / 1000",
                    "Height-proportional stride length estimation."
                )
                CalculatorResultCard(
                    "8. Running Pace",
                    FitnessCalculator.formatPace(runPace),
                    String.format("%.1f km/h avg", (runDist / (runMin / 60.0))),
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChallengesScreen(
    uid: String,
    challenges: List<UserChallenge>,
    onUpdateChallenge: (UserChallenge, Double, Boolean) -> Unit,
    onBack: () -> Unit
) {
    val ext = LocalFitTrackColors.current
    val displayChallenges = remember(challenges, uid) {
        if (challenges.isNotEmpty()) challenges else FitnessCatalog.defaultChallenges(uid)
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.85f)),
                title = { Text("Fitness Challenges") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        AuroraAmbientBackground {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            FitTrackGeometricIllustration(
                                type = FitTrackIllustrationType.ACHIEVEMENTS,
                                size = 76.dp
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                StatusBadge(text = "MILESTONES", tint = ext.warning)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Weekly Milestones", style = MaterialTheme.typography.titleLarge)
                                Text(
                                    "Join active challenges and log real steps, hydration, or workouts.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                items(displayChallenges, key = { it.title }) { ch ->
                    val ratio = (ch.currentValue / ch.targetValue.coerceAtLeast(1.0)).toFloat().coerceIn(0f, 1f)
                    FitTrackCard(
                        modifier = Modifier.fillMaxWidth(),
                        accentBorder = if (ch.completed) ext.success else null
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(ch.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            if (ch.completed) {
                                StatusBadge(text = "COMPLETED", tint = ext.success)
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(ch.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(10.dp))
                        AuroraProgressBar(
                            label = "Progress",
                            valueText = "${ch.currentValue.roundToInt()} / ${ch.targetValue.roundToInt()} ${ch.unit}",
                            progress = ratio,
                            color = if (ch.completed) ext.success else MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            SecondaryButton(
                                text = if (ch.joined) "Joined ✓" else "Join",
                                onClick = { onUpdateChallenge(ch, ch.currentValue, !ch.joined) },
                                modifier = Modifier.weight(1f)
                            )
                            val stepIncrement = when (ch.category) {
                                "steps" -> 2500.0
                                "hydration" -> 1.0
                                "workouts" -> 1.0
                                else -> 2.0
                            }
                            GradientButton(
                                text = "Log +${stepIncrement.roundToInt()}",
                                icon = Icons.Filled.Add,
                                onClick = {
                                    onUpdateChallenge(ch, (ch.currentValue + stepIncrement).coerceAtMost(ch.targetValue), true)
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    profile: UserProfile?,
    onChangeTheme: (String) -> Unit,
    onChangeUnits: (String) -> Unit,
    onUpdateConsent: (Boolean, Boolean) -> Unit,
    onExportJson: suspend () -> String,
    onSignOut: () -> Unit,
    onDeleteAccountData: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var exportedPreview by remember { mutableStateOf<String?>(null) }
    var isExporting by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val sensorConsent = profile?.sensorConsentGranted ?: true
    val locationConsent = profile?.locationConsentGranted ?: true

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.85f)),
                title = { Text("Settings, Privacy & Data") },
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
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                FitTrackCard(modifier = Modifier.fillMaxWidth()) {
                    Text("Appearance (Aurora Motion)", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("dark" to "Dark (#080B14)", "light" to "Light (#F6F8FC)", "system" to "System").forEach { (key, label) ->
                            FilterChip(
                                selected = (profile?.themePreference ?: "dark") == key,
                                onClick = { onChangeTheme(key) },
                                label = { Text(label) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text("Measurement Units", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("metric" to "Metric (kg, cm, km)", "imperial" to "Imperial (lb, in, mi)").forEach { (key, label) ->
                            FilterChip(
                                selected = (profile?.unitSystem ?: "metric") == key,
                                onClick = { onChangeUnits(key) },
                                label = { Text(label) }
                            )
                        }
                    }
                }

                FitTrackCard(modifier = Modifier.fillMaxWidth()) {
                    Text("Privacy & Telemetry Controls", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Step Sensor Sync", style = MaterialTheme.typography.titleMedium)
                            Text("Count and sync daily hardware step telemetry", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = sensorConsent, onCheckedChange = { onUpdateConsent(it, locationConsent) })
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("GPS Location Sessions", style = MaterialTheme.typography.titleMedium)
                            Text("Record route coordinates during active workouts", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = locationConsent, onCheckedChange = { onUpdateConsent(sensorConsent, it) })
                    }
                }

                FitTrackCard(modifier = Modifier.fillMaxWidth()) {
                    Text("Data Export & Ownership", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(10.dp))
                    GradientButton(
                        text = "Export All Data (JSON)",
                        icon = Icons.Filled.Download,
                        loading = isExporting,
                        onClick = {
                            isExporting = true
                            scope.launch {
                                runCatching { onExportJson() }
                                    .onSuccess { json ->
                                        exportedPreview = json
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "application/json"
                                            putExtra(Intent.EXTRA_SUBJECT, "FitTrack Data Export")
                                            putExtra(Intent.EXTRA_TEXT, json)
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, "Export FitTrack JSON"))
                                    }
                                isExporting = false
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("export_data_button")
                    )
                    if (exportedPreview != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = exportedPreview!!.take(300) + if (exportedPreview!!.length > 300) "..." else "",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    SecondaryButton(
                        text = "Sign Out",
                        icon = Icons.AutoMirrored.Filled.Logout,
                        onClick = onSignOut,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sign_out_button")
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { showDeleteDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("delete_data_button")
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Delete All User Data")
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        ConfirmationModal(
            title = "Delete All FitTrack Data?",
            message = "This permanently deletes your profile, step history, GPS routes, meals, workouts, weight records, and AI chat history from your account. This cannot be undone.",
            confirmText = "Delete Permanently",
            isDestructive = true,
            onConfirm = {
                showDeleteDialog = false
                onDeleteAccountData()
            },
            onDismiss = { showDeleteDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AiCoachScreen(
    messages: List<AiChatMessage>,
    coachState: AsyncState<String>,
    onSendMessage: (String) -> Unit,
    onBack: () -> Unit
) {
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    val quickPrompts = listOf(
        "Review my daily nutrition & steps",
        "Suggest a 20-min recovery routine",
        "How can I boost daily protein?",
        "Give me 5km pacing tips"
    )

    LaunchedEffect(messages.size, coachState) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.85f)),
                title = { Text("AI Fitness Coach") },
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
                    .imePadding()
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(132.dp)
                                .clip(MaterialTheme.shapes.large)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_ai_coach_banner_1794739037230),
                                contentDescription = "AI Fitness Coach Banner",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(Color.Transparent, MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
                                        )
                                    )
                            )
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(14.dp)
                            ) {
                                StatusBadge(text = "AURORA COACH", tint = MaterialTheme.colorScheme.secondary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Personalized Coaching", style = MaterialTheme.typography.titleLarge)
                                Text(
                                    "Context-aware guidance from your steps, meals & goals",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    item {
                        DisclaimerBanner(text = stringResource(R.string.ai_disclaimer))
                    }

                    item {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            quickPrompts.forEach { prompt ->
                                AssistChip(
                                    onClick = { onSendMessage(prompt) },
                                    label = { Text(prompt) }
                                )
                            }
                        }
                    }

                    if (messages.isEmpty() && coachState is AsyncState.Idle) {
                        item {
                            EmptyStateCard(
                                icon = Icons.Filled.AutoAwesome,
                                title = "Ask your FitTrack Coach",
                                subtitle = "Get tailored workout tips, macro strategies, or pacing guidance.",
                                illustration = FitTrackIllustrationType.AI_COACH
                            )
                        }
                    } else {
                        items(messages, key = { it.id.ifBlank { it.text.hashCode().toString() } }) { msg ->
                            val isUser = msg.role == "user"
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                            ) {
                                Surface(
                                    shape = MaterialTheme.shapes.medium,
                                    color = if (isUser) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        LocalFitTrackColors.current.surfaceElevated
                                    },
                                    modifier = Modifier.fillMaxWidth(0.86f)
                                ) {
                                    Text(
                                        text = msg.text,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(14.dp)
                                    )
                                }
                            }
                        }
                    }

                    when (coachState) {
                        is AsyncState.Loading -> {
                            item {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                    Text("Coach is thinking...", style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                        is AsyncState.Streaming -> {
                            item {
                                Surface(
                                    shape = MaterialTheme.shapes.medium,
                                    color = LocalFitTrackColors.current.surfaceElevated,
                                    modifier = Modifier.fillMaxWidth(0.86f)
                                ) {
                                    Text(
                                        text = coachState.text,
                                        style = MaterialTheme.typography.bodyLarge,
                                        modifier = Modifier.padding(14.dp)
                                    )
                                }
                            }
                        }
                        is AsyncState.Error -> {
                            item {
                                ErrorBannerCard(message = coachState.message)
                            }
                        }
                        else -> Unit
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                    tonalElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            placeholder = { Text("Ask about training, macros, recovery...") },
                            maxLines = 3,
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("ai_coach_input")
                        )
                        IconButton(
                            onClick = {
                                val prompt = inputText.trim()
                                if (prompt.isNotBlank()) {
                                    onSendMessage(prompt)
                                    inputText = ""
                                }
                            },
                            enabled = coachState !is AsyncState.Loading && coachState !is AsyncState.Streaming,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(LocalFitTrackColors.current.auroraBrush)
                                .testTag("ai_coach_send_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send message",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}
