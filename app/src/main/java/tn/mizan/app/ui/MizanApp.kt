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

@Composable
fun MizanApp(vm: MizanViewModel = viewModel()) {
    var showSplash by rememberSaveable { mutableStateOf(true) }
    if (showSplash) {
        AnimatedSplash(onFinished = { showSplash = false })
        return
    }

    val nav = rememberNavController()
    val state by vm.state.collectAsStateWithLifecycle()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val showBar = route != Routes.ADD

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
                HomeScreen(state, onSeeAll = { nav.navigate(Routes.TRANSACTIONS) })
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
            composable(Routes.PROFILE) { ProfileScreen(state) }
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
