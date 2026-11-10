package tn.mizan.app.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import tn.mizan.app.data.Category
import tn.mizan.app.ui.MonthState
import tn.mizan.app.util.Dates
import tn.mizan.app.util.Money
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate

/** État du bilan affiché sur l'écran Budgets. */
sealed interface ReviewState {
    data object Idle : ReviewState
    data object Loading : ReviewState
    data class Done(val text: String) : ReviewState
    data class Error(val message: String) : ReviewState
}

/**
 * Bilan du mois par l'IA.
 * Seuls les TOTAUX partent vers l'API (revenus, dépenses, budgets par catégorie) :
 * jamais les notes, ni la liste des transactions.
 */
object AiReview {

    private const val ENDPOINT = "https://api.openai.com/v1/chat/completions"

    private const val SYSTEM_PROMPT =
        "Tu es l'assistant budgétaire de l'application Mizan, utilisée en Tunisie. " +
            "La monnaie est le dinar tunisien (DT, 3 décimales). " +
            "À partir des totaux du mois, fais un bilan en français : une phrase de résumé, " +
            "puis 3 à 5 conseils courts et concrets, chacun sur une ligne commençant par « • ». " +
            "Signale les catégories qui dépassent ou approchent leur budget et le rythme de dépense " +
            "par rapport au jour du mois. Ton bienveillant, sans jugement. " +
            "N'invente aucun chiffre. Pas de Markdown, pas de titres."

    /** Résumé envoyé à l'IA (totaux uniquement). */
    fun summary(state: MonthState, today: LocalDate = LocalDate.now()): String = buildString {
        appendLine("Mois : ${Dates.monthTitle()}, jour ${today.dayOfMonth} sur ${today.lengthOfMonth()}.")
        appendLine("Revenus du mois : ${Money.format(state.income)} DT.")
        appendLine("Dépenses du mois : ${Money.format(state.spent)} DT, budget total ${Money.format(state.budgetTotal)} DT.")
        appendLine("Nombre de dépenses : ${state.expenses.count { !it.isIncome }}.")
        appendLine("Par catégorie (dépensé / budget) :")
        Category.entries.forEach { c ->
            appendLine("- ${c.label} : ${Money.format(state.spentBy(c))} / ${Money.format(state.budgets[c] ?: 0L)} DT")
        }
    }

    suspend fun request(apiKey: String, model: String, state: MonthState): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val body = JSONObject()
                    .put("model", model)
                    .put("temperature", 0.4)
                    .put(
                        "messages",
                        JSONArray()
                            .put(JSONObject().put("role", "system").put("content", SYSTEM_PROMPT))
                            .put(JSONObject().put("role", "user").put("content", summary(state)))
                    )

                val conn = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 15_000
                    readTimeout = 45_000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Authorization", "Bearer $apiKey")
                }
                try {
                    conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
                    val code = conn.responseCode
                    if (code !in 200..299) {
                        throw AiException(
                            when (code) {
                                401 -> "Clé API refusée. Vérifie-la dans Profil → Intelligence artificielle."
                                403 -> "Accès refusé à ce modèle avec cette clé."
                                404 -> "Modèle « $model » introuvable. Change-le dans Profil."
                                429 -> "Quota dépassé ou trop de demandes. Vérifie ton crédit OpenAI."
                                else -> "Erreur du service IA (code $code)."
                            }
                        )
                    }
                    val response = conn.inputStream.bufferedReader().use { it.readText() }
                    JSONObject(response)
                        .getJSONArray("choices").getJSONObject(0)
                        .getJSONObject("message").getString("content")
                        .replace("**", "")
                        .trim()
                } finally {
                    conn.disconnect()
                }
            }.recoverCatching { e ->
                throw if (e is AiException) e
                else AiException(
                    if (e is IOException) "Impossible de joindre le service IA. Vérifie ta connexion internet."
                    else "Réponse inattendue du service IA."
                )
            }
        }
}

/** Erreur avec un message lisible, prêt à afficher. */
class AiException(message: String) : Exception(message)
