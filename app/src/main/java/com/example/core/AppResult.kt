package com.example.core

import kotlinx.coroutines.CancellationException

sealed interface AppResult<out T> {
    data object Loading : AppResult<Nothing>
    data class Success<T>(val data: T) : AppResult<T>
    data class Error(
        val message: String,
        val throwable: Throwable? = null,
        val isRetryable: Boolean = true
    ) : AppResult<Nothing>
}

inline fun <T> runAppCatching(
    defaultErrorMessage: String = "Operation failed",
    block: () -> T
): AppResult<T> = try {
    AppResult.Success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    AppResult.Error(
        message = e.localizedMessage?.takeIf { it.isNotBlank() } ?: defaultErrorMessage,
        throwable = e,
        isRetryable = true
    )
}
