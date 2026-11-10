package tn.mizan.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tn.mizan.app.data.Category
import tn.mizan.app.data.Expense
import tn.mizan.app.ui.MonthState
import tn.mizan.app.ui.components.EmptyHint
import tn.mizan.app.ui.components.MizanCard
import tn.mizan.app.ui.components.MoneyText
import tn.mizan.app.ui.components.TransactionRow
import tn.mizan.app.ui.theme.CardWhite
import tn.mizan.app.ui.theme.Danger
import tn.mizan.app.ui.theme.Emerald
import tn.mizan.app.ui.theme.Line
import tn.mizan.app.ui.theme.Muted
import tn.mizan.app.ui.theme.Slate
import tn.mizan.app.util.Dates

@Composable
fun TransactionsScreen(state: MonthState, onDelete: (Expense) -> Unit) {
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf<Category?>(null) }
    var toDelete by remember { mutableStateOf<Expense?>(null) }

    val list = state.expenses.filter { e ->
        (filter == null || e.category == filter) &&
            (query.isBlank() || e.note.contains(query, ignoreCase = true) || e.category.label.contains(query, ignoreCase = true))
    }
    val byDay = list.groupBy { Dates.toLocalDate(it.dateMillis) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Text("Transactions", fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = Slate) }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("Rechercher une dépense") },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = Muted) },
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = CardWhite,
                    unfocusedContainerColor = CardWhite,
                    unfocusedBorderColor = Line,
                    focusedBorderColor = Emerald
                )
            )
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { Chip("Tout", filter == null) { filter = null } }
                items(Category.entries) { c -> Chip(c.label, filter == c) { filter = c } }
            }
        }
        item {
            MizanCard(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(Dates.monthTitle(), fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Slate)
                        Text("${state.expenses.size} transactions", fontSize = 12.sp, color = Muted)
                    }
                    MoneyText(state.spent, 18.sp, Emerald, sign = "−")
                }
            }
        }
        if (list.isEmpty()) {
            item { EmptyHint("Aucune transaction à afficher.") }
        }
        byDay.forEach { (day, expenses) ->
            item(key = "jour-$day") {
                Text(Dates.dayLabel(day), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Muted)
            }
            items(expenses, key = { it.id }) { e -> TransactionRow(e, onLongClick = { toDelete = e }) }
        }
        if (list.isNotEmpty()) {
            item {
                Text(
                    "Astuce : appui long sur une transaction pour la supprimer.",
                    fontSize = 12.sp,
                    color = Muted
                )
            }
        }
    }

    toDelete?.let { e ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Supprimer cette transaction ?") },
            text = { Text(e.note.ifBlank { e.category.label }) },
            confirmButton = {
                TextButton(onClick = { onDelete(e); toDelete = null }) { Text("Supprimer", color = Danger) }
            },
            dismissButton = {
                TextButton(onClick = { toDelete = null }) { Text("Annuler") }
            }
        )
    }
}

@Composable
private fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        shape = RoundedCornerShape(18.dp),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = CardWhite,
            labelColor = Slate,
            selectedContainerColor = Emerald,
            selectedLabelColor = Color.White
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = Line,
            selectedBorderColor = Emerald
        )
    )
}
