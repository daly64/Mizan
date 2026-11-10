package tn.mizan.app.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.speech.tts.TextToSpeech
import tn.mizan.app.R
import java.util.Locale

/**
 * Sons du splash :
 * - un carillon quand la balance se pose (fichier embarqué, res/raw) ;
 * - le mot « ميزان » prononcé en arabe pendant qu'il s'écrit, par la synthèse vocale
 *   du téléphone, avec une voix hors ligne uniquement (rien ne passe par internet) ;
 * - un léger bruit de plume, plus discret, sous la voix.
 * Tout reste muet en silencieux ou en vibreur, ou si l'option est coupée dans Profil.
 */
class SplashSounds(context: Context) {

    private val enabled: Boolean = isEnabled(context) &&
        (context.getSystemService(AudioManager::class.java)?.ringerMode == AudioManager.RINGER_MODE_NORMAL)

    private val attributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    // --- Carillon et plume (fichiers embarqués) ---

    private val pool: SoundPool? =
        if (!enabled) null else SoundPool.Builder().setMaxStreams(2).setAudioAttributes(attributes).build()

    private val loaded = mutableSetOf<Int>()
    private val pending = mutableMapOf<Int, Float>()

    private val chime: Int = pool?.load(context, R.raw.splash_chime, 1) ?: 0
    private val plume: Int = pool?.load(context, R.raw.splash_plume, 1) ?: 0

    // --- Voix arabe (synthèse vocale du téléphone, hors ligne) ---

    private var tts: TextToSpeech? = null
    private var voiceReady = false
    private var voicePending = false

    init {
        pool?.setOnLoadCompleteListener { p, id, status ->
            if (status == 0) {
                loaded += id
                pending.remove(id)?.let { volume -> p.play(id, volume, volume, 1, 0, 1f) }
            }
        }
        if (enabled) {
            tts = TextToSpeech(context.applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) setUpArabicVoice()
            }
        }
    }

    private fun setUpArabicVoice() {
        val engine = tts ?: return
        // Une voix arabe installée sur le téléphone et qui ne demande pas internet
        val voice = runCatching {
            engine.voices
                ?.filter { it.locale.language == "ar" && !it.isNetworkConnectionRequired }
                ?.maxByOrNull { it.quality }
        }.getOrNull()
        val ok = if (voice != null) {
            engine.voice = voice
            true
        } else {
            val r = engine.setLanguage(Locale("ar"))
            r != TextToSpeech.LANG_MISSING_DATA && r != TextToSpeech.LANG_NOT_SUPPORTED
        }
        if (!ok) return
        engine.setAudioAttributes(attributes)
        engine.setSpeechRate(0.8f)
        engine.setPitch(0.95f)
        voiceReady = true
        if (voicePending) speak()
    }

    private fun speak() {
        voicePending = false
        tts?.speak(NAME, TextToSpeech.QUEUE_FLUSH, null, "mizan_splash")
    }

    fun playChime() = play(chime, 1f)

    /** Au début de l'écriture : la voix dit « ميزان », la plume reste en fond. */
    fun playName() {
        play(plume, 0.45f)
        if (!enabled) return
        if (voiceReady) speak() else voicePending = true
    }

    private fun play(id: Int, volume: Float) {
        val p = pool ?: return
        if (id in loaded) p.play(id, volume, volume, 1, 0, 1f) else pending[id] = volume
    }

    fun release() {
        pool?.release()
        // On laisse la voix finir le mot avant de libérer le moteur
        tts?.let { engine ->
            if (engine.isSpeaking) {
                engine.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}
                    override fun onDone(utteranceId: String?) = engine.shutdown()
                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) = engine.shutdown()
                })
            } else {
                engine.shutdown()
            }
        }
        tts = null
    }

    companion object {
        private const val NAME = "ميزان"
        private const val PREFS = "mizan_reglages"
        private const val KEY = "son_demarrage"

        fun isEnabled(context: Context): Boolean =
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY, true)

        fun setEnabled(context: Context, value: Boolean) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY, value).apply()
        }
    }
}
