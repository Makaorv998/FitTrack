package com.example.platform

import android.content.Context
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.example.data.model.PlayUpdateUiState
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

class PlayInAppUpdateManager(context: Context) {

    private val appUpdateManager: AppUpdateManager = AppUpdateManagerFactory.create(context.applicationContext)

    private val _state = MutableStateFlow<PlayUpdateUiState>(PlayUpdateUiState.Idle)
    val state: StateFlow<PlayUpdateUiState> = _state.asStateFlow()

    private var cachedInfo: AppUpdateInfo? = null
    private var listenerRegistered = false

    private val installListener = InstallStateUpdatedListener { installState ->
        when (installState.installStatus()) {
            InstallStatus.DOWNLOADING -> {
                _state.value = PlayUpdateUiState.Downloading(
                    bytesDownloaded = installState.bytesDownloaded(),
                    totalBytes = installState.totalBytesToDownload().coerceAtLeast(1L)
                )
            }
            InstallStatus.DOWNLOADED -> {
                _state.value = PlayUpdateUiState.DownloadedReadyToInstall
            }
            InstallStatus.INSTALLING -> {
                _state.value = PlayUpdateUiState.Installing
            }
            InstallStatus.INSTALLED -> {
                unregisterListener()
                _state.value = PlayUpdateUiState.InstalledConfirmed
            }
            InstallStatus.FAILED -> {
                unregisterListener()
                _state.value = PlayUpdateUiState.Failed(
                    "Google Play update failed (error code ${installState.installErrorCode()})."
                )
            }
            InstallStatus.CANCELED -> {
                unregisterListener()
                _state.value = PlayUpdateUiState.Unavailable
            }
            else -> Unit
        }
    }

    suspend fun checkAvailability(priorityThreshold: Int = 4): PlayUpdateUiState {
        _state.value = PlayUpdateUiState.Checking
        return try {
            val info = appUpdateManager.appUpdateInfo.await()
            cachedInfo = info

            if (info.installStatus() == InstallStatus.DOWNLOADED) {
                _state.value = PlayUpdateUiState.DownloadedReadyToInstall
                return _state.value
            }

            val availability = info.updateAvailability()
            if (availability == UpdateAvailability.UPDATE_AVAILABLE ||
                availability == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS
            ) {
                val flexibleAllowed = info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
                val immediateAllowed = info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
                val priority = info.updatePriority()
                val nextState = PlayUpdateUiState.Available(
                    availableVersionCode = info.availableVersionCode(),
                    updatePriority = priority,
                    isImmediatePreferred = priority >= priorityThreshold && immediateAllowed,
                    isFlexibleAllowed = flexibleAllowed,
                    isImmediateAllowed = immediateAllowed
                )
                _state.value = nextState
                nextState
            } else {
                _state.value = PlayUpdateUiState.Unavailable
                PlayUpdateUiState.Unavailable
            }
        } catch (_: Exception) {
            // Expected on devices without Play Store (e.g., emulators, sideload builds, Huawei/local APK)
            _state.value = PlayUpdateUiState.Unavailable
            PlayUpdateUiState.Unavailable
        }
    }

    fun startUpdateFlow(
        launcher: ActivityResultLauncher<IntentSenderRequest>,
        forceImmediate: Boolean
    ): Boolean {
        val info = cachedInfo ?: return false
        val updateType = when {
            forceImmediate && info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE) -> AppUpdateType.IMMEDIATE
            !forceImmediate && info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) -> AppUpdateType.FLEXIBLE
            info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE) -> AppUpdateType.IMMEDIATE
            else -> return false
        }

        if (updateType == AppUpdateType.FLEXIBLE && !listenerRegistered) {
            appUpdateManager.registerListener(installListener)
            listenerRegistered = true
        }

        return runCatching {
            appUpdateManager.startUpdateFlowForResult(
                info,
                launcher,
                AppUpdateOptions.newBuilder(updateType).build()
            )
        }.onFailure { err ->
            _state.value = PlayUpdateUiState.Failed(err.localizedMessage ?: "Unable to launch Google Play update.")
        }.getOrDefault(false)
    }

    fun resumeIfInProgress(launcher: ActivityResultLauncher<IntentSenderRequest>) {
        appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
            cachedInfo = info
            when {
                info.installStatus() == InstallStatus.DOWNLOADED -> {
                    _state.value = PlayUpdateUiState.DownloadedReadyToInstall
                }
                info.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS &&
                    info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE) -> {
                    runCatching {
                        appUpdateManager.startUpdateFlowForResult(
                            info,
                            launcher,
                            AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build()
                        )
                    }
                }
            }
        }
    }

    fun onActivityResult(resultCode: Int) {
        if (resultCode != android.app.Activity.RESULT_OK && _state.value !is PlayUpdateUiState.Downloading) {
            // User declined or cancelled the Play prompt; keep Available if still cached or reset cleanly.
            val info = cachedInfo
            if (info != null && info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                _state.value = PlayUpdateUiState.Available(
                    availableVersionCode = info.availableVersionCode(),
                    updatePriority = info.updatePriority(),
                    isImmediatePreferred = false,
                    isFlexibleAllowed = info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE),
                    isImmediateAllowed = info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
                )
            }
        }
    }

    fun completeFlexibleUpdate() {
        _state.value = PlayUpdateUiState.Installing
        appUpdateManager.completeUpdate()
    }

    fun unregisterListener() {
        if (listenerRegistered) {
            runCatching { appUpdateManager.unregisterListener(installListener) }
            listenerRegistered = false
        }
    }
}
