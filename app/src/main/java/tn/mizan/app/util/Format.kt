package tn.mizan.app.util

import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs

/** Format des montants : 2,825.225 (virgule pour les milliers, millimes après le point). */
object Money {
    fun parts(millimes: Long): Pair<String, String> {
        val value = abs(millimes)
        val dinars = String.format(Locale.US, "%,d", value / 1000)
        val rest = String.format(Locale.US, ".%03d", value % 1000)
        return dinars to rest
    }

    fun format(millimes: Long): String = parts(millimes).let { it.first + it.second }


    /** « 64.3 » -> 64300 millimes. Renvoie null si la saisie n'est pas un nombre. */
    fun parse(input: String): Long? =
        input.replace(",", "").trim().toBigDecimalOrNull()
            ?.movePointRight(3)
            ?.setScale(0, RoundingMode.HALF_UP)
            ?.toLong()
}

object Dates {
    private val zone: ZoneId get() = ZoneId.systemDefault()
    private val dayMonth = DateTimeFormatter.ofPattern("d MMM", Locale.FRENCH)
    private val full = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.FRENCH)

    fun toLocalDate(millis: Long): LocalDate = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()

    fun dayLabel(date: LocalDate): String {
        val today = LocalDate.now()
        return when (date) {
            today -> "Aujourd’hui"
            today.minusDays(1) -> "Hier"
            else -> date.format(dayMonth)
        }
    }

    fun longLabel(date: LocalDate): String {
        val text = date.format(full)
        return if (date == LocalDate.now()) "Aujourd’hui, $text" else text
    }

    fun monthName(): String = YearMonth.now().month.getDisplayName(TextStyle.FULL, Locale.FRENCH)

    fun monthTitle(): String =
        YearMonth.now().format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.FRENCH))
            .replaceFirstChar { it.uppercase() }

    /** Début et fin du mois en cours, en millisecondes. */
    fun currentMonthRange(): Pair<Long, Long> {
        val month = YearMonth.now()
        val start = month.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val end = month.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
        return start to end
    }

    /** Date choisie -> instant enregistré (l'heure actuelle si c'est aujourd'hui, sinon midi). */
    fun toMillis(date: LocalDate): Long =
        if (date == LocalDate.now()) System.currentTimeMillis()
        else date.atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
}
