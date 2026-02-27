/*
 * Copyright (C) 2025 SpeekEZ
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.speekez.voice

import android.content.Context
import android.media.MediaRecorder
import android.util.Log
import com.speekez.api.ApiRouterManager
import com.speekez.api.SttClient
import com.speekez.core.AccessibilityUtils
import com.speekez.core.ModelTier
import com.speekez.core.NetworkUtils
import com.speekez.data.dao.DailyStatsDao
import com.speekez.data.dao.PresetDao
import com.speekez.data.dao.TranscriptionDao
import com.speekez.data.entity.Preset
import com.speekez.data.entity.RefinementLevel
import com.speekez.security.EncryptedPreferencesManager
import org.florisboard.lib.android.systemVibratorOrNull
import io.mockk.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class VoiceManagerTest {
    private lateinit var context: Context
    private lateinit var voiceManager: VoiceManager
    private lateinit var cacheDir: File
    private lateinit var mockPresetDao: PresetDao
    private val testDispatcher = StandardTestDispatcher()

    private val testPreset = Preset(
        id = 1,
        name = "Test",
        iconEmoji = "\uD83C\uDFA4",
        inputLanguages = listOf("en"),
        defaultInputLanguage = "en",
        outputLanguages = listOf("en"),
        defaultOutputLanguage = "en",
        refinementLevel = RefinementLevel.NONE,
        modelTier = ModelTier.CHEAP,
        systemPrompt = "",
        usageCount = 0,
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis()
    )

    /**
     * Replaces a Kotlin `by lazy` delegate field via reflection,
     * bypassing constructors that need Android framework (e.g. Keystore).
     */
    private fun <T> injectLazy(target: Any, propertyName: String, value: T) {
        val field = target::class.java.getDeclaredField("${propertyName}\$delegate")
        field.isAccessible = true
        field.set(target, lazyOf(value))
    }

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        context = mockk(relaxed = true)
        cacheDir = File("build/tmp/voice_manager_test_cache")
        cacheDir.mkdirs()
        every { context.cacheDir } returns cacheDir

        // Mock Android Log
        mockkStatic(Log::class)
        every { Log.i(any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
        every { Log.w(any<String>(), any<String>()) } returns 0

        // Mock MediaRecorder
        mockkConstructor(MediaRecorder::class)
        every { anyConstructed<MediaRecorder>().setAudioSource(any()) } just Runs
        every { anyConstructed<MediaRecorder>().setOutputFormat(any()) } just Runs
        every { anyConstructed<MediaRecorder>().setAudioEncoder(any()) } just Runs
        every { anyConstructed<MediaRecorder>().setAudioSamplingRate(any()) } just Runs
        every { anyConstructed<MediaRecorder>().setAudioChannels(any()) } just Runs
        every { anyConstructed<MediaRecorder>().setOutputFile(any<String>()) } just Runs
        every { anyConstructed<MediaRecorder>().prepare() } just Runs
        every { anyConstructed<MediaRecorder>().start() } just Runs
        every { anyConstructed<MediaRecorder>().stop() } just Runs
        every { anyConstructed<MediaRecorder>().reset() } just Runs
        every { anyConstructed<MediaRecorder>().release() } just Runs

        // Mock systemVibratorOrNull extension (VoiceHapticManager)
        mockkStatic("org.florisboard.lib.android.VibratorKt")
        every { any<Context>().systemVibratorOrNull() } returns null

        // Mock AccessibilityUtils
        mockkObject(AccessibilityUtils)
        every { AccessibilityUtils.announce(any(), any()) } just Runs

        // Mock PermissionUtils
        mockkObject(PermissionUtils)
        every { PermissionUtils.hasMicPermission(any()) } returns true

        // Mock NetworkUtils.isOnline
        mockkObject(NetworkUtils)
        every { NetworkUtils.isOnline(any()) } returns true

        // Create VoiceManager — lazy fields NOT yet evaluated
        voiceManager = VoiceManager(context)

        // Inject mock dependencies via reflection to bypass Android Keystore
        val mockPrefs = mockk<EncryptedPreferencesManager>(relaxed = true)
        injectLazy(voiceManager, "prefs", mockPrefs)

        // Relaxed SttClient so processAudio() completes without errors
        val mockSttClient = mockk<SttClient>(relaxed = true)
        coEvery { mockSttClient.transcribe(any(), any(), any()) } returns "test transcription"
        val mockApiRouter = mockk<ApiRouterManager>(relaxed = true)
        every { mockApiRouter.getSttClient() } returns mockSttClient
        every { mockApiRouter.getSttModel(any()) } returns "whisper-1"
        injectLazy(voiceManager, "apiRouterManager", mockApiRouter)

        mockPresetDao = mockk(relaxed = true)
        coEvery { mockPresetDao.getPresetById(any()) } returns testPreset
        injectLazy(voiceManager, "presetDao", mockPresetDao)

        injectLazy(voiceManager, "transcriptionDao", mockk<TranscriptionDao>(relaxed = true))
        injectLazy(voiceManager, "dailyStatsDao", mockk<DailyStatsDao>(relaxed = true))
    }

    private fun cancelVoiceManagerScope() {
        val scopeField = VoiceManager::class.java.getDeclaredField("scope")
        scopeField.isAccessible = true
        (scopeField.get(voiceManager) as CoroutineScope).cancel()
    }

    @AfterEach
    fun tearDown() {
        // Cancel VoiceManager's internal scope to stop IO coroutines from processAudio
        cancelVoiceManagerScope()
        // Allow IO coroutines to observe cancellation and finish
        Thread.sleep(100)

        Dispatchers.resetMain()
        unmockkAll()
        cacheDir.deleteRecursively()
    }

    @Test
    fun `startRecording transitions state to RECORDING`() = runTest {
        voiceManager.startRecording(1)
        // Use runCurrent() to execute pending coroutines WITHOUT advancing virtual time.
        // advanceUntilIdle() would fire the 5-minute auto-stop timer.
        runCurrent()
        assertEquals(VoiceState.RECORDING, voiceManager.state.value)
        assertTrue(voiceManager.isRecording())
    }

    @Test
    fun `stopRecording transitions state to PROCESSING`() = runTest {
        voiceManager.startRecording(1)
        runCurrent()
        assertEquals(VoiceState.RECORDING, voiceManager.state.value)

        voiceManager.stopRecording()
        runCurrent()
        assertEquals(VoiceState.PROCESSING, voiceManager.state.value)
    }

    @Test
    fun `cancelRecording resets state to IDLE`() = runTest {
        voiceManager.startRecording(1)
        runCurrent()
        voiceManager.cancelRecording()
        runCurrent()
        assertEquals(VoiceState.IDLE, voiceManager.state.value)
        assertFalse(voiceManager.isRecording())
    }

    @Test
    fun `auto-stop after 5 minutes transitions to PROCESSING`() = runTest {
        voiceManager.startRecording(1)
        runCurrent()
        assertEquals(VoiceState.RECORDING, voiceManager.state.value)

        // Advance past the 5-minute auto-stop timer
        advanceTimeBy(300001)
        runCurrent()

        assertEquals(VoiceState.PROCESSING, voiceManager.state.value)
    }

    @Test
    fun `onWindowHidden cancels recording`() = runTest {
        voiceManager.startRecording(1)
        runCurrent()
        voiceManager.onWindowHidden()
        runCurrent()
        assertEquals(VoiceState.IDLE, voiceManager.state.value)
    }
}
