package com.example

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.outlined.DirectionsRun
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.credentials.CredentialManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.data.firebase.AuthManager
import com.example.data.firebase.configureAppCheck
import com.example.di.AppContainer
import com.example.platform.UpdateNotificationManager
import com.example.ui.AppUpdateViewModel
import com.example.ui.FitTrackViewModel
import com.example.ui.components.AuroraAmbientBackground
import com.example.ui.components.BottomNavTabItem
import com.example.ui.components.ErrorBannerCard
import com.example.ui.components.FloatingAIButton
import com.example.ui.components.FloatingBottomNavigationBar
import com.example.ui.components.OfflineStatusBanner
import com.example.ui.components.SkeletonDashboard
import com.example.ui.navigation.FitTrackRoute
import com.example.ui.screens.ActivityMapScreen
import com.example.ui.screens.ActivityScreen
import com.example.ui.screens.AiCoachScreen
import com.example.ui.screens.AiFoodScannerScreen
import com.example.ui.screens.AiMealPlannerScreen
import com.example.ui.screens.AiWorkoutPlannerScreen
import com.example.ui.screens.AuroraSplashScreen
import com.example.ui.screens.BodyProgressScreen
import com.example.ui.screens.CalculatorsScreen
import com.example.ui.screens.ChallengesScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.NutritionScreen
import com.example.ui.screens.OnboardingWizardScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.UpdateCenterScreen
import com.example.ui.screens.WelcomeAuthScreen
import com.example.ui.screens.WhatsNewScreen
import com.example.ui.screens.WorkoutsScreen
import com.example.ui.theme.FitTrackTheme
import com.example.ui.theme.rememberReducedMotion
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val pendingUpdateNavTarget = MutableStateFlow<Pair<String, Long?>?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        // Must configure App Check before setContent, ViewModels, or Firebase API calls.
        configureAppCheck(this, intent)
        super.onCreate(savedInstanceState)
        UpdateNotificationManager(this).ensureNotificationChannels()
        handleUpdateIntent(intent)
        enableEdgeToEdge()
        setContent {
            val pendingTarget by pendingUpdateNavTarget.collectAsStateWithLifecycle()
            FitTrackAppRoot(
                pendingUpdateTarget = pendingTarget,
                onConsumeUpdateNavTarget = { pendingUpdateNavTarget.value = null }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleUpdateIntent(intent)
    }

    private fun handleUpdateIntent(intent: Intent?) {
        val target = intent?.getStringExtra(UpdateNotificationManager.EXTRA_UPDATE_NAV_TARGET)
        if (!target.isNullOrBlank()) {
            val ver = intent.getLongExtra(UpdateNotificationManager.EXTRA_RELEASE_VERSION_CODE, -1L)
                .takeIf { it > 0L }
            pendingUpdateNavTarget.value = target to ver
            intent.removeExtra(UpdateNotificationManager.EXTRA_UPDATE_NAV_TARGET)
        }
    }
}

private fun observeAuthState(context: Context) = callbackFlow<FirebaseUser?> {
    if (FirebaseApp.getApps(context).isEmpty()) {
        trySend(null)
        close()
        return@callbackFlow
    }
    val auth = Firebase.auth
    val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        trySend(firebaseAuth.currentUser)
    }
    auth.addAuthStateListener(listener)
    trySend(auth.currentUser)
    awaitClose { auth.removeAuthStateListener(listener) }
}

@Composable
private fun FitTrackAppRoot(
    pendingUpdateTarget: Pair<String, Long?>? = null,
    onConsumeUpdateNavTarget: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val firebaseReady = remember(context) {
        FirebaseApp.getApps(context).isNotEmpty() || FirebaseApp.initializeApp(context) != null
    }
    if (!firebaseReady) {
        FitTrackTheme(themePreference = "dark") {
            AuroraAmbientBackground {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    ErrorBannerCard(
                        message = "Firebase configuration is unavailable. Verify Google Services setup and restart the app."
                    )
                }
            }
        }
        return
    }

    val credentialManager = remember(context) { CredentialManager.create(context) }
    val currentUser by remember { observeAuthState(context) }.collectAsStateWithLifecycle(
        initialValue = runCatching { Firebase.auth.currentUser }.getOrNull()
    )
    var autoSignInChecked by remember { mutableStateOf(currentUser != null) }
    val reducedMotion = rememberReducedMotion()
    var splashReady by remember { mutableStateOf(reducedMotion) }

    LaunchedEffect(reducedMotion) {
        if (!reducedMotion && !splashReady) {
            delay(850)
            splashReady = true
        }
    }

    LaunchedEffect(Unit) {
        if (currentUser == null) {
            AuthManager.attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                scope = scope,
                onAuthSuccess = { autoSignInChecked = true },
                onUnauthenticated = { autoSignInChecked = true }
            )
        } else {
            autoSignInChecked = true
        }
    }

    val user = currentUser
    if (!autoSignInChecked || !splashReady) {
        FitTrackTheme(themePreference = "dark") {
            AuroraSplashScreen()
        }
        return
    }

    if (user == null) {
        FitTrackTheme(themePreference = "dark") {
            WelcomeAuthScreen(onAuthSuccess = { autoSignInChecked = true })
        }
        return
    }

    key(user.uid) {
        AuthenticatedFitTrackShell(
            user = user,
            pendingUpdateTarget = pendingUpdateTarget,
            onConsumeUpdateNavTarget = onConsumeUpdateNavTarget,
            onSignOut = {
                AuthManager.signOut(credentialManager = credentialManager, scope = scope)
            }
        )
    }
}

@Composable
private fun AuthenticatedFitTrackShell(
    user: FirebaseUser,
    pendingUpdateTarget: Pair<String, Long?>?,
    onConsumeUpdateNavTarget: () -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val appContainer = remember(appContext) { AppContainer.getInstance(appContext) }
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val scope = rememberCoroutineScope()
    val viewModel: FitTrackViewModel = viewModel(
        key = user.uid,
        factory = remember(user.uid, appContainer) {
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return appContainer.createFitTrackViewModel(
                        userId = user.uid,
                        userEmail = user.email.orEmpty(),
                        userDisplayName = user.displayName.orEmpty(),
                        userPhotoUrl = user.photoUrl?.toString().orEmpty()
                    ) as T
                }
            }
        }
    )
    val updateViewModel: AppUpdateViewModel = viewModel(
        key = "app_update_${user.uid}",
        factory = remember(appContainer) {
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return appContainer.createAppUpdateViewModel() as T
                }
            }
        }
    )
    val updateUiState by updateViewModel.uiState.collectAsStateWithLifecycle()

    val playUpdateLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        updateViewModel.onPlayUpdateResult(result.resultCode)
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        updateViewModel.refreshSystemPermissions()
    }
    val unknownSourcesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        updateViewModel.refreshSystemPermissions()
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, updateViewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                updateViewModel.onAppForegrounded(forceCheck = false)
                updateViewModel.resumePlayUpdateIfInProgress(playUpdateLauncher)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val profileLoaded by viewModel.profileLoaded.collectAsStateWithLifecycle()
    val profile by viewModel.userProfile.collectAsStateWithLifecycle()
    val trainingPreferences by viewModel.trainingPreferences.collectAsStateWithLifecycle()
    val nutritionPreferences by viewModel.nutritionPreferences.collectAsStateWithLifecycle()
    val savedRecipes by viewModel.savedRecipes.collectAsStateWithLifecycle()
    val plannedMeals by viewModel.plannedMeals.collectAsStateWithLifecycle()
    val recentFoods by viewModel.recentFoods.collectAsStateWithLifecycle()
    val todaySteps by viewModel.todaySteps.collectAsStateWithLifecycle()
    val stepSensor by viewModel.stepSensorState.collectAsStateWithLifecycle()
    val stepRecords by viewModel.stepRecords.collectAsStateWithLifecycle()
    val gpsState by viewModel.gpsTrackerState.collectAsStateWithLifecycle()
    val gpsHistory by viewModel.gpsActivities.collectAsStateWithLifecycle()
    val customFoods by viewModel.customFoods.collectAsStateWithLifecycle()
    val meals by viewModel.meals.collectAsStateWithLifecycle()
    val waterLogs by viewModel.waterLogs.collectAsStateWithLifecycle()
    val workoutPlans by viewModel.workoutPlans.collectAsStateWithLifecycle()
    val workoutPrograms by viewModel.workoutPrograms.collectAsStateWithLifecycle()
    val workoutSessions by viewModel.workoutSessions.collectAsStateWithLifecycle()
    val workoutSchedules by viewModel.workoutSchedules.collectAsStateWithLifecycle()
    val personalRecords by viewModel.personalRecords.collectAsStateWithLifecycle()
    val trainingNotes by viewModel.trainingNotes.collectAsStateWithLifecycle()
    val activeWorkout by viewModel.activeWorkout.collectAsStateWithLifecycle()
    val weightRecords by viewModel.weightRecords.collectAsStateWithLifecycle()
    val bodyMeasurements by viewModel.bodyMeasurements.collectAsStateWithLifecycle()
    val challenges by viewModel.challenges.collectAsStateWithLifecycle()
    val aiMessages by viewModel.aiMessages.collectAsStateWithLifecycle()
    val progressPhotos by viewModel.progressPhotos.collectAsStateWithLifecycle()
    val goalHistory by viewModel.goalHistory.collectAsStateWithLifecycle()

    val coachState by viewModel.coachState.collectAsStateWithLifecycle()
    val foodScanState by viewModel.foodScanState.collectAsStateWithLifecycle()
    val aiWorkoutState by viewModel.aiWorkoutState.collectAsStateWithLifecycle()
    val aiMealPlanState by viewModel.aiMealPlanState.collectAsStateWithLifecycle()
    val aiSubstitutionsState by viewModel.aiSubstitutionsState.collectAsStateWithLifecycle()
    val aiWorkoutAnalysisState by viewModel.aiWorkoutAnalysisState.collectAsStateWithLifecycle()
    val bannerError by viewModel.bannerError.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()

    FitTrackTheme(
        themePreference = profile?.themePreference ?: "dark",
        reducedMotionOverride = profile?.reducedMotionEnabled == true
    ) {
        if (!profileLoaded) {
            AuroraAmbientBackground {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 24.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    SkeletonDashboard(modifier = Modifier.widthIn(max = 580.dp))
                }
            }
            return@FitTrackTheme
        }

        if (profile?.onboardingCompleted == false) {
            OnboardingWizardScreen(
                initialName = profile?.displayName ?: user.displayName.orEmpty(),
                onComplete = { name, age, sex, height, weight, targetWeight, activityLevel, goal, sensorConsent, locationConsent ->
                    viewModel.completeOnboarding(
                        displayName = name,
                        ageYears = age,
                        sex = sex,
                        heightCm = height,
                        weightKg = weight,
                        targetWeightKg = targetWeight,
                        activityLevel = activityLevel,
                        goal = goal,
                        sensorConsent = sensorConsent,
                        locationConsent = locationConsent
                    )
                }
            )
            return@FitTrackTheme
        }

        val navController = rememberNavController()
        val backStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = backStackEntry?.destination
        val snackbarHostState = remember { SnackbarHostState() }

        LaunchedEffect(statusMessage) {
            statusMessage?.let { msg ->
                snackbarHostState.showSnackbar(msg)
                viewModel.clearStatusMessage()
            }
        }

        LaunchedEffect(
            profileLoaded,
            profile?.onboardingCompleted,
            updateUiState.shouldAutoShowWhatsNew,
            updateUiState.pendingWhatsNewVersionCode
        ) {
            if (profileLoaded && profile?.onboardingCompleted == true && updateUiState.shouldAutoShowWhatsNew) {
                val targetVersion = updateUiState.pendingWhatsNewVersionCode
                if (targetVersion != null) {
                    updateViewModel.acknowledgeWhatsNewPresented(targetVersion)
                }
                navController.navigate(FitTrackRoute.WhatsNew(targetVersion)) {
                    launchSingleTop = true
                }
            }
        }

        LaunchedEffect(profileLoaded, profile?.onboardingCompleted, pendingUpdateTarget) {
            if (profileLoaded && profile?.onboardingCompleted == true && pendingUpdateTarget != null) {
                val (target, versionCode) = pendingUpdateTarget
                when (target) {
                    UpdateNotificationManager.TARGET_WHATS_NEW -> {
                        navController.navigate(FitTrackRoute.WhatsNew(versionCode)) {
                            launchSingleTop = true
                        }
                    }
                    UpdateNotificationManager.TARGET_UPDATE_CENTER -> {
                        navController.navigate(FitTrackRoute.UpdateCenter) {
                            launchSingleTop = true
                        }
                    }
                }
                onConsumeUpdateNavTarget()
            }
        }

        val primaryTabs = listOf(
            BottomNavTabItem(
                route = FitTrackRoute.Home,
                label = stringResource(R.string.nav_home),
                activeIcon = Icons.Filled.Home,
                inactiveIcon = Icons.Outlined.Home
            ),
            BottomNavTabItem(
                route = FitTrackRoute.Activity,
                label = stringResource(R.string.nav_activity),
                activeIcon = Icons.AutoMirrored.Filled.DirectionsRun,
                inactiveIcon = Icons.AutoMirrored.Outlined.DirectionsRun
            ),
            BottomNavTabItem(
                route = FitTrackRoute.Workouts,
                label = stringResource(R.string.nav_workouts),
                activeIcon = Icons.Filled.FitnessCenter,
                inactiveIcon = Icons.Outlined.FitnessCenter
            ),
            BottomNavTabItem(
                route = FitTrackRoute.Nutrition,
                label = stringResource(R.string.nav_nutrition),
                activeIcon = Icons.Filled.Restaurant,
                inactiveIcon = Icons.Outlined.Restaurant
            ),
            BottomNavTabItem(
                route = FitTrackRoute.Profile,
                label = stringResource(R.string.nav_profile),
                activeIcon = Icons.Filled.Person,
                inactiveIcon = Icons.Outlined.Person
            )
        )

        val isPrimaryRoute = currentDestination?.let { dest ->
            primaryTabs.any { dest.hasRoute(it.route::class) }
        } ?: true

        fun navigateTopLevel(route: FitTrackRoute) {
            navController.navigate(route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useRail = maxWidth >= 640.dp
            Row(modifier = Modifier.fillMaxSize()) {
                if (useRail && isPrimaryRoute) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        Spacer(modifier = Modifier.height(12.dp))
                        primaryTabs.forEach { tab ->
                            val selected = currentDestination?.hasRoute(tab.route::class) == true
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTopLevel(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.activeIcon else tab.inactiveIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) }
                            )
                        }
                    }
                }

                Scaffold(
                    modifier = Modifier.weight(1f),
                    containerColor = Color.Transparent,
                    contentWindowInsets = WindowInsets.statusBars,
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    floatingActionButton = {
                        if (isPrimaryRoute) {
                            FloatingAIButton(
                                onClick = { navController.navigate(FitTrackRoute.AiCoach) },
                                modifier = Modifier.testTag("fab_ai_coach")
                            )
                        }
                    },
                    bottomBar = {
                        if (!useRail && isPrimaryRoute) {
                            FloatingBottomNavigationBar(
                                tabs = primaryTabs,
                                isSelected = { route -> currentDestination?.hasRoute(route::class) == true },
                                onSelect = { route -> navigateTopLevel(route) }
                            )
                        }
                    }
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = innerPadding.calculateTopPadding())
                            .consumeWindowInsets(WindowInsets.statusBars)
                    ) {
                        if (!isOnline) {
                            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                                OfflineStatusBanner()
                            }
                        }
                        if (bannerError != null) {
                            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                                ErrorBannerCard(
                                    message = bannerError!!,
                                    onDismiss = { viewModel.clearBannerError() }
                                )
                            }
                        }

                        NavHost(
                            navController = navController,
                            startDestination = FitTrackRoute.Home,
                            modifier = Modifier
                                .fillMaxSize()
                                .widthIn(max = 900.dp)
                                .align(Alignment.CenterHorizontally)
                        ) {
                            composable<FitTrackRoute.Home> {
                                HomeScreen(
                                    profile = profile,
                                    todaySteps = todaySteps,
                                    stepRecords = stepRecords,
                                    meals = meals,
                                    waterLogs = waterLogs,
                                    gpsActivities = gpsHistory,
                                    workoutPlans = workoutPlans,
                                    workoutSchedules = workoutSchedules,
                                    workoutSessions = workoutSessions,
                                    challenges = challenges,
                                    onQuickAddWater = { viewModel.addWater(it) },
                                    onOpenActivity = { navigateTopLevel(FitTrackRoute.Activity) },
                                    onOpenNutrition = { navigateTopLevel(FitTrackRoute.Nutrition) },
                                    onOpenFoodScanner = { navController.navigate(FitTrackRoute.AiFoodScanner) },
                                    onOpenAiCoach = { navController.navigate(FitTrackRoute.AiCoach) },
                                    onOpenWorkouts = { navigateTopLevel(FitTrackRoute.Workouts) },
                                    onStartWorkout = { plan ->
                                        viewModel.startActiveWorkout(plan)
                                        navigateTopLevel(FitTrackRoute.Workouts)
                                    },
                                    onOpenChallenges = { navController.navigate(FitTrackRoute.Challenges) },
                                    updateUiState = updateUiState,
                                    onOpenUpdateCenter = { navController.navigate(FitTrackRoute.UpdateCenter) },
                                    onOpenWhatsNew = { navController.navigate(FitTrackRoute.WhatsNew()) }
                                )
                            }

                            composable<FitTrackRoute.Activity> {
                                ActivityScreen(
                                    profile = profile,
                                    stepSensor = stepSensor,
                                    todaySteps = todaySteps,
                                    stepRecords = stepRecords,
                                    gpsState = gpsState,
                                    gpsHistory = gpsHistory,
                                    onAddManualSteps = { viewModel.addManualSteps(it) },
                                    onSyncSteps = { viewModel.syncTodaySteps() },
                                    onStartGps = { type -> viewModel.startGpsTracking(context, type) },
                                    onPauseGps = { viewModel.pauseGpsTracking(context) },
                                    onResumeGps = { viewModel.resumeGpsTracking(context) },
                                    onFinishGps = { viewModel.finishAndSaveGpsTracking(context) },
                                    onDiscardGps = { viewModel.discardGpsTracking(context) },
                                    onDeleteGpsActivity = { id -> viewModel.deleteGpsActivity(id) },
                                    onOpenReadOnlyMap = { activityId ->
                                        navController.navigate(FitTrackRoute.ActivityMap(activityId))
                                    }
                                )
                            }

                            composable<FitTrackRoute.Nutrition> {
                                NutritionScreen(
                                    profile = profile,
                                    meals = meals,
                                    waterLogs = waterLogs,
                                    customFoods = customFoods,
                                    recentFoods = recentFoods,
                                    savedRecipes = savedRecipes,
                                    plannedMeals = plannedMeals,
                                    nutritionPreferences = nutritionPreferences,
                                    onLogFood = { food, grams, mealType -> viewModel.logFoodToMeal(food, grams, mealType) },
                                    onLogFoodWithDate = { food, grams, mealType, dateKey ->
                                        viewModel.logFoodToMeal(food, grams, mealType, dateKey = dateKey)
                                    },
                                    onLogDirectMeal = { meal, dateKey ->
                                        viewModel.logDirectMeal(meal, dateKey = dateKey)
                                    },
                                    onUpdateMealGrams = { meal, newGrams -> viewModel.updateMealGrams(meal, newGrams) },
                                    onDeleteMeal = { id -> viewModel.deleteMeal(id) },
                                    onSaveCustomFood = { food -> viewModel.saveCustomFood(food) },
                                    onToggleFavorite = { food -> viewModel.toggleFavoriteFood(food) },
                                    onAddWater = { amount -> viewModel.addWater(amount) },
                                    onDeleteWaterLog = { id -> viewModel.deleteWaterLog(id) },
                                    onSaveNutritionPreferences = { viewModel.updateNutritionPreferences(it) },
                                    onSaveRecipe = { viewModel.saveRecipe(it) },
                                    onDeleteRecipe = { viewModel.deleteRecipe(it) },
                                    onLogRecipe = { recipe, mealType, servings ->
                                        viewModel.logRecipeToMeal(recipe, mealType, servings)
                                    },
                                    onSavePlannedMeal = { viewModel.savePlannedMeal(it) },
                                    onDeletePlannedMeal = { viewModel.deletePlannedMeal(it) },
                                    onOpenAiScanner = { navController.navigate(FitTrackRoute.AiFoodScanner) },
                                    onOpenAiMealPlanner = { navController.navigate(FitTrackRoute.AiMealPlanner) }
                                )
                            }

                            composable<FitTrackRoute.Workouts> {
                                WorkoutsScreen(
                                    profile = profile,
                                    trainingPreferences = trainingPreferences,
                                    customPlans = workoutPlans,
                                    customPrograms = workoutPrograms,
                                    sessions = workoutSessions,
                                    schedules = workoutSchedules,
                                    personalRecords = personalRecords,
                                    trainingNotes = trainingNotes,
                                    activeWorkout = activeWorkout,
                                    aiSubstitutionsState = aiSubstitutionsState,
                                    aiWorkoutAnalysisState = aiWorkoutAnalysisState,
                                    onStartWorkout = { viewModel.startActiveWorkout(it) },
                                    onSelectActiveProgram = { program ->
                                        viewModel.updateTrainingPreferences(
                                            trainingPreferences.copy(
                                                activeProgramId = program.id,
                                                activeProgramName = program.name,
                                                targetDaysPerWeek = program.daysPerWeek.toInt().coerceIn(1, 7)
                                            )
                                        )
                                    },
                                    onToggleWorkoutPause = { viewModel.toggleWorkoutPause() },
                                    onSelectExerciseIndex = { viewModel.selectExerciseIndex(it) },
                                    onLogSet = { exIdx, setType, reps, weightKg, rest ->
                                        viewModel.logActiveSet(exIdx, setType, reps, weightKg, rest)
                                    },
                                    onUpdateLoggedSet = { exIdx, setIdx, setType, reps, weightKg ->
                                        viewModel.updateLoggedSet(exIdx, setIdx, setType, reps, weightKg)
                                    },
                                    onDeleteLoggedSet = { exIdx, setIdx ->
                                        viewModel.deleteLoggedSet(exIdx, setIdx)
                                    },
                                    onSkipExercise = { viewModel.skipExercise(it) },
                                    onReplaceExercise = { exIdx, replacement ->
                                        viewModel.replaceExerciseInActiveWorkout(exIdx, replacement)
                                    },
                                    onUpdateExerciseNote = { exIdx, note ->
                                        viewModel.updateExerciseNote(exIdx, note)
                                    },
                                    onUpdateSessionNotes = { viewModel.updateSessionNotes(it) },
                                    onApplyProgressionConfirmation = { exIdx, weightKg, reps ->
                                        viewModel.applyProgressionConfirmation(exIdx, weightKg, reps)
                                    },
                                    onAdjustRest = { viewModel.adjustRestTimer(it) },
                                    onToggleRestPause = { viewModel.toggleRestPause() },
                                    onSkipRest = { viewModel.skipRestTimer() },
                                    onStartCustomRest = { sec ->
                                        val name = activeWorkout?.let { st ->
                                            st.records.getOrNull(st.currentExerciseIndex)?.exerciseName
                                        } ?: "Set"
                                        viewModel.startRestTimer(sec, name)
                                    },
                                    onUpdateRestSettings = { viewModel.updateRestSettings(it) },
                                    onFinishWorkout = { viewModel.finishActiveWorkout() },
                                    onCancelWorkout = { viewModel.cancelActiveWorkout() },
                                    onSaveWorkoutPlan = { viewModel.saveWorkoutPlan(it) },
                                    onDuplicateWorkoutPlan = { viewModel.duplicateWorkoutPlan(it) },
                                    onDeleteWorkoutPlan = { viewModel.deleteWorkoutPlan(it) },
                                    onSaveWorkoutProgram = { viewModel.saveWorkoutProgram(it) },
                                    onDeleteWorkoutProgram = { viewModel.deleteWorkoutProgram(it) },
                                    onUpsertSchedule = { viewModel.upsertWorkoutSchedule(it) },
                                    onDeleteSchedule = { viewModel.deleteWorkoutSchedule(it) },
                                    onSaveTrainingNote = { viewModel.saveTrainingNote(it) },
                                    onDeleteTrainingNote = { viewModel.deleteTrainingNote(it) },
                                    onRequestAiSubstitutions = { name, muscle, equip, reason ->
                                        viewModel.requestAiSubstitutions(name, muscle, equip, reason)
                                    },
                                    onResetAiSubstitutions = { viewModel.resetAiSubstitutions() },
                                    onRequestAiWorkoutAnalysis = { viewModel.requestAiWorkoutAnalysis() },
                                    onOpenAiWorkoutPlanner = { navController.navigate(FitTrackRoute.AiWorkoutPlanner) }
                                )
                            }

                            composable<FitTrackRoute.Profile> {
                                ProfileScreen(
                                    profile = profile,
                                    trainingPreferences = trainingPreferences,
                                    nutritionPreferences = nutritionPreferences,
                                    goalHistory = goalHistory,
                                    onSavePersonalInfoAndGoals = { name, age, sex, height, weight, targetWeight, actLevel, steps, water, cals, goal ->
                                        viewModel.updatePersonalInfoAndGoals(
                                            displayName = name,
                                            ageYears = age,
                                            sex = sex,
                                            heightCm = height,
                                            weightKg = weight,
                                            targetWeightKg = targetWeight,
                                            activityLevel = actLevel,
                                            stepGoal = steps,
                                            waterGoalMl = water,
                                            calorieGoal = cals,
                                            goal = goal
                                        )
                                    },
                                    onSavePersonalInfoFull = { name, age, dobIso, sex, height, weight, targetWeight, actLevel, steps, water, cals, goal ->
                                        viewModel.updatePersonalInfoAndGoals(
                                            displayName = name,
                                            ageYears = age,
                                            sex = sex,
                                            heightCm = height,
                                            weightKg = weight,
                                            targetWeightKg = targetWeight,
                                            activityLevel = actLevel,
                                            stepGoal = steps,
                                            waterGoalMl = water,
                                            calorieGoal = cals,
                                            goal = goal,
                                            dobIso = dobIso
                                        )
                                    },
                                    onSaveGoalHistoryEntry = { viewModel.saveGoalHistoryEntry(it) },
                                    onUpdateGoalStatus = { entry, status -> viewModel.updateGoalStatus(entry, status) },
                                    onDeleteGoalHistoryEntry = { viewModel.deleteGoalHistoryEntry(it) },
                                    onSaveTrainingPreferences = { viewModel.updateTrainingPreferences(it) },
                                    onSaveNutritionPreferences = { viewModel.updateNutritionPreferences(it) },
                                    onOpenBodyProgress = { navController.navigate(FitTrackRoute.BodyProgress) },
                                    onOpenCalculators = { navController.navigate(FitTrackRoute.Calculators) },
                                    onOpenChallenges = { navController.navigate(FitTrackRoute.Challenges) },
                                    onOpenAiCoach = { navController.navigate(FitTrackRoute.AiCoach) },
                                    onOpenSettings = { navController.navigate(FitTrackRoute.Settings) }
                                )
                            }

                            composable<FitTrackRoute.ActivityMap> { entry ->
                                val route = entry.toRoute<FitTrackRoute.ActivityMap>()
                                ActivityMapScreen(
                                    gpsHistory = gpsHistory,
                                    initialActivityId = route.activityId,
                                    unitSystem = profile?.unitSystem ?: "metric",
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable<FitTrackRoute.AiCoach> {
                                AiCoachScreen(
                                    messages = aiMessages,
                                    coachState = coachState,
                                    onSendMessage = { viewModel.sendCoachMessage(it) },
                                    onClearHistory = { viewModel.clearAiChatHistory() },
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable<FitTrackRoute.AiFoodScanner> {
                                AiFoodScannerScreen(
                                    scanState = foodScanState,
                                    onScanBitmap = { viewModel.scanFoodBitmap(it) },
                                    onConfirmLogMeal = { meal ->
                                        viewModel.logDirectMeal(meal)
                                    },
                                    onReset = { viewModel.resetFoodScan() },
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable<FitTrackRoute.AiMealPlanner> {
                                AiMealPlannerScreen(
                                    state = aiMealPlanState,
                                    nutritionPreferences = nutritionPreferences,
                                    onGenerate = { prefs, allergies -> viewModel.generateAiMealPlan(prefs, allergies) },
                                    onLogSuggestion = { meal -> viewModel.logDirectMeal(meal) },
                                    onSaveToPlan = { entry -> viewModel.savePlannedMeal(entry) },
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable<FitTrackRoute.AiWorkoutPlanner> {
                                AiWorkoutPlannerScreen(
                                    state = aiWorkoutState,
                                    onGenerate = { exp, equip, goal, days -> viewModel.generateAiWorkoutPlan(exp, equip, goal, days) },
                                    onConfirmSavePlan = { plan ->
                                        viewModel.saveWorkoutPlan(plan)
                                        navController.popBackStack()
                                    },
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable<FitTrackRoute.BodyProgress> {
                                BodyProgressScreen(
                                    profile = profile,
                                    weightRecords = weightRecords,
                                    measurements = bodyMeasurements,
                                    stepRecords = stepRecords,
                                    meals = meals,
                                    gpsActivities = gpsHistory,
                                    workoutSessions = workoutSessions,
                                    progressPhotos = progressPhotos,
                                    onLogWeight = { weight, note -> viewModel.logWeight(weight, note) },
                                    onLogMeasurements = { w, c, a, l, h -> viewModel.logMeasurements(w, c, a, l, h) },
                                    onAddProgressPhoto = { uri, caption -> viewModel.addProgressPhoto(uri, caption) },
                                    onDeleteProgressPhoto = { id -> viewModel.deleteProgressPhoto(id) },
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable<FitTrackRoute.Calculators> {
                                CalculatorsScreen(
                                    profile = profile,
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable<FitTrackRoute.Challenges> {
                                ChallengesScreen(
                                    uid = user.uid,
                                    challenges = challenges,
                                    onUpdateChallenge = { ch, valNum, joined -> viewModel.updateChallengeProgress(ch, valNum, joined) },
                                    onBack = { navController.popBackStack() },
                                    unitSystem = profile?.unitSystem ?: "metric"
                                )
                            }

                            composable<FitTrackRoute.Settings> {
                                SettingsScreen(
                                    profile = profile,
                                    onChangeTheme = { viewModel.updateThemePreference(it) },
                                    onChangeUnits = { viewModel.updateUnitSystem(it) },
                                    onToggleReducedMotion = { viewModel.updateReducedMotion(it) },
                                    onUpdateConsent = { sensor, loc ->
                                        profile?.let { p ->
                                            viewModel.updateProfileGoals(
                                                p.displayName,
                                                p.dailyStepGoal,
                                                p.dailyWaterTargetMl,
                                                p.dailyCalorieTarget,
                                                p.goal,
                                                sensor,
                                                loc
                                            )
                                        }
                                    },
                                    onExportJson = { viewModel.exportUserDataJson() },
                                    onSignOut = onSignOut,
                                    onDeleteAccountData = {
                                        viewModel.deleteAccountData {
                                            AuthManager.deleteCurrentAuthAccount(
                                                context = context,
                                                credentialManager = credentialManager,
                                                scope = scope,
                                                onDeleted = {},
                                                onError = { msg ->
                                                    scope.launch {
                                                        snackbarHostState.showSnackbar("User data deleted. $msg")
                                                    }
                                                }
                                            )
                                        }
                                    },
                                    onBack = { navController.popBackStack() },
                                    updateUiState = updateUiState,
                                    onOpenUpdateCenter = { navController.navigate(FitTrackRoute.UpdateCenter) },
                                    onOpenWhatsNew = { navController.navigate(FitTrackRoute.WhatsNew()) }
                                )
                            }

                            composable<FitTrackRoute.UpdateCenter> {
                                UpdateCenterScreen(
                                    uiState = updateUiState,
                                    onCheckForUpdates = { updateViewModel.checkForUpdates(force = true) },
                                    onStartPlayUpdate = { forceImmediate ->
                                        updateViewModel.startPlayUpdate(playUpdateLauncher, forceImmediate)
                                    },
                                    onCompletePlayUpdate = { updateViewModel.completePlayFlexibleUpdate() },
                                    onDownloadApkUpdate = { release ->
                                        updateViewModel.startApkDownloadAndVerification(release)
                                    },
                                    onOpenUnknownSourcesSettings = {
                                        updateViewModel.createUnknownSourcesSettingsIntent()?.let { intent ->
                                            runCatching { unknownSourcesLauncher.launch(intent) }
                                        }
                                    },
                                    onLaunchApkInstaller = { path, versionName ->
                                        updateViewModel.launchVerifiedApkInstaller(path, versionName)
                                    },
                                    onOpenWhatsNew = { navController.navigate(FitTrackRoute.WhatsNew()) },
                                    onRequestNotificationPermission = {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                            ContextCompat.checkSelfPermission(
                                                context,
                                                Manifest.permission.POST_NOTIFICATIONS
                                            ) != PackageManager.PERMISSION_GRANTED
                                        ) {
                                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        } else {
                                            val settingsIntent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                            }
                                            runCatching { context.startActivity(settingsIntent) }
                                        }
                                    },
                                    onUpdateNotificationPrefs = { prefs ->
                                        updateViewModel.updateNotificationPreferences(prefs)
                                    },
                                    onBack = { navController.popBackStack() },
                                    onOpenPlayStoreUrl = { url ->
                                        runCatching {
                                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                        }
                                    }
                                )
                            }

                            composable<FitTrackRoute.WhatsNew> { entry ->
                                val route = entry.toRoute<FitTrackRoute.WhatsNew>()
                                WhatsNewScreen(
                                    uiState = updateUiState,
                                    highlightVersionCode = route.versionCode,
                                    onMarkReleaseRead = { code -> updateViewModel.markReleaseAsRead(code) },
                                    onMarkAllRead = { updateViewModel.markAllReleasesAsRead() },
                                    onOpenUpdateCenter = {
                                        navController.navigate(FitTrackRoute.UpdateCenter) {
                                            launchSingleTop = true
                                        }
                                    },
                                    onCheckForUpdates = { updateViewModel.checkForUpdates(force = true) },
                                    onBack = { navController.popBackStack() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
