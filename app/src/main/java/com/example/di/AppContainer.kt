package com.example.di

import android.content.Context
import com.example.core.DefaultDispatcherProvider
import com.example.core.DispatcherProvider
import com.example.data.ai.FitTrackAiService
import com.example.data.firebase.AppUpdateRepository
import com.example.data.firebase.FitTrackRepository
import com.example.platform.AndroidInstalledAppInfoProvider
import com.example.platform.ApkUpdateInstaller
import com.example.platform.ConnectivityMonitor
import com.example.platform.GpsTrackerStore
import com.example.platform.InstalledAppInfoProvider
import com.example.platform.PlayInAppUpdateManager
import com.example.platform.RestTimerAndSessionStore
import com.example.platform.StepCounterManager
import com.example.platform.UpdateNotificationManager
import com.example.platform.UpdatePreferencesStore
import com.example.platform.UserPreferencesStore
import com.example.ui.AppUpdateViewModel
import com.example.ui.FitTrackViewModel

class AppContainer(context: Context) {

    val appContext: Context = context.applicationContext

    init {
        GpsTrackerStore.initialize(appContext)
    }
    val dispatchers: DispatcherProvider = DefaultDispatcherProvider
    val connectivityMonitor: ConnectivityMonitor by lazy { ConnectivityMonitor(appContext) }
    val aiService: FitTrackAiService by lazy { FitTrackAiService() }
    val userPreferencesStore: UserPreferencesStore by lazy { UserPreferencesStore(appContext) }
    val restSessionStore: RestTimerAndSessionStore by lazy { RestTimerAndSessionStore(appContext) }
    val stepCounterManager: StepCounterManager by lazy { StepCounterManager(appContext) }
    val fitTrackRepository: FitTrackRepository by lazy { FitTrackRepository(appContext) }
    val updatePreferencesStore: UpdatePreferencesStore by lazy { UpdatePreferencesStore(appContext) }
    val updateRepository: AppUpdateRepository by lazy { AppUpdateRepository(appContext) }
    val installedAppInfoProvider: InstalledAppInfoProvider by lazy { AndroidInstalledAppInfoProvider(appContext) }
    val updateNotificationManager: UpdateNotificationManager by lazy {
        UpdateNotificationManager(appContext, installedAppInfoProvider)
    }

    fun createFitTrackViewModel(
        userId: String,
        userEmail: String,
        userDisplayName: String,
        userPhotoUrl: String
    ): FitTrackViewModel {
        return FitTrackViewModel(
            repository = fitTrackRepository,
            aiService = aiService,
            stepManager = stepCounterManager,
            restSessionStore = restSessionStore,
            userPreferencesStore = userPreferencesStore,
            connectivityMonitor = connectivityMonitor,
            dispatchers = dispatchers,
            currentUserId = userId,
            userEmail = userEmail,
            userDisplayName = userDisplayName,
            userPhotoUrl = userPhotoUrl
        )
    }

    fun createAppUpdateViewModel(): AppUpdateViewModel {
        return AppUpdateViewModel(
            metadataFetcher = { preferServer -> updateRepository.fetchUpdateMetadata(preferServer) },
            stateStore = updatePreferencesStore,
            appInfoProvider = installedAppInfoProvider,
            notifier = { release, severity -> updateNotificationManager.postUpdateNotification(release, severity) },
            playUpdateManager = PlayInAppUpdateManager(appContext),
            apkInstaller = ApkUpdateInstaller(appContext, installedAppInfoProvider)
        )
    }

    companion object {
        @Volatile
        private var instance: AppContainer? = null

        fun getInstance(context: Context): AppContainer {
            return instance ?: synchronized(this) {
                instance ?: AppContainer(context).also { instance = it }
            }
        }
    }
}
