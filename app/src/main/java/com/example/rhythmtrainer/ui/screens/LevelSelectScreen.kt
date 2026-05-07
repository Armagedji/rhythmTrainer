package com.example.rhythmtrainer.ui.screens

import com.example.rhythmtrainer.model.LevelInfo

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rhythmtrainer.ranks.GameResultsStorage
import com.example.rhythmtrainer.ranks.getCurrentRank
import com.example.rhythmtrainer.ranks.getNextRank

@Composable
fun LevelSelectScreen(
    onBack: () -> Unit,
    onLevelSelected: (levelId: Int, bpm: Int) -> Unit,
    gameResultsStorage: GameResultsStorage = GameResultsStorage(LocalContext.current)
) {
    val levels = listOf(
        LevelInfo(1, "Уровень 1: Четверти", "BPM: 80", 80),
        LevelInfo(2, "Уровень 2: Восьмые", "BPM: 100", 100),
        LevelInfo(3, "Уровень 3: Паузы", "BPM: 120", 120)
    )

    // Получаем лучшие результаты для всех уровней
    val bestScores = remember {
        gameResultsStorage.getBestScores(1, 2, 3)
    }

    // Общий счёт и информация о звании
    val totalScore = gameResultsStorage.getTotalScore()
    val currentRank = getCurrentRank(totalScore)
    val nextRank = getNextRank(totalScore)

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Заголовок
        Text(
            text = "Выбор уровня",
            fontSize = 24.sp,
            modifier = Modifier.padding(top = 8.dp)
        )

        // Кнопки уровней с результатами
        levels.forEach { level ->
            val bestScore = bestScores[level.id] ?: 0
            Button(
                onClick = { onLevelSelected(level.id, level.bpm) },
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(level.name)
                    Text(level.description, fontSize = 12.sp)
                    Text(
                        text = if (bestScore > 0) "Лучший результат: $bestScore" else "Не пройден",
                        fontSize = 14.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Блок с информацией о текущем звании и прогрессе
        Card(
            modifier = Modifier.fillMaxWidth(0.85f),
            colors = CardDefaults.cardColors(
                containerColor = currentRank.color.copy(alpha = 0.15f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Текущее звание
                Text(
                    text = "${currentRank.emoji} ${currentRank.name}",
                    fontSize = 22.sp
                )

                // Общий счёт
                Text(
                    text = "Общий счёт: $totalScore",
                    fontSize = 16.sp
                )

                // Прогресс до следующего звания
                if (nextRank != null) {
                    val remaining = nextRank.minScore - totalScore
                    val progress = totalScore.toFloat() / nextRank.minScore

                    Text(
                        text = "До звания «${nextRank.name}»: $remaining очков",
                        fontSize = 14.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                        color = nextRank.color,
                        trackColor = nextRank.color.copy(alpha = 0.2f)
                    )
                } else {
                    Text(
                        text = "🎉 Максимальное звание!",
                        fontSize = 14.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Кнопка "Назад"
        Button(onClick = onBack) {
            Text("Назад")
        }
    }
}