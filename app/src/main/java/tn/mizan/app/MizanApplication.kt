package tn.mizan.app

import android.app.Application
import tn.mizan.app.data.MizanDatabase

class MizanApplication : Application() {
    val database: MizanDatabase by lazy { MizanDatabase.get(this) }
}
