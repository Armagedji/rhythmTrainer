package com.example.rhythmtrainer

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rhythmtrainer.ui.theme.RhythmTrainerTheme

class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    init {
        try {
            System.loadLibrary("rhythmtrainer")
            Log.d(TAG, "Library loaded successfully")
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "Failed to load library: ${e.message}")
        }
    }

    // Нативные функции
    private external fun startRhythm(bpm: Int)
    private external fun stopRhythm()
    private external fun isRhythmPlaying(): Boolean

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate called")
        setContent {
            RhythmTrainerTheme {
                NavigationHost(
                    onStartRhythm = { bpm ->
                        Log.d(TAG, "onStartRhythm lambda called with BPM=$bpm")
                        startRhythm(bpm)
                    },
                    onStopRhythm = {
                        Log.d(TAG, "onStopRhythm lambda called")
                        stopRhythm()
                    }
                )
            }
        }
    }
}

sealed class Screen(val title: String) {
    object MainMenu : Screen("Главное меню")
    object Learning : Screen("Обучение")
    object Settings : Screen("Настройки")
    object LevelSelect : Screen("Выбор уровня")
    data class Game(val levelId: Int, val bpm: Int) : Screen("Тренировка")
}

@Composable
fun NavigationHost(
    onStartRhythm: (bpm: Int) -> Unit,
    onStopRhythm: () -> Unit
) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.MainMenu) }
    var currentBpm by remember { mutableStateOf(80) }

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when (currentScreen) {
                Screen.MainMenu -> MainMenuScreen(
                    onNavigate = { currentScreen = it }
                )
                Screen.Learning -> LearningScreen(
                    onBack = { currentScreen = Screen.MainMenu }
                )
                Screen.Settings -> SettingsScreen(
                    onBack = { currentScreen = Screen.MainMenu }
                )
                Screen.LevelSelect -> LevelSelectScreen(
                    onBack = { currentScreen = Screen.MainMenu },
                    onLevelSelected = { levelId, bpm ->
                        Log.d("NavigationHost", "Level selected: id=$levelId, BPM=$bpm")
                        currentBpm = bpm
                        currentScreen = Screen.Game(levelId, bpm)
                    }
                )
                is Screen.Game -> {
                    val gameScreen = currentScreen as Screen.Game
                    Log.d("NavigationHost", "Showing GameScreen for level ${gameScreen.levelId}, BPM=${gameScreen.bpm}")
                    GameScreen(
                        levelId = gameScreen.levelId,
                        bpm = gameScreen.bpm,
                        onBack = {
                            Log.d("NavigationHost", "GameScreen onBack called")
                            onStopRhythm()
                            currentScreen = Screen.MainMenu
                        },
                        onStartRhythm = {
                            Log.d("NavigationHost", "GameScreen onStartRhythm called with BPM=${gameScreen.bpm}")
                            onStartRhythm(gameScreen.bpm)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun MainMenuScreen(onNavigate: (Screen) -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = { onNavigate(Screen.LevelSelect) }) {
            Text("Уровни", fontSize = 20.sp)
        }
        Button(onClick = { onNavigate(Screen.Learning) }) {
            Text("Обучение", fontSize = 20.sp)
        }
        Button(onClick = { onNavigate(Screen.Settings) }) {
            Text("Настройки", fontSize = 20.sp)
        }
    }
}

@Composable
fun LearningScreen(onBack: () -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = "Что такое ритм?\n\n" +
                    "Ритм – это чередование звуков разной длительности.\n\n" +
                    "В этом приложении вы будете нажимать на экран в такт музыке.\n\n" +
                    "Точность нажатий оценивается как:\n" +
                    "• Perfect – идеальное попадание\n" +
                    "• Good – небольшое отклонение\n" +
                    "• Miss – промах\n\n" +
                    "Начинайте с простых уровней и постепенно усложняйте!",
            fontSize = 16.sp
        )
        Button(onClick = onBack) {
            Text("Назад")
        }
    }
}

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    var soundEnabled by remember { mutableStateOf(true) }
    var vibrationEnabled by remember { mutableStateOf(true) }
    var calibrationOffset by remember { mutableStateOf(0) }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = { soundEnabled = !soundEnabled }) {
            Text("Звук: ${if (soundEnabled) "Вкл" else "Выкл"}")
        }
        Button(onClick = { vibrationEnabled = !vibrationEnabled }) {
            Text("Вибрация: ${if (vibrationEnabled) "Вкл" else "Выкл"}")
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Калибровка: $calibrationOffset мс")
            androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { calibrationOffset -= 10 }) { Text("-10") }
                Button(onClick = { calibrationOffset += 10 }) { Text("+10") }
            }
        }
        Button(onClick = onBack) {
            Text("Назад")
        }
    }
}

@Composable
fun LevelSelectScreen(onBack: () -> Unit, onLevelSelected: (levelId: Int, bpm: Int) -> Unit) {
    val levels = listOf(
        LevelInfo(1, "Уровень 1: Четверти", "BPM: 80", 80),
        LevelInfo(2, "Уровень 2: Восьмые", "BPM: 100", 100),
        LevelInfo(3, "Уровень 3: Паузы", "BPM: 120", 120)
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        levels.forEach { level ->
            Button(
                onClick = { onLevelSelected(level.id, level.bpm) },
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                Column {
                    Text(level.name)
                    Text(level.description, fontSize = 12.sp)
                }
            }
        }
        Button(onClick = onBack) {
            Text("Назад")
        }
    }
}

@Composable
fun GameScreen(
    levelId: Int,
    bpm: Int,
    onBack: () -> Unit,
    onStartRhythm: () -> Unit
) {
    var isPlaying by remember { mutableStateOf(false) }
    var score by remember { mutableStateOf(0) }
    var lastResult by remember { mutableStateOf("") }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = "Уровень $levelId\nBPM: $bpm\n\n" +
                    "Счёт: $score\n\n" +
                    "Последнее нажатие: $lastResult\n\n" +
                    "Статус: ${if (isPlaying) "Ритм играет" else "Ритм остановлен"}",
            fontSize = 18.sp
        )

        Button(
            onClick = {
                Log.d("GameScreen", "Main tap button clicked")
                // Здесь будет логика игры
                lastResult = "Нажатие зарегистрировано"
            },
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(120.dp)
        ) {
            Text("Нажми в ритм!", fontSize = 24.sp)
        }

        Button(
            onClick = {
                Log.d("GameScreen", "Start/Stop button clicked, isPlaying=$isPlaying")
                if (!isPlaying) {
                    onStartRhythm()
                    isPlaying = true
                    lastResult = "Ритм запущен!"
                }
            },
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Text(if (isPlaying) "Ритм играет" else "Запустить ритм", fontSize = 18.sp)
        }

        Button(onClick = {
            Log.d("GameScreen", "Exit button clicked")
            isPlaying = false
            onBack()
        }) {
            Text("Выйти")
        }
    }
}

data class LevelInfo(val id: Int, val name: String, val description: String, val bpm: Int)