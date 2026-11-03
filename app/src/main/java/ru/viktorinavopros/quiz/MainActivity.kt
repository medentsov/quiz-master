package ru.viktorinavopros.quiz

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import ru.viktorinavopros.quiz.audio.AppSounds
import ru.viktorinavopros.quiz.ui.ViktorinaApp
import ru.viktorinavopros.quiz.ui.theme.ViktorinaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppSounds.init(this)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        setContent {
            ViktorinaTheme {
                ViktorinaApp()
            }
        }
    }
}
