package com.example.rhythmtrainer

import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PaintingStyle.Companion.Stroke
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.example.rhythmtrainer.ui.theme.RhythmTrainerTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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

    // MutableState для UI
    private val _score = mutableStateOf(0)
    private val _lastResult = mutableStateOf("")
    private val _vibrationEnabled = mutableStateOf(true)
    private val _calibrationOffset = mutableStateOf(getCalibrationOffset())
    private val _calibrationTapCount = mutableStateOf(0)
    private val _calibrationAvgDev = mutableStateOf(0)
    private val _isCalibrating = mutableStateOf(false)
    private val _notePositions = mutableStateOf<Map<Int, Float>>(emptyMap())
    private val _noteColors = mutableStateOf<Map<Int, Color>>(emptyMap())
    private val _allProgresses = mutableStateOf(floatArrayOf())

    // Состояния навигации и уровня (подняты в MainActivity)
    private var currentScreen by mutableStateOf<Screen>(Screen.MainMenu)
    private var currentLevelId by mutableStateOf(1)
    private var currentBpm by mutableStateOf(80)

    // Состояния для диалога результатов
    private var showResultDialog by mutableStateOf(false)
    private var dialogScore by mutableStateOf(0)
    private var dialogHasNextLevel by mutableStateOf(false)

    // Таймер для завершения уровня
    private var levelCompletionJob: Job? = null



    // Callback'и из C++
    @Suppress("unused")
    fun updateScore(score: Int) {
        runOnUiThread {
            Log.d(TAG, "updateScore: $score")
            _score.value = score
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
    fun updateAllNotesProgress(progress: FloatArray) {
        runOnUiThread {
            _allProgresses.value = progress
        }
    }

    @Suppress("unused")
    fun updateResult(result: String) {
        runOnUiThread {
            Log.d(TAG, "updateResult: $result")
            _lastResult.value = result
            updateNoteColorFromResult(result)   // добавлено
            if (_vibrationEnabled.value && result != "Miss..." && result.isNotEmpty()) {
                vibrateDevice()
            }
        }
    }

    @Suppress("unused")
    fun updateCalibration(tapCount: Int, avgDeviation: Int) {
        runOnUiThread {
            Log.d(TAG, "updateCalibration: taps=$tapCount, avgDev=$avgDeviation")
            _calibrationTapCount.value = tapCount
            if (avgDeviation != 0) {
                _calibrationAvgDev.value = avgDeviation
                val offset = -avgDeviation
                setCalibrationOffset(offset)
                _calibrationOffset.value = offset
                Log.d(TAG, "Calibration saved: offset=$offset ms")
                _isCalibrating.value = false
                _calibrationTapCount.value = 0
                _calibrationAvgDev.value = 0
            }
        }
    }

    private fun updateNoteColorFromResult(result: String) {
        val progresses = _allProgresses.value
        var targetIndex = -1
        var minDist = 1f
        for (i in progresses.indices) {
            val dist = kotlin.math.abs(progresses[i] - 0.5f)
            if (dist < minDist) {
                minDist = dist
                targetIndex = i
            }
        }
        if (targetIndex != -1 && minDist < 0.1f) {
            val color = when {
                result.contains("Perfect") -> Color.Green
                result.contains("Good") -> Color.Yellow
                else -> Color.Red
            }
            _noteColors.value = _noteColors.value.toMutableMap().apply {
                put(targetIndex, color)
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

    private fun startLevel(bpm: Int, notesCount: Int = 32) {
        levelCompletionJob?.cancel()
        _score.value = 0
        _lastResult.value = ""
        _allProgresses.value = floatArrayOf()  // очистка прогрессов
        _noteColors.value = emptyMap()     // сброс цветов
        startRhythm(bpm)
        val beatDurationMs = 60000L / bpm
        val durationMs = (notesCount - 1) * beatDurationMs
        levelCompletionJob = lifecycleScope.launch {
            delay(durationMs + 300)
            if (isRhythmPlaying()) {
                stopRhythm()
                dialogScore = _score.value
                // Проверка, есть ли следующий уровень (всего 3 уровня)
                dialogHasNextLevel = (currentLevelId < 3)
                showResultDialog = true
            }
        }
    }

    private fun stopLevel() {
        levelCompletionJob?.cancel()
        if (isRhythmPlaying()) stopRhythm()
    }

    private fun finishCalibration() {
        Log.d(TAG, "finishCalibration called")
        stopCalibration()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
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
                    // Основная навигация
                    NavigationHost(
                        currentScreen = currentScreen,
                        score = _score.value,
                        lastResult = _lastResult.value,
                        vibrationEnabled = _vibrationEnabled.value,
                        calibrationOffset = _calibrationOffset.value,
                        isCalibrating = _isCalibrating.value,
                        calibrationTapCount = _calibrationTapCount.value,
                        calibrationAvgDev = _calibrationAvgDev.value,
                        currentLevelId = currentLevelId,
                        currentBpm = currentBpm,
                        onNavigate = { screen -> currentScreen = screen },
                        onLevelSelected = { levelId, bpm ->
                            currentLevelId = levelId
                            currentBpm = bpm
                            currentScreen = Screen.GameWithNotes
                        },
                        onStartLevel = { bpm -> startLevel(bpm) },
                        onStopRhythm = { stopRhythm() },
                        onTap = { onTap() },
                        onVibrationChanged = { enabled -> _vibrationEnabled.value = enabled },
                        onStartCalibration = {
                            _isCalibrating.value = true
                            _calibrationTapCount.value = 0
                            _calibrationAvgDev.value = 0
                            startCalibration(120)
                            currentScreen = Screen.Calibration
                        },
                        onCalibrationTap = { onTap() },
                        onFinishCalibration = { finishCalibration() },
                        onCancelCalibration = {
                            _isCalibrating.value = false
                            stopCalibration()
                            _calibrationTapCount.value = 0
                            _calibrationAvgDev.value = 0
                            currentScreen = Screen.Settings
                        },
                        onGetTotalNotes = { getTotalNotes() },
                        allProgresses = _allProgresses.value,
                        noteColors = _noteColors.value
                    )

                    // Диалог результатов
                    if (showResultDialog) {
                        ResultDialog(
                            score = dialogScore,
                            hasNextLevel = dialogHasNextLevel,
                            onRepeat = {
                                showResultDialog = false
                                startLevel(currentBpm)
                            },
                            onNext = {
                                showResultDialog = false
                                currentLevelId++
                                currentBpm = when (currentLevelId) {
                                    1 -> 80
                                    2 -> 100
                                    3 -> 120
                                    else -> 120
                                }
                                startLevel(currentBpm)
                            },
                            onSelectLevel = {
                                showResultDialog = false
                                currentScreen = Screen.LevelSelect
                            },
                            onDismiss = { showResultDialog = false }
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
    object Calibration : Screen("Калибровка")
    object GameWithNotes : Screen("Тренировка")
}

@Composable
fun NavigationHost(
    currentScreen: Screen,
    score: Int,
    lastResult: String,
    vibrationEnabled: Boolean,
    calibrationOffset: Int,
    isCalibrating: Boolean,
    calibrationTapCount: Int,
    calibrationAvgDev: Int,
    currentLevelId: Int,
    currentBpm: Int,
    onNavigate: (Screen) -> Unit,
    onLevelSelected: (Int, Int) -> Unit,
    onStartLevel: (bpm: Int) -> Unit,
    onStopRhythm: () -> Unit,
    onTap: () -> Unit,
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
            modifier = androidx.compose.ui.Modifier
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
                    calibrationOffset = calibrationOffset,
                    onVibrationChanged = onVibrationChanged,
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
                Screen.GameWithNotes -> {
                    val totalNotes = onGetTotalNotes()
                    GameScreenWithNotes(
                        levelId = currentLevelId,
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
fun GameScreenWithNotes(
    levelId: Int,
    bpm: Int,
    score: Int,
    lastResult: String,
    totalNotes: Int,
    allProgresses: FloatArray,
    noteColors: Map<Int, Color>,
    onBack: () -> Unit,
    onStartLevel: () -> Unit,
    onTap: () -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val screenWidth = maxWidth
        val screenHeight = maxHeight

        Canvas(modifier = Modifier.fillMaxSize()) {
            val widthPx = size.width
            val heightPx = size.height

            // Фон (чёрный для контраста)
            drawRect(color = Color.Black, size = size)

            // Параметры движения нот
            val startX = widthPx + 200f   // за правым краем
            val endX = -200f              // за левым краем
            val hitLineX = widthPx / 2f   // центральная линия

            for (i in 0 until totalNotes) {
                val progress = if (i < allProgresses.size) allProgresses[i] else -1f
                if (progress in 0f..1f) {
                    // Линейная интерполяция координаты X
                    val x = startX + (endX - startX) * progress
                    val y = heightPx / 2 + (if (i % 2 == 0) -40f else 40f)
                    val noteColor = noteColors[i] ?: Color.White

                    drawOval(
                        color = noteColor,
                        topLeft = Offset(x - 30f, y - 20f),
                        size = androidx.compose.ui.geometry.Size(60f, 40f)
                    )
                    drawLine(
                        color = noteColor,
                        start = Offset(x + 27f, y - 90f),
                        end = Offset(x + 27f, y),
                        strokeWidth = 5f
                    )
                }
            }

            // Зелёная линия попадания
            drawLine(
                color = Color.Green,
                start = Offset(hitLineX, 0f),
                end = Offset(hitLineX, heightPx),
                strokeWidth = 4f
            )
        }

        // Интерфейс поверх Canvas (без изменений)
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.DarkGray.copy(alpha = 0.8f)
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

            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = onStartLevel,
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
    onSelectLevel: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Уровень пройден!") },
        text = { Text("Ваш счёт: $score") },
        confirmButton = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onRepeat,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Повторить")
                }
                if (hasNextLevel) {
                    Button(
                        onClick = onNext,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Следующий уровень")
                    }
                }
                Button(
                    onClick = onSelectLevel,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Выбор уровня")
                }
            }
        }
    )
}


data class LevelInfo(val id: Int, val name: String, val description: String, val bpm: Int)