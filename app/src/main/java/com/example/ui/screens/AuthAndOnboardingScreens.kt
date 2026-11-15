package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.data.firebase.AuthManager
import com.example.domain.FitnessCalculator
import com.example.ui.branding.FitTrackGeometricIllustration
import com.example.ui.branding.FitTrackHorizontalLogo
import com.example.ui.branding.FitTrackIllustrationType
import com.example.ui.branding.FitTrackLogoVariant
import com.example.ui.branding.FitTrackSplashLogo
import com.example.ui.components.AuroraAmbientBackground
import com.example.ui.components.DisclaimerBanner
import com.example.ui.components.ErrorBannerCard
import com.example.ui.components.FitTrackCard
import com.example.ui.components.GlassCard
import com.example.ui.components.GradientButton
import com.example.ui.components.SecondaryButton
import com.example.ui.components.StatusBadge
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.LocalFitTrackColors

@Composable
fun AuroraSplashScreen(
    statusText: String = "Initializing FitTrack..."
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            FitTrackSplashLogo()
            Spacer(modifier = Modifier.height(28.dp))
            LinearProgressIndicator(
                modifier = Modifier
                    .width(160.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = Color(0xFF06B6D4),
                trackColor = Color(0xFF172033)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = statusText,
                style = MaterialTheme.typography.labelLarge,
                color = Color(0xFF94A3B8)
            )
        }
    }
}

@Composable
fun WelcomeAuthScreen(
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AuroraAmbientBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .widthIn(max = 560.dp)
                .align(Alignment.Center)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FitTrackHorizontalLogo(
                    markSize = 42.dp,
                    showSlogan = true,
                    variant = FitTrackLogoVariant.DARK_BG
                )
                StatusBadge(text = "ANDROID")
            }

            Spacer(modifier = Modifier.height(18.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(MaterialTheme.shapes.large)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.fittrack_welcome_hero_1794739022428),
                    contentDescription = "FitTrack Aurora Motion Hero",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Transparent,
                                    MaterialTheme.colorScheme.background.copy(alpha = 0.90f)
                                )
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(18.dp)
                ) {
                    Text(
                        text = "Move Smarter.\nBecome Stronger.",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                FeatureHighlightRow(
                    icon = Icons.AutoMirrored.Filled.DirectionsRun,
                    title = "Hardware Steps & GPS Telemetry",
                    subtitle = "Battery-efficient step sensors paired with live GPS routes."
                )
                Spacer(modifier = Modifier.height(12.dp))
                FeatureHighlightRow(
                    icon = Icons.Filled.Restaurant,
                    title = "Macro Precision & AI Food Scan",
                    subtitle = "Scan meals with Gemini AI and verify macros before saving."
                )
                Spacer(modifier = Modifier.height(12.dp))
                FeatureHighlightRow(
                    icon = Icons.Filled.Shield,
                    title = "Private Cloud Sync",
                    subtitle = "User-isolated Firestore sync with full JSON export & deletion."
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            if (errorMessage != null) {
                ErrorBannerCard(
                    message = errorMessage!!,
                    onDismiss = { errorMessage = null }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            GradientButton(
                text = if (isLoading) "Signing in..." else stringResource(R.string.sign_in_google),
                loading = isLoading,
                icon = Icons.AutoMirrored.Filled.ArrowForward,
                onClick = {
                    isLoading = true
                    errorMessage = null
                    AuthManager.onGoogleSignInClicked(
                        context = context,
                        credentialManager = credentialManager,
                        scope = scope,
                        onAuthSuccess = {
                            isLoading = false
                            onAuthSuccess()
                        },
                        onAuthError = { msg ->
                            isLoading = false
                            errorMessage = msg
                        },
                        onAuthCancelled = {
                            isLoading = false
                        }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("google_sign_in_button")
            )

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Secured by Google Sign-In & Firebase Auth. Your routes, nutrition logs, and progress stay private to your account.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun FeatureHighlightRow(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingWizardScreen(
    initialName: String,
    onComplete: (
        displayName: String,
        ageYears: Long,
        sex: String,
        heightCm: Double,
        weightKg: Double,
        targetWeightKg: Double,
        activityLevel: String,
        goal: String,
        sensorConsent: Boolean,
        locationConsent: Boolean
    ) -> Unit
) {
    val ext = LocalFitTrackColors.current
    var step by remember { mutableIntStateOf(0) } // 0..9 (10 screens)
    var displayName by remember { mutableStateOf(initialName.ifBlank { "Athlete" }) }
    var ageText by remember { mutableStateOf("26") }
    var sex by remember { mutableStateOf("male") }
    var heightText by remember { mutableStateOf("175") }
    var weightText by remember { mutableStateOf("72") }
    var targetWeightText by remember { mutableStateOf("70") }
    var activityLevel by remember { mutableStateOf("moderate") }
    var goal by remember { mutableStateOf("general_fitness") }
    var sensorConsent by remember { mutableStateOf(true) }
    var locationConsent by remember { mutableStateOf(true) }
    var permissionsRequested by remember { mutableStateOf(false) }

    val permissionsToRequest = remember {
        buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(Manifest.permission.ACTIVITY_RECOGNITION)
            }
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
            add(Manifest.permission.CAMERA)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }.toTypedArray()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        permissionsRequested = true
    }

    val ageInt = ageText.toIntOrNull()?.coerceIn(13, 120) ?: 26
    val isMinor = FitnessCalculator.isMinor(ageInt)
    val hVal = (heightText.toDoubleOrNull() ?: 175.0).coerceIn(80.0, 260.0)
    val wVal = (weightText.toDoubleOrNull() ?: 72.0).coerceIn(25.0, 400.0)
    val twVal = (targetWeightText.toDoubleOrNull() ?: wVal).coerceIn(25.0, 400.0)
    val safeGoal = if (isMinor && goal == "weight_loss") "general_fitness" else goal

    AuroraAmbientBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .widthIn(max = 580.dp)
                .align(Alignment.Center)
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Progress Top Header
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Step ${step + 1} of 10",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    if (step < 5) {
                        TextButton(onClick = { step = 5 }) {
                            Text("Skip Intro")
                        }
                    }
                }
                LinearProgressIndicator(
                    progress = { (step + 1) / 10f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            // Animated 10-Screen Content
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 16.dp)
            ) {
                AnimatedContent(
                    targetState = step,
                    transitionSpec = {
                        (fadeIn() + slideInHorizontally { it / 8 })
                            .togetherWith(fadeOut() + slideOutHorizontally { -it / 8 })
                    },
                    label = "onboarding_step"
                ) { currentStep ->
                    when (currentStep) {
                        0 -> OnboardingStorySlide(
                            illustration = FitTrackIllustrationType.ACHIEVEMENTS,
                            badge = "WELCOME",
                            title = "Welcome to FitTrack",
                            description = "Move Smarter. Become Stronger. Your unified Android companion for movement, nutrition, strength training, and AI coaching."
                        )
                        1 -> OnboardingStorySlide(
                            illustration = FitTrackIllustrationType.WALKING,
                            badge = "MOVEMENT",
                            title = "Track your daily movement",
                            description = "Hardware step sensors count every step efficiently, while live GPS sessions map your walks, runs, and rides with pace telemetry."
                        )
                        2 -> OnboardingStorySlide(
                            illustration = FitTrackIllustrationType.NUTRITION,
                            badge = "NUTRITION",
                            title = "Understand your nutrition",
                            description = "Log whole foods, monitor protein, carbs, fat, and hydration, or snap a meal photo for instant AI nutrition estimates."
                        )
                        3 -> OnboardingStorySlide(
                            illustration = FitTrackIllustrationType.STRENGTH,
                            badge = "STRENGTH",
                            title = "Build stronger workout habits",
                            description = "Train with guided exercises, custom routines, automatic rest timers, and personal volume records."
                        )
                        4 -> OnboardingStorySlide(
                            illustration = FitTrackIllustrationType.AI_COACH,
                            badge = "INTELLIGENCE",
                            title = "Meet your AI Fitness Coach",
                            description = "Get context-aware training guidance, meal planning, and workout builders powered by your real daily progress."
                        )
                        5 -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Choose your fitness goal", style = MaterialTheme.typography.headlineLarge)
                            Text(
                                "Your goal shapes daily calorie targets, macro splits, and coaching focus.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val goals = listOf(
                                Triple("weight_loss", "Lose weight", "Sustainable calorie deficit with muscle-preserving protein."),
                                Triple("muscle_gain", "Gain muscle", "Controlled surplus with progressive strength overload."),
                                Triple("general_fitness", "Stay fit", "Balanced energy, daily movement, and cardiovascular health."),
                                Triple("weight_maintenance", "Maintain weight", "Steady energy equilibrium and consistent habits.")
                            )
                            goals.forEach { (key, title, desc) ->
                                val disabled = isMinor && key == "weight_loss"
                                FitTrackCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = if (!disabled) { { goal = key } } else null,
                                    accentBorder = if (goal == key) MaterialTheme.colorScheme.primary else null,
                                    elevated = goal == key
                                ) {
                                    Text(
                                        text = if (disabled) "$title (Disabled under 18)" else title,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = if (disabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(desc, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        6 -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Personal information", style = MaterialTheme.typography.headlineLarge)
                            Text(
                                "Used only for accurate Mifflin-St Jeor BMR, stride distance, and hydration calculations.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            FitTrackCard(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = displayName,
                                    onValueChange = { displayName = it },
                                    label = { Text("Display Name") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("onboarding_name_input")
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    OutlinedTextField(
                                        value = ageText,
                                        onValueChange = { ageText = it.filter(Char::isDigit) },
                                        label = { Text("Age (yrs)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = heightText,
                                        onValueChange = { heightText = it },
                                        label = { Text("Height (cm)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    OutlinedTextField(
                                        value = weightText,
                                        onValueChange = { weightText = it },
                                        label = { Text("Weight (kg)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = targetWeightText,
                                        onValueChange = { targetWeightText = it },
                                        label = { Text("Target (kg)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("Biological Sex (BMR formula)", style = MaterialTheme.typography.labelLarge)
                                Spacer(modifier = Modifier.height(6.dp))
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf("male" to "Male", "female" to "Female", "other" to "Other").forEach { (key, label) ->
                                        FilterChip(selected = sex == key, onClick = { sex = key }, label = { Text(label) })
                                    }
                                }
                            }
                            if (isMinor) {
                                DisclaimerBanner(text = stringResource(R.string.minor_safeguard_notice), isWarning = true)
                            }
                        }
                        7 -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Activity level", style = MaterialTheme.typography.headlineLarge)
                            Text(
                                "Helps calculate your Total Daily Energy Expenditure (TDEE).",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val levels = listOf(
                                Triple("sedentary", "Sedentary (1.2x)", "Desk job, minimal planned exercise."),
                                Triple("light", "Lightly Active (1.375x)", "Light walks or training 1–3 days/week."),
                                Triple("moderate", "Moderately Active (1.55x)", "Moderate training 3–5 days/week."),
                                Triple("active", "Very Active (1.725x)", "Hard exercise 6–7 days/week."),
                                Triple("very_active", "Athlete (1.9x)", "High-volume training or physical work.")
                            )
                            levels.forEach { (key, title, desc) ->
                                FitTrackCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = { activityLevel = key },
                                    accentBorder = if (activityLevel == key) MaterialTheme.colorScheme.secondary else null,
                                    elevated = activityLevel == key
                                ) {
                                    Text(title, style = MaterialTheme.typography.titleMedium)
                                    Text(desc, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        8 -> Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Text("Permission explanation", style = MaterialTheme.typography.headlineLarge)
                            Text(
                                "FitTrack only asks for permissions tied directly to features you turn on.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            FitTrackCard(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Activity Recognition", style = MaterialTheme.typography.titleMedium)
                                        Text("Enables low-power hardware step counting.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Switch(checked = sensorConsent, onCheckedChange = { sensorConsent = it })
                                }
                                Spacer(modifier = Modifier.height(14.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Location & Camera", style = MaterialTheme.typography.titleMedium)
                                        Text("Location maps active GPS workouts; Camera powers food scanning.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Switch(checked = locationConsent, onCheckedChange = { locationConsent = it })
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                SecondaryButton(
                                    text = if (permissionsRequested) "Permissions Prompted ✓" else "Grant Android Permissions",
                                    icon = Icons.Filled.LocationOn,
                                    onClick = { permissionLauncher.launch(permissionsToRequest) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("grant_permissions_button")
                                )
                            }
                        }
                        else -> {
                            val (calTarget, note) = FitnessCalculator.calculateDailyCalorieTarget(
                                wVal, hVal, ageInt, sex, activityLevel, safeGoal
                            )
                            val macros = FitnessCalculator.calculateMacros(calTarget, wVal, safeGoal, ageInt)
                            val waterMl = FitnessCalculator.calculateWaterTargetMl(wVal)
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                FitTrackGeometricIllustration(
                                    type = FitTrackIllustrationType.ACHIEVEMENTS,
                                    size = 132.dp
                                )
                                Text("Ready to start, $displayName!", style = MaterialTheme.typography.headlineLarge, textAlign = TextAlign.Center)
                                Text(
                                    "Your personalized daily targets are calculated and ready to sync.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                                GlassCard(modifier = Modifier.fillMaxWidth()) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Daily Calories", style = MaterialTheme.typography.bodyLarge)
                                        Text("$calTarget kcal", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Macro Target", style = MaterialTheme.typography.bodyLarge)
                                        Text("P ${macros.proteinGrams}g • C ${macros.carbsGrams}g • F ${macros.fatGrams}g", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Hydration Goal", style = MaterialTheme.typography.bodyLarge)
                                        Text("$waterMl ml", style = MaterialTheme.typography.titleMedium, color = ext.success)
                                    }
                                }
                                if (!note.isNullOrBlank()) {
                                    DisclaimerBanner(text = note, isWarning = true)
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Navigation Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (step > 0) {
                    SecondaryButton(
                        text = "Back",
                        onClick = { step-- },
                        modifier = Modifier.weight(1f)
                    )
                }
                GradientButton(
                    text = if (step < 9) "Continue" else "Launch FitTrack",
                    icon = if (step == 9) Icons.Filled.CheckCircle else Icons.AutoMirrored.Filled.ArrowForward,
                    onClick = {
                        if (step < 9) {
                            step++
                        } else {
                            onComplete(
                                displayName,
                                ageInt.toLong(),
                                sex,
                                hVal,
                                wVal,
                                twVal,
                                activityLevel,
                                safeGoal,
                                sensorConsent,
                                locationConsent
                            )
                        }
                    },
                    modifier = Modifier
                        .weight(1.4f)
                        .testTag("onboarding_next_button")
                )
            }
        }
    }
}

@Composable
private fun OnboardingStorySlide(
    illustration: FitTrackIllustrationType,
    badge: String,
    title: String,
    description: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        StatusBadge(text = badge, tint = MaterialTheme.colorScheme.secondary)
        Spacer(modifier = Modifier.height(22.dp))
        FitTrackGeometricIllustration(type = illustration, size = 188.dp)
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
