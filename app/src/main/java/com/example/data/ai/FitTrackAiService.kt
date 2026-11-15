package com.example.data.ai

import android.graphics.Bitmap
import android.util.Log
import com.example.data.model.UserProfile
import com.example.data.model.WorkoutExerciseEntry
import com.example.data.model.WorkoutPlan
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

data class ScannedFoodItem(
    val name: String,
    val estimatedGrams: Double,
    val calories: Double,
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double,
    val fiberG: Double,
    val sugarG: Double,
    val confidenceNote: String
)

data class FoodScanResult(
    val mealSummary: String,
    val items: List<ScannedFoodItem>,
    val uncertaintyNotice: String
)

data class AiMealSuggestion(
    val mealType: String,
    val title: String,
    val description: String,
    val grams: Double,
    val calories: Double,
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double
)

data class AiMealPlanResult(
    val planTitle: String,
    val dailyNotes: String,
    val meals: List<AiMealSuggestion>
)

class FitTrackAiService {

    private val coachModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            systemInstruction = content {
                text(
                    "You are FitTrack Coach, a supportive, evidence-based strength, conditioning, and nutrition assistant. " +
                        "Explain progress clearly, suggest sustainable habits, and avoid medical or injury diagnoses, extreme calorie deficits, or unsafe advice. " +
                        "Never instruct anyone to push through sharp, acute, or joint pain; recommend stopping the aggravating movement and consulting a qualified healthcare professional if pain persists. " +
                        "If the user is under 18, never recommend calorie restriction or weight loss deficits. Keep responses concise and scannable."
                )
            }
        )
    }

    private val foodScanSchema = Schema.obj(
        properties = mapOf(
            "mealSummary" to Schema.string("Short name for the scanned meal"),
            "uncertaintyNotice" to Schema.string("Honest statement explaining portion estimation uncertainty"),
            "items" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "name" to Schema.string("Likely food item name"),
                        "estimatedGrams" to Schema.double("Estimated portion weight in grams"),
                        "calories" to Schema.double("Estimated calories for the portion"),
                        "proteinG" to Schema.double("Estimated protein in grams"),
                        "carbsG" to Schema.double("Estimated carbohydrates in grams"),
                        "fatG" to Schema.double("Estimated fat in grams"),
                        "fiberG" to Schema.double("Estimated fiber in grams"),
                        "sugarG" to Schema.double("Estimated sugar in grams"),
                        "confidenceNote" to Schema.string("Confidence level and visual assumption")
                    )
                ),
                description = "Detected food items"
            )
        )
    )

    private val foodScanModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = foodScanSchema
            }
        )
    }

    private val workoutPlanSchema = Schema.obj(
        properties = mapOf(
            "planName" to Schema.string("Name of the workout routine"),
            "recoveryGuidance" to Schema.string("Rest and recovery recommendation"),
            "exercises" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "exerciseName" to Schema.string("Exercise name"),
                        "muscleGroup" to Schema.string("Chest, Back, Legs, Shoulders, Arms, Core, or Cardio"),
                        "equipment" to Schema.string("Equipment required"),
                        "targetSets" to Schema.integer("Recommended sets"),
                        "targetReps" to Schema.integer("Recommended reps per set"),
                        "targetWeightKg" to Schema.double("Suggested starting weight in kg (0 for bodyweight)"),
                        "restSeconds" to Schema.integer("Rest seconds between sets")
                    )
                ),
                description = "Ordered workout exercises"
            )
        )
    )

    private val workoutPlanModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = workoutPlanSchema
            }
        )
    }

    private val mealPlanSchema = Schema.obj(
        properties = mapOf(
            "planTitle" to Schema.string("Title for the day's meal plan"),
            "dailyNotes" to Schema.string("Nutritional notes and allergy accommodations"),
            "meals" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "mealType" to Schema.enumeration(listOf("breakfast", "lunch", "dinner", "snack")),
                        "title" to Schema.string("Meal title"),
                        "description" to Schema.string("Brief ingredients overview"),
                        "grams" to Schema.double("Approximate portion weight in grams"),
                        "calories" to Schema.double("Estimated calories"),
                        "proteinG" to Schema.double("Estimated protein grams"),
                        "carbsG" to Schema.double("Estimated carbs grams"),
                        "fatG" to Schema.double("Estimated fat grams")
                    )
                ),
                description = "Suggested meals for the day"
            )
        )
    )

    private val mealPlanModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = mealPlanSchema
            }
        )
    }

    fun streamCoachResponse(
        userMessage: String,
        profile: UserProfile?,
        todaySteps: Long,
        todayCaloriesIn: Double,
        todayWaterMl: Long,
        workoutContextSummary: String = ""
    ): Flow<String> = flow {
        val contextPrompt = buildString {
            appendLine("User Context:")
            if (profile != null) {
                appendLine("- Name: ${profile.displayName}, Age: ${profile.ageYears}, Goal: ${profile.goal}")
                appendLine("- Weight: ${profile.weightKg} kg, Target Weight: ${profile.targetWeightKg} kg")
                appendLine("- Daily Targets: ${profile.dailyStepGoal} steps, ${profile.dailyCalorieTarget} kcal, ${profile.dailyWaterTargetMl} ml water")
            }
            appendLine("- Today so far: $todaySteps steps, ${todayCaloriesIn.toInt()} kcal logged, $todayWaterMl ml water.")
            if (workoutContextSummary.isNotBlank()) {
                appendLine("- Recent Training: $workoutContextSummary")
            }
            appendLine("User question: $userMessage")
        }

        val accumulated = StringBuilder()
        try {
            withTimeout(60_000L) {
                coachModel.generateContentStream(contextPrompt).collect { chunk ->
                    val piece = chunk.text.orEmpty()
                    if (piece.isNotEmpty()) {
                        accumulated.append(piece)
                        emit(accumulated.toString())
                    }
                }
            }
            if (accumulated.isBlank()) {
                throw IllegalStateException("AI Coach returned an empty response.")
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Coach stream timed out", e)
            throw IllegalStateException("AI Coach timed out after 60 seconds. Please try again.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Coach stream failed", e)
            throw e
        }
    }

    suspend fun analyzeFoodImage(bitmap: Bitmap): FoodScanResult {
        return try {
            val response = withTimeout(60_000L) {
                foodScanModel.generateContent(
                    content {
                        image(bitmap)
                        text(
                            "Identify the likely food items in this photo, estimate realistic portion weights in grams, " +
                                "and calculate estimated calories and macros. Clearly note estimation uncertainty; do not claim exact values."
                        )
                    }
                )
            }
            val jsonStr = response.text ?: throw IllegalStateException("Empty AI response.")
            val json = JSONObject(jsonStr)
            val summary = json.optString("mealSummary", "Scanned Meal")
            val uncertainty = json.optString(
                "uncertaintyNotice",
                "Portion sizes and calories are visual estimates only."
            )
            val arr = json.getJSONArray("items")
            val items = (0 until arr.length()).map { idx ->
                val obj = arr.getJSONObject(idx)
                ScannedFoodItem(
                    name = obj.optString("name", "Food Item"),
                    estimatedGrams = obj.optDouble("estimatedGrams", 150.0).coerceIn(1.0, 3000.0),
                    calories = obj.optDouble("calories", 200.0).coerceIn(0.0, 5000.0),
                    proteinG = obj.optDouble("proteinG", 10.0).coerceIn(0.0, 500.0),
                    carbsG = obj.optDouble("carbsG", 20.0).coerceIn(0.0, 500.0),
                    fatG = obj.optDouble("fatG", 8.0).coerceIn(0.0, 500.0),
                    fiberG = obj.optDouble("fiberG", 2.0).coerceIn(0.0, 200.0),
                    sugarG = obj.optDouble("sugarG", 3.0).coerceIn(0.0, 200.0),
                    confidenceNote = obj.optString("confidenceNote", "Visual estimate")
                )
            }
            FoodScanResult(summary, items, uncertainty)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Food scan timed out", e)
            throw IllegalStateException("Food scan timed out after 60 seconds. Please retry.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Food scan failed", e)
            throw e
        }
    }

    suspend fun generateWorkoutPlan(
        experienceLevel: String,
        equipment: String,
        goal: String,
        daysPerWeek: Int
    ): Pair<WorkoutPlan, String> {
        val prompt = "Create a safe, effective workout session plan for a $experienceLevel athlete " +
            "with goal '$goal', training $daysPerWeek days/week, using equipment: $equipment. " +
            "Include 4 to 6 exercises and practical recovery guidance."
        return try {
            val response = withTimeout(60_000L) {
                workoutPlanModel.generateContent(prompt)
            }
            val json = JSONObject(response.text ?: throw IllegalStateException("Empty AI response."))
            val planName = json.optString("planName", "AI Custom Workout")
            val recovery = json.optString("recoveryGuidance", "Allow 48 hours between intense sessions for the same muscle group.")
            val arr = json.getJSONArray("exercises")
            val entries = (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                WorkoutExerciseEntry(
                    exerciseId = "ai_$i",
                    exerciseName = obj.optString("exerciseName", "Exercise"),
                    muscleGroup = obj.optString("muscleGroup", "Core"),
                    equipment = obj.optString("equipment", equipment),
                    targetSets = obj.optLong("targetSets", 3L).coerceIn(1L, 10L),
                    targetReps = obj.optLong("targetReps", 10L).coerceIn(1L, 60L),
                    targetWeightKg = obj.optDouble("targetWeightKg", 0.0).coerceIn(0.0, 300.0),
                    restSeconds = obj.optLong("restSeconds", 60L).coerceIn(15L, 300L)
                )
            }
            WorkoutPlan(
                name = planName,
                level = experienceLevel,
                targetGoal = goal,
                exercises = entries,
                isAiGenerated = true
            ) to recovery
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Workout planner timed out", e)
            throw IllegalStateException("Workout planner timed out after 60 seconds.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Workout planner failed", e)
            throw e
        }
    }

    suspend fun generateMealPlan(
        calorieTarget: Int,
        proteinG: Int,
        carbsG: Int,
        fatG: Int,
        preferences: String,
        allergies: String
    ): AiMealPlanResult {
        val prompt = "Create a balanced 1-day meal plan (breakfast, lunch, dinner, snack) targeting approximately " +
            "$calorieTarget kcal ($proteinG g protein, $carbsG g carbs, $fatG g fat). " +
            "Dietary preferences: ${preferences.ifBlank { "Balanced" }}. " +
            "Allergies/Exclusions: ${allergies.ifBlank { "None" }}. Provide nutritional estimates."
        return try {
            val response = withTimeout(60_000L) {
                mealPlanModel.generateContent(prompt)
            }
            val json = JSONObject(response.text ?: throw IllegalStateException("Empty AI response."))
            val title = json.optString("planTitle", "Daily Balanced Plan")
            val notes = json.optString("dailyNotes", "Nutritional values are estimates.")
            val arr = json.getJSONArray("meals")
            val meals = (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                AiMealSuggestion(
                    mealType = obj.optString("mealType", "lunch"),
                    title = obj.optString("title", "Meal"),
                    description = obj.optString("description", ""),
                    grams = obj.optDouble("grams", 300.0).coerceIn(10.0, 2500.0),
                    calories = obj.optDouble("calories", 400.0).coerceIn(0.0, 4000.0),
                    proteinG = obj.optDouble("proteinG", 25.0).coerceIn(0.0, 300.0),
                    carbsG = obj.optDouble("carbsG", 40.0).coerceIn(0.0, 400.0),
                    fatG = obj.optDouble("fatG", 12.0).coerceIn(0.0, 200.0)
                )
            }
            AiMealPlanResult(title, notes, meals)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Meal planner timed out", e)
            throw IllegalStateException("Meal planner timed out after 60 seconds.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Meal planner failed", e)
            throw e
        }
    }

    suspend fun suggestExerciseSubstitutions(
        exerciseName: String,
        muscleGroup: String,
        availableEquipment: String,
        reason: String
    ): String {
        val prompt = "Suggest 3 safe, effective exercise substitutions for '$exerciseName' targeting '$muscleGroup' " +
            "using available equipment: $availableEquipment. Reason for swap: ${reason.ifBlank { "Equipment busy or preference" }}. " +
            "Provide brief setup cues for each. Do not diagnose injuries; if discomfort is mentioned, advise comfortable range of motion and stopping on sharp pain."
        return try {
            val response = withTimeout(60_000L) {
                coachModel.generateContent(prompt)
            }
            response.text?.takeIf { it.isNotBlank() }
                ?: "Try a controlled machine, dumbbell, or band alternative targeting $muscleGroup."
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Substitution timed out", e)
            throw IllegalStateException("AI suggestion timed out after 60 seconds.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Substitution failed", e)
            throw e
        }
    }

    suspend fun analyzeWorkoutProgressAndRecovery(
        profile: UserProfile?,
        recentSessionsSummary: String,
        personalRecordsSummary: String
    ): String {
        val prompt = buildString {
            appendLine("Review this athlete's training history and provide concise, practical guidance:")
            if (profile != null) {
                appendLine("Goal: ${profile.goal}, Activity: ${profile.activityLevel}, Age: ${profile.ageYears}")
            }
            appendLine("Recent sessions: $recentSessionsSummary")
            appendLine("Personal records: $personalRecordsSummary")
            appendLine("Give: 1) Volume & consistency observation, 2) Safe progressive overload focus, 3) Recovery/deload tip. Avoid medical claims.")
        }
        return try {
            val response = withTimeout(60_000L) {
                coachModel.generateContent(prompt)
            }
            response.text?.takeIf { it.isNotBlank() }
                ?: "Keep logging consistent working sets and allow 48h recovery per muscle group."
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Workout analysis timed out", e)
            throw IllegalStateException("AI workout analysis timed out after 60 seconds.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Workout analysis failed", e)
            throw e
        }
    }

    private companion object {
        const val TAG = "FitTrackAiService"
    }
}
