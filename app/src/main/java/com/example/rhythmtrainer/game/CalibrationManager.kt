package com.example.rhythmtrainer.game

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.*

class CalibrationManager(
    private val onOffsetChanged: (Int) -> Unit = {}
) {
    companion object {
        private const val TAG = "CalibrationManager"
    }

    var tapCount by mutableIntStateOf(0)
        private set
    var avgDeviation by mutableIntStateOf(0)
        private set
    var isCalibrating by mutableStateOf(false)
        private set

    var startCalibrationNative: (bpm: Int) -> Unit = {}
    var stopCalibrationNative: () -> Unit = {}
    var setCalibrationOffsetNative: (offsetMs: Int) -> Unit = {}

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    fun startCalibration(bpm: Int = 120) {
        isCalibrating = true
        tapCount = 0
        avgDeviation = 0
        try {
            startCalibrationNative(bpm)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting calibration: ${e.message}")
        }
    }

    fun updateCalibration(tapCount: Int, avgDeviation: Int) {
        this.tapCount = tapCount
        if (avgDeviation != 0) {
            this.avgDeviation = avgDeviation
            val offset = -avgDeviation
            try {
                setCalibrationOffsetNative(offset)
                Log.d(TAG, "Calibration saved: offset=$offset ms")
            } catch (e: Exception) {
                Log.e(TAG, "Error setting calibration offset: ${e.message}")
            }

            scope.launch {
                delay(100)
                onOffsetChanged(offset)
            }

            stopSafely()
        }
    }

    fun finishCalibration() {
        Log.d(TAG, "finishCalibration called")
        stopSafely()
    }

    fun cancelCalibration() {
        Log.d(TAG, "cancelCalibration called")
        stopSafely()
    }

    private fun stopSafely() {
        scope.launch {
            delay(50)
            try {
                stopCalibrationNative()
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping calibration: ${e.message}")
            }
            isCalibrating = false
            tapCount = 0
            avgDeviation = 0
        }
    }

    fun release() {
        scope.cancel()
    }
}