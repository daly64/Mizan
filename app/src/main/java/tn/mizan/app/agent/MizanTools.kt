package tn.mizan.app.agent

import com.google.ai.edge.litertlm.Tool
import com.google.ai.edge.litertlm.ToolParam
import com.google.ai.edge.litertlm.ToolSet
import kotlinx.coroutines.runBlocking
import tn.mizan.app.data.Category
import tn.mizan.app.data.Expense
import tn.mizan.app.data.MizanDao
import tn.mizan.app.data.Note
import tn.mizan.app.data.PaymentMethod
import tn.mizan.app.util.Money
import java.text.Normalizer
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToLong

/**
 * Les outils de l'assistant. Tout est calculé ici, en Kotlin, sur la base Room :
 * le modèle choisit l'outil et rédige la réponse, il ne fait jamais les calculs.
 * Peu de paramètres par outil : les petits modèles appellent mieux les outils simples.
 * Les appels arrivent sur le fil du moteur (jamais le fil de l'écran) : runBlocking est sans risque.
 */
class MizanTools(
    private val dao: MizanDao,
    /** Prévient l'écran qu'un outil a tourné (pour afficher « ✓ Dépense ajoutée », etc.). */
    private val onAction: (String) -> Unit
) : ToolSet {

    private val zone get() = ZoneId.systemDefault()

    @Tool(description = "Enregistre une dépense dans Mizan. Catégories : courses, transport, loisirs, factures.")
    fun ajouterDepense(
        @ToolParam(description = "Montant en dinars tunisiens, ex. 20 ou 12.5") montant: Double,
        @ToolParam(description = "courses, transport, loisirs ou factures") categorie: String,
        @ToolParam(description = "Courte description, ex. taxi") note: String = ""
    ): String {
        val millimes = toMillimes(montant) ?: return "Erreur : le montant doit être positif."
        val category = matchCategory(categorie, note)
            ?: return "Erreur : catégorie inconnue. Choisis parmi : courses, transport, loisirs, factures."
        val expense = Expense(
            amountMillimes = millimes,
            category = category,
            note = note.trim(),
            dateMillis = System.currentTimeMillis(),
            payment = PaymentMethod.ESPECES
        )
        runBlocking { dao.insert(expense) }
        val text = "Dépense de ${Money.format(millimes)} DT ajoutée (${category.label}${if (note.isNotBlank()) ", ${note.trim()}" else ""})."
        onAction(text)
        return text
    }

    @Tool(description = "Enregistre un revenu (salaire, vente, cadeau…) dans Mizan.")
    fun ajouterRevenu(
        @ToolParam(description = "Montant en dinars tunisiens") montant: Double,
        @ToolParam(description = "Courte description, ex. salaire") note: String = ""
    ): String {
        val millimes = toMillimes(montant) ?: return "Erreur : le montant doit être positif."
        runBlocking {
            dao.insert(
                Expense(
                    amountMillimes = millimes,
                    category = Category.FACTURES, // les revenus n'ont pas de catégorie affichée
                    note = note.trim(),
                    dateMillis = System.currentTimeMillis(),
                    payment = PaymentMethod.ESPECES,
                    isIncome = true
                )
            )
        }
        val text = "Revenu de ${Money.format(millimes)} DT ajouté${if (note.isNotBlank()) " (${note.trim()})" else ""}."
        onAction(text)
        return text
    }

    @Tool(description = "Enregistre une note libre (idée, rappel, liste…).")
    fun ajouterNote(
        @ToolParam(description = "Le texte de la note") texte: String
    ): String {
        if (texte.isBlank()) return "Erreur : la note est vide."
        runBlocking { dao.insertNote(Note(text = texte.trim(), dateMillis = System.currentTimeMillis())) }
        onAction("Note enregistrée.")
        return "Note enregistrée."
    }

    @Tool(description = "Cherche dans les notes enregistrées. Mot vide = les dernières notes.")
    fun chercherNotes(
        @ToolParam(description = "Mot à chercher, ou vide") recherche: String = ""
    ): String {
        val notes = runBlocking { dao.searchNotes(recherche.trim(), 10) }
        if (notes.isEmpty()) return "Aucune note trouvée."
        val fmt = DateTimeFormatter.ofPattern("d MMM", Locale.FRENCH)
        return notes.joinToString("\n") { n ->
            val d = java.time.Instant.ofEpochMilli(n.dateMillis).atZone(zone).toLocalDate().format(fmt)
            "- ($d) ${n.text}"
        }
    }

    @Tool(description = "Calcule une expression exacte avec + - * / et parenthèses. Ex. 350*20/100.")
    fun calculer(
        @ToolParam(description = "L'expression, ex. (120+45.5)*3") expression: String
    ): String = runCatching {
        "$expression = ${Calculator.format(Calculator.evaluate(expression))}"
    }.getOrElse { "Erreur de calcul : ${it.message}" }

    @Tool(description = "Bilan chiffré d'un mois : dépenses, revenus, solde, budgets, comparaison avec le mois d'avant. À utiliser pour tout bilan ou conseil d'optimisation.")
    fun bilanDuMois(
        @ToolParam(description = "Mois au format AAAA-MM, ou vide pour le mois en cours") mois: String = ""
    ): String {
        val month = runCatching { YearMonth.parse(mois.trim()) }.getOrElse { YearMonth.now() }
        return runBlocking { buildBilan(month) }
    }

    // --- Pour le mode rapide (QuickCommands) ---

    /** Catégorie citée dans un texte déjà normalisé, ou null. */
    fun categoryIn(text: String): Category? = matchCategory("", text)

    fun totalCategorie(category: Category, month: YearMonth?): String = runBlocking {
        val m = month ?: YearMonth.now()
        val list = load(m).filter { !it.isIncome && it.category == category }
        val name = m.month.getDisplayName(TextStyle.FULL, Locale.FRENCH) + " " + m.year
        val budget = dao.allBudgets().firstOrNull { it.category == category }?.limitMillimes ?: 0L
        val total = list.sumOf { it.amountMillimes }
        buildString {
            append("${category.label} en $name : ${Money.format(total)} DT sur ${list.size} dépense${if (list.size > 1) "s" else ""}.")
            if (budget > 0) append(" Budget : ${Money.format(budget)} DT (${percent(total, budget)}).")
        }
    }

    // --- Calculs du bilan ---

    private suspend fun buildBilan(month: YearMonth): String {
        val current = load(month)
        val previous = load(month.minusMonths(1))
        val budgets = dao.allBudgets().associate { it.category to it.limitMillimes }

        val spent = current.filter { !it.isIncome }
        val total = spent.sumOf { it.amountMillimes }
        val income = current.filter { it.isIncome }.sumOf { it.amountMillimes }
        val prevTotal = previous.filter { !it.isIncome }.sumOf { it.amountMillimes }
        val budgetTotal = budgets.values.sum()
        val name = month.month.getDisplayName(TextStyle.FULL, Locale.FRENCH) + " " + month.year

        val sb = StringBuilder()
        sb.appendLine("Bilan de $name (montants en DT) :")
        sb.appendLine("- Dépenses : ${Money.format(total)} sur ${spent.size} opérations")
        sb.appendLine("- Revenus : ${Money.format(income)}")
        sb.appendLine("- Solde : ${sign(income - total)}${Money.format(income - total)}")
        if (budgetTotal > 0) sb.appendLine("- Budget total : ${Money.format(budgetTotal)} (${percent(total, budgetTotal)} utilisé)")
        if (prevTotal > 0) sb.appendLine("- Mois précédent : ${Money.format(prevTotal)} (${evolution(total, prevTotal)})")

        val today = LocalDate.now()
        if (month == YearMonth.from(today) && total > 0) {
            val projection = (total.toDouble() / today.dayOfMonth * month.lengthOfMonth()).roundToLong()
            sb.appendLine("- Projection fin de mois au rythme actuel : ${Money.format(projection)}")
        }

        sb.appendLine("Par catégorie :")
        Category.entries.forEach { c ->
            val s = spent.filter { it.category == c }.sumOf { it.amountMillimes }
            val p = previous.filter { !it.isIncome && it.category == c }.sumOf { it.amountMillimes }
            val b = budgets[c] ?: 0L
            val parts = mutableListOf("${c.label} : ${Money.format(s)}")
            if (b > 0) parts += "budget ${Money.format(b)} (${percent(s, b)})" + if (s > b) " DÉPASSÉ" else ""
            if (p > 0) parts += "mois précédent ${Money.format(p)} (${evolution(s, p)})"
            sb.appendLine("- " + parts.joinToString(", "))
        }

        val top = spent.sortedByDescending { it.amountMillimes }.take(3)
        if (top.isNotEmpty()) {
            sb.appendLine("Plus grosses dépenses :")
            top.forEach { sb.appendLine("- ${Money.format(it.amountMillimes)} ${it.category.label}${if (it.note.isNotBlank()) " (${it.note})" else ""}") }
        }
        return sb.toString().trim()
    }

    private suspend fun load(month: YearMonth): List<Expense> {
        val start = month.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val end = month.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
        return dao.expensesBetweenNow(start, end)
    }

    private fun percent(part: Long, whole: Long) = "${(part * 100.0 / whole).roundToLong()} %"

    private fun evolution(now: Long, before: Long): String {
        val pct = ((now - before) * 100.0 / before).roundToLong()
        return if (pct >= 0) "+$pct %" else "$pct %"
    }

    private fun sign(v: Long) = if (v < 0) "-" else ""

    private fun toMillimes(dinars: Double): Long? =
        (dinars * 1000).roundToLong().takeIf { it > 0 }

    /** « Transport », « taxi », « électricité »… -> catégorie Mizan. */
    private fun matchCategory(name: String, note: String): Category? {
        val key = normalize(name)
        Category.entries.firstOrNull { normalize(it.label) == key || it.name.lowercase() == key }?.let { return it }
        val text = "$key ${normalize(note)}"
        return KEYWORDS.entries.firstOrNull { (_, words) -> words.any { it in text } }?.key
    }

    private fun normalize(s: String) =
        Normalizer.normalize(s.lowercase().trim(), Normalizer.Form.NFD).replace(Regex("\\p{M}"), "")

    private companion object {
        val KEYWORDS = mapOf(
            Category.TRANSPORT to listOf("transport", "taxi", "louage", "bus", "metro", "train", "essence", "carburant", "parking", "voiture"),
            Category.FACTURES to listOf("facture", "steg", "sonede", "electricite", "eau", "internet", "telephone", "loyer", "abonnement", "recharge"),
            Category.LOISIRS to listOf("loisir", "cafe", "resto", "restaurant", "cinema", "sortie", "sport", "jeu", "voyage", "cadeau"),
            Category.COURSES to listOf("course", "alimentation", "epicerie", "marche", "supermarche", "pain", "legume", "fruit", "viande", "aziza", "monoprix", "carrefour")
        )
    }
}
