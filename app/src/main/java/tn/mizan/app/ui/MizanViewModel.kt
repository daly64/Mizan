package tn.mizan.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import tn.mizan.app.MizanApplication
import tn.mizan.app.claude.ClaudeSettings
import tn.mizan.app.data.Category
import tn.mizan.app.data.Expense
import tn.mizan.app.util.Dates

/** Tout ce que les écrans affichent pour le mois en cours. */
data class MonthState(
    val expenses: List<Expense> = emptyList(),
    val budgets: Map<Category, Long> = emptyMap()
) {
    val spent: Long get() = expenses.filter { !it.isIncome }.sumOf { it.amountMillimes }
    val income: Long get() = expenses.filter { it.isIncome }.sumOf { it.amountMillimes }
    val balance: Long get() = income - spent
    val budgetTotal: Long get() = budgets.values.sum()
    val budgetRatio: Float get() = if (budgetTotal > 0) spent.toFloat() / budgetTotal else 0f

    fun spentBy(category: Category): Long =
        expenses.filter { !it.isIncome && it.category == category }.sumOf { it.amountMillimes }
}

/** Réglages Claude affichés dans le Profil (la clé elle-même n'est jamais affichée). */
data class ClaudeConfig(
    val configured: Boolean,
    val model: String,
    val keyHint: String,
    val storageAvailable: Boolean
)

class MizanViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = (app as MizanApplication).database.dao()
    private val range = Dates.currentMonthRange()
    private val claudeSettings = ClaudeSettings(app)

    private val _claude = MutableStateFlow(readClaude())
    val claude: StateFlow<ClaudeConfig> = _claude.asStateFlow()

    /** Clé et modèle pour un appel direct à Claude (null si pas de clé). */
    fun claudeCredentials(): Pair<String, String>? =
        claudeSettings.apiKey?.let { it to claudeSettings.model }

    fun saveClaude(key: String?, model: String) {
        if (key != null) claudeSettings.apiKey = key
        claudeSettings.model = model
        _claude.value = readClaude()
    }

    fun clearClaudeKey() {
        claudeSettings.apiKey = null
        _claude.value = readClaude()
    }

    private fun readClaude(): ClaudeConfig {
        val key = claudeSettings.apiKey
        return ClaudeConfig(
            configured = key != null,
            model = claudeSettings.model,
            keyHint = key?.let { "…" + it.takeLast(4) } ?: "",
            storageAvailable = claudeSettings.isAvailable
        )
    }

    val state: StateFlow<MonthState> =
        combine(dao.expensesBetween(range.first, range.second), dao.budgets()) { expenses, budgets ->
            MonthState(expenses, budgets.associate { it.category to it.limitMillimes })
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MonthState())

    fun add(expense: Expense, onAdded: (Long) -> Unit = {}) {
        viewModelScope.launch { onAdded(dao.insert(expense)) }
    }

    fun delete(expense: Expense) {
        viewModelScope.launch { dao.delete(expense) }
    }

    fun deleteById(id: Long) {
        viewModelScope.launch { dao.deleteById(id) }
    }

    /** Import depuis Claude (ou un CSV) : ajoute toutes les lignes reconnues. */
    fun importAll(expenses: List<Expense>, onDone: (Int) -> Unit) {
        viewModelScope.launch {
            dao.insertAll(expenses)
            onDone(expenses.size)
        }
    }

    /** Toutes les transactions (tous les mois), pour l'export CSV. */
    fun withAllExpenses(block: (List<Expense>) -> Unit) {
        viewModelScope.launch { block(dao.allExpenses()) }
    }
}
