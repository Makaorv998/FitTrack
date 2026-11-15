package com.example.ui.screens.workouts

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.PersonalRecord
import com.example.data.model.WorkoutSessionLog
import com.example.domain.WorkoutAnalyticsEngine
import com.example.domain.WorkoutDashboardMetrics
import com.example.ui.AsyncState
import com.example.ui.branding.FitTrackIllustrationType
import com.example.ui.components.AuroraProgressBar
import com.example.ui.components.DisclaimerBanner
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ErrorBannerCard
import com.example.ui.components.FitTrackCard
import com.example.ui.components.GlassCard
import com.example.ui.components.GradientButton
import com.example.ui.components.SectionHeader
import com.example.ui.components.SegmentedControl
import com.example.ui.components.SimpleBarChart
import com.example.ui.components.StatCard
import com.example.ui.components.StatusBadge
import com.example.ui.components.TrendLineChart
import com.example.ui.theme.LocalFitTrackColors
import java.util.Locale

@Composable
fun WorkoutAnalyticsView(
    metrics: WorkoutDashboardMetrics,
    sessions: List<WorkoutSessionLog>,
    personalRecords: List<PersonalRecord>,
    aiWorkoutAnalysisState: AsyncState<String>,
    onRequestAiAnalysis: () -> Unit
) {
    val ext = LocalFitTrackColors.current
    var volumePeriodTab by remember { mutableIntStateOf(0) } // 0 = Weekly, 1 = Monthly

    val loggedExercises = remember(sessions) {
        sessions.flatMap { it.exerciseRecords }
            .filter { !it.skipped && it.sets.isNotEmpty() }
            .map { it.exerciseId to it.exerciseName }
            .distinctBy { it.second }
    }
    var selectedExercise by remember(loggedExercises) {
        mutableStateOf(loggedExercises.firstOrNull())
    }
    val selectedSeries = remember(selectedExercise, sessions) {
        selectedExercise?.let { (id, name) ->
            WorkoutAnalyticsEngine.exerciseHistorySeries(id, name, sessions)
        }.orEmpty()
    }

    val totalWarmupVol = remember(sessions) { sessions.sumOf { it.warmupVolumeKg } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Recovery & Consistency Banner
        item {
            DisclaimerBanner(text = metrics.recoveryGuidance)
        }

        // Consistency & Frequency Tiles
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard(
                    title = "Consistency",
                    value = "${metrics.consistencyPercent}%",
                    subtitle = "Planned vs completed",
                    icon = Icons.Filled.CalendarMonth,
                    tint = ext.success,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Frequency",
                    value = String.format(Locale.US, "%.1f/wk", metrics.weeklyFrequency),
                    subtitle = "Last 28 days",
                    icon = Icons.Filled.Speed,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Weekly / Monthly Working Volume Chart
        item {
            FitTrackCard(modifier = Modifier.fillMaxWidth()) {
                SectionHeader(title = "Working Volume Trend (kg)")
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Working: ${metrics.totalWorkingVolumeKg.toInt()} kg • Warm-up excluded: ${totalWarmupVol.toInt()} kg",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(10.dp))
                SegmentedControl(
                    options = listOf("Weekly", "Monthly"),
                    selectedIndex = volumePeriodTab,
                    onSelect = { volumePeriodTab = it }
                )
                Spacer(modifier = Modifier.height(12.dp))
                val chartData = if (volumePeriodTab == 0) metrics.weeklyVolumeBars else metrics.monthlyVolumeBars
                if (chartData.isEmpty()) {
                    EmptyStateCard(
                        icon = Icons.Filled.FitnessCenter,
                        title = "No session volume yet",
                        subtitle = "Complete workouts to populate real weekly and monthly working volume charts.",
                        illustration = FitTrackIllustrationType.STRENGTH
                    )
                } else {
                    SimpleBarChart(values = chartData)
                }
            }
        }

        // Per-Exercise 1RM & Top Weight Trend
        item {
            FitTrackCard(modifier = Modifier.fillMaxWidth()) {
                SectionHeader(title = "Exercise Estimated 1RM Trend")
                Spacer(modifier = Modifier.height(8.dp))
                if (loggedExercises.isEmpty()) {
                    Text(
                        text = "Log working sets across sessions to chart per-exercise strength progression.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        loggedExercises.forEach { pair ->
                            FilterChip(
                                selected = selectedExercise?.second == pair.second,
                                onClick = { selectedExercise = pair },
                                label = { Text(pair.second) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    if (selectedSeries.isNotEmpty()) {
                        TrendLineChart(
                            points = selectedSeries.map { it.dateKey.takeLast(5) to it.estimatedOneRmKg.toFloat() },
                            lineColor = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }

        // Muscle Group Working Set Breakdown
        if (metrics.muscleDistribution.isNotEmpty()) {
            item {
                FitTrackCard(modifier = Modifier.fillMaxWidth()) {
                    SectionHeader(title = "Muscle Volume Balance")
                    Spacer(modifier = Modifier.height(10.dp))
                    val maxSets = (metrics.muscleDistribution.maxOfOrNull { it.second } ?: 1).coerceAtLeast(1)
                    metrics.muscleDistribution.forEachIndexed { idx, (muscle, sets) ->
                        AuroraProgressBar(
                            label = muscle,
                            valueText = "$sets working sets",
                            progress = sets.toFloat() / maxSets.toFloat(),
                            color = if (idx % 2 == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                        )
                        if (idx < metrics.muscleDistribution.lastIndex) Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }

        // Personal Records Timeline
        item {
            SectionHeader(title = "Personal Records Timeline (${personalRecords.size})")
        }
        if (personalRecords.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Filled.EmojiEvents,
                    title = "No personal records recorded yet",
                    subtitle = "Break a weight, rep, or estimated 1RM record during any workout to track it here."
                )
            }
        } else {
            items(personalRecords, key = { it.id }) { pr ->
                FitTrackCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(pr.exerciseName, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${pr.primaryMuscle} • ${pr.dateKey}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${WorkoutAnalyticsEngine.formatWeight(pr.weightKg)} × ${pr.reps}",
                                style = MaterialTheme.typography.titleMedium,
                                color = ext.success,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Est 1RM ${String.format(Locale.US, "%.1f", pr.estimatedOneRmKg)} kg",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }
        }

        // AI Training Review Card
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                accentBorder = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
            ) {
                StatusBadge(text = "AI TRAINING REVIEW", tint = MaterialTheme.colorScheme.secondary)
                Spacer(modifier = Modifier.height(6.dp))
                Text("Analyze Volume, Overload & Recovery", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Get an evidence-based checkup on your recent sessions, personal records, and deload pacing.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                GradientButton(
                    text = "Analyze My Training",
                    icon = Icons.Filled.AutoAwesome,
                    loading = aiWorkoutAnalysisState is AsyncState.Loading,
                    onClick = onRequestAiAnalysis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("analytics_ai_review_button")
                )
                when (aiWorkoutAnalysisState) {
                    is AsyncState.Loading -> {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CircularProgressIndicator()
                            Text("Reviewing volume and PR trends...")
                        }
                    }
                    is AsyncState.Success -> {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(aiWorkoutAnalysisState.data, style = MaterialTheme.typography.bodyMedium)
                    }
                    is AsyncState.Error -> {
                        Spacer(modifier = Modifier.height(10.dp))
                        ErrorBannerCard(message = aiWorkoutAnalysisState.message)
                    }
                    else -> Unit
                }
            }
        }
    }
}
