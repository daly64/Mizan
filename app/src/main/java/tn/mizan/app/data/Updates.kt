package tn.mizan.app.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import tn.mizan.app.BuildConfig
import java.net.HttpURLConnection
import java.net.URL

/**
 * Mises à jour d'AL MIZAN, sans Play Store :
 * - l'app regarde les versions publiées sur GitHub (Releases) par la compilation automatique ;
 * - si une version plus récente existe, le navigateur télécharge l'APK et Android propose de l'installer.
 *
 * C'est le seul accès internet de l'app : aucune donnée n'est envoyée, on lit seulement la liste des versions.
 * Les dépenses ne sont pas touchées par une mise à jour (même signature, même identifiant d'app).
 */
object Updates {
    /** Dépôt où la compilation publie les APK. Il doit être public pour que l'app puisse le lire. */
    private const val RELEASES_URL = "https://api.github.com/repos/daly64/mizan/releases?per_page=10"

    private const val PREFS = "mizan_reglages"
    private const val KEY_LAST_CHECK = "maj_derniere_verif"
    private const val KEY_SKIPPED = "maj_ignoree"

    data class Release(val versionCode: Int, val versionName: String, val notes: String, val apkUrl: String, val sizeBytes: Long)

    sealed interface Check {
        data class Available(val release: Release) : Check
        data object UpToDate : Check
        data class Failed(val reason: String) : Check
    }

    val currentVersionName: String get() = BuildConfig.VERSION_NAME

    /** Tag « v0.13.42 » → code de version 142 (même règle que dans app/build.gradle.kts). */
    fun codeFromTag(tag: String): Int? = tag.trimStart('v').split('.').getOrNull(2)?.toIntOrNull()?.let { 100 + it }

    /** Vérification automatique : au plus une fois toutes les 12 heures. */
    fun shouldAutoCheck(context: Context): Boolean {
        val last = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong(KEY_LAST_CHECK, 0L)
        return System.currentTimeMillis() - last > 12 * 60 * 60 * 1000L
    }

    fun isSkipped(context: Context, release: Release): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_SKIPPED, 0) == release.versionCode

    fun skip(context: Context, release: Release) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putInt(KEY_SKIPPED, release.versionCode).apply()
    }

    suspend fun check(context: Context): Check = withContext(Dispatchers.IO) {
        runCatching {
            val conn = (URL(RELEASES_URL).openConnection() as HttpURLConnection).apply {
                connectTimeout = 8_000
                readTimeout = 8_000
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "AL-MIZAN-Android")
            }
            try {
                when (conn.responseCode) {
                    200 -> Unit
                    404 -> return@runCatching Check.Failed("Les versions ne sont pas accessibles (dépôt privé ?)")
                    else -> return@runCatching Check.Failed("Serveur indisponible (${conn.responseCode})")
                }
                val json = JSONArray(conn.inputStream.bufferedReader().use { it.readText() })
                context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                    .putLong(KEY_LAST_CHECK, System.currentTimeMillis()).apply()
                var best: Release? = null
                for (i in 0 until json.length()) {
                    val r = json.getJSONObject(i)
                    if (r.optBoolean("draft")) continue
                    val tag = r.optString("tag_name")
                    val code = codeFromTag(tag) ?: continue
                    val assets = r.optJSONArray("assets") ?: continue
                    for (j in 0 until assets.length()) {
                        val a = assets.getJSONObject(j)
                        if (!a.optString("name").endsWith(".apk")) continue
                        if (best == null || code > best.versionCode) {
                            best = Release(
                                versionCode = code,
                                versionName = tag.trimStart('v'),
                                notes = r.optString("body").trim(),
                                apkUrl = a.optString("browser_download_url"),
                                sizeBytes = a.optLong("size")
                            )
                        }
                    }
                }
                val found = best
                if (found != null && found.versionCode > BuildConfig.VERSION_CODE) Check.Available(found) else Check.UpToDate
            } finally {
                conn.disconnect()
            }
        }.getOrElse { Check.Failed("Pas de connexion internet") }
    }

    /**
     * Ouvre le lien de l'APK dans le navigateur : il le télécharge, puis on touche le fichier pour l'installer.
     * L'app ne demande donc pas la permission d'installer des applications (permission sensible
     * qui déclenche les alertes de Play Protect).
     */
    fun openDownload(context: Context, release: Release) {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(release.apkUrl)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
