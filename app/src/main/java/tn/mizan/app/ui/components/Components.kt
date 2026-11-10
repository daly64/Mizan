package tn.mizan.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tn.mizan.app.data.Category
import tn.mizan.app.data.Expense
import tn.mizan.app.ui.theme.CardWhite
import tn.mizan.app.ui.theme.Cream
import tn.mizan.app.ui.theme.Emerald
import tn.mizan.app.ui.theme.Gold
import tn.mizan.app.ui.theme.Line
import tn.mizan.app.ui.theme.Mint
import tn.mizan.app.ui.theme.Muted
import tn.mizan.app.ui.theme.Slate
import tn.mizan.app.util.Dates
import tn.mizan.app.util.Money

/** Symbole du dinar Mizan : « د » traversé de deux traits. */
@Composable
fun DinarSymbol(color: Color, size: Dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        val k = this.size.width / 100f
        val dal = Path().apply {
            moveTo(36 * k, 24 * k)
            cubicTo(46 * k, 28 * k, 56 * k, 34 * k, 60 * k, 42 * k)
            lineTo(60 * k, 70 * k)
            cubicTo(60 * k, 74 * k, 58 * k, 76 * k, 54 * k, 76 * k)
            lineTo(22 * k, 76 * k)
        }
        drawPath(dal, color, style = Stroke(width = 9 * k, cap = StrokeCap.Round, join = StrokeJoin.Round))
        listOf(50f, 62f).forEach { y ->
            drawLine(color, Offset(16 * k, y * k), Offset(74 * k, y * k), strokeWidth = 8 * k, cap = StrokeCap.Round)
        }
    }
}

/** Montant : symbole avant le nombre, millimes en plus petit (ex. د 2,825.225). */
@Composable
fun MoneyText(
    millimes: Long,
    fontSize: TextUnit,
    color: Color,
    modifier: Modifier = Modifier,
    sign: String? = null
) {
    val (dinars, rest) = Money.parts(millimes)
    val symbolSize = with(LocalDensity.current) { (fontSize * 0.8f).toDp() }
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        if (sign != null) {
            Text(sign, color = color, fontSize = fontSize, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(2.dp))
        }
        DinarSymbol(color, symbolSize)
        Spacer(Modifier.width(4.dp))
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(fontSize = fontSize, fontWeight = FontWeight.SemiBold)) { append(dinars) }
                withStyle(SpanStyle(fontSize = fontSize * 0.62f, fontWeight = FontWeight.Medium)) { append(rest) }
            },
            color = color
        )
    }
}

/** Logo Mizan : balance dans un cercle. */
@Composable
fun MizanLogo(
    size: Dp,
    modifier: Modifier = Modifier,
    circle: Color = Emerald,
    lines: Color = Cream,
    accent: Color = Gold
) {
    Canvas(modifier.size(size)) {
        val k = this.size.width / 100f
        fun line(x1: Float, y1: Float, x2: Float, y2: Float, w: Float) =
            drawLine(lines, Offset(x1 * k, y1 * k), Offset(x2 * k, y2 * k), strokeWidth = w * k, cap = StrokeCap.Round)

        drawCircle(circle, radius = 42 * k, center = Offset(50 * k, 50 * k))
        line(50f, 27f, 50f, 72f, 4f)
        line(39f, 74f, 61f, 74f, 4f)
        line(24f, 35f, 76f, 35f, 4f)
        line(28f, 35f, 21f, 53f, 1.5f)
        line(28f, 35f, 35f, 53f, 1.5f)
        line(72f, 35f, 65f, 53f, 1.5f)
        line(72f, 35f, 79f, 53f, 1.5f)
        drawCircle(accent, radius = 3.5f * k, center = Offset(50 * k, 26 * k))
        listOf(28f, 72f).forEach { cx ->
            val plate = Path().apply {
                moveTo((cx - 9) * k, 53 * k)
                quadraticBezierTo(cx * k, 64 * k, (cx + 9) * k, 53 * k)
                close()
            }
            drawPath(plate, accent)
        }
    }
}

@Composable
fun IconCircle(icon: ImageVector, size: Dp, tint: Color = Emerald, background: Color = Mint) {
    Box(Modifier.size(size).background(background, CircleShape), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.5f))
    }
}

/** Carte blanche arrondie utilisée partout. */
@Composable
fun MizanCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(modifier = modifier, shape = RoundedCornerShape(20.dp), color = CardWhite, content = content)
}

@Composable
fun SectionHeader(title: String, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Slate, modifier = Modifier.weight(1f))
        if (action != null) {
            Text(
                action,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Emerald,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(enabled = onAction != null) { onAction?.invoke() }
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
fun CategoryTile(
    category: Category,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: () -> Unit = {}
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = if (selected) Mint else CardWhite,
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) Emerald else Line)
    ) {
        Column(
            Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconCircle(category.icon, 44.dp, background = if (selected) CardWhite else Mint)
            Text(category.label, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Slate, maxLines = 1)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransactionRow(expense: Expense, onLongClick: (() -> Unit)? = null) {
    val shape = RoundedCornerShape(16.dp)
    Surface(
        shape = shape,
        color = CardWhite,
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .combinedClickable(enabled = onLongClick != null, onClick = {}, onLongClick = onLongClick)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconCircle(expense.category.icon, 40.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    expense.note.ifBlank { expense.category.label },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Slate,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${Dates.dayLabel(Dates.toLocalDate(expense.dateMillis))} · ${expense.category.label}",
                    fontSize = 12.sp,
                    color = Muted
                )
            }
            Spacer(Modifier.width(8.dp))
            MoneyText(
                expense.amountMillimes,
                14.sp,
                if (expense.isIncome) Emerald else Slate,
                sign = if (expense.isIncome) "+" else "−"
            )
        }
    }
}

@Composable
fun EmptyHint(text: String) {
    MizanCard(Modifier.fillMaxWidth()) {
        Text(text, fontSize = 13.sp, color = Muted, modifier = Modifier.padding(18.dp))
    }
}
