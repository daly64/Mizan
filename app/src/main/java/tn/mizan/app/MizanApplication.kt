package tn.mizan.app

import android.app.Application
import tn.mizan.app.data.MizanDatabase

class MizanApplication : Application() {
    val database: MizanDatabase by lazy { MizanDatabase.get(this) }

    override fun onCreate() {
        super.onCreate()
        // Les versions 0.3 et 0.8 pouvaient enregistrer une clé API : on l'efface définitivement.
        deleteSharedPreferences("mizan_secure")
        deleteSharedPreferences("mizan_claude")
        deleteSharedPreferences("mizan_prefs")
    }
}
