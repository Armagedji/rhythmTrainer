package com.example.rhythmtrainer.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

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