package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AppRelease
import com.example.data.model.PlayUpdateUiState
import com.example.data.model.UpdateCenterUiState
import com.example.data.model.UpdateCheckStatus
import com.example.data.model.UpdateSeverity
import com.example.ui.theme.LocalFitTrackColors
import java.util.Locale

@Composable
fun ReleaseSeverityBadge(
    severity: UpdateSeverity,
    modifier: Modifier = Modifier
) {
    val ext = LocalFitTrackColors.current
    val (label, tint) = when (severity) {
        UpdateSeverity.CRITICAL -> "CRITICAL" to MaterialTheme.colorScheme.error
        UpdateSeverity.RECOMMENDED -> "RECOMMENDED" to ext.warning
        UpdateSeverity.OPTIONAL -> "OPTIONAL" to MaterialTheme.colorScheme.secondary
        UpdateSeverity.NONE -> "CURRENT" to ext.success
    }
    StatusBadge(text = label, tint = tint, modifier = modifier)
}

@Composable
fun ReleaseSeverityBadge(
    severityRaw: String,
    modifier: Modifier = Modifier
) {
    val parsed = when (severityRaw.lowercase(Locale.US)) {
        "critical" -> UpdateSeverity.CRITICAL
        "recommended" -> UpdateSeverity.RECOMMENDED
        "optional" -> UpdateSeverity.OPTIONAL
        else -> UpdateSeverity.OPTIONAL
    }
    ReleaseSeverityBadge(severity = parsed, modifier = modifier)
}

@Composable
fun UpdateAvailabilityBanner(
    uiState: UpdateCenterUiState,
    onOpenUpdateCenter: () -> Unit,
    onOpenWhatsNew: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalFitTrackColors.current
    val playDownloaded = uiState.playUpdateState is PlayUpdateUiState.DownloadedReadyToInstall
    val latest = uiState.latestRelease
    val hasUpdate = uiState.isUpdateAvailable && latest != null
    val hasUnreadCurrent = !hasUpdate && uiState.unreadReleaseCount > 0 && latest != null

    if (!playDownloaded && !hasUpdate && !hasUnreadCurrent) return

    val isCritical = uiState.status == UpdateCheckStatus.CRITICAL_UPDATE_REQUIRED ||
        uiState.severity == UpdateSeverity.CRITICAL
    val accent = when {
        isCritical -> MaterialTheme.colorScheme.error
        playDownloaded || hasUpdate -> MaterialTheme.colorScheme.secondary
        else -> ext.success
    }
    val icon = when {
        isCritical -> Icons.Filled.Warning
        playDownloaded || hasUpdate -> Icons.Filled.SystemUpdate
        else -> Icons.Filled.AutoAwesome
    }
    val title = when {
        playDownloaded -> "FitTrack update ready to install"
        isCritical && latest != null -> "Critical Update Required (v${latest.versionName})"
        hasUpdate -> "FitTrack v${latest?.versionName.orEmpty()} Available"
        latest != null -> "What's New in v${latest.versionName}"
        else -> "App Update"
    }
    val subtitle = when {
        playDownloaded -> "Restart FitTrack in Update Center to finish installing."
        latest != null -> latest.localizedFor().summary.ifBlank { latest.localizedFor().title }
        else -> "Tap to inspect release details."
    }

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("home_update_banner"),
        onClick = { if (hasUpdate || playDownloaded) onOpenUpdateCenter() else onOpenWhatsNew() },
        accentBorder = accent.copy(alpha = 0.6f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = accent,
                modifier = Modifier.size(24.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            StatusBadge(
                text = when {
                    playDownloaded -> "RESTART"
                    isCritical -> "REQUIRED"
                    hasUpdate -> "UPDATE"
                    else -> "NEW"
                },
                tint = accent
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReleaseChangelogCard(
    release: AppRelease,
    isLatest: Boolean,
    isInstalled: Boolean,
    isRead: Boolean,
    onMarkAsRead: (() -> Unit)? = null,
    locale: Locale = Locale.getDefault(),
    modifier: Modifier = Modifier
) {
    val ext = LocalFitTrackColors.current
    val content = release.localizedFor(locale)
    val accentBorder = when {
        release.severity.equals("critical", ignoreCase = true) -> MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
        isLatest -> MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
        else -> null
    }

    val cardContent: @Composable () -> Unit = {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "v${release.versionName} (build ${release.versionCode})",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Released ${release.releaseDate}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isRead) {
                StatusBadge(text = "READ", tint = ext.success)
            } else {
                StatusBadge(text = "UNREAD", tint = MaterialTheme.colorScheme.primary)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (isLatest) {
                StatusBadge(text = "LATEST", tint = MaterialTheme.colorScheme.secondary)
            }
            if (isInstalled) {
                StatusBadge(text = "INSTALLED", tint = ext.success)
            }
            ReleaseSeverityBadge(severityRaw = release.severity)
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = content.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (content.summary.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = content.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (content.newFeatures.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            ChangelogCategoryBlock(
                title = "New Features",
                icon = Icons.Filled.NewReleases,
                tint = MaterialTheme.colorScheme.primary,
                items = content.newFeatures
            )
        }

        if (content.improvements.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            ChangelogCategoryBlock(
                title = "Improvements",
                icon = Icons.AutoMirrored.Filled.TrendingUp,
                tint = MaterialTheme.colorScheme.secondary,
                items = content.improvements
            )
        }

        if (content.bugFixes.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            ChangelogCategoryBlock(
                title = "Bug Fixes",
                icon = Icons.Filled.BugReport,
                tint = ext.success,
                items = content.bugFixes
            )
        }

        if (content.securityChanges.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            ChangelogCategoryBlock(
                title = "Security Changes",
                icon = Icons.Filled.Security,
                tint = ext.warning,
                items = content.securityChanges
            )
        }

        if (release.minSupportedVersionCode > 0L) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Minimum supported version: ${release.minSupportedVersionName.ifBlank { "build ${release.minSupportedVersionCode}" }}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (!isRead && onMarkAsRead != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = onMarkAsRead,
                    modifier = Modifier.testTag("mark_read_${release.versionCode}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Mark as Read")
                }
            }
        }
    }

    if (isLatest) {
        GlassCard(
            modifier = modifier.fillMaxWidth(),
            accentBorder = accentBorder
        ) {
            cardContent()
        }
    } else {
        FitTrackCard(
            modifier = modifier.fillMaxWidth(),
            accentBorder = accentBorder
        ) {
            cardContent()
        }
    }
}

@Composable
private fun ChangelogCategoryBlock(
    title: String,
    icon: ImageVector,
    tint: Color,
    items: List<String>
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = tint,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = tint
            )
        }
        items.forEach { entry ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "•",
                    style = MaterialTheme.typography.bodyMedium,
                    color = tint
                )
                Text(
                    text = entry,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
