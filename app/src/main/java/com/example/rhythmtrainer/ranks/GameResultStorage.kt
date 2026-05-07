// com/example/rhythmtrainer/ranks/GameResultsStorage.kt
package com.example.rhythmtrainer.ranks

import android.content.Context
import android.content.SharedPreferences

class GameResultsStorage(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("game_results", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_BEST_SCORE_PREFIX = "best_score_level_"
        private const val KEY_TOTAL_SCORE = "total_score"
    }

    // Лучший результат на уровне
    fun getBestScore(levelId: Int): Int {
        return prefs.getInt(KEY_BEST_SCORE_PREFIX + levelId, 0)
    }

    fun setBestScore(levelId: Int, score: Int) {
        val current = getBestScore(levelId)
        if (score > current) {
            val diff = score - current
            prefs.edit()
                .putInt(KEY_BEST_SCORE_PREFIX + levelId, score)
                .putInt(KEY_TOTAL_SCORE, getTotalScore() + diff)
                .apply()
        }
    }

    // Общий счёт (сумма лучших результатов)
    fun getTotalScore(): Int {
        return prefs.getInt(KEY_TOTAL_SCORE, 0)
    }

    fun getBestScores(vararg levelIds: Int): Map<Int, Int> {
        return levelIds.associateWith { getBestScore(it) }
    }
}