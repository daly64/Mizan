package tn.mizan.app.claude

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import tn.mizan.app.data.Category
import tn.mizan.app.data.Expense
import tn.mizan.app.ui.MonthState
import tn.mizan.app.util.Dates
import tn.mizan.app.util.MizanCsv
import java.io.File
import java.time.LocalDate

/**
 * Échanges avec l'application Claude installée sur le téléphone, par le partage Android.
 * Rien ne part sans action de l'utilisateur, et Mizan n'a pas besoin d'internet.
 */
object ClaudeBridge {

    const val PACKAGE = "com.anthropic.claude"

    fun isInstalled(context: Context): Boolean =
        context.packageManager.getLaunchIntentForPackage(PACKAGE) != null

    /** Ouvre Claude, ou sa page sur le Play Store s'il n'est pas installé. */
    fun open(context: Context) {
        val launch = context.packageManager.getLaunchIntentForPackage(PACKAGE)
        val intent = launch ?: Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$PACKAGE"))
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            safeStart(context, Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$PACKAGE")))
        }
    }

    /** Envoie un texte à Claude (directement si l'app est installée, sinon via le menu de partage). */
    fun shareText(context: Context, text: String) {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        if (isInstalled(context)) {
            try {
                context.startActivity(Intent(send).setPackage(PACKAGE))
                return
            } catch (_: ActivityNotFoundException) {
                // L'app Claude n'accepte pas ce partage : on passe par le menu.
            }
        }
        safeStart(context, Intent.createChooser(send, "Envoyer à Claude"))
    }

    /** Bilan du mois : consigne + totaux + transactions du mois, prêt à envoyer à Claude. */
    fun monthReport(state: MonthState, today: LocalDate = LocalDate.now()): String = buildString {
        appendLine("Voici mes finances de ${Dates.monthTitle()}, exportées depuis mon application Mizan (dinar tunisien, DT).")
        appendLine(
            "Fais-moi un bilan clair en français : un résumé, les catégories qui dépassent ou approchent leur budget, " +
                "le rythme de dépense (nous sommes le ${today.dayOfMonth} sur ${today.lengthOfMonth()}), " +
                "puis 3 à 5 conseils concrets."
        )
        appendLine()
        appendLine("Totaux du mois :")
        appendLine("- Revenus : ${MizanCsv.plainAmount(state.income)} DT")
        appendLine("- Dépenses : ${MizanCsv.plainAmount(state.spent)} DT sur un budget de ${MizanCsv.plainAmount(state.budgetTotal)} DT")
        appendLine("- Solde : ${if (state.balance < 0) "-" else ""}${MizanCsv.plainAmount(kotlin.math.abs(state.balance))} DT")
        appendLine()
        appendLine("Par catégorie (dépensé / budget) :")
        Category.entries.forEach { c ->
            appendLine("- ${c.label} : ${MizanCsv.plainAmount(state.spentBy(c))} / ${MizanCsv.plainAmount(state.budgets[c] ?: 0L)} DT")
        }
        appendLine()
        appendLine("Transactions du mois :")
        append(MizanCsv.export(state.expenses))
    }

    /** Consigne à envoyer à Claude avec un ticket, une facture ou une liste, pour obtenir des lignes importables. */
    fun importPrompt(today: LocalDate = LocalDate.now()): String = """
        Je vais t'envoyer un ticket de caisse, une facture, une capture de relevé ou une liste de dépenses.
        Convertis-le pour mon application Mizan (dinar tunisien).
        Réponds UNIQUEMENT avec les lignes CSV ci-dessous, séparateur point-virgule, avec cet en-tête exact :
        ${MizanCsv.HEADER}
        - date : AAAA-MM-JJ (si absente, $today)
        - type : depense ou revenu
        - montant : en dinars avec un point et 3 décimales, sans symbole (ex. 12.500)
        - categorie : Courses, Transport, Loisirs ou Factures
        - paiement : Especes ou Carte (Especes par défaut)
        - note : courte description (sans point-virgule)
        Une ligne par achat ou par ticket, selon ce qui est le plus utile. Je partagerai ensuite ta réponse vers Mizan pour l'importer.
    """.trimIndent()

    /** Exporte toutes les transactions en fichier CSV et ouvre le menu de partage. */
    fun shareCsvFile(context: Context, expenses: List<Expense>) {
        if (expenses.isEmpty()) {
            Toast.makeText(context, "Aucune transaction à exporter.", Toast.LENGTH_SHORT).show()
            return
        }
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, "mizan-${LocalDate.now()}.csv")
        file.writeText(MizanCsv.export(expenses), Charsets.UTF_8)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Export Mizan")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        safeStart(context, Intent.createChooser(send, "Exporter le CSV"))
    }

    private fun safeStart(context: Context, intent: Intent) {
        try {
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, "Aucune application ne peut ouvrir ce contenu.", Toast.LENGTH_SHORT).show()
        }
    }
}
