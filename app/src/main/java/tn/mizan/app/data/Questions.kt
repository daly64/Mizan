package tn.mizan.app.data

import tn.mizan.app.util.Dates
import tn.mizan.app.util.Money
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Questions à la mini IA, sur le mois affiché dans le Bilan.
 * Elle reconnaît l'intention de la question par des mots-clés (« par jour », « mois dernier », « plus grosse »…)
 * ou cherche un mot dans tes notes (« taxi », « kahwa ») pour en faire le total. Tout est calculé sur le téléphone.
 */
object Questions {

    /** Questions proposées en un clic. */
    val suggestions = listOf(
        "Combien je peux dépenser par jour ?",
        "Où je dépense le plus ?",
        "Comparé au mois dernier ?",
        "Ma plus grosse dépense ?",
        "Fin du mois à ce rythme ?",
        "Combien j’économise ?"
    )

    private fun Expense.day(): LocalDate = Dates.toLocalDate(dateMillis)
    private fun List<Expense>.spent() = filter { !it.isIncome }.sumOf { it.amountMillimes }
    private fun dt(millimes: Long) = Money.format(millimes) + " DT"
    private fun norm(text: String) = TicketParser.normalize(text).lowercase()
    private val shortDate = DateTimeFormatter.ofPattern("d MMM", Locale.FRENCH)

    private val ignored = setOf(
        "combien", "quoi", "quel", "quelle", "quels", "est", "ce", "que", "qui", "pour", "en", "le", "la", "les", "de",
        "du", "des", "j", "je", "ai", "mes", "mon", "ma", "depense", "depenses", "depenser", "depensé", "mois", "ce",
        "total", "dinars", "dinar", "dt", "a", "au", "un", "une", "dans", "sur", "fait", "c", "l", "d", "il", "y"
    )

    fun repondre(
        question: String,
        month: YearMonth,
        all: List<Expense>,
        budgets: Map<Category, Long>,
        today: LocalDate = LocalDate.now()
    ): String {
        val q = norm(question)
        val inMonth = all.filter { YearMonth.from(it.day()) == month }
        val expenses = inMonth.filter { !it.isIncome }
        val spent = expenses.sumOf { it.amountMillimes }
        val income = inMonth.filter { it.isIncome }.sumOf { it.amountMillimes }
        val monthName = month.month.getDisplayName(TextStyle.FULL, Locale.FRENCH)
        val isCurrent = month == YearMonth.from(today)
        val length = month.lengthOfMonth()
        val elapsed = if (isCurrent) today.dayOfMonth else length
        val budget = budgets.values.sum()

        fun has(vararg words: String) = words.any { q.contains(it) }

        return when {
            has("par jour", "peux depenser", "reste", "tenir") -> {
                if (!isCurrent) "Ce mois est terminé : tu as dépensé en moyenne ${dt(spent / length)} par jour."
                else if (budget <= 0) "Tu n’as pas de budget défini. Ta moyenne actuelle : ${dt(spent / elapsed)} par jour."
                else {
                    val left = budget - spent
                    val daysLeft = (length - elapsed + 1).coerceAtLeast(1)
                    if (left <= 0) "Ton budget de ${dt(budget)} est déjà dépassé de ${dt(-left)}. Chaque dinar compte d’ici la fin du mois !"
                    else "Il te reste ${dt(left)} pour $daysLeft jours, soit au plus ${dt(left / daysLeft)} par jour. " +
                        "Jusqu’ici, ta moyenne est de ${dt(spent / elapsed)} par jour."
                }
            }

            has("plus grosse", "plus cher", "plus chere", "maximum", "max") -> {
                val top = expenses.maxByOrNull { it.amountMillimes }
                    ?: return "Aucune dépense en $monthName."
                "Ta plus grosse dépense en $monthName : ${top.note.ifBlank { top.category.label }}, " +
                    "${dt(top.amountMillimes)} le ${top.day().format(shortDate)} (${top.category.label})."
            }

            has("mois dernier", "mois precedent", "compar", "avant") -> {
                val prev = month.minusMonths(1)
                val prevSpent = all.filter { YearMonth.from(it.day()) == prev }.spent()
                val prevName = prev.month.getDisplayName(TextStyle.FULL, Locale.FRENCH)
                if (prevSpent == 0L) "Je n’ai aucune dépense en $prevName pour comparer."
                else {
                    // Mois en cours : on compare à la même date du mois précédent, pour être juste.
                    val prevSameDays = if (isCurrent) all.filter {
                        YearMonth.from(it.day()) == prev && !it.isIncome && it.day().dayOfMonth <= elapsed
                    }.sumOf { it.amountMillimes } else prevSpent
                    val diff = spent - prevSameDays
                    val pct = if (prevSameDays > 0) (diff * 100f / prevSameDays).roundToInt() else 0
                    val when_ = if (isCurrent) "au $elapsed $prevName" else "en $prevName"
                    val sens = if (diff >= 0) "de plus" else "de moins"
                    "${dt(spent)} en $monthName contre ${dt(prevSameDays)} $when_ : ${dt(abs(diff))} $sens (${if (pct >= 0) "+" else ""}$pct %)."
                }
            }

            has("ou je", "plus depens", "categorie", "repartition", "le plus") -> {
                if (spent == 0L) "Aucune dépense en $monthName."
                else Category.entries
                    .map { c -> c to expenses.filter { it.category == c }.sumOf { it.amountMillimes } }
                    .filter { it.second > 0 }
                    .sortedByDescending { it.second }
                    .joinToString(" · ", prefix = "En $monthName : ") { (c, v) -> "${c.label} ${dt(v)} (${(v * 100f / spent).roundToInt()} %)" }
            }

            has("fin du mois", "rythme", "prevision", "projection", "finir") -> {
                if (!isCurrent) "Ce mois est terminé : total ${dt(spent)}."
                else if (elapsed < 3) "C’est trop tôt dans le mois pour prévoir : reviens dans quelques jours."
                else {
                    val projection = spent * length / elapsed
                    val vsBudget = when {
                        budget <= 0 -> ""
                        projection > budget -> ", soit ${dt(projection - budget)} au-dessus de ton budget"
                        else -> ", sous ton budget de ${dt(budget)}"
                    }
                    "À ce rythme, tu finiras $monthName vers ${dt(projection)}$vsBudget."
                }
            }

            has("econom", "epargn", "mis de cote", "solde") -> {
                if (income == 0L) "Aucun revenu enregistré en $monthName : ajoute-le (bouton + → Revenu) pour que je calcule ton épargne."
                else {
                    val saved = income - spent
                    val rate = (saved * 100f / income).roundToInt()
                    if (saved >= 0) "Tu as gardé ${dt(saved)} sur ${dt(income)} de revenus, soit $rate %."
                    else "Tu as dépensé ${dt(-saved)} de plus que tes revenus en $monthName."
                }
            }

            else -> searchNotes(q, monthName, expenses)
        }
    }

    /** « combien pour le taxi ? » → total des dépenses dont la note ou la catégorie contient « taxi ». */
    private fun searchNotes(q: String, monthName: String, expenses: List<Expense>): String {
        val words = q.split(Regex("[^a-z]+")).filter { it.length >= 3 && it !in ignored }
        if (words.isEmpty()) return notUnderstood()
        // Une catégorie nommée dans la question ?
        Category.entries.firstOrNull { c -> words.any { w -> norm(c.label).startsWith(w) || w.startsWith(norm(c.label)) } }?.let { c ->
            val list = expenses.filter { it.category == c }
            return if (list.isEmpty()) "Aucune dépense en ${c.label} en $monthName."
            else "${c.label} en $monthName : ${dt(list.sumOf { it.amountMillimes })} en ${list.size} dépense${if (list.size > 1) "s" else ""}."
        }
        // Sinon, un mot présent dans tes notes (« taxi », « kahwa »…)
        for (w in words) {
            val list = expenses.filter { norm(it.note).contains(w) }
            if (list.isNotEmpty()) {
                val total = list.sumOf { it.amountMillimes }
                return "« $w » en $monthName : ${list.size} dépense${if (list.size > 1) "s" else ""} pour ${dt(total)}" +
                    (if (list.size > 1) ", en moyenne ${dt(total / list.size)}." else ".")
            }
        }
        return "Je ne trouve aucune dépense « ${words.first()} » en $monthName. " + notUnderstood()
    }

    private fun notUnderstood() =
        "Essaie : « par jour », « mois dernier », « plus grosse dépense », ou un mot de tes notes comme « taxi »."
}
