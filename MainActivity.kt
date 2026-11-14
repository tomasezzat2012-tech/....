package com.learnenglishsimply.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color

private val BabyBlue = Color(0xFF6EC6F0)
private val DeepBlue = Color(0xFF2B8FC7)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ReminderWorker.schedule(this)
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = DeepBlue, onPrimary = Color.White,
                    primaryContainer = Color(0xFFDFF3FD), onPrimaryContainer = Color(0xFF0B3D5A),
                    secondary = BabyBlue, background = Color.White, surface = Color.White,
                    surfaceVariant = Color(0xFFEFF8FD)
                )
            ) {
                Surface(color = Color.White) {
                    AskNotificationPermission()
                    AppRoot()
                }
            }
        }
    }
}

@Composable
private fun AskNotificationPermission() {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
