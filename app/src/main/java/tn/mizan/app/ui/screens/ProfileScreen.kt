package tn.mizan.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tn.mizan.app.ui.AiSettings
import tn.mizan.app.ui.MonthState
import tn.mizan.app.ui.components.IconCircle
import tn.mizan.app.ui.components.MizanCard
import tn.mizan.app.ui.components.MoneyText
import tn.mizan.app.ui.theme.Danger
import tn.mizan.app.ui.theme.Emerald
import tn.mizan.app.ui.theme.Line
import tn.mizan.app.ui.theme.Muted
import tn.mizan.app.ui.theme.Slate

@Composable
fun ProfileScreen(
    state: MonthState,
    ai: AiSettings,
    onSaveAi: (key: String?, model: String) -> Unit,
    onClearAiKey: () -> Unit
) {
    var reminder by remember { mutableStateOf(true) }
    var showAiDialog by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Profil", fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = Slate)

        MizanCard(Modifier.fillMaxWidth()) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(52.dp).background(Emerald, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("Mon profil", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Slate)
                    Text("Données sur ce téléphone", fontSize = 12.sp, color = Muted)
                }
            }
        }

        Group("Préférences") {
            SettingRow(Icons.Outlined.Payments, "Devise") { Value("Dinar tunisien") }
            Divider()
            SettingRow(Icons.Outlined.CreditCard, "Budget mensuel") {
                MoneyText(state.budgetTotal, 13.sp, Muted)
            }
            Divider()
            SettingRow(Icons.Outlined.CalendarMonth, "Début du mois") { Value("Le 1er") }
            Divider()
            SettingRow(Icons.Outlined.Notifications, "Rappel quotidien", chevron = false) {
                Switch(
                    checked = reminder,
                    onCheckedChange = { reminder = it },
                    colors = SwitchDefaults.colors(checkedTrackColor = Emerald)
                )
            }
            Divider()
            SettingRow(Icons.Outlined.LightMode, "Thème") { Value("Clair") }
        }

        Group("Intelligence artificielle") {
            SettingRow(Icons.Outlined.AutoAwesome, "Clé API OpenAI", onClick = { showAiDialog = true }) {
                Value(if (ai.configured) "Configurée ${ai.keyHint}" else "Non configurée")
            }
            Divider()
            SettingRow(Icons.Outlined.Memory, "Modèle", onClick = { showAiDialog = true }) { Value(ai.model) }
        }
        Text(
            "L’IA sert au bilan du mois (écran Budgets). Seuls les totaux sont envoyés, jamais tes notes.",
            fontSize = 12.sp,
            color = Muted
        )

        Group("Données") {
            SettingRow(Icons.Outlined.FileDownload, "Exporter en CSV") { Value("Bientôt") }
            Divider()
            SettingRow(Icons.Outlined.CloudUpload, "Sauvegarde") { Value("Bientôt") }
            Divider()
            SettingRow(Icons.Outlined.Info, "À propos de Mizan") { Value("v0.3") }
        }
    }

    if (showAiDialog) {
        AiKeyDialog(
            ai = ai,
            onSave = { key, model -> onSaveAi(key, model); showAiDialog = false },
            onClear = { onClearAiKey(); showAiDialog = false },
            onDismiss = { showAiDialog = false }
        )
    }
}

@Composable
private fun AiKeyDialog(
    ai: AiSettings,
    onSave: (key: String?, model: String) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    var key by remember { mutableStateOf("") }
    var model by remember { mutableStateOf(ai.model) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Clé API OpenAI") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (!ai.storageAvailable) {
                    Text(
                        "Le stockage chiffré n’est pas disponible sur ce téléphone : la clé ne peut pas être enregistrée.",
                        fontSize = 13.sp,
                        color = Danger
                    )
                }
                Text(
                    "Colle ta clé ici. Elle est chiffrée sur ce téléphone et n’est envoyée qu’à OpenAI. " +
                        "Ne la mets jamais dans le code ni sur GitHub.",
                    fontSize = 13.sp,
                    color = Muted
                )
                OutlinedTextField(
                    value = key,
                    onValueChange = { key = it.trim() },
                    singleLine = true,
                    label = { Text(if (ai.configured) "Nouvelle clé (laisser vide pour garder ${ai.keyHint})" else "Clé (sk-…)") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it.trim() },
                    singleLine = true,
                    label = { Text("Modèle") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(key.ifBlank { null }, model) },
                enabled = ai.storageAvailable && (key.isNotBlank() || ai.configured)
            ) { Text("Enregistrer") }
        },
        dismissButton = {
            Row {
                if (ai.configured) {
                    TextButton(onClick = onClear) { Text("Supprimer la clé", color = Danger) }
                }
                TextButton(onClick = onDismiss) { Text("Annuler") }
            }
        }
    )
}

@Composable
private fun Group(title: String, content: @Composable () -> Unit) {
    Text(title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Muted)
    MizanCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 2.dp)) { content() }
    }
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    label: String,
    chevron: Boolean = true,
    onClick: (() -> Unit)? = null,
    trailing: @Composable () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconCircle(icon, 32.dp)
        Spacer(Modifier.width(12.dp))
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Slate, modifier = Modifier.weight(1f))
        trailing()
        if (chevron) {
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = Muted, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun Value(text: String) {
    Text(text, fontSize = 13.sp, color = Muted)
}

@Composable
private fun Divider() {
    HorizontalDivider(color = Line)
}
