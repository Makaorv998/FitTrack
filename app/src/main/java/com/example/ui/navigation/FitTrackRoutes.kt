package com.example.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface FitTrackRoute {
    @Serializable data object Home : FitTrackRoute
    @Serializable data object Activity : FitTrackRoute
    @Serializable data object Nutrition : FitTrackRoute
    @Serializable data object Workouts : FitTrackRoute
    @Serializable data object Profile : FitTrackRoute

    // Secondary routes
    @Serializable data class ActivityMap(val activityId: String? = null) : FitTrackRoute
    @Serializable data object AiCoach : FitTrackRoute
    @Serializable data object AiFoodScanner : FitTrackRoute
    @Serializable data object AiMealPlanner : FitTrackRoute
    @Serializable data object AiWorkoutPlanner : FitTrackRoute
    @Serializable data object Calculators : FitTrackRoute
    @Serializable data object BodyProgress : FitTrackRoute
    @Serializable data object Challenges : FitTrackRoute
    @Serializable data object Settings : FitTrackRoute
    @Serializable data object UpdateCenter : FitTrackRoute
    @Serializable data class WhatsNew(val versionCode: Long? = null) : FitTrackRoute
}
