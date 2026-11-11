package com.erengokturk.booklate

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.erengokturk.booklate.di.LocalAppContainer
import com.erengokturk.booklate.domain.model.UserSettings
import com.erengokturk.booklate.ui.BooklateNavRoot
import com.erengokturk.booklate.ui.theme.BooklateTheme

class MainActivity : ComponentActivity() {
    private val container by lazy { (application as BooklateApp).container }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val settings by container.settingsRepository.settings
                .collectAsStateWithLifecycle(initialValue = UserSettings())

            BooklateTheme(themeMode = settings.themeMode) {
                CompositionLocalProvider(LocalAppContainer provides container) {
                    BooklateNavRoot()
                }
            }
        }
    }

    override fun onDestroy() {
        if (isFinishing) {
            container.shutdown()
        }
        super.onDestroy()
    }
}
