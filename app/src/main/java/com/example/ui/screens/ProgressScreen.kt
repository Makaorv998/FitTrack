package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.BodyMeasurement
import com.example.data.model.GpsActivity
import com.example.data.model.MealLog
import com.example.data.model.ProgressPhotoNote
import com.example.data.model.StepRecord
import com.example.data.model.UserProfile
import com.example.data.model.WeightRecord
import com.example.data.model.WorkoutSessionLog
import com.example.domain.FitnessCalculator
import com.example.domain.UnitFormatter
import com.example.ui.branding.FitTrackIllustrationType
import com.example.ui.components.AuroraAmbientBackground
import com.example.ui.components.DisclaimerBanner
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.FitTrackCard
import com.example.ui.components.GlassCard
import com.example.ui.components.GradientButton
import com.example.ui.components.SecondaryButton
import com.example.ui.components.SectionHeader
import com.example.ui.components.SegmentedControl
import com.example.ui.components.SimpleBarChart
import com.example.ui.components.StatCard
import com.example.ui.components.StatusBadge
import com.example.ui.components.TrendLineChart
import com.example.ui.theme.LocalFitTrackColors
import com.example.ui.theme.MetricNumberTextStyle
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

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
    val unitSystem = profile?.unitSystem ?: "metric"
    val weightUnit = UnitFormatter.weightUnitLabel(unitSystem)
    val lengthUnit = UnitFormatter.heightInputLabel(unitSystem)
    var weightInput by remember(profile?.weightKg, unitSystem) {
        val displayW = UnitFormatter.kgToDisplayWeight(profile?.weightKg ?: 72.0, unitSystem)
        mutableStateOf(String.format(Locale.US, "%.1f", displayW))
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
    val bmi = FitnessCalculator.calculateBmi(profile?.weightKg ?: 72.0, profile?.heightCm ?: 175.0)

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
                item {
                    SegmentedControl(
                        options = listOf("7D", "30D", "90D", "1Y"),
                        selectedIndex = rangeIndex,
                        onSelect = { rangeIndex = it }
                    )
                }

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
                                text = "Goal ${UnitFormatter.formatWeight(profile?.targetWeightKg ?: 70.0, unitSystem)} • BMI ${bmi.primaryValue}",
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = UnitFormatter.formatWeight(profile?.weightKg ?: 72.0, unitSystem),
                            style = MetricNumberTextStyle,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = weightInput,
                                onValueChange = { weightInput = it.filter { c -> c.isDigit() || c == '.' } },
                                label = { Text("Weight ($weightUnit)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("progress_weight_input")
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
                                    val kg = UnitFormatter.inputWeightToKg(w, unitSystem).coerceIn(25.0, 400.0)
                                    onLogWeight(kg, weightNote)
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
                                points = filteredWeights.map {
                                    it.dateKey.takeLast(5) to UnitFormatter.kgToDisplayWeight(it.weightKg, unitSystem).toFloat()
                                }
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
                                value = UnitFormatter.formatDistance(filteredGps.sumOf { it.distanceKm }, unitSystem, decimals = 1),
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

                item {
                    FitTrackCard(modifier = Modifier.fillMaxWidth()) {
                        SectionHeader(title = "Body Measurements ($lengthUnit)")
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = waistText,
                                onValueChange = { waistText = it.filter { c -> c.isDigit() || c == '.' } },
                                label = { Text("Waist") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = chestText,
                                onValueChange = { chestText = it.filter { c -> c.isDigit() || c == '.' } },
                                label = { Text("Chest") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = hipsText,
                                onValueChange = { hipsText = it.filter { c -> c.isDigit() || c == '.' } },
                                label = { Text("Hips") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = armsText,
                                onValueChange = { armsText = it.filter { c -> c.isDigit() || c == '.' } },
                                label = { Text("Arms") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = legsText,
                                onValueChange = { legsText = it.filter { c -> c.isDigit() || c == '.' } },
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
                                    UnitFormatter.inputHeightToCm(waistText.toDoubleOrNull() ?: 0.0, unitSystem),
                                    UnitFormatter.inputHeightToCm(chestText.toDoubleOrNull() ?: 0.0, unitSystem),
                                    UnitFormatter.inputHeightToCm(armsText.toDoubleOrNull() ?: 0.0, unitSystem),
                                    UnitFormatter.inputHeightToCm(legsText.toDoubleOrNull() ?: 0.0, unitSystem),
                                    UnitFormatter.inputHeightToCm(hipsText.toDoubleOrNull() ?: 0.0, unitSystem)
                                )
                                waistText = ""; chestText = ""; armsText = ""; legsText = ""; hipsText = ""
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("save_measurements_button")
                        )
                        if (measurements.isNotEmpty()) {
                            val latest = measurements.first()
                            fun fmtMeasure(cm: Double) = String.format(Locale.US, "%.1f", UnitFormatter.cmToDisplayHeight(cm, unitSystem))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Latest (${latest.dateKey}): Waist ${fmtMeasure(latest.waistCm)}$lengthUnit • Chest ${fmtMeasure(latest.chestCm)}$lengthUnit • Arms ${fmtMeasure(latest.armsCm)}$lengthUnit • Legs ${fmtMeasure(latest.legsCm)}$lengthUnit",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }

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
                                        "${photo.dateKey} • ${String.format(Locale.US, "%.1f", photo.weightKg)} kg",
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
