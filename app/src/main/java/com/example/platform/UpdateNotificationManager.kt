package com.example.platform

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.AppRelease
import com.example.data.model.UpdateSeverity

enum class NotificationPostResult {
    SENT,
    PERMISSION_DENIED,
    DISABLED_CHANNEL_OR_OS
}

class UpdateNotificationManager(
    private val context: Context,
    private val appInfoProvider: InstalledAppInfoProvider = AndroidInstalledAppInfoProvider(context)
) {

    fun ensureNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        val optionalChannel = NotificationChannel(
            CHANNEL_OPTIONAL_UPDATES,
            context.getString(R.string.update_channel_optional_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.update_channel_optional_desc)
        }

        val criticalChannel = NotificationChannel(
            CHANNEL_CRITICAL_UPDATES,
            context.getString(R.string.update_channel_critical_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.update_channel_critical_desc)
            enableVibration(true)
        }

        nm.createNotificationChannel(optionalChannel)
        nm.createNotificationChannel(criticalChannel)
    }

    fun postUpdateNotification(
        release: AppRelease,
        severity: UpdateSeverity
    ): NotificationPostResult {
        ensureNotificationChannels()
        if (!appInfoProvider.hasNotificationPermission()) {
            return NotificationPostResult.PERMISSION_DENIED
        }
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return NotificationPostResult.DISABLED_CHANNEL_OR_OS

        val isCritical = severity == UpdateSeverity.CRITICAL
        val channelId = if (isCritical) CHANNEL_CRITICAL_UPDATES else CHANNEL_OPTIONAL_UPDATES

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = nm.getNotificationChannel(channelId)
            if (channel != null && channel.importance == NotificationManager.IMPORTANCE_NONE) {
                return NotificationPostResult.DISABLED_CHANNEL_OR_OS
            }
        }

        val localized = release.localizedFor()
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_UPDATE_NAV_TARGET, TARGET_UPDATE_CENTER)
            putExtra(EXTRA_RELEASE_VERSION_CODE, release.versionCode)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            if (isCritical) REQUEST_CODE_CRITICAL else REQUEST_CODE_OPTIONAL,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (isCritical) {
            "Critical FitTrack Update Required (v${release.versionName})"
        } else {
            "FitTrack v${release.versionName} Available"
        }
        val body = localized.summary.ifBlank { localized.title }.ifBlank {
            "Open Update Center to review release notes and install the update."
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(if (isCritical) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(if (isCritical) NotificationCompat.CATEGORY_RECOMMENDATION else NotificationCompat.CATEGORY_STATUS)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationId = if (isCritical) NOTIFICATION_ID_CRITICAL else NOTIFICATION_ID_OPTIONAL
        return runCatching {
            nm.notify(notificationId, notification)
            NotificationPostResult.SENT
        }.getOrDefault(NotificationPostResult.DISABLED_CHANNEL_OR_OS)
    }

    companion object {
        const val CHANNEL_OPTIONAL_UPDATES = "fittrack_updates_optional"
        const val CHANNEL_CRITICAL_UPDATES = "fittrack_updates_critical"
        const val NOTIFICATION_ID_OPTIONAL = 5101
        const val NOTIFICATION_ID_CRITICAL = 5102
        const val REQUEST_CODE_OPTIONAL = 5201
        const val REQUEST_CODE_CRITICAL = 5202
        const val EXTRA_UPDATE_NAV_TARGET = "extra_update_nav_target"
        const val EXTRA_RELEASE_VERSION_CODE = "extra_release_version_code"
        const val TARGET_UPDATE_CENTER = "update_center"
        const val TARGET_WHATS_NEW = "whats_new"
    }
}
