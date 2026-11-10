package tn.mizan.app.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import tn.mizan.app.R

/**
 * Sons du splash, embarqués dans l'app (res/raw) : un carillon quand la balance se pose,
 * puis un léger bruit de plume pendant l'écriture de ميزان.
 * Muets si le téléphone est en silencieux ou en vibreur, ou si l'option est coupée dans Profil.
 */
class SplashSounds(context: Context) {

    private val enabled: Boolean = isEnabled(context) &&
        (context.getSystemService(AudioManager::class.java)?.ringerMode == AudioManager.RINGER_MODE_NORMAL)

    private val pool: SoundPool? = if (!enabled) null else SoundPool.Builder()
        .setMaxStreams(2)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val loaded = mutableSetOf<Int>()
    private val pending = mutableSetOf<Int>()

    private val chime: Int = pool?.load(context, R.raw.splash_chime, 1) ?: 0
    private val plume: Int = pool?.load(context, R.raw.splash_plume, 1) ?: 0

    init {
        pool?.setOnLoadCompleteListener { p, id, status ->
            if (status == 0) {
                loaded += id
                if (pending.remove(id)) p.play(id, 1f, 1f, 1, 0, 1f)
            }
        }
    }

    fun playChime() = play(chime)
    fun playPlume() = play(plume)

    private fun play(id: Int) {
        val p = pool ?: return
        if (id in loaded) p.play(id, 1f, 1f, 1, 0, 1f) else pending += id
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
