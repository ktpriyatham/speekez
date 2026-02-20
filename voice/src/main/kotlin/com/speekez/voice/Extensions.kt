package com.speekez.voice

import android.content.Context

/**
 * Extension function to easily access the VoiceManager from any context.
 */
fun Context.voiceManager(): VoiceManager {
    val provider = this.applicationContext as? VoiceManager.Provider
    return provider?.voiceManager ?: throw IllegalStateException("Application must implement VoiceManager.Provider")
}
