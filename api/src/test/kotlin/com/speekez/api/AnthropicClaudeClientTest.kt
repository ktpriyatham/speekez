package com.speekez.api

import com.speekez.api.model.AnthropicContent
import com.speekez.api.model.AnthropicResponse
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import retrofit2.HttpException
import retrofit2.Response

class AnthropicClaudeClientTest {

    private val api = mockk<AnthropicApi>()
    private val client = AnthropicClaudeClient("test-key", api)

    @Test
    fun `refine returns expected text`() = runTest {
        val rawText = "hello"
        val systemPrompt = "be nice"
        val refinedText = "Hello!"

        val response = AnthropicResponse(
            content = listOf(
                AnthropicContent(type = "text", text = refinedText)
            )
        )

        coEvery { api.refine(any(), any(), any()) } returns response

        val result = client.refine(rawText, "claude-3", systemPrompt)

        assertEquals(refinedText, result)
    }

    @Test
    fun `validateKey returns true on success`() = runTest {
        val response = AnthropicResponse(content = emptyList())
        coEvery { api.refine(any(), any(), any()) } returns response

        val result = client.validateKey("claude-3")
        assertEquals(true, result)
    }

    @Test
    fun `validateKey returns false on 403`() = runTest {
        val exception = HttpException(Response.error<Any>(403, "".toResponseBody(null)))
        coEvery { api.refine(any(), any(), any()) } throws exception

        val result = client.validateKey("claude-3")
        assertEquals(false, result)
    }
}
