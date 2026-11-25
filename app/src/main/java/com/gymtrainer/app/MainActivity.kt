package com.gymtrainer.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.gymtrainer.app.data.Settings
import com.gymtrainer.app.ui.navigation.AppRoot
import com.gymtrainer.app.ui.theme.GymTrainerTheme
import com.gymtrainer.app.ui.theme.ThemeMode
import com.gymtrainer.app.ui.theme.ThemeState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        ThemeState.mode = runCatching { ThemeMode.valueOf(Settings(this).themeMode) }.getOrDefault(ThemeMode.LIGHT)
        setContent {
            GymTrainerTheme {
                AppRoot()
            }
        }
    }
}
