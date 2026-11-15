package com.example.ui.screens

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.catalog.FitnessCatalog
import com.example.data.model.UserChallenge
import com.example.domain.UnitFormatter
import com.example.ui.branding.FitTrackGeometricIllustration
import com.example.ui.branding.FitTrackIllustrationType
import com.example.ui.components.AuroraAmbientBackground
import com.example.ui.components.AuroraProgressBar
import com.example.ui.components.FitTrackCard
import com.example.ui.components.GlassCard
import com.example.ui.components.GradientButton
import com.example.ui.components.SecondaryButton
import com.example.ui.components.StatusBadge
import com.example.ui.theme.LocalFitTrackColors
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChallengesScreen(
    uid: String,
    challenges: List<UserChallenge>,
    onUpdateChallenge: (UserChallenge, Double, Boolean) -> Unit,
    onBack: () -> Unit,
    unitSystem: String = "metric"
) {
    val ext = LocalFitTrackColors.current
    val isImperial = UnitFormatter.isImperial(unitSystem)
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
                    .padding(innerPadding)
                    .testTag("challenges_list"),
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

                items(displayChallenges, key = { it.id.ifBlank { it.title } }) { ch ->
                    val ratio = (ch.currentValue / ch.targetValue.coerceAtLeast(1.0)).toFloat().coerceIn(0f, 1f)
                    val isWaterChallenge = ch.category.equals("water", ignoreCase = true) ||
                        ch.category.equals("hydration", ignoreCase = true) ||
                        ch.unit.equals("ml", ignoreCase = true)
                    val isDistanceChallenge = ch.category.equals("gps", ignoreCase = true) ||
                        ch.unit.equals("km", ignoreCase = true)

                    val progressText = when {
                        isWaterChallenge -> UnitFormatter.formatWaterRatio(
                            ch.currentValue.roundToInt().toLong(),
                            ch.targetValue.roundToInt().toLong(),
                            unitSystem
                        )
                        isDistanceChallenge -> "${UnitFormatter.formatDistance(ch.currentValue, unitSystem, 1)} / ${UnitFormatter.formatDistance(ch.targetValue, unitSystem, 1)}"
                        else -> "${ch.currentValue.roundToInt()} / ${ch.targetValue.roundToInt()} ${ch.unit}"
                    }

                    val stepIncrement = when {
                        ch.category.equals("steps", ignoreCase = true) -> 2500.0
                        isWaterChallenge -> if (isImperial) UnitFormatter.inputWaterToMl(8.0, "imperial").toDouble() else 250.0
                        isDistanceChallenge -> if (isImperial) UnitFormatter.inputDistanceToKm(1.0, "imperial") else 1.0
                        ch.category.equals("workouts", ignoreCase = true) -> 1.0
                        else -> 1.0
                    }

                    val incrementButtonLabel = when {
                        ch.category.equals("steps", ignoreCase = true) -> "Log +2500"
                        isWaterChallenge -> if (isImperial) "Log +8 fl oz" else "Log +250 ml"
                        isDistanceChallenge -> if (isImperial) "Log +1 mi" else "Log +1 km"
                        else -> "Log +1"
                    }

                    FitTrackCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("challenge_card_${ch.id.ifBlank { ch.title }}"),
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
                            valueText = progressText,
                            progress = ratio,
                            color = if (ch.completed) ext.success else MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            SecondaryButton(
                                text = if (ch.joined) "Joined ✓" else "Join",
                                onClick = { onUpdateChallenge(ch, ch.currentValue, !ch.joined) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("challenge_join_${ch.id.ifBlank { ch.title }}")
                            )
                            GradientButton(
                                text = incrementButtonLabel,
                                icon = Icons.Filled.Add,
                                onClick = {
                                    onUpdateChallenge(ch, (ch.currentValue + stepIncrement).coerceAtMost(ch.targetValue), true)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("challenge_log_${ch.id.ifBlank { ch.title }}")
                            )
                        }
                    }
                }
            }
        }
    }
}
