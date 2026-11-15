package com.example.ui.screens

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.catalog.WorkoutCatalog
import com.example.data.model.ExerciseDefinition
import com.example.data.model.PersonalRecord
import com.example.data.model.TrainingNote
import com.example.data.model.UserProfile
import com.example.data.model.WorkoutPlan
import com.example.data.model.WorkoutProgram
import com.example.data.model.WorkoutScheduleItem
import com.example.data.model.WorkoutSessionLog
import com.example.domain.WorkoutAnalyticsEngine
import com.example.platform.RestTimerSettings
import com.example.platform.todayDateKey
import com.example.ui.ActiveWorkoutState
import com.example.ui.AsyncState
import com.example.ui.components.AuroraAmbientBackground
import com.example.ui.components.DisclaimerBanner
import com.example.ui.components.ErrorBannerCard
import com.example.ui.components.FitTrackCard
import com.example.ui.components.GradientButton
import com.example.ui.components.ScreenHeader
import com.example.ui.components.SegmentedControl
import com.example.ui.components.SkeletonWorkoutList
import com.example.ui.screens.workouts.ActiveWorkoutScreen
import com.example.ui.screens.workouts.ExerciseLibraryView
import com.example.ui.screens.workouts.WorkoutAnalyticsView
import com.example.ui.screens.workouts.WorkoutBuilderSheet
import com.example.ui.screens.workouts.WorkoutCalendarView
import com.example.ui.screens.workouts.WorkoutDashboardView
import com.example.ui.screens.workouts.WorkoutProgramsView

@Composable
fun WorkoutsScreen(
    profile: UserProfile?,
    customPlans: List<WorkoutPlan>,
    customPrograms: List<WorkoutProgram>,
    sessions: List<WorkoutSessionLog>,
    schedules: List<WorkoutScheduleItem>,
    personalRecords: List<PersonalRecord>,
    trainingNotes: List<TrainingNote>,
    activeWorkout: ActiveWorkoutState?,
    aiSubstitutionsState: AsyncState<String>,
    aiWorkoutAnalysisState: AsyncState<String>,
    onStartWorkout: (WorkoutPlan) -> Unit,
    onToggleWorkoutPause: () -> Unit,
    onSelectExerciseIndex: (Int) -> Unit,
    onLogSet: (Int, String, Int, Double, Int?) -> Unit,
    onUpdateLoggedSet: (Int, Int, String, Int, Double) -> Unit,
    onDeleteLoggedSet: (Int, Int) -> Unit,
    onSkipExercise: (Int) -> Unit,
    onReplaceExercise: (Int, ExerciseDefinition) -> Unit,
    onUpdateExerciseNote: (Int, String) -> Unit,
    onUpdateSessionNotes: (String) -> Unit,
    onApplyProgressionConfirmation: (Int, Double, Long) -> Unit,
    onAdjustRest: (Int) -> Unit,
    onToggleRestPause: () -> Unit,
    onSkipRest: () -> Unit,
    onStartCustomRest: (Int) -> Unit,
    onUpdateRestSettings: (RestTimerSettings) -> Unit,
    onFinishWorkout: () -> Unit,
    onCancelWorkout: () -> Unit,
    onSaveWorkoutPlan: (WorkoutPlan) -> Unit,
    onDuplicateWorkoutPlan: (WorkoutPlan) -> Unit,
    onDeleteWorkoutPlan: (String) -> Unit,
    onSaveWorkoutProgram: (WorkoutProgram) -> Unit,
    onDeleteWorkoutProgram: (String) -> Unit,
    onUpsertSchedule: (WorkoutScheduleItem) -> Unit,
    onDeleteSchedule: (String) -> Unit,
    onSaveTrainingNote: (TrainingNote) -> Unit,
    onDeleteTrainingNote: (String) -> Unit,
    onRequestAiSubstitutions: (String, String, String, String) -> Unit,
    onResetAiSubstitutions: () -> Unit,
    onRequestAiWorkoutAnalysis: () -> Unit,
    onOpenAiWorkoutPlanner: () -> Unit
) {
    if (activeWorkout != null) {
        ActiveWorkoutScreen(
            active = activeWorkout,
            sessions = sessions,
            personalRecords = personalRecords,
            aiSubstitutionsState = aiSubstitutionsState,
            onToggleWorkoutPause = onToggleWorkoutPause,
            onSelectExerciseIndex = onSelectExerciseIndex,
            onLogSet = onLogSet,
            onUpdateLoggedSet = onUpdateLoggedSet,
            onDeleteLoggedSet = onDeleteLoggedSet,
            onSkipExercise = onSkipExercise,
            onReplaceExercise = onReplaceExercise,
            onUpdateExerciseNote = onUpdateExerciseNote,
            onUpdateSessionNotes = onUpdateSessionNotes,
            onApplyProgressionConfirmation = onApplyProgressionConfirmation,
            onAdjustRest = onAdjustRest,
            onToggleRestPause = onToggleRestPause,
            onSkipRest = onSkipRest,
            onStartCustomRest = onStartCustomRest,
            onUpdateRestSettings = onUpdateRestSettings,
            onRequestAiSubstitutions = onRequestAiSubstitutions,
            onResetAiSubstitutions = onResetAiSubstitutions,
            onFinishWorkout = onFinishWorkout,
            onDiscardWorkout = onCancelWorkout
        )
        return
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    var showQuickBuilder by remember { mutableStateOf(false) }

    val allPlans = remember(customPlans, profile) {
        customPlans + WorkoutCatalog.starterTemplatesForUser(profile)
    }
    val dashboardMetrics = remember(sessions, schedules) {
        WorkoutAnalyticsEngine.computeDashboardMetrics(sessions, schedules)
    }

    AuroraAmbientBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                ScreenHeader(
                    title = "Workouts",
                    subtitle = "${dashboardMetrics.totalWorkouts} sessions • ${dashboardMetrics.totalWorkingVolumeKg.toInt()} kg working vol",
                    trailing = {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { showQuickBuilder = true },
                                modifier = Modifier.testTag("workouts_header_build_button")
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Build")
                            }
                            Button(
                                onClick = onOpenAiWorkoutPlanner,
                                modifier = Modifier.testTag("workouts_header_ai_button")
                            ) {
                                Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("AI Plan")
                            }
                        }
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
                SegmentedControl(
                    options = listOf("Overview", "Programs", "Calendar", "Library", "Analytics"),
                    selectedIndex = selectedTab,
                    onSelect = { selectedTab = it }
                )
            }

            when (selectedTab) {
                0 -> WorkoutDashboardView(
                    metrics = dashboardMetrics,
                    allPlans = allPlans,
                    sessions = sessions,
                    schedules = schedules,
                    personalRecords = personalRecords,
                    trainingNotes = trainingNotes,
                    onStartWorkout = onStartWorkout,
                    onOpenBuilder = { showQuickBuilder = true },
                    onOpenCalendar = { selectedTab = 2 },
                    onOpenAiPlanner = onOpenAiWorkoutPlanner,
                    onOpenAnalytics = { selectedTab = 4 },
                    onSaveTrainingNote = onSaveTrainingNote,
                    onDeleteTrainingNote = onDeleteTrainingNote
                )

                1 -> WorkoutProgramsView(
                    profile = profile,
                    customPlans = customPlans,
                    customPrograms = customPrograms,
                    onStartWorkout = onStartWorkout,
                    onScheduleWorkoutForToday = { plan ->
                        val today = todayDateKey()
                        onUpsertSchedule(
                            WorkoutScheduleItem(
                                id = today,
                                dateKey = today,
                                workoutId = plan.id,
                                workoutName = plan.name,
                                programId = plan.programId,
                                status = "scheduled"
                            )
                        )
                    },
                    onSavePlan = onSaveWorkoutPlan,
                    onDuplicatePlan = onDuplicateWorkoutPlan,
                    onDeletePlan = onDeleteWorkoutPlan,
                    onSaveProgram = onSaveWorkoutProgram,
                    onDeleteProgram = onDeleteWorkoutProgram,
                    onOpenAiPlanner = onOpenAiWorkoutPlanner
                )

                2 -> WorkoutCalendarView(
                    allPlans = allPlans,
                    schedules = schedules,
                    sessions = sessions,
                    onUpsertSchedule = onUpsertSchedule,
                    onDeleteSchedule = onDeleteSchedule,
                    onStartWorkout = onStartWorkout
                )

                3 -> ExerciseLibraryView(
                    sessions = sessions,
                    aiSubstitutionsState = aiSubstitutionsState,
                    onRequestAiSubstitutions = onRequestAiSubstitutions,
                    onResetAiSubstitutions = onResetAiSubstitutions
                )

                else -> WorkoutAnalyticsView(
                    metrics = dashboardMetrics,
                    sessions = sessions,
                    personalRecords = personalRecords,
                    aiWorkoutAnalysisState = aiWorkoutAnalysisState,
                    onRequestAiAnalysis = onRequestAiWorkoutAnalysis
                )
            }
        }
    }

    if (showQuickBuilder) {
        WorkoutBuilderSheet(
            initialPlan = null,
            onDismiss = { showQuickBuilder = false },
            onSave = { plan ->
                onSaveWorkoutPlan(plan)
                showQuickBuilder = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AiWorkoutPlannerScreen(
    state: AsyncState<Pair<WorkoutPlan, String>>,
    onGenerate: (String, String, String, Int) -> Unit,
    onConfirmSavePlan: (WorkoutPlan) -> Unit,
    onBack: () -> Unit
) {
    var experience by remember { mutableStateOf("intermediate") }
    var equipment by remember { mutableStateOf("Dumbbells & Bodyweight") }
    var goal by remember { mutableStateOf("muscle_gain") }
    var days by remember { mutableIntStateOf(3) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Workout Planner") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            DisclaimerBanner(text = stringResource(R.string.ai_disclaimer))
            FitTrackCard(modifier = Modifier.fillMaxWidth()) {
                Text("Experience Level", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("beginner", "intermediate", "advanced").forEach { lvl ->
                        FilterChip(
                            selected = experience == lvl,
                            onClick = { experience = lvl },
                            label = { Text(lvl.replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Goal", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("muscle_gain", "general_fitness", "weight_loss", "weight_maintenance").forEach { g ->
                        FilterChip(
                            selected = goal == g,
                            onClick = { goal = g },
                            label = { Text(g.replace("_", " ").replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = equipment,
                    onValueChange = { equipment = it },
                    label = { Text("Available Equipment") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("Training Days / Week: $days", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(2, 3, 4, 5).forEach { d ->
                        FilterChip(selected = days == d, onClick = { days = d }, label = { Text("$d days") })
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                GradientButton(
                    text = "Generate Workout Plan",
                    icon = Icons.Filled.AutoAwesome,
                    loading = state is AsyncState.Loading,
                    onClick = { onGenerate(experience, equipment, goal, days) },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            when (state) {
                AsyncState.Idle -> Unit
                AsyncState.Loading, is AsyncState.Streaming -> SkeletonWorkoutList()
                is AsyncState.Error -> ErrorBannerCard(message = state.message)
                is AsyncState.Success -> {
                    val (plan, recovery) = state.data
                    FitTrackCard(modifier = Modifier.fillMaxWidth(), accentBorder = MaterialTheme.colorScheme.secondary) {
                        Text(plan.name, style = MaterialTheme.typography.titleLarge)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Recovery: $recovery", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
                        Spacer(modifier = Modifier.height(10.dp))
                        plan.exercises.forEach { ex ->
                            Text("• ${ex.exerciseName}: ${ex.targetSets} × ${ex.targetReps} (rest ${ex.restSeconds}s)", style = MaterialTheme.typography.bodyMedium)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        GradientButton(
                            text = "Review & Save to My Workouts",
                            icon = Icons.Filled.Check,
                            onClick = { onConfirmSavePlan(plan) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}
