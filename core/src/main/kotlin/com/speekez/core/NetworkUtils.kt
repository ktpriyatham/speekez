package com.speekez.core

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.coroutines.delay
import retrofit2.HttpException
import java.io.IOException

object NetworkUtils {

    /**
     * Checks if the device has an active network connection.
     * Checks for WiFi, Cellular, or Ethernet.
     */
    fun isOnline(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }

    /**
     * Executes a suspendable API call and returns a [Result] wrapping the result.
     *
     * Retries on [IOException] and HTTP 5xx errors with exponential backoff:
     * 1s, 2s, 4s delays, max 3 retries.
     *
     * Does NOT retry on 401 (Unauthorized), 402 (Payment Required), or 429 (Too Many Requests).
     */
    suspend fun <T> safeApiCall(
        call: suspend () -> T
    ): Result<T> {
        var currentDelay = 1000L
        val maxRetries = 3

        for (attempt in 0..maxRetries) {
            try {
                return Result.success(call())
            } catch (e: IOException) {
                if (attempt == maxRetries) return Result.failure(e)
                delay(currentDelay)
                currentDelay *= 2
            } catch (e: HttpException) {
                val code = e.code()
                // Retry only on HTTP 5xx as per requirements.
                // Explicitly not retrying on 401, 402, 429 (which are already not 5xx).
                if (code in 500..599) {
                    if (attempt == maxRetries) return Result.failure(e)
                    delay(currentDelay)
                    currentDelay *= 2
                } else {
                    return Result.failure(e)
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                // For all other exceptions, do not retry and return failure immediately.
                return Result.failure(e)
            }
        }
        return Result.failure(IllegalStateException("Unknown error in safeApiCall"))
    }
}
