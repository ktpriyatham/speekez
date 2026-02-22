package com.speekez.api

import com.speekez.api.model.OpenRouterRefinementChoice
import com.speekez.api.model.OpenRouterRefinementMessage
import com.speekez.api.model.OpenRouterRefinementResponse
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import retrofit2.HttpException
import retrofit2.Response

class OpenRouterClaudeClientTest {

    private val api = mockk<OpenRouterRefinementApi>()
    private val client = OpenRouterClaudeClient("test-key", api)

    @Test
    fun `refine returns expected text`() = runTest {
        val rawText = "hello"
        val systemPrompt = "be nice"
        val refinedText = "Hello!"

        val response = OpenRouterRefinementResponse(
            choices = listOf(
                OpenRouterRefinementChoice(
                    message = OpenRouterRefinementMessage(role = "assistant", content = refinedText)
                )
            )
        )

        coEvery { api.refine(any(), any()) } returns response

        val result = client.refine(rawText, "claude-3", systemPrompt)

        assertEquals(refinedText, result)
    }

    @Test
    fun `validateKey returns true on success`() = runTest {
        val response = OpenRouterRefinementResponse(choices = emptyList())
        coEvery { api.refine(any(), any()) } returns response

        val result = client.validateKey("claude-3")
        assertEquals(true, result)
    }

    @Test
    fun `validateKey returns false on 401`() = runTest {
        val exception = HttpException(Response.error<Any>(401, "".toResponseBody(null)))
        coEvery { api.refine(any(), any()) } throws exception

        val result = client.validateKey("claude-3")
        assertEquals(false, result)
    }
}
