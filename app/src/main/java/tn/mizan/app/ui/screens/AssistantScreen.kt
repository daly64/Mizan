package tn.mizan.app.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tn.mizan.app.agent.AgentViewModel
import tn.mizan.app.agent.ChatItem
import tn.mizan.app.agent.ModelOption
import tn.mizan.app.agent.ModelState
import tn.mizan.app.agent.ModelStore
import tn.mizan.app.agent.Who
import tn.mizan.app.ui.components.MizanCard
import tn.mizan.app.ui.theme.CardWhite
import tn.mizan.app.ui.theme.Danger
import tn.mizan.app.ui.theme.Emerald
import tn.mizan.app.ui.theme.Line
import tn.mizan.app.ui.theme.Mint
import tn.mizan.app.ui.theme.Muted
import tn.mizan.app.ui.theme.Slate

private val SUGGESTIONS = listOf(
    "Fais-moi le bilan du mois",
    "Comment économiser ce mois-ci ?",
    "20 dinars taxi",
    "Note : payer la STEG avant le 15"
)

@Composable
fun AssistantScreen(vm: AgentViewModel, onBack: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    var menuOpen by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val hasModel = state.model == ModelState.Ready ||
        state.model == ModelState.Loading ||
        (state.model is ModelState.Failed && ModelStore.hasModel(LocalContext.current))

    Column(
        Modifier
            .fillMaxSize()
            .imePadding()
    ) {
        // Barre du haut
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = Slate)
            }
            Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = Emerald, modifier = Modifier.size(20.dp))
            Text(
                "  Assistant",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Slate,
                modifier = Modifier.weight(1f)
            )
            if (hasModel) {
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Outlined.MoreVert, contentDescription = "Options", tint = Slate)
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Nouvelle discussion") },
                            onClick = { menuOpen = false; vm.newChat() }
                        )
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Accélération GPU (essai)", modifier = Modifier.weight(1f))
                                    Switch(checked = state.gpu, onCheckedChange = null)
                                }
                            },
                            onClick = { menuOpen = false; vm.setGpu(!state.gpu) }
                        )
                        DropdownMenuItem(
                            text = { Text("Changer / supprimer le modèle", color = Danger) },
                            onClick = { menuOpen = false; confirmDelete = true }
                        )
                    }
                }
            }
        }

        when (val model = state.model) {
            ModelState.Missing -> SetupPanel(vm, error = null)
            is ModelState.Failed ->
                if (ModelStore.hasModel(LocalContext.current)) ErrorPanel(model.message, vm::retry)
                else SetupPanel(vm, error = model.message)
            is ModelState.Importing -> ImportingPanel(model.progress)
            ModelState.Loading -> CenterMessage(
                "Chargement du modèle…",
                "La première fois, ça peut prendre jusqu'à une minute."
            )
            ModelState.Ready -> ChatPanel(state.messages, state.busy, vm::send, vm::stop)
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Supprimer le modèle ?") },
            text = { Text("Cela libère ${ModelStore.sizeLabel(LocalContext.current)}. Vos dépenses et notes restent. Vous pourrez ensuite importer un autre modèle (léger ou complet).") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; vm.deleteModel() }) { Text("Supprimer", color = Danger) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Annuler") } }
        )
    }
}

// --- Installation du modèle ---

@Composable
private fun SetupPanel(vm: AgentViewModel, error: String?) {
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) vm.importModel(uri)
    }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            "Un assistant qui tourne entièrement sur votre téléphone : il enregistre vos dépenses et vos notes, fait les calculs et prépare le bilan du mois. Rien ne quitte le téléphone.",
            color = Slate,
            fontSize = 14.sp
        )
        Text("1. Choisissez un modèle et téléchargez-le", fontWeight = FontWeight.SemiBold, color = Slate, fontSize = 15.sp)
        ModelOption.entries.forEach { option ->
            MizanCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(option.title, fontWeight = FontWeight.SemiBold, color = Slate, fontSize = 15.sp, modifier = Modifier.weight(1f))
                        Text(
                            option.size,
                            color = Emerald,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Mint)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Text(option.description, color = Muted, fontSize = 13.sp)
                    OutlinedButton(onClick = {
                        try {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(option.url)))
                        } catch (_: ActivityNotFoundException) {
                        }
                    }) { Text("Télécharger (${option.size})", color = Emerald) }
                }
            }
        }
        StepCard(
            "2",
            "Importer le fichier",
            "Une fois le téléchargement fini, choisissez le fichier .litertlm dans Téléchargements. Mizan en garde une copie : vous pourrez ensuite supprimer le fichier téléchargé."
        ) {
            Button(
                onClick = { picker.launch(arrayOf("*/*")) },
                colors = ButtonDefaults.buttonColors(containerColor = Emerald)
            ) { Text("Choisir le fichier", color = Color.White) }
        }
        if (error != null) Text(error, color = Danger, fontSize = 13.sp)
    }
}

@Composable
private fun StepCard(number: String, title: String, body: String, action: @Composable () -> Unit) {
    MizanCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(26.dp).background(Mint, CircleShape),
                    contentAlignment = Alignment.Center
                ) { Text(number, color = Emerald, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                Text("  $title", fontWeight = FontWeight.SemiBold, color = Slate, fontSize = 15.sp)
            }
            Text(body, color = Muted, fontSize = 13.sp)
            action()
        }
    }
}

@Composable
private fun ImportingPanel(progress: Float) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Import du modèle…", color = Slate, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(12.dp))
        if (progress >= 0f) {
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth(), color = Emerald, trackColor = Mint)
            Text("${(progress * 100).toInt()} %", color = Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
        } else {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = Emerald, trackColor = Mint)
        }
        Text("Gardez Mizan ouvert pendant la copie.", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun CenterMessage(title: String, subtitle: String) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(color = Emerald)
        Spacer(Modifier.height(16.dp))
        Text(title, color = Slate, fontWeight = FontWeight.SemiBold)
        Text(subtitle, color = Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun ErrorPanel(message: String, onRetry: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(message, color = Danger, fontSize = 14.sp)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = Emerald)) {
            Text("Réessayer", color = Color.White)
        }
    }
}

// --- Discussion ---

@Composable
private fun ChatPanel(
    messages: List<ChatItem>,
    busy: Boolean,
    onSend: (String) -> Unit,
    onStop: () -> Unit
) {
    var input by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()
    val last = messages.lastOrNull()
    LaunchedEffect(messages.size, last?.text?.length) {
        if (messages.isNotEmpty()) listState.scrollToItem(messages.lastIndex)
    }
    val send = {
        if (input.isNotBlank()) {
            onSend(input)
            input = ""
        }
    }

    Column(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth(),
            state = listState,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (messages.isEmpty()) {
                item {
                    Text(
                        "Écrivez comme vous parlez : « 35 dinars courses », « combien j'ai dépensé en transport ? », « fais le bilan ».",
                        color = Muted,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
            }
            items(messages, key = { it.id }) { Bubble(it) }
            if (busy && (last == null || last.who != Who.AGENT)) {
                item { Text("L'assistant réfléchit…", color = Muted, fontSize = 13.sp) }
            }
        }

        if (messages.isEmpty()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SUGGESTIONS.forEach { s ->
                    Text(
                        s,
                        color = Emerald,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Mint)
                            .clickable { onSend(s) }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .background(CardWhite)
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                placeholder = { Text("Message…") },
                modifier = Modifier.weight(1f),
                maxLines = 4,
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Emerald,
                    unfocusedBorderColor = Line,
                    focusedTextColor = Slate,
                    unfocusedTextColor = Slate
                )
            )
            Spacer(Modifier.size(8.dp))
            Box(
                Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (busy) Danger else Emerald)
                    .clickable { if (busy) onStop() else send() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (busy) Icons.Filled.Stop else Icons.AutoMirrored.Filled.Send,
                    contentDescription = if (busy) "Arrêter" else "Envoyer",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
private fun Bubble(item: ChatItem) {
    when (item.who) {
        Who.ACTION -> Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = Emerald, modifier = Modifier.size(14.dp))
            Text(" ${item.text}", color = Muted, fontSize = 12.sp)
        }
        Who.USER -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Text(
                item.text,
                color = Color.White,
                fontSize = 14.sp,
                modifier = Modifier
                    .widthIn(max = 300.dp)
                    .clip(RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp))
                    .background(Emerald)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            )
        }
        Who.AGENT -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
            Text(
                item.text.replace("**", ""),
                color = Slate,
                fontSize = 14.sp,
                modifier = Modifier
                    .widthIn(max = 320.dp)
                    .clip(RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp))
                    .background(CardWhite)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            )
        }
    }
}
