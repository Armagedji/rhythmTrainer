package com.example.rhythmtrainer.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsScreen(
    vibrationEnabled: Boolean,
    soundEnabled: Boolean,
    calibrationOffset: Int,
    onVibrationChanged: (Boolean) -> Unit,
    onSoundChanged: (Boolean) -> Unit,
    onStartCalibration: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Настройки",
            fontSize = 24.sp,
            modifier = Modifier.padding(top = 8.dp)
        )

        // Звук
        Button(
            onClick = { onSoundChanged(!soundEnabled) },
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Text(
                text = "Звук: ${if (soundEnabled) "Вкл" else "Выкл"}",
                fontSize = 16.sp
            )
        }

        // Вибрация
        Button(
            onClick = { onVibrationChanged(!vibrationEnabled) },
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Text(
                text = "Вибрация: ${if (vibrationEnabled) "Вкл" else "Выкл"}",
                fontSize = 16.sp
            )
        }

        // Калибровка
        Text(
            text = "Задержка калибровки: $calibrationOffset мс",
            fontSize = 16.sp
        )

        Button(
            onClick = onStartCalibration,
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Text("🎯 Калибровать задержку", fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.weight(1f))

        // Кнопка "Назад"
        Button(onClick = onBack) {
            Text("Назад")
        }
    }
}