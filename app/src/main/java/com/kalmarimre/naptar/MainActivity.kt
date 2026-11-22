package com.kalmarimre.naptar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = Color(0xFF0B0B0D),
                    surface = Color(0xFF0B0B0D),
                    onSurface = Color(0xFFF2F2F4),
                    onSurfaceVariant = Color(0xFF9A9AA3),
                    primary = Color(0xFFF26B1D)
                )
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    CalendarScreen()
                }
            }
        }
    }
}
