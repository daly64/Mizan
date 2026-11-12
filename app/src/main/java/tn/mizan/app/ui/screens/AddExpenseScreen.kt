package tn.mizan.app.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import kotlinx.coroutines.launch
import tn.mizan.app.data.TicketParser
import tn.mizan.app.data.MiniIA
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.runtime.LaunchedEffect
import tn.mizan.app.data.Tickets
import java.io.File
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
fun AddExpenseScreen(ia: MiniIA.Model, onBack: () -> Unit, onSave: (Expense) -> Unit) {
    var isIncome by rememberSaveable { mutableStateOf(false) }
    var amountText by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf(Category.COURSES) }
    var note by rememberSaveable { mutableStateOf("") }
    var date by remember { mutableStateOf(LocalDate.now()) }
    var showPicker by remember { mutableStateOf(false) }

    // --- Mini IA : devine la catégorie depuis la note, et comprend la saisie rapide ---
    var categoryTouched by rememberSaveable { mutableStateOf(false) } // l'utilisateur a choisi lui-même
    var quickText by rememberSaveable { mutableStateOf("") }
    var quickMessage by rememberSaveable { mutableStateOf<String?>(null) }
    val guess = remember(note, ia, isIncome) { if (isIncome || note.isBlank()) null else ia.guess(note) }
    LaunchedEffect(guess) {
        if (!categoryTouched && guess != null) category = guess.category
    }

    // --- Scan de ticket ---
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var scanning by remember { mutableStateOf(false) }
    var scanMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var scannedMerchant by rememberSaveable { mutableStateOf<String?>(null) }
    var amountFromScan by rememberSaveable { mutableStateOf(false) }

    fun readTicket(uri: Uri, temp: File?) = scope.launch {
        scanning = true
        val lines = Tickets.readText(context, uri)
        temp?.delete() // la photo n'est jamais gardée
        scanning = false
        if (lines == null) {
            Toast.makeText(context, "Impossible de lire cette photo", Toast.LENGTH_SHORT).show()
            return@launch
        }
        val result = TicketParser.parse(lines) { Tickets.learnedCategory(context, it) }
        if (result.isEmpty) {
            scanMessage = null
            Toast.makeText(context, "Aucun montant trouvé : reprends la photo plus nette et bien éclairée", Toast.LENGTH_LONG).show()
            return@launch
        }
        isIncome = false
        result.amountMillimes?.let { amountText = "%d.%03d".format(it / 1000, it % 1000); amountFromScan = true }
        result.date?.let { date = it }
        result.category?.let { category = it; categoryTouched = true }
        result.merchant?.let { if (note.isBlank()) note = it.take(60) }
        scannedMerchant = result.merchant
        val missing = buildList {
            if (result.amountMillimes == null) add("le montant")
            if (result.date == null) add("la date")
            if (result.category == null) add("la catégorie")
        }
        scanMessage = if (missing.isEmpty()) "Rempli depuis le ticket. Vérifie le montant et la date avant d’enregistrer."
        else "Rempli depuis le ticket. Non trouvé : ${missing.joinToString(", ")}. Complète avant d’enregistrer."
    }

    var photoFile by remember { mutableStateOf<File?>(null) }
    val takePhoto = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val file = photoFile
        if (ok && file != null) readTicket(FileProvider.getUriForFile(context, "${context.packageName}.fichiers", file), file)
        else file?.delete()
    }
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) readTicket(uri, null)
    }
    fun openCamera() {
        val dir = File(context.cacheDir, "tickets").apply { mkdirs() }
        val file = File(dir, "ticket.jpg").also { photoFile = it }
        runCatching { takePhoto.launch(FileProvider.getUriForFile(context, "${context.packageName}.fichiers", file)) }
            .onFailure { Toast.makeText(context, "Aucune application appareil photo trouvée", Toast.LENGTH_SHORT).show() }
    }

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

            // Saisie rapide : « taxi 7.5 » → montant, note et catégorie remplis par la mini IA
            AnimatedVisibility(!isIncome, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                fun applyQuick() {
                    if (quickText.isBlank()) return
                    val q = MiniIA.quick(quickText, ia)
                    q.amountMillimes?.let { amountText = "%d.%03d".format(it / 1000, it % 1000); amountFromScan = false }
                    if (q.note.isNotBlank()) note = q.note.take(60)
                    q.guess?.let { category = it.category; categoryTouched = false }
                    quickMessage = buildList {
                        add(if (q.amountMillimes != null) "montant ${Money.format(q.amountMillimes)}" else "montant non trouvé")
                        q.guess?.let { add("${it.category.label} (${MiniIA.confidenceLabel(it)})") }
                            ?: add("catégorie à choisir")
                    }.joinToString(" · ")
                    quickText = ""
                }
                MizanCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = Gold, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Saisie rapide", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Slate)
                                Text("Écris comme tu parles, la mini IA remplit le reste", fontSize = 12.sp, color = Muted)
                            }
                        }
                        OutlinedTextField(
                            value = quickText,
                            onValueChange = { quickText = it.take(80) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            placeholder = { Text("taxi 7.5 · kahwa 2.8 · steg 85,500", color = Muted) },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { applyQuick() }),
                            trailingIcon = {
                                TextButton(onClick = { applyQuick() }, enabled = quickText.isNotBlank()) {
                                    Text("Remplir", color = if (quickText.isNotBlank()) Emerald else Line, fontWeight = FontWeight.SemiBold)
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = CardWhite,
                                unfocusedContainerColor = CardWhite,
                                unfocusedBorderColor = Line,
                                focusedBorderColor = Emerald
                            )
                        )
                        // Aperçu en direct : ce que la mini IA comprend pendant que tu écris
                        val preview = remember(quickText, ia) { if (quickText.isBlank()) null else MiniIA.quick(quickText, ia) }
                        AnimatedVisibility(preview != null, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                            val p = preview
                            if (p != null) Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                PreviewChip(p.amountMillimes?.let { Money.format(it) + " DT" } ?: "montant ?", p.amountMillimes != null)
                                PreviewChip(p.note.ifBlank { "note ?" }, p.note.isNotBlank())
                                PreviewChip(p.guess?.let { "${it.category.label} ${MiniIA.confidenceLabel(it)}" } ?: "catégorie ?", p.guess != null)
                            }
                        }
                        AnimatedVisibility(quickMessage != null && quickText.isBlank()) {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Mint)
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Outlined.TaskAlt, contentDescription = null, tint = Emerald, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Compris : ${quickMessage ?: ""}. Vérifie avant d’enregistrer.", fontSize = 12.sp, lineHeight = 17.sp, color = Slate)
                            }
                        }
                    }
                }
            }

            // Scanner un ticket : remplit le montant, la date, le magasin et la catégorie
            AnimatedVisibility(!isIncome, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                MizanCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.DocumentScanner, contentDescription = null, tint = Emerald, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Scanner un ticket", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Slate)
                                Text("Lu sur ton téléphone, la photo n’est pas gardée", fontSize = 12.sp, color = Muted)
                            }
                            if (scanning) CircularProgressIndicator(color = Emerald, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { openCamera() },
                                enabled = !scanning,
                                modifier = Modifier.weight(1f).height(44.dp),
                                shape = RoundedCornerShape(50),
                                colors = ButtonDefaults.buttonColors(containerColor = Emerald, contentColor = Color.White)
                            ) {
                                Icon(Icons.Outlined.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Photo", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            }
                            OutlinedButton(
                                onClick = { pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                                enabled = !scanning,
                                modifier = Modifier.weight(1f).height(44.dp),
                                shape = RoundedCornerShape(50),
                                border = BorderStroke(1.dp, Line)
                            ) {
                                Icon(Icons.Outlined.PhotoLibrary, contentDescription = null, tint = Emerald, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Galerie", fontSize = 14.sp, color = Slate)
                            }
                        }
                        AnimatedVisibility(scanMessage != null) {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Mint)
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Outlined.TaskAlt, contentDescription = null, tint = Emerald, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(scanMessage ?: "", fontSize = 12.sp, lineHeight = 17.sp, color = Slate)
                            }
                        }
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
                            if (amountPattern.matches(cleaned)) { amountText = cleaned; amountFromScan = false }
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
                Box(Modifier.width(180.dp).height(if (amountFromScan) 4.dp else 2.dp).background(Gold))
                if (amountFromScan) Text("Lu sur le ticket : vérifie-le", fontSize = 12.sp, color = Muted, modifier = Modifier.padding(top = 4.dp))
            }

            Label("Catégorie")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Category.entries.forEach { c ->
                    CategoryTile(c, Modifier.weight(1f), selected = c == category, onClick = { category = c; categoryTouched = true })
                }
            }
            // Ce que pense la mini IA de la note
            guess?.let { g ->
                if (!categoryTouched && g.category == category) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = Gold, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Devinée par la mini IA d’après la note · ${MiniIA.confidenceLabel(g)}", fontSize = 12.sp, color = Muted)
                    }
                } else if (g.category != category) {
                    Row(
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Mint)
                            .clickable { category = g.category; categoryTouched = false }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = Gold, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("La mini IA propose : ${g.category.label} (${MiniIA.confidenceLabel(g)})", fontSize = 12.sp, color = Emerald)
                    }
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
                // Le magasin scanné retient la catégorie choisie, pour les prochains tickets.
                scannedMerchant?.let { Tickets.learn(context, it, category) }
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

/** Une étiquette de l'aperçu de saisie rapide : verte si la mini IA a compris, grise sinon. */
@Composable
private fun PreviewChip(text: String, ok: Boolean) {
    Text(
        text,
        fontSize = 12.sp,
        maxLines = 1,
        color = if (ok) Emerald else Muted,
        fontWeight = if (ok) FontWeight.SemiBold else FontWeight.Normal,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (ok) Mint else Color.Transparent)
            .border(1.dp, if (ok) Mint else Line, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    )
}
