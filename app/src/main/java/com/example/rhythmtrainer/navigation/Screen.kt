// com/example/rhythmtrainer/navigation/Screen.kt
package com.example.rhythmtrainer.navigation

sealed class Screen {
    object MainMenu : Screen()
    object Learning : Screen()
    object Settings : Screen()
    object LevelSelect : Screen()
    object Calibration : Screen()
    object GameScreen : Screen()
}