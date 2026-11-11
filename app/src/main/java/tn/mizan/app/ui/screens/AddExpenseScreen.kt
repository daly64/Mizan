package tn.mizan.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tn.mizan.app.data.Category
import tn.mizan.app.data.Expense
import tn.mizan.app.data.PaymentMethod
import tn.mizan.app.ui.components.CategoryTile
import tn.mizan.app.ui.components.DinarSymbol
import tn.mizan.app.ui.components.MizanCard
import tn.mizan.app.ui.theme.CardWhite
import tn.mizan.app.ui.theme.Emerald
import tn.mizan.app.ui.theme.Gold
import tn.mizan.app.ui.theme.Line
import tn.mizan.app.ui.theme.Mint
import tn.mizan.app.ui.theme.Muted
import tn.mizan.app.ui.theme.Slate
import tn.mizan.app.util.Dates
import tn.mizan.app.util.Money
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

private val amountPattern = Regex("^\\d{0,7}(\\.\\d{0,3})?$")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseScreen(onBack: () -> Unit, onSave: (Expense) -> Unit) {
    var isIncome by rememberSaveable { mutableStateOf(false) }
    var amountText by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf(Category.COURSES) }
    var note by rememberSaveable { mutableStateOf("") }
    var date by remember { mutableStateOf(LocalDate.now()) }
    var showPicker by remember { mutableStateOf(false) }

    val millimes = Money.parse(amountText) ?: 0L

    Column(
        Modifier
            .fillMaxSize()
            .imePadding()
            .padding(horizontal = 20.dp)
    ) {
        // Barre du haut
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = Slate)
            }
            Text(
                if (isIncome) "Nouveau revenu" else "Nouvelle dépense",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Slate,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(48.dp))
        }

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Dépense / Revenu
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Mint)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(false to "Dépense", true to "Revenu").forEach { (income, label) ->
                    val selected = isIncome == income
                    Box(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) CardWhite else Color.Transparent)
                            .clickable { isIncome = income }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            label,
                            fontSize = 14.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                            color = if (selected) Emerald else Muted
                        )
                    }
                }
            }

            // Montant
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Montant", fontSize = 13.sp, color = Muted)
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DinarSymbol(Slate, 34.dp)
                    Spacer(Modifier.width(8.dp))
                    BasicTextField(
                        value = amountText,
                        onValueChange = { v ->
                            val cleaned = v.replace(',', '.')
                            if (amountPattern.matches(cleaned)) amountText = cleaned
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        textStyle = TextStyle(fontSize = 40.sp, fontWeight = FontWeight.SemiBold, color = Slate),
                        modifier = Modifier.widthIn(min = 60.dp, max = 220.dp),
                        decorationBox = { inner ->
                            if (amountText.isEmpty()) {
                                Text("0.000", fontSize = 40.sp, fontWeight = FontWeight.SemiBold, color = Line)
                            }
                            inner()
                        }
                    )
                }
                Spacer(Modifier.height(6.dp))
                Box(Modifier.width(180.dp).height(2.dp).background(Gold))
            }

            Label("Catégorie")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Category.entries.forEach { c ->
                    CategoryTile(c, Modifier.weight(1f), selected = c == category, onClick = { category = c })
                }
            }

            Label("Date")
            MizanCard(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { showPicker = true }
            ) {
                Row(Modifier.padding(horizontal = 14.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.CalendarMonth, contentDescription = null, tint = Emerald, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(Dates.longLabel(date), fontSize = 14.sp, color = Slate, modifier = Modifier.weight(1f))
                    Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = Muted)
                }
            }

            Label("Note")
            OutlinedTextField(
                value = note,
                onValueChange = { note = it.take(60) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("Ajouter une note…") },
                leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null, tint = Emerald) },
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = CardWhite,
                    unfocusedContainerColor = CardWhite,
                    unfocusedBorderColor = Line,
                    focusedBorderColor = Emerald
                )
            )

        }

        Button(
            onClick = {
                onSave(
                    Expense(
                        amountMillimes = millimes,
                        category = category,
                        note = note.trim(),
                        dateMillis = Dates.toMillis(date),
                        payment = PaymentMethod.ESPECES,
                        isIncome = isIncome
                    )
                )
            },
            enabled = millimes > 0,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Emerald)
        ) {
            Text(
                if (isIncome) "Enregistrer le revenu" else "Enregistrer la dépense",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }

    if (showPicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let {
                        date = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                    }
                    showPicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text("Annuler") } }
        ) {
            DatePicker(pickerState)
        }
    }
}

@Composable
private fun Label(text: String) {
    Text(text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Slate)
}
