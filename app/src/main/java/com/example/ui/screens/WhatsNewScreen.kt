package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.UpdateCenterUiState
import com.example.ui.components.AuroraAmbientBackground
import com.example.ui.components.DisclaimerBanner
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.GradientButton
import com.example.ui.components.ReleaseChangelogCard
import com.example.ui.components.SecondaryButton
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusBadge
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhatsNewScreen(
    uiState: UpdateCenterUiState,
    highlightVersionCode: Long? = null,
    onMarkReleaseRead: (Long) -> Unit,
    onMarkAllRead: () -> Unit,
    onOpenUpdateCenter: () -> Unit,
    onCheckForUpdates: () -> Unit,
    onBack: () -> Unit,
    locale: Locale = Locale.getDefault()
) {
    val publishedReleases = uiState.releases
        .filter { it.isPublished }
        .sortedByDescending { it.versionCode }

    val targetRelease = highlightVersionCode?.let { code ->
        publishedReleases.firstOrNull { it.versionCode == code }
    } ?: publishedReleases.firstOrNull()

    val previousReleases = publishedReleases.filter { it.versionCode != targetRelease?.versionCode }
    val bottomClearance = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 32.dp

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.88f)
                ),
                title = {
                    Column {
                        Text("What's New", style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = "Installed v${uiState.installedVersionName} (build ${uiState.installedVersionCode})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("whats_new_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (uiState.unreadReleaseCount > 0) {
                        TextButton(
                            onClick = onMarkAllRead,
                            modifier = Modifier.testTag("mark_all_read_button")
                        ) {
                            Icon(Icons.Filled.DoneAll, contentDescription = null)
                            Text("Mark All Read")
                        }
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
                    .testTag("whats_new_list"),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = bottomClearance),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (uiState.isOfflineOrCached) {
                    item {
                        DisclaimerBanner(
                            text = "Showing cached release notes because update server could not be reached.",
                            isWarning = true
                        )
                    }
                }

                if (targetRelease == null) {
                    item {
                        EmptyStateCard(
                            icon = Icons.Filled.History,
                            title = "No Published Release Notes Yet",
                            subtitle = "Release notes and version history will appear here once published from the FitTrack update catalog.",
                            actionLabel = "Check for Updates",
                            onAction = onCheckForUpdates
                        )
                    }
                } else {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SectionHeader(
                                title = if (highlightVersionCode != null && targetRelease.versionCode == uiState.installedVersionCode) {
                                    "Updated to v${targetRelease.versionName}"
                                } else {
                                    "Latest Release"
                                }
                            )
                            if (uiState.unreadReleaseCount > 0) {
                                StatusBadge(
                                    text = "${uiState.unreadReleaseCount} UNREAD",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    item(key = "hero_${targetRelease.versionCode}") {
                        ReleaseChangelogCard(
                            release = targetRelease,
                            isLatest = true,
                            isInstalled = targetRelease.versionCode == uiState.installedVersionCode,
                            isRead = targetRelease.versionCode in uiState.localState.readVersionCodes,
                            onMarkAsRead = { onMarkReleaseRead(targetRelease.versionCode) },
                            locale = locale,
                            modifier = Modifier.testTag("release_card_${targetRelease.versionCode}")
                        )
                    }

                    item {
                        if (uiState.isUpdateAvailable) {
                            GradientButton(
                                text = "Open Update Center to Install",
                                icon = Icons.Filled.SystemUpdate,
                                onClick = onOpenUpdateCenter,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("whats_new_open_update_center")
                            )
                        } else {
                            SecondaryButton(
                                text = "Open Update Center",
                                icon = Icons.Filled.SystemUpdate,
                                onClick = onOpenUpdateCenter,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("whats_new_open_update_center")
                            )
                        }
                    }

                    if (previousReleases.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(4.dp))
                            SectionHeader(title = "Previous Releases (${previousReleases.size})")
                        }

                        itemsIndexed(
                            items = previousReleases,
                            key = { _, item -> "prev_${item.versionCode}" }
                        ) { _, release ->
                            ReleaseChangelogCard(
                                release = release,
                                isLatest = false,
                                isInstalled = release.versionCode == uiState.installedVersionCode,
                                isRead = release.versionCode in uiState.localState.readVersionCodes,
                                onMarkAsRead = { onMarkReleaseRead(release.versionCode) },
                                locale = locale,
                                modifier = Modifier.testTag("release_card_${release.versionCode}")
                            )
                        }
                    }
                }
            }
        }
    }
}
