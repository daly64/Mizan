package tn.mizan.app

import android.app.Application
import tn.mizan.app.data.MizanDatabase

class MizanApplication : Application() {
    val database: MizanDatabase by lazy { MizanDatabase.get(this) }

    override fun onCreate() {
        super.onCreate()
        // La v0.3 pouvait enregistrer une clé API (fichier chiffré) : on l'efface définitivement.
        deleteSharedPreferences("mizan_secure")
    }
}
