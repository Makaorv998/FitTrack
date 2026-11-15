package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.ai.AiMealPlanResult
import com.example.data.ai.FoodScanResult
import com.example.data.catalog.FitnessCatalog
import com.example.data.model.FoodItem
import com.example.data.model.MealLog
import com.example.data.model.UserProfile
import com.example.data.model.WaterLog
import com.example.domain.FitnessCalculator
import com.example.platform.todayDateKey
import com.example.ui.AsyncState
import com.example.ui.branding.FitTrackGeometricIllustration
import com.example.ui.branding.FitTrackIllustrationType
import com.example.ui.components.ActivityCard
import com.example.ui.components.AnimatedCounterText
import com.example.ui.components.AnimatedProgressRing
import com.example.ui.components.AuroraAmbientBackground
import com.example.ui.components.AuroraProgressBar
import com.example.ui.components.DisclaimerBanner
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ErrorBannerCard
import com.example.ui.components.FitTrackCard
import com.example.ui.components.GlassCard
import com.example.ui.components.GradientButton
import com.example.ui.components.ScreenHeader
import com.example.ui.components.SecondaryButton
import com.example.ui.components.SectionHeader
import com.example.ui.components.SkeletonFoodList
import com.example.ui.components.StatusBadge
import com.example.ui.theme.LocalFitTrackColors
import com.example.ui.theme.MetricNumberTextStyle

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NutritionScreen(
    profile: UserProfile?,
    meals: List<MealLog>,
    waterLogs: List<WaterLog>,
    customFoods: List<FoodItem>,
    onLogFood: (FoodItem, Double, String) -> Unit,
    onUpdateMealGrams: (MealLog, Double) -> Unit,
    onDeleteMeal: (String) -> Unit,
    onSaveCustomFood: (FoodItem) -> Unit,
    onToggleFavorite: (FoodItem) -> Unit,
    onAddWater: (Long) -> Unit,
    onOpenAiScanner: () -> Unit,
    onOpenAiMealPlanner: () -> Unit
) {
    val ext = LocalFitTrackColors.current
    val today = remember { todayDateKey() }
    val todayMeals = remember(meals, today) { meals.filter { it.dateKey == today } }
    val todayWater = remember(waterLogs, today) { waterLogs.filter { it.dateKey == today }.sumOf { it.amountMl } }
    val waterGoal = (profile?.dailyWaterTargetMl ?: 2500L).coerceAtLeast(500L)
    val calorieGoal = (profile?.dailyCalorieTarget ?: 2200L).coerceAtLeast(1200L)

    val totalCalories = todayMeals.sumOf { it.calories }
    val totalProtein = todayMeals.sumOf { it.proteinG }
    val totalCarbs = todayMeals.sumOf { it.carbsG }
    val totalFat = todayMeals.sumOf { it.fatG }
    val totalFiber = todayMeals.sumOf { it.fiberG }
    val totalSugar = todayMeals.sumOf { it.sugarG }
    val remainingCal = (calorieGoal - totalCalories.toLong())

    val macroTargets = remember(profile, calorieGoal) {
        FitnessCalculator.calculateMacros(
            calorieTarget = calorieGoal.toInt(),
            weightKg = profile?.weightKg ?: 70.0,
            goal = profile?.goal ?: "general_fitness",
            ageYears = (profile?.ageYears ?: 26L).toInt()
        )
    }

    var searchQuery by remember { mutableStateOf("") }
    var favoritesOnly by remember { mutableStateOf(false) }
    var selectedMealFilter by remember { mutableStateOf("all") }
    var selectedFoodForLog by remember { mutableStateOf<FoodItem?>(null) }
    var editingMeal by remember { mutableStateOf<MealLog?>(null) }
    var showCreateFoodDialog by remember { mutableStateOf(false) }

    val allFoods = remember(customFoods) {
        val customNames = customFoods.map { it.name.lowercase() }.toSet()
        customFoods + FitnessCatalog.builtInFoods.filter { it.name.lowercase() !in customNames }
    }

    val filteredFoods = remember(allFoods, searchQuery, favoritesOnly) {
        allFoods.filter { food ->
            val matchesQuery = searchQuery.isBlank() ||
                food.name.contains(searchQuery, ignoreCase = true) ||
                food.brand.contains(searchQuery, ignoreCase = true)
            val matchesFav = !favoritesOnly || food.isFavorite
            matchesQuery && matchesFav
        }
    }

    val displayedTodayMeals = remember(todayMeals, selectedMealFilter) {
        if (selectedMealFilter == "all") todayMeals
        else todayMeals.filter { it.mealType.equals(selectedMealFilter, ignoreCase = true) }
    }

    val bottomClearance = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 104.dp

    AuroraAmbientBackground {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = bottomClearance),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                ScreenHeader(
                    title = "Nutrition",
                    subtitle = "${todayMeals.size} meals logged today",
                    trailing = {
                        StatusBadge(
                            text = if (remainingCal >= 0) "$remainingCal left" else "+${-remainingCal} over",
                            tint = if (remainingCal >= 0) ext.success else ext.warning
                        )
                    }
                )
            }

            // Calorie & Macro Hero Card
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    accentBorder = MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        AnimatedProgressRing(
                            progress = (totalCalories / calorieGoal.toDouble()).toFloat(),
                            size = 128.dp,
                            strokeWidth = 12.dp,
                            primaryColor = ext.success,
                            secondaryColor = MaterialTheme.colorScheme.secondary
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                AnimatedCounterText(
                                    targetValue = totalCalories.toInt(),
                                    style = MetricNumberTextStyle,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "/ $calorieGoal kcal",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AuroraProgressBar(
                                label = "Protein",
                                valueText = "${totalProtein.toInt()}/${macroTargets.proteinGrams}g",
                                progress = totalProtein.toFloat() / macroTargets.proteinGrams.coerceAtLeast(1),
                                color = MaterialTheme.colorScheme.primary
                            )
                            AuroraProgressBar(
                                label = "Carbs",
                                valueText = "${totalCarbs.toInt()}/${macroTargets.carbsGrams}g",
                                progress = totalCarbs.toFloat() / macroTargets.carbsGrams.coerceAtLeast(1),
                                color = MaterialTheme.colorScheme.secondary
                            )
                            AuroraProgressBar(
                                label = "Fat",
                                valueText = "${totalFat.toInt()}/${macroTargets.fatGrams}g",
                                progress = totalFat.toFloat() / macroTargets.fatGrams.coerceAtLeast(1),
                                color = ext.warning
                            )
                            Text(
                                text = "Fiber ${totalFiber.toInt()}g • Sugar ${totalSugar.toInt()}g",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        GradientButton(
                            text = "AI Scan",
                            icon = Icons.Filled.CameraAlt,
                            onClick = onOpenAiScanner,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("open_ai_food_scanner_button")
                        )
                        SecondaryButton(
                            text = "AI Meal Plan",
                            icon = Icons.Filled.AutoAwesome,
                            onClick = onOpenAiMealPlanner,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("open_ai_meal_planner_button")
                        )
                    }
                }
            }

            // Water Tracker Card
            item {
                ActivityCard(
                    modifier = Modifier.fillMaxWidth(),
                    accentBorder = MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        AnimatedProgressRing(
                            progress = todayWater.toFloat() / waterGoal.toFloat(),
                            size = 74.dp,
                            strokeWidth = 7.dp,
                            primaryColor = MaterialTheme.colorScheme.secondary,
                            secondaryColor = MaterialTheme.colorScheme.primary
                        ) {
                            Icon(
                                imageVector = Icons.Filled.WaterDrop,
                                contentDescription = "Water Intake",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text("Hydration Tracker", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "$todayWater / $waterGoal ml",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                SecondaryButton(
                                    text = "+250 ml",
                                    onClick = { onAddWater(250L) },
                                    modifier = Modifier.weight(1f)
                                )
                                SecondaryButton(
                                    text = "+500 ml",
                                    onClick = { onAddWater(500L) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            // Today's Meals Section with Meal Type Filter
            item {
                SectionHeader(title = "Today's Meals (${todayMeals.size})")
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "all" to "All",
                        "breakfast" to "Breakfast",
                        "lunch" to "Lunch",
                        "dinner" to "Dinner",
                        "snack" to "Snacks"
                    ).forEach { (key, label) ->
                        FilterChip(
                            selected = selectedMealFilter == key,
                            onClick = { selectedMealFilter = key },
                            label = { Text(label) }
                        )
                    }
                }
            }

            if (displayedTodayMeals.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Filled.Restaurant,
                        title = if (todayMeals.isEmpty()) "No meals logged today" else "No entries in ${selectedMealFilter.replaceFirstChar { it.uppercase() }}",
                        subtitle = "Search the food catalog below or snap a meal photo with Gemini AI.",
                        illustration = FitTrackIllustrationType.NUTRITION
                    )
                }
            } else {
                items(displayedTodayMeals, key = { it.id }) { meal ->
                    FitTrackCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    StatusBadge(
                                        text = meal.mealType.replaceFirstChar { it.uppercase() },
                                        tint = MaterialTheme.colorScheme.secondary
                                    )
                                    Text(
                                        text = meal.foodName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "${meal.grams.toInt()}g • ${meal.calories.toInt()} kcal • P ${meal.proteinG.toInt()}g / C ${meal.carbsG.toInt()}g / F ${meal.fatG.toInt()}g",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row {
                                IconButton(onClick = { editingMeal = meal }) {
                                    Icon(Icons.Filled.Edit, contentDescription = "Edit quantity")
                                }
                                IconButton(onClick = { onDeleteMeal(meal.id) }) {
                                    Icon(
                                        Icons.Filled.Delete,
                                        contentDescription = "Delete meal",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Food Search & Catalog
            item {
                SectionHeader(
                    title = "Food Database",
                    actionLabel = "+ Custom Food",
                    onAction = { showCreateFoodDialog = true }
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search chicken, oats, salmon, banana...") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Filled.Close, contentDescription = "Clear search")
                            }
                        }
                    },
                    shape = MaterialTheme.shapes.medium,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("food_search_input")
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !favoritesOnly,
                        onClick = { favoritesOnly = false },
                        label = { Text("All (${allFoods.size})") }
                    )
                    FilterChip(
                        selected = favoritesOnly,
                        onClick = { favoritesOnly = true },
                        label = { Text("Favorites") }
                    )
                }
            }

            if (filteredFoods.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Filled.Search,
                        title = "No matching foods",
                        subtitle = "Try another keyword or create a custom nutrition entry.",
                        actionLabel = "Add Custom Food",
                        onAction = { showCreateFoodDialog = true }
                    )
                }
            } else {
                items(filteredFoods.take(15), key = { it.id.ifBlank { it.name } }) { food ->
                    FitTrackCard(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { selectedFoodForLog = food }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(food.name, style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${food.caloriesPer100g.toInt()} kcal/100g • P ${food.proteinPer100g.toInt()}g • C ${food.carbsPer100g.toInt()}g • F ${food.fatPer100g.toInt()}g",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Serving: ${food.servingLabel}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                            Row {
                                IconButton(onClick = { onToggleFavorite(food) }) {
                                    Icon(
                                        imageVector = if (food.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                        contentDescription = "Favorite",
                                        tint = if (food.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = { selectedFoodForLog = food }) {
                                    Icon(
                                        Icons.Filled.Add,
                                        contentDescription = "Log food",
                                        tint = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    selectedFoodForLog?.let { food ->
        LogFoodDialog(
            food = food,
            onDismiss = { selectedFoodForLog = null },
            onConfirm = { grams, mealType ->
                onLogFood(food, grams, mealType)
                selectedFoodForLog = null
            }
        )
    }

    editingMeal?.let { meal ->
        EditMealDialog(
            meal = meal,
            onDismiss = { editingMeal = null },
            onSave = { newGrams ->
                onUpdateMealGrams(meal, newGrams)
                editingMeal = null
            }
        )
    }

    if (showCreateFoodDialog) {
        CreateCustomFoodDialog(
            onDismiss = { showCreateFoodDialog = false },
            onSave = { food ->
                onSaveCustomFood(food)
                showCreateFoodDialog = false
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LogFoodDialog(
    food: FoodItem,
    onDismiss: () -> Unit,
    onConfirm: (Double, String) -> Unit
) {
    var gramsText by remember { mutableStateOf(food.servingSizeGrams.toInt().toString()) }
    var mealType by remember { mutableStateOf("lunch") }
    val grams = (gramsText.toDoubleOrNull() ?: food.servingSizeGrams).coerceIn(1.0, 5000.0)
    val factor = grams / 100.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(food.name) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Calories: ${(food.caloriesPer100g * factor).toInt()} kcal • P: ${(food.proteinPer100g * factor).toInt()}g • C: ${(food.carbsPer100g * factor).toInt()}g • F: ${(food.fatPer100g * factor).toInt()}g",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
                OutlinedTextField(
                    value = gramsText,
                    onValueChange = { gramsText = it },
                    label = { Text("Weight (grams)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("breakfast", "lunch", "dinner", "snack").forEach { type ->
                        FilterChip(
                            selected = mealType == type,
                            onClick = { mealType = type },
                            label = { Text(type.replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(grams, mealType) }) { Text("Log Food") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun EditMealDialog(
    meal: MealLog,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit
) {
    var gramsText by remember { mutableStateOf(meal.grams.toInt().toString()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit ${meal.foodName}") },
        text = {
            OutlinedTextField(
                value = gramsText,
                onValueChange = { gramsText = it },
                label = { Text("New weight (grams)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(onClick = {
                val g = (gramsText.toDoubleOrNull() ?: meal.grams).coerceIn(1.0, 5000.0)
                onSave(g)
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun CreateCustomFoodDialog(
    onDismiss: () -> Unit,
    onSave: (FoodItem) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var cals by remember { mutableStateOf("200") }
    var protein by remember { mutableStateOf("15") }
    var carbs by remember { mutableStateOf("20") }
    var fat by remember { mutableStateOf("6") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Custom Food (per 100g)") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Food name") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = cals, onValueChange = { cals = it }, label = { Text("kcal") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = protein, onValueChange = { protein = it }, label = { Text("Protein g") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f), singleLine = true)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = carbs, onValueChange = { carbs = it }, label = { Text("Carbs g") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = fat, onValueChange = { fat = it }, label = { Text("Fat g") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f), singleLine = true)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(
                            FoodItem(
                                name = name.trim(),
                                caloriesPer100g = (cals.toDoubleOrNull() ?: 200.0).coerceIn(0.0, 1500.0),
                                proteinPer100g = (protein.toDoubleOrNull() ?: 0.0).coerceIn(0.0, 100.0),
                                carbsPer100g = (carbs.toDoubleOrNull() ?: 0.0).coerceIn(0.0, 100.0),
                                fatPer100g = (fat.toDoubleOrNull() ?: 0.0).coerceIn(0.0, 100.0),
                                isCustom = true
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AiFoodScannerScreen(
    scanState: AsyncState<FoodScanResult>,
    onScanBitmap: (Bitmap) -> Unit,
    onConfirmLogMeal: (MealLog) -> Unit,
    onReset: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var selectedMealType by remember { mutableStateOf("lunch") }
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bmp ->
        if (bmp != null) {
            previewBitmap = bmp
            onScanBitmap(bmp)
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) cameraLauncher.launch(null)
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val bmp = BitmapFactory.decodeStream(stream)
                if (bmp != null) {
                    val scaled = scaleBitmapDown(bmp, 1024)
                    previewBitmap = scaled
                    onScanBitmap(scaled)
                }
            }
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.85f)),
                title = { Text("AI Food Scanner") },
                navigationIcon = {
                    IconButton(onClick = {
                        onReset()
                        onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        AuroraAmbientBackground {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                DisclaimerBanner(
                    text = stringResource(R.string.calorie_estimate_disclaimer),
                    isWarning = true
                )

                // Viewfinder / Image Preview Card
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    accentBorder = secondaryColor.copy(alpha = 0.45f)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(210.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .background(LocalFitTrackColors.current.surfaceElevated),
                        contentAlignment = Alignment.Center
                    ) {
                        if (previewBitmap != null) {
                            Image(
                                bitmap = previewBitmap!!.asImageBitmap(),
                                contentDescription = "Scanned food preview",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                FitTrackGeometricIllustration(
                                    type = FitTrackIllustrationType.NUTRITION,
                                    size = 96.dp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Frame your plate for AI estimation",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Scanner Reticle Corner Brackets Overlay
                        Canvas(modifier = Modifier.fillMaxSize().padding(14.dp)) {
                            val corner = 24.dp.toPx()
                            val stroke = 3.dp.toPx()
                            // Top-left
                            drawLine(secondaryColor, Offset(0f, 0f), Offset(corner, 0f), stroke, StrokeCap.Round)
                            drawLine(secondaryColor, Offset(0f, 0f), Offset(0f, corner), stroke, StrokeCap.Round)
                            // Top-right
                            drawLine(primaryColor, Offset(size.width, 0f), Offset(size.width - corner, 0f), stroke, StrokeCap.Round)
                            drawLine(primaryColor, Offset(size.width, 0f), Offset(size.width, corner), stroke, StrokeCap.Round)
                            // Bottom-left
                            drawLine(primaryColor, Offset(0f, size.height), Offset(corner, size.height), stroke, StrokeCap.Round)
                            drawLine(primaryColor, Offset(0f, size.height), Offset(0f, size.height - corner), stroke, StrokeCap.Round)
                            // Bottom-right
                            drawLine(secondaryColor, Offset(size.width, size.height), Offset(size.width - corner, size.height), stroke, StrokeCap.Round)
                            drawLine(secondaryColor, Offset(size.width, size.height), Offset(size.width, size.height - corner), stroke, StrokeCap.Round)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        GradientButton(
                            text = "Camera",
                            icon = Icons.Filled.CameraAlt,
                            onClick = {
                                val hasCam = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                                if (hasCam) cameraLauncher.launch(null) else cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("capture_food_camera_button")
                        )
                        SecondaryButton(
                            text = "Photo Picker",
                            icon = Icons.Filled.PhotoLibrary,
                            onClick = {
                                photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("pick_food_photo_button")
                        )
                    }
                }

                when (scanState) {
                    AsyncState.Idle -> {
                        EmptyStateCard(
                            icon = Icons.Filled.CameraAlt,
                            title = "Ready to analyze a meal",
                            subtitle = "Take a food photo or pick one from your gallery to estimate portions and macros before logging."
                        )
                    }

                    AsyncState.Loading, is AsyncState.Streaming -> {
                        FitTrackCard(modifier = Modifier.fillMaxWidth()) {
                            StatusBadge(text = "ANALYZING IMAGE", tint = MaterialTheme.colorScheme.secondary)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Identifying ingredients and portion estimates...", style = MaterialTheme.typography.titleMedium)
                        }
                        SkeletonFoodList()
                    }

                    is AsyncState.Error -> {
                        ErrorBannerCard(message = scanState.message)
                    }

                    is AsyncState.Success -> {
                        val result = scanState.data
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            StatusBadge(text = "VERIFY BEFORE SAVING", tint = LocalFitTrackColors.current.warning)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(result.mealSummary, style = MaterialTheme.typography.titleLarge)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                result.uncertaintyNotice,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("breakfast", "lunch", "dinner", "snack").forEach { type ->
                                    FilterChip(
                                        selected = selectedMealType == type,
                                        onClick = { selectedMealType = type },
                                        label = { Text(type.replaceFirstChar { it.uppercase() }) }
                                    )
                                }
                            }
                        }

                        result.items.forEach { item ->
                            FitTrackCard(modifier = Modifier.fillMaxWidth()) {
                                Text(item.name, style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "~${item.estimatedGrams.toInt()}g • ~${item.calories.toInt()} kcal (P ${item.proteinG.toInt()}g / C ${item.carbsG.toInt()}g / F ${item.fatG.toInt()}g)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Text(
                                    text = item.confidenceNote,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                GradientButton(
                                    text = "Confirm & Log Item",
                                    icon = Icons.Filled.Check,
                                    onClick = {
                                        onConfirmLogMeal(
                                            MealLog(
                                                mealType = selectedMealType,
                                                foodName = item.name,
                                                grams = item.estimatedGrams,
                                                calories = item.calories,
                                                proteinG = item.proteinG,
                                                carbsG = item.carbsG,
                                                fatG = item.fatG,
                                                fiberG = item.fiberG,
                                                sugarG = item.sugarG,
                                                source = "ai_scan"
                                            )
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiMealPlannerScreen(
    state: AsyncState<AiMealPlanResult>,
    onGenerate: (String, String) -> Unit,
    onLogSuggestion: (MealLog) -> Unit,
    onBack: () -> Unit
) {
    var preferences by remember { mutableStateOf("High protein, whole foods") }
    var allergies by remember { mutableStateOf("None") }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.85f)),
                title = { Text("AI Meal Planner") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        AuroraAmbientBackground {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                DisclaimerBanner(text = stringResource(R.string.ai_disclaimer))
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = preferences,
                        onValueChange = { preferences = it },
                        label = { Text("Dietary Preferences") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = allergies,
                        onValueChange = { allergies = it },
                        label = { Text("Allergies / Exclusions") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    GradientButton(
                        text = "Generate Meal Suggestions",
                        icon = Icons.Filled.AutoAwesome,
                        loading = state is AsyncState.Loading || state is AsyncState.Streaming,
                        onClick = { onGenerate(preferences, allergies) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                when (state) {
                    AsyncState.Idle -> {
                        EmptyStateCard(
                            icon = Icons.Filled.AutoAwesome,
                            title = "Personalized daily menu",
                            subtitle = "Enter your food preferences and exclusions to generate balanced meal ideas aligned with your targets.",
                            illustration = FitTrackIllustrationType.NUTRITION
                        )
                    }
                    AsyncState.Loading, is AsyncState.Streaming -> {
                        SkeletonFoodList()
                    }
                    is AsyncState.Error -> ErrorBannerCard(message = state.message)
                    is AsyncState.Success -> {
                        val plan = state.data
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            StatusBadge(text = "DAILY MENU", tint = MaterialTheme.colorScheme.secondary)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(plan.planTitle, style = MaterialTheme.typography.titleLarge)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(plan.dailyNotes, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        plan.meals.forEach { suggestion ->
                            FitTrackCard(modifier = Modifier.fillMaxWidth()) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    StatusBadge(text = suggestion.mealType.replaceFirstChar { it.uppercase() })
                                    Text(suggestion.title, style = MaterialTheme.typography.titleMedium)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(suggestion.description, style = MaterialTheme.typography.bodyMedium)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    "~${suggestion.calories.toInt()} kcal • P ${suggestion.proteinG.toInt()}g / C ${suggestion.carbsG.toInt()}g / F ${suggestion.fatG.toInt()}g",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                SecondaryButton(
                                    text = "Confirm & Log Meal",
                                    icon = Icons.Filled.Check,
                                    onClick = {
                                        onLogSuggestion(
                                            MealLog(
                                                mealType = suggestion.mealType,
                                                foodName = suggestion.title,
                                                grams = suggestion.grams,
                                                calories = suggestion.calories,
                                                proteinG = suggestion.proteinG,
                                                carbsG = suggestion.carbsG,
                                                fatG = suggestion.fatG,
                                                source = "ai_plan"
                                            )
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun scaleBitmapDown(bitmap: Bitmap, maxDimension: Int): Bitmap {
    val ratio = minOf(maxDimension.toFloat() / bitmap.width, maxDimension.toFloat() / bitmap.height)
    if (ratio >= 1f) return bitmap
    return Bitmap.createScaledBitmap(
        bitmap,
        (bitmap.width * ratio).toInt().coerceAtLeast(1),
        (bitmap.height * ratio).toInt().coerceAtLeast(1),
        true
    )
}
