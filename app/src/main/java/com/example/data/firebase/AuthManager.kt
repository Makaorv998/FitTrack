package com.example.data.firebase

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

object AuthManager {

    private const val TAG = "FitTrackAuth"

    fun attemptAutoSignIn(
        context: Context,
        credentialManager: CredentialManager,
        scope: CoroutineScope,
        onAuthSuccess: () -> Unit,
        onUnauthenticated: () -> Unit
    ) {
        if (Firebase.auth.currentUser != null) {
            onAuthSuccess()
            return
        }
        val clientIdRes = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        val clientId = if (clientIdRes != 0) context.getString(clientIdRes) else ""
        if (clientId.isBlank()) {
            onUnauthenticated()
            return
        }

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(true)
            .setServerClientId(clientId)
            .setAutoSelectEnabled(true)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        scope.launch {
            try {
                val result = credentialManager.getCredential(context, request)
                val credential = result.credential
                if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                    val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                    Firebase.auth.signInWithCredential(authCredential).await()
                    onAuthSuccess()
                } else {
                    onUnauthenticated()
                }
            } catch (_: Exception) {
                onUnauthenticated()
            }
        }
    }

    fun onGoogleSignInClicked(
        context: Context,
        credentialManager: CredentialManager,
        scope: CoroutineScope,
        onAuthSuccess: () -> Unit,
        onAuthError: (String) -> Unit,
        onAuthCancelled: () -> Unit
    ) {
        val clientIdRes = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        val clientId = if (clientIdRes != 0) context.getString(clientIdRes) else ""
        if (clientId.isBlank()) {
            onAuthError("Google Sign-In configuration missing: default_web_client_id not found")
            return
        }

        val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(signInOption)
            .build()

        scope.launch {
            try {
                val activity = context as? Activity
                val result = credentialManager.getCredential(activity ?: context, request)
                val credential = result.credential
                if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                    val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                    Firebase.auth.signInWithCredential(authCredential).await()
                    onAuthSuccess()
                } else {
                    onAuthError("Unexpected credential type returned.")
                }
            } catch (e: GetCredentialCancellationException) {
                Log.w(TAG, "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
                onAuthCancelled()
            } catch (e: Exception) {
                Log.e(TAG, "Google Sign-In failed", e)
                onAuthError(e.localizedMessage ?: "Sign-in failed. Please try again.")
            }
        }
    }

    fun signOut(
        credentialManager: CredentialManager,
        scope: CoroutineScope,
        onSignOutComplete: () -> Unit = {}
    ) {
        scope.launch {
            withContext(NonCancellable) {
                try {
                    credentialManager.clearCredentialState(ClearCredentialStateRequest())
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to clear credential state: ${e.message}")
                } finally {
                    runCatching { Firebase.auth.signOut() }
                    onSignOutComplete()
                }
            }
        }
    }

    fun deleteCurrentAuthAccount(
        context: Context,
        credentialManager: CredentialManager,
        scope: CoroutineScope,
        onDeleted: () -> Unit,
        onError: (String) -> Unit
    ) {
        scope.launch {
            val user = runCatching { Firebase.auth.currentUser }.getOrNull()
            if (user == null) {
                onDeleted()
                return@launch
            }
            try {
                user.delete().await()
                runCatching { credentialManager.clearCredentialState(ClearCredentialStateRequest()) }
                runCatching { Firebase.auth.signOut() }
                onDeleted()
            } catch (e: com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException) {
                val clientIdRes = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
                val clientId = if (clientIdRes != 0) context.getString(clientIdRes) else ""
                if (clientId.isBlank()) {
                    onError("Please sign in again to delete your account.")
                    return@launch
                }
                try {
                    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
                    val request = GetCredentialRequest.Builder()
                        .addCredentialOption(signInOption)
                        .build()
                    val activity = context as? Activity
                    val result = credentialManager.getCredential(activity ?: context, request)
                    val credential = result.credential
                    if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                        val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                        val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                        user.reauthenticate(authCredential).await()
                        user.delete().await()
                        runCatching { credentialManager.clearCredentialState(ClearCredentialStateRequest()) }
                        runCatching { Firebase.auth.signOut() }
                        onDeleted()
                    } else {
                        onError("Re-authentication required to finish deleting account.")
                    }
                } catch (reauthError: GetCredentialCancellationException) {
                    Log.w(TAG, "Re-authentication cancelled during account deletion: ${reauthError.message}")
                    onError("Account deletion paused — sign in with Google again if you want to remove the Firebase Auth account.")
                } catch (reauthError: Exception) {
                    Log.e(TAG, "Account re-authentication/deletion failed", reauthError)
                    onError(reauthError.localizedMessage ?: "Failed to delete authentication account.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Firebase Auth account delete failed", e)
                onError(e.localizedMessage ?: "Failed to delete authentication account.")
            }
        }
    }
}
