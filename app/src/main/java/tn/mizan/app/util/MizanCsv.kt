package tn.mizan.app.util

import tn.mizan.app.data.Category
import tn.mizan.app.data.Expense
import tn.mizan.app.data.PaymentMethod
import java.math.RoundingMode
import java.text.Normalizer
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Format d'échange de Mizan (avec l'app Claude, ou un tableur) :
 *   date;type;montant;categorie;paiement;note
 *   2026-11-10;depense;12.500;Transport;Especes;Taxi
 * La lecture est tolérante : point-virgule, tabulation, tableau Markdown, blocs ```.
 */
object MizanCsv {

    const val HEADER = "date;type;montant;categorie;paiement;note"

    data class Result(val expenses: List<Expense>, val ignored: Int)

    // --- Écriture ---

    fun export(expenses: List<Expense>): String = buildString {
        appendLine(HEADER)
        expenses.sortedWith(compareBy({ it.dateMillis }, { it.id })).forEach { appendLine(line(it)) }
    }

    fun line(e: Expense): String = listOf(
        Dates.toLocalDate(e.dateMillis).toString(),
        if (e.isIncome) "revenu" else "depense",
        plainAmount(e.amountMillimes),
        e.category.label,
        if (e.payment == PaymentMethod.CARTE) "Carte" else "Especes",
        e.note.replace(";", ",").replace('\n', ' ').trim()
    ).joinToString(";")

    fun plainAmount(millimes: Long): String =
        String.format(Locale.US, "%d.%03d", millimes / 1000, millimes % 1000)

    // --- Lecture ---

    fun parse(text: String): Result {
        val out = mutableListOf<Expense>()
        var ignored = 0
        for (raw in text.lines()) {
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("```")) continue
            val fields = split(line) ?: continue
            if (isHeader(fields)) continue
            val expense = toExpense(fields)
            if (expense == null) ignored++ else out += expense
        }
        return Result(out, ignored)
    }

    /** Découpe une ligne ; null pour les lignes à sauter sans les compter (séparateurs de tableau). */
    private fun split(line: String): List<String>? {
        val cells = when {
            line.startsWith("|") -> {
                if (line.replace(Regex("[|:\\-\\s]"), "").isEmpty()) return null
                line.trim('|').split("|")
            }
            ';' in line -> line.split(";")
            '\t' in line -> line.split("\t")
            else -> line.split(",")
        }
        return cells.map { it.trim().trim('"').trim() }
    }

    private fun isHeader(f: List<String>): Boolean {
        val first = norm(f.firstOrNull() ?: "")
        return first == "date" || f.any { norm(it) == "montant" }
    }

    private fun toExpense(f: List<String>): Expense? {
        if (f.size < 4) return null
        val date = parseDate(f[0]) ?: return null
        val amount = parseAmount(f[2]) ?: return null
        if (amount <= 0) return null
        val isIncome = norm(f[1]).startsWith("rev")
        val category = parseCategory(f[3])
        val payment = if (f.size > 4 && norm(f[4]).startsWith("cart")) PaymentMethod.CARTE else PaymentMethod.ESPECES
        val note = if (f.size > 5) f.subList(5, f.size).joinToString(", ").trim().take(60) else ""
        return Expense(
            amountMillimes = amount,
            category = category,
            note = note,
            dateMillis = Dates.toMillis(date),
            payment = payment,
            isIncome = isIncome
        )
    }

    private val DATE_FORMATS = listOf("yyyy-MM-dd", "dd/MM/yyyy", "d/M/yyyy", "dd-MM-yyyy", "dd.MM.yyyy")
        .map { DateTimeFormatter.ofPattern(it) }

    private fun parseDate(s: String): LocalDate? {
        val t = s.trim()
        for (f in DATE_FORMATS) {
            try {
                return LocalDate.parse(t, f)
            } catch (_: Exception) {
            }
        }
        return null
    }

    /** « 12.500 », « 12,5 », « 1 250.000 DT » -> millimes. */
    private fun parseAmount(s: String): Long? {
        var t = s.replace(Regex("[^0-9.,]"), "")
        if (t.isEmpty()) return null
        t = if (',' in t && '.' !in t) t.replace(',', '.') else t.replace(",", "")
        if (t.count { it == '.' } > 1) return null
        return t.toBigDecimalOrNull()?.movePointRight(3)?.setScale(0, RoundingMode.HALF_UP)?.toLong()
    }

    private fun parseCategory(s: String): Category {
        val n = norm(s)
        return when {
            n.startsWith("transp") -> Category.TRANSPORT
            n.startsWith("loisir") -> Category.LOISIRS
            n.startsWith("factur") -> Category.FACTURES
            else -> Category.COURSES
        }
    }

    private fun norm(s: String): String =
        Normalizer.normalize(s.lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "").trim()
}
