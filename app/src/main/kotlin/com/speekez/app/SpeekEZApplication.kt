package com.speekez.app

import android.app.Application
import com.speekez.data.SpeekEZDatabase
import com.speekez.voice.VoiceManager

/**
 * Main application class for SpeekEZ.
 * Following Florisboard's manual DI pattern.
 */
class SpeekEZApplication : Application(), VoiceManager.Provider {

    override val voiceManager by lazy { VoiceManager(this) }
    val database: SpeekEZDatabase by lazy { SpeekEZDatabase.getDatabase(this) }

    override fun onCreate() {
        super.onCreate()
    }
}
