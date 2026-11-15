package com.example.domain

import com.example.data.model.AppRelease
import com.example.data.model.LocalUpdateState
import com.example.data.model.UpdateNotificationPreferences
import com.example.data.model.UpdatePolicyConfig
import com.example.data.model.UpdateSeverity
import java.net.URI
import java.security.MessageDigest
import java.util.Locale

data class PostUpdateEvaluation(
    val isFirstInstall: Boolean,
    val isNewlyUpdated: Boolean,
    val shouldShowWhatsNewOnce: Boolean,
    val targetVersionCodeToRecord: Long,
    val targetVersionNameToRecord: String
)

data class NotificationDispatchDecision(
    val shouldNotify: Boolean,
    val isCritical: Boolean,
    val reason: String
)

object AppUpdateEngine {

    /**
     * Compares two semantic version strings (e.g., "1.2.10" vs "1.2.2").
     * Strips leading 'v' and pre-release/build suffixes before numeric comparison.
     */
    fun compareVersionNames(a: String, b: String): Int {
        val partsA = parseSemanticParts(a)
        val partsB = parseSemanticParts(b)
        val maxLen = maxOf(partsA.size, partsB.size)
        for (i in 0 until maxLen) {
            val va = partsA.getOrElse(i) { 0L }
            val vb = partsB.getOrElse(i) { 0L }
            if (va != vb) return va.compareTo(vb)
        }
        return 0
    }

    private fun parseSemanticParts(raw: String): List<Long> {
        val cleaned = raw.trim()
            .removePrefix("v")
            .removePrefix("V")
            .substringBefore('-')
            .substringBefore('+')
        if (cleaned.isBlank()) return listOf(0L)
        return cleaned.split('.').map { token ->
            token.takeWhile { it.isDigit() }.toLongOrNull() ?: 0L
        }
    }

    fun isRemoteReleaseNewer(
        installedVersionCode: Long,
        installedVersionName: String,
        remote: AppRelease
    ): Boolean {
        if (!remote.isPublished || remote.versionCode <= 0L) return false
        if (remote.versionCode != installedVersionCode) {
            return remote.versionCode > installedVersionCode
        }
        return compareVersionNames(remote.versionName, installedVersionName) > 0
    }

    fun classifyUpdateSeverity(
        installedVersionCode: Long,
        installedVersionName: String,
        latestRelease: AppRelease?,
        policy: UpdatePolicyConfig
    ): UpdateSeverity {
        if (latestRelease == null || !isRemoteReleaseNewer(installedVersionCode, installedVersionName, latestRelease)) {
            if (policy.minSupportedVersionCode > 0L && installedVersionCode < policy.minSupportedVersionCode) {
                return UpdateSeverity.CRITICAL
            }
            return UpdateSeverity.NONE
        }
        val effectiveMinSupported = maxOf(
            latestRelease.minSupportedVersionCode,
            policy.minSupportedVersionCode
        )
        if (effectiveMinSupported > 0L && installedVersionCode < effectiveMinSupported) {
            return UpdateSeverity.CRITICAL
        }
        return when (latestRelease.severity.lowercase(Locale.US)) {
            "critical" -> UpdateSeverity.CRITICAL
            "recommended" -> UpdateSeverity.RECOMMENDED
            "optional" -> UpdateSeverity.OPTIONAL
            else -> UpdateSeverity.OPTIONAL
        }
    }

    fun shouldRunAutomaticCheck(
        nowMs: Long,
        lastCheckMs: Long,
        intervalMinutes: Long,
        force: Boolean
    ): Boolean {
        if (force) return true
        if (lastCheckMs <= 0L) return true
        val clampedMinutes = intervalMinutes.coerceIn(5L, 10080L)
        val elapsedMs = (nowMs - lastCheckMs).coerceAtLeast(0L)
        return elapsedMs >= clampedMinutes * 60_000L
    }

    fun evaluateLaunchVersionState(
        installedVersionCode: Long,
        installedVersionName: String,
        state: LocalUpdateState
    ): PostUpdateEvaluation {
        if (state.lastSeenVersionCode <= 0L) {
            // Fresh first installation: establish baseline without showing an unwanted "updated" dialog.
            return PostUpdateEvaluation(
                isFirstInstall = true,
                isNewlyUpdated = false,
                shouldShowWhatsNewOnce = false,
                targetVersionCodeToRecord = installedVersionCode,
                targetVersionNameToRecord = installedVersionName
            )
        }

        val isUpgrade = installedVersionCode > state.lastSeenVersionCode ||
            (installedVersionCode == state.lastSeenVersionCode &&
                compareVersionNames(installedVersionName, state.lastSeenVersionName) > 0)

        if (isUpgrade) {
            val alreadyShown = state.shownWhatsNewVersionCode >= installedVersionCode
            val alreadyMarkedRead = installedVersionCode in state.readVersionCodes
            return PostUpdateEvaluation(
                isFirstInstall = false,
                isNewlyUpdated = true,
                shouldShowWhatsNewOnce = !alreadyShown && !alreadyMarkedRead,
                targetVersionCodeToRecord = installedVersionCode,
                targetVersionNameToRecord = installedVersionName
            )
        }

        return PostUpdateEvaluation(
            isFirstInstall = false,
            isNewlyUpdated = false,
            shouldShowWhatsNewOnce = false,
            targetVersionCodeToRecord = installedVersionCode,
            targetVersionNameToRecord = installedVersionName
        )
    }

    fun evaluateNotificationDispatch(
        severity: UpdateSeverity,
        release: AppRelease?,
        state: LocalUpdateState,
        preferences: UpdateNotificationPreferences,
        hasSystemPermission: Boolean
    ): NotificationDispatchDecision {
        if (release == null || severity == UpdateSeverity.NONE) {
            return NotificationDispatchDecision(false, false, "No actionable update")
        }
        if (!hasSystemPermission) {
            return NotificationDispatchDecision(false, severity == UpdateSeverity.CRITICAL, "Notification permission denied")
        }
        return when (severity) {
            UpdateSeverity.CRITICAL -> {
                if (!preferences.criticalUpdatesEnabled) {
                    NotificationDispatchDecision(false, true, "Critical update notifications disabled")
                } else if (state.lastNotifiedCriticalVersionCode == release.versionCode) {
                    NotificationDispatchDecision(false, true, "Duplicate critical notification suppressed")
                } else {
                    NotificationDispatchDecision(true, true, "Dispatch critical update alert")
                }
            }
            UpdateSeverity.OPTIONAL, UpdateSeverity.RECOMMENDED -> {
                if (!preferences.optionalUpdatesEnabled) {
                    NotificationDispatchDecision(false, false, "Optional update notifications disabled")
                } else if (
                    state.lastNotifiedOptionalVersionCode == release.versionCode ||
                    state.lastNotifiedCriticalVersionCode == release.versionCode
                ) {
                    NotificationDispatchDecision(false, false, "Duplicate optional notification suppressed")
                } else {
                    NotificationDispatchDecision(true, false, "Dispatch optional update alert")
                }
            }
            UpdateSeverity.NONE -> NotificationDispatchDecision(false, false, "No update")
        }
    }

    fun isTrustedApkUrl(
        url: String,
        trustedHosts: List<String> = emptyList()
    ): Boolean {
        val trimmed = url.trim()
        if (trimmed.isBlank()) return false
        val uri = runCatching { URI(trimmed) }.getOrNull() ?: return false
        if (!uri.scheme.equals("https", ignoreCase = true)) return false
        if (!uri.userInfo.isNullOrBlank()) return false
        val host = uri.host?.lowercase(Locale.US)?.takeIf { it.isNotBlank() } ?: return false
        if (host == "localhost" || host == "127.0.0.1" || host == "0.0.0.0") return false

        val normalizedAllowlist = trustedHosts
            .map { it.trim().lowercase(Locale.US) }
            .filter { it.isNotBlank() }
        if (normalizedAllowlist.isEmpty()) {
            // Require explicit allowlisted host policy when configured; otherwise only permit HTTPS with non-empty host.
            return true
        }
        return normalizedAllowlist.any { allowed ->
            host == allowed || host.endsWith(".$allowed")
        }
    }

    fun normalizeSha256(raw: String): String {
        return raw.trim()
            .replace(":", "")
            .replace(" ", "")
            .lowercase(Locale.US)
    }

    fun sha256Hex(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun verifySha256Digest(actualHex: String, expectedHex: String): Boolean {
        val normExpected = normalizeSha256(expectedHex)
        if (normExpected.isBlank()) return true
        if (normExpected.length != 64 || !normExpected.all { it in '0'..'9' || it in 'a'..'f' }) {
            return false
        }
        val normActual = normalizeSha256(actualHex)
        return normActual == normExpected
    }

    fun verifySigningCertificates(
        apkCertSha256Digests: Set<String>,
        installedCertSha256Digests: Set<String>,
        expectedRemoteCertSha256: String
    ): Boolean {
        val normalizedApk = apkCertSha256Digests.map(::normalizeSha256).filter { it.length == 64 }.toSet()
        if (normalizedApk.isEmpty()) return false

        val normalizedInstalled = installedCertSha256Digests.map(::normalizeSha256).filter { it.length == 64 }.toSet()
        if (normalizedInstalled.isNotEmpty() && normalizedApk.intersect(normalizedInstalled).isEmpty()) {
            return false
        }

        val normRemote = normalizeSha256(expectedRemoteCertSha256)
        if (normRemote.isNotBlank()) {
            if (normRemote.length != 64 || normRemote !in normalizedApk) {
                return false
            }
        }
        return true
    }
}
