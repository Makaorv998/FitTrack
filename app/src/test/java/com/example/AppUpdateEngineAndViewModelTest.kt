package com.example

import com.example.data.model.AppRelease
import com.example.data.model.LocalUpdateState
import com.example.data.model.RemoteUpdateSnapshot
import com.example.data.model.UpdateCheckStatus
import com.example.data.model.UpdateNotificationPreferences
import com.example.data.model.UpdatePolicyConfig
import com.example.data.model.UpdateSeverity
import com.example.domain.AppUpdateEngine
import com.example.platform.InstalledAppSnapshot
import com.example.platform.InstalledAppInfoProvider
import com.example.platform.NotificationPostResult
import com.example.platform.UpdateStateStore
import com.example.ui.AppUpdateViewModel
import com.example.ui.UpdateMetadataFetcher
import com.example.ui.UpdateNotifier
import com.example.data.model.DistributionChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class AppUpdateEngineAndViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun firstInstallation_establishesBaselineWithoutWhatsNewPrompt() = runTest(testDispatcher) {
        val store = InMemoryUpdateStateStore(LocalUpdateState())
        val appInfo = FakeInstalledAppInfoProvider(
            versionCode = 1L,
            versionName = "1.0"
        )
        val release1 = sampleRelease(versionCode = 1L, versionName = "1.0", severity = "optional")
        val fetcher = UpdateMetadataFetcher {
            Result.success(RemoteUpdateSnapshot(listOf(release1), UpdatePolicyConfig(latestVersionCode = 1L)))
        }
        val notifier = RecordingUpdateNotifier()

        val vm = AppUpdateViewModel(
            metadataFetcher = fetcher,
            stateStore = store,
            appInfoProvider = appInfo,
            notifier = notifier,
            nowProvider = { 1_000_000L }
        )
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(UpdateCheckStatus.UP_TO_DATE, state.status)
        assertEquals(UpdateSeverity.NONE, state.severity)
        assertFalse(state.shouldAutoShowWhatsNew)
        assertNull(state.pendingWhatsNewVersionCode)
        assertEquals(1L, store.loadState().lastSeenVersionCode)
        assertTrue(notifier.sent.isEmpty())
    }

    @Test
    fun existingVersionWithNoUpdate_reportsUpToDateAndNoNotification() = runTest(testDispatcher) {
        val store = InMemoryUpdateStateStore(
            LocalUpdateState(
                lastSeenVersionCode = 5L,
                lastSeenVersionName = "1.5.0",
                shownWhatsNewVersionCode = 5L,
                readVersionCodes = setOf(5L),
                lastCheckEpochMs = 0L
            )
        )
        val appInfo = FakeInstalledAppInfoProvider(versionCode = 5L, versionName = "1.5.0")
        val release5 = sampleRelease(versionCode = 5L, versionName = "1.5.0", severity = "optional")
        val notifier = RecordingUpdateNotifier()

        val vm = AppUpdateViewModel(
            metadataFetcher = { Result.success(RemoteUpdateSnapshot(listOf(release5), UpdatePolicyConfig(latestVersionCode = 5L))) },
            stateStore = store,
            appInfoProvider = appInfo,
            notifier = notifier,
            nowProvider = { 5_000_000L }
        )
        advanceUntilIdle()

        assertEquals(UpdateCheckStatus.UP_TO_DATE, vm.uiState.value.status)
        assertFalse(vm.uiState.value.isUpdateAvailable)
        assertTrue(notifier.sent.isEmpty())
    }

    @Test
    fun newOptionalUpdate_detectsUpdateAndSendsSingleNotificationWithoutDuplicates() = runTest(testDispatcher) {
        var nowMs = 1_000_000L
        val store = InMemoryUpdateStateStore(
            LocalUpdateState(
                lastSeenVersionCode = 1L,
                lastSeenVersionName = "1.0",
                shownWhatsNewVersionCode = 1L
            )
        )
        val appInfo = FakeInstalledAppInfoProvider(versionCode = 1L, versionName = "1.0", hasNotifPerm = true)
        val release2 = sampleRelease(versionCode = 2L, versionName = "1.1.0", severity = "optional")
        val notifier = RecordingUpdateNotifier()

        val vm = AppUpdateViewModel(
            metadataFetcher = { Result.success(RemoteUpdateSnapshot(listOf(release2), UpdatePolicyConfig(latestVersionCode = 2L))) },
            stateStore = store,
            appInfoProvider = appInfo,
            notifier = notifier,
            nowProvider = { nowMs }
        )
        advanceUntilIdle()

        assertEquals(UpdateCheckStatus.UPDATE_AVAILABLE, vm.uiState.value.status)
        assertEquals(UpdateSeverity.OPTIONAL, vm.uiState.value.severity)
        assertEquals(1, notifier.sent.size)
        assertEquals(2L to UpdateSeverity.OPTIONAL, notifier.sent.single())

        // Subsequent manual/interval check for the same release must NOT send a duplicate notification
        nowMs += 400 * 60_000L
        vm.checkForUpdates(force = true)
        advanceUntilIdle()

        assertEquals(1, notifier.sent.size)
    }

    @Test
    fun criticalUpdate_viaSeverityOrMinSupportedVersion_dispatchesCriticalAlert() = runTest(testDispatcher) {
        val store = InMemoryUpdateStateStore(
            LocalUpdateState(lastSeenVersionCode = 2L, lastSeenVersionName = "1.1.0", shownWhatsNewVersionCode = 2L)
        )
        val appInfo = FakeInstalledAppInfoProvider(versionCode = 2L, versionName = "1.1.0", hasNotifPerm = true)
        // Even if severity says "recommended", minSupportedVersionCode = 3 forces CRITICAL
        val release4 = sampleRelease(
            versionCode = 4L,
            versionName = "2.0.0",
            severity = "recommended",
            minSupportedVersionCode = 3L
        )
        val notifier = RecordingUpdateNotifier()

        val vm = AppUpdateViewModel(
            metadataFetcher = { Result.success(RemoteUpdateSnapshot(listOf(release4), UpdatePolicyConfig(minSupportedVersionCode = 3L))) },
            stateStore = store,
            appInfoProvider = appInfo,
            notifier = notifier,
            nowProvider = { 2_000_000L }
        )
        advanceUntilIdle()

        assertEquals(UpdateCheckStatus.CRITICAL_UPDATE_REQUIRED, vm.uiState.value.status)
        assertEquals(UpdateSeverity.CRITICAL, vm.uiState.value.severity)
        assertEquals(listOf(4L to UpdateSeverity.CRITICAL), notifier.sent)
    }

    @Test
    fun offlineLaunchWithCache_usesCachedReleasesAndOfflineStatus() = runTest(testDispatcher) {
        val cachedRelease = sampleRelease(versionCode = 3L, versionName = "1.2.0", severity = "optional")
        val store = InMemoryUpdateStateStore(
            LocalUpdateState(
                lastSeenVersionCode = 1L,
                lastSeenVersionName = "1.0",
                shownWhatsNewVersionCode = 1L,
                lastSuccessfulCheckEpochMs = 100_000L,
                lastCheckEpochMs = 100_000L,
                cachedReleases = listOf(cachedRelease),
                cachedPolicy = UpdatePolicyConfig(latestVersionCode = 3L)
            )
        )
        val appInfo = FakeInstalledAppInfoProvider(versionCode = 1L, versionName = "1.0")
        val notifier = RecordingUpdateNotifier()

        val vm = AppUpdateViewModel(
            metadataFetcher = { Result.failure(IOException("Network offline")) },
            stateStore = store,
            appInfoProvider = appInfo,
            notifier = notifier,
            nowProvider = { 100_000L + 400 * 60_000L }
        )
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(UpdateCheckStatus.OFFLINE_CACHED, state.status)
        assertTrue(state.isOfflineOrCached)
        assertEquals("1.2.0", state.latestRelease?.versionName)
        assertTrue(state.isUpdateAvailable)
    }

    @Test
    fun serverUnavailableWithoutCache_reportsServerUnavailable() = runTest(testDispatcher) {
        val store = InMemoryUpdateStateStore(
            LocalUpdateState(lastSeenVersionCode = 1L, lastSeenVersionName = "1.0", shownWhatsNewVersionCode = 1L)
        )
        val vm = AppUpdateViewModel(
            metadataFetcher = { Result.failure(IllegalStateException("503 Service Unavailable")) },
            stateStore = store,
            appInfoProvider = FakeInstalledAppInfoProvider(versionCode = 1L, versionName = "1.0"),
            notifier = RecordingUpdateNotifier(),
            nowProvider = { 10_000_000L }
        )
        advanceUntilIdle()

        assertEquals(UpdateCheckStatus.SERVER_UNAVAILABLE, vm.uiState.value.status)
        assertFalse(vm.uiState.value.isOfflineOrCached)
        assertTrue(vm.uiState.value.lastErrorMessage?.contains("503") == true)
    }

    @Test
    fun firstLaunchAfterUpdate_showsWhatsNewOnce_andReopeningDoesNotRepeat() = runTest(testDispatcher) {
        val store = InMemoryUpdateStateStore(
            LocalUpdateState(
                lastSeenVersionCode = 1L,
                lastSeenVersionName = "1.0",
                shownWhatsNewVersionCode = 1L,
                readVersionCodes = setOf(1L)
            )
        )
        val appInfo = FakeInstalledAppInfoProvider(versionCode = 2L, versionName = "1.1.0")
        val release2 = sampleRelease(versionCode = 2L, versionName = "1.1.0", severity = "optional")
        val fetcher = UpdateMetadataFetcher {
            Result.success(RemoteUpdateSnapshot(listOf(release2), UpdatePolicyConfig(latestVersionCode = 2L)))
        }

        val vmFirstLaunch = AppUpdateViewModel(
            metadataFetcher = fetcher,
            stateStore = store,
            appInfoProvider = appInfo,
            notifier = RecordingUpdateNotifier(),
            nowProvider = { 10_000_000L }
        )
        advanceUntilIdle()

        assertTrue(vmFirstLaunch.uiState.value.shouldAutoShowWhatsNew)
        assertEquals(2L, vmFirstLaunch.uiState.value.pendingWhatsNewVersionCode)

        // Acknowledge shown / mark read
        vmFirstLaunch.acknowledgeWhatsNewPresented(2L)
        vmFirstLaunch.markReleaseAsRead(2L)
        advanceUntilIdle()

        assertFalse(vmFirstLaunch.uiState.value.shouldAutoShowWhatsNew)
        assertEquals(0, vmFirstLaunch.uiState.value.unreadReleaseCount)

        // Reopening app on same installed version must NOT prompt What's New again
        val vmReopened = AppUpdateViewModel(
            metadataFetcher = fetcher,
            stateStore = store,
            appInfoProvider = appInfo,
            notifier = RecordingUpdateNotifier(),
            nowProvider = { 10_060_000L }
        )
        advanceUntilIdle()

        assertFalse(vmReopened.uiState.value.shouldAutoShowWhatsNew)
        assertNull(vmReopened.uiState.value.pendingWhatsNewVersionCode)
    }

    @Test
    fun notificationPermissionDenied_preventsDispatchAndAllowsRetryOnceGranted() = runTest(testDispatcher) {
        var nowMs = 1_000_000L
        val store = InMemoryUpdateStateStore(
            LocalUpdateState(lastSeenVersionCode = 1L, lastSeenVersionName = "1.0", shownWhatsNewVersionCode = 1L)
        )
        val appInfo = FakeInstalledAppInfoProvider(versionCode = 1L, versionName = "1.0", hasNotifPerm = false)
        val release2 = sampleRelease(versionCode = 2L, versionName = "1.1.0", severity = "critical")
        val notifier = RecordingUpdateNotifier()

        val vm = AppUpdateViewModel(
            metadataFetcher = { Result.success(RemoteUpdateSnapshot(listOf(release2), UpdatePolicyConfig())) },
            stateStore = store,
            appInfoProvider = appInfo,
            notifier = notifier,
            nowProvider = { nowMs }
        )
        advanceUntilIdle()

        assertTrue("Notification must not be posted when OS permission denied", notifier.sent.isEmpty())
        assertFalse(vm.uiState.value.hasNotificationPermission)
        assertEquals(-1L, store.loadState().lastNotifiedCriticalVersionCode)

        // Grant permission and re-check
        appInfo.hasNotifPerm = true
        vm.refreshSystemPermissions()
        nowMs += 400 * 60_000L
        vm.checkForUpdates(force = true)
        advanceUntilIdle()

        assertEquals(1, notifier.sent.size)
        assertEquals(2L, store.loadState().lastNotifiedCriticalVersionCode)
    }

    @Test
    fun checkIntervalThrottling_skipsRedundantForegroundChecksUnlessForced() = runTest(testDispatcher) {
        var fetchCalls = 0
        var nowMs = 10_000_000L
        val store = InMemoryUpdateStateStore(
            LocalUpdateState(
                lastSeenVersionCode = 1L,
                lastSeenVersionName = "1.0",
                shownWhatsNewVersionCode = 1L,
                lastCheckEpochMs = nowMs - 30 * 60_000L, // 30 mins ago (< 360 min interval)
                notificationPreferences = UpdateNotificationPreferences(checkIntervalMinutes = 360L)
            )
        )
        val vm = AppUpdateViewModel(
            metadataFetcher = {
                fetchCalls++
                Result.success(RemoteUpdateSnapshot(emptyList(), UpdatePolicyConfig()))
            },
            stateStore = store,
            appInfoProvider = FakeInstalledAppInfoProvider(versionCode = 1L, versionName = "1.0"),
            notifier = RecordingUpdateNotifier(),
            nowProvider = { nowMs }
        )
        advanceUntilIdle()
        assertEquals(0, fetchCalls)

        // Foreground again after 365 minutes total elapsed -> runs automatically
        nowMs += 335 * 60_000L
        vm.onAppForegrounded(forceCheck = false)
        advanceUntilIdle()
        assertEquals(1, fetchCalls)
    }

    @Test
    fun apkAuthenticityChecks_validateTrustedHttpsUrlsSha256AndSigningCerts() {
        // URL checks
        assertTrue(AppUpdateEngine.isTrustedApkUrl("https://releases.fittrack.app/v2.apk", listOf("releases.fittrack.app")))
        assertTrue(AppUpdateEngine.isTrustedApkUrl("https://cdn.releases.fittrack.app/v2.apk", listOf("releases.fittrack.app")))
        assertFalse(AppUpdateEngine.isTrustedApkUrl("http://releases.fittrack.app/v2.apk", listOf("releases.fittrack.app")))
        assertFalse(AppUpdateEngine.isTrustedApkUrl("https://evil.example.com/v2.apk", listOf("releases.fittrack.app")))
        assertFalse(AppUpdateEngine.isTrustedApkUrl("https://localhost/v2.apk", emptyList()))
        assertFalse(AppUpdateEngine.isTrustedApkUrl("https://user:pass@releases.fittrack.app/v2.apk", emptyList()))

        // SHA-256 verification
        val digest = AppUpdateEngine.sha256Hex("fittrack-apk-bytes".toByteArray())
        assertTrue(AppUpdateEngine.verifySha256Digest(digest, digest.uppercase(Locale.US)))
        assertFalse(AppUpdateEngine.verifySha256Digest(digest, "0".repeat(64)))
        assertFalse(AppUpdateEngine.verifySha256Digest(digest, "invalid-short-hex"))

        // Signing certificate matching
        val certA = "aa".repeat(32)
        val certB = "bb".repeat(32)
        assertTrue(AppUpdateEngine.verifySigningCertificates(setOf(certA), setOf(certA), certA))
        assertFalse(AppUpdateEngine.verifySigningCertificates(setOf(certB), setOf(certA), certA))
        assertFalse(AppUpdateEngine.verifySigningCertificates(emptySet(), setOf(certA), certA))
    }

    @Test
    fun releaseLocalization_resolvesLocaleSpecificChangelogAndFallsBackCleanly() {
        val release = sampleRelease(versionCode = 7L, versionName = "1.7.0", severity = "optional").copy(
            localizedEntries = mapOf(
                "es" to com.example.data.model.LocalizedReleaseContent(
                    title = "Centro de Actualizaciones",
                    summary = "Notas de versión localizadas",
                    newFeatures = listOf("Detección automática de actualizaciones")
                )
            )
        )
        val es = release.localizedFor(Locale.forLanguageTag("es-ES"))
        assertEquals("Centro de Actualizaciones", es.title)
        assertEquals(listOf("Detección automática de actualizaciones"), es.newFeatures)
        // Missing fields fall back to default English lists
        assertEquals(release.bugFixes, es.bugFixes)

        val fr = release.localizedFor(Locale.FRENCH)
        assertEquals(release.title, fr.title)
    }

    private fun sampleRelease(
        versionCode: Long,
        versionName: String,
        severity: String,
        minSupportedVersionCode: Long = 0L
    ) = AppRelease(
        id = "v$versionCode",
        versionName = versionName,
        versionCode = versionCode,
        releaseDate = "2026-11-15",
        title = "FitTrack v$versionName",
        summary = "Release $versionName summary",
        severity = severity,
        minSupportedVersionCode = minSupportedVersionCode,
        minSupportedVersionName = if (minSupportedVersionCode > 0L) "1.$minSupportedVersionCode.0" else "",
        distributionChannel = "both",
        apkDownloadUrl = "https://releases.fittrack.app/fittrack-$versionCode.apk",
        apkSha256 = "a".repeat(64),
        expectedSigningCertSha256 = "b".repeat(64),
        playStoreUrl = "https://play.google.com/store/apps/details?id=com.aistudio.fittrack.vrmqtx",
        newFeatures = listOf("Feature $versionCode"),
        improvements = listOf("Improvement $versionCode"),
        bugFixes = listOf("Bugfix $versionCode"),
        securityChanges = listOf("Security $versionCode"),
        isPublished = true
    )

    private class InMemoryUpdateStateStore(initial: LocalUpdateState) : UpdateStateStore {
        private val flow = MutableStateFlow(initial)
        override fun observeState(): Flow<LocalUpdateState> = flow
        override suspend fun loadState(): LocalUpdateState = flow.value
        override suspend fun updateState(transform: (LocalUpdateState) -> LocalUpdateState) {
            flow.value = transform(flow.value)
        }
        override suspend fun markReleaseRead(versionCode: Long) {
            updateState { it.copy(readVersionCodes = it.readVersionCodes + versionCode, shownWhatsNewVersionCode = maxOf(it.shownWhatsNewVersionCode, versionCode)) }
        }
        override suspend fun markAllReleasesRead(versionCodes: Collection<Long>) {
            updateState { it.copy(readVersionCodes = it.readVersionCodes + versionCodes) }
        }
        override suspend fun markWhatsNewShown(versionCode: Long) {
            updateState { it.copy(shownWhatsNewVersionCode = maxOf(it.shownWhatsNewVersionCode, versionCode), lastSeenVersionCode = maxOf(it.lastSeenVersionCode, versionCode)) }
        }
        override suspend fun recordCheckSuccess(nowMs: Long, releases: List<AppRelease>, policy: UpdatePolicyConfig) {
            updateState { it.copy(lastCheckEpochMs = nowMs, lastSuccessfulCheckEpochMs = nowMs, cachedReleases = releases, cachedPolicy = policy) }
        }
        override suspend fun recordNotificationSent(versionCode: Long, isCritical: Boolean) {
            updateState {
                if (isCritical) it.copy(lastNotifiedCriticalVersionCode = versionCode)
                else it.copy(lastNotifiedOptionalVersionCode = versionCode)
            }
        }
        override suspend fun updateNotificationPreferences(preferences: UpdateNotificationPreferences) {
            updateState { it.copy(notificationPreferences = preferences) }
        }
    }

    private class FakeInstalledAppInfoProvider(
        private val versionCode: Long,
        private val versionName: String,
        var hasNotifPerm: Boolean = true,
        var canInstallApks: Boolean = true
    ) : InstalledAppInfoProvider {
        override fun getInstalledAppSnapshot() = InstalledAppSnapshot(
            packageName = "com.aistudio.fittrack.vrmqtx",
            versionCode = versionCode,
            versionName = versionName,
            installerPackageName = "",
            distributionChannel = DistributionChannel.DIRECT_APK,
            signingCertSha256Digests = setOf("b".repeat(64))
        )
        override fun canRequestPackageInstalls(): Boolean = canInstallApks
        override fun hasNotificationPermission(): Boolean = hasNotifPerm
    }

    private class RecordingUpdateNotifier : UpdateNotifier {
        val sent = mutableListOf<Pair<Long, UpdateSeverity>>()
        override fun notify(release: AppRelease, severity: UpdateSeverity): NotificationPostResult {
            sent += release.versionCode to severity
            return NotificationPostResult.SENT
        }
    }
}
