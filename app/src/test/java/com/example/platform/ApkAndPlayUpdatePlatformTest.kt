package com.example.platform

import android.Manifest
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.ApkInstallUiState
import com.example.data.model.AppRelease
import com.example.data.model.DistributionChannel
import com.example.data.model.PlayUpdateUiState
import com.example.data.model.UpdateSeverity
import com.example.domain.AppUpdateEngine
import com.google.android.play.core.appupdate.testing.FakeAppUpdateManager
import com.google.android.play.core.install.model.AppUpdateType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ApkAndPlayUpdatePlatformTest {

    @Test
    fun updateNotificationManager_createsChannelsRespectsPermissionAndPostsOnce() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        var permissionGranted = false
        val infoProvider = object : InstalledAppInfoProvider {
            override fun getInstalledAppSnapshot() = InstalledAppSnapshot(
                packageName = context.packageName,
                versionCode = 1L,
                versionName = "1.0",
                installerPackageName = "",
                distributionChannel = DistributionChannel.DIRECT_APK,
                signingCertSha256Digests = emptySet()
            )
            override fun canRequestPackageInstalls(): Boolean = true
            override fun hasNotificationPermission(): Boolean = permissionGranted
        }

        val manager = UpdateNotificationManager(context, infoProvider)
        val release = AppRelease(
            id = "v2",
            versionName = "1.2.0",
            versionCode = 2L,
            releaseDate = "2026-11-15",
            title = "Critical Security Patch",
            summary = "Fixes authentication session handling.",
            severity = "critical",
            isPublished = true
        )

        // Denied -> must not post
        val deniedResult = manager.postUpdateNotification(release, UpdateSeverity.CRITICAL)
        assertEquals(NotificationPostResult.PERMISSION_DENIED, deniedResult)

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val shadowNm = shadowOf(nm)
        assertEquals(0, shadowNm.size())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            assertNotNull(nm.getNotificationChannel(UpdateNotificationManager.CHANNEL_OPTIONAL_UPDATES))
            assertNotNull(nm.getNotificationChannel(UpdateNotificationManager.CHANNEL_CRITICAL_UPDATES))
        }

        // Granted -> posts notification
        permissionGranted = true
        shadowOf(context).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        val sentResult = manager.postUpdateNotification(release, UpdateSeverity.CRITICAL)
        assertEquals(NotificationPostResult.SENT, sentResult)
        assertEquals(1, shadowNm.size())
    }

    @Test
    fun apkUpdateInstaller_rejectsCorruptArchiveAndShaMismatchAndBlocksWithoutInstallPermission() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        var allowUnknownSources = false
        val infoProvider = object : InstalledAppInfoProvider {
            override fun getInstalledAppSnapshot() = InstalledAppSnapshot(
                packageName = context.packageName,
                versionCode = 1L,
                versionName = "1.0",
                installerPackageName = "",
                distributionChannel = DistributionChannel.DIRECT_APK,
                signingCertSha256Digests = setOf("a".repeat(64))
            )
            override fun canRequestPackageInstalls(): Boolean = allowUnknownSources
            override fun hasNotificationPermission(): Boolean = true
        }
        val installer = ApkUpdateInstaller(context, infoProvider)
        val tempApk = File(context.cacheDir, "test_fake.apk").apply {
            writeBytes("not-a-valid-apk".toByteArray())
        }
        val actualSha = AppUpdateEngine.sha256Hex(tempApk.readBytes())
        val releaseMismatch = AppRelease(
            id = "v2",
            versionName = "1.2.0",
            versionCode = 2L,
            releaseDate = "2026-11-15",
            title = "Update",
            apkSha256 = "f".repeat(64),
            isPublished = true
        )
        val shaFailed = installer.verifyDownloadedApk(tempApk, actualSha, releaseMismatch)
        assertFalse(shaFailed.isValid)
        assertTrue(shaFailed.failureReason.contains("SHA-256"))

        val releaseValidSha = releaseMismatch.copy(apkSha256 = actualSha)
        val archiveFailed = installer.verifyDownloadedApk(tempApk, actualSha, releaseValidSha)
        assertFalse(archiveFailed.isValid)
        assertTrue(archiveFailed.failureReason.contains("APK"))

        // Launching without unknown sources permission transitions to NeedsUnknownSourcesPermission
        assertFalse(installer.launchSystemInstaller(tempApk.absolutePath, "1.2.0"))
        assertTrue(installer.state.value is ApkInstallUiState.NeedsUnknownSourcesPermission)

        // After permission granted, refresh moves to VerifiedReady
        allowUnknownSources = true
        installer.refreshInstallPermissionState()
        assertTrue(installer.state.value is ApkInstallUiState.VerifiedReady)
    }

    @Test
    fun playInAppUpdateManager_detectsAvailableFlexibleAndImmediateUpdatesWithFakeManager() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val fakePlayManager = FakeAppUpdateManager(context).apply {
            setUpdateAvailable(5, AppUpdateType.FLEXIBLE)
            setUpdatePriority(5)
        }
        val manager = PlayInAppUpdateManager(context, fakePlayManager)

        val state = manager.checkAvailability(priorityThreshold = 4)
        assertTrue(state is PlayUpdateUiState.Available)
        val available = state as PlayUpdateUiState.Available
        assertEquals(5, available.availableVersionCode)
        assertTrue(available.isFlexibleAllowed)

        // Simulate user declining prompt
        manager.onActivityResult(android.app.Activity.RESULT_CANCELED)
        assertTrue(manager.state.value is PlayUpdateUiState.Available)
    }
}
