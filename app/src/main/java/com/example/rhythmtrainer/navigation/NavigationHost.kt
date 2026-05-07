package com.example.rhythmtrainer.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.rhythmtrainer.learning.LearningScreen
import com.example.rhythmtrainer.ui.screens.*

@Composable
fun NavigationHost(
    currentScreen: Screen,
    score: Int,
    lastResult: String,
    soundEnabled: Boolean,
    vibrationEnabled: Boolean,
    calibrationOffset: Int,
    calibrationTapCount: Int,
    calibrationAvgDev: Int,
    currentBpm: Int,
    onNavigate: (Screen) -> Unit,
    onLevelSelected: (Int, Int) -> Unit,
    onStartLevel: (bpm: Int) -> Unit,
    onStopRhythm: () -> Unit,
    onTap: () -> Unit,
    onSoundChanged: (Boolean) -> Unit,
    onVibrationChanged: (Boolean) -> Unit,
    onStartCalibration: () -> Unit,
    onCalibrationTap: () -> Unit,
    onFinishCalibration: () -> Unit,
    onCancelCalibration: () -> Unit,
    onGetTotalNotes: () -> Int,
    allProgresses: FloatArray,
    noteColors: Map<Int, Color>,
) {
    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when (currentScreen) {
                Screen.MainMenu -> MainMenuScreen(onNavigate = onNavigate)
                Screen.Learning -> LearningScreen(onBack = { onNavigate(Screen.MainMenu) })
                Screen.Settings -> SettingsScreen(
                    vibrationEnabled = vibrationEnabled,
                    soundEnabled = soundEnabled,
                    calibrationOffset = calibrationOffset,
                    onVibrationChanged = onVibrationChanged,
                    onSoundChanged = onSoundChanged,
                    onStartCalibration = onStartCalibration,
                    onBack = { onNavigate(Screen.MainMenu) }
                )
                Screen.LevelSelect -> LevelSelectScreen(
                    onBack = { onNavigate(Screen.MainMenu) },
                    onLevelSelected = onLevelSelected
                )
                Screen.Calibration -> CalibrationScreen(
                    tapCount = calibrationTapCount,
                    avgDeviation = calibrationAvgDev,
                    onTap = onCalibrationTap,
                    onFinish = onFinishCalibration,
                    onCancel = onCancelCalibration
                )
                Screen.GameScreen -> {
                    val totalNotes = onGetTotalNotes()
                    GameScreen(
                        bpm = currentBpm,
                        score = score,
                        lastResult = lastResult,
                        totalNotes = totalNotes,
                        onBack = {
                            onStopRhythm()
                            onNavigate(Screen.MainMenu)
                        },
                        onStartLevel = { onStartLevel(currentBpm) },
                        onTap = onTap,
                        allProgresses = allProgresses,
                        noteColors = noteColors
                    )
                }
            }
        }
    }
}