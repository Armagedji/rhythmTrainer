// com/example/rhythmtrainer/settings/SettingsManager.kt
package com.example.rhythmtrainer.settings

import android.content.Context
import android.media.AudioManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class SettingsManager(
    private val context: Context,
    private val storage: SettingsStorage
) {
    var soundEnabled by mutableStateOf(storage.soundEnabled)
        private set

    var vibrationEnabled by mutableStateOf(storage.vibrationEnabled)
        private set

    var calibrationOffset by mutableIntStateOf(storage.calibrationOffset)
        private set

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    init {
        applySoundSettings()
    }

    fun updateSoundEnabled(enabled: Boolean) {
        soundEnabled = enabled
        storage.soundEnabled = enabled
        applySoundSettings()
    }

    fun updateVibrationEnabled(enabled: Boolean) {
        vibrationEnabled = enabled
        storage.vibrationEnabled = enabled
    }

    fun updateCalibrationOffset(offset: Int) {
        calibrationOffset = offset
        storage.calibrationOffset = offset
    }

    private fun applySoundSettings() {
        if (soundEnabled) {
            audioManager.setStreamVolume(
                AudioManager.STREAM_MUSIC,
                audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) / 2,
                0
            )
        } else {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0)
        }
    }
}