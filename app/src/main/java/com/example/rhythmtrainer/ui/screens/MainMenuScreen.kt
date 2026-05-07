package com.example.rhythmtrainer.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.example.rhythmtrainer.navigation.Screen

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