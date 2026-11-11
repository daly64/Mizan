package tn.mizan.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
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
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import tn.mizan.app.data.Updates
import tn.mizan.app.ui.screens.Onboarding
import tn.mizan.app.ui.screens.OnboardingScreen
import tn.mizan.app.ui.screens.UpdateSheet
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import tn.mizan.app.ui.screens.AddExpenseScreen
import tn.mizan.app.ui.screens.AnimatedSplash
import tn.mizan.app.ui.screens.BilanScreen
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
    const val PROFILE = "profile"
    const val BILAN = "bilan"
}

@Composable
fun MizanApp(vm: MizanViewModel = viewModel()) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showSplash by rememberSaveable { mutableStateOf(true) }
    var showWelcome by rememberSaveable { mutableStateOf(!Onboarding.seen(context)) }
    var openSettings by rememberSaveable { mutableStateOf(false) }
    var update by remember { mutableStateOf<Updates.Release?>(null) }

    /** Cherche une nouvelle version ; [manual] = demandé depuis les réglages (on affiche le résultat). */
    fun checkUpdates(manual: Boolean) = scope.launch {
        when (val result = Updates.check(context)) {
            is Updates.Check.Available ->
                if (manual || !Updates.isSkipped(context, result.release)) update = result.release
            Updates.Check.UpToDate ->
                if (manual) Toast.makeText(context, "AL MIZAN est à jour (${Updates.currentVersionName})", Toast.LENGTH_SHORT).show()
            is Updates.Check.Failed ->
                if (manual) Toast.makeText(context, result.reason, Toast.LENGTH_LONG).show()
        }
    }

    if (showSplash) {
        AnimatedSplash(onFinished = { showSplash = false })
        return
    }
    if (showWelcome) {
        OnboardingScreen(
            onFinished = { Onboarding.markSeen(context); showWelcome = false },
            onRestore = { Onboarding.markSeen(context); openSettings = true; showWelcome = false }
        )
        return
    }

    // Vérification automatique des mises à jour (au plus toutes les 12 h, en silence si rien de neuf).
    LaunchedEffect(Unit) { if (Updates.shouldAutoCheck(context)) checkUpdates(manual = false) }
    update?.let { release -> UpdateSheet(release, onDismiss = { update = null }) }

    val nav = rememberNavController()
    LaunchedEffect(openSettings) {
        if (openSettings) { nav.navigate(Routes.PROFILE) { launchSingleTop = true }; openSettings = false }
    }
    val state by vm.state.collectAsStateWithLifecycle()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val showBar = route != Routes.ADD

    Scaffold(
        containerColor = Cream,
        // En plein écran, on garde quand même le contenu à l'écart de l'encoche de la caméra.
        contentWindowInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout),
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
                    state,
                    onSeeAll = { nav.navigate(Routes.TRANSACTIONS) },
                    onSettings = { nav.navigate(Routes.PROFILE) { launchSingleTop = true } }
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
            composable(Routes.BILAN) {
                val all by vm.all.collectAsStateWithLifecycle()
                BilanScreen(all, state.budgets)
            }
            composable(Routes.PROFILE) {
                ProfileScreen(
                    state,
                    onRestore = vm::restore,
                    onBackupNow = vm::backupNow,
                    onCheckUpdates = { checkUpdates(manual = true) }
                )
            }
        }
    }

}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

@Composable
private fun MizanBottomBar(
    current: String?,
    onSelect: (String) -> Unit,
    onAdd: () -> Unit
) {
    // Accueil, Bilan, +, Transactions, Réglages.
    val left = listOf(Tab(Routes.HOME, "Accueil", Icons.Outlined.Home), Tab(Routes.BILAN, "Bilan", Icons.Outlined.BarChart))
    val right = listOf(
        Tab(Routes.TRANSACTIONS, "Transactions", Icons.AutoMirrored.Outlined.List),
        Tab(Routes.PROFILE, "Réglages", Icons.Outlined.Settings)
    )
    Surface(color = CardWhite) {
        Column {
            HorizontalDivider(color = Line)
            Row(
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
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
