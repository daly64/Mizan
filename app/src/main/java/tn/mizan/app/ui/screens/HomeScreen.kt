package tn.mizan.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tn.mizan.app.data.Category
import tn.mizan.app.ui.MonthState
import tn.mizan.app.ui.components.CategoryTile
import tn.mizan.app.ui.components.EmptyHint
import tn.mizan.app.ui.components.MizanCard
import tn.mizan.app.ui.components.MizanLogo
import tn.mizan.app.ui.components.MoneyText
import tn.mizan.app.ui.components.SectionHeader
import tn.mizan.app.ui.components.TransactionRow
import tn.mizan.app.ui.theme.Danger
import tn.mizan.app.ui.theme.Emerald
import tn.mizan.app.ui.theme.EmeraldLight
import tn.mizan.app.ui.theme.Gold
import tn.mizan.app.ui.theme.Muted
import tn.mizan.app.ui.theme.OnEmeraldSoft
import tn.mizan.app.ui.theme.Slate
import tn.mizan.app.util.Dates
import tn.mizan.app.util.Money
import kotlin.math.roundToInt

@Composable
fun HomeScreen(state: MonthState, onSeeAll: () -> Unit) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { HomeTopBar() }
        item { TotalCard(state) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MiniCard("Revenus", state.income, Emerald, Modifier.weight(1f))
                MiniCard("Solde disponible", state.balance, Slate, Modifier.weight(1f))
            }
        }
        item { SectionHeader("Catégories") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Category.entries.forEach { CategoryTile(it, Modifier.weight(1f)) }
            }
        }
        item { SectionHeader("Dernières transactions", "Voir tout", onSeeAll) }
        val recent = state.expenses.take(5)
        if (recent.isEmpty()) {
            item { EmptyHint("Aucune dépense ce mois-ci. Appuie sur + pour en ajouter une.") }
        } else {
            items(recent, key = { it.id }) { TransactionRow(it) }
        }
    }
}

@Composable
private fun HomeTopBar() {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Outlined.Menu, contentDescription = "Menu", tint = Slate)
        Spacer(Modifier.weight(1f))
        MizanLogo(28.dp)
        Spacer(Modifier.width(6.dp))
        Text("ميـزان", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Emerald)
        Spacer(Modifier.weight(1f))
        Icon(Icons.Outlined.Notifications, contentDescription = "Notifications", tint = Slate)
    }
}

@Composable
private fun TotalCard(state: MonthState) {
    val ratio = state.budgetRatio
    Surface(shape = RoundedCornerShape(22.dp), color = Emerald) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Dépenses de ${Dates.monthName()}", color = OnEmeraldSoft, fontSize = 13.sp)
            MoneyText(state.spent, 30.sp, Color.White)
            LinearProgressIndicator(
                progress = { ratio.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = if (ratio > 1f) Danger else Gold,
                trackColor = EmeraldLight,
                strokeCap = StrokeCap.Round,
                gapSize = 0.dp,
                drawStopIndicator = {}
            )
            Row(Modifier.fillMaxWidth()) {
                Text("${(ratio * 100).roundToInt()} % du budget", color = OnEmeraldSoft, fontSize = 12.sp)
                Spacer(Modifier.weight(1f))
                Text("Budget : ${Money.format(state.budgetTotal)}", color = OnEmeraldSoft, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun MiniCard(label: String, millimes: Long, color: Color, modifier: Modifier) {
    MizanCard(modifier) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, fontSize = 12.sp, color = Muted)
            MoneyText(millimes, 17.sp, if (millimes < 0) Danger else color, sign = if (millimes < 0) "−" else null)
        }
    }
}
