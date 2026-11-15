package com.example.platform

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.core.content.pm.PackageInfoCompat
import com.example.data.model.ApkInstallUiState
import com.example.data.model.AppRelease
import com.example.domain.AppUpdateEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

data class ApkVerificationResult(
    val isValid: Boolean,
    val certSha256: String = "",
    val fileSha256: String = "",
    val failureReason: String = ""
)

class ApkUpdateInstaller(
    private val context: Context,
    private val appInfoProvider: InstalledAppInfoProvider = AndroidInstalledAppInfoProvider(context)
) {
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val _state = MutableStateFlow<ApkInstallUiState>(ApkInstallUiState.Idle)
    val state: StateFlow<ApkInstallUiState> = _state.asStateFlow()

    suspend fun downloadAndVerifyApk(
        release: AppRelease,
        trustedHosts: List<String>
    ): ApkInstallUiState = withContext(Dispatchers.IO) {
        val url = release.apkDownloadUrl.trim()
        if (!AppUpdateEngine.isTrustedApkUrl(url, trustedHosts)) {
            val failed = ApkInstallUiState.Failed(
                "Blocked APK download: release URL is missing, insecure (non-HTTPS), or not from a trusted host."
            )
            _state.value = failed
            return@withContext failed
        }

        val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
        // Clean stale APKs from older checks
        updatesDir.listFiles()?.forEach { existing ->
            if (existing.isFile && existing.name.endsWith(".apk")) {
                existing.delete()
            }
        }
        val targetFile = File(updatesDir, "fittrack-v${release.versionCode}.apk")

        try {
            _state.value = ApkInstallUiState.Downloading(0f, 0L, 0L)
            val request = Request.Builder().url(url).get().build()
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val failed = ApkInstallUiState.Failed("APK download failed with HTTP ${response.code}.")
                    _state.value = failed
                    return@withContext failed
                }
                val body = response.body ?: run {
                    val failed = ApkInstallUiState.Failed("Empty APK response body.")
                    _state.value = failed
                    return@withContext failed
                }
                val contentLength = body.contentLength()
                val digest = MessageDigest.getInstance("SHA-256")
                var totalRead = 0L
                val buffer = ByteArray(16 * 1024)

                body.byteStream().use { input ->
                    FileOutputStream(targetFile).use { output ->
                        while (true) {
                            ensureActive()
                            val read = input.read(buffer)
                            if (read == -1) break
                            output.write(buffer, 0, read)
                            digest.update(buffer, 0, read)
                            totalRead += read
                            val progress = if (contentLength > 0L) {
                                (totalRead.toFloat() / contentLength.toFloat()).coerceIn(0f, 1f)
                            } else {
                                0f
                            }
                            _state.value = ApkInstallUiState.Downloading(progress, totalRead, contentLength)
                        }
                        output.flush()
                    }
                }

                val actualFileSha256 = digest.digest().joinToString("") { "%02x".format(it) }
                _state.value = ApkInstallUiState.Verifying

                val verification = verifyDownloadedApk(
                    apkFile = targetFile,
                    actualFileSha256 = actualFileSha256,
                    release = release
                )
                if (!verification.isValid) {
                    targetFile.delete()
                    val failed = ApkInstallUiState.Failed(verification.failureReason)
                    _state.value = failed
                    return@withContext failed
                }

                val nextState = if (!appInfoProvider.canRequestPackageInstalls()) {
                    ApkInstallUiState.NeedsUnknownSourcesPermission(
                        apkFilePath = targetFile.absolutePath,
                        targetVersionName = release.versionName
                    )
                } else {
                    ApkInstallUiState.VerifiedReady(
                        apkFilePath = targetFile.absolutePath,
                        certSha256 = verification.certSha256,
                        targetVersionName = release.versionName
                    )
                }
                _state.value = nextState
                nextState
            }
        } catch (e: Exception) {
            targetFile.delete()
            val failed = ApkInstallUiState.Failed(
                e.localizedMessage ?: "Failed to download and verify update APK."
            )
            _state.value = failed
            failed
        }
    }

    fun verifyDownloadedApk(
        apkFile: File,
        actualFileSha256: String,
        release: AppRelease
    ): ApkVerificationResult {
        if (!apkFile.exists() || apkFile.length() == 0L) {
            return ApkVerificationResult(false, failureReason = "Downloaded APK file is missing or empty.")
        }

        if (!AppUpdateEngine.verifySha256Digest(actualFileSha256, release.apkSha256)) {
            return ApkVerificationResult(
                false,
                fileSha256 = actualFileSha256,
                failureReason = "APK SHA-256 checksum verification failed. File may be tampered or corrupted."
            )
        }

        val pm = context.packageManager
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            @Suppress("DEPRECATION")
            PackageManager.GET_SIGNATURES
        }
        val archiveInfo = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageArchiveInfo(
                    apkFile.absolutePath,
                    PackageManager.PackageInfoFlags.of(flags.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageArchiveInfo(apkFile.absolutePath, flags)
            }
        }.getOrNull() ?: return ApkVerificationResult(
            false,
            fileSha256 = actualFileSha256,
            failureReason = "Downloaded file is not a valid Android APK archive."
        )

        val installedSnapshot = appInfoProvider.getInstalledAppSnapshot()
        if (archiveInfo.packageName != installedSnapshot.packageName) {
            return ApkVerificationResult(
                false,
                fileSha256 = actualFileSha256,
                failureReason = "Package ID mismatch (${archiveInfo.packageName} != ${installedSnapshot.packageName})."
            )
        }

        val archiveVersionCode = PackageInfoCompat.getLongVersionCode(archiveInfo)
        if (archiveVersionCode < installedSnapshot.versionCode) {
            return ApkVerificationResult(
                false,
                fileSha256 = actualFileSha256,
                failureReason = "Version downgrade blocked (archive $archiveVersionCode < installed ${installedSnapshot.versionCode})."
            )
        }

        val apkCerts = AndroidInstalledAppInfoProvider.extractSignatureSha256Digests(archiveInfo)
        val certsValid = AppUpdateEngine.verifySigningCertificates(
            apkCertSha256Digests = apkCerts,
            installedCertSha256Digests = installedSnapshot.signingCertSha256Digests,
            expectedRemoteCertSha256 = release.expectedSigningCertSha256
        )
        if (!certsValid) {
            return ApkVerificationResult(
                false,
                fileSha256 = actualFileSha256,
                failureReason = "APK signing certificate does not match FitTrack's verified release signature."
            )
        }

        return ApkVerificationResult(
            isValid = true,
            certSha256 = apkCerts.firstOrNull().orEmpty(),
            fileSha256 = actualFileSha256
        )
    }

    fun createUnknownSourcesPermissionIntent(): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } else {
            Intent(Settings.ACTION_SECURITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
    }

    fun launchSystemInstaller(apkFilePath: String, targetVersionName: String): Boolean {
        val file = File(apkFilePath)
        if (!file.exists()) {
            _state.value = ApkInstallUiState.Failed("Verified APK file is no longer available.")
            return false
        }
        if (!appInfoProvider.canRequestPackageInstalls()) {
            _state.value = ApkInstallUiState.NeedsUnknownSourcesPermission(apkFilePath, targetVersionName)
            return false
        }
        return runCatching {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(installIntent)
            // Do NOT claim installation completed; only confirm Android's installer sheet was launched.
            _state.value = ApkInstallUiState.InstallerLaunched(targetVersionName)
            true
        }.getOrElse { err ->
            _state.value = ApkInstallUiState.Failed(
                err.localizedMessage ?: "Failed to launch Android package installer."
            )
            false
        }
    }

    fun refreshInstallPermissionState() {
        val current = _state.value
        if (current is ApkInstallUiState.NeedsUnknownSourcesPermission && appInfoProvider.canRequestPackageInstalls()) {
            _state.value = ApkInstallUiState.VerifiedReady(
                apkFilePath = current.apkFilePath,
                certSha256 = "",
                targetVersionName = current.targetVersionName
            )
        }
    }

    fun reset() {
        _state.value = ApkInstallUiState.Idle
    }
}
