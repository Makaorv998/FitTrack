package com.example.domain

import java.util.Locale
import kotlin.math.roundToInt

object UnitFormatter {

    private const val KG_TO_LB = 2.20462262185
    private const val CM_TO_IN = 0.3937007874
    private const val KM_TO_MI = 0.6213711922
    private const val ML_TO_FL_OZ = 0.0338140227

    fun isImperial(unitSystem: String?): Boolean =
        unitSystem.equals("imperial", ignoreCase = true)

    fun weightUnitLabel(unitSystem: String?): String =
        if (isImperial(unitSystem)) "lb" else "kg"

    fun heightInputLabel(unitSystem: String?): String =
        if (isImperial(unitSystem)) "in" else "cm"

    fun distanceUnitLabel(unitSystem: String?): String =
        if (isImperial(unitSystem)) "mi" else "km"

    fun waterUnitLabel(unitSystem: String?): String =
        if (isImperial(unitSystem)) "fl oz" else "ml"

    fun kgToDisplayWeight(weightKg: Double, unitSystem: String?): Double =
        if (isImperial(unitSystem)) weightKg * KG_TO_LB else weightKg

    fun inputWeightToKg(inputWeight: Double, unitSystem: String?): Double =
        if (isImperial(unitSystem)) inputWeight / KG_TO_LB else inputWeight

    fun cmToDisplayHeight(heightCm: Double, unitSystem: String?): Double =
        if (isImperial(unitSystem)) heightCm * CM_TO_IN else heightCm

    fun inputHeightToCm(inputHeight: Double, unitSystem: String?): Double =
        if (isImperial(unitSystem)) inputHeight / CM_TO_IN else inputHeight

    fun kmToDisplayDistance(distanceKm: Double, unitSystem: String?): Double =
        if (isImperial(unitSystem)) distanceKm * KM_TO_MI else distanceKm

    fun inputDistanceToKm(inputDistance: Double, unitSystem: String?): Double =
        if (isImperial(unitSystem)) inputDistance / KM_TO_MI else inputDistance

    fun mlToDisplayWater(ml: Long, unitSystem: String?): Double =
        if (isImperial(unitSystem)) ml * ML_TO_FL_OZ else ml.toDouble()

    fun inputWaterToMl(inputAmount: Double, unitSystem: String?): Long =
        if (isImperial(unitSystem)) (inputAmount / ML_TO_FL_OZ).roundToInt().toLong() else inputAmount.roundToInt().toLong()

    fun formatWeight(weightKg: Double, unitSystem: String?, decimals: Int = 1): String {
        val value = kgToDisplayWeight(weightKg, unitSystem)
        return String.format(Locale.US, "%.${decimals}f %s", value, weightUnitLabel(unitSystem))
    }

    fun formatHeight(heightCm: Double, unitSystem: String?): String {
        if (!isImperial(unitSystem)) {
            return String.format(Locale.US, "%.0f cm", heightCm)
        }
        val totalInches = (heightCm * CM_TO_IN).roundToInt().coerceAtLeast(0)
        val feet = totalInches / 12
        val inches = totalInches % 12
        return "$feet' $inches\""
    }

    fun formatDistance(distanceKm: Double, unitSystem: String?, decimals: Int = 2): String {
        val value = kmToDisplayDistance(distanceKm, unitSystem)
        return String.format(Locale.US, "%.${decimals}f %s", value, distanceUnitLabel(unitSystem))
    }

    fun formatSpeed(speedKmh: Double, unitSystem: String?, decimals: Int = 1): String {
        val value = kmToDisplayDistance(speedKmh, unitSystem)
        val unit = if (isImperial(unitSystem)) "mph" else "km/h"
        return String.format(Locale.US, "%.${decimals}f %s", value, unit)
    }

    fun formatPace(paceMinPerKm: Double, unitSystem: String?): String {
        val unit = if (isImperial(unitSystem)) "/mi" else "/km"
        if (paceMinPerKm <= 0.0 || paceMinPerKm > 99.0) return "--:-- $unit"
        val adjusted = if (isImperial(unitSystem)) paceMinPerKm / KM_TO_MI else paceMinPerKm
        if (adjusted > 99.0) return "--:-- $unit"
        val mins = adjusted.toInt()
        val secs = ((adjusted - mins) * 60.0).roundToInt().coerceIn(0, 59)
        return String.format(Locale.US, "%d:%02d %s", mins, secs, unit)
    }

    fun formatWater(ml: Long, unitSystem: String?): String {
        return if (isImperial(unitSystem)) {
            String.format(Locale.US, "%.0f fl oz", ml * ML_TO_FL_OZ)
        } else {
            "$ml ml"
        }
    }

    fun formatWaterRatio(currentMl: Long, targetMl: Long, unitSystem: String?): String {
        return if (isImperial(unitSystem)) {
            String.format(Locale.US, "%.0f / %.0f fl oz", currentMl * ML_TO_FL_OZ, targetMl * ML_TO_FL_OZ)
        } else {
            "$currentMl / $targetMl ml"
        }
    }
}
