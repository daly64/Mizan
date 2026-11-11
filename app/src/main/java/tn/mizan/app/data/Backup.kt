package tn.mizan.app.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalDate

/**
 * Sauvegarde automatique, 100 % locale.
 * À la fin de chaque session (quand on quitte Mizan), toutes les données sont écrites dans :
 * - le dossier de l'app (Android/data/tn.mizan.app/files/Sauvegardes) ;
 * - et, si on l'a choisi dans Profil, un dossier du téléphone (ex. Documents/Mizan),
 *   qui reste même si l'app est désinstallée.
 * Les notes de l'assistant sont incluses (v0.11).
 * On garde « mizan-derniere.json » plus une copie par jour (les 7 derniers jours).
 */
object Backup {

    private const val PREFS = "mizan_reglages"
    private const val KEY_FOLDER = "dossier_sauvegarde"
    private const val KEY_LAST = "derniere_sauvegarde"
    private const val LATEST = "mizan-derniere.json"
    private const val DAILY_PREFIX = "mizan-20"
    private const val KEEP_DAYS = 7

    data class Content(
        val savedAt: Long,
        val expenses: List<Expense>,
        val budgets: List<Budget>,
        /** null pour les sauvegardes d'avant la v0.11 (sans notes). */
        val notes: List<Note>? = null
    )

    // --- Réglages ---

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun lastSaved(context: Context): Long = prefs(context).getLong(KEY_LAST, 0L)

    fun folder(context: Context): Uri? = prefs(context).getString(KEY_FOLDER, null)?.let(Uri::parse)

    fun folderName(context: Context): String? =
        folder(context)?.let { DocumentFile.fromTreeUri(context, it)?.name }

    /** Mémorise le dossier choisi et garde l'autorisation d'y écrire après redémarrage. */
    fun setFolder(context: Context, uri: Uri) {
        context.contentResolver.takePersistableUriPermission(
            uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
        prefs(context).edit().putString(KEY_FOLDER, uri.toString()).apply()
    }

    // --- Format ---

    fun toJson(expenses: List<Expense>, budgets: List<Budget>, notes: List<Note>, savedAt: Long): String = JSONObject()
        .put("app", "Mizan")
        .put("version", 1)
        .put("savedAt", savedAt)
        .put("expenses", JSONArray().apply {
            expenses.forEach { e ->
                put(
                    JSONObject()
                        .put("id", e.id)
                        .put("amountMillimes", e.amountMillimes)
                        .put("category", e.category.name)
                        .put("note", e.note)
                        .put("dateMillis", e.dateMillis)
                        .put("payment", e.payment.name)
                        .put("isIncome", e.isIncome)
                )
            }
        })
        .put("budgets", JSONArray().apply {
            budgets.forEach { b -> put(JSONObject().put("category", b.category.name).put("limitMillimes", b.limitMillimes)) }
        })
        .put("notes", JSONArray().apply {
            notes.forEach { n -> put(JSONObject().put("id", n.id).put("text", n.text).put("dateMillis", n.dateMillis)) }
        })
        .toString(2)

    /** Lit une sauvegarde ; null si le fichier n'est pas une sauvegarde Mizan. */
    fun parse(text: String): Content? = runCatching {
        val root = JSONObject(text)
        if (root.optString("app") != "Mizan") return null
        val ex = root.getJSONArray("expenses")
        val expenses = (0 until ex.length()).map { i ->
            val o = ex.getJSONObject(i)
            Expense(
                id = o.optLong("id", 0L),
                amountMillimes = o.getLong("amountMillimes"),
                category = Category.valueOf(o.getString("category")),
                note = o.optString("note", ""),
                dateMillis = o.getLong("dateMillis"),
                payment = runCatching { PaymentMethod.valueOf(o.getString("payment")) }.getOrDefault(PaymentMethod.ESPECES),
                isIncome = o.optBoolean("isIncome", false)
            )
        }
        val bu = root.optJSONArray("budgets") ?: JSONArray()
        val budgets = (0 until bu.length()).mapNotNull { i ->
            val o = bu.getJSONObject(i)
            runCatching { Budget(Category.valueOf(o.getString("category")), o.getLong("limitMillimes")) }.getOrNull()
        }
        val notes = root.optJSONArray("notes")?.let { no ->
            (0 until no.length()).mapNotNull { i ->
                val o = no.getJSONObject(i)
                runCatching { Note(o.optLong("id", 0L), o.getString("text"), o.getLong("dateMillis")) }.getOrNull()
            }
        }
        Content(root.optLong("savedAt", 0L), expenses, budgets, notes)
    }.getOrNull()

    // --- Écriture ---

    /**
     * Sauvegarde maintenant. Ne fait rien s'il n'y a aucune transaction,
     * pour ne jamais écraser une bonne sauvegarde par une base vide (ex. après réinstallation).
     * @return true si la sauvegarde a été écrite.
     */
    suspend fun saveNow(context: Context, db: MizanDatabase): Boolean = withContext(Dispatchers.IO) {
        val dao = db.dao()
        val expenses = dao.allExpenses()
        val notes = dao.allNotes()
        if (expenses.isEmpty() && notes.isEmpty()) return@withContext false
        val now = System.currentTimeMillis()
        val json = toJson(expenses, dao.allBudgets(), notes, now)
        val daily = "mizan-${LocalDate.now()}.json"

        var ok = runCatching {
            val dir = File(context.getExternalFilesDir(null) ?: context.filesDir, "Sauvegardes").apply { mkdirs() }
            File(dir, LATEST).writeText(json)
            File(dir, daily).writeText(json)
            dir.listFiles { f -> f.name.startsWith(DAILY_PREFIX) }
                ?.sortedByDescending { it.name }?.drop(KEEP_DAYS)?.forEach { it.delete() }
        }.isSuccess

        folder(context)?.let { uri ->
            ok = runCatching {
                val tree = DocumentFile.fromTreeUri(context, uri) ?: error("dossier introuvable")
                writeDoc(context, tree, LATEST, json)
                writeDoc(context, tree, daily, json)
                tree.listFiles().filter { it.name?.startsWith(DAILY_PREFIX) == true }
                    .sortedByDescending { it.name }.drop(KEEP_DAYS).forEach { it.delete() }
            }.isSuccess && ok
        }

        if (ok) prefs(context).edit().putLong(KEY_LAST, now).apply()
        ok
    }

    private fun writeDoc(context: Context, tree: DocumentFile, name: String, text: String) {
        val file = tree.findFile(name) ?: tree.createFile("application/json", name.removeSuffix(".json"))
            ?: error("création impossible")
        context.contentResolver.openOutputStream(file.uri, "wt")?.use { it.write(text.toByteArray(Charsets.UTF_8)) }
            ?: error("écriture impossible")
    }

    /** Lit un fichier choisi par l'utilisateur. */
    fun read(context: Context, uri: Uri): Content? = runCatching {
        context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
    }.getOrNull()?.let(::parse)
}
