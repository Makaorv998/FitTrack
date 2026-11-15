package com.example.ui.screens.workouts

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.model.WorkoutPlan
import com.example.data.model.WorkoutScheduleItem
import com.example.data.model.WorkoutSessionLog
import com.example.domain.WorkoutAnalyticsEngine
import com.example.platform.todayDateKey
import com.example.ui.components.FitTrackCard
import com.example.ui.components.GlassCard
import com.example.ui.components.GradientButton
import com.example.ui.components.SecondaryButton
import com.example.ui.components.SectionHeader
import com.example.ui.components.SegmentedControl
import com.example.ui.components.StatusBadge
import com.example.ui.theme.LocalFitTrackColors
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WorkoutCalendarView(
    allPlans: List<WorkoutPlan>,
    schedules: List<WorkoutScheduleItem>,
    sessions: List<WorkoutSessionLog>,
    unitSystem: String = "metric",
    onUpsertSchedule: (WorkoutScheduleItem) -> Unit,
    onDeleteSchedule: (String) -> Unit,
    onStartWorkout: (WorkoutPlan) -> Unit
) {
    val ext = LocalFitTrackColors.current
    val today = remember { todayDateKey() }
    var viewMode by remember { mutableIntStateOf(0) } // 0 = Week, 1 = Month
    var monthOffset by remember { mutableIntStateOf(0) }
    var selectedDateKey by remember { mutableStateOf(today) }
    var showAssignDialog by remember { mutableStateOf(false) }

    val sessionDates = remember(sessions) {
        sessions.mapNotNull { it.dateKey.ifBlank { null } }.toSet()
    }
    val scheduleMap = remember(schedules) {
        schedules.associateBy { it.dateKey }
    }

    fun statusForDate(dateKey: String): String {
        val item = scheduleMap[dateKey]
        return when {
            dateKey in sessionDates || item?.status == "completed" -> "completed"
            item?.status == "rest" -> "rest"
            item?.status == "scheduled" && dateKey < today -> "missed"
            item?.status == "scheduled" -> "scheduled"
            else -> "open"
        }
    }

    val calendarGrid = remember(viewMode, monthOffset, today) {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val cal = Calendar.getInstance()
        if (viewMode == 0) {
            cal.add(Calendar.WEEK_OF_YEAR, monthOffset)
            val dow = cal.get(Calendar.DAY_OF_WEEK)
            val offsetFromMon = if (dow == Calendar.SUNDAY) -6 else Calendar.MONDAY - dow
            cal.add(Calendar.DAY_OF_YEAR, offsetFromMon)
            val title = "Week of ${SimpleDateFormat("MMM d", Locale.US).format(cal.time)}"
            val days = (0..6).map {
                val dk = fmt.format(cal.time)
                val dayNum = cal.get(Calendar.DAY_OF_MONTH)
                cal.add(Calendar.DAY_OF_YEAR, 1)
                dk to dayNum
            }
            title to days
        } else {
            cal.add(Calendar.MONTH, monthOffset)
            cal.set(Calendar.DAY_OF_MONTH, 1)
            val title = SimpleDateFormat("MMMM yyyy", Locale.US).format(cal.time)
            val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            val firstDow = cal.get(Calendar.DAY_OF_WEEK)
            val leadingBlanks = if (firstDow == Calendar.SUNDAY) 6 else firstDow - Calendar.MONDAY
            val list = mutableListOf<Pair<String, Int>?>()
            repeat(leadingBlanks) { list.add(null) }
            repeat(daysInMonth) {
                val dk = fmt.format(cal.time)
                val dayNum = cal.get(Calendar.DAY_OF_MONTH)
                list.add(dk to dayNum)
                cal.add(Calendar.DAY_OF_YEAR, 1)
            }
            while (list.size % 7 != 0) list.add(null)
            title to list
        }
    }

    val selectedSchedule = scheduleMap[selectedDateKey]
    val selectedStatus = statusForDate(selectedDateKey)
    val selectedSessions = remember(sessions, selectedDateKey) {
        sessions.filter { it.dateKey == selectedDateKey }
    }
    val linkedPlan = remember(selectedSchedule, allPlans) {
        allPlans.firstOrNull { it.id == selectedSchedule?.workoutId }
            ?: allPlans.firstOrNull { it.name == selectedSchedule?.workoutName }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SegmentedControl(
                options = listOf("Week View", "Month View"),
                selectedIndex = viewMode,
                onSelect = {
                    viewMode = it
                    monthOffset = 0
                }
            )
        }

        item {
            FitTrackCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { monthOffset -= 1 }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous period")
                    }
                    Text(calendarGrid.first, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    IconButton(onClick = { monthOffset += 1 }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next period")
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    listOf("M", "T", "W", "T", "F", "S", "S").forEach { d ->
                        Text(
                            text = d,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))

                val cells = calendarGrid.second
                cells.chunked(7).forEach { weekRow ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        weekRow.forEach { cell ->
                            if (cell == null) {
                                Box(modifier = Modifier.weight(1f).height(46.dp))
                            } else {
                                val (dk, dayNum) = cell
                                val st = statusForDate(dk)
                                val selected = dk == selectedDateKey
                                val dotColor = when (st) {
                                    "completed" -> ext.success
                                    "scheduled" -> MaterialTheme.colorScheme.primary
                                    "rest" -> MaterialTheme.colorScheme.secondary
                                    "missed" -> ext.warning
                                    else -> null
                                }
                                Surface(
                                    onClick = { selectedDateKey = dk },
                                    shape = MaterialTheme.shapes.small,
                                    color = when {
                                        selected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                                        dk == today -> ext.surfaceElevated
                                        else -> MaterialTheme.colorScheme.surface
                                    },
                                    border = if (selected) {
                                        BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                                    } else {
                                        BorderStroke(1.dp, ext.border.copy(alpha = 0.5f))
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = "$dayNum",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = if (selected || dk == today) FontWeight.Bold else FontWeight.Normal
                                        )
                                        if (dotColor != null) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Box(
                                                modifier = Modifier
                                                    .size(7.dp)
                                                    .clip(CircleShape)
                                                    .background(dotColor)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CalendarLegendDot("Completed", ext.success)
                    CalendarLegendDot("Scheduled", MaterialTheme.colorScheme.primary)
                    CalendarLegendDot("Rest", MaterialTheme.colorScheme.secondary)
                    CalendarLegendDot("Missed", ext.warning)
                }
            }
        }

        // Selected Date Panel
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                accentBorder = MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(selectedDateKey, style = MaterialTheme.typography.titleLarge)
                        Text(
                            when (selectedStatus) {
                                "completed" -> "Workout completed"
                                "scheduled" -> "Workout scheduled"
                                "rest" -> "Rest & recovery day"
                                "missed" -> "Missed session — reschedule anytime"
                                else -> "Open training day"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    StatusBadge(
                        text = selectedStatus.uppercase(),
                        tint = when (selectedStatus) {
                            "completed" -> ext.success
                            "scheduled" -> MaterialTheme.colorScheme.primary
                            "rest" -> MaterialTheme.colorScheme.secondary
                            "missed" -> ext.warning
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }

                if (selectedSchedule != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = selectedSchedule.workoutName.ifBlank { "Rest Day" },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    if (selectedSchedule.notes.isNotBlank()) {
                        Text(
                            text = selectedSchedule.notes,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (selectedSessions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    selectedSessions.forEach { sess ->
                        val workVol = if (sess.workingVolumeKg > 0.0) sess.workingVolumeKg else sess.totalVolumeKg
                        Text(
                            text = "✓ Logged: ${sess.workoutName} (${sess.setsCompleted} sets, ${WorkoutAnalyticsEngine.formatVolume(workVol, unitSystem)})",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ext.success
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GradientButton(
                        text = if (selectedSchedule == null) "Assign Workout" else "Change / Reschedule",
                        icon = Icons.Filled.CalendarMonth,
                        onClick = { showAssignDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("calendar_assign_workout_button")
                    )
                    SecondaryButton(
                        text = "Rest Day",
                        icon = Icons.Filled.Hotel,
                        onClick = {
                            onUpsertSchedule(
                                WorkoutScheduleItem(
                                    id = selectedDateKey,
                                    dateKey = selectedDateKey,
                                    workoutName = "Active Recovery / Rest Day",
                                    status = "rest",
                                    notes = "Prioritize sleep, hydration, and light mobility."
                                )
                            )
                        },
                        modifier = Modifier
                            .weight(0.75f)
                            .testTag("calendar_rest_day_button")
                    )
                }

                if (linkedPlan != null && selectedStatus != "rest") {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { onStartWorkout(linkedPlan) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Start ${linkedPlan.name}")
                    }
                }

                if (selectedSchedule != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    TextButton(
                        onClick = { onDeleteSchedule(selectedSchedule.id.ifBlank { selectedDateKey }) },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clear Day", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }

    if (showAssignDialog) {
        AssignWorkoutDialog(
            initialDateKey = selectedDateKey,
            allPlans = allPlans,
            onDismiss = { showAssignDialog = false },
            onConfirm = { targetDate, plan, notes ->
                onUpsertSchedule(
                    WorkoutScheduleItem(
                        id = targetDate,
                        dateKey = targetDate,
                        workoutId = plan.id,
                        workoutName = plan.name,
                        programId = plan.programId,
                        status = "scheduled",
                        notes = notes
                    )
                )
                selectedDateKey = targetDate
                showAssignDialog = false
            }
        )
    }
}

@Composable
private fun CalendarLegendDot(label: String, color: androidx.compose.ui.graphics.Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AssignWorkoutDialog(
    initialDateKey: String,
    allPlans: List<WorkoutPlan>,
    onDismiss: () -> Unit,
    onConfirm: (String, WorkoutPlan, String) -> Unit
) {
    var dateKeyText by remember { mutableStateOf(initialDateKey) }
    var selectedPlan by remember { mutableStateOf(allPlans.firstOrNull()) }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Schedule Workout") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = dateKeyText,
                    onValueChange = { dateKeyText = it },
                    label = { Text("Date (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Choose Workout:", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    allPlans.take(10).forEach { plan ->
                        FilterChip(
                            selected = selectedPlan?.id == plan.id && selectedPlan?.name == plan.name,
                            onClick = { selectedPlan = plan },
                            label = { Text(plan.name) }
                        )
                    }
                }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Session note (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                enabled = selectedPlan != null && dateKeyText.matches(Regex("\\d{4}-\\d{2}-\\d{2}")),
                onClick = { selectedPlan?.let { onConfirm(dateKeyText, it, notes) } }
            ) { Text("Save Schedule") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
