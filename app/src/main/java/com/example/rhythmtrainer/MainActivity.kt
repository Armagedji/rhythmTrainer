package com.example.rhythmtrainer

import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.lifecycle.lifecycleScope
import com.example.rhythmtrainer.game.GameManager
import com.example.rhythmtrainer.game.CalibrationManager
import com.example.rhythmtrainer.settings.SettingsManager
import com.example.rhythmtrainer.settings.SettingsStorage
import com.example.rhythmtrainer.ranks.GameResultsStorage
import com.example.rhythmtrainer.navigation.Screen
import com.example.rhythmtrainer.navigation.NavigationHost
import com.example.rhythmtrainer.ui.screens.ResultDialog
import com.example.rhythmtrainer.ui.theme.RhythmTrainerTheme

class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    // Менеджеры
    private lateinit var gameManager: GameManager
    private lateinit var calibrationManager: CalibrationManager
    private lateinit var settingsManager: SettingsManager
    private lateinit var gameResultsStorage: GameResultsStorage

    // Аппаратные компоненты
    private var vibrator: Vibrator? = null

    // UI состояния
    private var currentScreen by mutableStateOf<Screen>(Screen.MainMenu)
    private var currentLevelId by mutableIntStateOf(1)
    private var currentBpm by mutableIntStateOf(80)
    private var showResultDialog by mutableStateOf(false)
    private var dialogScore by mutableIntStateOf(0)
    private var dialogHasNextLevel by mutableStateOf(false)

    init {
        try {
            System.loadLibrary("rhythmtrainer")
            Log.d(TAG, "Library loaded successfully")
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "Failed to load library: ${e.message}")
        }
    }

    // Нативные методы
    private external fun nativeInit()
    private external fun startRhythm(bpm: Int)
    private external fun stopRhythm()
    private external fun onTap()
    private external fun isRhythmPlaying(): Boolean
    private external fun startCalibration(bpm: Int)
    private external fun stopCalibration()
    private external fun setCalibrationOffset(offsetMs: Int)
    private external fun getTotalNotes(): Int
    private external fun setAllNotesProgressCallback()

    // Callback'и из C++ (должны остаться в Activity для JNI)
    @Suppress("unused")
    fun updateScore(score: Int) {
        runOnUiThread {
            Log.d(TAG, "updateScore: $score")
            gameManager.updateScore(score)
        }
    }

    @Suppress("unused")
    fun updateAllNotesProgress(progress: FloatArray) {
        runOnUiThread {
            gameManager.updateAllNotesProgress(progress)
        }
    }

    @Suppress("unused")
    fun updateResult(result: String) {
        runOnUiThread {
            Log.d(TAG, "updateResult: $result")
            gameManager.updateResult(result)
            if (settingsManager.vibrationEnabled && result != "Miss..." && result.isNotEmpty()) {
                vibrateDevice()
            }
        }
    }

    @Suppress("unused")
    fun updateCalibration(tapCount: Int, avgDeviation: Int) {
        runOnUiThread {
            Log.d(TAG, "updateCalibration: taps=$tapCount, avgDev=$avgDeviation")
            calibrationManager.updateCalibration(tapCount, avgDeviation)
        }
    }

    private fun vibrateDevice() {
        val v = vibrator ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(50)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Инициализация хранилищ
        gameResultsStorage = GameResultsStorage(this)
        val settingsStorage = SettingsStorage(this)

        // Инициализация менеджеров
        settingsManager = SettingsManager(this, settingsStorage)

        calibrationManager = CalibrationManager { offset ->
            settingsManager.updateCalibrationOffset(offset)
        }

        gameManager = GameManager(lifecycleScope)

        // Связываем нативные методы с менеджерами
        gameManager.startRhythm = { bpm -> startRhythm(bpm) }
        gameManager.stopRhythm = { stopRhythm() }
        gameManager.onTap = { onTap() }
        gameManager.isRhythmPlayingNative = { isRhythmPlaying() }

        calibrationManager.startCalibrationNative = { bpm -> startCalibration(bpm) }
        calibrationManager.stopCalibrationNative = { stopCalibration() }
        calibrationManager.setCalibrationOffsetNative = { offset -> setCalibrationOffset(offset) }

        // Применяем сохранённую калибровку
        setCalibrationOffset(settingsManager.calibrationOffset)

        // Инициализация вибратора
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }

        nativeInit()
        setAllNotesProgressCallback()

        setContent {
            RhythmTrainerTheme {
                Box {
                    NavigationHost(
                        currentScreen = currentScreen,
                        score = gameManager.score,
                        lastResult = gameManager.lastResult,
                        soundEnabled = settingsManager.soundEnabled,
                        vibrationEnabled = settingsManager.vibrationEnabled,
                        calibrationOffset = settingsManager.calibrationOffset,
                        calibrationTapCount = calibrationManager.tapCount,
                        calibrationAvgDev = calibrationManager.avgDeviation,
                        currentBpm = currentBpm,
                        onNavigate = { screen -> currentScreen = screen },
                        onLevelSelected = { levelId, bpm ->
                            currentLevelId = levelId
                            currentBpm = bpm
                            currentScreen = Screen.GameScreen
                        },
                        onStartLevel = { bpm -> gameManager.startLevel(bpm) },
                        onStopRhythm = { gameManager.stopLevel() },
                        onTap = { onTap() },
                        onSoundChanged = { enabled -> settingsManager.updateSoundEnabled(enabled) },
                        onVibrationChanged = { enabled -> settingsManager.updateVibrationEnabled(enabled) },
                        onStartCalibration = {
                            calibrationManager.startCalibration()
                            currentScreen = Screen.Calibration
                        },
                        onCalibrationTap = { onTap() },
                        onFinishCalibration = {
                            calibrationManager.finishCalibration()
                            currentScreen = Screen.Settings
                        },
                        onCancelCalibration = {
                            calibrationManager.cancelCalibration()
                            currentScreen = Screen.Settings
                        },
                        onGetTotalNotes = { getTotalNotes() },
                        allProgresses = gameManager.allProgresses,
                        noteColors = gameManager.noteColors
                    )

                    if (showResultDialog) {
                        ResultDialog(
                            score = dialogScore,
                            hasNextLevel = dialogHasNextLevel,
                            onRepeat = {
                                gameResultsStorage.setBestScore(currentLevelId, dialogScore)
                                showResultDialog = false
                                gameManager.startLevel(currentBpm)
                            },
                            onNext = {
                                gameResultsStorage.setBestScore(currentLevelId, dialogScore)
                                showResultDialog = false
                                currentLevelId++
                                currentBpm = when (currentLevelId) {
                                    1 -> 80
                                    2 -> 100
                                    3 -> 120
                                    else -> 120
                                }
                                gameManager.startLevel(currentBpm)
                            },
                            onSelectLevel = {
                                gameResultsStorage.setBestScore(currentLevelId, dialogScore)
                                showResultDialog = false
                                currentScreen = Screen.LevelSelect
                            },
                            onDismiss = {
                                gameResultsStorage.setBestScore(currentLevelId, dialogScore)
                                showResultDialog = false
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        gameManager.stopLevel()
    }
}