package com.example.rhythmtrainer.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class SettingsStorage(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_VIBRATION_ENABLED = "vibration_enabled"
        private const val KEY_SOUND_ENABLED = "sound_enabled"
        private const val KEY_CALIBRATION_OFFSET = "calibration_offset"

        const val DEFAULT_VIBRATION_ENABLED = true
        const val DEFAULT_SOUND_ENABLED = true
        const val DEFAULT_CALIBRATION_OFFSET = 0
    }

    var vibrationEnabled: Boolean
        get() = prefs.getBoolean(KEY_VIBRATION_ENABLED, DEFAULT_VIBRATION_ENABLED)
        set(value) = prefs.edit { putBoolean(KEY_VIBRATION_ENABLED, value) }

    var soundEnabled: Boolean
        get() = prefs.getBoolean(KEY_SOUND_ENABLED, DEFAULT_SOUND_ENABLED)
        set(value) = prefs.edit { putBoolean(KEY_SOUND_ENABLED, value) }

    var calibrationOffset: Int
        get() = prefs.getInt(KEY_CALIBRATION_OFFSET, DEFAULT_CALIBRATION_OFFSET)
        set(value) = prefs.edit { putInt(KEY_CALIBRATION_OFFSET, value) }
}