package com.example.rhythmtrainer

import androidx.annotation.StringRes

enum class Wavetable {
    SINE {
        override fun toResourceString(): Int {
            return R.string.sine
        }
    },
    TRIANGLE {
        override fun toResourceString(): Int {
            return R.string.sine
        }
    },
    SQUARE {
        override fun toResourceString(): Int {
            return R.string.sine
        }
    },
    SAW {
        override fun toResourceString(): Int {
            return R.string.sine
        }
    };

    @StringRes
    abstract fun toResourceString() : Int
}

interface WavetableSynthesizer {
    suspend fun play()
    suspend fun stop()
    suspend fun isPlaying(): Boolean
    suspend fun setFrequency(frequencyInHz: Float)
    suspend fun setVolume(volumeInDb: Float)
    suspend fun setWavetable(wavetable: Wavetable)


}