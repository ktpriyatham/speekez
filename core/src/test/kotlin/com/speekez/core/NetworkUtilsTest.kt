package com.speekez.core

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class NetworkUtilsTest {

    @Test
    fun `isOnline returns true when WiFi is connected`() {
        val context = mockk<Context>()
        val connectivityManager = mockk<ConnectivityManager>()
        val network = mockk<Network>()
        val capabilities = mockk<NetworkCapabilities>()

        every { context.getSystemService(Context.CONNECTIVITY_SERVICE) } returns connectivityManager
        every { connectivityManager.activeNetwork } returns network
        every { connectivityManager.getNetworkCapabilities(network) } returns capabilities
        every { capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) } returns true
        every { capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) } returns false
        every { capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) } returns false

        assertTrue(NetworkUtils.isOnline(context))
    }

    @Test
    fun `isOnline returns true when Cellular is connected`() {
        val context = mockk<Context>()
        val connectivityManager = mockk<ConnectivityManager>()
        val network = mockk<Network>()
        val capabilities = mockk<NetworkCapabilities>()

        every { context.getSystemService(Context.CONNECTIVITY_SERVICE) } returns connectivityManager
        every { connectivityManager.activeNetwork } returns network
        every { connectivityManager.getNetworkCapabilities(network) } returns capabilities
        every { capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) } returns false
        every { capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) } returns true
        every { capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) } returns false

        assertTrue(NetworkUtils.isOnline(context))
    }

    @Test
    fun `isOnline returns false when no network is active`() {
        val context = mockk<Context>()
        val connectivityManager = mockk<ConnectivityManager>()

        every { context.getSystemService(Context.CONNECTIVITY_SERVICE) } returns connectivityManager
        every { connectivityManager.activeNetwork } returns null

        assertFalse(NetworkUtils.isOnline(context))
    }

    @Test
    fun `safeApiCall returns success on first try`() = runTest {
        val result = NetworkUtils.safeApiCall {
            "Success"
        }
        assertTrue(result.isSuccess)
        assertEquals("Success", result.getOrNull())
    }

    @Test
    fun `safeApiCall retries on IOException and eventually succeeds`() = runTest {
        var attempts = 0
        val result = NetworkUtils.safeApiCall {
            attempts++
            if (attempts < 3) throw IOException("Network error")
            "Success"
        }
        assertTrue(result.isSuccess)
        assertEquals("Success", result.getOrNull())
        assertEquals(3, attempts)
    }

    @Test
    fun `safeApiCall retries on IOException and fails after max retries`() = runTest {
        var attempts = 0
        val result = NetworkUtils.safeApiCall {
            attempts++
            throw IOException("Persistent network error")
        }
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IOException)
        assertEquals(4, attempts) // Initial + 3 retries
    }

    @Test
    fun `safeApiCall retries on HTTP 500 and succeeds`() = runTest {
        var attempts = 0
        val response = Response.error<String>(500, okhttp3.ResponseBody.create(null, ""))
        val httpException = HttpException(response)

        val result = NetworkUtils.safeApiCall {
            attempts++
            if (attempts < 2) throw httpException
            "Success"
        }
        assertTrue(result.isSuccess)
        assertEquals("Success", result.getOrNull())
        assertEquals(2, attempts)
    }

    @Test
    fun `safeApiCall does not retry on HTTP 401`() = runTest {
        var attempts = 0
        val response = Response.error<String>(401, okhttp3.ResponseBody.create(null, ""))
        val httpException = HttpException(response)

        val result = NetworkUtils.safeApiCall {
            attempts++
            throw httpException
        }
        assertTrue(result.isFailure)
        assertEquals(1, attempts)
    }

    @Test
    fun `safeApiCall does not retry on HTTP 402`() = runTest {
        var attempts = 0
        val response = Response.error<String>(402, okhttp3.ResponseBody.create(null, ""))
        val httpException = HttpException(response)

        val result = NetworkUtils.safeApiCall {
            attempts++
            throw httpException
        }
        assertTrue(result.isFailure)
        assertEquals(1, attempts)
    }

    @Test
    fun `safeApiCall does not retry on HTTP 429`() = runTest {
        var attempts = 0
        val response = Response.error<String>(429, okhttp3.ResponseBody.create(null, ""))
        val httpException = HttpException(response)

        val result = NetworkUtils.safeApiCall {
            attempts++
            throw httpException
        }
        assertTrue(result.isFailure)
        assertEquals(1, attempts)
    }

    @Test
    fun `safeApiCall does not retry on generic Exception`() = runTest {
        var attempts = 0
        val result = NetworkUtils.safeApiCall {
            attempts++
            throw RuntimeException("Generic error")
        }
        assertTrue(result.isFailure)
        assertEquals(1, attempts)
    }

    @Test
    fun `safeApiCall rethrows CancellationException`() = runTest {
        try {
            NetworkUtils.safeApiCall<String> {
                throw kotlinx.coroutines.CancellationException("Cancelled")
            }
            org.junit.jupiter.api.Assertions.fail("Should have thrown CancellationException")
        } catch (e: kotlinx.coroutines.CancellationException) {
            assertEquals("Cancelled", e.message)
        }
    }
}
