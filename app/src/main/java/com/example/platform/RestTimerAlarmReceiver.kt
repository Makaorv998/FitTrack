package com.example.platform

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R

class RestTimerAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val exerciseName = intent.getStringExtra(EXTRA_EXERCISE_NAME).orEmpty().ifBlank { "Next Set" }
        val playSound = intent.getBooleanExtra(EXTRA_SOUND_ENABLED, true)
        val vibrate = intent.getBooleanExtra(EXTRA_VIBRATION_ENABLED, true)

        if (vibrate) {
            triggerHapticAlert(context)
        }
        if (playSound) {
            runCatching {
                val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                RingtoneManager.getRingtone(context, uri)?.play()
            }
        }
        postRestDoneNotification(context, exerciseName)
    }

    companion object {
        const val CHANNEL_ID = "fittrack_rest_timer"
        const val NOTIFICATION_ID = 4109
        const val EXTRA_EXERCISE_NAME = "extra_exercise_name"
        const val EXTRA_SOUND_ENABLED = "extra_sound_enabled"
        const val EXTRA_VIBRATION_ENABLED = "extra_vibration_enabled"

        fun triggerHapticAlert(context: Context) {
            runCatching {
                val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                    vm?.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                }
                if (vibrator?.hasVibrator() == true) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 220, 120, 220), -1))
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(longArrayOf(0, 220, 120, 220), -1)
                    }
                }
            }
        }

        fun postRestDoneNotification(context: Context, exerciseName: String) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Workout Rest Timer",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Alerts you when your rest interval between workout sets finishes."
                    enableVibration(true)
                }
                nm.createNotificationChannel(channel)
            }

            if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return

            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingOpen = PendingIntent.getActivity(
                context,
                0,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Rest Complete — Back to Work")
                .setContentText("Ready for your next set of $exerciseName")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .setContentIntent(pendingOpen)
                .build()

            runCatching { nm.notify(NOTIFICATION_ID, notification) }
        }

        fun cancelNotification(context: Context) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            nm.cancel(NOTIFICATION_ID)
        }
    }
}
