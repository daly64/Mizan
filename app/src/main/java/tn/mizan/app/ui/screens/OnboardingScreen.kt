package tn.mizan.app.ui.screens

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import tn.mizan.app.R
import tn.mizan.app.ui.components.MizanLogo
import tn.mizan.app.ui.theme.Cream
import tn.mizan.app.ui.theme.Emerald
import tn.mizan.app.ui.theme.Gold
import tn.mizan.app.ui.theme.Line
import tn.mizan.app.ui.theme.Muted
import tn.mizan.app.ui.theme.Slate

/** L'écran de bienvenue n'apparaît qu'au premier lancement. */
object Onboarding {
    private const val PREFS = "mizan_reglages"
    private const val KEY = "bienvenue_vue"
    fun seen(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY, false)
    fun markSeen(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY, true).apply()
}

private data class Page(@DrawableRes val image: Int, val description: String, val title: String, val text: String)

private val pages = listOf(
    Page(
        R.drawable.bienvenue_1,
        "Trois personnes devant un téléphone qui affiche une carte",
        "Bienvenue sur AL MIZAN",
        "Note tes dépenses en dinars, fixe tes budgets et garde l’équilibre chaque mois."
    ),
    Page(
        R.drawable.bienvenue_2,
        "Un anneau de budget, des pièces de dinar et une plante",
        "Fixe tes budgets",
        "Un budget par mois et par catégorie. L’anneau te montre en un coup d’œil ce qui reste, en dinars et millimes."
    ),
    Page(
        R.drawable.bienvenue_3,
        "Un téléphone protégé par un bouclier et un dossier de sauvegarde",
        "Tes données restent chez toi",
        "Aucun compte, rien en ligne. Une sauvegarde automatique dans le dossier de ton choix, à restaurer quand tu veux."
    )
)

/** Courbe douce utilisée pour toutes les entrées (la même que sur les maquettes). */
private val Douce = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)

@Composable
fun OnboardingScreen(onFinished: () -> Unit, onRestore: () -> Unit) {
    val pager = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()
    val last = pager.currentPage == pages.lastIndex

    Column(
        Modifier
            .fillMaxSize()
            .background(Cream)
            .safeDrawingPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Entree(0) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MizanLogo(36.dp)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("الميزان", color = Emerald, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("AL MIZAN", color = Gold, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 3.sp)
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            if (!last) {
                TextButton(onClick = onFinished) { Text("Passer", color = Emerald, fontWeight = FontWeight.Medium) }
            }
        }

        HorizontalPager(pager, Modifier.weight(1f)) { index ->
            val page = pages[index]
            // Effet de profondeur pendant le glissement : l'image se décale moins vite que le texte.
            val offset = (pager.currentPage - index) + pager.currentPageOffsetFraction
            Column(
                Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Entree(1, key = index) {
                    Flottante {
                        Image(
                            painterResource(page.image),
                            contentDescription = page.description,
                            modifier = Modifier
                                .widthIn(max = 340.dp)
                                .fillMaxWidth()
                                .aspectRatio(334f / 300f)
                                .graphicsLayer {
                                    translationX = offset * size.width * 0.35f
                                    alpha = 1f - kotlin.math.abs(offset).coerceAtMost(1f) * 0.6f
                                }
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))
                Entree(2, key = index) {
                    Text(page.title, color = Slate, fontSize = 26.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                }
                Spacer(Modifier.height(10.dp))
                Entree(3, key = index) {
                    Text(
                        page.text,
                        color = Muted,
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.widthIn(max = 320.dp)
                    )
                }
            }
        }

        // Points de pagination : le point actif s'allonge en douceur.
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 18.dp)) {
            pages.indices.forEach { i ->
                val width by animateDpAsState(if (i == pager.currentPage) 20.dp else 6.dp, tween(350, easing = Douce), label = "point")
                Box(
                    Modifier
                        .height(6.dp)
                        .width(width)
                        .background(if (i == pager.currentPage) Emerald else Line, RoundedCornerShape(50))
                )
            }
        }

        Entree(4) {
            Button(
                onClick = {
                    if (last) onFinished() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Emerald, contentColor = Color.White),
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .animateContentSize()
            ) {
                Text(if (last) "C’est parti" else "Suivant", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        TextButton(onClick = onRestore, modifier = Modifier.padding(top = 4.dp)) {
            Text("Restaurer une sauvegarde", color = Emerald, fontWeight = FontWeight.Medium)
        }
    }
}

/**
 * Entrée animée : l'élément monte de 16 dp et apparaît, avec un décalage de 90 ms par [ordre].
 * Rejouée à chaque nouvelle page grâce à [key]. Android raccourcit ou coupe ces animations
 * si le téléphone est réglé pour réduire les animations.
 */
@Composable
private fun Entree(ordre: Int, key: Any = Unit, content: @Composable () -> Unit) {
    val progress = remember(key) { Animatable(0f) }
    val shift = with(LocalDensity.current) { 16.dp.toPx() }
    LaunchedEffect(key) {
        delay(90L * ordre)
        progress.animateTo(1f, tween(600, easing = Douce))
    }
    Box(Modifier.graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * shift
    }) { content() }
}

/** L'illustration flotte doucement de haut en bas. */
@Composable
private fun Flottante(content: @Composable () -> Unit) {
    val transition = rememberInfiniteTransition(label = "flotte")
    val y by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2_500, easing = androidx.compose.animation.core.FastOutSlowInEasing), RepeatMode.Reverse),
        label = "y"
    )
    val amplitude = with(LocalDensity.current) { 6.dp.toPx() }
    Box(Modifier.graphicsLayer { translationY = -amplitude * y }) { content() }
}
