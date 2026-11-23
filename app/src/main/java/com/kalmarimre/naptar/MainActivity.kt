package com.kalmarimre.naptar

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {

    private val requestNotifications =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Notifier.ensureChannel(this)
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        // Újraindítás és frissítés után is megvannak az emlékeztetők
        Scheduler.scheduleAll(this, EntryStore.load(this))

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = Color(0xFF0B0B0D),
                    surface = Color(0xFF0B0B0D),
                    surfaceContainerHigh = Color(0xFF141417),
                    onSurface = Color(0xFFF2F2F4),
                    onSurfaceVariant = Color(0xFF9A9AA3),
                    primary = Color(0xFFF26B1D),
                    onPrimary = Color(0xFF120A05),
                    primaryContainer = Color(0xFFF26B1D),
                    onPrimaryContainer = Color(0xFF120A05),
                    secondaryContainer = Color(0xFFF26B1D),
                    onSecondaryContainer = Color(0xFF120A05)
                )
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    CalendarScreen()
                }
            }
        }
    }
}
