package com.speekez.api

import android.content.Context
import com.speekez.core.ApiMode
import com.speekez.core.ModelTier
import com.speekez.security.EncryptedPreferencesManager
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class ApiRouterManager(
    private val context: Context,
    private val prefs: EncryptedPreferencesManager
) {
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val openRouterRetrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://openrouter.ai/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private val openAiRetrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.openai.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private val groqRetrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.groq.com/openai/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    fun getSttClient(): SttClient? {
        val mode = prefs.getApiMode()
        return when (mode) {
            ApiMode.OPENROUTER -> {
                // Prefer Groq Whisper for speed if key is available
                val groqKey = prefs.getGroqKey()
                if (groqKey != null) {
                    OpenAiWhisperClient(groqRetrofit.create(OpenAiApi::class.java), groqKey)
                } else {
                    // Fallback to OpenRouter chat completions (slower)
                    val key = prefs.getOpenRouterKey() ?: return null
                    OpenRouterAudioClient(openRouterRetrofit.create(OpenRouterApi::class.java), key)
                }
            }
            ApiMode.SEPARATE -> {
                val key = prefs.getOpenAiKey() ?: return null
                OpenAiWhisperClient(openAiRetrofit.create(OpenAiApi::class.java), key)
            }
            ApiMode.NO_KEYS -> null
        }
    }

    fun getRefinementClient(): RefinementClient? {
        val mode = prefs.getApiMode()
        return when (mode) {
            ApiMode.OPENROUTER -> {
                val key = prefs.getOpenRouterKey() ?: return null
                OpenRouterClaudeClient(key)
            }
            ApiMode.SEPARATE -> {
                val key = prefs.getAnthropicKey() ?: return null
                AnthropicClaudeClient(key)
            }
            ApiMode.NO_KEYS -> null
        }
    }

    fun getApiMode(): ApiMode = prefs.getApiMode()

    fun hasGroqKey(): Boolean = prefs.hasGroqKey()

    fun getSttModel(tier: ModelTier): String {
        val mode = prefs.getApiMode()
        val hasGroq = prefs.hasGroqKey()
        return when (mode) {
            ApiMode.OPENROUTER -> {
                if (hasGroq) {
                    // Groq Whisper — same model regardless of tier (it's already the best/fastest)
                    "whisper-large-v3-turbo"
                } else {
                    // OpenRouter chat completions fallback
                    when (tier) {
                        ModelTier.CHEAP -> "google/gemini-2.5-flash"
                        ModelTier.BEST -> "openai/gpt-4o-audio-preview"
                        ModelTier.CUSTOM -> prefs.getCustomSttModel() ?: "google/gemini-2.5-flash"
                    }
                }
            }
            ApiMode.SEPARATE -> when (tier) {
                ModelTier.CHEAP -> "gpt-4o-mini-transcribe"
                ModelTier.BEST -> "gpt-4o-transcribe"
                ModelTier.CUSTOM -> prefs.getCustomSttModel() ?: "gpt-4o-mini-transcribe"
            }
            ApiMode.NO_KEYS -> ""
        }
    }

    fun getRefinementModel(tier: ModelTier): String {
        val mode = prefs.getApiMode()
        return when (tier) {
            ModelTier.CHEAP -> when (mode) {
                ApiMode.OPENROUTER -> "anthropic/claude-haiku-4-5"
                ApiMode.SEPARATE -> "claude-haiku-4-5-20251001"
                ApiMode.NO_KEYS -> ""
            }
            ModelTier.BEST -> when (mode) {
                ApiMode.OPENROUTER -> "anthropic/claude-sonnet-4-5"
                ApiMode.SEPARATE -> "claude-sonnet-4-5-20250929"
                ApiMode.NO_KEYS -> ""
            }
            ModelTier.CUSTOM -> when (mode) {
                ApiMode.OPENROUTER -> prefs.getCustomRefinementModel() ?: "anthropic/claude-haiku-4-5"
                ApiMode.SEPARATE -> prefs.getCustomRefinementModel() ?: "claude-haiku-4-5-20251001"
                ApiMode.NO_KEYS -> ""
            }
        }
    }
}
