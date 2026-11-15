package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.ApkInstallUiState
import com.example.data.model.AppRelease
import com.example.data.model.DistributionChannel
import com.example.data.model.PlayUpdateUiState
import com.example.data.model.UpdateCenterUiState
import com.example.data.model.UpdateCheckStatus
import com.example.data.model.UpdateNotificationPreferences
import com.example.data.model.UpdateSeverity
import com.example.ui.components.AuroraAmbientBackground
import com.example.ui.components.DisclaimerBanner
import com.example.ui.components.ErrorBannerCard
import com.example.ui.components.FitTrackCard
import com.example.ui.components.GlassCard
import com.example.ui.components.GradientButton
import com.example.ui.components.ReleaseChangelogCard
import com.example.ui.components.ReleaseSeverityBadge
import com.example.ui.components.SecondaryButton
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusBadge
import com.example.ui.components.SuccessStateCard
import com.example.ui.theme.LocalFitTrackColors
import java.net.URI
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun UpdateCenterScreen(
    uiState: UpdateCenterUiState,
    onCheckForUpdates: () -> Unit,
    onStartPlayUpdate: (forceImmediate: Boolean) -> Unit,
    onCompletePlayUpdate: () -> Unit,
    onDownloadApkUpdate: (AppRelease) -> Unit,
    onOpenUnknownSourcesSettings: () -> Unit,
    onLaunchApkInstaller: (apkFilePath: String, targetVersionName: String) -> Unit,
    onOpenWhatsNew: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onUpdateNotificationPrefs: (UpdateNotificationPreferences) -> Unit,
    onBack: () -> Unit,
    onOpenPlayStoreUrl: (String) -> Unit = {}
) {
    val ext = LocalFitTrackColors.current
    val bottomClearance = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 32.dp
    val channelLabel = when (uiState.distributionChannel) {
        DistributionChannel.PLAY_STORE -> "Google Play"
        DistributionChannel.DIRECT_APK -> "Direct APK"
        DistributionChannel.UNKNOWN -> "Local / Side-loaded"
    }
    val formattedLastCheck = remember(uiState.localState.lastSuccessfulCheckEpochMs) {
        val ts = uiState.localState.lastSuccessfulCheckEpochMs
        if (ts <= 0L) "Never checked"
        else SimpleDateFormat("MMM d, yyyy • HH:mm", Locale.US).format(Date(ts))
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.88f)
                ),
                title = { Text("Update Center", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("update_center_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        AuroraAmbientBackground {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = innerPadding.calculateTopPadding())
                    .testTag("update_center_list"),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = bottomClearance),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Installed version & check card
                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        accentBorder = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Installed Version",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "v${uiState.installedVersionName} (build ${uiState.installedVersionCode})",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Last checked: $formattedLastCheck",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            StatusBadge(text = channelLabel, tint = MaterialTheme.colorScheme.secondary)
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        GradientButton(
                            text = if (uiState.status == UpdateCheckStatus.CHECKING) {
                                "Checking for Updates..."
                            } else {
                                "Check for Updates"
                            },
                            icon = Icons.Filled.Refresh,
                            loading = uiState.status == UpdateCheckStatus.CHECKING,
                            onClick = onCheckForUpdates,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("check_updates_button")
                        )
                    }
                }

                // 2. Status Banner (Offline / Server Error / Up to Date / Critical)
                item {
                    when (uiState.status) {
                        UpdateCheckStatus.OFFLINE_CACHED -> {
                            DisclaimerBanner(
                                text = "Offline or update service unavailable. Displaying cached release metadata.",
                                isWarning = true,
                                modifier = Modifier.testTag("offline_cached_banner")
                            )
                        }
                        UpdateCheckStatus.SERVER_UNAVAILABLE -> {
                            ErrorBannerCard(
                                message = uiState.lastErrorMessage
                                    ?: "Unable to reach FitTrack update service. Check your connection and retry.",
                                onRetry = onCheckForUpdates,
                                modifier = Modifier.testTag("server_unavailable_banner")
                            )
                        }
                        UpdateCheckStatus.CRITICAL_UPDATE_REQUIRED -> {
                            val latest = uiState.latestRelease
                            DisclaimerBanner(
                                text = if (latest != null) {
                                    "Critical Update Required: FitTrack v${latest.versionName} includes mandatory security or compatibility updates. Please update now."
                                } else {
                                    "Critical Update Required: This version is below the minimum supported version (${uiState.policy.minSupportedVersionCode})."
                                },
                                isWarning = true,
                                modifier = Modifier.testTag("critical_update_banner")
                            )
                        }
                        UpdateCheckStatus.UP_TO_DATE -> {
                            SuccessStateCard(
                                title = "FitTrack is up to date",
                                subtitle = "You are running the latest verified release.",
                                modifier = Modifier.testTag("up_to_date_card")
                            )
                        }
                        else -> Unit
                    }
                }

                // 3. Available Update Distribution Workflow (Play Store + Direct APK)
                val latestRelease = uiState.latestRelease
                val showPlayCard = uiState.playUpdateState !is PlayUpdateUiState.Idle &&
                    uiState.playUpdateState !is PlayUpdateUiState.Unavailable
                val canOfferApk = latestRelease != null &&
                    uiState.isUpdateAvailable &&
                    latestRelease.apkDownloadUrl.isNotBlank() &&
                    latestRelease.distributionChannel in setOf("apk", "both") &&
                    uiState.distributionChannel != DistributionChannel.PLAY_STORE

                if (uiState.isUpdateAvailable && latestRelease != null) {
                    item {
                        FitTrackCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.SystemUpdate,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary
                                    )
                                    Text(
                                        text = "Update Available: v${latestRelease.versionName}",
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                }
                                ReleaseSeverityBadge(severity = uiState.severity)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            val localized = latestRelease.localizedFor()
                            Text(
                                text = localized.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (localized.summary.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = localized.summary,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Google Play In-App Update Section (with Store URL fallback if needed)
                            if (uiState.distributionChannel == DistributionChannel.PLAY_STORE || showPlayCard) {
                                Spacer(modifier = Modifier.height(14.dp))
                                PlayUpdateActionBlock(
                                    playState = uiState.playUpdateState,
                                    isCritical = uiState.severity == UpdateSeverity.CRITICAL,
                                    onStartPlayUpdate = onStartPlayUpdate,
                                    onCompletePlayUpdate = onCompletePlayUpdate
                                )
                                if (!showPlayCard && latestRelease.playStoreUrl.startsWith("https://")) {
                                    SecondaryButton(
                                        text = "Open in Google Play",
                                        icon = Icons.Filled.SystemUpdate,
                                        onClick = { onOpenPlayStoreUrl(latestRelease.playStoreUrl) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("open_play_store_fallback_button")
                                    )
                                }
                            }

                            // Direct APK Verified Install Workflow
                            if (canOfferApk || uiState.apkInstallState !is ApkInstallUiState.Idle) {
                                Spacer(modifier = Modifier.height(14.dp))
                                DirectApkActionBlock(
                                    release = latestRelease,
                                    apkState = uiState.apkInstallState,
                                    canRequestInstalls = uiState.canRequestApkInstalls,
                                    onDownloadApk = { onDownloadApkUpdate(latestRelease) },
                                    onOpenUnknownSourcesSettings = onOpenUnknownSourcesSettings,
                                    onLaunchInstaller = onLaunchApkInstaller
                                )
                            }
                        }
                    }

                    item {
                        SectionHeader(title = "Release Notes Preview")
                        Spacer(modifier = Modifier.height(8.dp))
                        ReleaseChangelogCard(
                            release = latestRelease,
                            isLatest = true,
                            isInstalled = false,
                            isRead = latestRelease.versionCode in uiState.localState.readVersionCodes,
                            modifier = Modifier.testTag("update_center_release_preview")
                        )
                    }
                } else if (showPlayCard || uiState.apkInstallState !is ApkInstallUiState.Idle) {
                    item {
                        FitTrackCard(modifier = Modifier.fillMaxWidth()) {
                            if (showPlayCard) {
                                PlayUpdateActionBlock(
                                    playState = uiState.playUpdateState,
                                    isCritical = uiState.severity == UpdateSeverity.CRITICAL,
                                    onStartPlayUpdate = onStartPlayUpdate,
                                    onCompletePlayUpdate = onCompletePlayUpdate
                                )
                            }
                            if (latestRelease != null && uiState.apkInstallState !is ApkInstallUiState.Idle) {
                                DirectApkActionBlock(
                                    release = latestRelease,
                                    apkState = uiState.apkInstallState,
                                    canRequestInstalls = uiState.canRequestApkInstalls,
                                    onDownloadApk = { onDownloadApkUpdate(latestRelease) },
                                    onOpenUnknownSourcesSettings = onOpenUnknownSourcesSettings,
                                    onLaunchInstaller = onLaunchApkInstaller
                                )
                            }
                        }
                    }
                }

                // 4. What's New & Version History Link
                item {
                    FitTrackCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Changelog & Version History", style = MaterialTheme.typography.titleLarge)
                                Text(
                                    text = if (uiState.unreadReleaseCount > 0) {
                                        "${uiState.unreadReleaseCount} unread release note(s)"
                                    } else {
                                        "Browse features, improvements & fixes across versions"
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (uiState.unreadReleaseCount > 0) {
                                StatusBadge(
                                    text = "${uiState.unreadReleaseCount} NEW",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        SecondaryButton(
                            text = "Open What's New",
                            icon = Icons.Filled.History,
                            onClick = onOpenWhatsNew,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("open_whats_new_button")
                        )
                    }
                }

                // 5. Notification & Background Check Preferences
                item {
                    val prefs = uiState.localState.notificationPreferences
                    FitTrackCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Filled.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text("Update Notifications & Checks", style = MaterialTheme.typography.titleLarge)
                        }
                        Spacer(modifier = Modifier.height(10.dp))

                        if (!uiState.hasNotificationPermission) {
                            DisclaimerBanner(
                                text = "Android notification permission is denied. Grant permission to receive optional and critical update alerts.",
                                isWarning = true,
                                modifier = Modifier.testTag("notif_permission_denied_banner")
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            SecondaryButton(
                                text = "Grant Notification Permission",
                                icon = Icons.Filled.Notifications,
                                onClick = onRequestNotificationPermission,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("grant_notif_permission_button")
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        PreferenceSwitchRow(
                            title = "Optional & Feature Updates",
                            subtitle = "Notify once when new versions are released",
                            checked = prefs.optionalUpdatesEnabled,
                            testTag = "switch_optional_updates",
                            onCheckedChange = { enabled ->
                                onUpdateNotificationPrefs(prefs.copy(optionalUpdatesEnabled = enabled))
                            }
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        PreferenceSwitchRow(
                            title = "Critical Security Alerts",
                            subtitle = "High-priority alerts for mandatory updates",
                            checked = prefs.criticalUpdatesEnabled,
                            testTag = "switch_critical_updates",
                            onCheckedChange = { enabled ->
                                onUpdateNotificationPrefs(prefs.copy(criticalUpdatesEnabled = enabled))
                            }
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        PreferenceSwitchRow(
                            title = "Check on Launch & Foreground",
                            subtitle = "Automatically verify updates within check interval",
                            checked = prefs.autoCheckOnForeground,
                            testTag = "switch_foreground_checks",
                            onCheckedChange = { enabled ->
                                onUpdateNotificationPrefs(prefs.copy(autoCheckOnForeground = enabled))
                            }
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Check Interval", style = MaterialTheme.typography.labelLarge)
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(
                                60L to "1 hr",
                                360L to "6 hr",
                                720L to "12 hr",
                                1440L to "24 hr"
                            ).forEach { (minutes, label) ->
                                FilterChip(
                                    selected = prefs.checkIntervalMinutes == minutes,
                                    onClick = {
                                        onUpdateNotificationPrefs(prefs.copy(checkIntervalMinutes = minutes))
                                    },
                                    label = { Text(label) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayUpdateActionBlock(
    playState: PlayUpdateUiState,
    isCritical: Boolean,
    onStartPlayUpdate: (Boolean) -> Unit,
    onCompletePlayUpdate: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Google Play In-App Update", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
        when (playState) {
            is PlayUpdateUiState.Available -> {
                if (isCritical || playState.isImmediatePreferred) {
                    if (playState.isImmediateAllowed) {
                        GradientButton(
                            text = "Update Now (Immediate)",
                            icon = Icons.Filled.SystemUpdate,
                            onClick = { onStartPlayUpdate(true) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("play_immediate_update_button")
                        )
                    } else if (playState.isFlexibleAllowed) {
                        GradientButton(
                            text = "Download Update in Background",
                            icon = Icons.Filled.Download,
                            onClick = { onStartPlayUpdate(false) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("play_flexible_update_button")
                        )
                    }
                } else {
                    if (playState.isFlexibleAllowed) {
                        GradientButton(
                            text = "Download Flexible Update",
                            icon = Icons.Filled.Download,
                            onClick = { onStartPlayUpdate(false) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("play_flexible_update_button")
                        )
                    }
                    if (playState.isImmediateAllowed) {
                        SecondaryButton(
                            text = "Immediate Update & Restart",
                            icon = Icons.Filled.SystemUpdate,
                            onClick = { onStartPlayUpdate(true) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("play_immediate_update_button")
                        )
                    }
                }
            }
            is PlayUpdateUiState.Downloading -> {
                val ratio = if (playState.totalBytes > 0L) {
                    (playState.bytesDownloaded.toFloat() / playState.totalBytes.toFloat()).coerceIn(0f, 1f)
                } else 0f
                Text("Downloading from Google Play (${(ratio * 100).toInt()}%)", style = MaterialTheme.typography.bodyMedium)
                LinearProgressIndicator(progress = { ratio }, modifier = Modifier.fillMaxWidth())
            }
            PlayUpdateUiState.DownloadedReadyToInstall -> {
                SuccessStateCard(
                    title = "Play Update Downloaded",
                    subtitle = "Restart FitTrack to apply the downloaded update."
                )
                GradientButton(
                    text = "Restart to Complete Update",
                    icon = Icons.Filled.RestartAlt,
                    onClick = onCompletePlayUpdate,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("play_complete_update_button")
                )
            }
            PlayUpdateUiState.Installing -> {
                Text("Installing Google Play update...", style = MaterialTheme.typography.bodyMedium)
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            PlayUpdateUiState.InstalledConfirmed -> {
                SuccessStateCard(
                    title = "Update Installed",
                    subtitle = "Google Play confirmed update installation."
                )
            }
            is PlayUpdateUiState.Failed -> {
                ErrorBannerCard(message = playState.message)
            }
            PlayUpdateUiState.Checking -> {
                Text("Checking Google Play availability...", style = MaterialTheme.typography.bodyMedium)
            }
            PlayUpdateUiState.Idle, PlayUpdateUiState.Unavailable -> Unit
        }
    }
}

@Composable
private fun DirectApkActionBlock(
    release: AppRelease,
    apkState: ApkInstallUiState,
    canRequestInstalls: Boolean,
    onDownloadApk: () -> Unit,
    onOpenUnknownSourcesSettings: () -> Unit,
    onLaunchInstaller: (String, String) -> Unit
) {
    val hostLabel = remember(release.apkDownloadUrl) {
        runCatching { URI(release.apkDownloadUrl).host }.getOrNull().orEmpty()
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                Icons.Filled.VerifiedUser,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = if (hostLabel.isNotBlank()) "Verified APK Source ($hostLabel)" else "Verified Direct APK",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary
            )
        }

        when (apkState) {
            ApkInstallUiState.Idle -> {
                if (!canRequestInstalls) {
                    DisclaimerBanner(
                        text = "Allow installing apps from this source in Android Settings before launching the verified APK installer.",
                        isWarning = false
                    )
                }
                GradientButton(
                    text = "Download & Verify v${release.versionName} APK",
                    icon = Icons.Filled.Download,
                    onClick = onDownloadApk,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("apk_download_button")
                )
            }
            is ApkInstallUiState.Downloading -> {
                val pct = (apkState.progress * 100f).toInt().coerceIn(0, 100)
                Text("Downloading verified APK ($pct%)", style = MaterialTheme.typography.bodyMedium)
                LinearProgressIndicator(progress = { apkState.progress }, modifier = Modifier.fillMaxWidth())
            }
            ApkInstallUiState.Verifying -> {
                Text("Verifying SHA-256 digest and FitTrack signing certificate...", style = MaterialTheme.typography.bodyMedium)
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            is ApkInstallUiState.NeedsUnknownSourcesPermission -> {
                DisclaimerBanner(
                    text = "APK checksum & signature verified! Enable 'Allow from this source' in Android Settings to continue installation.",
                    isWarning = true,
                    modifier = Modifier.testTag("unknown_sources_banner")
                )
                GradientButton(
                    text = "Allow Unknown Sources in Settings",
                    icon = Icons.Filled.Security,
                    onClick = onOpenUnknownSourcesSettings,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("allow_unknown_sources_button")
                )
            }
            is ApkInstallUiState.VerifiedReady -> {
                SuccessStateCard(
                    title = "APK Signature & SHA-256 Verified",
                    subtitle = "Ready for Android system installer (v${apkState.targetVersionName}).",
                    modifier = Modifier.testTag("apk_verified_card")
                )
                GradientButton(
                    text = "Install Verified APK",
                    icon = Icons.Filled.InstallMobile,
                    onClick = { onLaunchInstaller(apkState.apkFilePath, apkState.targetVersionName) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("apk_install_button")
                )
            }
            is ApkInstallUiState.InstallerLaunched -> {
                DisclaimerBanner(
                    text = "Android installer opened for v${apkState.targetVersionName}. Confirm installation in the system prompt to complete the upgrade.",
                    isWarning = false,
                    modifier = Modifier.testTag("apk_installer_launched_banner")
                )
            }
            is ApkInstallUiState.Failed -> {
                ErrorBannerCard(
                    message = apkState.message,
                    onRetry = onDownloadApk,
                    modifier = Modifier.testTag("apk_failed_banner")
                )
            }
        }
    }
}

@Composable
private fun PreferenceSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    testTag: String,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag)
        )
    }
}
