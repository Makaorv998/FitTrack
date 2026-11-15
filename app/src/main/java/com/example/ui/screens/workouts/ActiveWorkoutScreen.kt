package com.example.ui.screens.workouts

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.catalog.WorkoutCatalog
import com.example.data.model.CompletedSetRecord
import com.example.data.model.ExerciseDefinition
import com.example.data.model.PersonalRecord
import com.example.data.model.WorkoutSessionLog
import com.example.domain.FitnessCalculator
import com.example.domain.WorkoutAnalyticsEngine
import com.example.platform.RestTimerSettings
import com.example.ui.ActiveWorkoutState
import com.example.ui.AsyncState
import com.example.ui.components.AuroraAmbientBackground
import com.example.ui.components.CircularRestTimerCard
import com.example.ui.components.ConfirmationModal
import com.example.ui.components.ErrorBannerCard
import com.example.ui.components.ExercisePoseIllustration
import com.example.ui.components.FitTrackBottomSheet
import com.example.ui.components.FitTrackCard
import com.example.ui.components.GlassCard
import com.example.ui.components.GradientButton
import com.example.ui.components.MuscleDiagramView
import com.example.ui.components.SecondaryButton
import com.example.ui.components.StatusBadge
import com.example.ui.theme.LocalFitTrackColors

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ActiveWorkoutScreen(
    active: ActiveWorkoutState,
    sessions: List<WorkoutSessionLog>,
    personalRecords: List<PersonalRecord>,
    aiSubstitutionsState: AsyncState<String>,
    onToggleWorkoutPause: () -> Unit,
    onSelectExerciseIndex: (Int) -> Unit,
    onLogSet: (exerciseIndex: Int, setType: String, reps: Int, weightKg: Double, overrideRestSeconds: Int?) -> Unit,
    onUpdateLoggedSet: (exerciseIndex: Int, setIndex: Int, setType: String, reps: Int, weightKg: Double) -> Unit,
    onDeleteLoggedSet: (exerciseIndex: Int, setIndex: Int) -> Unit,
    onSkipExercise: (Int) -> Unit,
    onReplaceExercise: (Int, ExerciseDefinition) -> Unit,
    onUpdateExerciseNote: (Int, String) -> Unit,
    onUpdateSessionNotes: (String) -> Unit,
    onApplyProgressionConfirmation: (exerciseIndex: Int, weightKg: Double, reps: Long) -> Unit,
    onAdjustRest: (Int) -> Unit,
    onToggleRestPause: () -> Unit,
    onSkipRest: () -> Unit,
    onStartCustomRest: (Int) -> Unit,
    onUpdateRestSettings: (RestTimerSettings) -> Unit,
    onRequestAiSubstitutions: (String, String, String, String) -> Unit,
    onResetAiSubstitutions: () -> Unit,
    onFinishWorkout: () -> Unit,
    onDiscardWorkout: () -> Unit
) {
    val ext = LocalFitTrackColors.current
    val idx = active.currentExerciseIndex.coerceIn(0, (active.records.size - 1).coerceAtLeast(0))
    val currentEntry = active.plan.exercises.getOrNull(idx)
    val currentRecord = active.records.getOrNull(idx)
    val exerciseDef = remember(currentEntry?.exerciseId, currentEntry?.exerciseName) {
        currentEntry?.let {
            WorkoutCatalog.findExerciseById(it.exerciseId) ?: WorkoutCatalog.findExerciseByName(it.exerciseName)
        }
    }

    var selectedSetType by remember(idx) {
        mutableStateOf(currentEntry?.setType ?: "working")
    }
    var repsInput by remember(idx, currentEntry?.targetReps) {
        mutableIntStateOf((currentEntry?.targetReps ?: 10L).toInt().coerceAtLeast(1))
    }
    var weightInput by remember(idx, currentEntry?.targetWeightKg) {
        mutableStateOf(currentEntry?.targetWeightKg ?: 0.0)
    }

    var showAnatomy by remember { mutableStateOf(false) }
    var showReplaceSheet by remember { mutableStateOf(false) }
    var editingSetPair by remember { mutableStateOf<Pair<Int, CompletedSetRecord>?>(null) }
    var showFinishDialog by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }

    val progression = remember(currentEntry, sessions, personalRecords) {
        currentEntry?.let { entry ->
            val pr = personalRecords.firstOrNull {
                it.exerciseId == entry.exerciseId || it.exerciseName.equals(entry.exerciseName, ignoreCase = true)
            }
            WorkoutAnalyticsEngine.analyzeProgression(entry, sessions, pr)
        }
    }

    AuroraAmbientBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Session Telemetry HUD
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                accentBorder = MaterialTheme.colorScheme.primary
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            StatusBadge(
                                text = if (active.isPaused) "PAUSED" else "ACTIVE SESSION",
                                tint = if (active.isPaused) ext.warning else ext.success
                            )
                            StatusBadge(text = "SAVED LOCALLY", tint = MaterialTheme.colorScheme.secondary)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(active.plan.name, style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = "${FitnessCalculator.formatDuration(active.elapsedSeconds)} • ${active.completedSetsCount} sets • Working ${active.workingVolumeKg.toInt()} kg",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    IconButton(
                        onClick = onToggleWorkoutPause,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("pause_workout_button")
                    ) {
                        Icon(
                            imageVector = if (active.isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                            contentDescription = if (active.isPaused) "Resume workout" else "Pause workout",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Interactive Rest Timer Card
            CircularRestTimerCard(
                isResting = active.isResting,
                secondsRemaining = active.restSecondsRemaining,
                totalSeconds = active.restTotalSeconds,
                isPaused = active.restPaused,
                exerciseName = active.restExerciseName,
                settings = active.restSettings,
                onAdjustSeconds = onAdjustRest,
                onTogglePause = onToggleRestPause,
                onSkip = onSkipRest,
                onStartCustomRest = { sec ->
                    onStartCustomRest(sec)
                },
                onUpdateSettings = onUpdateRestSettings
            )

            // Exercise Navigation Pills
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                active.records.forEachIndexed { exIdx, rec ->
                    val targetSets = active.plan.exercises.getOrNull(exIdx)?.targetSets ?: 3L
                    val selected = exIdx == idx
                    val label = if (rec.skipped) {
                        "${exIdx + 1}. ${rec.exerciseName} (Skipped)"
                    } else {
                        "${exIdx + 1}. ${rec.exerciseName} (${rec.sets.size}/$targetSets)"
                    }
                    FilterChip(
                        selected = selected,
                        onClick = { onSelectExerciseIndex(exIdx) },
                        label = { Text(label) }
                    )
                }
            }

            if (currentEntry != null && currentRecord != null) {
                FitTrackCard(
                    modifier = Modifier.fillMaxWidth(),
                    accentBorder = if (currentRecord.skipped) ext.warning else MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                ) {
                    // Header with pose diagram & anatomy toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ExercisePoseIllustration(
                            poseType = exerciseDef?.illustrationPose ?: "press",
                            size = 64.dp
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currentRecord.exerciseName,
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = "${currentRecord.primaryMuscle} • ${currentRecord.equipment} • Target ${currentEntry.targetSets}×${currentEntry.repRangeMin}-${currentEntry.repRangeMax}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            if (currentRecord.replacedFromExerciseName.isNotBlank()) {
                                Text(
                                    text = "Swapped from ${currentRecord.replacedFromExerciseName}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ext.warning
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { showAnatomy = !showAnatomy }) {
                            Text(if (showAnatomy) "Hide Anatomy" else "Target Anatomy")
                        }
                        TextButton(
                            onClick = {
                                onResetAiSubstitutions()
                                showReplaceSheet = true
                            },
                            modifier = Modifier.testTag("replace_exercise_button")
                        ) {
                            Icon(Icons.Filled.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Replace")
                        }
                        TextButton(
                            onClick = { onSkipExercise(idx) },
                            modifier = Modifier.testTag("skip_exercise_button")
                        ) {
                            Icon(Icons.Filled.SkipNext, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (currentRecord.skipped) "Unskip" else "Skip")
                        }
                    }

                    if (showAnatomy) {
                        Spacer(modifier = Modifier.height(8.dp))
                        MuscleDiagramView(
                            primaryMuscle = currentRecord.primaryMuscle,
                            secondaryMuscles = currentRecord.secondaryMuscles,
                            height = 132.dp
                        )
                    }

                    // Previous Performance & Progressive Overload Banner (explicit user confirmation required!)
                    if (progression != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = ext.surfaceElevated,
                            border = BorderStroke(
                                1.dp,
                                if (progression.shouldDeload) ext.warning.copy(alpha = 0.6f) else MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(
                                        Icons.Filled.TrendingUp,
                                        contentDescription = null,
                                        tint = if (progression.shouldDeload) ext.warning else ext.success,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = progression.previousSessionSummary,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Text(
                                    text = progression.rationale,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (progression.hasActionableIncrease) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    OutlinedButton(
                                        onClick = {
                                            weightInput = progression.suggestedWeightKg
                                            repsInput = progression.suggestedReps.toInt()
                                            onApplyProgressionConfirmation(
                                                idx,
                                                progression.suggestedWeightKg,
                                                progression.suggestedReps
                                            )
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("confirm_overload_button")
                                    ) {
                                        Text(
                                            "Confirm & Apply ${WorkoutAnalyticsEngine.formatWeight(progression.suggestedWeightKg)} × ${progression.suggestedReps} reps"
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Logged Sets List (Tap to Edit or Delete)
                    if (currentRecord.sets.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Logged Sets (${currentRecord.sets.size})", style = MaterialTheme.typography.labelLarge)
                        Spacer(modifier = Modifier.height(4.dp))
                        currentRecord.sets.forEachIndexed { setIdx, set ->
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = ext.surfaceElevated,
                                border = BorderStroke(1.dp, ext.border),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        StatusBadge(
                                            text = set.setType.uppercase(),
                                            tint = when (set.setType.lowercase()) {
                                                "warmup" -> ext.warning
                                                "superset" -> MaterialTheme.colorScheme.primary
                                                "dropset" -> MaterialTheme.colorScheme.error
                                                else -> ext.success
                                            }
                                        )
                                        Text(
                                            text = "Set ${set.setNumber}: ${WorkoutAnalyticsEngine.formatWeight(set.weightKg)} × ${set.reps} reps",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    Row {
                                        IconButton(onClick = { editingSetPair = setIdx to set }) {
                                            Icon(Icons.Filled.Edit, contentDescription = "Edit set", modifier = Modifier.size(18.dp))
                                        }
                                        IconButton(onClick = { onDeleteLoggedSet(idx, setIdx) }) {
                                            Icon(Icons.Filled.Delete, contentDescription = "Delete set", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Set Type & Load Input Matrix
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Next Set (${currentRecord.sets.size + 1})", style = MaterialTheme.typography.labelLarge)
                    Spacer(modifier = Modifier.height(4.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            "working" to "Working",
                            "warmup" to "Warm-up",
                            "superset" to "Superset",
                            "dropset" to "Drop Set"
                        ).forEach { (key, label) ->
                            FilterChip(
                                selected = selectedSetType == key,
                                onClick = { selectedSetType = key },
                                label = { Text(label) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Weight Stepper
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            IconButton(
                                onClick = { weightInput = (weightInput - 2.5).coerceAtLeast(0.0) },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(Icons.Filled.Remove, contentDescription = "Decrease weight")
                            }
                            Text(
                                text = WorkoutAnalyticsEngine.formatWeight(weightInput),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(
                                onClick = { weightInput += 2.5 },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = "Increase weight")
                            }
                        }
                        // Reps Stepper
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            IconButton(
                                onClick = { repsInput = (repsInput - 1).coerceAtLeast(1) },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(Icons.Filled.Remove, contentDescription = "Decrease reps")
                            }
                            Text(
                                text = "$repsInput reps",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(
                                onClick = { repsInput += 1 },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = "Increase reps")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    GradientButton(
                        text = "Log Set ${currentRecord.sets.size + 1}",
                        icon = Icons.Filled.Check,
                        onClick = {
                            onLogSet(idx, selectedSetType, repsInput, weightInput, null)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("log_set_button")
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = currentRecord.notes,
                        onValueChange = { onUpdateExerciseNote(idx, it) },
                        label = { Text("Exercise note (grip, seat pin, tempo...)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        OutlinedButton(
                            onClick = { onSelectExerciseIndex(idx - 1) },
                            enabled = idx > 0,
                            modifier = Modifier.height(48.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Previous")
                        }
                        OutlinedButton(
                            onClick = { onSelectExerciseIndex(idx + 1) },
                            enabled = idx < active.records.lastIndex,
                            modifier = Modifier.height(48.dp)
                        ) {
                            Text("Next Exercise")
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // Session Summary Notes & Finish Controls
            FitTrackCard(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = active.sessionNotes,
                    onValueChange = onUpdateSessionNotes,
                    label = { Text("Overall workout note (energy, sleep, pump...)") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SecondaryButton(
                        text = "Discard",
                        onClick = { showDiscardDialog = true },
                        modifier = Modifier
                            .weight(0.9f)
                            .testTag("discard_workout_button")
                    )
                    Button(
                        onClick = { showFinishDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = ext.success),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier
                            .weight(1.3f)
                            .height(52.dp)
                            .testTag("finish_workout_button")
                    ) {
                        Text("Finish & Save", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }

    editingSetPair?.let { (setIdx, setRec) ->
        EditLoggedSetDialog(
            set = setRec,
            onDismiss = { editingSetPair = null },
            onSave = { newType, newReps, newWeight ->
                onUpdateLoggedSet(idx, setIdx, newType, newReps, newWeight)
                editingSetPair = null
            }
        )
    }

    if (showReplaceSheet && currentEntry != null) {
        ReplaceExerciseSheet(
            currentName = currentEntry.exerciseName,
            muscleGroup = currentEntry.muscleGroup,
            alternatives = exerciseDef?.let { WorkoutCatalog.alternativesFor(it) }
                ?: WorkoutCatalog.exercises.filter { it.muscleGroup == currentEntry.muscleGroup }.take(5),
            aiSubstitutionsState = aiSubstitutionsState,
            onRequestAi = { reason ->
                onRequestAiSubstitutions(
                    currentEntry.exerciseName,
                    currentEntry.muscleGroup,
                    "Gym or Home",
                    reason
                )
            },
            onPickReplacement = { replacement ->
                onReplaceExercise(idx, replacement)
                showReplaceSheet = false
            },
            onDismiss = { showReplaceSheet = false }
        )
    }

    if (showFinishDialog) {
        ConfirmationModal(
            title = "Finish Workout?",
            message = "Save ${active.completedSetsCount} completed sets (${active.workingVolumeKg.toInt()} kg working volume) to your training log and update Personal Records?",
            confirmText = "Save Workout",
            onConfirm = {
                showFinishDialog = false
                onFinishWorkout()
            },
            onDismiss = { showFinishDialog = false }
        )
    }

    if (showDiscardDialog) {
        ConfirmationModal(
            title = "Discard Active Workout?",
            message = "All sets logged in this session will be discarded permanently.",
            confirmText = "Discard",
            isDestructive = true,
            onConfirm = {
                showDiscardDialog = false
                onDiscardWorkout()
            },
            onDismiss = { showDiscardDialog = false }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditLoggedSetDialog(
    set: CompletedSetRecord,
    onDismiss: () -> Unit,
    onSave: (String, Int, Double) -> Unit
) {
    var setType by remember { mutableStateOf(set.setType) }
    var weightText by remember { mutableStateOf(set.weightKg.toString()) }
    var repsText by remember { mutableStateOf(set.reps.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Set ${set.setNumber}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("working", "warmup", "superset", "dropset").forEach { t ->
                        FilterChip(
                            selected = setType == t,
                            onClick = { setType = t },
                            label = { Text(t.replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }
                OutlinedTextField(
                    value = weightText,
                    onValueChange = { weightText = it },
                    label = { Text("Weight (kg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                OutlinedTextField(
                    value = repsText,
                    onValueChange = { repsText = it.filter(Char::isDigit) },
                    label = { Text("Reps") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(
                    setType,
                    (repsText.toIntOrNull() ?: set.reps.toInt()).coerceAtLeast(1),
                    (weightText.toDoubleOrNull() ?: set.weightKg).coerceAtLeast(0.0)
                )
            }) { Text("Update Set") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun ReplaceExerciseSheet(
    currentName: String,
    muscleGroup: String,
    alternatives: List<ExerciseDefinition>,
    aiSubstitutionsState: AsyncState<String>,
    onRequestAi: (String) -> Unit,
    onPickReplacement: (ExerciseDefinition) -> Unit,
    onDismiss: () -> Unit
) {
    val ext = LocalFitTrackColors.current
    var search by remember { mutableStateOf("") }
    val allMatching = remember(search, alternatives) {
        if (search.isBlank()) alternatives
        else WorkoutCatalog.exercises.filter {
            it.name.contains(search, ignoreCase = true) || it.muscleGroup.contains(search, ignoreCase = true)
        }.take(10)
    }

    FitTrackBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Replace $currentName", style = MaterialTheme.typography.headlineSmall)
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                label = { Text("Search any exercise ($muscleGroup recommended)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            allMatching.forEach { alt ->
                Surface(
                    onClick = { onPickReplacement(alt) },
                    shape = MaterialTheme.shapes.small,
                    color = ext.surfaceElevated,
                    border = BorderStroke(1.dp, ext.border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(alt.name, style = MaterialTheme.typography.titleMedium)
                            Text("${alt.muscleGroup} • ${alt.equipment}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                        }
                        StatusBadge(text = "SELECT", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            SecondaryButton(
                text = "Ask AI for Swaps",
                icon = Icons.Filled.AutoAwesome,
                onClick = { onRequestAi("Machine or bench unavailable") },
                modifier = Modifier.fillMaxWidth()
            )
            when (aiSubstitutionsState) {
                is AsyncState.Success -> {
                    FitTrackCard(modifier = Modifier.fillMaxWidth(), accentBorder = MaterialTheme.colorScheme.secondary) {
                        Text(aiSubstitutionsState.data, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                is AsyncState.Error -> ErrorBannerCard(message = aiSubstitutionsState.message)
                else -> Unit
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
