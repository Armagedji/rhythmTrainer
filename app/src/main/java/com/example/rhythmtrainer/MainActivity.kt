package com.example.rhythmtrainer

import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.example.rhythmtrainer.ui.theme.RhythmTrainerTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import com.example.rhythmtrainer.learning.LearningScreen
import com.example.rhythmtrainer.ui.screens.LevelSelectScreen
import com.example.rhythmtrainer.ranks.GameResultsStorage
import com.example.rhythmtrainer.ui.screens.SettingsScreen
import com.example.rhythmtrainer.ui.screens.CalibrationScreen
import com.example.rhythmtrainer.settings.SettingsStorage
import com.example.rhythmtrainer.ui.components.AppTopBar

class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    private lateinit var settingsStorage: SettingsStorage

    private var vibrator: Vibrator? = null

    init {
        try {
            System.loadLibrary("rhythmtrainer")
            Log.d(TAG, "Library loaded successfully")
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "Failed to load library: ${e.message}")
        }
    }

    private external fun nativeInit()
    private external fun startRhythm(bpm: Int)
    private external fun stopRhythm()
    private external fun onTap()
    private external fun isRhythmPlaying(): Boolean
    private external fun startCalibration(bpm: Int)
    private external fun stopCalibration()
    private external fun getCalibrationOffset(): Int
    private external fun setCalibrationOffset(offsetMs: Int)
    private external fun getTotalNotes(): Int
    private external fun setAllNotesProgressCallback()
    private external fun pauseRhythm()
    private external fun resumeRhythm()
    private external fun isRhythmPaused(): Boolean

    private val _score = mutableIntStateOf(0)
    private val _lastResult = mutableStateOf("")
    private val _soundEnabled = mutableStateOf(true)
    private val _vibrationEnabled = mutableStateOf(true)
    private val _calibrationOffset = mutableStateOf(getCalibrationOffset())
    private val _calibrationTapCount = mutableStateOf(0)
    private val _calibrationAvgDev = mutableStateOf(0)
    private val _isCalibrating = mutableStateOf(false)
    private val _notePositions = mutableStateOf<Map<Int, Float>>(emptyMap())
    private val _noteColors = mutableStateOf<Map<Int, Color>>(emptyMap())
    private val _allProgresses = mutableStateOf(floatArrayOf())
    private val _isPaused = mutableStateOf(false)

    private var currentScreen by mutableStateOf<Screen>(Screen.MainMenu)
    private var currentLevelId by mutableStateOf(1)
    private var currentBpm by mutableStateOf(80)

    private var showResultDialog by mutableStateOf(false)
    private var dialogScore by mutableStateOf(0)
    private var dialogHasNextLevel by mutableStateOf(false)

    private var levelCompletionJob: Job? = null

    private var levelStartTime: Long = 0L
    private var totalLevelDuration: Long = 0L

    private lateinit var gameResultsStorage: GameResultsStorage

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
            updateNoteColorFromResult(result)
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
                settingsStorage.calibrationOffset = offset
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


    fun togglePause() {
        if (_isPaused.value) {
            // Возобновляем
            resumeRhythm()
            _isPaused.value = false

            val elapsed = SystemClock.elapsedRealtime() - levelStartTime
            val remaining = totalLevelDuration - elapsed
            if (remaining > 0) {
                levelCompletionJob?.cancel()
                levelCompletionJob = lifecycleScope.launch {
                    delay(remaining)
                    if (isRhythmPlaying()) {
                        stopRhythm()
                        dialogScore = _score.value
                        dialogHasNextLevel = (currentLevelId < 3)
                        showResultDialog = true
                    }
                }
            }
        } else {
            // Ставим на паузу
            pauseRhythm()
            _isPaused.value = true
            levelCompletionJob?.cancel()
        }
    }

    private fun setSoundEnabled(enabled: Boolean) {
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        if (enabled) {
            audioManager.setStreamVolume(
                AudioManager.STREAM_MUSIC,
                audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) / 2,
                0
            )
        } else {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0)
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
        _allProgresses.value = floatArrayOf()
        _noteColors.value = emptyMap()
        _isPaused.value = false

        levelStartTime = System.currentTimeMillis()
        val beatDurationMs = 60000L / bpm
        totalLevelDuration = (notesCount - 1) * beatDurationMs + 300

        startRhythm(bpm)
        levelCompletionJob = lifecycleScope.launch {
            delay(totalLevelDuration)
            if (isRhythmPlaying()) {
                stopRhythm()
                dialogScore = _score.value
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

        gameResultsStorage = GameResultsStorage(this)
        settingsStorage = SettingsStorage(this)
        _soundEnabled.value = settingsStorage.soundEnabled
        _vibrationEnabled.value = settingsStorage.vibrationEnabled
        _calibrationOffset.value = settingsStorage.calibrationOffset

        setCalibrationOffset(settingsStorage.calibrationOffset)
        setSoundEnabled(settingsStorage.soundEnabled)

        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }

        val onSoundChanged: (Boolean) -> Unit = { enabled ->
            setSoundEnabled(enabled)
        }

        nativeInit()
        setAllNotesProgressCallback()

        setContent {

            RhythmTrainerTheme {
                Box {
                    NavigationHost(
                        currentScreen = currentScreen,
                        score = _score.value,
                        lastResult = _lastResult.value,
                        soundEnabled = _soundEnabled.value,
                        vibrationEnabled = _vibrationEnabled.value,
                        calibrationOffset = _calibrationOffset.value,
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
                        isPaused = _isPaused.value,           // ← добавь
                        onTogglePause = { togglePause() },    // ← добавь
                        onStartLevel = { bpm -> startLevel(bpm) },
                        onStopRhythm = { stopRhythm() },
                        onTap = { onTap() },
                        onSoundChanged ={
                            enabled -> _soundEnabled.value = enabled
                            settingsStorage.soundEnabled = enabled
                            setSoundEnabled(enabled)
                                         },
                        onVibrationChanged = {
                            enabled -> _vibrationEnabled.value = enabled
                            settingsStorage.vibrationEnabled = enabled
                                             },
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
                        noteColors = _noteColors.value,
                        gameResultsStorage = gameResultsStorage,
                    )

                    if (showResultDialog) {
                        ResultDialog(
                            score = dialogScore,
                            hasNextLevel = dialogHasNextLevel,
                            onRepeat = {
                                gameResultsStorage.setBestScore(currentLevelId, dialogScore)
                                showResultDialog = false
                                startLevel(currentBpm)
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
                                startLevel(currentBpm)
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
    soundEnabled: Boolean,
    vibrationEnabled: Boolean,
    calibrationOffset: Int,
    calibrationTapCount: Int,
    calibrationAvgDev: Int,
    currentLevelId: Int,
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
    gameResultsStorage: GameResultsStorage,
    isPaused: Boolean,
    onTogglePause: () -> Unit
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
                        noteColors = noteColors,
                        isPaused = isPaused,              // ← новый
                        onTogglePause = onTogglePause
                    )
                }
            }
        }
    }
}

@Composable
fun MainMenuScreen(onNavigate: (Screen) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        AppTopBar(title = "Главное меню")

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Название приложения
            Text(
                text = "Rhythm Trainer",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Приложение для развития чувства ритма",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Кнопки
            MenuButton(
                text = "🎯 Уровни",
                onClick = { onNavigate(Screen.LevelSelect) }
            )
            Spacer(modifier = Modifier.height(16.dp))
            MenuButton(
                text = "📚 Обучение",
                onClick = { onNavigate(Screen.Learning) }
            )
            Spacer(modifier = Modifier.height(16.dp))
            MenuButton(
                text = "⚙️ Настройки",
                onClick = { onNavigate(Screen.Settings) }
            )
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
    onTap: () -> Unit,
    isPaused: Boolean,
    onTogglePause: () -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val screenWidth = maxWidth
        val screenHeight = maxHeight

        Canvas(modifier = Modifier.fillMaxSize()) {
            val widthPx = size.width
            val heightPx = size.height

            drawRect(color = Color.Black, size = size)

            val startX = widthPx + 200f
            val endX = -200f
            val hitLineX = widthPx / 2f

            for (i in 0 until totalNotes) {
                val progress = if (i < allProgresses.size) allProgresses[i] else -1f
                if (progress in 0f..1f) {
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

            drawLine(
                color = Color.Green,
                start = Offset(hitLineX, 0f),
                end = Offset(hitLineX, heightPx),
                strokeWidth = 4f
            )
        }

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
                    onClick = onTogglePause,
                    modifier = Modifier.fillMaxWidth().height(60.dp)
                ) {
                    Text(if (isPaused) "▶ Продолжить" else "⏸ Пауза", fontSize = 18.sp)
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

@Composable
private fun MenuButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Text(
            text = text,
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

data class LevelInfo(val id: Int, val name: String, val description: String, val bpm: Int)