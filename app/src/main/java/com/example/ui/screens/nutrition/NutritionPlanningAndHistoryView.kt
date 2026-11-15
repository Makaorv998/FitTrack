package com.example.ui.screens.nutrition

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Restaurant
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
import com.example.data.model.FoodItem
import com.example.data.model.MealLog
import com.example.data.model.UserProfile
import com.example.data.model.WaterLog
import com.example.platform.NutritionPreferences
import com.example.platform.PlannedMealEntry
import com.example.platform.RecipeIngredient
import com.example.platform.SavedRecipe
import com.example.platform.todayDateKey
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.FitTrackBottomSheet
import com.example.ui.components.FitTrackCard
import com.example.ui.components.GlassCard
import com.example.ui.components.GradientButton
import com.example.ui.components.SecondaryButton
import com.example.ui.components.SectionHeader
import com.example.ui.components.SimpleBarChart
import com.example.ui.components.StatCard
import com.example.ui.components.StatusBadge
import com.example.ui.theme.LocalFitTrackColors
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NutritionHistoryAndReportView(
    profile: UserProfile?,
    meals: List<MealLog>,
    waterLogs: List<WaterLog>,
    onOpenQuickAddForDate: (dateKey: String, mealType: String) -> Unit,
    onDeleteMeal: (String) -> Unit
) {
    val ext = LocalFitTrackColors.current
    val today = remember { todayDateKey() }
    val recentDates = remember(today) {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val labelFmt = SimpleDateFormat("EEE dd", Locale.US)
        (0..13).map { offset ->
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -offset)
            val dk = fmt.format(cal.time)
            val label = if (offset == 0) "Today" else if (offset == 1) "Yesterday" else labelFmt.format(cal.time)
            dk to label
        }
    }
    var selectedDateKey by remember { mutableStateOf(today) }

    val selectedDayMeals = remember(meals, selectedDateKey) {
        meals.filter { it.dateKey == selectedDateKey }
    }
    val selectedDayWater = remember(waterLogs, selectedDateKey) {
        waterLogs.filter { it.dateKey == selectedDateKey }.sumOf { it.amountMl }
    }

    val last7Keys = remember(today) {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val shortFmt = SimpleDateFormat("EEE", Locale.US)
        (6 downTo 0).map { offset ->
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -offset)
            fmt.format(cal.time) to shortFmt.format(cal.time)
        }
    }
    val calorieGoal = (profile?.dailyCalorieTarget ?: 2200L).coerceAtLeast(1200L)
    val dailyTotals7 = remember(meals, last7Keys) {
        last7Keys.map { (dk, label) ->
            val dayMeals = meals.filter { it.dateKey == dk }
            Triple(label, dayMeals.sumOf { it.calories }.toFloat(), dayMeals)
        }
    }
    val loggedDaysCount = remember(dailyTotals7) { dailyTotals7.count { it.third.isNotEmpty() }.coerceAtLeast(1) }
    val avgCalories7 = remember(dailyTotals7, loggedDaysCount) {
        (dailyTotals7.sumOf { it.second.toDouble() } / loggedDaysCount).toInt()
    }
    val avgProtein7 = remember(dailyTotals7, loggedDaysCount) {
        (dailyTotals7.sumOf { t -> t.third.sumOf { it.proteinG } } / loggedDaysCount).toInt()
    }
    val avgCarbs7 = remember(dailyTotals7, loggedDaysCount) {
        (dailyTotals7.sumOf { t -> t.third.sumOf { it.carbsG } } / loggedDaysCount).toInt()
    }
    val avgFat7 = remember(dailyTotals7, loggedDaysCount) {
        (dailyTotals7.sumOf { t -> t.third.sumOf { it.fatG } } / loggedDaysCount).toInt()
    }
    val weeklyWaterTotal = remember(waterLogs, last7Keys) {
        val keys = last7Keys.map { it.first }.toSet()
        waterLogs.filter { it.dateKey in keys }.sumOf { it.amountMl }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 7-Day Weekly Nutrition Report
        item {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("weekly_nutrition_report_card"),
                accentBorder = MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f)
            ) {
                SectionHeader(title = "7-Day Nutrition Report")
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard(
                        title = "Avg Calories",
                        value = "$avgCalories7 kcal",
                        subtitle = "Goal $calorieGoal kcal",
                        icon = Icons.Filled.Restaurant,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Avg Protein",
                        value = "${avgProtein7}g",
                        subtitle = "C ${avgCarbs7}g • F ${avgFat7}g",
                        icon = Icons.Filled.CalendarMonth,
                        tint = ext.success,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Weekly hydration logged: ${weeklyWaterTotal} ml",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(10.dp))
                SimpleBarChart(
                    values = dailyTotals7.map { it.first to it.second },
                    barColor = MaterialTheme.colorScheme.secondary
                )
            }
        }

        // Day-by-Day Meal History Picker
        item {
            SectionHeader(
                title = "Daily History",
                actionLabel = "+ Log on Day",
                onAction = { onOpenQuickAddForDate(selectedDateKey, "lunch") }
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                recentDates.forEach { (dk, label) ->
                    FilterChip(
                        selected = selectedDateKey == dk,
                        onClick = { selectedDateKey = dk },
                        label = { Text(label) }
                    )
                }
            }
        }

        item {
            val dayCals = selectedDayMeals.sumOf { it.calories }.toInt()
            val dayP = selectedDayMeals.sumOf { it.proteinG }.toInt()
            val dayC = selectedDayMeals.sumOf { it.carbsG }.toInt()
            val dayF = selectedDayMeals.sumOf { it.fatG }.toInt()
            FitTrackCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(selectedDateKey, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            "$dayCals kcal • P ${dayP}g / C ${dayC}g / F ${dayF}g • Water ${selectedDayWater} ml",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    StatusBadge(text = "${selectedDayMeals.size} items", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }

        if (selectedDayMeals.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Filled.Restaurant,
                    title = "No meals logged on $selectedDateKey",
                    subtitle = "Tap below to log food to this date.",
                    actionLabel = "Log Meal for $selectedDateKey",
                    onAction = { onOpenQuickAddForDate(selectedDateKey, "lunch") }
                )
            }
        } else {
            items(selectedDayMeals, key = { it.id }) { meal ->
                FitTrackCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                StatusBadge(text = meal.mealType.replaceFirstChar { it.uppercase() }, tint = MaterialTheme.colorScheme.secondary)
                                Text(meal.foodName, style = MaterialTheme.typography.titleMedium)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "${meal.grams.toInt()}g • ${meal.calories.toInt()} kcal • P ${meal.proteinG.toInt()}g / C ${meal.carbsG.toInt()}g / F ${meal.fatG.toInt()}g",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { onDeleteMeal(meal.id) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete meal", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NutritionRecipesAndPlanView(
    allFoods: List<FoodItem>,
    savedRecipes: List<SavedRecipe>,
    plannedMeals: List<PlannedMealEntry>,
    nutritionPreferences: NutritionPreferences,
    onSaveNutritionPreferences: (NutritionPreferences) -> Unit,
    onSaveRecipe: (SavedRecipe) -> Unit,
    onDeleteRecipe: (String) -> Unit,
    onLogRecipe: (SavedRecipe, String, Double) -> Unit,
    onSavePlannedMeal: (PlannedMealEntry) -> Unit,
    onDeletePlannedMeal: (String) -> Unit,
    onLogPlannedMealToday: (PlannedMealEntry) -> Unit
) {
    val ext = LocalFitTrackColors.current
    var showRecipeBuilder by remember { mutableStateOf(false) }
    var showAddPlanDialog by remember { mutableStateOf(false) }
    var showPrefDialog by remember { mutableStateOf(false) }
    var recipeToLog by remember { mutableStateOf<SavedRecipe?>(null) }
    var selectedPlanDay by remember { mutableStateOf("Monday") }

    val daysOfWeek = remember {
        listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
    }
    val dayPlannedMeals = remember(plannedMeals, selectedPlanDay) {
        plannedMeals.filter { it.dayOfWeek.equals(selectedPlanDay, ignoreCase = true) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Nutrition Preferences Summary Card
        item {
            FitTrackCard(modifier = Modifier.fillMaxWidth()) {
                SectionHeader(
                    title = "Dietary Preferences",
                    actionLabel = "Edit",
                    onAction = { showPrefDialog = true }
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    StatusBadge(text = nutritionPreferences.dietaryStyle, tint = MaterialTheme.colorScheme.primary)
                    StatusBadge(text = "${nutritionPreferences.mealsPerDay} MEALS/DAY", tint = MaterialTheme.colorScheme.secondary)
                    if (nutritionPreferences.allergies.isNotBlank()) {
                        StatusBadge(text = "Avoid: ${nutritionPreferences.allergies}", tint = ext.warning)
                    }
                }
            }
        }

        // Saved Recipes Section
        item {
            SectionHeader(
                title = "My Recipes (${savedRecipes.size})",
                actionLabel = "+ Build Recipe",
                onAction = { showRecipeBuilder = true }
            )
        }
        if (savedRecipes.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.AutoMirrored.Filled.MenuBook,
                    title = "No saved recipes yet",
                    subtitle = "Combine multiple ingredients into a recipe and automatically calculate calories and macros per serving.",
                    actionLabel = "Create Recipe",
                    onAction = { showRecipeBuilder = true }
                )
            }
        } else {
            items(savedRecipes, key = { it.id }) { recipe ->
                FitTrackCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(recipe.name, style = MaterialTheme.typography.titleLarge)
                            Text(
                                "${recipe.servings} servings • ${recipe.ingredients.size} ingredients",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Per serving: ${recipe.caloriesPerServing.toInt()} kcal • P ${recipe.proteinPerServingG.toInt()}g / C ${recipe.carbsPerServingG.toInt()}g / F ${recipe.fatPerServingG.toInt()}g",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        IconButton(onClick = { onDeleteRecipe(recipe.id) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete recipe", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { recipeToLog = recipe },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Log Recipe Serving")
                    }
                }
            }
        }

        // Weekly Meal Planning Section
        item {
            SectionHeader(
                title = "Weekly Meal Plan (${plannedMeals.size})",
                actionLabel = "+ Plan Meal",
                onAction = { showAddPlanDialog = true }
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                daysOfWeek.forEach { day ->
                    FilterChip(
                        selected = selectedPlanDay == day,
                        onClick = { selectedPlanDay = day },
                        label = { Text(day.take(3)) }
                    )
                }
            }
        }

        if (dayPlannedMeals.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Filled.CalendarMonth,
                    title = "No planned meals for $selectedPlanDay",
                    subtitle = "Add planned meals for your week and log them to Today in one tap.",
                    actionLabel = "Plan Meal for $selectedPlanDay",
                    onAction = { showAddPlanDialog = true }
                )
            }
        } else {
            items(dayPlannedMeals, key = { it.id }) { planned ->
                FitTrackCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                StatusBadge(text = planned.mealType.uppercase(), tint = MaterialTheme.colorScheme.primary)
                                Text(planned.title, style = MaterialTheme.typography.titleMedium)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "${planned.grams.toInt()}g • ${planned.calories.toInt()} kcal • P ${planned.proteinG.toInt()}g / C ${planned.carbsG.toInt()}g / F ${planned.fatG.toInt()}g",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { onDeletePlannedMeal(planned.id) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Remove planned meal", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { onLogPlannedMealToday(planned) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Log to Today's ${planned.mealType.replaceFirstChar { it.uppercase() }}")
                    }
                }
            }
        }
    }

    if (showRecipeBuilder) {
        RecipeBuilderSheet(
            allFoods = allFoods,
            onDismiss = { showRecipeBuilder = false },
            onSave = { recipe ->
                onSaveRecipe(recipe)
                showRecipeBuilder = false
            }
        )
    }

    recipeToLog?.let { recipe ->
        LogRecipeDialog(
            recipe = recipe,
            onDismiss = { recipeToLog = null },
            onConfirm = { mealType, servings ->
                onLogRecipe(recipe, mealType, servings)
                recipeToLog = null
            }
        )
    }

    if (showAddPlanDialog) {
        AddPlannedMealDialog(
            initialDay = selectedPlanDay,
            daysOfWeek = daysOfWeek,
            onDismiss = { showAddPlanDialog = false },
            onSave = { entry ->
                onSavePlannedMeal(entry)
                showAddPlanDialog = false
            }
        )
    }

    if (showPrefDialog) {
        EditNutritionPreferencesDialog(
            initial = nutritionPreferences,
            onDismiss = { showPrefDialog = false },
            onSave = { updated ->
                onSaveNutritionPreferences(updated)
                showPrefDialog = false
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecipeBuilderSheet(
    allFoods: List<FoodItem>,
    onDismiss: () -> Unit,
    onSave: (SavedRecipe) -> Unit
) {
    val ext = LocalFitTrackColors.current
    var name by remember { mutableStateOf("") }
    var servingsText by remember { mutableStateOf("2") }
    var search by remember { mutableStateOf("") }
    var ingredientGramsText by remember { mutableStateOf("100") }
    val ingredients = remember { mutableStateListOf<RecipeIngredient>() }

    val matching = remember(allFoods, search) {
        allFoods.filter {
            search.isBlank() || it.name.contains(search, ignoreCase = true)
        }.take(8)
    }

    val servings = (servingsText.toIntOrNull() ?: 1).coerceIn(1, 30)
    val totalCals = ingredients.sumOf { it.calories }
    val totalP = ingredients.sumOf { it.proteinG }
    val totalC = ingredients.sumOf { it.carbsG }
    val totalF = ingredients.sumOf { it.fatG }

    FitTrackBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Recipe Builder", style = MaterialTheme.typography.headlineSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Recipe Name") },
                    singleLine = true,
                    modifier = Modifier.weight(1.6f)
                )
                OutlinedTextField(
                    value = servingsText,
                    onValueChange = { servingsText = it.filter(Char::isDigit) },
                    label = { Text("Servings") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(0.8f)
                )
            }

            if (ingredients.isNotEmpty()) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = ext.surfaceElevated,
                    border = BorderStroke(1.dp, ext.border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "Per Serving (${servings} srv): ${(totalCals / servings).toInt()} kcal • P ${(totalP / servings).toInt()}g • C ${(totalC / servings).toInt()}g • F ${(totalF / servings).toInt()}g",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        ingredients.forEachIndexed { idx, ing ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "${ing.foodName} (${ing.grams.toInt()}g • ${ing.calories.toInt()} kcal)",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = { ingredients.removeAt(idx) }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Remove ingredient", modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    placeholder = { Text("Search ingredients...") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        if (search.isNotBlank()) {
                            IconButton(onClick = { search = "" }) {
                                Icon(Icons.Filled.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.weight(1.6f)
                )
                OutlinedTextField(
                    value = ingredientGramsText,
                    onValueChange = { ingredientGramsText = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Grams") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(0.8f)
                )
            }

            matching.forEach { food ->
                Surface(
                    onClick = {
                        val g = (ingredientGramsText.toDoubleOrNull() ?: 100.0).coerceIn(1.0, 5000.0)
                        val factor = g / 100.0
                        ingredients.add(
                            RecipeIngredient(
                                foodName = food.name,
                                grams = g,
                                calories = food.caloriesPer100g * factor,
                                proteinG = food.proteinPer100g * factor,
                                carbsG = food.carbsPer100g * factor,
                                fatG = food.fatPer100g * factor,
                                fiberG = food.fiberPer100g * factor,
                                sugarG = food.sugarPer100g * factor
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
                            Text(food.name, style = MaterialTheme.typography.titleSmall)
                            Text("${food.caloriesPer100g.toInt()} kcal/100g", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                        }
                        Icon(Icons.Filled.Add, contentDescription = "Add ${food.name}", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            GradientButton(
                text = "Save Recipe",
                enabled = name.isNotBlank() && ingredients.isNotEmpty(),
                onClick = {
                    onSave(
                        SavedRecipe(
                            name = name.trim(),
                            servings = servings,
                            ingredients = ingredients.toList()
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LogRecipeDialog(
    recipe: SavedRecipe,
    onDismiss: () -> Unit,
    onConfirm: (String, Double) -> Unit
) {
    var mealType by remember { mutableStateOf("lunch") }
    var servingsText by remember { mutableStateOf("1") }
    val servingsCount = (servingsText.toDoubleOrNull() ?: 1.0).coerceIn(0.25, 10.0)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log ${recipe.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "${(recipe.caloriesPerServing * servingsCount).toInt()} kcal • P ${(recipe.proteinPerServingG * servingsCount).toInt()}g / C ${(recipe.carbsPerServingG * servingsCount).toInt()}g / F ${(recipe.fatPerServingG * servingsCount).toInt()}g",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
                OutlinedTextField(
                    value = servingsText,
                    onValueChange = { servingsText = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Servings") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("breakfast", "lunch", "dinner", "snack").forEach { type ->
                        FilterChip(
                            selected = mealType == type,
                            onClick = { mealType = type },
                            label = { Text(if (type == "snack") "Snacks" else type.replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(mealType, servingsCount) }) { Text("Log Serving") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddPlannedMealDialog(
    initialDay: String,
    daysOfWeek: List<String>,
    onDismiss: () -> Unit,
    onSave: (PlannedMealEntry) -> Unit
) {
    var day by remember { mutableStateOf(initialDay) }
    var mealType by remember { mutableStateOf("lunch") }
    var title by remember { mutableStateOf("") }
    var calsText by remember { mutableStateOf("500") }
    var pText by remember { mutableStateOf("35") }
    var cText by remember { mutableStateOf("50") }
    var fText by remember { mutableStateOf("15") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Plan Meal") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Meal Name") }, singleLine = true)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    daysOfWeek.forEach { d ->
                        FilterChip(selected = day == d, onClick = { day = d }, label = { Text(d.take(3)) })
                    }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("breakfast", "lunch", "dinner", "snack").forEach { m ->
                        FilterChip(
                            selected = mealType == m,
                            onClick = { mealType = m },
                            label = { Text(if (m == "snack") "Snacks" else m.replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(value = calsText, onValueChange = { calsText = it }, label = { Text("kcal") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = pText, onValueChange = { pText = it }, label = { Text("P (g)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), singleLine = true)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(value = cText, onValueChange = { cText = it }, label = { Text("C (g)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = fText, onValueChange = { fText = it }, label = { Text("F (g)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), singleLine = true)
                }
            }
        },
        confirmButton = {
            Button(
                enabled = title.isNotBlank(),
                onClick = {
                    onSave(
                        PlannedMealEntry(
                            dayOfWeek = day,
                            mealType = mealType,
                            title = title.trim(),
                            grams = 300.0,
                            calories = (calsText.toDoubleOrNull() ?: 500.0).coerceAtLeast(0.0),
                            proteinG = (pText.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0),
                            carbsG = (cText.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0),
                            fatG = (fText.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0)
                        )
                    )
                }
            ) { Text("Save to Plan") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditNutritionPreferencesDialog(
    initial: NutritionPreferences,
    onDismiss: () -> Unit,
    onSave: (NutritionPreferences) -> Unit
) {
    var style by remember { mutableStateOf(initial.dietaryStyle) }
    var allergies by remember { mutableStateOf(initial.allergies) }
    var mealsPerDay by remember { mutableIntStateOf(initial.mealsPerDay) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Dietary Preferences") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Balanced", "High Protein", "Vegetarian", "Mediterranean", "Low Carb").forEach { s ->
                        FilterChip(selected = style == s, onClick = { style = s }, label = { Text(s) })
                    }
                }
                OutlinedTextField(
                    value = allergies,
                    onValueChange = { allergies = it },
                    label = { Text("Allergies / Exclusions") },
                    singleLine = true
                )
                Text("Meals per day: $mealsPerDay", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(2, 3, 4, 5, 6).forEach { m ->
                        FilterChip(selected = mealsPerDay == m, onClick = { mealsPerDay = m }, label = { Text("$m") })
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(NutritionPreferences(style, allergies.trim(), mealsPerDay)) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
