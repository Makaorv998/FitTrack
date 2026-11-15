package com.example.ui.screens.workouts

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.runtime.mutableStateListOf
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
import com.example.data.model.UserProfile
import com.example.data.model.WorkoutExerciseEntry
import com.example.data.model.WorkoutPlan
import com.example.data.model.WorkoutProgram
import com.example.platform.todayDateKey
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.FitTrackBottomSheet
import com.example.ui.components.FitTrackCard
import com.example.ui.components.GlassCard
import com.example.ui.components.GradientButton
import com.example.ui.components.SecondaryButton
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusBadge
import com.example.ui.theme.LocalFitTrackColors

@Composable
fun WorkoutProgramsView(
    profile: UserProfile?,
    customPlans: List<WorkoutPlan>,
    customPrograms: List<WorkoutProgram>,
    onStartWorkout: (WorkoutPlan) -> Unit,
    onScheduleWorkoutForToday: (WorkoutPlan) -> Unit,
    onSavePlan: (WorkoutPlan) -> Unit,
    onDuplicatePlan: (WorkoutPlan) -> Unit,
    onDeletePlan: (String) -> Unit,
    onSaveProgram: (WorkoutProgram) -> Unit,
    onDeleteProgram: (String) -> Unit,
    onOpenAiPlanner: () -> Unit
) {
    val ext = LocalFitTrackColors.current
    val personalizedPrograms = remember(profile) {
        WorkoutCatalog.personalizedProgramsForUser(profile)
    }
    var builderPlan by remember { mutableStateOf<WorkoutPlan?>(null) }
    var showBuilderSheet by remember { mutableStateOf(false) }
    var showProgramCreator by remember { mutableStateOf(false) }
    var expandedProgramId by remember { mutableStateOf<String?>(personalizedPrograms.firstOrNull()?.id) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GradientButton(
                    text = "Build Workout",
                    icon = Icons.Filled.Add,
                    onClick = {
                        builderPlan = null
                        showBuilderSheet = true
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("programs_build_workout_button")
                )
                SecondaryButton(
                    text = "New Program",
                    icon = Icons.Filled.Add,
                    onClick = { showProgramCreator = true },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("programs_new_program_button")
                )
            }
        }

        // Custom Programs
        if (customPrograms.isNotEmpty()) {
            item {
                SectionHeader(title = "My Custom Programs (${customPrograms.size})")
            }
            items(customPrograms, key = { it.id }) { prog ->
                ProgramCard(
                    program = prog,
                    expanded = expandedProgramId == prog.id,
                    onToggleExpand = {
                        expandedProgramId = if (expandedProgramId == prog.id) null else prog.id
                    },
                    onStartWorkout = onStartWorkout,
                    onScheduleWorkoutForToday = onScheduleWorkoutForToday,
                    onDeleteProgram = { onDeleteProgram(prog.id) }
                )
            }
        }

        // Custom Workouts
        item {
            SectionHeader(
                title = "My Custom Workouts (${customPlans.size})",
                actionLabel = "AI Planner",
                onAction = onOpenAiPlanner
            )
        }
        if (customPlans.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Filled.AutoAwesome,
                    title = "No custom workouts yet",
                    subtitle = "Create a custom routine with supersets, drop sets, and custom rest timers or generate one with AI.",
                    actionLabel = "Create Custom Workout",
                    onAction = {
                        builderPlan = null
                        showBuilderSheet = true
                    }
                )
            }
        } else {
            items(customPlans, key = { it.id }) { plan ->
                FitTrackCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                StatusBadge(text = plan.level.uppercase(), tint = MaterialTheme.colorScheme.secondary)
                                if (plan.isAiGenerated) {
                                    StatusBadge(text = "AI", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(plan.name, style = MaterialTheme.typography.titleLarge)
                            Text(
                                "${plan.exercises.size} exercises • ${plan.targetGoal.replace("_", " ")}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row {
                            IconButton(onClick = {
                                builderPlan = plan
                                showBuilderSheet = true
                            }) {
                                Icon(Icons.Filled.Edit, contentDescription = "Edit workout")
                            }
                            IconButton(onClick = { onDuplicatePlan(plan) }) {
                                Icon(Icons.Filled.ContentCopy, contentDescription = "Duplicate workout")
                            }
                            IconButton(onClick = { onDeletePlan(plan.id) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete workout", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    plan.exercises.take(5).forEach { ex ->
                        Text(
                            text = "• ${ex.exerciseName}: ${ex.targetSets} × ${ex.repRangeMin}-${ex.repRangeMax} (${ex.restSeconds}s)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GradientButton(
                            text = "Start",
                            icon = Icons.Filled.PlayArrow,
                            onClick = { onStartWorkout(plan) },
                            modifier = Modifier.weight(1f)
                        )
                        SecondaryButton(
                            text = "Today",
                            icon = Icons.Filled.CalendarToday,
                            onClick = { onScheduleWorkoutForToday(plan) },
                            modifier = Modifier.weight(0.75f)
                        )
                    }
                }
            }
        }

        // Personalized Structured Programs (8 curated programs)
        item {
            SectionHeader(title = "Personalized Training Programs (${personalizedPrograms.size})")
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Sets and rest intervals adapted to your age, goal, and activity profile.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        items(personalizedPrograms, key = { it.id }) { prog ->
            ProgramCard(
                program = prog,
                expanded = expandedProgramId == prog.id,
                onToggleExpand = {
                    expandedProgramId = if (expandedProgramId == prog.id) null else prog.id
                },
                onStartWorkout = onStartWorkout,
                onScheduleWorkoutForToday = onScheduleWorkoutForToday,
                onDeleteProgram = null
            )
        }
    }

    if (showBuilderSheet) {
        WorkoutBuilderSheet(
            initialPlan = builderPlan,
            onDismiss = { showBuilderSheet = false },
            onSave = { saved ->
                onSavePlan(saved)
                showBuilderSheet = false
            }
        )
    }

    if (showProgramCreator) {
        ProgramCreatorDialog(
            availablePlans = customPlans + WorkoutCatalog.starterTemplatesForUser(profile),
            onDismiss = { showProgramCreator = false },
            onSave = { prog ->
                onSaveProgram(prog)
                showProgramCreator = false
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProgramCard(
    program: WorkoutProgram,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    onStartWorkout: (WorkoutPlan) -> Unit,
    onScheduleWorkoutForToday: (WorkoutPlan) -> Unit,
    onDeleteProgram: (() -> Unit)?
) {
    val ext = LocalFitTrackColors.current
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onToggleExpand,
        accentBorder = if (expanded) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f) else ext.border
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    StatusBadge(text = "${program.daysPerWeek} DAYS/WK", tint = MaterialTheme.colorScheme.secondary)
                    StatusBadge(text = program.level.uppercase(), tint = MaterialTheme.colorScheme.primary)
                    if (program.isCustom) {
                        StatusBadge(text = "CUSTOM", tint = ext.success)
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(program.name, style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    program.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (onDeleteProgram != null) {
                IconButton(onClick = onDeleteProgram) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete program", tint = MaterialTheme.colorScheme.error)
                }
            }
        }

        if (expanded) {
            Spacer(modifier = Modifier.height(10.dp))
            if (program.weeklySplit.isNotEmpty()) {
                Text("Weekly Split", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                Spacer(modifier = Modifier.height(4.dp))
                program.weeklySplit.forEach { split ->
                    Text("• $split", style = MaterialTheme.typography.bodyMedium)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Progression: ${program.progressionGuidance}",
                style = MaterialTheme.typography.labelLarge,
                color = ext.success
            )
            Spacer(modifier = Modifier.height(10.dp))
            program.workouts.forEach { dayPlan ->
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = ext.surfaceElevated,
                    border = BorderStroke(1.dp, ext.border),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(dayPlan.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            dayPlan.exercises.joinToString(", ") { "${it.exerciseName} (${it.targetSets}×${it.repRangeMin}-${it.repRangeMax})" },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { onStartWorkout(dayPlan) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Start")
                            }
                            OutlinedButton(
                                onClick = { onScheduleWorkoutForToday(dayPlan) },
                                modifier = Modifier.weight(0.9f)
                            ) {
                                Icon(Icons.Filled.CalendarToday, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Today")
                            }
                        }
                    }
                }
            }
        } else {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Tap to view ${program.workouts.size} workouts & progression plan",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WorkoutBuilderSheet(
    initialPlan: WorkoutPlan?,
    onDismiss: () -> Unit,
    onSave: (WorkoutPlan) -> Unit
) {
    val ext = LocalFitTrackColors.current
    var name by remember { mutableStateOf(initialPlan?.name ?: "Custom Athletic Split") }
    var level by remember { mutableStateOf(initialPlan?.level ?: "intermediate") }
    var targetGoal by remember { mutableStateOf(initialPlan?.targetGoal ?: "muscle_gain") }
    var progressionNote by remember { mutableStateOf(initialPlan?.progressionNote ?: "Add 2.5 kg once top reps are achieved cleanly.") }
    val entries = remember {
        mutableStateListOf<WorkoutExerciseEntry>().apply {
            if (initialPlan != null) addAll(initialPlan.exercises)
        }
    }
    var search by remember { mutableStateOf("") }

    val matchingCatalog = remember(search) {
        WorkoutCatalog.exercises.filter { ex ->
            search.isBlank() ||
                ex.name.contains(search, ignoreCase = true) ||
                ex.muscleGroup.contains(search, ignoreCase = true) ||
                ex.equipment.contains(search, ignoreCase = true)
        }
    }

    FitTrackBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = if (initialPlan == null) "Workout Builder" else "Edit Workout",
                style = MaterialTheme.typography.headlineSmall
            )
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Workout Name") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("builder_workout_name_input")
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("beginner", "intermediate", "advanced").forEach { lvl ->
                    FilterChip(
                        selected = level == lvl,
                        onClick = { level = lvl },
                        label = { Text(lvl.replaceFirstChar { it.uppercase() }) }
                    )
                }
            }
            OutlinedTextField(
                value = progressionNote,
                onValueChange = { progressionNote = it },
                label = { Text("Progression Note (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            SectionHeader(title = "Selected Exercises (${entries.size})")
            if (entries.isEmpty()) {
                Text(
                    "Search and tap exercises below to build your session.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                entries.forEachIndexed { idx, item ->
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = ext.surfaceElevated,
                        border = BorderStroke(1.dp, ext.border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("${idx + 1}. ${item.exerciseName}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                                Row {
                                    IconButton(
                                        onClick = {
                                            if (idx > 0) {
                                                val moved = entries.removeAt(idx)
                                                entries.add(idx - 1, moved)
                                            }
                                        },
                                        enabled = idx > 0
                                    ) {
                                        Icon(Icons.Filled.ArrowUpward, contentDescription = "Move up")
                                    }
                                    IconButton(
                                        onClick = {
                                            if (idx < entries.lastIndex) {
                                                val moved = entries.removeAt(idx)
                                                entries.add(idx + 1, moved)
                                            }
                                        },
                                        enabled = idx < entries.lastIndex
                                    ) {
                                        Icon(Icons.Filled.ArrowDownward, contentDescription = "Move down")
                                    }
                                    IconButton(onClick = { entries.removeAt(idx) }) {
                                        Icon(Icons.Filled.Delete, contentDescription = "Remove exercise", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedTextField(
                                    value = item.targetSets.toString(),
                                    onValueChange = { v ->
                                        val s = v.filter(Char::isDigit).toLongOrNull() ?: 1L
                                        entries[idx] = item.copy(targetSets = s.coerceIn(1L, 12L))
                                    },
                                    label = { Text("Sets") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = item.repRangeMin.toString(),
                                    onValueChange = { v ->
                                        val r = v.filter(Char::isDigit).toLongOrNull() ?: 1L
                                        entries[idx] = item.copy(repRangeMin = r.coerceIn(1L, 60L), targetReps = maxOf(r, item.repRangeMax))
                                    },
                                    label = { Text("Min") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = item.repRangeMax.toString(),
                                    onValueChange = { v ->
                                        val r = v.filter(Char::isDigit).toLongOrNull() ?: 1L
                                        entries[idx] = item.copy(repRangeMax = r.coerceIn(1L, 60L), targetReps = r.coerceIn(1L, 60L))
                                    },
                                    label = { Text("Max") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = item.restSeconds.toString(),
                                    onValueChange = { v ->
                                        val rs = v.filter(Char::isDigit).toLongOrNull() ?: 60L
                                        entries[idx] = item.copy(restSeconds = rs.coerceIn(15L, 300L))
                                    },
                                    label = { Text("Rest s") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("working", "warmup", "superset", "dropset").forEach { st ->
                                    FilterChip(
                                        selected = item.setType == st,
                                        onClick = { entries[idx] = item.copy(setType = st) },
                                        label = { Text(st.replaceFirstChar { it.uppercase() }) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            SectionHeader(title = "Add From Exercise Library")
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                placeholder = { Text("Search chest, squat, dumbbell...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (search.isNotBlank()) {
                        IconButton(onClick = { search = "" }) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            matchingCatalog.take(10).forEach { ex ->
                Surface(
                    onClick = {
                        entries.add(
                            WorkoutExerciseEntry(
                                exerciseId = ex.id,
                                exerciseName = ex.name,
                                muscleGroup = ex.muscleGroup,
                                secondaryMuscles = ex.secondaryMuscles,
                                equipment = ex.equipment,
                                targetSets = ex.defaultSets.toLong(),
                                targetReps = ex.defaultRepMax.toLong(),
                                repRangeMin = ex.defaultRepMin.toLong(),
                                repRangeMax = ex.defaultRepMax.toLong(),
                                targetWeightKg = 0.0,
                                restSeconds = ex.defaultRestSeconds.toLong()
                            )
                        )
                    },
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, ext.border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(ex.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text("${ex.muscleGroup} • ${ex.equipment}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                        }
                        Icon(Icons.Filled.Add, contentDescription = "Add ${ex.name}", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            GradientButton(
                text = if (initialPlan == null) "Save Custom Workout" else "Update Workout",
                enabled = name.isNotBlank() && entries.isNotEmpty(),
                onClick = {
                    onSave(
                        (initialPlan ?: WorkoutPlan()).copy(
                            name = name.trim(),
                            level = level,
                            targetGoal = targetGoal,
                            progressionNote = progressionNote.trim(),
                            exercises = entries.toList()
                        )
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("builder_save_workout_button")
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ProgramCreatorDialog(
    availablePlans: List<WorkoutPlan>,
    onDismiss: () -> Unit,
    onSave: (WorkoutProgram) -> Unit
) {
    var name by remember { mutableStateOf("My 3-Day Strength Split") }
    var description by remember { mutableStateOf("Custom multi-day rotation with tracked progressive overload.") }
    var daysPerWeek by remember { mutableIntStateOf(3) }
    val selectedPlanIds = remember { mutableStateListOf<String>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Custom Program") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Program Name") }, singleLine = true)
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") })
                Text("Select Workout Days (${selectedPlanIds.size}):", style = MaterialTheme.typography.labelLarge)
                availablePlans.distinctBy { it.name }.take(10).forEach { plan ->
                    val checked = plan.id in selectedPlanIds
                    FilterChip(
                        selected = checked,
                        onClick = {
                            if (checked) selectedPlanIds.remove(plan.id) else selectedPlanIds.add(plan.id)
                            daysPerWeek = selectedPlanIds.size.coerceIn(1, 7)
                        },
                        label = { Text(plan.name) }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = name.isNotBlank() && selectedPlanIds.isNotEmpty(),
                onClick = {
                    val chosen = availablePlans.filter { it.id in selectedPlanIds }
                    onSave(
                        WorkoutProgram(
                            name = name.trim(),
                            category = "custom",
                            level = chosen.firstOrNull()?.level ?: "intermediate",
                            daysPerWeek = daysPerWeek.toLong(),
                            description = description.trim(),
                            weeklySplit = chosen.mapIndexed { i, p -> "Day ${i + 1}: ${p.name}" },
                            progressionGuidance = "Aim for +1 rep or +2.5 kg when top reps feel clean.",
                            workouts = chosen,
                            isCustom = true
                        )
                    )
                }
            ) { Text("Create Program") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
