package com.example.rhythmtrainer.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
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
                    Color.Green
                else
                    Color.Yellow
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
            Text(
                if (tapCount >= 3) "✅ Завершить калибровку"
                else "Сделайте хотя бы 3 нажатия"
            )
        }

        Button(onClick = onCancel) {
            Text("❌ Отмена")
        }
    }
}