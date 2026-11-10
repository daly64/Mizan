package tn.mizan.app.claude

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import tn.mizan.app.ui.MonthState
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.max
import kotlin.math.roundToInt

/** Erreur avec un message lisible, prêt à afficher. */
class ClaudeException(message: String) : Exception(message)

/**
 * Claude directement dans Mizan, via l'API d'Anthropic (Messages API).
 * N'est utilisé que si l'utilisateur a enregistré sa propre clé dans Profil → Claude.
 */
object ClaudeApi {

    private const val ENDPOINT = "https://api.anthropic.com/v1/messages"
    private const val API_VERSION = "2023-06-01"
    private const val MAX_IMAGE_SIDE = 1568

    private const val REVIEW_SYSTEM =
        "Tu es l'assistant budgétaire de l'application Mizan, utilisée en Tunisie (dinar tunisien, DT). " +
            "Réponds en français, en texte simple, sans Markdown (pas de **, pas de #). " +
            "Commence par une phrase de résumé, puis des conseils sur des lignes commençant par « • ». " +
            "N'invente aucun chiffre."

    private const val TICKET_SYSTEM =
        "Tu convertis des tickets et factures en lignes CSV pour l'application Mizan. " +
            "Réponds uniquement avec le CSV demandé, sans commentaire ni bloc de code."

    /** Bilan du mois, lu directement dans Mizan. */
    suspend fun review(apiKey: String, model: String, state: MonthState): Result<String> =
        call(
            apiKey, model, REVIEW_SYSTEM,
            JSONArray().put(textBlock(ClaudeBridge.monthReport(state)))
        )

    /** Lit la photo d'un ticket et renvoie des lignes au format Mizan. */
    suspend fun readTicket(apiKey: String, model: String, photo: File): Result<String> {
        val image = withContext(Dispatchers.Default) { runCatching { encodeImage(photo) } }.getOrElse {
            return Result.failure(ClaudeException("Impossible de lire cette photo."))
        }
        val content = JSONArray()
            .put(
                JSONObject().put("type", "image").put(
                    "source",
                    JSONObject().put("type", "base64").put("media_type", "image/jpeg").put("data", image)
                )
            )
            .put(textBlock(ClaudeBridge.importPrompt()))
        return call(apiKey, model, TICKET_SYSTEM, content)
    }

    private fun textBlock(text: String) = JSONObject().put("type", "text").put("text", text)

    private suspend fun call(apiKey: String, model: String, system: String, content: JSONArray): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val body = JSONObject()
                    .put("model", model)
                    .put("max_tokens", 1500)
                    .put("system", system)
                    .put("messages", JSONArray().put(JSONObject().put("role", "user").put("content", content)))

                val conn = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 15_000
                    readTimeout = 90_000
                    doOutput = true
                    setRequestProperty("content-type", "application/json")
                    setRequestProperty("x-api-key", apiKey)
                    setRequestProperty("anthropic-version", API_VERSION)
                }
                try {
                    conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
                    val code = conn.responseCode
                    if (code !in 200..299) {
                        val detail = conn.errorStream?.bufferedReader()?.use { it.readText() }
                            ?.let { runCatching { JSONObject(it).getJSONObject("error").getString("message") }.getOrNull() }
                        throw ClaudeException(errorMessage(code, model, detail))
                    }
                    val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
                    val blocks = json.getJSONArray("content")
                    buildString {
                        for (i in 0 until blocks.length()) {
                            val block = blocks.getJSONObject(i)
                            if (block.optString("type") == "text") append(block.getString("text"))
                        }
                    }.replace("**", "").trim()
                } finally {
                    conn.disconnect()
                }
            }.recoverCatching { e ->
                throw if (e is ClaudeException) e
                else ClaudeException(
                    if (e is IOException) "Impossible de joindre Claude. Vérifie ta connexion internet."
                    else "Réponse inattendue de Claude."
                )
            }
        }

    private fun errorMessage(code: Int, model: String, detail: String?): String = when {
        code == 401 -> "Clé API refusée. Vérifie-la dans Profil → Claude."
        code == 403 -> "Cette clé n’a pas accès à ce service."
        code == 404 -> "Modèle « $model » introuvable. Change-le dans Profil → Claude."
        code == 429 -> "Trop de demandes ou limite atteinte. Réessaie dans un moment."
        code == 529 || code == 503 -> "Claude est très demandé en ce moment. Réessaie dans un instant."
        detail?.contains("credit", ignoreCase = true) == true ->
            "Crédit insuffisant sur ta console Anthropic. Ajoute du crédit puis réessaie."
        detail != null -> "Erreur de Claude : $detail"
        else -> "Erreur de Claude (code $code)."
    }

    /** Photo réduite (côté max 1568 px, JPEG) puis encodée en base64, comme le recommande Anthropic. */
    private fun encodeImage(file: File): String {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_IMAGE_SIDE) sample *= 2
        val decoded = BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample })
            ?: throw IOException("image illisible")
        val longest = max(decoded.width, decoded.height)
        val bitmap = if (longest > MAX_IMAGE_SIDE) {
            val k = MAX_IMAGE_SIDE.toFloat() / longest
            Bitmap.createScaledBitmap(decoded, (decoded.width * k).roundToInt(), (decoded.height * k).roundToInt(), true)
        } else decoded
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
        return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }
}
