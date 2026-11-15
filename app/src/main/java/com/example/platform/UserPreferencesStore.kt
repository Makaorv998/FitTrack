package com.example.platform

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.FoodItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.preferencesDataStore: DataStore<Preferences> by preferencesDataStore(name = "fittrack_preferences_store")

@Serializable
data class TrainingPreferences(
    val preferredSplit: String = "Push / Pull / Legs",
    val targetDaysPerWeek: Int = 4,
    val experienceLevel: String = "intermediate",
    val availableEquipment: String = "Full Gym",
    val activeProgramId: String = "",
    val activeProgramName: String = "",
    val defaultRestSeconds: Int = 90
)

@Serializable
data class NutritionPreferences(
    val dietaryStyle: String = "Balanced",
    val allergies: String = "",
    val mealsPerDay: Int = 4
)

@Serializable
data class RecipeIngredient(
    val foodName: String = "",
    val grams: Double = 100.0,
    val calories: Double = 0.0,
    val proteinG: Double = 0.0,
    val carbsG: Double = 0.0,
    val fatG: Double = 0.0,
    val fiberG: Double = 0.0,
    val sugarG: Double = 0.0
)

@Serializable
data class SavedRecipe(
    val id: String = "",
    val name: String = "",
    val servings: Int = 1,
    val notes: String = "",
    val ingredients: List<RecipeIngredient> = emptyList(),
    val createdAtMs: Long = System.currentTimeMillis()
) {
    val totalCalories: Double get() = ingredients.sumOf { it.calories }
    val totalProteinG: Double get() = ingredients.sumOf { it.proteinG }
    val totalCarbsG: Double get() = ingredients.sumOf { it.carbsG }
    val totalFatG: Double get() = ingredients.sumOf { it.fatG }
    val totalFiberG: Double get() = ingredients.sumOf { it.fiberG }
    val totalSugarG: Double get() = ingredients.sumOf { it.sugarG }
    val totalGrams: Double get() = ingredients.sumOf { it.grams }

    val caloriesPerServing: Double get() = totalCalories / servings.coerceAtLeast(1)
    val proteinPerServingG: Double get() = totalProteinG / servings.coerceAtLeast(1)
    val carbsPerServingG: Double get() = totalCarbsG / servings.coerceAtLeast(1)
    val fatPerServingG: Double get() = totalFatG / servings.coerceAtLeast(1)
    val fiberPerServingG: Double get() = totalFiberG / servings.coerceAtLeast(1)
    val sugarPerServingG: Double get() = totalSugarG / servings.coerceAtLeast(1)
    val gramsPerServing: Double get() = totalGrams / servings.coerceAtLeast(1)
}

@Serializable
data class PlannedMealEntry(
    val id: String = "",
    val dayOfWeek: String = "Monday",
    val mealType: String = "breakfast",
    val title: String = "",
    val grams: Double = 250.0,
    val calories: Double = 0.0,
    val proteinG: Double = 0.0,
    val carbsG: Double = 0.0,
    val fatG: Double = 0.0
)

@Serializable
data class RecentFoodEntry(
    val foodName: String = "",
    val brand: String = "",
    val caloriesPer100g: Double = 0.0,
    val proteinPer100g: Double = 0.0,
    val carbsPer100g: Double = 0.0,
    val fatPer100g: Double = 0.0,
    val fiberPer100g: Double = 0.0,
    val sugarPer100g: Double = 0.0,
    val lastGrams: Double = 100.0,
    val lastUsedMs: Long = System.currentTimeMillis()
) {
    fun toFoodItem() = FoodItem(
        id = "recent_${foodName.lowercase().replace(" ", "_")}",
        name = foodName,
        brand = brand,
        servingSizeGrams = lastGrams,
        servingLabel = "${lastGrams.toInt()}g",
        caloriesPer100g = caloriesPer100g,
        proteinPer100g = proteinPer100g,
        carbsPer100g = carbsPer100g,
        fatPer100g = fatPer100g,
        fiberPer100g = fiberPer100g,
        sugarPer100g = sugarPer100g
    )
}

class UserPreferencesStore(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private fun trainingKey(uid: String) = stringPreferencesKey("training_prefs_$uid")
    private fun nutritionKey(uid: String) = stringPreferencesKey("nutrition_prefs_$uid")
    private fun recipesKey(uid: String) = stringPreferencesKey("saved_recipes_$uid")
    private fun mealPlanKey(uid: String) = stringPreferencesKey("planned_meals_$uid")
    private fun recentFoodsKey(uid: String) = stringPreferencesKey("recent_foods_$uid")

    fun observeTrainingPreferences(uid: String): Flow<TrainingPreferences> =
        context.preferencesDataStore.data.map { prefs ->
            prefs[trainingKey(uid)]?.let { raw ->
                runCatching { json.decodeFromString<TrainingPreferences>(raw) }.getOrNull()
            } ?: TrainingPreferences()
        }

    suspend fun saveTrainingPreferences(uid: String, value: TrainingPreferences) {
        context.preferencesDataStore.edit { prefs ->
            prefs[trainingKey(uid)] = json.encodeToString(value)
        }
    }

    fun observeNutritionPreferences(uid: String): Flow<NutritionPreferences> =
        context.preferencesDataStore.data.map { prefs ->
            prefs[nutritionKey(uid)]?.let { raw ->
                runCatching { json.decodeFromString<NutritionPreferences>(raw) }.getOrNull()
            } ?: NutritionPreferences()
        }

    suspend fun saveNutritionPreferences(uid: String, value: NutritionPreferences) {
        context.preferencesDataStore.edit { prefs ->
            prefs[nutritionKey(uid)] = json.encodeToString(value)
        }
    }

    fun observeRecipes(uid: String): Flow<List<SavedRecipe>> =
        context.preferencesDataStore.data.map { prefs ->
            prefs[recipesKey(uid)]?.let { raw ->
                runCatching { json.decodeFromString<List<SavedRecipe>>(raw) }.getOrNull()
            } ?: emptyList()
        }

    suspend fun saveRecipe(uid: String, recipe: SavedRecipe) {
        context.preferencesDataStore.edit { prefs ->
            val current = prefs[recipesKey(uid)]?.let { raw ->
                runCatching { json.decodeFromString<List<SavedRecipe>>(raw) }.getOrNull()
            }.orEmpty().toMutableList()
            val clean = if (recipe.id.isBlank()) {
                recipe.copy(id = "rec_${System.currentTimeMillis()}")
            } else recipe
            val idx = current.indexOfFirst { it.id == clean.id }
            if (idx >= 0) current[idx] = clean else current.add(0, clean)
            prefs[recipesKey(uid)] = json.encodeToString(current)
        }
    }

    suspend fun deleteRecipe(uid: String, recipeId: String) {
        context.preferencesDataStore.edit { prefs ->
            val current = prefs[recipesKey(uid)]?.let { raw ->
                runCatching { json.decodeFromString<List<SavedRecipe>>(raw) }.getOrNull()
            }.orEmpty().filterNot { it.id == recipeId }
            prefs[recipesKey(uid)] = json.encodeToString(current)
        }
    }

    fun observePlannedMeals(uid: String): Flow<List<PlannedMealEntry>> =
        context.preferencesDataStore.data.map { prefs ->
            prefs[mealPlanKey(uid)]?.let { raw ->
                runCatching { json.decodeFromString<List<PlannedMealEntry>>(raw) }.getOrNull()
            } ?: emptyList()
        }

    suspend fun savePlannedMeal(uid: String, entry: PlannedMealEntry) {
        context.preferencesDataStore.edit { prefs ->
            val current = prefs[mealPlanKey(uid)]?.let { raw ->
                runCatching { json.decodeFromString<List<PlannedMealEntry>>(raw) }.getOrNull()
            }.orEmpty().toMutableList()
            val clean = if (entry.id.isBlank()) {
                entry.copy(id = "pm_${System.currentTimeMillis()}_${current.size}")
            } else entry
            val idx = current.indexOfFirst { it.id == clean.id }
            if (idx >= 0) current[idx] = clean else current.add(clean)
            prefs[mealPlanKey(uid)] = json.encodeToString(current)
        }
    }

    suspend fun deletePlannedMeal(uid: String, entryId: String) {
        context.preferencesDataStore.edit { prefs ->
            val current = prefs[mealPlanKey(uid)]?.let { raw ->
                runCatching { json.decodeFromString<List<PlannedMealEntry>>(raw) }.getOrNull()
            }.orEmpty().filterNot { it.id == entryId }
            prefs[mealPlanKey(uid)] = json.encodeToString(current)
        }
    }

    fun observeRecentFoods(uid: String): Flow<List<RecentFoodEntry>> =
        context.preferencesDataStore.data.map { prefs ->
            prefs[recentFoodsKey(uid)]?.let { raw ->
                runCatching { json.decodeFromString<List<RecentFoodEntry>>(raw) }.getOrNull()
            } ?: emptyList()
        }

    suspend fun recordRecentFood(uid: String, entry: RecentFoodEntry) {
        context.preferencesDataStore.edit { prefs ->
            val current = prefs[recentFoodsKey(uid)]?.let { raw ->
                runCatching { json.decodeFromString<List<RecentFoodEntry>>(raw) }.getOrNull()
            }.orEmpty().filterNot { it.foodName.equals(entry.foodName, ignoreCase = true) }.toMutableList()
            current.add(0, entry.copy(lastUsedMs = System.currentTimeMillis()))
            prefs[recentFoodsKey(uid)] = json.encodeToString(current.take(25))
        }
    }

    suspend fun clearAllForUser(uid: String) {
        context.preferencesDataStore.edit { prefs ->
            prefs.remove(trainingKey(uid))
            prefs.remove(nutritionKey(uid))
            prefs.remove(recipesKey(uid))
            prefs.remove(mealPlanKey(uid))
            prefs.remove(recentFoodsKey(uid))
        }
    }
}
