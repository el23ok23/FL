package com.mifleteciudad.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.mifleteciudad.app.ui.MiFleteApp
import com.mifleteciudad.app.ui.theme.MiFleteTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // El splash del sistema se cierra apenas Compose dibuja el primer frame.
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MiFleteTheme {
                MiFleteApp()
            }
        }
    }
}
