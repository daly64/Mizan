package tn.mizan.app

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableIntStateOf
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import tn.mizan.app.ui.MizanApp
import tn.mizan.app.ui.theme.MizanTheme

class MainActivity : ComponentActivity() {

    /** Augmente à chaque ouverture par le raccourci « Dépense rapide ». */
    private val quickRequests = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        // Splash système (logo sur fond émeraude), puis splash animé en Compose.
        installSplashScreen()
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) handleShortcut(intent)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        )
        setContent {
            MizanTheme {
                MizanApp(quickRequest = quickRequests.intValue)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleShortcut(intent)
    }

    private fun handleShortcut(intent: Intent?) {
        if (intent?.action == ACTION_QUICK_ADD) quickRequests.intValue += 1
    }

    companion object {
        const val ACTION_QUICK_ADD = "tn.mizan.app.QUICK_ADD"
    }
}
