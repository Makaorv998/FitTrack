package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import kotlinx.serialization.Serializable
import java.util.Locale

@Serializable
@IgnoreExtraProperties
data class LocalizedReleaseContent(
    val title: String = "",
    val summary: String = "",
    val newFeatures: List<String> = emptyList(),
    val improvements: List<String> = emptyList(),
    val bugFixes: List<String> = emptyList(),
    val securityChanges: List<String> = emptyList()
)

@Serializable
@IgnoreExtraProperties
data class AppRelease(
    @DocumentId val id: String = "",
    val versionName: String = "",
    val versionCode: Long = 0L,
    val releaseDate: String = "",
    val title: String = "",
    val summary: String = "",
    val severity: String = "optional", // optional | recommended | critical
    val minSupportedVersionCode: Long = 0L,
    val minSupportedVersionName: String = "",
    val distributionChannel: String = "both", // play_store | apk | both
    val apkDownloadUrl: String = "",
    val apkSha256: String = "",
    val expectedSigningCertSha256: String = "",
    val playStoreUrl: String = "",
    val newFeatures: List<String> = emptyList(),
    val improvements: List<String> = emptyList(),
    val bugFixes: List<String> = emptyList(),
    val securityChanges: List<String> = emptyList(),
    val localizedEntries: Map<String, LocalizedReleaseContent> = emptyMap(),
    val isPublished: Boolean = true
) {
    fun localizedFor(locale: Locale = Locale.getDefault()): LocalizedReleaseContent {
        val lang = locale.language.lowercase(Locale.US)
        val tag = locale.toLanguageTag().lowercase(Locale.US)
        val matched = localizedEntries[tag]
            ?: localizedEntries[lang]
            ?: localizedEntries.entries.firstOrNull { it.key.equals(lang, ignoreCase = true) }?.value

        return if (matched != null) {
            LocalizedReleaseContent(
                title = matched.title.ifBlank { title },
                summary = matched.summary.ifBlank { summary },
                newFeatures = matched.newFeatures.ifEmpty { newFeatures },
                improvements = matched.improvements.ifEmpty { improvements },
                bugFixes = matched.bugFixes.ifEmpty { bugFixes },
                securityChanges = matched.securityChanges.ifEmpty { securityChanges }
            )
        } else {
            LocalizedReleaseContent(
                title = title,
                summary = summary,
                newFeatures = newFeatures,
                improvements = improvements,
                bugFixes = bugFixes,
                securityChanges = securityChanges
            )
        }
    }

    fun isValidMetadata(): Boolean {
        return versionName.isNotBlank() &&
            versionCode > 0L &&
            releaseDate.isNotBlank() &&
            title.isNotBlank() &&
            severity in setOf("optional", "recommended", "critical")
    }
}

@Serializable
@IgnoreExtraProperties
data class UpdatePolicyConfig(
    val checkIntervalMinutes: Long = 360L,
    val minSupportedVersionCode: Long = 0L,
    val latestVersionCode: Long = 0L,
    val trustedApkHosts: List<String> = emptyList(),
    val defaultDistributionChannel: String = "both",
    val playUpdatePriorityThreshold: Long = 4L
)

enum class UpdateSeverity {
    NONE,
    OPTIONAL,
    RECOMMENDED,
    CRITICAL
}

enum class DistributionChannel {
    PLAY_STORE,
    DIRECT_APK,
    UNKNOWN
}

enum class UpdateCheckStatus {
    IDLE,
    CHECKING,
    UP_TO_DATE,
    UPDATE_AVAILABLE,
    CRITICAL_UPDATE_REQUIRED,
    OFFLINE_CACHED,
    SERVER_UNAVAILABLE
}

@Serializable
data class UpdateNotificationPreferences(
    val optionalUpdatesEnabled: Boolean = true,
    val criticalUpdatesEnabled: Boolean = true,
    val autoCheckOnForeground: Boolean = true,
    val checkIntervalMinutes: Long = 360L
)

@Serializable
data class LocalUpdateState(
    val lastSeenVersionCode: Long = -1L,
    val lastSeenVersionName: String = "",
    val shownWhatsNewVersionCode: Long = -1L,
    val readVersionCodes: Set<Long> = emptySet(),
    val lastCheckEpochMs: Long = 0L,
    val lastSuccessfulCheckEpochMs: Long = 0L,
    val cachedReleases: List<AppRelease> = emptyList(),
    val cachedPolicy: UpdatePolicyConfig = UpdatePolicyConfig(),
    val lastNotifiedOptionalVersionCode: Long = -1L,
    val lastNotifiedCriticalVersionCode: Long = -1L,
    val notificationPreferences: UpdateNotificationPreferences = UpdateNotificationPreferences()
)

data class RemoteUpdateSnapshot(
    val releases: List<AppRelease>,
    val policy: UpdatePolicyConfig,
    val fetchedAtMs: Long = System.currentTimeMillis()
)

sealed interface PlayUpdateUiState {
    data object Idle : PlayUpdateUiState
    data object Checking : PlayUpdateUiState
    data object Unavailable : PlayUpdateUiState
    data class Available(
        val availableVersionCode: Int,
        val updatePriority: Int,
        val isImmediatePreferred: Boolean,
        val isFlexibleAllowed: Boolean,
        val isImmediateAllowed: Boolean
    ) : PlayUpdateUiState
    data class Downloading(val bytesDownloaded: Long, val totalBytes: Long) : PlayUpdateUiState
    data object DownloadedReadyToInstall : PlayUpdateUiState
    data object Installing : PlayUpdateUiState
    data object InstalledConfirmed : PlayUpdateUiState
    data class Failed(val message: String) : PlayUpdateUiState
}

sealed interface ApkInstallUiState {
    data object Idle : ApkInstallUiState
    data class Downloading(val progress: Float, val bytesRead: Long, val totalBytes: Long) : ApkInstallUiState
    data object Verifying : ApkInstallUiState
    data class NeedsUnknownSourcesPermission(val apkFilePath: String, val targetVersionName: String) : ApkInstallUiState
    data class VerifiedReady(val apkFilePath: String, val certSha256: String, val targetVersionName: String) : ApkInstallUiState
    data class InstallerLaunched(val targetVersionName: String) : ApkInstallUiState
    data class Failed(val message: String) : ApkInstallUiState
}

data class UpdateCenterUiState(
    val installedVersionCode: Long = 1L,
    val installedVersionName: String = "1.0",
    val distributionChannel: DistributionChannel = DistributionChannel.UNKNOWN,
    val installerPackageName: String = "",
    val status: UpdateCheckStatus = UpdateCheckStatus.IDLE,
    val severity: UpdateSeverity = UpdateSeverity.NONE,
    val latestRelease: AppRelease? = null,
    val releases: List<AppRelease> = emptyList(),
    val policy: UpdatePolicyConfig = UpdatePolicyConfig(),
    val localState: LocalUpdateState = LocalUpdateState(),
    val shouldAutoShowWhatsNew: Boolean = false,
    val pendingWhatsNewVersionCode: Long? = null,
    val lastErrorMessage: String? = null,
    val isOfflineOrCached: Boolean = false,
    val hasNotificationPermission: Boolean = true,
    val canRequestApkInstalls: Boolean = true,
    val playUpdateState: PlayUpdateUiState = PlayUpdateUiState.Idle,
    val apkInstallState: ApkInstallUiState = ApkInstallUiState.Idle
) {
    val isUpdateAvailable: Boolean
        get() = latestRelease != null &&
            (status == UpdateCheckStatus.UPDATE_AVAILABLE ||
                status == UpdateCheckStatus.CRITICAL_UPDATE_REQUIRED ||
                (isOfflineOrCached && severity != UpdateSeverity.NONE))

    val unreadReleaseCount: Int
        get() = releases.count { it.isPublished && it.versionCode !in localState.readVersionCodes }
}
