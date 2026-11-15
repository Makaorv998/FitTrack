package com.example.platform

import android.Manifest
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.pm.PackageInfoCompat
import com.example.BuildConfig
import com.example.data.model.DistributionChannel
import com.example.domain.AppUpdateEngine

data class InstalledAppSnapshot(
    val packageName: String,
    val versionCode: Long,
    val versionName: String,
    val installerPackageName: String,
    val distributionChannel: DistributionChannel,
    val signingCertSha256Digests: Set<String>
)

interface InstalledAppInfoProvider {
    fun getInstalledAppSnapshot(): InstalledAppSnapshot
    fun canRequestPackageInstalls(): Boolean
    fun hasNotificationPermission(): Boolean
}

class AndroidInstalledAppInfoProvider(private val context: Context) : InstalledAppInfoProvider {

    override fun getInstalledAppSnapshot(): InstalledAppSnapshot {
        val pm = context.packageManager
        val pkg = context.packageName

        val pkgInfo = runCatching {
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                PackageManager.GET_SIGNING_CERTIFICATES
            } else {
                @Suppress("DEPRECATION")
                PackageManager.GET_SIGNATURES
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(pkg, PackageManager.PackageInfoFlags.of(flags.toLong()))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(pkg, flags)
            }
        }.getOrNull()

        val versionCode = pkgInfo?.let { PackageInfoCompat.getLongVersionCode(it) }
            ?: BuildConfig.VERSION_CODE.toLong()
        val versionName = pkgInfo?.versionName?.takeIf { it.isNotBlank() }
            ?: BuildConfig.VERSION_NAME

        val installer = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                pm.getInstallSourceInfo(pkg).installingPackageName.orEmpty()
            } else {
                @Suppress("DEPRECATION")
                pm.getInstallerPackageName(pkg).orEmpty()
            }
        }.getOrDefault("")

        val channel = when (installer) {
            PLAY_STORE_PACKAGE -> DistributionChannel.PLAY_STORE
            else -> DistributionChannel.DIRECT_APK
        }

        val digests = pkgInfo?.let(::extractSignatureSha256Digests).orEmpty()

        return InstalledAppSnapshot(
            packageName = pkg,
            versionCode = versionCode,
            versionName = versionName,
            installerPackageName = installer,
            distributionChannel = channel,
            signingCertSha256Digests = digests
        )
    }

    override fun canRequestPackageInstalls(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            runCatching { context.packageManager.canRequestPackageInstalls() }.getOrDefault(false)
        } else {
            true
        }
    }

    override fun hasNotificationPermission(): Boolean {
        val enabledInSystem = NotificationManagerCompat.from(context).areNotificationsEnabled()
        if (!enabledInSystem) return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    companion object {
        const val PLAY_STORE_PACKAGE = "com.android.vending"

        fun extractSignatureSha256Digests(packageInfo: PackageInfo): Set<String> {
            val signatures: Array<out Signature>? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val signingInfo = packageInfo.signingInfo
                if (signingInfo == null) {
                    null
                } else if (signingInfo.hasMultipleSigners()) {
                    signingInfo.apkContentsSigners
                } else {
                    signingInfo.signingCertificateHistory
                }
            } else {
                @Suppress("DEPRECATION")
                packageInfo.signatures
            }
            return signatures
                ?.map { sig -> AppUpdateEngine.sha256Hex(sig.toByteArray()) }
                ?.toSet()
                .orEmpty()
        }
    }
}
