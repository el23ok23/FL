package com.fletaba.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.graphics.Color
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.fletaba.app.data.AppConfig
import com.fletaba.app.ui.FletabaApp
import com.fletaba.app.ui.theme.FletabaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // El splash del sistema se cierra apenas Compose dibuja el primer frame.
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val brand = AppConfig.load(this).brand
        setContent {
            FletabaTheme(primary = Color(brand.primary), accent = Color(brand.accent)) {
                FletabaApp()
            }
        }
    }
}
