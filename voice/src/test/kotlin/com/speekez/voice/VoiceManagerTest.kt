package com.speekez.voice

import android.content.Context
import android.util.Log
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class VoiceManagerTest {

    private lateinit var context: Context
    private lateinit var voiceManager: VoiceManager

    @BeforeEach
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        context = mockk()
        voiceManager = VoiceManager(context)
    }

    @Test
    fun `test recording state transitions`() {
        assertFalse(voiceManager.isRecording())
        
        voiceManager.startRecording(1)
        assertTrue(voiceManager.isRecording())
        
        voiceManager.stopRecording()
        assertFalse(voiceManager.isRecording())
        
        voiceManager.startRecording(1)
        voiceManager.cancelRecording()
        assertFalse(voiceManager.isRecording())
    }

    @Test
    fun `test onWindowHidden cancels recording`() {
        voiceManager.startRecording(1)
        assertTrue(voiceManager.isRecording())
        
        voiceManager.onWindowHidden()
        assertFalse(voiceManager.isRecording())
    }
}
