package tn.mizan.app.util

import tn.mizan.app.data.Category
import tn.mizan.app.data.PaymentMethod
import java.math.RoundingMode
import java.text.Normalizer
import java.time.LocalDate

/** Ce que la saisie rapide a compris d'une phrase comme « 20 taxi ». */
data class QuickEntry(
    val amountMillimes: Long?,
    val category: Category?,
    val isIncome: Boolean,
    val date: LocalDate,
    val note: String,
    val payment: PaymentMethod
)

/**
 * Analyse hors ligne, par règles simples (pas d'IA) :
 * montant, catégorie par mots-clés, « hier », revenu, mode de paiement ; le reste devient la note.
 */
object QuickParser {

    private val CATEGORY_KEYWORDS: Map<Category, Set<String>> = mapOf(
        Category.TRANSPORT to setOf(
            "taxi", "louage", "bus", "metro", "train", "tgm", "essence", "carburant", "gasoil",
            "parking", "peage", "uber", "bolt", "transport", "voiture", "garage", "vidange", "moto"
        ),
        Category.COURSES to setOf(
            "course", "courses", "epicerie", "marche", "supermarche", "monoprix", "carrefour",
            "aziza", "geant", "magasin", "pain", "boulangerie", "lait", "legume", "legumes",
            "fruit", "fruits", "viande", "boucherie", "poisson", "hanout", "attar", "provisions"
        ),
        Category.LOISIRS to setOf(
            "cinema", "cafe", "resto", "restaurant", "sortie", "loisir", "loisirs", "jeu", "jeux",
            "sport", "concert", "film", "netflix", "voyage", "plage", "livre", "cadeau", "pizza",
            "sandwich", "mlawi", "chapati", "glace", "patisserie", "the"
        ),
        Category.FACTURES to setOf(
            "facture", "factures", "steg", "sonede", "electricite", "eau", "gaz", "internet",
            "wifi", "telephone", "recharge", "ooredoo", "orange", "topnet", "loyer",
            "abonnement", "assurance"
        )
    )

    private val INCOME_WORDS = setOf(
        "salaire", "recu", "revenu", "revenus", "prime", "gagne", "vente", "vendu",
        "rembourse", "remboursement", "virement", "paie"
    )
    private val DINAR_UNITS = setOf("dinar", "dinars", "dt", "d", "tnd")
    private val MILLIME_UNITS = setOf("millime", "millimes", "mil", "mill", "m")
    private val FILLERS = setOf(
        "de", "du", "des", "pour", "le", "la", "les", "l", "au", "aux", "en", "un", "une",
        "a", "j", "ai", "paye", "payee", "depense", "et", "avec", "par"
    )
    private val NUMBER = Regex("^(\\d+(?:[.,]\\d+)?)([a-z]*)$")

    fun parse(text: String, today: LocalDate = LocalDate.now()): QuickEntry {
        val words = text.replace('’', ' ').replace('\'', ' ').split(Regex("\\s+")).filter { it.isNotBlank() }
        val norm = words.map { normalize(it) }
        val used = BooleanArray(words.size)

        // 1. Montant : « 20 », « 20.500 », « 20,5 », « 20dt », « 20 dinars 500 », « 500 millimes »
        var amount: Long? = null
        for (i in words.indices) {
            val m = NUMBER.find(norm[i]) ?: continue
            val glued = m.groupValues[2]
            if (glued.isNotEmpty() && glued !in DINAR_UNITS && glued !in MILLIME_UNITS) continue
            val value = m.groupValues[1].replace(',', '.').toBigDecimalOrNull() ?: continue
            used[i] = true
            var unit = glued
            if (unit.isEmpty() && i + 1 < words.size && (norm[i + 1] in DINAR_UNITS || norm[i + 1] in MILLIME_UNITS)) {
                unit = norm[i + 1]
                used[i + 1] = true
            }
            amount = if (unit in MILLIME_UNITS) {
                value.setScale(0, RoundingMode.HALF_UP).toLong()
            } else {
                var millimes = value.movePointRight(3).setScale(0, RoundingMode.HALF_UP).toLong()
                // « 20 dinars 500 » : les millimes dictés après l'unité
                val next = if (unit.isNotEmpty() && glued.isEmpty()) i + 2 else i + 1
                if (unit in DINAR_UNITS && value.scale() <= 0 && next < words.size && !used[next]) {
                    norm[next].toIntOrNull()?.takeIf { it in 1..999 }?.let { extra ->
                        millimes += extra
                        used[next] = true
                        if (next + 1 < words.size && norm[next + 1] in MILLIME_UNITS) used[next + 1] = true
                    }
                }
                millimes
            }
            break
        }

        // 2. Date
        var date = today
        for (i in words.indices) {
            when (norm[i]) {
                "hier" -> {
                    if (i > 0 && norm[i - 1] == "avant") { date = today.minusDays(2); used[i - 1] = true }
                    else if (date == today) date = today.minusDays(1)
                    used[i] = true
                }
                "avant-hier", "avanthier" -> { date = today.minusDays(2); used[i] = true }
                "aujourd", "aujourdhui", "hui" -> used[i] = true
            }
        }

        // 3. Paiement
        var payment = PaymentMethod.ESPECES
        for (i in words.indices) {
            when (norm[i]) {
                "carte", "cb", "visa" -> { payment = PaymentMethod.CARTE; used[i] = true }
                "especes", "espece", "cash", "liquide" -> { payment = PaymentMethod.ESPECES; used[i] = true }
            }
        }

        // 4. Revenu ou dépense, puis catégorie (le mot reste dans la note)
        val isIncome = norm.indices.any { !used[it] && norm[it] in INCOME_WORDS }
        val category = if (isIncome) null else norm.indices.firstNotNullOfOrNull { i ->
            if (used[i]) null else categoryOf(norm[i])
        }

        // 5. Note : les mots restants, sans les petits mots au début et à la fin
        val rest = words.indices.filter { !used[it] }.toMutableList()
        while (rest.isNotEmpty() && norm[rest.first()] in FILLERS) rest.removeAt(0)
        while (rest.isNotEmpty() && norm[rest.last()] in FILLERS) rest.removeAt(rest.lastIndex)
        val note = rest.joinToString(" ") { words[it] }.replaceFirstChar { it.uppercase() }

        return QuickEntry(amount, category, isIncome, date, note, payment)
    }

    private fun categoryOf(word: String): Category? {
        val singular = word.removeSuffix("s")
        return CATEGORY_KEYWORDS.entries.firstOrNull { (_, keys) -> word in keys || singular in keys }?.key
    }

    private fun normalize(word: String): String =
        Normalizer.normalize(word.lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .trim('.', ',', '!', '?', ';', ':', '"', '(', ')')
}
