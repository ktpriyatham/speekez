package com.speekez.keyboard

import android.inputmethodservice.InputMethodService
import com.speekez.voice.voiceManager

/**
 * Main InputMethodService for SpeekEZ.
 * Orchestrates keyboard lifecycle and manager hooks.
 */
class SpeekEZInputMethodService : InputMethodService() {

    override fun onWindowShown() {
        super.onWindowShown()
        voiceManager().onWindowShown()
    }

    override fun onWindowHidden() {
        super.onWindowHidden()
        voiceManager().onWindowHidden()
    }

    // Adding a dummy method to simulate start recording
    fun startVoiceRecording(presetId: Int) {
        voiceManager().startRecording(presetId)
    }
}
