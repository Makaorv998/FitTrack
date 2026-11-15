package com.example.platform

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class StepSensorState(
    val dateKey: String = todayDateKey(),
    val hardwareAvailable: Boolean = false,
    val detectorAvailable: Boolean = false,
    val dailyHardwareSteps: Long = 0L,
    val manualSteps: Long = 0L,
    val sensorBaseline: Long = 0L,
    val isListening: Boolean = false
) {
    val totalSteps: Long get() = (dailyHardwareSteps + manualSteps).coerceAtLeast(0L)
}

fun todayDateKey(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

class StepCounterManager(context: Context) : SensorEventListener {

    private val appContext = context.applicationContext
    private val sensorManager = appContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val stepCounterSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    private val stepDetectorSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
    private val prefs = appContext.getSharedPreferences("fittrack_step_prefs", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(loadPersistedState())
    val state: StateFlow<StepSensorState> = _state.asStateFlow()

    private fun loadPersistedState(): StepSensorState {
        val today = todayDateKey()
        val storedDate = prefs.getString("date_key", today) ?: today
        if (storedDate != today) {
            val lastCounter = prefs.getLong("last_boot_counter", -1L)
            prefs.edit()
                .putString("date_key", today)
                .putLong("daily_baseline", if (lastCounter >= 0L) lastCounter else 0L)
                .putLong("daily_hardware_steps", 0L)
                .putLong("manual_steps", 0L)
                .apply()
        }
        return StepSensorState(
            dateKey = today,
            hardwareAvailable = stepCounterSensor != null || stepDetectorSensor != null,
            detectorAvailable = stepDetectorSensor != null,
            dailyHardwareSteps = prefs.getLong("daily_hardware_steps", 0L),
            manualSteps = prefs.getLong("manual_steps", 0L),
            sensorBaseline = prefs.getLong("daily_baseline", 0L),
            isListening = false
        )
    }

    fun startListening() {
        checkMidnightReset()
        var registered = false
        stepCounterSensor?.let {
            registered = sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) || registered
        }
        if (stepCounterSensor == null && stepDetectorSensor != null) {
            registered = sensorManager.registerListener(this, stepDetectorSensor, SensorManager.SENSOR_DELAY_UI) || registered
        }
        _state.value = _state.value.copy(isListening = registered)
    }

    fun stopListening() {
        sensorManager.unregisterListener(this)
        _state.value = _state.value.copy(isListening = false)
    }

    fun checkMidnightReset() {
        val today = todayDateKey()
        val current = _state.value
        if (current.dateKey != today) {
            val lastBoot = prefs.getLong("last_boot_counter", -1L)
            val newBaseline = if (lastBoot >= 0L) lastBoot else 0L
            prefs.edit()
                .putString("date_key", today)
                .putLong("daily_baseline", newBaseline)
                .putLong("daily_hardware_steps", 0L)
                .putLong("manual_steps", 0L)
                .apply()
            _state.value = current.copy(
                dateKey = today,
                dailyHardwareSteps = 0L,
                manualSteps = 0L,
                sensorBaseline = newBaseline
            )
        }
    }

    fun addManualSteps(deltaSteps: Long) {
        checkMidnightReset()
        val updated = (_state.value.manualSteps + deltaSteps).coerceIn(0L, 500_000L)
        prefs.edit().putLong("manual_steps", updated).apply()
        _state.value = _state.value.copy(manualSteps = updated)
    }

    fun setManualSteps(steps: Long) {
        checkMidnightReset()
        val updated = steps.coerceIn(0L, 500_000L)
        prefs.edit().putLong("manual_steps", updated).apply()
        _state.value = _state.value.copy(manualSteps = updated)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return
        checkMidnightReset()
        if (event.sensor.type == Sensor.TYPE_STEP_COUNTER) {
            val totalSinceBoot = event.values.firstOrNull()?.toLong() ?: return
            val lastBootCounter = prefs.getLong("last_boot_counter", -1L)
            var baseline = prefs.getLong("daily_baseline", -1L)
            var carriedSteps = prefs.getLong("daily_hardware_steps", 0L)

            // Handle reboot (counter reset below previous reading) or first initialization
            if (baseline < 0L || (lastBootCounter >= 0L && totalSinceBoot < lastBootCounter)) {
                baseline = totalSinceBoot - carriedSteps
                if (baseline < 0L) baseline = 0L
            }

            val computedDaily = (totalSinceBoot - baseline).coerceAtLeast(carriedSteps)
            prefs.edit()
                .putLong("last_boot_counter", totalSinceBoot)
                .putLong("daily_baseline", baseline)
                .putLong("daily_hardware_steps", computedDaily)
                .apply()

            _state.value = _state.value.copy(
                dailyHardwareSteps = computedDaily,
                sensorBaseline = baseline
            )
        } else if (event.sensor.type == Sensor.TYPE_STEP_DETECTOR && stepCounterSensor == null) {
            val updated = _state.value.dailyHardwareSteps + 1L
            prefs.edit().putLong("daily_hardware_steps", updated).apply()
            _state.value = _state.value.copy(dailyHardwareSteps = updated)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
