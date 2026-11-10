package tn.mizan.app.claude

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Clé API Claude (console Anthropic) et modèle, chiffrés sur le téléphone (Android Keystore).
 * La clé n'est jamais dans le code ni sur GitHub, et n'est envoyée qu'à api.anthropic.com.
 */
class ClaudeSettings(context: Context) {

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
        const val FILE_NAME = "mizan_claude"
        const val DEFAULT_MODEL = "claude-haiku-5-5"
        private const val KEY_API = "anthropic_api_key"
        private const val KEY_MODEL = "anthropic_model"
    }
}
