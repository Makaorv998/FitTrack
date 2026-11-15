package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import com.example.data.model.ApkInstallUiState
import com.example.data.model.AppRelease
import com.example.data.model.DistributionChannel
import com.example.data.model.LocalUpdateState
import com.example.data.model.PlayUpdateUiState
import com.example.data.model.UpdateCenterUiState
import com.example.data.model.UpdateCheckStatus
import com.example.data.model.UpdateSeverity
import com.example.data.model.UserProfile
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.UpdateCenterScreen
import com.example.ui.screens.WhatsNewScreen
import com.example.ui.theme.FitTrackTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class UpdateUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val release2 = AppRelease(
        id = "v2",
        versionName = "1.2.0",
        versionCode = 2L,
        releaseDate = "2026-11-15",
        title = "Aurora Update & Verified Releases",
        summary = "Automatic update detection and full changelog history.",
        severity = "critical",
        minSupportedVersionCode = 1L,
        minSupportedVersionName = "1.0",
        distributionChannel = "both",
        apkDownloadUrl = "https://releases.fittrack.app/fittrack-v2.apk",
        newFeatures = listOf("Smart Auto Update Center", "Categorized What's New history"),
        improvements = listOf("Faster startup version verification"),
        bugFixes = listOf("Fixed duplicate rest timer notifications"),
        securityChanges = listOf("Added SHA-256 and signing cert checks"),
        isPublished = true
    )

    private val release1 = AppRelease(
        id = "v1",
        versionName = "1.0",
        versionCode = 1L,
        releaseDate = "2026-10-01",
        title = "Initial FitTrack Release",
        summary = "Baseline fitness tracking release.",
        severity = "optional",
        newFeatures = listOf("Step counter and GPS route tracker"),
        isPublished = true
    )

    @Test
    fun whatsNewScreen_rendersChangelogHistoryBadgesAndMarkAsReadInDarkAndLightThemes() {
        var markedVersion = -1L
        val uiState = UpdateCenterUiState(
            installedVersionCode = 2L,
            installedVersionName = "1.2.0",
            releases = listOf(release2, release1),
            latestRelease = release2,
            localState = LocalUpdateState(readVersionCodes = setOf(1L))
        )

        composeRule.setContent {
            FitTrackTheme(themePreference = "dark") {
                WhatsNewScreen(
                    uiState = uiState,
                    highlightVersionCode = 2L,
                    onMarkReleaseRead = { markedVersion = it },
                    onMarkAllRead = {},
                    onOpenUpdateCenter = {},
                    onCheckForUpdates = {},
                    onBack = {}
                )
            }
        }

        composeRule.onNodeWithText("Updated to v1.2.0").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Aurora Update & Verified Releases").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Smart Auto Update Center").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Added SHA-256 and signing cert checks").performScrollTo().assertIsDisplayed()

        composeRule.onNodeWithTag("mark_read_2").performScrollTo().performClick()
        assertEquals(2L, markedVersion)

        composeRule.onNodeWithTag("whats_new_list")
            .performScrollToNode(hasTestTag("release_card_1"))
        composeRule.onNodeWithText("Previous Releases (1)").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Initial FitTrack Release").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun updateCenterScreen_showsCriticalUpdatePlayAndApkActionsAndPermissionBanners() {
        var checkClicked = false
        var playImmediateClicked = false
        var apkInstallClicked = false

        val uiState = UpdateCenterUiState(
            installedVersionCode = 1L,
            installedVersionName = "1.0",
            distributionChannel = DistributionChannel.DIRECT_APK,
            status = UpdateCheckStatus.CRITICAL_UPDATE_REQUIRED,
            severity = UpdateSeverity.CRITICAL,
            latestRelease = release2,
            releases = listOf(release2, release1),
            hasNotificationPermission = false,
            playUpdateState = PlayUpdateUiState.Available(
                availableVersionCode = 2,
                updatePriority = 5,
                isImmediatePreferred = true,
                isFlexibleAllowed = true,
                isImmediateAllowed = true
            ),
            apkInstallState = ApkInstallUiState.VerifiedReady(
                apkFilePath = "/cache/updates/fittrack-v2.apk",
                certSha256 = "ab".repeat(32),
                targetVersionName = "1.2.0"
            )
        )

        composeRule.setContent {
            FitTrackTheme(themePreference = "light") {
                UpdateCenterScreen(
                    uiState = uiState,
                    onCheckForUpdates = { checkClicked = true },
                    onStartPlayUpdate = { immediate -> if (immediate) playImmediateClicked = true },
                    onCompletePlayUpdate = {},
                    onDownloadApkUpdate = {},
                    onOpenUnknownSourcesSettings = {},
                    onLaunchApkInstaller = { _, _ -> apkInstallClicked = true },
                    onOpenWhatsNew = {},
                    onRequestNotificationPermission = {},
                    onUpdateNotificationPrefs = {},
                    onBack = {}
                )
            }
        }

        composeRule.onNodeWithTag("check_updates_button").performScrollTo().performClick()
        assertTrue(checkClicked)

        composeRule.onNodeWithTag("update_center_list")
            .performScrollToNode(hasTestTag("critical_update_banner"))
        composeRule.onNodeWithTag("critical_update_banner").assertIsDisplayed()

        composeRule.onNodeWithTag("update_center_list")
            .performScrollToNode(hasTestTag("play_immediate_update_button"))
        composeRule.onNodeWithTag("play_immediate_update_button").performScrollTo().performClick()
        assertTrue(playImmediateClicked)

        composeRule.onNodeWithTag("update_center_list")
            .performScrollToNode(hasTestTag("apk_install_button"))
        composeRule.onNodeWithTag("apk_install_button").performScrollTo().performClick()
        assertTrue(apkInstallClicked)

        composeRule.onNodeWithTag("update_center_list")
            .performScrollToNode(hasTestTag("notif_permission_denied_banner"))
        composeRule.onNodeWithTag("notif_permission_denied_banner").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_exposesUpdateCenterAndWhatsNewEntryPoints() {
        var openedUpdateCenter = false
        var openedWhatsNew = false

        composeRule.setContent {
            FitTrackTheme(themePreference = "dark") {
                SettingsScreen(
                    profile = UserProfile(displayName = "Test User"),
                    onChangeTheme = {},
                    onChangeUnits = {},
                    onUpdateConsent = { _, _ -> },
                    onExportJson = { "{}" },
                    onSignOut = {},
                    onDeleteAccountData = {},
                    onBack = {},
                    updateUiState = UpdateCenterUiState(
                        installedVersionCode = 1L,
                        installedVersionName = "1.0",
                        status = UpdateCheckStatus.UPDATE_AVAILABLE,
                        severity = UpdateSeverity.OPTIONAL,
                        latestRelease = release2,
                        releases = listOf(release2, release1)
                    ),
                    onOpenUpdateCenter = { openedUpdateCenter = true },
                    onOpenWhatsNew = { openedWhatsNew = true }
                )
            }
        }

        composeRule.onNodeWithTag("settings_update_center_button").performScrollTo().performClick()
        assertTrue(openedUpdateCenter)

        composeRule.onNodeWithTag("settings_whats_new_button").performScrollTo().performClick()
        assertTrue(openedWhatsNew)
    }
}
