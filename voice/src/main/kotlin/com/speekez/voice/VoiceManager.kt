package com.speekez.voice

import android.content.Context
import android.util.Log

/**
 * Manager responsible for orchestrating voice recording, transcription, and refinement.
 * Following Florisboard's manager pattern.
 */
class VoiceManager(private val context: Context) {

    companion object {
        private const val TAG = "VoiceManager"
    }

    interface Provider {
        val voiceManager: VoiceManager
    }

    private var recording: Boolean = false

    fun startRecording(presetId: Int) {
        Log.d(TAG, "startRecording(presetId=$presetId)")
        recording = true
    }

    fun stopRecording() {
        Log.d(TAG, "stopRecording()")
        recording = false
    }

    fun isRecording(): Boolean {
        return recording
    }

    fun cancelRecording() {
        Log.d(TAG, "cancelRecording()")
        recording = false
    }

    /**
     * Called when the keyboard window is shown.
     */
    fun onWindowShown() {
        Log.d(TAG, "onWindowShown()")
    }

    /**
     * Called when the keyboard window is hidden.
     */
    fun onWindowHidden() {
        Log.d(TAG, "onWindowHidden()")
        if (isRecording()) {
            cancelRecording()
        }
    }
}
