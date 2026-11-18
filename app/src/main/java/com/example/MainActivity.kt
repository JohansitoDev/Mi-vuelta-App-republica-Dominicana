package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.ThemePreferenceStore
import com.example.notifications.VueltaReminderScheduler
import com.example.ui.AppNavigation
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.MiVueltaTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    val initialVueltaId = intent?.getStringExtra(VueltaReminderScheduler.EXTRA_VUELTA_ID)
    setContent {
      val themeStore = remember { ThemePreferenceStore(applicationContext) }
      val themeMode by themeStore.themeMode.collectAsStateWithLifecycle()

      LaunchedEffect(themeMode) {
        if (themeMode == AppThemeMode.BLACK) {
          enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
          )
        } else {
          enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
              android.graphics.Color.TRANSPARENT,
              android.graphics.Color.TRANSPARENT,
            ),
            navigationBarStyle = SystemBarStyle.light(
              android.graphics.Color.TRANSPARENT,
              android.graphics.Color.TRANSPARENT,
            ),
          )
        }
      }

      MiVueltaTheme(themeMode = themeMode) {
        AppNavigation(
          themeMode = themeMode,
          onSetThemeMode = { themeStore.setThemeMode(it) },
          onToggleTheme = { themeStore.toggleTheme() },
          initialVueltaId = initialVueltaId,
        )
      }
    }
  }
}
