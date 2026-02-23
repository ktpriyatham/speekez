package com.speekez.app

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.speekez.voice.VoiceState
import com.speekez.voice.voiceManager
import dev.patrickgold.florisboard.FlorisApplication
import dev.patrickgold.florisboard.clipboardManager

class VoiceShortcutActivity : ComponentActivity() {
    private val TAG = "VoiceShortcutActivity"

    private val transcriptionListener: (String) -> Unit = { text ->
        val clipboard = (application as FlorisApplication).clipboardManager.value
        clipboard.addNewPlaintext(text)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val presetId = intent.getIntExtra("preset_id", -1)
        if (presetId == -1) {
            Log.e(TAG, "No preset_id provided")
            finish()
            return
        }

        val voiceManager = voiceManager().value

        // Register transcription listener
        voiceManager.addTranscriptionListener(transcriptionListener)

        // Start recording
        voiceManager.startRecording(presetId)

        setContent {
            SpeekEZTheme {
                val state by voiceManager.state.collectAsState()
                val errorMessage by voiceManager.errorMessage.collectAsState()
                val processingMessage by voiceManager.processingMessage.collectAsState()

                var hasStarted by remember { mutableStateOf(false) }
                LaunchedEffect(state) {
                    if (state != VoiceState.IDLE) {
                        hasStarted = true
                    }
                    if (hasStarted && state == VoiceState.IDLE) {
                        finish()
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF12121F)),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.padding(32.dp).wrapContentSize()
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = when (state) {
                                    VoiceState.RECORDING -> "Recording..."
                                    VoiceState.PROCESSING -> processingMessage ?: "Transcribing..."
                                    VoiceState.DONE -> "Done!"
                                    VoiceState.ERROR -> "Error"
                                    else -> "Starting..."
                                },
                                style = MaterialTheme.typography.headlineSmall,
                                color = Color.White
                            )

                            if (state == VoiceState.ERROR) {
                                Text(
                                    text = errorMessage ?: "Unknown error",
                                    color = Color.Red,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            if (state == VoiceState.RECORDING) {
                                Button(
                                    onClick = { voiceManager.stopRecording() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00D4AA)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Stop Recording", color = Color.Black)
                                }
                            } else if (state == VoiceState.ERROR || state == VoiceState.DONE) {
                                Button(
                                    onClick = { finish() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Close")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        val voiceManager = voiceManager().value
        if (voiceManager.isRecording()) {
            voiceManager.cancelRecording()
        }
    }

    override fun onDestroy() {
        voiceManager().value.removeTranscriptionListener(transcriptionListener)
        super.onDestroy()
    }
}
