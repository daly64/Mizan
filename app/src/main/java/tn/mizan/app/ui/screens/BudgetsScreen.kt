package tn.mizan.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tn.mizan.app.data.Category
import tn.mizan.app.ui.MonthState
import tn.mizan.app.ui.components.IconCircle
import tn.mizan.app.ui.components.MizanCard
import tn.mizan.app.ui.components.MoneyText
import tn.mizan.app.ui.components.SectionHeader
import tn.mizan.app.ui.theme.Danger
import tn.mizan.app.ui.theme.Emerald
import tn.mizan.app.ui.theme.Gold
import tn.mizan.app.ui.theme.Mint
import tn.mizan.app.ui.theme.Muted
import tn.mizan.app.ui.theme.Slate
import tn.mizan.app.util.Dates
import tn.mizan.app.util.Money
import kotlin.math.roundToInt

@Composable
fun BudgetsScreen(state: MonthState) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Budgets", fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = Slate, modifier = Modifier.weight(1f))
                Text(Dates.monthTitle(), fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Muted)
            }
        }
        item { OverviewCard(state) }
        item { SectionHeader("Par catégorie") }
        items(Category.entries) { c -> CategoryBudgetCard(c, state.spentBy(c), state.budgets[c] ?: 0L) }
    }
}

@Composable
private fun OverviewCard(state: MonthState) {
    val ratio = state.budgetRatio
    MizanCard(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(110.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val stroke = 14.dp.toPx()
                    val inset = stroke / 2
                    val arcSize = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke)
                    val topLeft = androidx.compose.ui.geometry.Offset(inset, inset)
                    drawArc(Mint, 0f, 360f, false, topLeft, arcSize, style = Stroke(stroke))
                    drawArc(
                        if (ratio > 1f) Danger else Emerald,
                        -90f,
                        360f * ratio.coerceIn(0f, 1f),
                        false,
                        topLeft,
                        arcSize,
                        style = Stroke(stroke, cap = StrokeCap.Round)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${(ratio * 100).roundToInt()} %", fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = Slate)
                    Text("utilisé", fontSize = 11.sp, color = Muted)
                }
            }
            Spacer(Modifier.width(18.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Figure("Dépensé", state.spent, Slate)
                Figure("Budget", state.budgetTotal, Slate)
                val rest = state.budgetTotal - state.spent
                Figure("Reste", rest, if (rest < 0) Danger else Emerald)
            }
        }
    }
}

@Composable
private fun Figure(label: String, millimes: Long, color: Color) {
    Column {
        Text(label, fontSize = 12.sp, color = Muted)
        MoneyText(millimes, 15.sp, color, sign = if (millimes < 0) "−" else null)
    }
}

@Composable
private fun CategoryBudgetCard(category: Category, spent: Long, limit: Long) {
    val ratio = if (limit > 0) spent.toFloat() / limit else 0f
    val over = spent > limit && limit > 0
    val barColor = when {
        over -> Danger
        ratio > 0.8f -> Gold
        else -> Emerald
    }
    MizanCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconCircle(category.icon, 36.dp)
                Spacer(Modifier.width(12.dp))
                Text(category.label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Slate, modifier = Modifier.weight(1f))
                Text(
                    "${(ratio * 100).roundToInt()} %",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (over) Danger else Emerald
                )
            }
            LinearProgressIndicator(
                progress = { ratio.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = barColor,
                trackColor = Mint,
                strokeCap = StrokeCap.Round,
                gapSize = 0.dp,
                drawStopIndicator = {}
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                MoneyText(spent, 13.sp, Slate)
                Spacer(Modifier.weight(1f))
                Text(
                    if (over) "Dépassé de ${Money.format(spent - limit)}" else "sur ${Money.format(limit)}",
                    fontSize = 12.sp,
                    color = if (over) Danger else Muted
                )
            }
        }
    }
}
