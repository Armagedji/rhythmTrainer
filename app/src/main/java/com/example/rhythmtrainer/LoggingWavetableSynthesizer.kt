package com.example.rhythmtrainer

import android.util.Log

class LoggingWavetableSynthesizer : WavetableSynthesizer {
    private var isPlayingVal = false

    override suspend fun play() {
        Log.d("LoggingWavetableSynthesizer", "play() called.")
        isPlayingVal = true
    }

    override suspend fun stop() {
        Log.d("LoggingWavetableSynthesizer", "stop() called.")
        isPlayingVal = false
    }

    override suspend fun isPlaying(): Boolean {
        Log.d("LoggingWavetableSynthesizer", "play() called.")
        return isPlayingVal
    }

    override suspend fun setFrequency(frequencyInHz: Float) {
        Log.d("LoggingWavetableSynthesizer", "setFrequency() called with $frequencyInHz")
    }

    override suspend fun setVolume(volumeInDb: Float) {
        Log.d("LoggingWavetableSynthesizer", "setVolume() called with $volumeInDb")
    }

    override suspend fun setWavetable(wavetable: Wavetable) {
        Log.d("LoggingWavetableSynthesizer", "setWavetable() called with $wavetable")
    }
}