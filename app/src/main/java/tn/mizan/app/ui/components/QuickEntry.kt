package tn.mizan.app.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tn.mizan.app.data.Category
import tn.mizan.app.ui.theme.CardWhite
import tn.mizan.app.ui.theme.Cream
import tn.mizan.app.ui.theme.Danger
import tn.mizan.app.ui.theme.Emerald
import tn.mizan.app.ui.theme.Line
import tn.mizan.app.ui.theme.Muted
import tn.mizan.app.ui.theme.Slate
import tn.mizan.app.util.Dates
import tn.mizan.app.util.QuickEntry

/** Barre de saisie rapide : on tape « 20 taxi », ou on touche le micro pour dicter. */
@Composable
fun QuickEntryBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onMic: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = text,
        onValueChange = onTextChange,
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        placeholder = { Text("Ex. : 20 taxi", color = Muted) },
        leadingIcon = { Icon(Icons.Outlined.Bolt, contentDescription = null, tint = Emerald) },
        trailingIcon = {
            if (text.isBlank()) {
                IconButton(onClick = onMic) {
                    Icon(Icons.Outlined.Mic, contentDescription = "Dicter une dépense", tint = Emerald)
                }
            } else {
                IconButton(onClick = onSubmit) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Analyser", tint = Emerald)
                }
            }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { if (text.isNotBlank()) onSubmit() }),
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = CardWhite,
            unfocusedContainerColor = CardWhite,
            unfocusedBorderColor = Line,
            focusedBorderColor = Emerald
        )
    )
}

/** Carte de confirmation : ce que l'app a compris, à valider ou à modifier. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickConfirmSheet(
    entry: QuickEntry,
    onChange: (QuickEntry) -> Unit,
    onConfirm: () -> Unit,
    onEdit: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Cream
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("J’ai compris :", fontSize = 13.sp, color = Muted)

            val amount = entry.amountMillimes
            if (amount == null || amount <= 0) {
                Text(
                    "Je n’ai pas trouvé de montant. Essaie par exemple « 20 taxi » ou « 3.500 café ».",
                    fontSize = 14.sp,
                    color = Danger
                )
            } else {
                MoneyText(
                    amount,
                    32.sp,
                    if (entry.isIncome) Emerald else Slate,
                    sign = if (entry.isIncome) "+" else null
                )
            }

            // Dépense ou revenu
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip("Dépense", !entry.isIncome) { onChange(entry.copy(isIncome = false)) }
                Chip("Revenu", entry.isIncome) { onChange(entry.copy(isIncome = true)) }
            }

            // Catégorie (pré-choisie par les mots-clés, modifiable d'un geste)
            if (!entry.isIncome) {
                if (entry.category == null) {
                    Text("Choisis une catégorie :", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Slate)
                }
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Category.entries.forEach { c ->
                        FilterChip(
                            selected = entry.category == c,
                            onClick = { onChange(entry.copy(category = c)) },
                            label = { Text(c.label) },
                            leadingIcon = { Icon(c.icon, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            shape = RoundedCornerShape(18.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = CardWhite,
                                iconColor = Emerald,
                                selectedContainerColor = Emerald,
                                selectedLabelColor = Color.White,
                                selectedLeadingIconColor = Color.White
                            )
                        )
                    }
                }
            }

            val details = buildList {
                add(Dates.dayLabel(entry.date))
                add(entry.payment.label)
                if (entry.note.isNotBlank()) add(entry.note)
            }.joinToString(" · ")
            Text(details, fontSize = 14.sp, color = Slate)

            Spacer(Modifier.height(2.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Modifier", color = Emerald, fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = onConfirm,
                    enabled = amount != null && amount > 0 && (entry.isIncome || entry.category != null),
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald)
                ) {
                    Text("Valider", fontWeight = FontWeight.SemiBold)
                }
            }
        }
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
            selectedContainerColor = Emerald,
            selectedLabelColor = Color.White
        )
    )
}
