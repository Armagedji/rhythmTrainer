package com.example.rhythmtrainer.learning

import androidx.annotation.RawRes
import com.example.rhythmtrainer.R

data class Lesson(
    val id: String,
    val title: String,
    val description: String,
    @RawRes val videoResId: Int? = null   // ID видео из res/raw
)

object LessonsRepository {
    val lessons = listOf(
        Lesson(
            id = "intro",
            title = "Что такое ритм?",
            description = "Ритм – это равномерное чередование звуков...",
            videoResId = R.raw.rhythm_intro
        ),
        Lesson(
            id = "notes",
            title = "Какие бывают ноты?",
            description = "Нота - это...",
            videoResId = null
        ),
        // Добавляйте новые уроки сюда
    )
}