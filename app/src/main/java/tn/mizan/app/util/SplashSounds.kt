package tn.mizan.app.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import tn.mizan.app.R

/**
 * Son du splash : le bruit d'un calame (plume de roseau) qui écrit ميزان, trait par trait,
 * synchronisé avec l'apparition du nom. Fichier embarqué (res/raw), aucune voix, aucun internet.
 * Muet en silencieux ou en vibreur, ou si l'option est coupée dans Profil.
 */
class SplashSounds(context: Context) {

    private val enabled: Boolean = isEnabled(context) &&
        (context.getSystemService(AudioManager::class.java)?.ringerMode == AudioManager.RINGER_MODE_NORMAL)

    private val pool: SoundPool? = if (!enabled) null else SoundPool.Builder()
        .setMaxStreams(1)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private var loaded = false
    private var pending = false
    private val calame: Int = pool?.load(context, R.raw.splash_calame, 1) ?: 0

    init {
        pool?.setOnLoadCompleteListener { p, id, status ->
            if (status == 0 && id == calame) {
                loaded = true
                if (pending) {
                    pending = false
                    p.play(id, 1f, 1f, 1, 0, 1f)
                }
            }
        }
    }

    /** Au moment où le calame commence à écrire ميزان. */
    fun playCalame() {
        val p = pool ?: return
        if (loaded) p.play(calame, 1f, 1f, 1, 0, 1f) else pending = true
    }

    fun release() {
        pool?.release()
    }

    companion object {
        private const val PREFS = "mizan_reglages"
        private const val KEY = "son_demarrage"

        fun isEnabled(context: Context): Boolean =
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY, true)

        fun setEnabled(context: Context, value: Boolean) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY, value).apply()
        }
    }
}
