package tn.mizan.app.agent

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

/**
 * Petite calculatrice exacte pour l'assistant : + - * / % ( ) et nombres décimaux.
 * Le modèle n'a jamais à calculer lui-même : il passe l'expression ici.
 * « 20% de 350 » s'écrit 350*20/100 ; « 2,5 » est accepté comme 2.5.
 */
object Calculator {

    fun evaluate(expression: String): BigDecimal {
        val cleaned = expression
            .replace('×', '*').replace('x', '*').replace('X', '*')
            .replace('÷', '/').replace(':', '/')
            .replace(Regex("(?<=\\d),(?=\\d)"), ".")
            .replace(" ", "")
        require(cleaned.isNotEmpty()) { "expression vide" }
        val parser = Parser(cleaned)
        val result = parser.parseExpression()
        require(parser.pos == cleaned.length) { "caractère inattendu : « ${cleaned[parser.pos]} »" }
        return result
    }

    /** Résultat lisible : au plus 3 décimales (les millimes), sans zéros inutiles. */
    fun format(value: BigDecimal): String =
        value.setScale(3, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()

    private class Parser(val s: String) {
        var pos = 0
        private val mc = MathContext.DECIMAL64

        fun parseExpression(): BigDecimal {
            var value = parseTerm()
            while (pos < s.length && (s[pos] == '+' || s[pos] == '-')) {
                val op = s[pos++]
                val right = parseTerm()
                value = if (op == '+') value.add(right, mc) else value.subtract(right, mc)
            }
            return value
        }

        private fun parseTerm(): BigDecimal {
            var value = parseFactor()
            while (pos < s.length && (s[pos] == '*' || s[pos] == '/' || s[pos] == '%')) {
                val op = s[pos++]
                val right = parseFactor()
                value = when (op) {
                    '*' -> value.multiply(right, mc)
                    '/' -> {
                        require(right.signum() != 0) { "division par zéro" }
                        value.divide(right, mc)
                    }
                    else -> value.remainder(right, mc)
                }
            }
            return value
        }

        private fun parseFactor(): BigDecimal {
            require(pos < s.length) { "expression incomplète" }
            return when (s[pos]) {
                '-' -> { pos++; parseFactor().negate() }
                '+' -> { pos++; parseFactor() }
                '(' -> {
                    pos++
                    val v = parseExpression()
                    require(pos < s.length && s[pos] == ')') { "parenthèse non fermée" }
                    pos++
                    v
                }
                else -> {
                    val start = pos
                    while (pos < s.length && (s[pos].isDigit() || s[pos] == '.')) pos++
                    require(pos > start) { "nombre attendu à la position ${start + 1}" }
                    BigDecimal(s.substring(start, pos))
                }
            }
        }
    }
}
