package com.example.platform

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.LatLngPoint
import com.example.domain.FitnessCalculator
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class TrackingStatus { IDLE, ACTIVE, PAUSED }

data class GpsTrackerState(
    val status: TrackingStatus = TrackingStatus.IDLE,
    val activityType: String = "running",
    val elapsedSeconds: Long = 0L,
    val distanceKm: Double = 0.0,
    val currentSpeedKmh: Double = 0.0,
    val avgPaceMinPerKm: Double = 0.0,
    val estimatedCalories: Double = 0.0,
    val routePoints: List<LatLngPoint> = emptyList(),
    val lastAccuracyMeters: Float? = null,
    val startedAtMs: Long = 0L,
    val weightKg: Double = 70.0
)

object GpsTrackerStore {
    private val _state = MutableStateFlow(GpsTrackerState())
    val state: StateFlow<GpsTrackerState> = _state.asStateFlow()

    internal fun update(transform: (GpsTrackerState) -> GpsTrackerState) {
        _state.value = transform(_state.value)
    }

    internal fun reset() {
        _state.value = GpsTrackerState()
    }
}

class LocationTrackingService : Service() {

    private lateinit var fusedClient: FusedLocationProviderClient
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var timerJob: Job? = null
    private var lastValidLocation: Location? = null

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val current = GpsTrackerStore.state.value
            if (current.status != TrackingStatus.ACTIVE) return

            for (loc in result.locations) {
                // Filter low-accuracy readings (> 35m)
                if (loc.hasAccuracy() && loc.accuracy > 35f) continue

                val prev = lastValidLocation
                if (prev != null) {
                    val deltaMeters = prev.distanceTo(loc)
                    val deltaSec = ((loc.time - prev.time) / 1000.0).coerceAtLeast(0.5)
                    val impliedSpeedMps = deltaMeters / deltaSec

                    // Reject duplicate stationary jitter (< 2m) or unrealistic GPS jumps (> 45 m/s)
                    if (deltaMeters < 2f || impliedSpeedMps > 45.0) continue

                    GpsTrackerStore.update { st ->
                        val newDistanceKm = st.distanceKm + (deltaMeters / 1000.0)
                        val durationMin = st.elapsedSeconds / 60.0
                        val pace = FitnessCalculator.calculateRunningPaceMinPerKm(durationMin, newDistanceKm)
                        val speedKmh = if (loc.hasSpeed()) (loc.speed * 3.6) else (impliedSpeedMps * 3.6)
                        val met = metForActivity(st.activityType)
                        val cals = FitnessCalculator.calculateMetCalories(met, st.weightKg, durationMin)
                        st.copy(
                            distanceKm = newDistanceKm,
                            currentSpeedKmh = speedKmh.coerceIn(0.0, 120.0),
                            avgPaceMinPerKm = pace,
                            estimatedCalories = cals,
                            lastAccuracyMeters = if (loc.hasAccuracy()) loc.accuracy else null,
                            routePoints = (st.routePoints + LatLngPoint(loc.latitude, loc.longitude, loc.time))
                        )
                    }
                } else {
                    GpsTrackerStore.update { st ->
                        st.copy(
                            lastAccuracyMeters = if (loc.hasAccuracy()) loc.accuracy else null,
                            routePoints = st.routePoints + LatLngPoint(loc.latitude, loc.longitude, loc.time)
                        )
                    }
                }
                lastValidLocation = loc
                updateNotification()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        fusedClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val type = intent.getStringExtra(EXTRA_ACTIVITY_TYPE) ?: "running"
                val weight = intent.getDoubleExtra(EXTRA_WEIGHT_KG, 70.0)
                startTracking(type, weight)
            }
            ACTION_PAUSE -> pauseTracking()
            ACTION_RESUME -> resumeTracking()
            ACTION_STOP -> stopTracking()
        }
        return START_NOT_STICKY
    }

    private fun startTracking(activityType: String, weightKg: Double) {
        lastValidLocation = null
        GpsTrackerStore.update {
            GpsTrackerState(
                status = TrackingStatus.ACTIVE,
                activityType = activityType,
                startedAtMs = System.currentTimeMillis(),
                weightKg = weightKg
            )
        }
        startForegroundWithNotification()
        requestLocationUpdates()
        startElapsedTimer()
    }

    private fun pauseTracking() {
        GpsTrackerStore.update { it.copy(status = TrackingStatus.PAUSED, currentSpeedKmh = 0.0) }
        timerJob?.cancel()
        fusedClient.removeLocationUpdates(locationCallback)
        updateNotification()
    }

    private fun resumeTracking() {
        GpsTrackerStore.update { it.copy(status = TrackingStatus.ACTIVE) }
        requestLocationUpdates()
        startElapsedTimer()
        updateNotification()
    }

    private fun stopTracking() {
        timerJob?.cancel()
        fusedClient.removeLocationUpdates(locationCallback)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    @SuppressLint("MissingPermission")
    private fun requestLocationUpdates() {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000L)
            .setMinUpdateIntervalMillis(1500L)
            .setMinUpdateDistanceMeters(2f)
            .build()
        try {
            fusedClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
        } catch (_: SecurityException) {
            // Permission revoked externally
        }
    }

    private fun startElapsedTimer() {
        timerJob?.cancel()
        timerJob = serviceScope.launch {
            while (isActive) {
                delay(1000L)
                GpsTrackerStore.update { st ->
                    if (st.status != TrackingStatus.ACTIVE) st
                    else {
                        val secs = st.elapsedSeconds + 1L
                        val mins = secs / 60.0
                        val cals = FitnessCalculator.calculateMetCalories(metForActivity(st.activityType), st.weightKg, mins)
                        st.copy(
                            elapsedSeconds = secs,
                            estimatedCalories = cals,
                            avgPaceMinPerKm = FitnessCalculator.calculateRunningPaceMinPerKm(mins, st.distanceKm)
                        )
                    }
                }
                updateNotification()
            }
        }
    }

    private fun startForegroundWithNotification() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun buildNotification(): Notification {
        val st = GpsTrackerStore.state.value
        val openIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val statusLabel = if (st.status == TrackingStatus.PAUSED) "Paused" else "Active"
        val title = "FitTrack ${st.activityType.replaceFirstChar { it.uppercase() }} ($statusLabel)"
        val text = "${String.format("%.2f", st.distanceKm)} km • ${FitnessCalculator.formatDuration(st.elapsedSeconds)} • ${st.estimatedCalories.toInt()} kcal"
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setOngoing(true)
            .setContentIntent(openIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "GPS Activity Tracking",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        timerJob?.cancel()
        serviceScope.cancel()
        fusedClient.removeLocationUpdates(locationCallback)
    }

    companion object {
        const val CHANNEL_ID = "fittrack_gps_channel"
        const val NOTIFICATION_ID = 4102
        const val ACTION_START = "com.example.action.GPS_START"
        const val ACTION_PAUSE = "com.example.action.GPS_PAUSE"
        const val ACTION_RESUME = "com.example.action.GPS_RESUME"
        const val ACTION_STOP = "com.example.action.GPS_STOP"
        const val EXTRA_ACTIVITY_TYPE = "extra_activity_type"
        const val EXTRA_WEIGHT_KG = "extra_weight_kg"

        fun metForActivity(type: String): Double = when (type.lowercase()) {
            "walking" -> 3.8
            "cycling" -> 7.5
            else -> 9.8
        }

        fun sendCommand(context: Context, action: String, activityType: String = "running", weightKg: Double = 70.0) {
            val intent = Intent(context, LocationTrackingService::class.java).apply {
                this.action = action
                putExtra(EXTRA_ACTIVITY_TYPE, activityType)
                putExtra(EXTRA_WEIGHT_KG, weightKg)
            }
            if (action == ACTION_START && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun discardSession(context: Context) {
            sendCommand(context, ACTION_STOP)
            GpsTrackerStore.reset()
        }
    }
}
