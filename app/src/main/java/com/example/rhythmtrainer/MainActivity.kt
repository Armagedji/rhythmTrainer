package com.example.rhythmtrainer

import ProgressRepository
import android.app.AlertDialog
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.lifecycleScope
import com.example.rhythmtrainer.ui.theme.RhythmTrainerTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    private var vibrator: Vibrator? = null

    init {
        try {
            System.loadLibrary("rhythmtrainer")
            Log.d(TAG, "Library loaded successfully")
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "Failed to load library: ${e.message}")
        }
    }

    // Нативные функции
    private external fun nativeInit()
    private external fun startRhythm(bpm: Int)
    private external fun stopRhythm()
    private external fun onTap()
    private external fun isRhythmPlaying(): Boolean
    private external fun startCalibration(bpm: Int)
    private external fun stopCalibration()
    private external fun getCalibrationOffset(): Int
    private external fun setCalibrationOffset(offsetMs: Int)
    private external fun loadSong(bpm: Int, totalNotes: Int)
    private external fun getTotalNotes(): Int
    private external fun setNotePositionCallback()
    private external fun setAllNotesProgressCallback()
    private external fun setLevelCompleteCallback()
    private external fun pauseGame()
    private external fun resumeGame()
    private external fun resetGameState()


    // MutableState для UI
    private val _score = mutableStateOf(0)
    private val _lastResult = mutableStateOf("")
    private val _vibrationEnabled = mutableStateOf(true)
    private val _calibrationOffset = mutableStateOf(getCalibrationOffset())
    private val _calibrationTapCount = mutableStateOf(0)
    private val _calibrationAvgDev = mutableStateOf(0)
    private val _isCalibrating = mutableStateOf(false)
    private val _notePositions = mutableStateOf<Map<Int, Float>>(emptyMap())
    private val _allNotesProgress = mutableStateOf(floatArrayOf())
    private lateinit var progressRepository: ProgressRepository
    private var currentLevelId by mutableStateOf(1)
    private var currentBpm by mutableStateOf(80)
    private var showResultDialog by mutableStateOf(false)
    private var completedLevelScore by mutableStateOf(0)
    private var hasNextLevel by mutableStateOf(false)


    // Callback'и из C++
    @Suppress("unused")
    fun updateScore(score: Int) {
        runOnUiThread {
            Log.d(TAG, "updateScore: $score")
            _score.value = score
        }
    }

    @Suppress("unused")
    fun updateAllNotesProgress(progress: FloatArray) {
        Log.d(TAG, "updateAllNotesProgress: size=${progress.size}, first=${progress.firstOrNull()}")
        runOnUiThread {
            _allNotesProgress.value = progress
        }
    }

    @Suppress("unused")
    fun updateNotePosition(index: Int, progress: Float) {
        runOnUiThread {
            _notePositions.value = _notePositions.value.toMutableMap().apply {
                put(index, progress)
            }
        }
    }

    @Suppress("unused")
    fun updateResult(result: String) {
        runOnUiThread {
            Log.d(TAG, "updateResult: $result")
            _lastResult.value = result
            if (_vibrationEnabled.value && result != "Miss..." && result.isNotEmpty()) {
                vibrateDevice()
            }
        }
    }

    @Suppress("unused")
    fun onLevelComplete(finalScore: Int) {
        runOnUiThread {
            // Сохраняем прогресс
            lifecycleScope.launch {
                progressRepository.saveCompletedLevel("level_$currentLevelId")
                progressRepository.addToTotalScore(finalScore)
            }
            // Показываем диалог
            showResult(finalScore)
        }
    }

    @Suppress("unused")
    fun updateCalibration(tapCount: Int, avgDeviation: Int) {
        runOnUiThread {
            Log.d(TAG, "updateCalibration: taps=$tapCount, avgDev=$avgDeviation")
            _calibrationTapCount.value = tapCount
            if (avgDeviation != 0) {
                // Это финальное значение после stopCalibration()
                _calibrationAvgDev.value = avgDeviation
                val offset = -avgDeviation
                setCalibrationOffset(offset)
                _calibrationOffset.value = offset
                Log.d(TAG, "Calibration saved: offset=$offset ms")

                // Закрываем экран калибровки
                _isCalibrating.value = false
                _calibrationTapCount.value = 0
                _calibrationAvgDev.value = 0
            }
        }
    }



    private fun vibrateDevice() {
        val vibratorInstance = vibrator ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibratorInstance.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibratorInstance.vibrate(50)
        }
    }

    private fun showResult(score: Int) {
        completedLevelScore = score
        hasNextLevel = (currentLevelId < 3) // определите totalLevels (например, 3)
        showResultDialog = true
    }

    private fun showResultDialog(score: Int) {
        AlertDialog.Builder(this)
            .setTitle("Уровень пройден!")
            .setMessage("Ваш счёт: $score")
            .setPositiveButton("Повторить") { _, _ ->
                resetGameState()
                startRhythm(currentBpm)
            }
            .setNeutralButton("Следующий уровень") { _, _ ->
                currentLevelId++
                currentBpm = when (currentLevelId) {
                    1 -> 80
                    2 -> 100
                    3 -> 120
                    else -> 120
                }
                resetGameState()
                startRhythm(currentBpm)
            }
            .setNegativeButton("Главное меню") { _, _ ->
                resetGameState()
                stopRhythm()
                // Обновите текущий экран через колбэк (например, переданный в NavigationHost)
            }
            .show()
    }

    private fun finishCalibration() {
        Log.d(TAG, "finishCalibration called")
        // Просто останавливаем калибровку - C++ сам вызовет callback с результатом
        stopCalibration()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        progressRepository = ProgressRepository(this)
        super.onCreate(savedInstanceState)

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
                    // Основной экран навигации
                    NavigationHost(
                        score = _score.value,
                        lastResult = _lastResult.value,
                        vibrationEnabled = _vibrationEnabled.value,
                        calibrationOffset = _calibrationOffset.value,
                        isCalibrating = _isCalibrating.value,
                        calibrationTapCount = _calibrationTapCount.value,
                        calibrationAvgDev = _calibrationAvgDev.value,
                        onStartRhythm = { bpm ->
                            Log.d(TAG, "onStartRhythm lambda called with BPM=$bpm")
                            startRhythm(bpm)
                        },
                        onStopRhythm = {
                            Log.d(TAG, "onStopRhythm lambda called")
                            stopRhythm()
                            _score.value = 0
                            _lastResult.value = ""
                        },
                        onTap = {
                            Log.d(TAG, "onTap called")
                            onTap()
                        },
                        onVibrationChanged = { enabled ->
                            _vibrationEnabled.value = enabled
                        },
                        onStartCalibration = {
                            Log.d(TAG, "onStartCalibration called")
                            _isCalibrating.value = true
                            _calibrationTapCount.value = 0
                            _calibrationAvgDev.value = 0
                            startCalibration(120)
                        },
                        onCalibrationTap = {
                            Log.d(TAG, "onCalibrationTap called")
                            onTap()
                        },
                        onFinishCalibration = {
                            Log.d(TAG, "onFinishCalibration called")
                            finishCalibration()
                        },
                        onCancelCalibration = {
                            Log.d(TAG, "onCancelCalibration called")
                            _isCalibrating.value = false
                            stopCalibration()
                            _calibrationTapCount.value = 0
                            _calibrationAvgDev.value = 0
                        },
                        onGetTotalNotes = { getTotalNotes() },
                        allProgresses = _allNotesProgress.value,
                        onSetAllNotesProgressCallback = { setAllNotesProgressCallback() },
                        currentLevelId = currentLevelId,
                        currentBpm = currentBpm,
                        onPauseGame = { pauseGame() },
                        onResumeGame = { resumeGame() },
                        onResetGameState = { resetGameState() },
                        onShowResultDialog = { score -> showResultDialog(score) },
                        onLevelCompleteCallback = { finalScore -> onLevelComplete(finalScore) }
                    )

                    // Диалог результатов (отображается поверх)
                    if (showResultDialog) {
                        ResultDialog(
                            score = completedLevelScore,
                            hasNextLevel = hasNextLevel,
                            onRepeat = {
                                showResultDialog = false
                                resetGameState()
                                startRhythm(currentBpm)
                            },
                            onNext = {
                                showResultDialog = false
                                currentLevelId++
                                // Обновите BPM для следующего уровня (пример)
                                currentBpm = when (currentLevelId) {
                                    1 -> 80
                                    2 -> 100
                                    3 -> 120
                                    else -> 120
                                }
                                resetGameState()
                                startRhythm(currentBpm)
                            },
                            onSelectLevel = {
                                showResultDialog = false
                                resetGameState()
                                stopRhythm()
                                // Переключите текущий экран на выбор уровня – передайте callback из NavigationHost
                                // Для простоты можно использовать общую переменную currentScreen, но сейчас она внутри NavigationHost.
                                // Если вы не передаёте callback, можно через mutableState, но проще добавить параметр onNavigateToLevelSelect в NavigationHost.
                                // Пока оставим заглушку – вам нужно реализовать смену экрана.
                                Log.d(TAG, "Select level clicked")
                            }
                        )
                    }
                }
            }
        }
    }
}

sealed class Screen(val title: String) {
    object MainMenu : Screen("Главное меню")
    object Learning : Screen("Обучение")
    object Settings : Screen("Настройки")
    object LevelSelect : Screen("Выбор уровня")
//    data class Game(val levelId: Int, val bpm: Int) : Screen("Тренировка")
    object Calibration : Screen("Калибровка")
    object GameWithNotes : Screen("Тренировка")
}

@Composable
fun NavigationHost(
    score: Int,
    lastResult: String,
    vibrationEnabled: Boolean,
    calibrationOffset: Int,
    isCalibrating: Boolean,
    calibrationTapCount: Int,
    calibrationAvgDev: Int,
    onStartRhythm: (bpm: Int) -> Unit,
    onStopRhythm: () -> Unit,
    onTap: () -> Unit,
    onVibrationChanged: (Boolean) -> Unit,
    onStartCalibration: () -> Unit,
    onCalibrationTap: () -> Unit,
    onFinishCalibration: () -> Unit,
    onCancelCalibration: () -> Unit,
    onGetTotalNotes: () -> Int,
    allProgresses: FloatArray,
    onSetAllNotesProgressCallback: () -> Unit,
    currentLevelId: Int,
    currentBpm: Int,
    onPauseGame: () -> Unit,
    onResumeGame: () -> Unit,
    onResetGameState: () -> Unit,
    onShowResultDialog: (Int) -> Unit,
    onLevelCompleteCallback: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.MainMenu) }
    var currentLevelId by remember { mutableStateOf(1) }
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
                    vibrationEnabled = vibrationEnabled,
                    calibrationOffset = calibrationOffset,
                    onVibrationChanged = onVibrationChanged,
                    onStartCalibration = {
                        onStartCalibration()
                        currentScreen = Screen.Calibration
                    },
                    onBack = { currentScreen = Screen.MainMenu }
                )
                Screen.LevelSelect -> LevelSelectScreen(
                    onBack = { currentScreen = Screen.MainMenu },
                    onLevelSelected = { levelId, bpm ->
                        currentLevelId = levelId
                        currentBpm = bpm
                        currentScreen = Screen.GameWithNotes
                    }
                )
                Screen.Calibration -> CalibrationScreen(
                    tapCount = calibrationTapCount,
                    avgDeviation = calibrationAvgDev,
                    onTap = onCalibrationTap,
                    onFinish = {
                        onFinishCalibration()
                        currentScreen = Screen.Settings
                    },
                    onCancel = {
                        onCancelCalibration()
                        currentScreen = Screen.Settings
                    }
                )
                Screen.GameWithNotes -> {
                    val totalNotes = onGetTotalNotes() // нативная функция
                    GameScreenWithNotes(
                        levelId = currentLevelId,
                        bpm = currentBpm,
                        score = score,
                        lastResult = lastResult,
                        totalNotes = totalNotes,
                        onBack = {
                            onStopRhythm()
                            currentScreen = Screen.MainMenu
                        },
                        onStartRhythm = { onStartRhythm(currentBpm) },
                        onTap = onTap,
                        allProgresses = allProgresses,
                        onSetCallback = onSetAllNotesProgressCallback,
                        onPause = onPauseGame,
                        onResume = onResumeGame,
                        onReset = onResetGameState,
                        onExit = {
                            onResetGameState()
                            onStopRhythm()
                            // смена экрана – возможно, через общий колбэк
                        },
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
                    "• Perfect – идеальное попадание (+10 очков)\n" +
                    "• Good – небольшое отклонение (+5 очков)\n" +
                    "• Miss – промах (0 очков)\n\n" +
                    "Начинайте с простых уровней и постепенно усложняйте!",
            fontSize = 16.sp
        )
        Button(onClick = onBack) {
            Text("Назад")
        }
    }
}

@Composable
fun SettingsScreen(
    vibrationEnabled: Boolean,
    calibrationOffset: Int,
    onVibrationChanged: (Boolean) -> Unit,
    onStartCalibration: () -> Unit,
    onBack: () -> Unit
) {
    var soundEnabled by remember { mutableStateOf(true) }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = { soundEnabled = !soundEnabled }) {
            Text("Звук: ${if (soundEnabled) "Вкл" else "Выкл"}")
        }
        Button(onClick = { onVibrationChanged(!vibrationEnabled) }) {
            Text("Вибрация: ${if (vibrationEnabled) "Вкл" else "Выкл"}")
        }
        Text("Калибровка: $calibrationOffset мс", fontSize = 16.sp)
        Button(onClick = onStartCalibration) {
            Text("🎯 Калибровать задержку")
        }
        Button(onClick = onBack) {
            Text("Назад")
        }
    }
}

@Composable
fun CalibrationScreen(
    tapCount: Int,
    avgDeviation: Int,
    onTap: () -> Unit,
    onFinish: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = "Калибровка задержки\n\n" +
                    "Слушайте ритм и нажимайте на кнопку\n" +
                    "в такт метроному.\n\n" +
                    "Сделайте 8-10 нажатий для точной калибровки.",
            fontSize = 18.sp
        )

        Text(
            text = "Нажатий: $tapCount",
            fontSize = 20.sp
        )

        if (avgDeviation != 0 && tapCount >= 5) {
            Text(
                text = "Среднее отклонение: $avgDeviation мс",
                fontSize = 16.sp,
                color = if (kotlin.math.abs(avgDeviation) < 30)
                    androidx.compose.ui.graphics.Color.Green
                else
                    androidx.compose.ui.graphics.Color.Yellow
            )
        }

        Button(
            onClick = onTap,
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(100.dp)
        ) {
            Text("👆 Нажми в ритм!", fontSize = 20.sp)
        }

        Button(
            onClick = onFinish,
            modifier = Modifier.fillMaxWidth(0.8f),
            enabled = tapCount >= 3
        ) {
            Text(if (tapCount >= 3) "✅ Завершить калибровку" else "Сделайте хотя бы 3 нажатия")
        }

        Button(onClick = onCancel) {
            Text("❌ Отмена")
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
    score: Int,
    lastResult: String,
    onBack: () -> Unit,
    onStartRhythm: () -> Unit,
    onTap: () -> Unit
) {
    var isPlaying by remember { mutableStateOf(false) }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = "Уровень $levelId\nBPM: $bpm\n\n" +
                    "Счёт: $score\n\n" +
                    "Результат: $lastResult\n\n" +
                    "Статус: ${if (isPlaying) "🎵 Ритм играет" else "⏸ Ритм остановлен"}",
            fontSize = 20.sp
        )

        Button(
            onClick = {
                Log.d("GameScreen", "Main tap button clicked")
                if (isPlaying) {
                    onTap()
                }
            },
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(120.dp)
        ) {
            Text("👆 Нажми в ритм!", fontSize = 24.sp)
        }

        Button(
            onClick = {
                Log.d("GameScreen", "Start/Stop button clicked, isPlaying=$isPlaying")
                if (!isPlaying) {
                    onStartRhythm()
                    isPlaying = true
                }
            },
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Text(if (isPlaying) "🔊 Ритм играет" else "▶ Запустить ритм", fontSize = 18.sp)
        }

        Button(onClick = {
            Log.d("GameScreen", "Exit button clicked")
            isPlaying = false
            onBack()
        }) {
            Text("🚪 Выйти")
        }
    }
}

@Composable
fun GameScreenWithNotes(
    levelId: Int,
    bpm: Int,
    score: Int,
    lastResult: String,
    totalNotes: Int,
    onBack: () -> Unit,
    onStartRhythm: () -> Unit,
    onTap: () -> Unit,
    allProgresses: FloatArray,
    onSetCallback: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onReset: () -> Unit,
    onExit: () -> Unit
) {
    var isPaused by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        onSetCallback()
    }
    LaunchedEffect(allProgresses) {
        Log.d("GameScreen", "allProgresses size=${allProgresses.size}, totalNotes=$totalNotes")
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val screenWidth = size.width
            val screenHeight = size.height
            val hitLineX = screenWidth / 2f  // линия попадания по центру
            val startX = screenWidth + 100f  // за правым краем
            val endX = -100f                 // за левым краем

            for (i in 0 until totalNotes) {
                val progress = if (i < allProgresses.size) allProgresses[i] else -1f
                if (progress in 0f..1f) {
                    val x = startX - (startX - hitLineX) * progress
                    val y = screenHeight / 2 + (if (i % 2 == 0) -40f else 40f)

                    drawCircle(
                        color = if (progress > 0.95f) Color.Red else Color.White,
                        radius = 25f,
                        center = Offset(x, y)
                    )
                    drawLine(
                        color = Color.White,
                        start = Offset(x, y),
                        end = Offset(x, y - 50f),
                        strokeWidth = 4f
                    )
                }
            }

            // Линия попадания (зелёная вертикальная)
            drawLine(
                color = Color.Green,
                start = Offset(hitLineX, 0f),
                end = Offset(hitLineX, screenHeight),
                strokeWidth = 5f
            )
        }

        // Интерфейс (счёт, кнопки) поверх Canvas
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Панель счёта
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.DarkGray
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Счёт: $score", color = Color.White, fontSize = 24.sp)
                    Text("BPM: $bpm", color = Color.White, fontSize = 24.sp)
                    Text("Результат: $lastResult", color = Color.White, fontSize = 20.sp)
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Кнопки управления
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(onClick = {
                    isPaused = !isPaused
                    if (isPaused) onPause() else onResume()
                }) {
                    Text(if (isPaused) "Продолжить" else "Пауза")
                }
                Button(
                    onClick = onStartRhythm,
                    modifier = Modifier.fillMaxWidth().height(80.dp)
                ) {
                    Text("Запустить ритм", fontSize = 20.sp)
                }
                Button(
                    onClick = onTap,
                    modifier = Modifier.fillMaxWidth().height(80.dp).padding(top = 8.dp)
                ) {
                    Text("Нажми в ритм!", fontSize = 20.sp)
                }
                Button(
                    onClick = onBack,
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Text("Выйти")
                }
            }
        }
    }
}

@Composable
fun ResultDialog(
    score: Int,
    hasNextLevel: Boolean,
    onRepeat: () -> Unit,
    onNext: () -> Unit,
    onSelectLevel: () -> Unit
) {
    Dialog(onDismissRequest = {}) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Уровень пройден!", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Ваш счёт: $score", fontSize = 20.sp)
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(onClick = onRepeat, modifier = Modifier.weight(1f)) {
                        Text("Повторить")
                    }
                    if (hasNextLevel) {
                        Button(onClick = onNext, modifier = Modifier.weight(1f)) {
                            Text("Следующий")
                        }
                    }
                    Button(onClick = onSelectLevel, modifier = Modifier.weight(1f)) {
                        Text("Выбор уровня")
                    }
                }
            }
        }
    }
}

data class LevelInfo(val id: Int, val name: String, val description: String, val bpm: Int)