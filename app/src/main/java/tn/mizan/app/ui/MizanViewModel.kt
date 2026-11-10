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
import tn.mizan.app.ai.AiReview
import tn.mizan.app.ai.ReviewState
import tn.mizan.app.ai.SecureSettings
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

/** Réglages IA affichés dans le Profil (la clé elle-même n'est jamais exposée à l'écran). */
data class AiSettings(
    val configured: Boolean,
    val model: String,
    val keyHint: String,
    val storageAvailable: Boolean
)

class MizanViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = (app as MizanApplication).database.dao()
    private val range = Dates.currentMonthRange()
    private val secure = SecureSettings(app)

    val state: StateFlow<MonthState> =
        combine(dao.expensesBetween(range.first, range.second), dao.budgets()) { expenses, budgets ->
            MonthState(expenses, budgets.associate { it.category to it.limitMillimes })
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MonthState())

    private val _ai = MutableStateFlow(readAi())
    val ai: StateFlow<AiSettings> = _ai.asStateFlow()

    private val _review = MutableStateFlow<ReviewState>(ReviewState.Idle)
    val review: StateFlow<ReviewState> = _review.asStateFlow()

    fun add(expense: Expense, onAdded: (Long) -> Unit = {}) {
        viewModelScope.launch { onAdded(dao.insert(expense)) }
    }

    fun delete(expense: Expense) {
        viewModelScope.launch { dao.delete(expense) }
    }

    fun deleteById(id: Long) {
        viewModelScope.launch { dao.deleteById(id) }
    }

    // --- IA ---

    fun saveAi(key: String?, model: String) {
        if (key != null) secure.apiKey = key
        secure.model = model
        _ai.value = readAi()
        _review.value = ReviewState.Idle
    }

    fun clearAiKey() {
        secure.apiKey = null
        _ai.value = readAi()
        _review.value = ReviewState.Idle
    }

    fun requestReview() {
        val key = secure.apiKey ?: return
        if (_review.value is ReviewState.Loading) return
        _review.value = ReviewState.Loading
        viewModelScope.launch {
            _review.value = AiReview.request(key, secure.model, state.value).fold(
                onSuccess = { ReviewState.Done(it) },
                onFailure = { ReviewState.Error(it.message ?: "Erreur inconnue.") }
            )
        }
    }

    private fun readAi(): AiSettings {
        val key = secure.apiKey
        return AiSettings(
            configured = key != null,
            model = secure.model,
            keyHint = key?.let { "…" + it.takeLast(4) } ?: "",
            storageAvailable = secure.isAvailable
        )
    }
}
