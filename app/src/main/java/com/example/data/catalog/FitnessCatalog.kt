package com.example.data.catalog

import com.example.data.model.ExerciseDefinition
import com.example.data.model.FoodItem
import com.example.data.model.UserChallenge
import com.example.data.model.WorkoutExerciseEntry
import com.example.data.model.WorkoutPlan

object FitnessCatalog {

    val builtInFoods: List<FoodItem> = listOf(
        FoodItem(id = "food_chicken_breast", name = "Grilled Chicken Breast", brand = "Whole Foods", servingSizeGrams = 150.0, servingLabel = "150g fillet", caloriesPer100g = 165.0, proteinPer100g = 31.0, carbsPer100g = 0.0, fatPer100g = 3.6, fiberPer100g = 0.0, sugarPer100g = 0.0),
        FoodItem(id = "food_salmon", name = "Atlantic Salmon Fillet", brand = "Fresh Catch", servingSizeGrams = 150.0, servingLabel = "150g fillet", caloriesPer100g = 208.0, proteinPer100g = 20.0, carbsPer100g = 0.0, fatPer100g = 13.0, fiberPer100g = 0.0, sugarPer100g = 0.0),
        FoodItem(id = "food_eggs", name = "Whole Eggs (Boiled/Scrambled)", brand = "Farm Fresh", servingSizeGrams = 100.0, servingLabel = "2 large eggs (100g)", caloriesPer100g = 155.0, proteinPer100g = 13.0, carbsPer100g = 1.1, fatPer100g = 11.0, fiberPer100g = 0.0, sugarPer100g = 1.1),
        FoodItem(id = "food_greek_yogurt", name = "Greek Yogurt (0% Fat)", brand = "Dairy", servingSizeGrams = 170.0, servingLabel = "1 cup (170g)", caloriesPer100g = 59.0, proteinPer100g = 10.0, carbsPer100g = 3.6, fatPer100g = 0.4, fiberPer100g = 0.0, sugarPer100g = 3.2),
        FoodItem(id = "food_oats", name = "Rolled Oats (Dry)", brand = "Whole Grain", servingSizeGrams = 60.0, servingLabel = "1/2 cup dry (60g)", caloriesPer100g = 389.0, proteinPer100g = 16.9, carbsPer100g = 66.3, fatPer100g = 6.9, fiberPer100g = 10.6, sugarPer100g = 0.9),
        FoodItem(id = "food_brown_rice", name = "Cooked Brown Rice", brand = "Whole Grain", servingSizeGrams = 180.0, servingLabel = "1 cup cooked (180g)", caloriesPer100g = 112.0, proteinPer100g = 2.6, carbsPer100g = 23.5, fatPer100g = 0.9, fiberPer100g = 1.8, sugarPer100g = 0.4),
        FoodItem(id = "food_white_rice", name = "Cooked Jasmine White Rice", brand = "Grain", servingSizeGrams = 180.0, servingLabel = "1 cup cooked (180g)", caloriesPer100g = 130.0, proteinPer100g = 2.7, carbsPer100g = 28.2, fatPer100g = 0.3, fiberPer100g = 0.4, sugarPer100g = 0.1),
        FoodItem(id = "food_quinoa", name = "Cooked Quinoa", brand = "Grain", servingSizeGrams = 185.0, servingLabel = "1 cup cooked (185g)", caloriesPer100g = 120.0, proteinPer100g = 4.4, carbsPer100g = 21.3, fatPer100g = 1.9, fiberPer100g = 2.8, sugarPer100g = 0.9),
        FoodItem(id = "food_banana", name = "Banana", brand = "Fresh Fruit", servingSizeGrams = 120.0, servingLabel = "1 medium (120g)", caloriesPer100g = 89.0, proteinPer100g = 1.1, carbsPer100g = 22.8, fatPer100g = 0.3, fiberPer100g = 2.6, sugarPer100g = 12.2),
        FoodItem(id = "food_apple", name = "Crisp Apple", brand = "Fresh Fruit", servingSizeGrams = 180.0, servingLabel = "1 medium (180g)", caloriesPer100g = 52.0, proteinPer100g = 0.3, carbsPer100g = 13.8, fatPer100g = 0.2, fiberPer100g = 2.4, sugarPer100g = 10.4),
        FoodItem(id = "food_blueberries", name = "Fresh Blueberries", brand = "Berries", servingSizeGrams = 148.0, servingLabel = "1 cup (148g)", caloriesPer100g = 57.0, proteinPer100g = 0.7, carbsPer100g = 14.5, fatPer100g = 0.3, fiberPer100g = 2.4, sugarPer100g = 10.0),
        FoodItem(id = "food_avocado", name = "Avocado", brand = "Fresh Produce", servingSizeGrams = 100.0, servingLabel = "1/2 avocado (100g)", caloriesPer100g = 160.0, proteinPer100g = 2.0, carbsPer100g = 8.5, fatPer100g = 14.7, fiberPer100g = 6.7, sugarPer100g = 0.7),
        FoodItem(id = "food_almonds", name = "Raw Almonds", brand = "Nuts", servingSizeGrams = 30.0, servingLabel = "1 handful (30g)", caloriesPer100g = 579.0, proteinPer100g = 21.2, carbsPer100g = 21.6, fatPer100g = 49.9, fiberPer100g = 12.5, sugarPer100g = 4.4),
        FoodItem(id = "food_peanut_butter", name = "Natural Peanut Butter", brand = "Spread", servingSizeGrams = 32.0, servingLabel = "2 tbsp (32g)", caloriesPer100g = 588.0, proteinPer100g = 25.0, carbsPer100g = 20.0, fatPer100g = 50.0, fiberPer100g = 6.0, sugarPer100g = 9.0),
        FoodItem(id = "food_whey_protein", name = "Whey Protein Isolate", brand = "Supplement", servingSizeGrams = 32.0, servingLabel = "1 scoop (32g)", caloriesPer100g = 375.0, proteinPer100g = 84.0, carbsPer100g = 5.0, fatPer100g = 1.5, fiberPer100g = 0.0, sugarPer100g = 2.0),
        FoodItem(id = "food_sweet_potato", name = "Baked Sweet Potato", brand = "Produce", servingSizeGrams = 150.0, servingLabel = "1 medium (150g)", caloriesPer100g = 90.0, proteinPer100g = 2.0, carbsPer100g = 20.7, fatPer100g = 0.2, fiberPer100g = 3.3, sugarPer100g = 6.5),
        FoodItem(id = "food_broccoli", name = "Steamed Broccoli", brand = "Produce", servingSizeGrams = 150.0, servingLabel = "1.5 cups (150g)", caloriesPer100g = 35.0, proteinPer100g = 2.4, carbsPer100g = 7.2, fatPer100g = 0.4, fiberPer100g = 3.3, sugarPer100g = 1.4),
        FoodItem(id = "food_spinach", name = "Fresh Baby Spinach", brand = "Produce", servingSizeGrams = 85.0, servingLabel = "2.5 cups (85g)", caloriesPer100g = 23.0, proteinPer100g = 2.9, carbsPer100g = 3.6, fatPer100g = 0.4, fiberPer100g = 2.2, sugarPer100g = 0.4),
        FoodItem(id = "food_tofu", name = "Extra Firm Tofu", brand = "Plant Protein", servingSizeGrams = 150.0, servingLabel = "150g block", caloriesPer100g = 83.0, proteinPer100g = 10.0, carbsPer100g = 1.2, fatPer100g = 4.5, fiberPer100g = 0.8, sugarPer100g = 0.5),
        FoodItem(id = "food_lentils", name = "Cooked Brown Lentils", brand = "Legumes", servingSizeGrams = 198.0, servingLabel = "1 cup cooked (198g)", caloriesPer100g = 116.0, proteinPer100g = 9.0, carbsPer100g = 20.1, fatPer100g = 0.4, fiberPer100g = 7.9, sugarPer100g = 1.8),
        FoodItem(id = "food_tuna", name = "Canned Tuna in Water", brand = "Seafood", servingSizeGrams = 120.0, servingLabel = "1 drained can (120g)", caloriesPer100g = 116.0, proteinPer100g = 25.5, carbsPer100g = 0.0, fatPer100g = 0.8, fiberPer100g = 0.0, sugarPer100g = 0.0),
        FoodItem(id = "food_lean_beef", name = "Lean Ground Beef (93/7)", brand = "Meat", servingSizeGrams = 150.0, servingLabel = "150g cooked", caloriesPer100g = 214.0, proteinPer100g = 26.0, carbsPer100g = 0.0, fatPer100g = 11.5, fiberPer100g = 0.0, sugarPer100g = 0.0),
        FoodItem(id = "food_whole_wheat_bread", name = "Whole Wheat Bread", brand = "Bakery", servingSizeGrams = 56.0, servingLabel = "2 slices (56g)", caloriesPer100g = 247.0, proteinPer100g = 13.0, carbsPer100g = 41.0, fatPer100g = 3.4, fiberPer100g = 6.0, sugarPer100g = 5.0),
        FoodItem(id = "food_cottage_cheese", name = "Low-Fat Cottage Cheese (2%)", brand = "Dairy", servingSizeGrams = 150.0, servingLabel = "2/3 cup (150g)", caloriesPer100g = 81.0, proteinPer100g = 10.5, carbsPer100g = 4.3, fatPer100g = 2.3, fiberPer100g = 0.0, sugarPer100g = 3.6),
        FoodItem(id = "food_olive_oil", name = "Extra Virgin Olive Oil", brand = "Pantry", servingSizeGrams = 14.0, servingLabel = "1 tbsp (14ml)", measurementUnit = "ml", caloriesPer100g = 884.0, proteinPer100g = 0.0, carbsPer100g = 0.0, fatPer100g = 100.0, fiberPer100g = 0.0, sugarPer100g = 0.0),
        FoodItem(id = "food_pasta", name = "Cooked Whole Wheat Pasta", brand = "Grain", servingSizeGrams = 140.0, servingLabel = "1 cup cooked (140g)", caloriesPer100g = 124.0, proteinPer100g = 5.3, carbsPer100g = 26.5, fatPer100g = 0.5, fiberPer100g = 3.9, sugarPer100g = 0.8),
        FoodItem(id = "food_edamame", name = "Shelled Edamame", brand = "Plant Protein", servingSizeGrams = 155.0, servingLabel = "1 cup (155g)", caloriesPer100g = 121.0, proteinPer100g = 11.9, carbsPer100g = 8.9, fatPer100g = 5.2, fiberPer100g = 5.2, sugarPer100g = 2.2),
        FoodItem(id = "food_milk", name = "Skim Milk", brand = "Dairy", servingSizeGrams = 245.0, servingLabel = "1 cup (245ml)", measurementUnit = "ml", caloriesPer100g = 34.0, proteinPer100g = 3.4, carbsPer100g = 5.0, fatPer100g = 0.1, fiberPer100g = 0.0, sugarPer100g = 5.0)
    )

    val exercises: List<ExerciseDefinition>
        get() = WorkoutCatalog.exercises

    val starterTemplates: List<WorkoutPlan>
        get() = WorkoutCatalog.starterTemplates

    fun defaultChallenges(uid: String): List<UserChallenge> = listOf(
        UserChallenge(
            id = "ch_10k_steps",
            userId = uid,
            title = "10K Daily Stride",
            description = "Reach 10,000 tracked steps in a single day.",
            category = "steps",
            targetValue = 10000.0,
            currentValue = 0.0,
            unit = "steps",
            joined = true,
            completed = false
        ),
        UserChallenge(
            id = "ch_hydration_hero",
            userId = uid,
            title = "Hydration Hero",
            description = "Log 2,500 ml of water intake today.",
            category = "water",
            targetValue = 2500.0,
            currentValue = 0.0,
            unit = "ml",
            joined = true,
            completed = false
        ),
        UserChallenge(
            id = "ch_5k_run",
            userId = uid,
            title = "5K Outdoor Explorer",
            description = "Complete 5.0 km of GPS walking or running.",
            category = "gps",
            targetValue = 5.0,
            currentValue = 0.0,
            unit = "km",
            joined = false,
            completed = false
        ),
        UserChallenge(
            id = "ch_strength_week",
            userId = uid,
            title = "Iron Consistency",
            description = "Complete 3 logged strength or conditioning workouts.",
            category = "workouts",
            targetValue = 3.0,
            currentValue = 0.0,
            unit = "workouts",
            joined = false,
            completed = false
        )
    )
}
