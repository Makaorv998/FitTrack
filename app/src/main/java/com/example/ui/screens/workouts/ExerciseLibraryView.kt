package com.example.ui.screens.workouts

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.catalog.WorkoutCatalog
import com.example.data.model.ExerciseDefinition
import com.example.data.model.WorkoutSessionLog
import com.example.domain.WorkoutAnalyticsEngine
import com.example.ui.AsyncState
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ErrorBannerCard
import com.example.ui.components.ExercisePoseIllustration
import com.example.ui.components.FitTrackBottomSheet
import com.example.ui.components.FitTrackCard
import com.example.ui.components.GradientButton
import com.example.ui.components.MuscleDiagramView
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusBadge
import com.example.ui.components.TrendLineChart
import com.example.ui.theme.LocalFitTrackColors

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExerciseLibraryView(
    sessions: List<WorkoutSessionLog>,
    aiSubstitutionsState: AsyncState<String>,
    onRequestAiSubstitutions: (exerciseName: String, muscleGroup: String, equipment: String, reason: String) -> Unit,
    onResetAiSubstitutions: () -> Unit
) {
    val ext = LocalFitTrackColors.current
    var query by remember { mutableStateOf("") }
    var selectedMuscle by remember { mutableStateOf("All") }
    var selectedEquipment by remember { mutableStateOf("All") }
    var selectedLevel by remember { mutableStateOf("All") }
    var activeDetail by remember { mutableStateOf<ExerciseDefinition?>(null) }

    val filtered = remember(query, selectedMuscle, selectedEquipment, selectedLevel) {
        WorkoutCatalog.exercises.filter { ex ->
            val matchQuery = query.isBlank() ||
                ex.name.contains(query, ignoreCase = true) ||
                ex.muscleGroup.contains(query, ignoreCase = true) ||
                ex.equipment.contains(query, ignoreCase = true) ||
                ex.secondaryMuscles.any { it.contains(query, ignoreCase = true) }
            val matchMuscle = selectedMuscle == "All" ||
                ex.muscleGroup.equals(selectedMuscle, ignoreCase = true) ||
                ex.secondaryMuscles.any { it.equals(selectedMuscle, ignoreCase = true) }
            val matchEquip = selectedEquipment == "All" || ex.equipment.equals(selectedEquipment, ignoreCase = true)
            val matchLevel = selectedLevel == "All" || ex.level.equals(selectedLevel, ignoreCase = true)
            matchQuery && matchMuscle && matchEquip && matchLevel
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search 36+ exercises, muscles, equipment...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotBlank()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("exercise_search_input")
            )
            Spacer(modifier = Modifier.height(8.dp))

            Text("Muscle Group", style = MaterialTheme.typography.labelLarge)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                WorkoutCatalog.muscleGroups.forEach { mg ->
                    FilterChip(
                        selected = selectedMuscle == mg,
                        onClick = { selectedMuscle = mg },
                        label = { Text(mg) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text("Equipment", style = MaterialTheme.typography.labelLarge)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                WorkoutCatalog.equipmentTypes.forEach { eq ->
                    FilterChip(
                        selected = selectedEquipment == eq,
                        onClick = { selectedEquipment = eq },
                        label = { Text(eq) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                WorkoutCatalog.difficultyLevels.forEach { lvl ->
                    FilterChip(
                        selected = selectedLevel == lvl,
                        onClick = { selectedLevel = lvl },
                        label = { Text(lvl) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Showing ${filtered.size} of ${WorkoutCatalog.exercises.size} exercises",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }

        if (filtered.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Filled.FitnessCenter,
                    title = "No matching exercises",
                    subtitle = "Clear one of your muscle, equipment, or difficulty filters.",
                    actionLabel = "Reset Filters",
                    onAction = {
                        query = ""
                        selectedMuscle = "All"
                        selectedEquipment = "All"
                        selectedLevel = "All"
                    }
                )
            }
        } else {
            items(filtered, key = { it.id }) { ex ->
                FitTrackCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        onResetAiSubstitutions()
                        activeDetail = ex
                    }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ExercisePoseIllustration(poseType = ex.illustrationPose, size = 62.dp)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(ex.name, style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${ex.muscleGroup} • ${ex.equipment} • ${ex.defaultSets}×${ex.defaultRepMin}-${ex.defaultRepMax}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            if (ex.secondaryMuscles.isNotEmpty()) {
                                Text(
                                    text = "Synergists: ${ex.secondaryMuscles.joinToString(", ")}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        StatusBadge(
                            text = ex.level.uppercase(),
                            tint = when (ex.level.lowercase()) {
                                "beginner" -> ext.success
                                "advanced" -> ext.warning
                                else -> MaterialTheme.colorScheme.primary
                            }
                        )
                    }
                }
            }
        }
    }

    activeDetail?.let { ex ->
        ExerciseDetailSheet(
            exercise = ex,
            sessions = sessions,
            aiSubstitutionsState = aiSubstitutionsState,
            onRequestAiSubstitutions = onRequestAiSubstitutions,
            onSelectAlternative = { alt ->
                onResetAiSubstitutions()
                activeDetail = alt
            },
            onDismiss = {
                onResetAiSubstitutions()
                activeDetail = null
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExerciseDetailSheet(
    exercise: ExerciseDefinition,
    sessions: List<WorkoutSessionLog>,
    aiSubstitutionsState: AsyncState<String>,
    onRequestAiSubstitutions: (String, String, String, String) -> Unit,
    onSelectAlternative: (ExerciseDefinition) -> Unit,
    onDismiss: () -> Unit
) {
    val ext = LocalFitTrackColors.current
    val history = remember(exercise.id, sessions) {
        WorkoutAnalyticsEngine.exerciseHistorySeries(exercise.id, exercise.name, sessions)
    }
    val alternatives = remember(exercise.id) {
        WorkoutCatalog.alternativesFor(exercise)
    }

    FitTrackBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ExercisePoseIllustration(poseType = exercise.illustrationPose, size = 72.dp)
                Column(modifier = Modifier.weight(1f)) {
                    Text(exercise.name, style = MaterialTheme.typography.headlineSmall)
                    Spacer(modifier = Modifier.height(4.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        StatusBadge(text = exercise.muscleGroup, tint = MaterialTheme.colorScheme.secondary)
                        StatusBadge(text = exercise.equipment, tint = MaterialTheme.colorScheme.primary)
                        StatusBadge(text = exercise.level.uppercase(), tint = ext.success)
                    }
                }
            }

            val recentSessionMatches = remember(exercise.id, exercise.name, sessions) {
                sessions.mapNotNull { s ->
                    val rec = s.exerciseRecords.firstOrNull {
                        it.exerciseId == exercise.id || it.exerciseName.equals(exercise.name, ignoreCase = true)
                    }
                    if (rec != null && !rec.skipped && rec.sets.isNotEmpty()) {
                        s to rec
                    } else null
                }.take(5)
            }

            MuscleDiagramView(
                primaryMuscle = exercise.muscleGroup,
                secondaryMuscles = exercise.secondaryMuscles
            )

            // Sets & Rep Recommendations Pill Bar
            FitTrackCard(modifier = Modifier.fillMaxWidth()) {
                SectionHeader(title = "Recommended Protocol")
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    StatusBadge(text = "${exercise.defaultSets} SETS", tint = MaterialTheme.colorScheme.primary)
                    StatusBadge(text = "${exercise.defaultRepMin}–${exercise.defaultRepMax} REPS", tint = MaterialTheme.colorScheme.secondary)
                    StatusBadge(text = "${exercise.defaultRestSeconds}S REST", tint = ext.success)
                }
            }

            FitTrackCard(modifier = Modifier.fillMaxWidth()) {
                SectionHeader(title = "Execution Steps")
                Spacer(modifier = Modifier.height(6.dp))
                val stepsToRender = exercise.steps.ifEmpty { listOf(exercise.instructions) }
                stepsToRender.forEachIndexed { idx, step ->
                    Text("${idx + 1}. $step", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                }
                if (exercise.techniqueGuidance.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Technique Cue: ${exercise.techniqueGuidance}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            if (exercise.commonMistakes.isNotEmpty()) {
                FitTrackCard(modifier = Modifier.fillMaxWidth()) {
                    SectionHeader(title = "Common Mistakes to Avoid")
                    Spacer(modifier = Modifier.height(6.dp))
                    exercise.commonMistakes.forEach { m ->
                        Text("• $m", style = MaterialTheme.typography.bodyMedium, color = ext.warning)
                    }
                }
            }

            // Previous Performance & Progression History
            FitTrackCard(modifier = Modifier.fillMaxWidth()) {
                SectionHeader(title = "Previous Performance History")
                Spacer(modifier = Modifier.height(8.dp))
                if (history.isEmpty() && recentSessionMatches.isEmpty()) {
                    Text(
                        text = "No completed sets logged for ${exercise.name} yet. Start a workout to record baseline sets.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    if (history.isNotEmpty()) {
                        TrendLineChart(
                            points = history.map { it.dateKey.takeLast(5) to it.estimatedOneRmKg.toFloat() },
                            lineColor = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                    recentSessionMatches.forEach { (session, rec) ->
                        val summary = rec.sets.joinToString(" • ") { s ->
                            "${WorkoutAnalyticsEngine.formatWeight(s.weightKg)}×${s.reps}"
                        }
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = ext.surfaceElevated,
                            border = BorderStroke(1.dp, ext.border),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "${session.dateKey.ifBlank { "Session" }} • ${session.workoutName}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Text(
                                    text = summary,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            if (alternatives.isNotEmpty()) {
                SectionHeader(title = "Catalog Alternatives")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    alternatives.forEach { alt ->
                        Surface(
                            onClick = { onSelectAlternative(alt) },
                            shape = MaterialTheme.shapes.small,
                            color = ext.surfaceElevated,
                            border = BorderStroke(1.dp, ext.border)
                        ) {
                            Text(
                                text = "${alt.name} (${alt.equipment})",
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            GradientButton(
                text = "Ask AI Coach for Substitutions",
                icon = Icons.Filled.AutoAwesome,
                loading = aiSubstitutionsState is AsyncState.Loading,
                onClick = {
                    onRequestAiSubstitutions(
                        exercise.name,
                        exercise.muscleGroup,
                        "Home & Commercial Gym",
                        "Equipment busy or variation needed"
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("exercise_ai_substitute_button")
            )

            when (aiSubstitutionsState) {
                is AsyncState.Loading -> {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircularProgressIndicator(modifier = Modifier.padding(4.dp))
                        Text("Fetching Gemini substitution cues...")
                    }
                }
                is AsyncState.Success -> {
                    FitTrackCard(modifier = Modifier.fillMaxWidth(), accentBorder = MaterialTheme.colorScheme.secondary) {
                        Text("AI Alternatives", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(aiSubstitutionsState.data, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                is AsyncState.Error -> {
                    ErrorBannerCard(message = aiSubstitutionsState.message)
                }
                else -> Unit
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
