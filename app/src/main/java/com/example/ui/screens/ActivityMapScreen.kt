package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import com.example.domain.FitnessCalculator
import com.example.ui.branding.FitTrackIllustrationType
import com.example.ui.components.AuroraAmbientBackground
import com.example.ui.components.DisclaimerBanner
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.FitTrackCard
import com.example.ui.components.GlassCard
import com.example.ui.components.LocationPermissionRequester
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusBadge
import com.example.ui.components.rememberLocationPermissionState
import com.example.ui.theme.LocalFitTrackColors
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * STRICTLY READ-ONLY GPS Route Visualization & History Map Screen.
 * Does NOT contain Start/Stop activity buttons, tracking initiation, or turn-by-turn directions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityMapScreen(
    gpsHistory: List<GpsActivity>,
    initialActivityId: String? = null,
    onBack: () -> Unit
) {
    val ext = LocalFitTrackColors.current
    val orderedHistory = remember(gpsHistory) {
        gpsHistory.sortedByDescending { it.startedAtMs }
    }
    var selectedId by remember(initialActivityId, orderedHistory) {
        mutableStateOf(
            initialActivityId?.takeIf { id -> orderedHistory.any { it.id == id } }
                ?: orderedHistory.firstOrNull()?.id
        )
    }
    val selectedSession = remember(selectedId, orderedHistory) {
        orderedHistory.firstOrNull { it.id == selectedId } ?: orderedHistory.firstOrNull()
    }

    val locationPermissionState = rememberLocationPermissionState { _, _ -> }
    val hasMapsApiKey = remember {
        BuildConfig.MAPS_API_KEY.isNotBlank() && BuildConfig.MAPS_API_KEY != "MY_MAPS_API_KEY"
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.85f)),
                title = { Text("Route Map & History") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("map_back_button")) {
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
                    .padding(innerPadding)
                    .testTag("read_only_map_screen"),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    DisclaimerBanner(
                        text = "Read-only route visualization. Start or finish new outdoor GPS sessions from the Activity tab."
                    )
                }

                // Location Permission Status (displays current location dot when granted)
                item {
                    LocationPermissionRequester(
                        permissionState = locationPermissionState,
                        showReadyIndicator = true
                    )
                }

                // Route Selector Chips when saved sessions exist
                if (orderedHistory.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            orderedHistory.forEach { session ->
                                val label = "${session.activityType.replaceFirstChar { it.uppercase() }} • ${String.format(Locale.US, "%.2f", session.distanceKm)} km"
                                FilterChip(
                                    selected = session.id == selectedSession?.id,
                                    onClick = { selectedId = session.id },
                                    label = { Text(label) },
                                    leadingIcon = {
                                        Icon(Icons.Filled.Route, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                )
                            }
                        }
                    }
                }

                // Main Read-Only Map / Empty State Visualization
                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        accentBorder = MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f)
                    ) {
                        val routePoints = selectedSession?.routePoints.orEmpty()
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(240.dp)
                                .clip(MaterialTheme.shapes.medium)
                                .background(ext.backgroundSecondary)
                        ) {
                            if (routePoints.isEmpty() && !locationPermissionState.hasAnyLocation) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(20.dp),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Map,
                                        contentDescription = "Empty Map",
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(44.dp)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "No Recorded GPS Route Yet",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Grant location permission to view your current position on the map, or record an outdoor walk, run, or cycle in Activity.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else if (hasMapsApiKey) {
                                val focusPoint = routePoints.firstOrNull()
                                val targetLatLng = if (focusPoint != null) {
                                    LatLng(focusPoint.lat, focusPoint.lng)
                                } else {
                                    LatLng(0.0, 0.0)
                                }
                                val cameraState = rememberCameraPositionState(key = selectedSession?.id ?: "user_loc") {
                                    position = CameraPosition.fromLatLngZoom(targetLatLng, if (focusPoint != null) 15f else 2f)
                                }
                                GoogleMap(
                                    modifier = Modifier.fillMaxSize(),
                                    cameraPositionState = cameraState,
                                    properties = MapProperties(isMyLocationEnabled = locationPermissionState.hasAnyLocation),
                                    uiSettings = MapUiSettings(
                                        myLocationButtonEnabled = locationPermissionState.hasAnyLocation,
                                        zoomControlsEnabled = true,
                                        mapToolbarEnabled = false
                                    )
                                ) {
                                    val polyPoints = routePoints.map { LatLng(it.lat, it.lng) }
                                    if (polyPoints.size >= 2) {
                                        Polyline(
                                            points = polyPoints,
                                            color = MaterialTheme.colorScheme.secondary,
                                            width = 12f
                                        )
                                    }
                                    polyPoints.firstOrNull()?.let { start ->
                                        Marker(
                                            state = MarkerState(position = start),
                                            title = "Start",
                                            snippet = selectedSession?.activityType?.replaceFirstChar { it.uppercase() }
                                        )
                                    }
                                    if (polyPoints.size >= 2) {
                                        polyPoints.lastOrNull()?.let { finish ->
                                            Marker(
                                                state = MarkerState(position = finish),
                                                title = "Finish",
                                                snippet = selectedSession?.let { String.format(Locale.US, "%.2f km", it.distanceKm) }
                                            )
                                        }
                                    }
                                }
                            } else {
                                ReadOnlyRouteVectorCanvas(
                                    points = routePoints,
                                    startColor = ext.success,
                                    finishColor = MaterialTheme.colorScheme.primary,
                                    lineColor = MaterialTheme.colorScheme.secondary
                                )
                                Surface(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(10.dp),
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
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
                                            text = if (routePoints.isEmpty()) {
                                                "Location ready • No saved route selected"
                                            } else {
                                                "Route preview (${routePoints.size} GPS points)"
                                            },
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                            }
                        }

                        if (selectedSession != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                MarkerLegend(color = ext.success, label = "Start Marker", icon = Icons.Filled.MyLocation)
                                MarkerLegend(color = MaterialTheme.colorScheme.primary, label = "Finish Marker", icon = Icons.Filled.Flag)
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                StatusBadge(
                                    text = selectedSession.activityType.uppercase(),
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                                Text(
                                    text = formatSessionDate(selectedSession.startedAtMs),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                MapMetricColumn("Distance", String.format(Locale.US, "%.2f km", selectedSession.distanceKm))
                                MapMetricColumn("Duration", FitnessCalculator.formatDuration(selectedSession.durationSeconds))
                                MapMetricColumn("Pace", FitnessCalculator.formatPace(selectedSession.avgPaceMinPerKm))
                                MapMetricColumn("Calories", "${selectedSession.caloriesBurned.toInt()} kcal")
                            }
                        }
                    }
                }

                // Saved Route History List
                item {
                    SectionHeader(title = "Saved Route History (${orderedHistory.size})")
                }

                if (orderedHistory.isEmpty()) {
                    item {
                        EmptyStateCard(
                            icon = Icons.Filled.Route,
                            title = "No saved routes",
                            subtitle = "Completed GPS walks, runs, and cycling routes will appear here with Start and Finish markers.",
                            illustration = FitTrackIllustrationType.RUNNING
                        )
                    }
                } else {
                    items(orderedHistory, key = { it.id }) { session ->
                        val isSelected = session.id == selectedSession?.id
                        FitTrackCard(
                            modifier = Modifier.fillMaxWidth(),
                            accentBorder = if (isSelected) MaterialTheme.colorScheme.secondary else null,
                            onClick = { selectedId = session.id }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${session.activityType.replaceFirstChar { it.uppercase() }} • ${String.format(Locale.US, "%.2f", session.distanceKm)} km",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${FitnessCalculator.formatDuration(session.durationSeconds)} • ${FitnessCalculator.formatPace(session.avgPaceMinPerKm)} • ${session.routePoints.size} pts",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                StatusBadge(
                                    text = if (isSelected) "VIEWING" else "VIEW ROUTE",
                                    tint = if (isSelected) ext.success else MaterialTheme.colorScheme.primary
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
private fun MapMetricColumn(label: String, value: String) {
    Column {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun MarkerLegend(color: Color, label: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(12.dp))
        }
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun formatSessionDate(startedAtMs: Long): String {
    if (startedAtMs <= 0L) return "Recorded session"
    return SimpleDateFormat("MMM d, yyyy • HH:mm", Locale.US).format(Date(startedAtMs))
}

@Composable
fun ReadOnlyRouteVectorCanvas(
    points: List<LatLngPoint>,
    startColor: Color,
    finishColor: Color,
    lineColor: Color
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val stepX = size.width / 6f
        val stepY = size.height / 5f
        for (i in 1..5) {
            drawLine(Color.Gray.copy(alpha = 0.14f), Offset(i * stepX, 0f), Offset(i * stepX, size.height), 1f)
        }
        for (j in 1..4) {
            drawLine(Color.Gray.copy(alpha = 0.14f), Offset(0f, j * stepY), Offset(size.width, j * stepY), 1f)
        }

        if (points.size >= 2) {
            val minLat = points.minOf { it.lat }
            val maxLat = points.maxOf { it.lat }
            val minLng = points.minOf { it.lng }
            val maxLng = points.maxOf { it.lng }
            val latSpan = (maxLat - minLat).coerceAtLeast(0.0005)
            val lngSpan = (maxLng - minLng).coerceAtLeast(0.0005)
            val pad = 42f
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
                brush = Brush.linearGradient(listOf(startColor, lineColor, finishColor)),
                style = Stroke(width = 9f, cap = StrokeCap.Round)
            )
            // Start marker
            drawCircle(color = startColor.copy(alpha = 0.25f), radius = 18f, center = startPt)
            drawCircle(color = startColor, radius = 9f, center = startPt)
            // Finish marker
            drawCircle(color = finishColor.copy(alpha = 0.25f), radius = 18f, center = endPt)
            drawCircle(color = finishColor, radius = 9f, center = endPt)
        } else if (points.size == 1) {
            val center = Offset(size.width / 2f, size.height / 2f)
            drawCircle(color = startColor.copy(alpha = 0.25f), radius = 22f, center = center)
            drawCircle(color = startColor, radius = 9f, center = center)
        }
    }
}
