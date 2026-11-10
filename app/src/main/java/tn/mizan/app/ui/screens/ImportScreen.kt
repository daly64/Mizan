package tn.mizan.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tn.mizan.app.data.Expense
import tn.mizan.app.ui.components.MizanCard
import tn.mizan.app.ui.components.TransactionRow
import tn.mizan.app.ui.theme.CardWhite
import tn.mizan.app.ui.theme.Danger
import tn.mizan.app.ui.theme.Emerald
import tn.mizan.app.ui.theme.Line
import tn.mizan.app.ui.theme.Muted
import tn.mizan.app.ui.theme.Slate
import tn.mizan.app.util.MizanCsv

/**
 * Import depuis Claude : on colle (ou on partage vers Mizan) la réponse de Claude au format Mizan,
 * on vérifie l'aperçu, puis on importe.
 */
@Composable
fun ImportScreen(
    initialText: String,
    onBack: () -> Unit,
    onAskClaude: () -> Unit,
    onImport: (List<Expense>) -> Unit
) {
    var text by rememberSaveable(initialText) { mutableStateOf(initialText) }
    val result = remember(text) { MizanCsv.parse(text) }
    val clipboard = LocalClipboardManager.current

    Column(
        Modifier
            .fillMaxSize()
            .imePadding()
            .padding(horizontal = 20.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = Slate)
            }
            Text(
                "Importer depuis Claude",
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
            MizanCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Comment faire", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Slate)
                    Text(
                        "1. Touche « Préparer la demande » : la consigne s’ouvre dans Claude.\n" +
                            "2. Ajoute ta photo de ticket, ta facture ou ta liste, puis envoie.\n" +
                            "3. Partage la réponse de Claude vers Mizan, ou copie-la et touche « Coller ».",
                        fontSize = 13.sp,
                        color = Muted,
                        lineHeight = 20.sp
                    )
                    OutlinedButton(onClick = onAskClaude, shape = RoundedCornerShape(14.dp)) {
                        Icon(Icons.AutoMirrored.Outlined.Send, contentDescription = null, tint = Emerald)
                        Spacer(Modifier.width(8.dp))
                        Text("Préparer la demande pour Claude", color = Emerald)
                    }
                }
            }

            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                minLines = 5,
                maxLines = 10,
                placeholder = { Text("${MizanCsv.HEADER}\n2026-11-10;depense;12.500;Transport;Especes;Taxi") },
                textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = Slate),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = CardWhite,
                    unfocusedContainerColor = CardWhite,
                    unfocusedBorderColor = Line,
                    focusedBorderColor = Emerald
                )
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { clipboard.getText()?.text?.let { text = it } }) {
                    Icon(Icons.Outlined.ContentPaste, contentDescription = null, tint = Emerald)
                    Spacer(Modifier.width(6.dp))
                    Text("Coller", color = Emerald)
                }
                if (text.isNotEmpty()) {
                    TextButton(onClick = { text = "" }) { Text("Effacer", color = Muted) }
                }
            }

            if (text.isNotBlank()) {
                val n = result.expenses.size
                Text(
                    buildString {
                        append(if (n == 0) "Aucune transaction reconnue" else "$n transaction${if (n > 1) "s" else ""} reconnue${if (n > 1) "s" else ""}")
                        if (result.ignored > 0) append(" · ${result.ignored} ligne${if (result.ignored > 1) "s" else ""} ignorée${if (result.ignored > 1) "s" else ""}")
                    },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (n == 0) Danger else Emerald
                )
                result.expenses.take(30).forEach { TransactionRow(it) }
                if (n > 30) Text("… et ${n - 30} autres", fontSize = 12.sp, color = Muted)
            }
        }

        Button(
            onClick = { onImport(result.expenses) },
            enabled = result.expenses.isNotEmpty(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Emerald)
        ) {
            val n = result.expenses.size
            Text(
                if (n == 0) "Importer" else "Importer $n transaction${if (n > 1) "s" else ""}",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
