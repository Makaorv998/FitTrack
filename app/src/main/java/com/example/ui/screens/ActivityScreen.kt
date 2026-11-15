package com.example.ui.screens

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.GpsActivity
import com.example.data.model.StepRecord
import com.example.data.model.UserProfile
import com.example.domain.FitnessCalculator
import com.example.platform.GpsTrackerState
import com.example.platform.StepSensorState
import com.example.platform.TrackingStatus
import com.example.ui.branding.FitTrackIllustrationType
import com.example.ui.components.ActivityCard
import com.example.ui.components.AnimatedCounterText
import com.example.ui.components.AnimatedProgressRing
import com.example.ui.components.AuroraAmbientBackground
import com.example.ui.components.DisclaimerBanner
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.FitTrackCard
import com.example.ui.components.GlassCard
import com.example.ui.components.GradientButton
import com.example.ui.components.LocationPermissionRequester
import com.example.ui.components.ScreenHeader
import com.example.ui.components.SecondaryButton
import com.example.ui.components.SectionHeader
import com.example.ui.components.SegmentedControl
import com.example.ui.components.SimpleBarChart
import com.example.ui.components.StatusBadge
import com.example.ui.components.rememberLocationPermissionState
import com.example.ui.theme.HeroNumberTextStyle
import com.example.ui.theme.LocalFitTrackColors
import com.example.ui.theme.MetricNumberTextStyle
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val Int.sp get() = androidx.compose.ui.unit.TextUnit(this.toFloat(), androidx.compose.ui.unit.TextUnitType.Sp)

@Composable
fun ActivityScreen(
    profile: UserProfile?,
    stepSensor: StepSensorState,
    todaySteps: Long,
    stepRecords: List<StepRecord>,
    gpsState: GpsTrackerState,
    gpsHistory: List<GpsActivity>,
    onAddManualSteps: (Long) -> Unit,
    onSyncSteps: () -> Unit,
    onStartGps: (String) -> Unit,
    onPauseGps: () -> Unit,
    onResumeGps: () -> Unit,
    onFinishGps: () -> Unit,
    onDiscardGps: () -> Unit,
    onDeleteGpsActivity: (String) -> Unit,
    onOpenReadOnlyMap: (String?) -> Unit = {}
) {
    val ext = LocalFitTrackColors.current
    var selectedType by remember { mutableStateOf("running") }
    var startPendingPermission by remember { mutableStateOf(false) }
    var statsWindowIndex by remember { mutableIntStateOf(0) } // 0 = Weekly (7D), 1 = Monthly (30D)

    val stepGoal = (profile?.dailyStepGoal ?: 10000L).coerceAtLeast(1000L)
    val heightCm = profile?.heightCm ?: 172.0
    val weightKg = profile?.weightKg ?: 70.0
    val stepDistanceKm = FitnessCalculator.calculateWalkingDistanceKm(todaySteps, heightCm)
    val stepCalories = (todaySteps * 0.04) * (weightKg / 70.0)
    val activeMinutes = (todaySteps / 100L).toInt()

    val windowDays = if (statsWindowIndex == 0) 7 else 30
    val cutoffDateKey = remember(windowDays) {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -(windowDays - 1))
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
    }
    val cutoffMs = remember(windowDays) {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -(windowDays - 1))
        cal.timeInMillis
    }

    val windowStepRecords = remember(stepRecords, cutoffDateKey) {
        stepRecords.filter { it.dateKey >= cutoffDateKey }.sortedBy { it.dateKey }
    }
    val windowGps = remember(gpsHistory, cutoffMs) {
        gpsHistory.filter {
            val ts = if (it.startedAtMs > 0L) it.startedAtMs else (it.createdAt?.toDate()?.time ?: 0L)
            ts >= cutoffMs
        }
    }
    val walkingSessions = remember(gpsHistory) { gpsHistory.filter { it.activityType == "walking" } }
    val runningSessions = remember(gpsHistory) { gpsHistory.filter { it.activityType == "running" } }
    val cyclingSessions = remember(gpsHistory) { gpsHistory.filter { it.activityType == "cycling" } }

    val locationPermissionState = rememberLocationPermissionState { fine, coarse ->
        if ((fine || coarse) && startPendingPermission) {
            startPendingPermission = false
            onStartGps(selectedType)
        } else {
            startPendingPermission = false
        }
    }

    val pulseTransition = rememberInfiniteTransition(label = "gps_live_pulse")
    val liveDotColor by pulseTransition.animateColor(
        initialValue = ext.success,
        targetValue = ext.success.copy(alpha = 0.28f),
        animationSpec = infiniteRepeatable(tween(750), RepeatMode.Reverse),
        label = "dot_color"
    )
    val bottomClearance = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 104.dp

    AuroraAmbientBackground {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = bottomClearance),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                ScreenHeader(
                    title = "Activity",
                    subtitle = "Steps, outdoor workouts & route history"
                )
            }

            // 1. Daily Steps Hero Card
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    accentBorder = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            StatusBadge(
                                text = if (stepSensor.hardwareAvailable) "HARDWARE SENSOR" else "MANUAL STEP MODE",
                                tint = if (stepSensor.hardwareAvailable) ext.success else ext.warning
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            AnimatedCounterText(
                                targetValue = todaySteps.toInt(),
                                style = HeroNumberTextStyle,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "of $stepGoal daily goal",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = String.format(Locale.US, "%.2f km • %.0f kcal • %d min", stepDistanceKm, stepCalories, activeMinutes),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        AnimatedProgressRing(
                            progress = todaySteps.toFloat() / stepGoal.toFloat(),
                            size = 104.dp,
                            strokeWidth = 10.dp
                        ) {
                            Text(
                                text = "${((todaySteps.toFloat() / stepGoal.toFloat()) * 100).toInt().coerceAtMost(999)}%",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onAddManualSteps(500L) },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("add_500_steps_button")
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add 500")
                        }
                        OutlinedButton(
                            onClick = onSyncSteps,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("sync_steps_button")
                        ) {
                            Text("Sync to Cloud")
                        }
                    }
                }
            }

            // 2. Walking, Running & Cycling Activity Breakdown
            item {
                FitTrackCard(modifier = Modifier.fillMaxWidth()) {
                    SectionHeader(title = "Activity Breakdown")
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ModalitySummaryTile(
                            title = "Walking",
                            sessions = walkingSessions,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.weight(1f)
                        )
                        ModalitySummaryTile(
                            title = "Running",
                            sessions = runningSessions,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        ModalitySummaryTile(
                            title = "Cycling",
                            sessions = cyclingSessions,
                            tint = ext.success,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 3. Live GPS Session Recording Card (cleanly separated from Read-Only Map Screen)
            item {
                ActivityCard(
                    modifier = Modifier.fillMaxWidth(),
                    accentBorder = if (gpsState.status != TrackingStatus.IDLE) MaterialTheme.colorScheme.secondary else null
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SectionHeader(title = "Outdoor GPS Tracker")
                        if (gpsState.status == TrackingStatus.ACTIVE) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(liveDotColor))
                                Text("LIVE", style = MaterialTheme.typography.labelSmall, color = ext.success, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    if (gpsState.status == TrackingStatus.IDLE) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(
                                Triple("walking", "Walking", Icons.AutoMirrored.Filled.DirectionsWalk),
                                Triple("running", "Running", Icons.AutoMirrored.Filled.DirectionsRun),
                                Triple("cycling", "Cycling", Icons.AutoMirrored.Filled.DirectionsBike)
                            ).forEach { (key, label, icon) ->
                                FilterChip(
                                    selected = selectedType == key,
                                    onClick = { selectedType = key },
                                    label = { Text(label) },
                                    leadingIcon = { Icon(icon, contentDescription = label, modifier = Modifier.size(16.dp)) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            TelemetryStat("Distance", String.format(Locale.US, "%.2f km", gpsState.distanceKm))
                            TelemetryStat("Duration", FitnessCalculator.formatDuration(gpsState.elapsedSeconds))
                            TelemetryStat("Pace", FitnessCalculator.formatPace(gpsState.avgPaceMinPerKm))
                            TelemetryStat("Burn", "${gpsState.estimatedCalories.toInt()} kcal")
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    LocationPermissionRequester(
                        permissionState = locationPermissionState,
                        showReadyIndicator = gpsState.status == TrackingStatus.IDLE
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    when (gpsState.status) {
                        TrackingStatus.IDLE -> {
                            GradientButton(
                                text = "Start ${selectedType.replaceFirstChar { it.uppercase() }}",
                                icon = Icons.Filled.PlayArrow,
                                onClick = {
                                    if (locationPermissionState.hasAnyLocation) {
                                        onStartGps(selectedType)
                                    } else {
                                        startPendingPermission = true
                                        locationPermissionState.requestPermissions()
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("start_gps_button")
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            SecondaryButton(
                                text = "View Route Map & History",
                                icon = Icons.Filled.Map,
                                onClick = { onOpenReadOnlyMap(gpsHistory.firstOrNull()?.id) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("open_route_map_button")
                            )
                        }

                        TrackingStatus.ACTIVE, TrackingStatus.PAUSED -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                SecondaryButton(
                                    text = if (gpsState.status == TrackingStatus.ACTIVE) "Pause" else "Resume",
                                    icon = if (gpsState.status == TrackingStatus.ACTIVE) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    onClick = {
                                        if (gpsState.status == TrackingStatus.ACTIVE) onPauseGps() else onResumeGps()
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("pause_resume_gps_button")
                                )
                                Button(
                                    onClick = onFinishGps,
                                    colors = ButtonDefaults.buttonColors(containerColor = ext.success),
                                    shape = MaterialTheme.shapes.medium,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(52.dp)
                                        .testTag("finish_gps_button")
                                ) {
                                    Icon(Icons.Filled.Stop, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Finish", style = MaterialTheme.typography.titleMedium)
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = onDiscardGps,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("discard_gps_button")
                            ) {
                                Text("Discard Session", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }

            // 4. Weekly & Monthly Statistics
            item {
                FitTrackCard(modifier = Modifier.fillMaxWidth()) {
                    SectionHeader(title = "Weekly & Monthly Statistics")
                    Spacer(modifier = Modifier.height(10.dp))
                    SegmentedControl(
                        options = listOf("Weekly (7D)", "Monthly (30D)"),
                        selectedIndex = statsWindowIndex,
                        onSelect = { statsWindowIndex = it }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    val avgSteps = if (windowStepRecords.isNotEmpty()) {
                        windowStepRecords.sumOf { it.steps } / windowStepRecords.size
                    } else todaySteps
                    val totalDist = windowGps.sumOf { it.distanceKm } +
                        windowStepRecords.sumOf { it.distanceKm }
                    val totalBurn = windowGps.sumOf { it.caloriesBurned } +
                        windowStepRecords.sumOf { it.caloriesBurned }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TelemetryStat("Avg Steps", "$avgSteps")
                        TelemetryStat("Distance", String.format(Locale.US, "%.1f km", totalDist))
                        TelemetryStat("Burned", "${totalBurn.toInt()} kcal")
                        TelemetryStat("Sessions", "${windowGps.size}")
                    }
                    if (windowStepRecords.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        val chartData = windowStepRecords.takeLast(12).map {
                            it.dateKey.takeLast(5) to it.steps.toFloat()
                        }
                        SimpleBarChart(values = chartData)
                    }
                }
            }

            item {
                DisclaimerBanner(
                    text = "GPS distance is tracked independently from step-estimated distance so sessions are never double-counted."
                )
            }

            item {
                SectionHeader(
                    title = "GPS Activity History (${gpsHistory.size})",
                    actionLabel = if (gpsHistory.isNotEmpty()) "Open Map" else null,
                    onAction = if (gpsHistory.isNotEmpty()) {
                        { onOpenReadOnlyMap(gpsHistory.firstOrNull()?.id) }
                    } else null
                )
            }

            if (gpsHistory.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.AutoMirrored.Filled.DirectionsRun,
                        title = "No GPS sessions yet",
                        subtitle = "Start a walk, run, or cycle above to record your first route.",
                        illustration = FitTrackIllustrationType.RUNNING
                    )
                }
            } else {
                items(gpsHistory, key = { it.id }) { session ->
                    FitTrackCard(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onOpenReadOnlyMap(session.id) }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(width = 76.dp, height = 64.dp)
                                    .clip(MaterialTheme.shapes.small)
                                    .background(ext.backgroundSecondary)
                            ) {
                                ReadOnlyRouteVectorCanvas(
                                    points = session.routePoints,
                                    startColor = ext.success,
                                    finishColor = MaterialTheme.colorScheme.primary,
                                    lineColor = MaterialTheme.colorScheme.secondary
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    StatusBadge(
                                        text = session.activityType.uppercase(),
                                        tint = MaterialTheme.colorScheme.secondary
                                    )
                                    Text(
                                        text = SimpleDateFormat("MMM d", Locale.US).format(Date(session.startedAtMs)),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "${String.format(Locale.US, "%.2f", session.distanceKm)} km • ${FitnessCalculator.formatDuration(session.durationSeconds)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Pace ${FitnessCalculator.formatPace(session.avgPaceMinPerKm)} • ${session.caloriesBurned.toInt()} kcal",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { onOpenReadOnlyMap(session.id) }) {
                                Icon(
                                    Icons.Filled.Route,
                                    contentDescription = "View route on map",
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                            }
                            IconButton(onClick = { onDeleteGpsActivity(session.id) }) {
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = "Delete activity",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModalitySummaryTile(
    title: String,
    sessions: List<GpsActivity>,
    tint: Color,
    modifier: Modifier = Modifier
) {
    val totalKm = sessions.sumOf { it.distanceKm }
    val totalCals = sessions.sumOf { it.caloriesBurned }.toInt()
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .background(tint.copy(alpha = 0.08f))
            .padding(10.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(title, style = MaterialTheme.typography.labelLarge, color = tint, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = String.format(Locale.US, "%.1f km", totalKm),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "${sessions.size}x • $totalCals kcal",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TelemetryStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MetricNumberTextStyle.copy(fontSize = 17.sp), fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
