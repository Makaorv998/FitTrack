package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.domain.FitnessCalculator
import com.example.platform.RestTimerSettings
import com.example.ui.theme.LocalFitTrackColors

/**
 * Custom anatomical front/back athletic muscle group diagram highlighting
 * primary target muscles in Aurora Cyan and secondary synergists in Aurora Purple.
 */
@Composable
fun MuscleDiagramView(
    primaryMuscle: String,
    secondaryMuscles: List<String> = emptyList(),
    modifier: Modifier = Modifier,
    height: Dp = 156.dp
) {
    val ext = LocalFitTrackColors.current
    val primaryTint = MaterialTheme.colorScheme.secondary
    val secondaryTint = MaterialTheme.colorScheme.primary
    val baseBody = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.22f)
    val outline = ext.border

    fun colorFor(region: String): Color {
        val norm = region.lowercase()
        val isFullBody = primaryMuscle.equals("Full Body", ignoreCase = true)
        return when {
            isFullBody || primaryMuscle.lowercase().contains(norm) || norm == primaryMuscle.lowercase() ->
                primaryTint.copy(alpha = 0.90f)
            secondaryMuscles.any { it.lowercase().contains(norm) || norm.contains(it.lowercase()) } ->
                secondaryTint.copy(alpha = 0.65f)
            else -> baseBody
        }
    }

    FitTrackCard(modifier = modifier, elevated = true) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Target Anatomy",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                LegendDot(label = primaryMuscle.ifBlank { "Primary" }, color = primaryTint)
                if (secondaryMuscles.isNotEmpty()) {
                    LegendDot(label = "Secondary", color = secondaryTint)
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
        ) {
            // Left: Front Silhouette, Right: Back Silhouette
            val halfW = size.width / 2f
            val h = size.height

            // Front figure center
            val fc = halfW * 0.5f
            drawFigureBase(fc, h, baseBody, outline)
            // Front Shoulders
            val shoulderColor = colorFor("shoulders")
            drawOval(shoulderColor, Offset(fc - 32f, h * 0.21f), Size(18f, 16f))
            drawOval(shoulderColor, Offset(fc + 14f, h * 0.21f), Size(18f, 16f))
            // Chest
            val chestColor = colorFor("chest")
            drawRoundRect(chestColor, Offset(fc - 20f, h * 0.23f), Size(19f, 20f), CornerRadius(6f, 6f))
            drawRoundRect(chestColor, Offset(fc + 1f, h * 0.23f), Size(19f, 20f), CornerRadius(6f, 6f))
            // Abs
            val absColor = colorFor("abs")
            drawRoundRect(absColor, Offset(fc - 13f, h * 0.37f), Size(26f, 28f), CornerRadius(6f, 6f))
            // Biceps
            val bicepColor = colorFor("biceps")
            drawRoundRect(bicepColor, Offset(fc - 36f, h * 0.30f), Size(11f, 22f), CornerRadius(5f, 5f))
            drawRoundRect(bicepColor, Offset(fc + 25f, h * 0.30f), Size(11f, 22f), CornerRadius(5f, 5f))
            // Forearms
            val forearmColor = colorFor("forearms")
            drawRoundRect(forearmColor, Offset(fc - 38f, h * 0.44f), Size(9f, 20f), CornerRadius(4f, 4f))
            drawRoundRect(forearmColor, Offset(fc + 29f, h * 0.44f), Size(9f, 20f), CornerRadius(4f, 4f))
            // Quads
            val quadColor = colorFor("quadriceps")
            drawRoundRect(quadColor, Offset(fc - 20f, h * 0.56f), Size(17f, 34f), CornerRadius(7f, 7f))
            drawRoundRect(quadColor, Offset(fc + 3f, h * 0.56f), Size(17f, 34f), CornerRadius(7f, 7f))

            // Back figure center
            val bc = halfW + halfW * 0.5f
            drawFigureBase(bc, h, baseBody, outline)
            // Back Shoulders
            drawOval(shoulderColor, Offset(bc - 32f, h * 0.21f), Size(18f, 16f))
            drawOval(shoulderColor, Offset(bc + 14f, h * 0.21f), Size(18f, 16f))
            // Lats / Upper Back
            val backColor = colorFor("back")
            drawRoundRect(backColor, Offset(bc - 21f, h * 0.22f), Size(42f, 34f), CornerRadius(10f, 10f))
            // Triceps
            val triColor = colorFor("triceps")
            drawRoundRect(triColor, Offset(bc - 36f, h * 0.30f), Size(11f, 22f), CornerRadius(5f, 5f))
            drawRoundRect(triColor, Offset(bc + 25f, h * 0.30f), Size(11f, 22f), CornerRadius(5f, 5f))
            // Glutes
            val gluteColor = colorFor("glutes")
            drawRoundRect(gluteColor, Offset(bc - 20f, h * 0.47f), Size(19f, 18f), CornerRadius(8f, 8f))
            drawRoundRect(gluteColor, Offset(bc + 1f, h * 0.47f), Size(19f, 18f), CornerRadius(8f, 8f))
            // Hamstrings
            val hamColor = colorFor("hamstrings")
            drawRoundRect(hamColor, Offset(bc - 20f, h * 0.58f), Size(16f, 28f), CornerRadius(6f, 6f))
            drawRoundRect(hamColor, Offset(bc + 4f, h * 0.58f), Size(16f, 28f), CornerRadius(6f, 6f))
            // Calves
            val calfColor = colorFor("calves")
            drawRoundRect(calfColor, Offset(bc - 18f, h * 0.77f), Size(13f, 20f), CornerRadius(5f, 5f))
            drawRoundRect(calfColor, Offset(bc + 5f, h * 0.77f), Size(13f, 20f), CornerRadius(5f, 5f))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            Text("FRONT", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("BACK", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun DrawScope.drawFigureBase(cx: Float, h: Float, fill: Color, stroke: Color) {
    // Head
    drawCircle(color = fill, radius = 14f, center = Offset(cx, h * 0.10f))
    drawCircle(color = stroke, radius = 14f, center = Offset(cx, h * 0.10f), style = Stroke(2f))
    // Torso silhouette
    drawRoundRect(
        color = fill.copy(alpha = 0.45f),
        topLeft = Offset(cx - 24f, h * 0.20f),
        size = Size(48f, h * 0.34f),
        cornerRadius = CornerRadius(12f, 12f)
    )
    // Lower legs silhouette
    drawRoundRect(
        color = fill.copy(alpha = 0.45f),
        topLeft = Offset(cx - 20f, h * 0.55f),
        size = Size(16f, h * 0.38f),
        cornerRadius = CornerRadius(7f, 7f)
    )
    drawRoundRect(
        color = fill.copy(alpha = 0.45f),
        topLeft = Offset(cx + 4f, h * 0.55f),
        size = Size(16f, h * 0.38f),
        cornerRadius = CornerRadius(7f, 7f)
    )
}

@Composable
private fun LegendDot(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Custom vector athletic movement pose diagram tailored to the exercise biomechanical pattern.
 */
@Composable
fun ExercisePoseIllustration(
    poseType: String,
    modifier: Modifier = Modifier,
    size: Dp = 68.dp
) {
    val ext = LocalFitTrackColors.current
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val bg = ext.surfaceElevated

    Surface(
        modifier = modifier.size(size),
        shape = MaterialTheme.shapes.medium,
        color = bg,
        border = BorderStroke(1.dp, ext.border)
    ) {
        Canvas(modifier = Modifier.padding(10.dp)) {
            val w = this.size.width
            val h = this.size.height
            val brush = Brush.linearGradient(listOf(primary, secondary))
            val stroke = Stroke(width = 5f, cap = StrokeCap.Round)

            when (poseType.lowercase()) {
                "press" -> {
                    // Bench + pressing barbell upward
                    drawLine(primary.copy(alpha = 0.5f), Offset(w * 0.15f, h * 0.72f), Offset(w * 0.85f, h * 0.72f), 5f, StrokeCap.Round)
                    drawCircle(secondary, radius = w * 0.09f, center = Offset(w * 0.25f, h * 0.62f))
                    drawLine(brush, Offset(w * 0.36f, h * 0.66f), Offset(w * 0.56f, h * 0.66f), 6f, StrokeCap.Round)
                    drawLine(brush, Offset(w * 0.48f, h * 0.64f), Offset(w * 0.48f, h * 0.24f), 5f, StrokeCap.Round)
                    drawLine(secondary, Offset(w * 0.22f, h * 0.24f), Offset(w * 0.74f, h * 0.24f), 6f, StrokeCap.Round)
                }
                "squat" -> {
                    // Athletic squat stance + bar
                    drawCircle(secondary, radius = w * 0.09f, center = Offset(w * 0.50f, h * 0.18f))
                    drawLine(primary, Offset(w * 0.20f, h * 0.30f), Offset(w * 0.80f, h * 0.30f), 6f, StrokeCap.Round)
                    val path = Path().apply {
                        moveTo(w * 0.50f, h * 0.30f)
                        lineTo(w * 0.44f, h * 0.58f)
                        lineTo(w * 0.66f, h * 0.68f)
                        lineTo(w * 0.60f, h * 0.88f)
                    }
                    drawPath(path, brush, style = stroke)
                    drawLine(brush, Offset(w * 0.44f, h * 0.58f), Offset(w * 0.30f, h * 0.88f), 5f, StrokeCap.Round)
                }
                "hinge" -> {
                    // Hip hinge / deadlift / RDL
                    drawCircle(secondary, radius = w * 0.09f, center = Offset(w * 0.70f, h * 0.30f))
                    drawLine(brush, Offset(w * 0.62f, h * 0.38f), Offset(w * 0.36f, h * 0.54f), 6f, StrokeCap.Round)
                    drawLine(brush, Offset(w * 0.36f, h * 0.54f), Offset(w * 0.38f, h * 0.86f), 6f, StrokeCap.Round)
                    drawLine(primary, Offset(w * 0.60f, h * 0.42f), Offset(w * 0.60f, h * 0.78f), 5f, StrokeCap.Round)
                    drawCircle(secondary, radius = w * 0.10f, center = Offset(w * 0.60f, h * 0.82f), style = Stroke(4f))
                }
                "pull", "row" -> {
                    // Overhead pull / bent row
                    drawLine(primary, Offset(w * 0.16f, h * 0.16f), Offset(w * 0.84f, h * 0.16f), 6f, StrokeCap.Round)
                    drawCircle(secondary, radius = w * 0.09f, center = Offset(w * 0.50f, h * 0.42f))
                    drawLine(brush, Offset(w * 0.32f, h * 0.18f), Offset(w * 0.42f, h * 0.46f), 5f, StrokeCap.Round)
                    drawLine(brush, Offset(w * 0.68f, h * 0.18f), Offset(w * 0.58f, h * 0.46f), 5f, StrokeCap.Round)
                    drawLine(brush, Offset(w * 0.50f, h * 0.50f), Offset(w * 0.50f, h * 0.86f), 6f, StrokeCap.Round)
                }
                "curl", "extension" -> {
                    // Arm isolation arc
                    drawCircle(secondary, radius = w * 0.09f, center = Offset(w * 0.38f, h * 0.22f))
                    drawLine(brush, Offset(w * 0.38f, h * 0.32f), Offset(w * 0.38f, h * 0.84f), 6f, StrokeCap.Round)
                    drawLine(primary, Offset(w * 0.40f, h * 0.42f), Offset(w * 0.46f, h * 0.62f), 5f, StrokeCap.Round)
                    drawLine(secondary, Offset(w * 0.46f, h * 0.62f), Offset(w * 0.68f, h * 0.44f), 5f, StrokeCap.Round)
                    drawCircle(secondary, radius = w * 0.08f, center = Offset(w * 0.72f, h * 0.40f))
                }
                "core" -> {
                    // Plank / hollow body line
                    drawCircle(secondary, radius = w * 0.08f, center = Offset(w * 0.20f, h * 0.52f))
                    drawLine(brush, Offset(w * 0.26f, h * 0.56f), Offset(w * 0.84f, h * 0.72f), 6f, StrokeCap.Round)
                    drawLine(primary, Offset(w * 0.32f, h * 0.58f), Offset(w * 0.26f, h * 0.78f), 5f, StrokeCap.Round)
                    drawLine(primary.copy(alpha = 0.45f), Offset(w * 0.14f, h * 0.78f), Offset(w * 0.88f, h * 0.78f), 3f, StrokeCap.Round)
                }
                else -> {
                    // Raise / Fly / Dynamic athletic arc
                    drawCircle(secondary, radius = w * 0.09f, center = Offset(w * 0.50f, h * 0.22f))
                    drawLine(brush, Offset(w * 0.50f, h * 0.32f), Offset(w * 0.50f, h * 0.82f), 6f, StrokeCap.Round)
                    drawLine(primary, Offset(w * 0.18f, h * 0.42f), Offset(w * 0.82f, h * 0.42f), 5f, StrokeCap.Round)
                    drawCircle(secondary, radius = w * 0.07f, center = Offset(w * 0.16f, h * 0.42f))
                    drawCircle(secondary, radius = w * 0.07f, center = Offset(w * 0.84f, h * 0.42f))
                }
            }
        }
    }
}

/**
 * Interactive rest timer panel with circular countdown, +/-15s adjustments,
 * pause/resume, skip, custom preset durations, and alert toggles.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CircularRestTimerCard(
    isResting: Boolean,
    secondsRemaining: Int,
    totalSeconds: Int,
    isPaused: Boolean,
    exerciseName: String,
    settings: RestTimerSettings,
    onAdjustSeconds: (Int) -> Unit,
    onTogglePause: () -> Unit,
    onSkip: () -> Unit,
    onStartCustomRest: (Int) -> Unit,
    onUpdateSettings: (RestTimerSettings) -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalFitTrackColors.current
    var showSettings by remember { mutableStateOf(false) }

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        accentBorder = if (isResting) MaterialTheme.colorScheme.secondary.copy(alpha = 0.75f) else ext.border
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                StatusBadge(
                    text = if (isResting) (if (isPaused) "REST PAUSED" else "RESTING") else "REST TIMER READY",
                    tint = if (isResting) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (isResting) "Recovering • $exerciseName" else "Auto-starts after logged sets",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = if (isResting) {
                        "Alarm scheduled in background if minimized"
                    } else {
                        "Default ${settings.defaultRestSeconds}s • Warm-up ${settings.warmupRestSeconds}s"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isResting) {
                val progress = if (totalSeconds > 0) (secondsRemaining.toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f) else 0f
                AnimatedProgressRing(
                    progress = progress,
                    size = 86.dp,
                    strokeWidth = 8.dp,
                    primaryColor = MaterialTheme.colorScheme.secondary,
                    secondaryColor = MaterialTheme.colorScheme.primary
                ) {
                    Text(
                        text = FitnessCalculator.formatDuration(secondsRemaining.toLong()),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                IconButton(
                    onClick = { showSettings = !showSettings },
                    modifier = Modifier.testTag("rest_settings_toggle")
                ) {
                    Icon(Icons.Filled.Tune, contentDescription = "Rest timer settings")
                }
            }
        }

        if (isResting) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { onAdjustSeconds(-15) },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("rest_minus_15_button")
                ) {
                    Icon(Icons.Filled.Remove, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("15s")
                }
                OutlinedButton(
                    onClick = onTogglePause,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("rest_pause_resume_button")
                ) {
                    Icon(
                        imageVector = if (isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                        contentDescription = if (isPaused) "Resume rest" else "Pause rest",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isPaused) "Resume" else "Pause")
                }
                OutlinedButton(
                    onClick = { onAdjustSeconds(15) },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("rest_plus_15_button")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("15s")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = { showSettings = !showSettings },
                    modifier = Modifier.testTag("rest_settings_inline_toggle")
                ) {
                    Icon(Icons.Filled.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Timer Settings")
                }
                SecondaryButton(
                    text = "Skip Rest",
                    icon = Icons.Filled.SkipNext,
                    onClick = onSkip,
                    modifier = Modifier.testTag("skip_rest_button")
                )
            }
        } else {
            Spacer(modifier = Modifier.height(10.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                settings.presets.forEach { preset ->
                    FilterChip(
                        selected = settings.defaultRestSeconds == preset,
                        onClick = { onStartCustomRest(preset) },
                        label = { Text("${preset}s") }
                    )
                }
            }
        }

        if (showSettings) {
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                shape = MaterialTheme.shapes.small,
                color = ext.surfaceElevated,
                border = BorderStroke(1.dp, ext.border)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Rest Preferences", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Auto-start after completing set", style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = settings.autoStartRest,
                            onCheckedChange = { onUpdateSettings(settings.copy(autoStartRest = it)) }
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Filled.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("Completion chime", style = MaterialTheme.typography.bodyMedium)
                        }
                        Switch(
                            checked = settings.soundEnabled,
                            onCheckedChange = { onUpdateSettings(settings.copy(soundEnabled = it)) }
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Filled.Vibration, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("Haptic vibration alert", style = MaterialTheme.typography.bodyMedium)
                        }
                        Switch(
                            checked = settings.vibrationEnabled,
                            onCheckedChange = { onUpdateSettings(settings.copy(vibrationEnabled = it)) }
                        )
                    }
                    Text("Default rest duration", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(45, 60, 90, 120, 180).forEach { sec ->
                            FilterChip(
                                selected = settings.defaultRestSeconds == sec,
                                onClick = { onUpdateSettings(settings.copy(defaultRestSeconds = sec)) },
                                label = { Text("${sec}s") }
                            )
                        }
                    }
                }
            }
        }
    }
}
