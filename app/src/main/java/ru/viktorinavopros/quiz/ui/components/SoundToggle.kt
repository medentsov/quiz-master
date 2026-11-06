package ru.viktorinavopros.quiz.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ru.viktorinavopros.quiz.audio.AppSounds
import ru.viktorinavopros.quiz.ui.theme.JeopardyGold
import ru.viktorinavopros.quiz.ui.theme.JeopardyMuted

/** Глобальный выключатель звука: эмбиент, тиканье, реакции хомяка и голос. */
@Composable
fun SoundToggle(modifier: Modifier = Modifier) {
    Surface(
        onClick = { AppSounds.toggleMute() },
        modifier = modifier,
        shape = CircleShape,
        color = Color.White.copy(alpha = 0.07f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
    ) {
        Box(
            modifier = Modifier.size(46.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (AppSounds.muted) {
                    Icons.Default.VolumeOff
                } else {
                    Icons.Default.VolumeUp
                },
                contentDescription = if (AppSounds.muted) {
                    "Включить звук"
                } else {
                    "Выключить звук"
                },
                tint = if (AppSounds.muted) JeopardyMuted else JeopardyGold,
            )
        }
    }
}
