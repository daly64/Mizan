package tn.mizan.app

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.IntentCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import tn.mizan.app.ui.MizanApp
import tn.mizan.app.ui.theme.MizanTheme

class MainActivity : ComponentActivity() {

    /** Texte reçu par partage (réponse de Claude ou fichier CSV) et compteur de partages reçus. */
    private val sharedText = mutableStateOf<String?>(null)
    private val shareCount = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        // Splash système (logo sur fond émeraude), puis splash animé en Compose.
        installSplashScreen()
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) handleShare(intent)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        )
        setContent {
            MizanTheme {
                MizanApp(sharedText = sharedText.value, shareRequest = shareCount.intValue)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleShare(intent)
    }

    private fun handleShare(intent: Intent?) {
        if (intent?.action != Intent.ACTION_SEND) return
        val text = intent.getStringExtra(Intent.EXTRA_TEXT)?.takeIf { it.isNotBlank() } ?: readStream(intent)
        if (!text.isNullOrBlank()) {
            sharedText.value = text
            shareCount.intValue += 1
        }
    }

    private fun readStream(intent: Intent): String? {
        val uri = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java) ?: return null
        return try {
            contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText().take(500_000) }
        } catch (_: Exception) {
            null
        }
    }
}
