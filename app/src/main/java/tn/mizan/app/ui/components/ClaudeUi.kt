package tn.mizan.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tn.mizan.app.data.Expense
import tn.mizan.app.ui.theme.Cream
import tn.mizan.app.ui.theme.Emerald
import tn.mizan.app.ui.theme.Gold
import tn.mizan.app.ui.theme.Mint
import tn.mizan.app.ui.theme.Muted
import tn.mizan.app.ui.theme.Slate

/** La seule porte d'entrée vers Claude : deux boutons sur l'accueil. */
@Composable
fun ClaudeCard(onReview: () -> Unit, onTicket: () -> Unit) {
    MizanCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconCircle(Icons.Outlined.AutoAwesome, 36.dp, tint = Gold, background = Mint)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Avec Claude", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Slate)
                    Text("Ton bilan, ou un ticket transformé en dépenses", fontSize = 12.sp, color = Muted)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onReview,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Outlined.Insights, contentDescription = null, tint = Emerald, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Bilan", color = Emerald, fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = onTicket,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald)
                ) {
                    Icon(Icons.Outlined.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Ticket", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

/** Choix de la source du ticket. */
@Composable
fun TicketSourceDialog(onCamera: () -> Unit, onGallery: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ticket ou facture") },
        text = { Text("La photo sera envoyée à Claude, qui la transformera en dépenses pour Mizan.") },
        confirmButton = {
            TextButton(onClick = onCamera) {
                Icon(Icons.Outlined.CameraAlt, contentDescription = null, tint = Emerald)
                Spacer(Modifier.width(6.dp))
                Text("Photo", color = Emerald)
            }
        },
        dismissButton = {
            TextButton(onClick = onGallery) {
                Icon(Icons.Outlined.PhotoLibrary, contentDescription = null, tint = Emerald)
                Spacer(Modifier.width(6.dp))
                Text("Galerie", color = Emerald)
            }
        }
    )
}

/** Confirmation en une étape quand une réponse de Claude arrive (partage ou presse-papiers). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportSheet(expenses: List<Expense>, onImport: () -> Unit, onDismiss: () -> Unit) {
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val n = expenses.size
            Text(
                "Réponse de Claude : $n transaction${if (n > 1) "s" else ""}",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = Slate
            )
            Column(
                Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                expenses.forEach { TransactionRow(it) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(14.dp)
                ) { Text("Ignorer", color = Muted) }
                Button(
                    onClick = onImport,
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald)
                ) { Text("Ajouter", fontWeight = FontWeight.SemiBold) }
            }
        }
    }
}
