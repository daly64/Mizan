package tn.mizan.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.foundation.layout.size
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.expandVertically
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tn.mizan.app.data.Category
import tn.mizan.app.data.Conseils
import tn.mizan.app.data.Questions
import tn.mizan.app.data.Expense
import tn.mizan.app.ui.components.EmptyHint
import tn.mizan.app.ui.components.IconCircle
import tn.mizan.app.ui.components.MizanCard
import tn.mizan.app.ui.components.MoneyText
import tn.mizan.app.ui.components.SectionHeader
import tn.mizan.app.ui.components.TransactionRow
import tn.mizan.app.ui.theme.CardWhite
import tn.mizan.app.ui.theme.Danger
import tn.mizan.app.ui.theme.Emerald
import tn.mizan.app.ui.theme.Gold
import tn.mizan.app.ui.theme.Line
import tn.mizan.app.ui.theme.Mint
import tn.mizan.app.ui.theme.Muted
import tn.mizan.app.ui.theme.Slate
import tn.mizan.app.util.Dates
import tn.mizan.app.util.Money
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/** Bilan par jour ou par mois, calculé sur toutes les transactions enregistrées (tout reste sur le téléphone). */
@Composable
fun BilanScreen(all: List<Expense>, budgets: Map<Category, Long>) {
    var byMonth by rememberSaveable { mutableStateOf(false) }
    var dayEpoch by rememberSaveable { mutableStateOf(LocalDate.now().toEpochDay()) }
    var monthIndex by rememberSaveable { mutableStateOf(YearMonth.now().let { it.year * 12 + it.monthValue - 1 }) }
    val day = LocalDate.ofEpochDay(dayEpoch)
    val month = YearMonth.of(monthIndex / 12, monthIndex % 12 + 1)

    // Transactions groupées par jour une seule fois.
    val byDay = remember(all) { all.groupBy { Dates.toLocalDate(it.dateMillis) } }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { Text("Bilan", fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = Slate) }
        item {
            Segmented(byMonth) { byMonth = it }
        }
        item {
            if (byMonth) {
                Stepper(
                    label = month.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.FRENCH)).replaceFirstChar { it.uppercase() },
                    canNext = month < YearMonth.now(),
                    onPrev = { monthIndex-- },
                    onNext = { monthIndex++ }
                )
            } else {
                Stepper(
                    label = Dates.longLabel(day),
                    canNext = day < LocalDate.now(),
                    onPrev = { dayEpoch-- },
                    onNext = { dayEpoch++ }
                )
            }
        }
        item {
            AnimatedContent(
                targetState = if (byMonth) "m$monthIndex" else "d$dayEpoch",
                transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(150)) },
                label = "bilan"
            ) { _ ->
                if (byMonth) MonthReport(month, byDay, budgets, all) else DayReport(day, byDay, all)
            }
        }
    }
}

// ---------------------------------------------------------------- Jour

@Composable
private fun DayReport(day: LocalDate, byDay: Map<LocalDate, List<Expense>>, all: List<Expense>) {
    val list = byDay[day].orEmpty().sortedByDescending { it.dateMillis }
    val spent = list.spent()
    val income = list.income()
    // Moyenne des jours précédents du même mois (jours écoulés avant ce jour), pour situer la journée.
    val previousDays = (1 until day.dayOfMonth).map { day.withDayOfMonth(it) }
    val average = if (previousDays.isEmpty()) null else previousDays.sumOf { byDay[it].orEmpty().spent() } / previousDays.size

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Summary(spent, income, comparison = average?.let { avg ->
            when {
                avg == 0L && spent == 0L -> null
                avg == 0L -> "Plus que d’habitude ce mois-ci"
                else -> {
                    val pct = ((spent - avg) * 100f / avg).roundToInt()
                    when {
                        pct > 5 -> "$pct % de plus que ta moyenne du mois (${Money.format(avg)} par jour)"
                        pct < -5 -> "${abs(pct)} % de moins que ta moyenne du mois (${Money.format(avg)} par jour)"
                        else -> "Dans ta moyenne du mois (${Money.format(avg)} par jour)"
                    }
                }
            }
        })

        // Les 7 derniers jours, le jour choisi en vert.
        val week = (6 downTo 0).map { day.minusDays(it.toLong()) }
        MizanCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("7 derniers jours", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Slate)
                Bars(
                    values = week.map { byDay[it].orEmpty().spent() },
                    labels = week.map { it.format(DateTimeFormatter.ofPattern("EEE", Locale.FRENCH)).take(3) },
                    highlight = 6,
                    description = "Dépenses des 7 derniers jours"
                )
            }
        }

        CategoryBreakdown(list)

        SectionHeader("Transactions du jour")
        if (list.isEmpty()) EmptyHint("Aucune transaction ce jour-là.")
        else Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { list.forEach { TransactionRow(it) } }
    }
}

// ---------------------------------------------------------------- Mois

@Composable
private fun MonthReport(month: YearMonth, byDay: Map<LocalDate, List<Expense>>, budgets: Map<Category, Long>, all: List<Expense>) {
    val days = (1..month.lengthOfMonth()).map { month.atDay(it) }
    val list = days.flatMap { byDay[it].orEmpty() }
    val spent = list.spent()
    val income = list.income()
    val prevSpent = month.minusMonths(1).let { p -> (1..p.lengthOfMonth()).sumOf { byDay[p.atDay(it)].orEmpty().spent() } }
    val today = LocalDate.now()
    val elapsed = if (month == YearMonth.from(today)) today.dayOfMonth else month.lengthOfMonth()
    val perDay = days.map { byDay[it].orEmpty().spent() }
    val busiest = perDay.withIndex().maxByOrNull { it.value }?.takeIf { it.value > 0 }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Summary(spent, income, comparison = when {
            prevSpent == 0L -> null
            else -> {
                val pct = ((spent - prevSpent) * 100f / prevSpent).roundToInt()
                if (pct >= 0) "$pct % de plus que le mois précédent (${Money.format(prevSpent)})"
                else "${abs(pct)} % de moins que le mois précédent (${Money.format(prevSpent)})"
            }
        })

        val bilan = remember(month, all, budgets) { Conseils.pour(month, all, budgets) }
        ConseilsCard(bilan)
        AskCard(month, all, budgets)

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Stat("Moyenne par jour", Money.format(if (elapsed > 0) spent / elapsed else 0), Modifier.weight(1f))
            Stat(
                "Jour le plus cher",
                busiest?.let { "${it.index + 1} · ${Money.format(it.value)}" } ?: "—",
                Modifier.weight(1f)
            )
        }

        val budgetTotal = budgets.values.sum()
        if (budgetTotal > 0) {
            val ratio = spent.toFloat() / budgetTotal
            MizanCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row {
                        Text("Budget du mois", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Slate, modifier = Modifier.weight(1f))
                        Text("${(ratio * 100).roundToInt()} %", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = if (ratio > 1f) Danger else Slate)
                    }
                    ProgressBar(ratio, if (ratio > 1f) Danger else Gold)
                    Text(
                        if (spent <= budgetTotal) "Reste ${Money.format(budgetTotal - spent)} sur ${Money.format(budgetTotal)}"
                        else "Dépassé de ${Money.format(spent - budgetTotal)}",
                        fontSize = 12.sp,
                        color = Muted
                    )
                }
            }
        }

        MizanCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Dépenses jour par jour", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Slate)
                Bars(
                    values = perDay,
                    labels = days.map { if (it.dayOfMonth == 1 || it.dayOfMonth % 5 == 0) "${it.dayOfMonth}" else "" },
                    highlight = busiest?.index,
                    description = "Dépenses de chaque jour du mois"
                )
            }
        }

        CategoryBreakdown(list)

        val top = list.filter { !it.isIncome }.sortedByDescending { it.amountMillimes }.take(3)
        if (top.isNotEmpty()) {
            SectionHeader("Plus grosses dépenses")
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { top.forEach { TransactionRow(it) } }
        }
    }
}

// ---------------------------------------------------------------- Bilan et conseils de la mini IA

@Composable
private fun ConseilsCard(bilan: Conseils.Bilan) {
    MizanCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = Gold, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Bilan et conseils", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Slate, modifier = Modifier.weight(1f))
                Text("Mini IA", fontSize = 11.sp, color = Muted)
            }
            Text(bilan.resume, fontSize = 13.sp, lineHeight = 18.sp, color = Slate)
            bilan.conseils.forEachIndexed { index, conseil ->
                // Les conseils apparaissent un par un, en glissant doucement.
                val visible = remember(bilan) { MutableTransitionState(false).apply { targetState = true } }
                val color = when (conseil.kind) {
                    Conseils.Kind.ALERTE -> Danger
                    Conseils.Kind.ATTENTION -> Gold
                    Conseils.Kind.BRAVO -> Emerald
                    Conseils.Kind.INFO -> Muted
                }
                AnimatedVisibility(
                    visibleState = visible,
                    enter = fadeIn(tween(350, delayMillis = 150 * index)) +
                        slideInVertically(tween(350, delayMillis = 150 * index)) { it / 2 }
                ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Mint)
                        .padding(10.dp)
                ) {
                    Box(
                        Modifier
                            .padding(top = 5.dp)
                            .size(8.dp)
                            .clip(RoundedCornerShape(50))
                            .background(color)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(conseil.text, fontSize = 12.sp, lineHeight = 17.sp, color = Slate)
                }
                }
            }
        }
    }
}

/** « Demande à la mini IA » : questions en un clic ou écrites, réponses calculées sur le mois affiché. */
@Composable
private fun AskCard(month: YearMonth, all: List<Expense>, budgets: Map<Category, Long>) {
    val conversation = remember(month) { mutableStateListOf<Pair<String, String>>() }
    var text by remember(month) { mutableStateOf("") }
    fun ask(question: String) {
        if (question.isBlank()) return
        conversation += question.trim() to Questions.repondre(question, month, all, budgets)
        if (conversation.size > 4) conversation.removeAt(0)
        text = ""
    }
    MizanCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = Gold, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Demande à la mini IA", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Slate)
            }
            // Questions en un clic
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Questions.suggestions.forEach { s ->
                    Text(
                        s,
                        fontSize = 12.sp,
                        color = Emerald,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Mint)
                            .clickable { ask(s) }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
            // La conversation : question à droite, réponse à gauche, qui apparaît en douceur
            conversation.forEach { (question, answer) ->
                val visible = remember(question, answer) { MutableTransitionState(false).apply { targetState = true } }
                AnimatedVisibility(visibleState = visible, enter = fadeIn(tween(300)) + expandVertically(tween(300))) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(Modifier.fillMaxWidth()) {
                            Spacer(Modifier.weight(1f))
                            Text(
                                question,
                                fontSize = 12.sp,
                                color = Color.White,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp, 14.dp, 4.dp, 14.dp))
                                    .background(Emerald)
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                        Text(
                            answer,
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            color = Slate,
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp, 14.dp, 14.dp, 4.dp))
                                .background(Mint)
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.take(80) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("Ex. : combien pour le taxi ?", color = Muted, fontSize = 13.sp) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { ask(text) }),
                trailingIcon = {
                    IconButton(onClick = { ask(text) }, enabled = text.isNotBlank()) {
                        Icon(Icons.AutoMirrored.Outlined.Send, contentDescription = "Envoyer", tint = if (text.isNotBlank()) Emerald else Line)
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
        }
    }
}

// ---------------------------------------------------------------- Éléments communs

private fun List<Expense>.spent() = filter { !it.isIncome }.sumOf { it.amountMillimes }
private fun List<Expense>.income() = filter { it.isIncome }.sumOf { it.amountMillimes }

@Composable
private fun Segmented(byMonth: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(Mint)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        listOf(false to "Par jour", true to "Par mois").forEach { (value, label) ->
            val selected = byMonth == value
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(50))
                    .background(if (selected) Emerald else Color.Transparent)
                    .clickable { onChange(value) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    fontSize = 14.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (selected) Color.White else Muted
                )
            }
        }
    }
}

@Composable
private fun Stepper(label: String, canNext: Boolean, onPrev: () -> Unit, onNext: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onPrev) {
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, contentDescription = "Précédent", tint = Emerald)
        }
        Text(label, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Slate, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
        IconButton(onClick = onNext, enabled = canNext) {
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = "Suivant", tint = if (canNext) Emerald else Line)
        }
    }
}

@Composable
private fun Summary(spent: Long, income: Long, comparison: String?) {
    MizanCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Dépensé", fontSize = 13.sp, color = Muted)
            MoneyText(spent, 30.sp, Slate)
            comparison?.let { Text(it, fontSize = 12.sp, color = Muted) }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("Revenus", fontSize = 12.sp, color = Muted)
                    MoneyText(income, 16.sp, Emerald)
                }
                Column(Modifier.weight(1f)) {
                    Text("Solde", fontSize = 12.sp, color = Muted)
                    MoneyText(income - spent, 16.sp, if (income - spent < 0) Danger else Slate, sign = if (income - spent < 0) "−" else null)
                }
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier) {
    MizanCard(modifier) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, fontSize = 12.sp, color = Muted)
            Text(value, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Slate)
        }
    }
}

@Composable
private fun ProgressBar(ratio: Float, color: Color) {
    val animated by animateFloatAsState(ratio.coerceIn(0f, 1f), tween(700), label = "barre")
    Box(
        Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(50))
            .background(Mint)
    ) {
        Box(
            Modifier
                .fillMaxWidth(animated)
                .height(8.dp)
                .clip(RoundedCornerShape(50))
                .background(color)
        )
    }
}

/** Répartition des dépenses par catégorie : montant, part du total, barre. */
@Composable
private fun CategoryBreakdown(list: List<Expense>) {
    val spent = list.spent()
    if (spent == 0L) return
    val rows = Category.entries
        .map { c -> c to list.filter { !it.isIncome && it.category == c }.sumOf { it.amountMillimes } }
        .filter { it.second > 0 }
        .sortedByDescending { it.second }
    MizanCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Par catégorie", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Slate)
            rows.forEach { (category, amount) ->
                val share = amount.toFloat() / spent
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconCircle(category.icon, 36.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row {
                            Text(category.label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Slate, modifier = Modifier.weight(1f))
                            Text("${Money.format(amount)} · ${(share * 100).roundToInt()} %", fontSize = 12.sp, color = Muted)
                        }
                        ProgressBar(share, Gold)
                    }
                }
            }
        }
    }
}

/** Petit graphique en barres (dépenses), qui pousse en douceur à l'affichage. */
@Composable
private fun Bars(values: List<Long>, labels: List<String>, highlight: Int?, description: String) {
    val max = (values.maxOrNull() ?: 0L).coerceAtLeast(1L)
    val growAnim = remember(values) { Animatable(0f) }
    LaunchedEffect(values) { growAnim.animateTo(1f, tween(600)) }
    val grow = growAnim.value
    val gold = Gold
    val green = Emerald
    val track = Mint
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(120.dp)
                .semantics { contentDescription = description }
        ) {
            val n = values.size
            val slot = size.width / n
            val barWidth = (slot * 0.62f).coerceAtMost(28.dp.toPx())
            values.forEachIndexed { i, v ->
                val x = i * slot + (slot - barWidth) / 2
                val h = if (v == 0L) 3.dp.toPx() else (size.height * v / max * grow).coerceAtLeast(3.dp.toPx())
                drawRoundRect(
                    color = when {
                        v == 0L -> track
                        i == highlight -> green
                        else -> gold
                    },
                    topLeft = Offset(x, size.height - h),
                    size = Size(barWidth, h),
                    cornerRadius = CornerRadius(barWidth / 3, barWidth / 3)
                )
            }
        }
        Row(Modifier.fillMaxWidth()) {
            labels.forEach { label ->
                Text(label, fontSize = 10.sp, color = Muted, textAlign = TextAlign.Center, maxLines = 1, modifier = Modifier.weight(1f))
            }
        }
    }
}
