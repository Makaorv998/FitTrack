package com.example.data.firebase

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.model.AppRelease
import com.example.data.model.LocalizedReleaseContent
import com.example.data.model.RemoteUpdateSnapshot
import com.example.data.model.UpdatePolicyConfig
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

class AppUpdateRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    suspend fun fetchUpdateMetadata(preferServer: Boolean = true): Result<RemoteUpdateSnapshot> {
        val source = if (preferServer) Source.SERVER else Source.DEFAULT
        return try {
            val releasesSnap = try {
                db.collection(COLLECTION_RELEASES)
                    .whereEqualTo("isPublished", true)
                    .orderBy("versionCode", Query.Direction.DESCENDING)
                    .limit(30)
                    .get(source)
                    .await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: FirebaseFirestoreException) {
                if (e.code == FirebaseFirestoreException.Code.FAILED_PRECONDITION) {
                    // Fallback without composite index if single-field query is needed on fresh project
                    db.collection(COLLECTION_RELEASES)
                        .whereEqualTo("isPublished", true)
                        .get(source)
                        .await()
                } else {
                    throw e
                }
            }

            val parsedReleases = releasesSnap.documents
                .mapNotNull { parseReleaseDocument(it) }
                .filter { it.isValidMetadata() && it.isPublished }
                .sortedByDescending { it.versionCode }

            val policySnap = runCatching {
                db.collection(COLLECTION_CONFIG)
                    .document(DOC_UPDATE_POLICY)
                    .get(source)
                    .await()
            }.getOrNull()

            val policy = if (policySnap != null && policySnap.exists()) {
                parsePolicyDocument(policySnap)
            } else {
                UpdatePolicyConfig()
            }

            Result.success(
                RemoteUpdateSnapshot(
                    releases = parsedReleases,
                    policy = policy,
                    fetchedAtMs = System.currentTimeMillis()
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, COLLECTION_RELEASES)
            Log.w(TAG, "Unable to fetch update metadata: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun isCurrentUserReleaseAdmin(): Boolean {
        val user = auth.currentUser ?: return false
        return try {
            val snap = db.collection(COLLECTION_ADMINS).document(user.uid).get().await()
            snap.exists()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            false
        }
    }

    private fun parsePolicyDocument(doc: DocumentSnapshot): UpdatePolicyConfig {
        val data = doc.data ?: return UpdatePolicyConfig()
        val interval = (data["checkIntervalMinutes"] as? Number)?.toLong()?.coerceIn(5L, 10080L) ?: 360L
        val minSupported = (data["minSupportedVersionCode"] as? Number)?.toLong()?.coerceAtLeast(0L) ?: 0L
        val latestCode = (data["latestVersionCode"] as? Number)?.toLong()?.coerceAtLeast(0L) ?: 0L
        val threshold = (data["playUpdatePriorityThreshold"] as? Number)?.toLong()?.coerceIn(0L, 5L) ?: 4L
        val channel = (data["defaultDistributionChannel"] as? String)
            ?.takeIf { it in setOf("play_store", "apk", "both") }
            ?: "both"
        val hosts = (data["trustedApkHosts"] as? List<*>)
            ?.mapNotNull { it as? String }
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() }
            .orEmpty()

        return UpdatePolicyConfig(
            checkIntervalMinutes = interval,
            minSupportedVersionCode = minSupported,
            latestVersionCode = latestCode,
            trustedApkHosts = hosts,
            defaultDistributionChannel = channel,
            playUpdatePriorityThreshold = threshold
        )
    }

    private fun parseReleaseDocument(doc: DocumentSnapshot): AppRelease? {
        val data = doc.data ?: return null
        val versionName = (data["versionName"] as? String)?.trim().orEmpty()
        val versionCode = (data["versionCode"] as? Number)?.toLong() ?: 0L
        val releaseDate = (data["releaseDate"] as? String)?.trim().orEmpty()
        val title = (data["title"] as? String)?.trim().orEmpty()
        if (versionName.isBlank() || versionCode <= 0L || releaseDate.isBlank() || title.isBlank()) {
            return null
        }

        val severity = (data["severity"] as? String)?.lowercase()?.takeIf {
            it in setOf("optional", "recommended", "critical")
        } ?: "optional"
        val channel = (data["distributionChannel"] as? String)?.lowercase()?.takeIf {
            it in setOf("play_store", "apk", "both")
        } ?: "both"

        fun stringList(key: String): List<String> =
            (data[key] as? List<*>)
                ?.mapNotNull { (it as? String)?.trim() }
                ?.filter { it.isNotBlank() }
                .orEmpty()

        val localizedMap = mutableMapOf<String, LocalizedReleaseContent>()
        val rawLocalized = data["localizedEntries"] as? Map<*, *>
        rawLocalized?.forEach { (langKey, rawContent) ->
            val lang = langKey as? String ?: return@forEach
            val map = rawContent as? Map<*, *> ?: return@forEach
            fun subList(k: String): List<String> =
                (map[k] as? List<*>)?.mapNotNull { (it as? String)?.trim() }?.filter { it.isNotBlank() }.orEmpty()
            localizedMap[lang] = LocalizedReleaseContent(
                title = (map["title"] as? String)?.trim().orEmpty(),
                summary = (map["summary"] as? String)?.trim().orEmpty(),
                newFeatures = subList("newFeatures"),
                improvements = subList("improvements"),
                bugFixes = subList("bugFixes"),
                securityChanges = subList("securityChanges")
            )
        }

        return AppRelease(
            id = doc.id,
            versionName = versionName,
            versionCode = versionCode,
            releaseDate = releaseDate,
            title = title,
            summary = (data["summary"] as? String)?.trim().orEmpty(),
            severity = severity,
            minSupportedVersionCode = (data["minSupportedVersionCode"] as? Number)?.toLong()?.coerceAtLeast(0L) ?: 0L,
            minSupportedVersionName = (data["minSupportedVersionName"] as? String)?.trim().orEmpty(),
            distributionChannel = channel,
            apkDownloadUrl = (data["apkDownloadUrl"] as? String)?.trim().orEmpty(),
            apkSha256 = (data["apkSha256"] as? String)?.trim().orEmpty(),
            expectedSigningCertSha256 = (data["expectedSigningCertSha256"] as? String)?.trim().orEmpty(),
            playStoreUrl = (data["playStoreUrl"] as? String)?.trim().orEmpty(),
            newFeatures = stringList("newFeatures"),
            improvements = stringList("improvements"),
            bugFixes = stringList("bugFixes"),
            securityChanges = stringList("securityChanges"),
            localizedEntries = localizedMap,
            isPublished = (data["isPublished"] as? Boolean) ?: false
        )
    }

    companion object {
        const val COLLECTION_RELEASES = "app_releases"
        const val COLLECTION_CONFIG = "app_config"
        const val DOC_UPDATE_POLICY = "update_policy"
        const val COLLECTION_ADMINS = "admins"
        private const val TAG = "AppUpdateRepository"
    }
}
