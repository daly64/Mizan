package tn.mizan.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
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
            }
            MizanTheme {
                MizanApp()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // Fin de session (l'app passe en arrière-plan ou se ferme) : sauvegarde automatique
        if (!isChangingConfigurations) (application as MizanApplication).backupSession()
    }
}
