package tn.mizan.app.agent

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.coroutineContext

/**
 * Le fichier du modèle (Gemma 4 E2B, ~2,6 Go) n'est pas dans l'APK et Mizan n'a pas accès à internet.
 * On le télécharge une fois avec le navigateur, puis on l'importe ici : il est copié dans le
 * dossier privé de l'app. Ensuite le fichier téléchargé peut être supprimé.
 */
object ModelStore {

    const val DOWNLOAD_URL =
        "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/gemma-4-E2B-it.litertlm"

    private const val PREFS = "mizan_assistant"
    private const val KEY_GPU = "gpu"

    fun modelFile(context: Context): File = File(File(context.filesDir, "modeles"), "assistant.litertlm")

    fun hasModel(context: Context): Boolean = modelFile(context).let { it.exists() && it.length() > 0 }

    fun sizeLabel(context: Context): String =
        String.format(java.util.Locale.FRENCH, "%.2f Go", modelFile(context).length() / 1_073_741_824.0)

    fun useGpu(context: Context): Boolean = prefs(context).getBoolean(KEY_GPU, false)

    fun setUseGpu(context: Context, value: Boolean) = prefs(context).edit().putBoolean(KEY_GPU, value).apply()

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Nom du fichier choisi, pour vérifier que c'est bien un .litertlm. */
    fun displayName(context: Context, uri: Uri): String? =
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }

    /**
     * Copie le modèle choisi dans le dossier de l'app. [onProgress] reçoit 0..1 (ou -1 si taille inconnue).
     * On écrit d'abord dans un fichier temporaire : un import interrompu ne laisse pas de modèle cassé.
     */
    suspend fun import(context: Context, uri: Uri, onProgress: (Float) -> Unit) = withContext(Dispatchers.IO) {
        val total = context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { c ->
            if (c.moveToFirst() && !c.isNull(0)) c.getLong(0) else -1L
        } ?: -1L
        val target = modelFile(context)
        target.parentFile?.mkdirs()
        val tmp = File(target.parentFile, "import.tmp")
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                tmp.outputStream().use { output ->
                    val buffer = ByteArray(1 shl 20)
                    var copied = 0L
                    var lastReport = 0L
                    while (true) {
                        coroutineContext.ensureActive()
                        val n = input.read(buffer)
                        if (n < 0) break
                        output.write(buffer, 0, n)
                        copied += n
                        if (copied - lastReport > 16L shl 20) {
                            lastReport = copied
                            onProgress(if (total > 0) copied.toFloat() / total else -1f)
                        }
                    }
                }
            } ?: error("Impossible d'ouvrir le fichier choisi.")
            if (target.exists()) target.delete()
            check(tmp.renameTo(target)) { "Impossible d'enregistrer le modèle." }
            onProgress(1f)
        } finally {
            tmp.delete()
        }
    }

    fun delete(context: Context) {
        modelFile(context).delete()
        File(context.cacheDir, "litertlm").deleteRecursively()
    }
}
