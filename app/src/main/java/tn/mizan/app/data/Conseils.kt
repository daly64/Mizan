package tn.mizan.app.data

import tn.mizan.app.util.Dates
import tn.mizan.app.util.Money
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Bilan et conseils de la mini IA, calculés sur le téléphone à partir de tes transactions :
 * projection de fin de mois, budgets, comparaison avec tes 3 derniers mois, dépenses inhabituelles,
 * jour de la semaine le plus dépensier, épargne, jours sans dépense.
 * Aucun texte n'est inventé : chaque phrase vient d'un calcul sur tes chiffres.
 */
object Conseils {

    enum class Kind { ALERTE, ATTENTION, BRAVO, INFO }

    data class Conseil(val kind: Kind, val text: String)

    data class Bilan(val resume: String, val conseils: List<Conseil>)

    private fun Expense.day(): LocalDate = Dates.toLocalDate(dateMillis)
    private fun List<Expense>.spent() = filter { !it.isIncome }.sumOf { it.amountMillimes }
    private fun List<Expense>.income() = filter { it.isIncome }.sumOf { it.amountMillimes }
    private fun dt(millimes: Long) = Money.format(millimes) + " DT"
    private fun pct(part: Long, total: Long) = if (total == 0L) 0 else (part * 100f / total).roundToInt()

    fun pour(month: YearMonth, all: List<Expense>, budgets: Map<Category, Long>, today: LocalDate = LocalDate.now()): Bilan {
        val inMonth = all.filter { YearMonth.from(it.day()) == month }
        val spent = inMonth.spent()
        val income = inMonth.income()
        val monthName = month.month.getDisplayName(TextStyle.FULL, Locale.FRENCH)
        val isCurrent = month == YearMonth.from(today)
        val length = month.lengthOfMonth()
        val elapsed = if (isCurrent) today.dayOfMonth else length

        if (inMonth.none { !it.isIncome }) {
            return Bilan(
                "Aucune dépense en $monthName pour l’instant.",
                listOf(Conseil(Kind.INFO, "Ajoute tes dépenses avec une note courte (« taxi », « kahwa », « steg ») : la mini IA apprend tes habitudes et devine la catégorie à ta place."))
            )
        }

        val byCategory = Category.entries.associateWith { c -> inMonth.filter { !it.isIncome && it.category == c }.sumOf { it.amountMillimes } }
        val top = byCategory.maxByOrNull { it.value }!!
        val resume = buildString {
            append("En $monthName : ${dt(spent)} dépensés")
            if (top.value > 0) append(", surtout en ${top.key.label} (${pct(top.value, spent)} %)")
            append(".")
            if (income > 0) append(" Solde : ${if (income - spent < 0) "−" else ""}${dt(abs(income - spent))}.")
        }

        val out = mutableListOf<Conseil>()

        // 1. Projection de fin de mois et budget global
        val budgetTotal = budgets.values.sum()
        if (isCurrent && elapsed >= 3 && elapsed < length) {
            val projection = spent * length / elapsed
            if (budgetTotal > 0 && projection > budgetTotal) {
                val left = budgetTotal - spent
                val daysLeft = length - elapsed
                out += if (left > 0) Conseil(
                    Kind.ALERTE,
                    "À ce rythme, tu finiras $monthName vers ${dt(projection)}, soit ${dt(projection - budgetTotal)} au-dessus de ton budget. " +
                        "Pour le respecter : au plus ${dt(left / daysLeft)} par jour d’ici la fin du mois."
                ) else Conseil(Kind.ALERTE, "Budget du mois déjà dépassé de ${dt(-left)}, avec encore $daysLeft jours à tenir.")
            } else if (budgetTotal > 0) {
                out += Conseil(Kind.BRAVO, "À ce rythme, tu finiras $monthName vers ${dt(projection)}, sous ton budget de ${dt(budgetTotal)}.")
            } else {
                out += Conseil(Kind.INFO, "À ce rythme, tu finiras $monthName vers ${dt(projection)}.")
            }
        } else if (!isCurrent && budgetTotal > 0) {
            out += if (spent > budgetTotal) Conseil(Kind.ATTENTION, "Budget dépassé de ${dt(spent - budgetTotal)} ce mois-là.")
            else Conseil(Kind.BRAVO, "Budget respecté : ${dt(budgetTotal - spent)} de marge.")
        }

        // 2. Catégories qui dépassent (ou vont dépasser) leur budget
        byCategory.forEach { (c, amount) ->
            val limit = budgets[c] ?: return@forEach
            if (limit <= 0 || amount == 0L) return@forEach
            val expected = if (isCurrent && elapsed in 7 until length) amount * length / elapsed else amount
            when {
                amount > limit -> out.add(Conseil(Kind.ALERTE, "${c.label} : budget dépassé (${dt(amount)} pour ${dt(limit)})."))
                isCurrent && expected > limit * 11 / 10 ->
                    out.add(Conseil(Kind.ATTENTION, "${c.label} : déjà ${pct(amount, limit)} % du budget, tu risques de le dépasser vers ${dt(expected)}."))
            }
        }

        // 3. Comparaison avec la moyenne des 3 mois précédents, par catégorie
        val previous = (1..3).map { month.minusMonths(it.toLong()) }
            .map { m -> all.filter { YearMonth.from(it.day()) == m } }
            .filter { list -> list.any { !it.isIncome } }
        if (previous.isNotEmpty() && (!isCurrent || elapsed >= 10)) {
            byCategory.forEach { (c, amount) ->
                val avg = previous.sumOf { list -> list.filter { !it.isIncome && it.category == c }.sumOf { it.amountMillimes } } / previous.size
                val compared = if (isCurrent) amount * length / elapsed else amount
                if (avg < 20_000) return@forEach // moins de 20 dinars : pas significatif
                val change = pct(compared - avg, avg)
                val period = if (previous.size == 1) "le mois précédent" else "ta moyenne des ${previous.size} derniers mois"
                when {
                    change >= 30 -> out.add(Conseil(Kind.ATTENTION, "${c.label} : +$change % par rapport à $period (${dt(compared)} contre ${dt(avg)}${if (isCurrent) ", projection" else ""})."))
                    change <= -20 -> out.add(Conseil(Kind.BRAVO, "${c.label} : ${abs(change)} % de moins que $period. Bien joué !"))
                }
            }
        }

        // 4. Dépenses inhabituelles : bien plus chères que d'habitude dans leur catégorie (90 derniers jours)
        val history = all.filter { !it.isIncome && !it.day().isBefore(month.atEndOfMonth().minusDays(90)) }
        inMonth.filter { !it.isIncome && it.amountMillimes >= 20_000 }
            .mapNotNull { e ->
                val usual = history.filter { it.category == e.category && it.id != e.id }.map { it.amountMillimes }.sorted()
                if (usual.size < 5) return@mapNotNull null
                val median = usual[usual.size / 2].coerceAtLeast(1L)
                val ratio = e.amountMillimes.toFloat() / median
                if (ratio >= 3f) e to ratio else null
            }
            .sortedByDescending { it.second }
            .take(2)
            .forEach { (e, ratio) ->
                val what = e.note.ifBlank { e.category.label }
                out += Conseil(Kind.ATTENTION, "Dépense inhabituelle : $what, ${dt(e.amountMillimes)} le ${e.day().format(DateTimeFormatter.ofPattern("d MMM", Locale.FRENCH))}, environ ${ratio.roundToInt()} fois ta dépense habituelle en ${e.category.label}.")
            }

        // 5. Le jour de la semaine le plus dépensier (sur les 8 dernières semaines)
        val since = today.minusWeeks(8)
        val recent = all.filter { !it.isIncome && it.day().isAfter(since) && !it.day().isAfter(today) }
        val firstDay = recent.minOfOrNull { it.day() }
        if (firstDay != null && firstDay.plusWeeks(4) <= today) {
            val weeks = ((today.toEpochDay() - firstDay.toEpochDay()) / 7f).coerceAtLeast(1f)
            val perWeekday = DayOfWeek.values().toList().associateWith { d -> recent.filter { it.day().dayOfWeek == d }.sumOf { it.amountMillimes } / weeks }
            val best = perWeekday.maxByOrNull { it.value }!!
            val others = perWeekday.filterKeys { it != best.key }.values.average().toFloat()
            if (others > 0 && best.value / others >= 1.6f) {
                val name = best.key.getDisplayName(TextStyle.FULL, Locale.FRENCH)
                out += Conseil(Kind.INFO, "Tu dépenses surtout le $name : ${dt(best.value.toLong())} en moyenne, contre ${dt(others.toLong())} les autres jours.")
            }
        }

        // 6. Épargne
        if (income > 0) {
            val saved = income - spent
            val rate = pct(saved, income)
            when {
                saved < 0 -> out.add(Conseil(Kind.ALERTE, "Tu dépenses plus que tes revenus en $monthName (${dt(-saved)} de plus)."))
                rate >= 20 -> out.add(Conseil(Kind.BRAVO, "Tu as mis de côté $rate % de tes revenus (${dt(saved)})."))
                else -> out.add(Conseil(Kind.INFO, "Tu gardes $rate % de tes revenus. Viser 20 % aide à faire face aux imprévus."))
            }
        }

        // 7. Jours sans aucune dépense
        val spendDays = inMonth.filter { !it.isIncome }.map { it.day() }.toSet()
        val noSpend = (1..elapsed).count { month.atDay(it) !in spendDays }
        if (noSpend >= 5) out += Conseil(Kind.BRAVO, "$noSpend jours sans aucune dépense en $monthName.")

        return Bilan(resume, out.sortedBy { it.kind.ordinal }.take(6))
    }
}
