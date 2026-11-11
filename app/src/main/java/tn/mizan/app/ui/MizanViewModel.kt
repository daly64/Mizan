package tn.mizan.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import androidx.room.withTransaction
import tn.mizan.app.MizanApplication
import tn.mizan.app.data.Backup
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

class MizanViewModel(app: Application) : AndroidViewModel(app) {
    private val db = (app as MizanApplication).database
    private val dao = db.dao()
    private val range = Dates.currentMonthRange()

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

    /** Remplace toutes les données par celles d'une sauvegarde. */
    fun restore(content: Backup.Content, onDone: () -> Unit) {
        viewModelScope.launch {
            db.withTransaction {
                dao.deleteAllExpenses()
                dao.insertAll(content.expenses)
                if (content.budgets.isNotEmpty()) dao.upsertBudgets(content.budgets)
                content.notes?.let { notes ->
                    dao.deleteAllNotes()
                    dao.insertNotes(notes)
                }
            }
            onDone()
        }
    }

    /** Sauvegarde tout de suite (bouton « Sauvegarder maintenant »). */
    fun backupNow(onDone: (Boolean) -> Unit) {
        viewModelScope.launch { onDone(Backup.saveNow(getApplication(), db)) }
    }
}
