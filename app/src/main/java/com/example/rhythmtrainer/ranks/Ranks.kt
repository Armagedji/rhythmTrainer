package com.example.rhythmtrainer.ranks

import androidx.compose.ui.graphics.Color

data class Rank(
    val name: String,
    val minScore: Int,          // минимальный общий счёт для звания
    val color: Color,
    val emoji: String
)

val RANKS = listOf(
    Rank("Новичок",     0,    Color.Gray,            "🔰"),
    Rank("Ученик",      100,  Color(0xFF4CAF50),     "🥉"),
    Rank("Любитель",    300,  Color(0xFF2196F3),     "🥈"),
    Rank("Музыкант",    600,  Color(0xFFFF9800),     "🥇"),
    Rank("Виртуоз",     1000, Color(0xFFE91E63),     "💎"),
    Rank("Мастер ритма", 1500, Color(0xFF9C27B0),    "👑"),
    Rank("Легенда",     2000, Color.Yellow,            "🌟")
)

// Максимальный счёт среди всех званий (для прогресс-бара)
val MAX_RANK_SCORE = RANKS.last().minScore

fun getCurrentRank(totalScore: Int): Rank {
    return RANKS.lastOrNull { totalScore >= it.minScore } ?: RANKS.first()
}

fun getNextRank(totalScore: Int): Rank? {
    return RANKS.firstOrNull { totalScore < it.minScore }
}