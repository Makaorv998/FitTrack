package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.firebase.FitTrackRepository
import com.example.data.model.LatLngPoint
import com.example.domain.FitnessCalculator
import com.example.domain.UnitFormatter
import com.example.platform.GpsTrackerState
import com.example.platform.GpsTrackerStore
import com.example.platform.StepCounterManager
import com.example.platform.TrackingStatus
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CoreFitnessSystemTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("fittrack_step_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("fittrack_gps_tracker_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        GpsTrackerStore.reset(context)
    }

    @After
    fun tearDown() {
        GpsTrackerStore.reset(context)
    }

    @Test
    fun dobComputation_andMinorGuardrails_handleLeapYearsFutureAndInvalidInputs() {
        val nowCal = Calendar.getInstance().apply { set(2026, Calendar.NOVEMBER, 15, 12, 0, 0) }
        val nowMs = nowCal.timeInMillis

        // Minor: born 2009-12-01 -> 16 years old on 2026-11-15
        val minorAge = FitnessCalculator.computeAgeFromDobIso("2009-12-01", nowMs)
        assertEquals(16, minorAge)
        assertTrue(FitnessCalculator.isMinorFromProfile(25L, "2009-12-01", nowMs))

        // Adult: born 2000-06-15 -> 26 years old
        val adultAge = FitnessCalculator.computeAgeFromDobIso("2000-06-15", nowMs)
        assertEquals(26, adultAge)
        assertFalse(FitnessCalculator.isMinorFromProfile(16L, "2000-06-15", nowMs))

        // Future / invalid DOB returns null cleanly
        assertNull(FitnessCalculator.computeAgeFromDobIso("2030-01-01", nowMs))
        assertNull(FitnessCalculator.computeAgeFromDobIso("2000-02-31", nowMs))
        assertNull(FitnessCalculator.computeAgeFromDobIso("not-a-date", nowMs))

        // Step calories scale linearly with steps and body weight
        val cals70 = FitnessCalculator.calculateStepCalories(10_000L, 70.0)
        val cals140 = FitnessCalculator.calculateStepCalories(10_000L, 140.0)
        assertEquals(400.0, cals70, 0.01)
        assertEquals(800.0, cals140, 0.01)
    }

    @Test
    fun unitFormatter_roundTripsMetricAndImperialConsistently() {
        val kg = 80.0
        val lb = UnitFormatter.kgToDisplayWeight(kg, "imperial")
        assertEquals(176.37, lb, 0.2)
        assertEquals(kg, UnitFormatter.inputWeightToKg(lb, "imperial"), 0.05)

        val cm = 180.0
        val inches = UnitFormatter.cmToDisplayHeight(cm, "imperial")
        assertEquals(cm, UnitFormatter.inputHeightToCm(inches, "imperial"), 0.05)
        assertEquals("5' 11\"", UnitFormatter.formatHeight(cm, "imperial"))
        assertEquals("180 cm", UnitFormatter.formatHeight(cm, "metric"))

        val dist = UnitFormatter.formatDistance(10.0, "imperial")
        assertTrue(dist.endsWith(" mi"))
        assertEquals("6.21 mi", dist)

        val waterRatio = UnitFormatter.formatWaterRatio(500L, 2500L, "imperial")
        assertTrue(waterRatio.contains("fl oz"))
    }

    @Test
    fun stepCounterManager_handlesHardwareBaselineRebootAndMidnightReset() {
        val manager = StepCounterManager(context)
        val day1 = com.example.platform.todayDateKey()
        val day2 = "2099-01-02"

        // Initial boot counter reading sets baseline and counts delta steps
        manager.handleStepCounterSample(totalSinceBoot = 1000L, dateOverride = day1)
        manager.handleStepCounterSample(totalSinceBoot = 1450L, dateOverride = day1)
        assertEquals(450L, manager.state.value.dailyHardwareSteps)

        // Device reboot resets boot counter to 200; daily hardware steps must carry forward safely
        manager.handleStepCounterSample(totalSinceBoot = 200L, dateOverride = day1)
        manager.handleStepCounterSample(totalSinceBoot = 350L, dateOverride = day1)
        assertEquals(600L, manager.state.value.dailyHardwareSteps)

        // Midnight rollout resets daily hardware & manual steps to 0 while keeping last boot baseline
        manager.addManualSteps(250L)
        manager.handleStepCounterSample(totalSinceBoot = 400L, dateOverride = day2)
        assertEquals(day2, manager.state.value.dateKey)
        assertEquals(50L, manager.state.value.dailyHardwareSteps)
        assertEquals(0L, manager.state.value.manualSteps)
    }

    @Test
    fun gpsTrackerStore_persistsAndRestoresActiveRouteAcrossRestart() {
        GpsTrackerStore.initialize(context)
        GpsTrackerStore.update {
            GpsTrackerState(
                status = TrackingStatus.ACTIVE,
                activityType = "running",
                elapsedSeconds = 620L,
                distanceKm = 2.45,
                avgPaceMinPerKm = 4.22,
                estimatedCalories = 185.0,
                routePoints = listOf(
                    LatLngPoint(37.7749, -122.4194, 1000L),
                    LatLngPoint(37.7790, -122.4150, 2000L)
                ),
                startedAtMs = 1000L,
                weightKg = 72.0
            )
        }

        // Simulate process death by reading raw prefs into fresh state
        val rawJson = context.getSharedPreferences("fittrack_gps_tracker_prefs", Context.MODE_PRIVATE)
            .getString("active_gps_session_json", null)
        assertTrue(!rawJson.isNullOrBlank())

        // Reset in-memory only and verify initialize recovers as PAUSED
        val prefsField = GpsTrackerStore::class.java.getDeclaredField("prefs").apply { isAccessible = true }
        val stateField = GpsTrackerStore::class.java.getDeclaredField("_state").apply { isAccessible = true }
        @Suppress("UNCHECKED_CAST")
        val mutableState = stateField.get(GpsTrackerStore) as kotlinx.coroutines.flow.MutableStateFlow<GpsTrackerState>
        mutableState.value = GpsTrackerState()
        prefsField.set(GpsTrackerStore, null)

        GpsTrackerStore.initialize(context)
        val restored = GpsTrackerStore.state.value
        assertEquals(TrackingStatus.PAUSED, restored.status)
        assertEquals("running", restored.activityType)
        assertEquals(620L, restored.elapsedSeconds)
        assertEquals(2.45, restored.distanceKm, 0.001)
        assertEquals(2, restored.routePoints.size)
    }

    @Test
    fun downsampleRoutePoints_preservesStartAndEndAndCapsCount() {
        val longRoute = (0 until 1000).map { i ->
            LatLngPoint(37.0 + i * 0.0001, -122.0 - i * 0.0001, i * 1000L)
        }
        val sampled = FitTrackRepository.downsampleRoutePoints(longRoute, maxPoints = 250)
        assertEquals(250, sampled.size)
        assertEquals(longRoute.first(), sampled.first())
        assertEquals(longRoute.last(), sampled.last())
    }
}
