package tn.mizan.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import tn.mizan.app.MizanApplication
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
    private val dao = (app as MizanApplication).database.dao()
    private val range = Dates.currentMonthRange()

    val state: StateFlow<MonthState> =
        combine(dao.expensesBetween(range.first, range.second), dao.budgets()) { expenses, budgets ->
            MonthState(expenses, budgets.associate { it.category to it.limitMillimes })
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MonthState())

    fun add(expense: Expense) {
        viewModelScope.launch { dao.insert(expense) }
    }

    fun delete(expense: Expense) {
        viewModelScope.launch { dao.delete(expense) }
    }
}
