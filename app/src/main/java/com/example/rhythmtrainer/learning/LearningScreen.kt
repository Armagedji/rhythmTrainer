package com.example.rhythmtrainer.learning

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LearningScreen(onBack: () -> Unit) {
    var selectedLesson by remember { mutableStateOf<Lesson?>(null) }

    // Локальная неизменяемая переменная
    val lesson = selectedLesson

    if (lesson == null) {
        // Список уроков
        Column(modifier = Modifier.fillMaxSize()) {
            Button(
                onClick = onBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text("Назад")
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(LessonsRepository.lessons.size) { index ->
                    val lessonItem = LessonsRepository.lessons[index]
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedLesson = lessonItem }
                    ) {
                        Text(
                            text = "${index + 1}. ${lessonItem.title}",
                            modifier = Modifier.padding(16.dp),
                            fontSize = 18.sp
                        )
                    }
                }
            }
        }
    } else {
        LessonDetailScreen(
            lesson = lesson,
            onBack = { selectedLesson = null }
        )
    }
}