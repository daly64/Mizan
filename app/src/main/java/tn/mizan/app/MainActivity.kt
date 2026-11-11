package tn.mizan.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import tn.mizan.app.ui.MizanApp
import tn.mizan.app.ui.theme.MizanTheme
import tn.mizan.app.ui.theme.isMizanDark

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Splash système (logo sur fond émeraude), puis splash animé en Compose.
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContent {
            val dark = isMizanDark()
            // Icônes de la barre d'état claires en thème sombre, foncées en thème clair
            LaunchedEffect(dark) {
                val style = if (dark) SystemBarStyle.dark(Color.TRANSPARENT)
                else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                pleinEcran()
            }
            MizanTheme {
                MizanApp()
            }
        }
        pleinEcran()
    }

    /**
     * Plein écran automatique : barre d'état et barre de navigation cachées.
     * Un glissement depuis le bord de l'écran les fait réapparaître un instant, puis elles se recachent.
     */
    private fun pleinEcran() {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        // Au retour dans l'app (après une notification, le sélecteur de fichiers…), on recache les barres.
        if (hasFocus) pleinEcran()
    }

    override fun onStop() {
        super.onStop()
        // Fin de session (l'app passe en arrière-plan ou se ferme) : sauvegarde automatique
        if (!isChangingConfigurations) (application as MizanApplication).backupSession()
    }
}
