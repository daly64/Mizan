package tn.mizan.app.data

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.text.Normalizer
import java.time.LocalDate
import kotlin.coroutines.resume

/**
 * Scan de ticket de caisse, 100 % sur le téléphone :
 * ML Kit lit le texte de la photo (modèle embarqué dans l'app, aucune connexion),
 * puis [TicketParser] en tire le total, la date, le magasin et une catégorie probable.
 * Rien n'est envoyé ni conservé : la photo est effacée après lecture.
 */
object Tickets {

    /** Lit tout le texte de la photo, ligne par ligne, de haut en bas. Null si la lecture échoue. */
    suspend fun readText(context: Context, uri: Uri): List<String>? = withContext(Dispatchers.Default) {
        val image = runCatching { InputImage.fromFilePath(context, uri) }.getOrNull() ?: return@withContext null
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        try {
            suspendCancellableCoroutine<List<String>?> { cont ->
                recognizer.process(image)
                    .addOnSuccessListener { result ->
                        // Toutes les lignes, triées de haut en bas puis de gauche à droite.
                        val lines = result.textBlocks.flatMap { it.lines }
                            .sortedWith(compareBy({ it.boundingBox?.centerY() ?: 0 }, { it.boundingBox?.left ?: 0 }))
                        // On regroupe les morceaux qui sont sur la même ligne du ticket (ex. « TOTAL » … « 64,300 »).
                        val rows = mutableListOf<Pair<Int, StringBuilder>>()
                        lines.forEach { line ->
                            val box = line.boundingBox
                            val y = box?.centerY() ?: 0
                            val tolerance = ((box?.height() ?: 20) * 0.6f).toInt()
                            val row = rows.lastOrNull()
                            if (row != null && kotlin.math.abs(row.first - y) <= tolerance) row.second.append("  ").append(line.text)
                            else rows += y to StringBuilder(line.text)
                        }
                        cont.resume(rows.map { it.second.toString() })
                    }
                    .addOnFailureListener { cont.resume(null) }
            }
        } finally {
            recognizer.close()
        }
    }

    // --- Catégories apprises : quand tu corriges la catégorie d'un magasin, l'app s'en souvient. ---

    private const val PREFS = "mizan_tickets"

    fun learnedCategory(context: Context, merchant: String): Category? {
        val saved = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(TicketParser.key(merchant), null)
        return Category.entries.firstOrNull { it.name == saved }
    }

    fun learn(context: Context, merchant: String, category: Category) {
        if (merchant.isBlank()) return
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(TicketParser.key(merchant), category.name).apply()
    }
}

/** Ce que le scan a trouvé. Chaque champ peut manquer : l'utilisateur complète et confirme. */
data class TicketResult(
    val amountMillimes: Long?,
    val date: LocalDate?,
    val merchant: String?,
    val category: Category?
) {
    val isEmpty get() = amountMillimes == null && date == null && merchant == null
}

/** Analyse du texte d'un ticket tunisien (français, montants en dinars à 3 décimales). Sans Android : testable. */
object TicketParser {

    /** 64,300 · 64.300 · 1 250,500 · 1.250,500 · 1,250.500 (3 décimales = millimes). */
    private val amount = Regex("(?<![\\d.,])(\\d{1,3}(?:[ .,\\u00A0]\\d{3})+|\\d+)[.,](\\d{3})(?![\\d])")

    /** 11/11/2026 · 11-11-26 · 11.11.2026 · 2026-11-11 */
    private val dateDmy = Regex("(?<!\\d)(\\d{1,2})[/.\\-](\\d{1,2})[/.\\-](\\d{4}|\\d{2})(?!\\d)")
    private val dateYmd = Regex("(?<!\\d)(20\\d{2})[/.\\-](\\d{1,2})[/.\\-](\\d{1,2})(?!\\d)")

    private val totalWords = listOf("NET A PAYER", "NET APAYER", "TOTAL TTC", "TOTAL A PAYER", "A PAYER", "MONTANT TTC", "TOTAL NET", "TOTAL", "MONTANT", "NET")
    private val notTotalWords = listOf("TVA", "REMISE", "RENDU", "MONNAIE", "ESPECE", "RECU", "CARTE", "TIMBRE", "HT", "SOUS", "ARTICLE", "QTE", "POINTS", "FIDELITE")

    private val skipMerchantWords = listOf(
        "BIENVENUE", "TICKET", "TEL", "FAX", "MF", "MATRICULE", "M.F", "ADRESSE", "RUE", "AVENUE", "AV ", "ROUTE",
        "CAISSE", "CAISSIER", "FACTURE", "DATE", "HEURE", "MERCI", "CODE", "TVA", "RC "
    )

    /** Mots du magasin → catégorie. Le premier qui correspond gagne. */
    private val rules: List<Pair<Category, List<String>>> = listOf(
        Category.FACTURES to listOf("STEG", "SONEDE", "TUNISIE TELECOM", "TUNISIETELECOM", "OOREDOO", "ORANGE", "TOPNET",
            "GLOBALNET", "HEXABYTE", "LOYER", "ASSURANCE", "FACTURE"),
        Category.TRANSPORT to listOf("TAXI", "AGIL", "SHELL", "TOTALENERGIES", "TOTAL ENERGIES", "OLA ENERGY", "STAR OIL",
            "STATION", "CARBURANT", "ESSENCE", "GASOIL", "SANS PLOMB", "PEAGE", "AUTOROUTE", "TRANSTU", "SNCFT", "LOUAGE", "PARKING", "METRO"),
        Category.COURSES to listOf("CARREFOUR", "MONOPRIX", "AZIZA", "MAGASIN GENERAL", "GEANT", "PROMOGROS", "BOULANGERIE",
            "PATISSERIE", "EPICERIE", "SUPERETTE", "SUPERMARCHE", "HYPER", "MARCHE", "BOUCHERIE", "PRIMEUR", "PHARMACIE", "PARAPHARMACIE"),
        Category.LOISIRS to listOf("CAFE", "COFFEE", "RESTAURANT", "RESTO", "PIZZ", "CINEMA", "SALON DE THE", "FAST FOOD",
            "SNACK", "LIBRAIRIE", "GLACIER", "CREPERIE", "BAR ", "LOUNGE", "SPORT")
    )

    /** Majuscules sans accents, pour comparer « Épicerie » et « EPICERIE ». */
    fun normalize(text: String): String =
        Normalizer.normalize(text, Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "").uppercase()

    fun key(merchant: String): String = normalize(merchant).replace(Regex("[^A-Z0-9]+"), " ").trim()

    fun parse(lines: List<String>, today: LocalDate = LocalDate.now(), learned: (String) -> Category? = { null }): TicketResult {
        val clean = lines.map { it.trim() }.filter { it.isNotEmpty() }
        val merchant = findMerchant(clean)
        val category = merchant?.let(learned) ?: guessCategory(clean.take(6) + listOfNotNull(merchant))
        return TicketResult(findTotal(clean), findDate(clean, today), merchant, category)
    }

    fun amounts(line: String): List<Long> = amount.findAll(line).mapNotNull { m ->
        val dinars = m.groupValues[1].replace(Regex("[ .,\\u00A0]"), "").toLongOrNull() ?: return@mapNotNull null
        val millimes = m.groupValues[2].toLong()
        (dinars * 1000 + millimes).takeIf { it in 1..100_000_000L } // jusqu'à 100 000 dinars
    }.toList()

    fun findTotal(lines: List<String>): Long? {
        val upper = lines.map(::normalize)
        // 1. Une ligne « TOTAL / NET A PAYER… » : le montant sur la même ligne, sinon sur les 2 lignes suivantes.
        for (word in totalWords) {
            for ((i, line) in upper.withIndex()) {
                if (!Regex("\\b${Regex.escape(word)}\\b").containsMatchIn(line)) continue
                if (word == "TOTAL" && notTotalWords.any { line.contains(it) }) continue
                (listOf(line) + upper.drop(i + 1).take(2)).forEach { candidate ->
                    amounts(candidate).maxOrNull()?.let { return it }
                }
            }
        }
        // 2. Sinon : le plus grand montant hors lignes de paiement (espèces données, monnaie rendue…).
        return upper.filterNot { l -> notTotalWords.any { l.contains(it) } }
            .flatMap(::amounts)
            .maxOrNull()
    }

    fun findDate(lines: List<String>, today: LocalDate): LocalDate? {
        val text = lines.joinToString("\n")
        val candidates = dateYmd.findAll(text).mapNotNull { m ->
            runCatching { LocalDate.of(m.groupValues[1].toInt(), m.groupValues[2].toInt(), m.groupValues[3].toInt()) }.getOrNull()
        } + dateDmy.findAll(text).mapNotNull { m ->
            val y = m.groupValues[3].toInt().let { if (it < 100) 2000 + it else it }
            runCatching { LocalDate.of(y, m.groupValues[2].toInt(), m.groupValues[1].toInt()) }.getOrNull()
        }
        // Une date de ticket plausible : pas dans le futur, et dans les deux dernières années.
        return candidates.firstOrNull { !it.isAfter(today) && it.isAfter(today.minusYears(2)) }
    }

    fun findMerchant(lines: List<String>): String? =
        lines.take(5).firstOrNull { line ->
            val n = normalize(line)
            val letters = n.count { it in 'A'..'Z' }
            letters >= 3 && letters >= n.length / 2 &&
                skipMerchantWords.none { n.startsWith(it) || n.contains(" $it") } &&
                amount.find(line) == null && dateDmy.find(line) == null
        }?.let { raw ->
            raw.replace(Regex("\\s+"), " ").trim().take(40)
                .lowercase().split(' ').joinToString(" ") { w -> w.replaceFirstChar { it.uppercase() } }
        }

    fun guessCategory(lines: List<String>): Category? {
        val text = " " + lines.joinToString(" ") { normalize(it) } + " "
        return rules.firstOrNull { (_, words) -> words.any { w -> text.contains(w) } }?.first
    }
}
