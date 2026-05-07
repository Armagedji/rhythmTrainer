package com.example.rhythmtrainer.learning

import android.net.Uri
import android.view.Gravity
import android.view.TextureView
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer

@Composable
fun LessonDetailScreen(lesson: Lesson, onBack: () -> Unit) {
    val context = LocalContext.current
    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }

    // Создаём плеер при появлении экрана
    LaunchedEffect(lesson.videoResId) {
        if (lesson.videoResId != null && exoPlayer == null) {
            val player = ExoPlayer.Builder(context).build().apply {
                val uri = Uri.parse("android.resource://${context.packageName}/${lesson.videoResId}")
                setMediaItem(MediaItem.fromUri(uri))
                prepare()
                playWhenReady = true
            }
            exoPlayer = player
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // Кнопка "Назад к списку"
        Button(
            onClick = onBack,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text("Назад к списку")
        }

        // Контент урока с прокруткой
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = lesson.title,
                fontSize = 24.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = lesson.description,
                fontSize = 16.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (lesson.videoResId != null && exoPlayer != null) {
                AndroidView(
                    factory = { context ->
                        // Создаём контейнер
                        val frameLayout = FrameLayout(context)
                        frameLayout.layoutParams = FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.MATCH_PARENT
                        )

                        // Создаём TextureView
                        val textureView = TextureView(context)
                        textureView.layoutParams = FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            Gravity.CENTER
                        )
                        frameLayout.addView(textureView)

                        // Назначаем TextureView плееру
                        exoPlayer?.setVideoTextureView(textureView)

                        // Не используем стандартный PlayerView, чтобы избежать конфликтов
                        frameLayout
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { exoPlayer?.play() }) { Text("Play") }
                    Button(onClick = { exoPlayer?.pause() }) { Text("Pause") }
                }
            } else if (lesson.videoResId != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .background(Color.LightGray),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Загрузка видео...", color = Color.Black)
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .background(Color.LightGray),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Видео отсутствует", color = Color.Black)
                }
            }
        }
    }
}