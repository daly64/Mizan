package tn.mizan.app.agent

import java.text.Normalizer
import java.time.Month
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/**
 * Mode rapide : les demandes simples sont comprises par des règles Kotlin, sans réveiller le modèle.
 * Réponse instantanée pour « 20 dinars taxi », « note : … », « 12*3.5 », « bilan d'octobre »,
 * « combien en transport ? ». Tout le reste (questions ouvertes, conseils) part au modèle.
 */
class QuickCommands(private val tools: MizanTools) {

    /**
     * null : la demande part au modèle. "" : c'est fait, la pastille « ✓ … » suffit.
     * Sinon : le texte à afficher comme réponse de l'assistant.
     */
    fun handle(input: String): String? {
        val raw = input.trim()
        val text = normalize(raw)
        if (text.isEmpty()) return null

        // Questions ouvertes et conseils : c'est le travail du modèle.
        if (ADVICE.any { it in text }) return null

        // Note : « note : acheter du lait », « noter appeler Sami »
        NOTE.find(raw)?.let { m ->
            val body = m.groupValues[1].trim()
            if (body.isNotEmpty()) return silent(tools.ajouterNote(body))
        }

        // Calcul pur : « 12*3.5 », « calcule (120+45)/3 », « combien fait 350*20/100 »
        val expr = raw.replace(CALC_PREFIX, "").trim().removeSuffix("?").removeSuffix("=").trim()
        if (CALC.matches(expr) && expr.any { it in "+-*/x×÷" } && expr.any { it.isDigit() }) {
            return tools.calculer(expr)
        }

        // Bilan : « bilan », « bilan d'octobre », « résumé du mois dernier »
        if (BILAN.any { it in text }) {
            return tools.bilanDuMois(monthIn(text)?.toString() ?: "")
        }

        // Total d'une catégorie : « combien j'ai dépensé en transport ? »
        if (text.startsWith("combien") && !text.any { it.isDigit() }) {
            val category = tools.categoryIn(text) ?: return null
            return tools.totalCategorie(category, monthIn(text))
        }

        // Dépense ou revenu : « 20 dinars taxi », « taxi 20 », « 35dt courses », « salaire 1500 »
        val numbers = AMOUNT.findAll(raw).toList()
        if (numbers.size != 1) return null // aucun ou plusieurs nombres : on laisse le modèle comprendre
        val amount = numbers[0].groupValues[1].replace(',', '.').toDoubleOrNull() ?: return null
        val words = raw.replace(AMOUNT, " ").replace(CURRENCY, " ").replace(Regex("\\s+"), " ").trim()
        if (words.isEmpty() || words.length > 40) return null // trop long : probablement une vraie question
        if (INCOME.any { it in text }) return silent(tools.ajouterRevenu(amount, words))
        val category = tools.categoryIn(normalize(words)) ?: return null // catégorie pas claire : le modèle demandera
        return silent(tools.ajouterDepense(amount, category.label, words))
    }

    /** Une action réussie s'affiche déjà en pastille ; seules les erreurs deviennent une réponse. */
    private fun silent(result: String) = if (result.startsWith("Erreur")) result else ""

    /** « octobre », « mois dernier », « ce mois » -> mois voulu (null = mois en cours). */
    private fun monthIn(text: String): YearMonth? {
        val now = YearMonth.now()
        if ("mois dernier" in text || "mois precedent" in text) return now.minusMonths(1)
        Month.entries.forEach { m ->
            val name = normalize(m.getDisplayName(TextStyle.FULL, Locale.FRENCH))
            if (Regex("\\b$name\\b").containsMatchIn(text)) {
                val candidate = YearMonth.of(now.year, m)
                return if (candidate.isAfter(now)) candidate.minusYears(1) else candidate
            }
        }
        return null
    }

    private companion object {
        val ADVICE = listOf("conseil", "econom", "optimis", "pourquoi", "comment ", "aide-moi", "aide moi", "analyse", "explique", "reduire")
        val BILAN = listOf("bilan", "resume du mois", "recap")
        val INCOME = listOf("salaire", "revenu", "recu ", "gagne", "paie ", "prime")
        val NOTE = Regex("^\\s*(?:note|noter|nota)\\s*[:\\-]?\\s+(.+)$", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        val CALC_PREFIX = Regex("^(?i)(calcule[rz]?|combien fait|ca fait|ça fait)\\s*:?")
        val CALC = Regex("^[0-9.,+\\-*/x×÷()\\s%]+$")
        val AMOUNT = Regex("(\\d+(?:[.,]\\d{1,3})?)")
        val CURRENCY = Regex("(?i)\\b(dinars?|dt|tnd|d)\\b")

        fun normalize(s: String) =
            Normalizer.normalize(s.lowercase().trim(), Normalizer.Form.NFD).replace(Regex("\\p{M}"), "")
    }
}
