package com.example.rhythmtrainer.game

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.LifecycleCoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class GameManager(
    private val lifecycleScope: LifecycleCoroutineScope,
    private val onScoreUpdated: (Int) -> Unit = {},
    private val onResultUpdated: (String) -> Unit = {},
    private val onAllNotesProgressUpdated: (FloatArray) -> Unit = {}
) {
    companion object {
        private const val TAG = "GameManager"
    }

    // Состояния игры
    var score by mutableIntStateOf(0)
        private set
    var lastResult by mutableStateOf("")
        private set
    var allProgresses by mutableStateOf(floatArrayOf())
        private set
    var noteColors by mutableStateOf<Map<Int, Color>>(emptyMap())
        private set
    var isRhythmPlaying = false
        private set

    private var levelCompletionJob: Job? = null

    // Нативные методы (должны быть реализованы в MainActivity)
    var startRhythm: (bpm: Int) -> Unit = {}
    var stopRhythm: () -> Unit = {}
    var onTap: () -> Unit = {}
    var isRhythmPlayingNative: () -> Boolean = { false }

    fun updateScore(newScore: Int) {
        score = newScore
        onScoreUpdated(newScore)
    }

    fun updateResult(result: String) {
        lastResult = result
        onResultUpdated(result)
        updateNoteColorFromResult(result)
    }

    fun updateAllNotesProgress(progress: FloatArray) {
        allProgresses = progress
        onAllNotesProgressUpdated(progress)
    }

    fun startLevel(bpm: Int, notesCount: Int = 32) {
        levelCompletionJob?.cancel()
        score = 0
        lastResult = ""
        allProgresses = floatArrayOf()
        noteColors = emptyMap()
        startRhythm(bpm)
        isRhythmPlaying = true
        val beatDurationMs = 60000L / bpm
        val durationMs = (notesCount - 1) * beatDurationMs
        levelCompletionJob = lifecycleScope.launch {
            delay(durationMs + 300)
            if (isRhythmPlayingNative()) {
                stopLevel()
            }
        }
    }

    fun stopLevel() {
        levelCompletionJob?.cancel()
        if (isRhythmPlayingNative()) {
            stopRhythm()
        }
        isRhythmPlaying = false
    }

    fun reset() {
        score = 0
        lastResult = ""
        allProgresses = floatArrayOf()
        noteColors = emptyMap()
        isRhythmPlaying = false
    }

    private fun updateNoteColorFromResult(result: String) {
        val progresses = allProgresses
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
            noteColors = noteColors.toMutableMap().apply {
                put(targetIndex, color)
            }
        }
    }
}