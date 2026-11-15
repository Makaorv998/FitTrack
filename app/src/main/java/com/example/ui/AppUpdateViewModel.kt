package com.example.ui

import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.AppUpdateRepository
import com.example.data.model.ApkInstallUiState
import com.example.data.model.AppRelease
import com.example.data.model.LocalUpdateState
import com.example.data.model.PlayUpdateUiState
import com.example.data.model.RemoteUpdateSnapshot
import com.example.data.model.UpdateCenterUiState
import com.example.data.model.UpdateCheckStatus
import com.example.data.model.UpdateNotificationPreferences
import com.example.data.model.UpdatePolicyConfig
import com.example.data.model.UpdateSeverity
import com.example.domain.AppUpdateEngine
import com.example.platform.AndroidInstalledAppInfoProvider
import com.example.platform.ApkUpdateInstaller
import com.example.platform.InstalledAppInfoProvider
import com.example.platform.NotificationPostResult
import com.example.platform.PlayInAppUpdateManager
import com.example.platform.UpdateNotificationManager
import com.example.platform.UpdatePreferencesStore
import com.example.platform.UpdateStateStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

fun interface UpdateMetadataFetcher {
    suspend fun fetch(preferServer: Boolean): Result<RemoteUpdateSnapshot>
}

fun interface UpdateNotifier {
    fun notify(release: AppRelease, severity: UpdateSeverity): NotificationPostResult
}

class AppUpdateViewModel(
    private val metadataFetcher: UpdateMetadataFetcher,
    private val stateStore: UpdateStateStore,
    private val appInfoProvider: InstalledAppInfoProvider,
    private val notifier: UpdateNotifier,
    private val playUpdateManager: PlayInAppUpdateManager? = null,
    private val apkInstaller: ApkUpdateInstaller? = null,
    private val nowProvider: () -> Long = { System.currentTimeMillis() }
) : ViewModel() {

    private val _uiState = MutableStateFlow(createInitialUiState())
    val uiState: StateFlow<UpdateCenterUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            if (playUpdateManager != null) {
                launch {
                    playUpdateManager.state.collect { playState ->
                        _uiState.update { it.copy(playUpdateState = playState) }
                    }
                }
            }
            if (apkInstaller != null) {
                launch {
                    apkInstaller.state.collect { apkState ->
                        _uiState.update { it.copy(apkInstallState = apkState) }
                    }
                }
            }
            stateStore.observeState().collect { localState ->
                applyLocalState(localState)
            }
        }
        onAppForegrounded(forceCheck = false)
    }

    private fun createInitialUiState(): UpdateCenterUiState {
        val snapshot = appInfoProvider.getInstalledAppSnapshot()
        return UpdateCenterUiState(
            installedVersionCode = snapshot.versionCode,
            installedVersionName = snapshot.versionName,
            distributionChannel = snapshot.distributionChannel,
            installerPackageName = snapshot.installerPackageName,
            hasNotificationPermission = appInfoProvider.hasNotificationPermission(),
            canRequestApkInstalls = appInfoProvider.canRequestPackageInstalls()
        )
    }

    private fun applyLocalState(localState: LocalUpdateState) {
        val snapshot = appInfoProvider.getInstalledAppSnapshot()
        val cachedReleases = localState.cachedReleases.sortedByDescending { it.versionCode }
        val latest = cachedReleases.firstOrNull { it.isPublished }
        val severity = AppUpdateEngine.classifyUpdateSeverity(
            installedVersionCode = snapshot.versionCode,
            installedVersionName = snapshot.versionName,
            latestRelease = latest,
            policy = localState.cachedPolicy
        )
        _uiState.update { current ->
            val resolvedStatus = when {
                current.status == UpdateCheckStatus.CHECKING -> UpdateCheckStatus.CHECKING
                current.status == UpdateCheckStatus.SERVER_UNAVAILABLE -> UpdateCheckStatus.SERVER_UNAVAILABLE
                current.status == UpdateCheckStatus.OFFLINE_CACHED -> UpdateCheckStatus.OFFLINE_CACHED
                severity == UpdateSeverity.CRITICAL -> UpdateCheckStatus.CRITICAL_UPDATE_REQUIRED
                severity == UpdateSeverity.OPTIONAL || severity == UpdateSeverity.RECOMMENDED ->
                    UpdateCheckStatus.UPDATE_AVAILABLE
                localState.lastSuccessfulCheckEpochMs > 0L -> UpdateCheckStatus.UP_TO_DATE
                else -> UpdateCheckStatus.IDLE
            }
            current.copy(
                installedVersionCode = snapshot.versionCode,
                installedVersionName = snapshot.versionName,
                distributionChannel = snapshot.distributionChannel,
                installerPackageName = snapshot.installerPackageName,
                localState = localState,
                releases = cachedReleases,
                latestRelease = latest,
                policy = localState.cachedPolicy,
                severity = severity,
                status = resolvedStatus,
                hasNotificationPermission = appInfoProvider.hasNotificationPermission(),
                canRequestApkInstalls = appInfoProvider.canRequestPackageInstalls()
            )
        }
    }

    fun onAppForegrounded(forceCheck: Boolean = false) {
        viewModelScope.launch {
            refreshSystemPermissions()
            evaluateFirstOpenVersionExperience()

            val localState = stateStore.loadState()
            val prefs = localState.notificationPreferences
            val effectiveInterval = effectiveCheckIntervalMinutes(localState)

            if (forceCheck || prefs.autoCheckOnForeground) {
                if (AppUpdateEngine.shouldRunAutomaticCheck(
                        nowMs = nowProvider(),
                        lastCheckMs = localState.lastCheckEpochMs,
                        intervalMinutes = effectiveInterval,
                        force = forceCheck
                    )
                ) {
                    performUpdateCheck(force = forceCheck)
                }
            }
        }
    }

    private fun effectiveCheckIntervalMinutes(state: LocalUpdateState): Long {
        val prefInterval = state.notificationPreferences.checkIntervalMinutes
        return if (state.cachedPolicy.checkIntervalMinutes > 0L && prefInterval == 360L) {
            state.cachedPolicy.checkIntervalMinutes
        } else {
            prefInterval
        }
    }

    fun refreshSystemPermissions() {
        val hasNotif = appInfoProvider.hasNotificationPermission()
        val canInstall = appInfoProvider.canRequestPackageInstalls()
        apkInstaller?.refreshInstallPermissionState()
        _uiState.update {
            it.copy(
                hasNotificationPermission = hasNotif,
                canRequestApkInstalls = canInstall
            )
        }
    }

    private suspend fun evaluateFirstOpenVersionExperience() {
        val snapshot = appInfoProvider.getInstalledAppSnapshot()
        val localState = stateStore.loadState()
        val eval = AppUpdateEngine.evaluateLaunchVersionState(
            installedVersionCode = snapshot.versionCode,
            installedVersionName = snapshot.versionName,
            state = localState
        )

        if (eval.isFirstInstall) {
            stateStore.updateState { current ->
                current.copy(
                    lastSeenVersionCode = eval.targetVersionCodeToRecord,
                    lastSeenVersionName = eval.targetVersionNameToRecord,
                    shownWhatsNewVersionCode = maxOf(current.shownWhatsNewVersionCode, eval.targetVersionCodeToRecord)
                )
            }
            _uiState.update {
                it.copy(
                    shouldAutoShowWhatsNew = false,
                    pendingWhatsNewVersionCode = null
                )
            }
        } else if (eval.isNewlyUpdated) {
            stateStore.updateState { current ->
                current.copy(
                    lastSeenVersionCode = eval.targetVersionCodeToRecord,
                    lastSeenVersionName = eval.targetVersionNameToRecord
                )
            }
            if (eval.shouldShowWhatsNewOnce) {
                _uiState.update {
                    it.copy(
                        shouldAutoShowWhatsNew = true,
                        pendingWhatsNewVersionCode = eval.targetVersionCodeToRecord
                    )
                }
            }
        }
    }

    fun checkForUpdates(force: Boolean = true) {
        viewModelScope.launch {
            performUpdateCheck(force = force)
        }
    }

    private suspend fun performUpdateCheck(force: Boolean) {
        val snapshot = appInfoProvider.getInstalledAppSnapshot()
        val currentState = stateStore.loadState()
        val effectiveInterval = effectiveCheckIntervalMinutes(currentState)
        if (!AppUpdateEngine.shouldRunAutomaticCheck(
                nowMs = nowProvider(),
                lastCheckMs = currentState.lastCheckEpochMs,
                intervalMinutes = effectiveInterval,
                force = force
            )
        ) {
            return
        }

        _uiState.update {
            it.copy(
                status = UpdateCheckStatus.CHECKING,
                lastErrorMessage = null,
                isOfflineOrCached = false
            )
        }

        val playCheckResult = playUpdateManager?.checkAvailability(
            priorityThreshold = currentState.cachedPolicy.playUpdatePriorityThreshold.toInt().coerceIn(0, 5)
        )

        val remoteResult = metadataFetcher.fetch(true)
        remoteResult.onSuccess { remote ->
            val nowMs = nowProvider()
            val sortedReleases = remote.releases.sortedByDescending { it.versionCode }
            stateStore.recordCheckSuccess(nowMs, sortedReleases, remote.policy)

            val latest = sortedReleases.firstOrNull { it.isPublished }
            val severity = AppUpdateEngine.classifyUpdateSeverity(
                installedVersionCode = snapshot.versionCode,
                installedVersionName = snapshot.versionName,
                latestRelease = latest,
                policy = remote.policy
            )
            val status = when (severity) {
                UpdateSeverity.CRITICAL -> UpdateCheckStatus.CRITICAL_UPDATE_REQUIRED
                UpdateSeverity.OPTIONAL, UpdateSeverity.RECOMMENDED -> UpdateCheckStatus.UPDATE_AVAILABLE
                UpdateSeverity.NONE -> UpdateCheckStatus.UP_TO_DATE
            }

            val updatedLocal = stateStore.loadState()
            _uiState.update { current ->
                current.copy(
                    status = status,
                    severity = severity,
                    latestRelease = latest,
                    releases = sortedReleases,
                    policy = remote.policy,
                    localState = updatedLocal,
                    isOfflineOrCached = false,
                    lastErrorMessage = null,
                    playUpdateState = playCheckResult ?: current.playUpdateState
                )
            }

            maybeDispatchUpdateNotification(
                severity = severity,
                latestRelease = latest,
                localState = updatedLocal
            )
        }.onFailure { err ->
            if (err is CancellationException) throw err
            val cached = stateStore.loadState()
            val hasCache = cached.cachedReleases.isNotEmpty() || cached.lastSuccessfulCheckEpochMs > 0L
            val latestCached = cached.cachedReleases.maxByOrNull { it.versionCode }
            val cachedSeverity = AppUpdateEngine.classifyUpdateSeverity(
                installedVersionCode = snapshot.versionCode,
                installedVersionName = snapshot.versionName,
                latestRelease = latestCached,
                policy = cached.cachedPolicy
            )
            val errMsg = err.localizedMessage?.takeIf { it.isNotBlank() }
                ?: "Update server unreachable. Showing cached status."

            _uiState.update { current ->
                current.copy(
                    status = if (hasCache) UpdateCheckStatus.OFFLINE_CACHED else UpdateCheckStatus.SERVER_UNAVAILABLE,
                    severity = cachedSeverity,
                    latestRelease = latestCached,
                    releases = cached.cachedReleases.sortedByDescending { it.versionCode },
                    policy = cached.cachedPolicy,
                    localState = cached,
                    isOfflineOrCached = hasCache,
                    lastErrorMessage = errMsg,
                    playUpdateState = playCheckResult ?: current.playUpdateState
                )
            }
        }
    }

    private suspend fun maybeDispatchUpdateNotification(
        severity: UpdateSeverity,
        latestRelease: AppRelease?,
        localState: LocalUpdateState
    ) {
        val hasPerm = appInfoProvider.hasNotificationPermission()
        val decision = AppUpdateEngine.evaluateNotificationDispatch(
            severity = severity,
            release = latestRelease,
            state = localState,
            preferences = localState.notificationPreferences,
            hasSystemPermission = hasPerm
        )
        if (!decision.shouldNotify || latestRelease == null) return

        val postResult = notifier.notify(latestRelease, severity)
        if (postResult == NotificationPostResult.SENT) {
            stateStore.recordNotificationSent(latestRelease.versionCode, decision.isCritical)
        }
    }

    fun acknowledgeWhatsNewPresented(versionCode: Long) {
        viewModelScope.launch {
            stateStore.markWhatsNewShown(versionCode)
            _uiState.update {
                it.copy(shouldAutoShowWhatsNew = false, pendingWhatsNewVersionCode = null)
            }
        }
    }

    fun markReleaseAsRead(versionCode: Long) {
        viewModelScope.launch {
            stateStore.markReleaseRead(versionCode)
            _uiState.update {
                it.copy(shouldAutoShowWhatsNew = false, pendingWhatsNewVersionCode = null)
            }
        }
    }

    fun markAllReleasesAsRead() {
        viewModelScope.launch {
            val codes = _uiState.value.releases.map { it.versionCode }
            stateStore.markAllReleasesRead(codes)
            _uiState.update {
                it.copy(shouldAutoShowWhatsNew = false, pendingWhatsNewVersionCode = null)
            }
        }
    }

    fun updateNotificationPreferences(preferences: UpdateNotificationPreferences) {
        viewModelScope.launch {
            stateStore.updateNotificationPreferences(preferences)
        }
    }

    fun startPlayUpdate(
        launcher: ActivityResultLauncher<IntentSenderRequest>,
        forceImmediate: Boolean
    ): Boolean {
        return playUpdateManager?.startUpdateFlow(launcher, forceImmediate) ?: false
    }

    fun resumePlayUpdateIfInProgress(launcher: ActivityResultLauncher<IntentSenderRequest>) {
        playUpdateManager?.resumeIfInProgress(launcher)
    }

    fun onPlayUpdateResult(resultCode: Int) {
        playUpdateManager?.onActivityResult(resultCode)
    }

    fun completePlayFlexibleUpdate() {
        playUpdateManager?.completeFlexibleUpdate()
    }

    fun startApkDownloadAndVerification(release: AppRelease) {
        val installer = apkInstaller ?: return
        viewModelScope.launch {
            installer.downloadAndVerifyApk(
                release = release,
                trustedHosts = _uiState.value.policy.trustedApkHosts
            )
        }
    }

    fun launchVerifiedApkInstaller(apkFilePath: String, targetVersionName: String) {
        apkInstaller?.launchSystemInstaller(apkFilePath, targetVersionName)
    }

    fun createUnknownSourcesSettingsIntent(): Intent? {
        return apkInstaller?.createUnknownSourcesPermissionIntent()
    }

    fun resetApkInstallState() {
        apkInstaller?.reset()
    }

    override fun onCleared() {
        super.onCleared()
        playUpdateManager?.unregisterListener()
    }

    companion object {
        fun createDefault(context: Context): AppUpdateViewModel {
            val appCtx = context.applicationContext
            val repo = AppUpdateRepository(appCtx)
            val store = UpdatePreferencesStore(appCtx)
            val appInfo = AndroidInstalledAppInfoProvider(appCtx)
            val notifManager = UpdateNotificationManager(appCtx, appInfo)
            val playManager = PlayInAppUpdateManager(appCtx)
            val apkInstaller = ApkUpdateInstaller(appCtx, appInfo)
            return AppUpdateViewModel(
                metadataFetcher = { preferServer -> repo.fetchUpdateMetadata(preferServer) },
                stateStore = store,
                appInfoProvider = appInfo,
                notifier = { release, severity -> notifManager.postUpdateNotification(release, severity) },
                playUpdateManager = playManager,
                apkInstaller = apkInstaller
            )
        }
    }
}
