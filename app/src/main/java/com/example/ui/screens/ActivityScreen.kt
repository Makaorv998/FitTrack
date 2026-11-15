package com.example.ui.screens

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.data.model.GpsActivity
import com.example.data.model.LatLngPoint
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
import com.example.ui.components.SimpleBarChart
import com.example.ui.components.StatusBadge
import com.example.ui.components.rememberLocationPermissionState
import com.example.ui.theme.HeroNumberTextStyle
import com.example.ui.theme.LocalFitTrackColors
import com.example.ui.theme.MetricNumberTextStyle
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState

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
    onDeleteGpsActivity: (String) -> Unit
) {
    val ext = LocalFitTrackColors.current
    var selectedType by remember { mutableStateOf("running") }
    var startPendingPermission by remember { mutableStateOf(false) }

    val stepGoal = (profile?.dailyStepGoal ?: 10000L).coerceAtLeast(1000L)
    val heightCm = profile?.heightCm ?: 172.0
    val weightKg = profile?.weightKg ?: 70.0
    val stepDistanceKm = FitnessCalculator.calculateWalkingDistanceKm(todaySteps, heightCm)
    val stepCalories = (todaySteps * 0.04) * (weightKg / 70.0)
    val activeMinutes = (todaySteps / 100L).toInt()

    val locationPermissionState = rememberLocationPermissionState { fine, coarse ->
        if ((fine || coarse) && startPendingPermission) {
            startPendingPermission = false
            onStartGps(selectedType)
        } else {
            startPendingPermission = false
        }
    }

    val hasMapsApiKey = remember {
        BuildConfig.MAPS_API_KEY.isNotBlank() && BuildConfig.MAPS_API_KEY != "MY_MAPS_API_KEY"
    }

    val pulseTransition = rememberInfiniteTransition(label = "gps_live_pulse")
    val liveDotColor by pulseTransition.animateColor(
        initialValue = ext.success,
        targetValue = ext.success.copy(alpha = 0.28f),
        animationSpec = infiniteRepeatable(tween(750), RepeatMode.Reverse),
        label = "dot_color"
    )

    AuroraAmbientBackground {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 112.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                ScreenHeader(
                    title = "Activity & GPS",
                    subtitle = "Motion sensors & outdoor route telemetry"
                )
            }

            // Large Step Counter Hero Card
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
                                text = String.format("%.2f km • %.0f kcal • %d min", stepDistanceKm, stepCalories, activeMinutes),
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

            // Live GPS Session Card
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
                        SectionHeader(title = "GPS Outdoor Session")
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
                        Spacer(modifier = Modifier.height(12.dp))
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            TelemetryStat("Distance", String.format("%.2f km", gpsState.distanceKm))
                            TelemetryStat("Duration", FitnessCalculator.formatDuration(gpsState.elapsedSeconds))
                            TelemetryStat("Pace", FitnessCalculator.formatPace(gpsState.avgPaceMinPerKm))
                            TelemetryStat("Burn", "${gpsState.estimatedCalories.toInt()} kcal")
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Live Map / Tactical Vector Route View
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .background(ext.backgroundSecondary)
                    ) {
                        if (hasMapsApiKey) {
                            val lastPoint = gpsState.routePoints.lastOrNull()
                                ?: gpsHistory.firstOrNull()?.routePoints?.lastOrNull()
                            val targetLatLng = if (lastPoint != null) LatLng(lastPoint.lat, lastPoint.lng) else LatLng(37.7749, -122.4194)
                            val cameraState = rememberCameraPositionState {
                                position = CameraPosition.fromLatLngZoom(targetLatLng, 15f)
                            }
                            GoogleMap(
                                modifier = Modifier.fillMaxSize(),
                                cameraPositionState = cameraState,
                                properties = MapProperties(isMyLocationEnabled = locationPermissionState.hasAnyLocation)
                            ) {
                                val polyPoints = gpsState.routePoints.map { LatLng(it.lat, it.lng) }
                                if (polyPoints.size >= 2) {
                                    Polyline(
                                        points = polyPoints,
                                        color = MaterialTheme.colorScheme.secondary,
                                        width = 10f
                                    )
                                }
                                polyPoints.firstOrNull()?.let {
                                    Marker(state = MarkerState(position = it), title = "Start")
                                }
                                polyPoints.lastOrNull()?.let {
                                    Marker(state = MarkerState(position = it), title = "Current")
                                }
                            }
                        } else {
                            val previewPoints = gpsState.routePoints.ifEmpty {
                                gpsHistory.firstOrNull()?.routePoints.orEmpty()
                            }
                            TacticalRouteCanvas(
                                points = previewPoints,
                                lineColor = MaterialTheme.colorScheme.secondary,
                                accentColor = MaterialTheme.colorScheme.primary
                            )
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(10.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f),
                                shape = MaterialTheme.shapes.small
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Filled.LocationOn,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Add MAPS_API_KEY in AI Studio Secrets for Google Maps tiles",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    LocationPermissionRequester(
                        permissionState = locationPermissionState,
                        showReadyIndicator = gpsState.status == TrackingStatus.IDLE
                    )
                    Spacer(modifier = Modifier.height(10.dp))
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

            // Weekly Step History Chart (real synced records)
            if (stepRecords.isNotEmpty()) {
                item {
                    FitTrackCard(modifier = Modifier.fillMaxWidth()) {
                        SectionHeader(title = "Weekly Step History")
                        Spacer(modifier = Modifier.height(10.dp))
                        val chartData = stepRecords.sortedBy { it.dateKey }.takeLast(7).map {
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
                SectionHeader(title = "GPS Activity History (${gpsHistory.size})")
            }

            if (gpsHistory.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.AutoMirrored.Filled.DirectionsRun,
                        title = "No GPS sessions yet",
                        subtitle = "Start a walk, run, or cycle above to map your first route.",
                        illustration = FitTrackIllustrationType.RUNNING
                    )
                }
            } else {
                items(gpsHistory, key = { it.id }) { session ->
                    FitTrackCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                StatusBadge(
                                    text = session.activityType.uppercase(),
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "${String.format("%.2f", session.distanceKm)} km • ${FitnessCalculator.formatDuration(session.durationSeconds)} • ${session.caloriesBurned.toInt()} kcal",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "Avg Pace: ${FitnessCalculator.formatPace(session.avgPaceMinPerKm)} • ${session.routePoints.size} GPS points",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
private fun TelemetryStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MetricNumberTextStyle.copy(fontSize = 18.sp), fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private val Int.sp get() = androidx.compose.ui.unit.TextUnit(this.toFloat(), androidx.compose.ui.unit.TextUnitType.Sp)

@Composable
private fun TacticalRouteCanvas(
    points: List<LatLngPoint>,
    lineColor: Color,
    accentColor: Color
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val stepX = size.width / 6f
        val stepY = size.height / 4f
        for (i in 1..5) {
            drawLine(Color.Gray.copy(alpha = 0.14f), Offset(i * stepX, 0f), Offset(i * stepX, size.height), 1f)
        }
        for (j in 1..3) {
            drawLine(Color.Gray.copy(alpha = 0.14f), Offset(0f, j * stepY), Offset(size.width, j * stepY), 1f)
        }

        if (points.size >= 2) {
            val minLat = points.minOf { it.lat }
            val maxLat = points.maxOf { it.lat }
            val minLng = points.minOf { it.lng }
            val maxLng = points.maxOf { it.lng }
            val latSpan = (maxLat - minLat).coerceAtLeast(0.0005)
            val lngSpan = (maxLng - minLng).coerceAtLeast(0.0005)
            val pad = 34f
            val w = size.width - pad * 2
            val h = size.height - pad * 2

            val path = Path()
            var startPt = Offset.Zero
            var endPt = Offset.Zero
            points.forEachIndexed { idx, p ->
                val x = pad + (((p.lng - minLng) / lngSpan) * w).toFloat()
                val y = pad + ((1.0 - (p.lat - minLat) / latSpan) * h).toFloat()
                if (idx == 0) {
                    startPt = Offset(x, y)
                    path.moveTo(x, y)
                } else {
                    endPt = Offset(x, y)
                    path.lineTo(x, y)
                }
            }
            drawPath(
                path = path,
                brush = Brush.linearGradient(listOf(accentColor, lineColor)),
                style = Stroke(width = 8f, cap = StrokeCap.Round)
            )
            drawCircle(color = accentColor, radius = 9f, center = startPt)
            drawCircle(color = lineColor, radius = 9f, center = endPt)
        } else {
            drawCircle(
                color = lineColor.copy(alpha = 0.25f),
                radius = 28f,
                center = Offset(size.width / 2f, size.height / 2f)
            )
            drawCircle(
                color = lineColor,
                radius = 9f,
                center = Offset(size.width / 2f, size.height / 2f)
            )
        }
    }
}
