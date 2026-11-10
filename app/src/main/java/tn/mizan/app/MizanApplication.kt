package tn.mizan.app

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import tn.mizan.app.data.Backup
import tn.mizan.app.data.MizanDatabase
import tn.mizan.app.ui.theme.ThemeSettings

class MizanApplication : Application() {
    val database: MizanDatabase by lazy { MizanDatabase.get(this) }

    /** Tâches qui doivent finir même quand l'écran se ferme (sauvegarde de fin de session). */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        ThemeSettings.load(this)
        // Les versions 0.3 et 0.8 pouvaient enregistrer une clé API : on l'efface définitivement.
        deleteSharedPreferences("mizan_secure")
        deleteSharedPreferences("mizan_claude")
        deleteSharedPreferences("mizan_prefs")
    }

    /** Sauvegarde automatique à la fin de chaque session. */
    fun backupSession() {
        appScope.launch { Backup.saveNow(this@MizanApplication, database) }
    }
}
