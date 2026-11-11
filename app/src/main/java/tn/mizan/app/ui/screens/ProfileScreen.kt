package tn.mizan.app.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.InstallMobile
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import tn.mizan.app.data.Updates
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableLongStateOf
import tn.mizan.app.data.Backup
import tn.mizan.app.ui.theme.Danger
import tn.mizan.app.ui.theme.ThemeMode
import tn.mizan.app.ui.theme.ThemeSettings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tn.mizan.app.ui.MonthState
import tn.mizan.app.ui.components.IconCircle
import tn.mizan.app.ui.components.MizanCard
import tn.mizan.app.ui.components.MoneyText
import tn.mizan.app.ui.theme.Emerald
import tn.mizan.app.ui.theme.Line
import tn.mizan.app.ui.theme.Muted
import tn.mizan.app.ui.theme.Slate

@Composable
fun ProfileScreen(
    state: MonthState,
    onRestore: (Backup.Content, () -> Unit) -> Unit,
    onBackupNow: ((Boolean) -> Unit) -> Unit,
    onCheckUpdates: () -> Unit
) {
    val context = LocalContext.current
    var showTheme by remember { mutableStateOf(false) }
    var lastSaved by remember { mutableLongStateOf(Backup.lastSaved(context)) }
    var folderName by remember { mutableStateOf(Backup.folderName(context)) }
    var toRestore by remember { mutableStateOf<Backup.Content?>(null) }

    fun toast(text: String) = Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
    fun saveNow() = onBackupNow { ok ->
        lastSaved = Backup.lastSaved(context)
        toast(if (ok) "Sauvegarde faite" else "Rien à sauvegarder pour l’instant")
    }

    val pickFolder = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            runCatching { Backup.setFolder(context, uri) }
                .onSuccess { folderName = Backup.folderName(context); saveNow() }
                .onFailure { toast("Impossible d’utiliser ce dossier") }
        }
    }
    val scope = rememberCoroutineScope()
    val pickApk = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            val file = Updates.copyLocalApk(context, uri)
            when {
                file == null -> toast("Impossible de lire ce fichier")
                !Updates.canInstall(context) -> {
                    toast("Autorise AL MIZAN à installer des applications, puis recommence")
                    Updates.openInstallPermission(context)
                }
                else -> Updates.install(context, file)
            }
        }
    }
    val pickBackup = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val content = Backup.read(context, uri)
            if (content == null) toast("Ce fichier n’est pas une sauvegarde AL MIZAN") else toRestore = content
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Réglages", fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = Slate)

        Group {
            SettingRow(Icons.Outlined.DarkMode, "Thème", onClick = { showTheme = true }) {
                Value(ThemeSettings.mode.label)
            }
        }

        Group {
            SettingRow(Icons.Outlined.Backup, "Sauvegarder maintenant", onClick = { saveNow() }) {
                Value(lastSavedLabel(lastSaved))
            }
            Divider()
            SettingRow(Icons.Outlined.Folder, "Dossier de sauvegarde", onClick = { pickFolder.launch(null) }) {
                Value(folderName ?: "Dans l’app")
            }
            Divider()
            SettingRow(Icons.Outlined.Restore, "Restaurer une sauvegarde", onClick = {
                pickBackup.launch(arrayOf("application/json", "application/octet-stream", "text/plain"))
            }) { }
        }
        Group {
            SettingRow(Icons.Outlined.SystemUpdate, "Rechercher une mise à jour", onClick = onCheckUpdates) {
                Value("v${Updates.currentVersionName}")
            }
            Divider()
            SettingRow(Icons.Outlined.InstallMobile, "Installer un APK du téléphone", onClick = {
                pickApk.launch(arrayOf("application/vnd.android.package-archive", "application/octet-stream"))
            }) { }
        }
        Text(
            "AL MIZAN se sauvegarde tout seul chaque fois que tu quittes l’app. " +
                "Choisis un dossier du téléphone (ex. Documents) pour garder tes sauvegardes même après une désinstallation.",
            fontSize = 12.sp,
            color = Muted
        )
        Text("AL MIZAN v${Updates.currentVersionName}", fontSize = 12.sp, color = Muted)
    }

    if (showTheme) {
        AlertDialog(
            onDismissRequest = { showTheme = false },
            title = { Text("Thème") },
            text = {
                Column {
                    ThemeMode.entries.forEach { mode ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    ThemeSettings.set(context, mode)
                                    showTheme = false
                                }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = ThemeSettings.mode == mode,
                                onClick = {
                                    ThemeSettings.set(context, mode)
                                    showTheme = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = Emerald)
                            )
                            Text(mode.label, fontSize = 15.sp, color = Slate)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showTheme = false }) { Text("Fermer", color = Emerald) } }
        )
    }

    toRestore?.let { content ->
        AlertDialog(
            onDismissRequest = { toRestore = null },
            title = { Text("Restaurer cette sauvegarde ?") },
            text = {
                Text(
                    "${content.expenses.size} transactions, sauvegardées ${lastSavedLabel(content.savedAt).lowercase()}. " +
                        "Tes données actuelles seront remplacées."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onRestore(content) { toast("Sauvegarde restaurée") }
                    toRestore = null
                }) { Text("Restaurer", color = Danger) }
            },
            dismissButton = { TextButton(onClick = { toRestore = null }) { Text("Annuler", color = Muted) } }
        )
    }
}

@Composable
private fun Group(content: @Composable () -> Unit) {
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

private fun lastSavedLabel(millis: Long): String {
    if (millis <= 0L) return "Pas encore"
    val time = java.time.Instant.ofEpochMilli(millis).atZone(java.time.ZoneId.systemDefault())
    val hour = time.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
    val date = time.toLocalDate()
    return when (date) {
        java.time.LocalDate.now() -> "Aujourd’hui à $hour"
        java.time.LocalDate.now().minusDays(1) -> "Hier à $hour"
        else -> date.format(java.time.format.DateTimeFormatter.ofPattern("d MMM", java.util.Locale.FRENCH)) + " à $hour"
    }
}
