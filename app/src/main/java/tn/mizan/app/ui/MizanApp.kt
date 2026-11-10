package tn.mizan.app.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import kotlinx.coroutines.launch
import tn.mizan.app.data.Category
import tn.mizan.app.data.Expense
import tn.mizan.app.ui.components.QuickConfirmSheet
import tn.mizan.app.ui.screens.AddExpenseScreen
import tn.mizan.app.ui.screens.AnimatedSplash
import tn.mizan.app.ui.screens.BudgetsScreen
import tn.mizan.app.ui.screens.HomeScreen
import tn.mizan.app.ui.screens.ProfileScreen
import tn.mizan.app.ui.screens.TransactionsScreen
import tn.mizan.app.ui.theme.CardWhite
import tn.mizan.app.ui.theme.Cream
import tn.mizan.app.ui.theme.Emerald
import tn.mizan.app.ui.theme.Line
import tn.mizan.app.ui.theme.Muted
import tn.mizan.app.util.Dates
import tn.mizan.app.util.Money
import tn.mizan.app.util.QuickEntry
import tn.mizan.app.util.QuickParser

object Routes {
    const val HOME = "home"
    const val TRANSACTIONS = "transactions"
    const val ADD = "add"
    const val BUDGETS = "budgets"
    const val PROFILE = "profile"
}

/**
 * @param quickRequest augmente à chaque ouverture par le raccourci « Dépense rapide » :
 * on saute alors le splash et on lance directement la dictée.
 */
@Composable
fun MizanApp(quickRequest: Int = 0, vm: MizanViewModel = viewModel()) {
    var showSplash by rememberSaveable { mutableStateOf(quickRequest == 0) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Saisie rapide (partagée entre l'accueil, le bouton + et le raccourci)
    var quickText by rememberSaveable { mutableStateOf("") }
    var preview by remember { mutableStateOf<QuickEntry?>(null) }
    var editPrefill by remember { mutableStateOf<QuickEntry?>(null) }

    val voiceLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (result.resultCode == Activity.RESULT_OK && !spoken.isNullOrBlank()) {
            quickText = spoken
            preview = QuickParser.parse(spoken)
        }
    }

    fun launchVoice() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fr-FR")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Ex. : 20 dinars taxi")
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
        }
        try {
            voiceLauncher.launch(intent)
        } catch (e: ActivityNotFoundException) {
            scope.launch { snackbar.showSnackbar("La reconnaissance vocale n’est pas disponible sur ce téléphone.") }
        }
    }

    LaunchedEffect(quickRequest) {
        if (quickRequest > 0) {
            showSplash = false
            launchVoice()
        }
    }

    if (showSplash) {
        AnimatedSplash(onFinished = { showSplash = false })
        return
    }

    val nav = rememberNavController()
    val state by vm.state.collectAsStateWithLifecycle()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val showBar = route != Routes.ADD

    fun saveQuick(entry: QuickEntry) {
        val amount = entry.amountMillimes ?: return
        val expense = Expense(
            amountMillimes = amount,
            category = entry.category ?: Category.COURSES,
            note = entry.note,
            dateMillis = Dates.toMillis(entry.date),
            payment = entry.payment,
            isIncome = entry.isIncome
        )
        preview = null
        quickText = ""
        vm.add(expense) { id ->
            scope.launch {
                val what = if (entry.isIncome) "Revenu ajouté" else "Dépense ajoutée"
                val result = snackbar.showSnackbar(
                    message = "$what : ${Money.format(amount)}",
                    actionLabel = "Annuler",
                    duration = SnackbarDuration.Short
                )
                if (result == SnackbarResult.ActionPerformed) vm.deleteById(id)
            }
        }
    }

    Scaffold(
        containerColor = Cream,
        snackbarHost = { SnackbarHost(snackbar) },
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
                    onAdd = {
                        editPrefill = null
                        nav.navigate(Routes.ADD)
                    },
                    onAddLongPress = { launchVoice() }
                )
            }
        }
    ) { padding ->
        NavHost(nav, startDestination = Routes.HOME, modifier = Modifier.padding(padding)) {
            composable(Routes.HOME) {
                HomeScreen(
                    state = state,
                    quickText = quickText,
                    onQuickTextChange = { quickText = it },
                    onQuickSubmit = { preview = QuickParser.parse(quickText) },
                    onMic = { launchVoice() },
                    onSeeAll = { nav.navigate(Routes.TRANSACTIONS) }
                )
            }
            composable(Routes.TRANSACTIONS) {
                TransactionsScreen(state, onDelete = vm::delete)
            }
            composable(Routes.ADD) {
                AddExpenseScreen(
                    prefill = editPrefill,
                    onBack = { nav.popBackStack() },
                    onSave = { expense ->
                        vm.add(expense)
                        if (editPrefill != null) quickText = ""
                        editPrefill = null
                        nav.popBackStack()
                    }
                )
            }
            composable(Routes.BUDGETS) { BudgetsScreen(state) }
            composable(Routes.PROFILE) { ProfileScreen(state) }
        }
    }

    preview?.let { entry ->
        QuickConfirmSheet(
            entry = entry,
            onChange = { preview = it },
            onConfirm = { saveQuick(entry) },
            onEdit = {
                editPrefill = entry
                preview = null
                nav.navigate(Routes.ADD)
            },
            onDismiss = { preview = null }
        )
    }
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MizanBottomBar(
    current: String?,
    onSelect: (String) -> Unit,
    onAdd: () -> Unit,
    onAddLongPress: () -> Unit
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
                // Bouton + : appui court = formulaire, appui long = dictée
                Box(
                    Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Emerald)
                        .combinedClickable(onClick = onAdd, onLongClick = onAddLongPress)
                        .semantics { contentDescription = "Ajouter une dépense (appui long : dicter)" },
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
