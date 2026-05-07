package com.example.rhythmtrainer.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun GameScreen(
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
        maxWidth
        maxHeight

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
                    onClick = onBack,
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Text("Выйти")
                }
            }
        }
    }
}