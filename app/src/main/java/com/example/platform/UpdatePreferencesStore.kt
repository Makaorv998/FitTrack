package com.example.platform

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.AppRelease
import com.example.data.model.LocalUpdateState
import com.example.data.model.UpdateNotificationPreferences
import com.example.data.model.UpdatePolicyConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.updateDataStore: DataStore<Preferences> by preferencesDataStore(name = "fittrack_update_store")

interface UpdateStateStore {
    fun observeState(): Flow<LocalUpdateState>
    suspend fun loadState(): LocalUpdateState
    suspend fun updateState(transform: (LocalUpdateState) -> LocalUpdateState)
    suspend fun markReleaseRead(versionCode: Long)
    suspend fun markAllReleasesRead(versionCodes: Collection<Long>)
    suspend fun markWhatsNewShown(versionCode: Long)
    suspend fun recordCheckSuccess(
        nowMs: Long,
        releases: List<AppRelease>,
        policy: UpdatePolicyConfig
    )
    suspend fun recordNotificationSent(versionCode: Long, isCritical: Boolean)
    suspend fun updateNotificationPreferences(preferences: UpdateNotificationPreferences)
}

class UpdatePreferencesStore(private val context: Context) : UpdateStateStore {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val stateKey = stringPreferencesKey("local_update_state_v1")

    override fun observeState(): Flow<LocalUpdateState> =
        context.updateDataStore.data.map { prefs ->
            prefs[stateKey]?.let { raw ->
                runCatching { json.decodeFromString<LocalUpdateState>(raw) }.getOrNull()
            } ?: LocalUpdateState()
        }

    override suspend fun loadState(): LocalUpdateState = observeState().first()

    override suspend fun updateState(transform: (LocalUpdateState) -> LocalUpdateState) {
        context.updateDataStore.edit { prefs ->
            val current = prefs[stateKey]?.let { raw ->
                runCatching { json.decodeFromString<LocalUpdateState>(raw) }.getOrNull()
            } ?: LocalUpdateState()
            val updated = transform(current)
            prefs[stateKey] = json.encodeToString(updated)
        }
    }

    override suspend fun markReleaseRead(versionCode: Long) {
        if (versionCode <= 0L) return
        updateState { current ->
            current.copy(
                readVersionCodes = current.readVersionCodes + versionCode,
                shownWhatsNewVersionCode = maxOf(current.shownWhatsNewVersionCode, versionCode)
            )
        }
    }

    override suspend fun markAllReleasesRead(versionCodes: Collection<Long>) {
        if (versionCodes.isEmpty()) return
        updateState { current ->
            val maxCode = versionCodes.maxOrNull() ?: current.shownWhatsNewVersionCode
            current.copy(
                readVersionCodes = current.readVersionCodes + versionCodes.filter { it > 0L },
                shownWhatsNewVersionCode = maxOf(current.shownWhatsNewVersionCode, maxCode)
            )
        }
    }

    override suspend fun markWhatsNewShown(versionCode: Long) {
        if (versionCode <= 0L) return
        updateState { current ->
            current.copy(
                shownWhatsNewVersionCode = maxOf(current.shownWhatsNewVersionCode, versionCode),
                lastSeenVersionCode = maxOf(current.lastSeenVersionCode, versionCode)
            )
        }
    }

    override suspend fun recordCheckSuccess(
        nowMs: Long,
        releases: List<AppRelease>,
        policy: UpdatePolicyConfig
    ) {
        updateState { current ->
            current.copy(
                lastCheckEpochMs = nowMs,
                lastSuccessfulCheckEpochMs = nowMs,
                cachedReleases = releases.take(30),
                cachedPolicy = policy
            )
        }
    }

    override suspend fun recordNotificationSent(versionCode: Long, isCritical: Boolean) {
        updateState { current ->
            if (isCritical) {
                current.copy(lastNotifiedCriticalVersionCode = versionCode)
            } else {
                current.copy(lastNotifiedOptionalVersionCode = versionCode)
            }
        }
    }

    override suspend fun updateNotificationPreferences(preferences: UpdateNotificationPreferences) {
        updateState { current ->
            current.copy(notificationPreferences = preferences)
        }
    }
}
