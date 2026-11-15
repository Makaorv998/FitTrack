package com.example.ui.screens.workouts

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.PersonalRecord
import com.example.data.model.TrainingNote
import com.example.data.model.WorkoutPlan
import com.example.data.model.WorkoutScheduleItem
import com.example.data.model.WorkoutSessionLog
import com.example.domain.FitnessCalculator
import com.example.domain.WorkoutAnalyticsEngine
import com.example.domain.WorkoutDashboardMetrics
import com.example.platform.todayDateKey
import com.example.ui.branding.FitTrackGeometricIllustration
import com.example.ui.branding.FitTrackIllustrationType
import com.example.ui.components.AuroraProgressBar
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.FitTrackCard
import com.example.ui.components.GlassCard
import com.example.ui.components.GradientButton
import com.example.ui.components.SecondaryButton
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatCard
import com.example.ui.components.StatusBadge
import com.example.ui.theme.LocalFitTrackColors
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun WorkoutDashboardView(
    metrics: WorkoutDashboardMetrics,
    allPlans: List<WorkoutPlan>,
    sessions: List<WorkoutSessionLog>,
    schedules: List<WorkoutScheduleItem>,
    personalRecords: List<PersonalRecord>,
    trainingNotes: List<TrainingNote>,
    onStartWorkout: (WorkoutPlan) -> Unit,
    onOpenBuilder: () -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenAiPlanner: () -> Unit,
    onOpenAnalytics: () -> Unit,
    onSaveTrainingNote: (TrainingNote) -> Unit,
    onDeleteTrainingNote: (String) -> Unit
) {
    val ext = LocalFitTrackColors.current
    val today = remember { todayDateKey() }
    val todaySchedule = remember(schedules, today) { schedules.firstOrNull { it.dateKey == today } }
    val upcomingSchedule = remember(schedules, today) {
        schedules.filter { it.dateKey >= today && it.status == "scheduled" }.minByOrNull { it.dateKey }
    }
    val todayFocusPlan = remember(todaySchedule, upcomingSchedule, allPlans) {
        val targetId = todaySchedule?.workoutId?.ifBlank { null } ?: upcomingSchedule?.workoutId?.ifBlank { null }
        allPlans.firstOrNull { it.id == targetId } ?: allPlans.firstOrNull()
    }

    var showNoteDialog by remember { mutableStateOf(false) }
    var editingNote by remember { mutableStateOf<TrainingNote?>(null) }

    val weekDays = remember(today, schedules, sessions) {
        val cal = Calendar.getInstance()
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val dayFmt = SimpleDateFormat("EEE", Locale.US)
        val dow = cal.get(Calendar.DAY_OF_WEEK)
        val offsetFromMon = if (dow == Calendar.SUNDAY) -6 else Calendar.MONDAY - dow
        cal.add(Calendar.DAY_OF_YEAR, offsetFromMon)
        val sessionDates = sessions.mapNotNull { it.dateKey.ifBlank { null } }.toSet()
        (0..6).map { idx ->
            val dk = fmt.format(cal.time)
            val label = dayFmt.format(cal.time)
            val dom = cal.get(Calendar.DAY_OF_MONTH).toString()
            val sched = schedules.firstOrNull { it.dateKey == dk }
            val status = when {
                dk in sessionDates || sched?.status == "completed" -> "completed"
                sched?.status == "rest" -> "rest"
                sched?.status == "scheduled" && dk < today -> "missed"
                sched?.status == "scheduled" -> "scheduled"
                else -> "open"
            }
            cal.add(Calendar.DAY_OF_YEAR, 1)
            Triple(dk, "$label $dom", status)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Today's Focus Hero Card
        if (todayFocusPlan != null) {
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    accentBorder = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FitTrackGeometricIllustration(type = FitTrackIllustrationType.STRENGTH, size = 70.dp)
                        Column(modifier = Modifier.weight(1f)) {
                            StatusBadge(
                                text = when {
                                    todaySchedule?.status == "completed" -> "COMPLETED TODAY"
                                    todaySchedule?.status == "rest" -> "ACTIVE RECOVERY DAY"
                                    todaySchedule != null -> "SCHEDULED TODAY"
                                    else -> "RECOMMENDED FOCUS"
                                },
                                tint = if (todaySchedule?.status == "completed") ext.success else MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(todayFocusPlan.name, style = MaterialTheme.typography.titleLarge)
                            Text(
                                "${todayFocusPlan.exercises.size} exercises • ${todayFocusPlan.level.replaceFirstChar { it.uppercase() }}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    GradientButton(
                        text = "Start Workout",
                        icon = Icons.Filled.PlayArrow,
                        onClick = { onStartWorkout(todayFocusPlan) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("start_focus_workout_button")
                    )
                }
            }
        }

        // Weekly Strip & Upcoming Card
        item {
            FitTrackCard(modifier = Modifier.fillMaxWidth(), onClick = onOpenCalendar) {
                SectionHeader(
                    title = "This Week",
                    actionLabel = "Calendar",
                    onAction = onOpenCalendar
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    weekDays.forEach { (dk, label, status) ->
                        val isToday = dk == today
                        val dotColor = when (status) {
                            "completed" -> ext.success
                            "scheduled" -> MaterialTheme.colorScheme.primary
                            "rest" -> MaterialTheme.colorScheme.secondary
                            "missed" -> ext.warning
                            else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)
                        }
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = if (isToday) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else ext.surfaceElevated,
                            border = if (isToday) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = label.substringBefore(" "),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = label.substringAfter(" "),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(dotColor)
                                )
                            }
                        }
                    }
                }
                if (upcomingSchedule != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Next scheduled (${upcomingSchedule.dateKey}): ${upcomingSchedule.workoutName}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }

        // Athletic 4-KPI Summary Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard(
                        title = "Streak",
                        value = "${metrics.currentStreakDays}d",
                        subtitle = "${metrics.consistencyPercent}% consistency",
                        icon = Icons.Filled.LocalFireDepartment,
                        tint = ext.warning,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenAnalytics
                    )
                    StatCard(
                        title = "Sessions",
                        value = "${metrics.totalWorkouts}",
                        subtitle = String.format("%.1f / week", metrics.weeklyFrequency),
                        icon = Icons.Filled.FitnessCenter,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenAnalytics
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard(
                        title = "Working Vol",
                        value = "${metrics.totalWorkingVolumeKg.toInt()} kg",
                        subtitle = "Excludes warm-ups",
                        icon = Icons.Filled.EmojiEvents,
                        tint = ext.success,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenAnalytics
                    )
                    StatCard(
                        title = "Total Time",
                        value = "${metrics.totalMinutes} min",
                        subtitle = "Avg ${metrics.averageDurationMinutes} min",
                        icon = Icons.Filled.Timer,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenAnalytics
                    )
                }
            }
        }

        // Quick Creation & AI Row
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SecondaryButton(
                    text = "Build Workout",
                    icon = Icons.Filled.Add,
                    onClick = onOpenBuilder,
                    modifier = Modifier.testTag("dashboard_open_builder_button")
                )
                SecondaryButton(
                    text = "AI Planner",
                    icon = Icons.Filled.AutoAwesome,
                    onClick = onOpenAiPlanner,
                    modifier = Modifier.testTag("dashboard_open_ai_planner_button")
                )
                SecondaryButton(
                    text = "Schedule Day",
                    icon = Icons.Filled.CalendarMonth,
                    onClick = onOpenCalendar,
                    modifier = Modifier.testTag("dashboard_open_calendar_button")
                )
            }
        }

        // Personal Records Wall
        item {
            SectionHeader(
                title = "Personal Records (${personalRecords.size})",
                actionLabel = "Analytics",
                onAction = onOpenAnalytics
            )
        }
        if (personalRecords.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Filled.EmojiEvents,
                    title = "No personal records yet",
                    subtitle = "Log working sets in an active session to automatically record 1RM and top-set PRs."
                )
            }
        } else {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    personalRecords.take(8).forEach { pr ->
                        FitTrackCard(modifier = Modifier.width(185.dp), elevated = true) {
                            StatusBadge(text = pr.primaryMuscle.ifBlank { "PR" }.uppercase(), tint = ext.success)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(pr.exerciseName, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${WorkoutAnalyticsEngine.formatWeight(pr.weightKg)} × ${pr.reps}",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Est 1RM: ${String.format(Locale.US, "%.1f", pr.estimatedOneRmKg)} kg",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Muscle Distribution
        if (metrics.muscleDistribution.isNotEmpty()) {
            item {
                FitTrackCard(modifier = Modifier.fillMaxWidth()) {
                    SectionHeader(title = "Working Sets by Muscle")
                    Spacer(modifier = Modifier.height(10.dp))
                    val maxSets = (metrics.muscleDistribution.maxOfOrNull { it.second } ?: 1).coerceAtLeast(1)
                    metrics.muscleDistribution.take(5).forEachIndexed { idx, (muscle, count) ->
                        AuroraProgressBar(
                            label = muscle,
                            valueText = "$count sets",
                            progress = count.toFloat() / maxSets.toFloat(),
                            color = if (idx % 2 == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                        )
                        if (idx < 4) Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }

        // Recent Completed Sessions
        item {
            SectionHeader(title = "Recent Workouts (${sessions.size})")
        }
        if (sessions.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Filled.FitnessCenter,
                    title = "Ready for session one",
                    subtitle = "Start today's workout above to track sets, rest intervals, and progressive overload.",
                    illustration = FitTrackIllustrationType.STRENGTH
                )
            }
        } else {
            items(sessions.take(5), key = { it.id }) { s ->
                FitTrackCard(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(s.workoutName, style = MaterialTheme.typography.titleMedium)
                            if (s.dateKey.isNotBlank()) {
                                Text(s.dateKey, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                            }
                        }
                        if (s.personalRecordsHit > 0) {
                            StatusBadge(text = "${s.personalRecordsHit} PR", tint = ext.success)
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    val workVol = if (s.workingVolumeKg > 0.0) s.workingVolumeKg else s.totalVolumeKg
                    Text(
                        text = "${FitnessCalculator.formatDuration(s.durationSeconds)} • ${s.setsCompleted} sets • ${workVol.toInt()} kg working vol • ${s.caloriesBurned.toInt()} kcal",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (s.notes.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Note: ${s.notes}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Training Notes Section
        item {
            SectionHeader(
                title = "Training Notes (${trainingNotes.size})",
                actionLabel = "+ Add Note",
                onAction = {
                    editingNote = null
                    showNoteDialog = true
                }
            )
        }
        if (trainingNotes.isEmpty()) {
            item {
                FitTrackCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        editingNote = null
                        showNoteDialog = true
                    }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.AutoMirrored.Filled.NoteAdd, contentDescription = "Add note", tint = MaterialTheme.colorScheme.secondary)
                        Text(
                            "Tap to jot down technique cues, bench pin height, or fatigue notes.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(trainingNotes, key = { it.id }) { note ->
                FitTrackCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(note.title, style = MaterialTheme.typography.titleMedium)
                            if (note.exerciseName.isNotBlank() || note.dateKey.isNotBlank()) {
                                Text(
                                    listOf(note.dateKey, note.exerciseName).filter { it.isNotBlank() }.joinToString(" • "),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(note.body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Row {
                            IconButton(onClick = {
                                editingNote = note
                                showNoteDialog = true
                            }) {
                                Icon(Icons.Filled.Edit, contentDescription = "Edit note")
                            }
                            IconButton(onClick = { onDeleteTrainingNote(note.id) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete note", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showNoteDialog) {
        TrainingNoteDialog(
            initial = editingNote,
            onDismiss = { showNoteDialog = false },
            onSave = { note ->
                onSaveTrainingNote(note)
                showNoteDialog = false
            }
        )
    }
}

@Composable
private fun TrainingNoteDialog(
    initial: TrainingNote?,
    onDismiss: () -> Unit,
    onSave: (TrainingNote) -> Unit
) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var exercise by remember { mutableStateOf(initial?.exerciseName ?: "") }
    var body by remember { mutableStateOf(initial?.body ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "New Training Note" else "Edit Training Note") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title (e.g. Squat Stance Cue)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = exercise,
                    onValueChange = { exercise = it },
                    label = { Text("Exercise (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = body,
                    onValueChange = { body = it },
                    label = { Text("Notes / Form Cues") },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() || body.isNotBlank()) {
                        onSave(
                            (initial ?: TrainingNote()).copy(
                                title = title.trim().ifBlank { "Training Note" },
                                exerciseName = exercise.trim(),
                                body = body.trim(),
                                dateKey = initial?.dateKey?.ifBlank { todayDateKey() } ?: todayDateKey()
                            )
                        )
                    }
                }
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
