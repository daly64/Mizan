package tn.mizan.app.ai

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Clé API et modèle, chiffrés sur le téléphone (Android Keystore).
 * La clé n'est jamais écrite dans le code ni envoyée ailleurs qu'à l'API d'OpenAI.
 */
class SecureSettings(context: Context) {

    private val prefs: SharedPreferences? = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        null
    }

    val isAvailable: Boolean get() = prefs != null

    var apiKey: String?
        get() = prefs?.getString(KEY_API, null)?.takeIf { it.isNotBlank() }
        set(value) {
            prefs?.edit()?.apply {
                if (value.isNullOrBlank()) remove(KEY_API) else putString(KEY_API, value.trim())
            }?.apply()
        }

    var model: String
        get() = prefs?.getString(KEY_MODEL, null)?.takeIf { it.isNotBlank() } ?: DEFAULT_MODEL
        set(value) {
            prefs?.edit()?.putString(KEY_MODEL, value.trim().ifBlank { DEFAULT_MODEL })?.apply()
        }

    companion object {
        const val FILE_NAME = "mizan_secure"
        const val DEFAULT_MODEL = "gpt-4o-mini"
        private const val KEY_API = "openai_api_key"
        private const val KEY_MODEL = "openai_model"
    }
}
