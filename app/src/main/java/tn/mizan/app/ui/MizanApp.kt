package tn.mizan.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import android.content.ActivityNotFoundException
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import androidx.compose.ui.platform.LocalWindowInfo
import java.io.File
import tn.mizan.app.data.Expense
import tn.mizan.app.ui.components.ClaudeBusyDialog
import tn.mizan.app.ui.components.ImportSheet
import tn.mizan.app.ui.components.ReviewSheet
import tn.mizan.app.claude.ClaudeApi
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import tn.mizan.app.ui.components.TicketSourceDialog
import tn.mizan.app.util.MizanCsv
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import tn.mizan.app.ui.screens.AddExpenseScreen
import tn.mizan.app.ui.screens.AnimatedSplash
import tn.mizan.app.ui.screens.BudgetsScreen
import tn.mizan.app.claude.ClaudeBridge
import tn.mizan.app.ui.screens.HomeScreen
import tn.mizan.app.ui.screens.ProfileScreen
import tn.mizan.app.ui.screens.TransactionsScreen
import tn.mizan.app.ui.theme.CardWhite
import tn.mizan.app.ui.theme.Cream
import tn.mizan.app.ui.theme.Emerald
import tn.mizan.app.ui.theme.Line
import tn.mizan.app.ui.theme.Muted

object Routes {
    const val HOME = "home"
    const val TRANSACTIONS = "transactions"
    const val ADD = "add"
    const val BUDGETS = "budgets"
    const val PROFILE = "profile"
}

/**
 * @param sharedText texte reçu par partage (réponse de Claude, fichier CSV)
 * @param shareRequest augmente à chaque partage reçu : on propose alors l'import.
 */
@Composable
fun MizanApp(sharedText: String? = null, shareRequest: Int = 0, vm: MizanViewModel = viewModel()) {
    var showSplash by rememberSaveable { mutableStateOf(shareRequest == 0) }
    val context = LocalContext.current
    if (showSplash) {
        AnimatedSplash(onFinished = { showSplash = false })
        return
    }

    val nav = rememberNavController()
    val state by vm.state.collectAsStateWithLifecycle()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val showBar = route != Routes.ADD

    // --- Échanges avec Claude ---
    var pendingImport by remember { mutableStateOf<List<Expense>?>(null) }
    var showTicketChoice by remember { mutableStateOf(false) }
    var cameraFile by remember { mutableStateOf<File?>(null) }
    val windowInfo = LocalWindowInfo.current
    val clipboard = remember { context.getSystemService(ClipboardManager::class.java) }
    val prefs = remember { context.getSharedPreferences("mizan_prefs", Context.MODE_PRIVATE) }

    fun offerImport(text: String, fromShare: Boolean) {
        val result = MizanCsv.parse(text)
        if (result.expenses.isNotEmpty()) {
            pendingImport = result.expenses
        } else if (fromShare) {
            Toast.makeText(context, "Aucune dépense reconnue dans ce texte.", Toast.LENGTH_SHORT).show()
        }
    }

    // Réponse de Claude partagée vers Mizan
    LaunchedEffect(shareRequest) {
        if (shareRequest > 0) offerImport(sharedText.orEmpty(), fromShare = true)
    }

    // Réponse de Claude copiée : détectée au retour dans Mizan (une seule lecture par nouvelle copie)
    LaunchedEffect(windowInfo.isWindowFocused) {
        if (!windowInfo.isWindowFocused || pendingImport != null || clipboard == null) return@LaunchedEffect
        val description = clipboard.primaryClipDescription ?: return@LaunchedEffect
        val stamp = description.timestamp
        if (stamp == prefs.getLong("last_clip", 0L) || !description.hasMimeType("text/*")) return@LaunchedEffect
        prefs.edit().putLong("last_clip", stamp).apply()
        val text = clipboard.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString() ?: return@LaunchedEffect
        if (MizanCsv.looksLikeMizan(text)) offerImport(text, fromShare = false)
    }

    // Claude directement dans Mizan (si une clé est enregistrée), sinon via l'app Claude
    val claude by vm.claude.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var busyMessage by remember { mutableStateOf<String?>(null) }
    var busyJob by remember { mutableStateOf<Job?>(null) }
    var reviewText by remember { mutableStateOf<String?>(null) }

    fun toast(message: String) = Toast.makeText(context, message, Toast.LENGTH_LONG).show()

    fun runClaude(message: String, block: suspend () -> Unit) {
        busyMessage = message
        busyJob = scope.launch {
            try {
                block()
            } finally {
                busyMessage = null
            }
        }
    }

    fun claudeReview() {
        val creds = vm.claudeCredentials()
        if (creds == null) {
            ClaudeBridge.shareText(context, ClaudeBridge.monthReport(state))
            return
        }
        runClaude("Claude prépare ton bilan…") {
            ClaudeApi.review(creds.first, creds.second, state).fold(
                onSuccess = { reviewText = it },
                onFailure = { toast(it.message ?: "Erreur de Claude.") }
            )
        }
    }

    fun handleTicket(file: File) {
        val creds = vm.claudeCredentials()
        if (creds == null) {
            ClaudeBridge.shareTicket(context, file)
            return
        }
        runClaude("Claude lit ton ticket…") {
            ClaudeApi.readTicket(creds.first, creds.second, file).fold(
                onSuccess = { answer ->
                    val result = MizanCsv.parse(answer)
                    if (result.expenses.isNotEmpty()) pendingImport = result.expenses
                    else toast("Claude n’a pas trouvé de dépense sur cette photo. Essaie une photo plus nette.")
                },
                onFailure = { toast(it.message ?: "Erreur de Claude.") }
            )
        }
    }

    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val file = cameraFile
        if (ok && file != null && file.length() > 0) handleTicket(file)
    }
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            val file = ClaudeBridge.copyToTicket(context, uri)
            if (file != null) handleTicket(file)
            else Toast.makeText(context, "Impossible de lire cette image.", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        containerColor = Cream,
        bottomBar = {
            if (showBar) {
                MizanBottomBar(
                    current = route,
                    onSelect = { target ->
                        nav.navigate(target) {
                            popUpTo(Routes.HOME) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onAdd = { nav.navigate(Routes.ADD) }
                )
            }
        }
    ) { padding ->
        NavHost(nav, startDestination = Routes.HOME, modifier = Modifier.padding(padding)) {
            composable(Routes.HOME) {
                HomeScreen(
                    state = state,
                    onSeeAll = { nav.navigate(Routes.TRANSACTIONS) },
                    claudeDirect = claude.configured,
                    onClaudeReview = { claudeReview() },
                    onClaudeTicket = { showTicketChoice = true }
                )
            }
            composable(Routes.TRANSACTIONS) {
                TransactionsScreen(state, onDelete = vm::delete)
            }
            composable(Routes.ADD) {
                AddExpenseScreen(
                    onBack = { nav.popBackStack() },
                    onSave = { expense ->
                        vm.add(expense)
                        nav.popBackStack()
                    }
                )
            }
            composable(Routes.BUDGETS) { BudgetsScreen(state) }
            composable(Routes.PROFILE) {
                ProfileScreen(
                    state = state,
                    claude = claude,
                    onSaveClaude = vm::saveClaude,
                    onClearClaudeKey = vm::clearClaudeKey,
                    onExportCsv = { vm.withAllExpenses { ClaudeBridge.shareCsvFile(context, it) } }
                )
            }
        }
    }

    if (showTicketChoice) {
        TicketSourceDialog(
            onCamera = {
                showTicketChoice = false
                val file = ClaudeBridge.newTicketFile(context)
                cameraFile = file
                try {
                    takePicture.launch(ClaudeBridge.uriFor(context, file))
                } catch (_: ActivityNotFoundException) {
                    Toast.makeText(context, "Aucun appareil photo disponible.", Toast.LENGTH_SHORT).show()
                }
            },
            onGallery = {
                showTicketChoice = false
                pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            onDismiss = { showTicketChoice = false }
        )
    }

    busyMessage?.let { message ->
        ClaudeBusyDialog(message, onCancel = { busyJob?.cancel() })
    }

    reviewText?.let { text ->
        ReviewSheet(text, onDismiss = { reviewText = null })
    }

    pendingImport?.let { expenses ->
        ImportSheet(
            expenses = expenses,
            onImport = {
                pendingImport = null
                clearClipboard(clipboard)
                vm.importAll(expenses) { n ->
                    Toast.makeText(
                        context,
                        "$n transaction${if (n > 1) "s" else ""} ajoutée${if (n > 1) "s" else ""}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            },
            onDismiss = { pendingImport = null }
        )
    }
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

@Composable
private fun MizanBottomBar(
    current: String?,
    onSelect: (String) -> Unit,
    onAdd: () -> Unit
) {
    val left = listOf(
        Tab(Routes.HOME, "Accueil", Icons.Outlined.Home),
        Tab(Routes.TRANSACTIONS, "Transactions", Icons.AutoMirrored.Outlined.List)
    )
    val right = listOf(
        Tab(Routes.BUDGETS, "Budgets", Icons.Outlined.PieChart),
        Tab(Routes.PROFILE, "Profil", Icons.Outlined.Person)
    )
    Surface(color = CardWhite) {
        Column {
            HorizontalDivider(color = Line)
            Row(
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                left.forEach { TabItem(it, current == it.route, onSelect) }
                // Bouton + : ouvre le formulaire
                Box(
                    Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Emerald)
                        .clickable(onClick = onAdd)
                        .semantics { contentDescription = "Ajouter une dépense" },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, tint = Color.White)
                }
                right.forEach { TabItem(it, current == it.route, onSelect) }
            }
        }
    }
}

@Composable
private fun TabItem(tab: Tab, selected: Boolean, onSelect: (String) -> Unit) {
    val color = if (selected) Emerald else Muted
    Column(
        Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onSelect(tab.route) }
            .widthIn(min = 56.dp)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(tab.icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
        Text(
            tab.label,
            color = color,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1
        )
    }
}

/** Vide le presse-papiers après un import, pour ne pas proposer deux fois la même réponse. */
private fun clearClipboard(clipboard: ClipboardManager?) {
    if (clipboard == null) return
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) clipboard.clearPrimaryClip()
    else clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
}
